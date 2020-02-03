package com.depuysynthes.scripts.showpad;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.depuysynthes.scripts.DSMediaBinImporterV2;
import com.depuysynthes.scripts.MediaBinDeltaVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.FileType;
import com.siliconmtn.util.StringUtil;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/****************************************************************************
 * <b>Title</b>: ShowpadTagManager.java<p/>
 * <b>Description: Manages interactions with Showpad related to Tags.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Aug 31, 2016
 ****************************************************************************/
public class ShowpadTagManager {

	protected static Logger log = Logger.getLogger(ShowpadTagManager.class);
	private String sourceConstant; //for all mediabin assets.  Gets replaced with "internal" for private assets

	/**
	 * a Constant put into the 'externalId' field to "tag the tags" that are product related.
	 */
	public static final String SMT_PRODUCT_EXTERNALID = "smt-product";
	public static final String SMT_MEDIABIN_EXTERNALID = "smt-mediabin";

	protected String divisionId;
	protected String divisionUrl;
	protected String showpadApiUrl;
	protected ShowpadApiUtil showpadUtil;
	private Map<String, ShowpadTagVO> showpadTags;

	/**
	 * List of errors 
	 */
	protected List <Exception> failures = new ArrayList<>();

	/**
	 * default constructor - this class requires arguments in order to function properly
	 */
	public ShowpadTagManager(String apiUrl, String divisionId, String divisionUrl, ShowpadApiUtil util) {
		this.showpadApiUrl = apiUrl;
		this.divisionId = divisionId;
		this.divisionUrl = divisionUrl;
		this.showpadUtil = util;
		showpadTags = new HashMap<>(3000);
		loadDivisionTagList();
	}


	/**
	 * Load a list of tags already at Showpad
	 * If we try to add a tag to an asset without using it's ID, and it already existing in the system, it will fail.
	 * @throws QuotaException 
	 */
	protected void loadDivisionTagList() {
		int fetchSize = 1000;
		int offset=0;
		do {
			loadTags(fetchSize, offset);
			offset += fetchSize;
			//if we've retrieve less than the maximum amount of tags, we're done.  If the #s are equal we need to iterate.
		} while (showpadTags.size() == offset);
	}


	/**
	 * loads a list of tags for the given division.  Takes into consideration the range limit (1000) and offset (for repeated calls).
	 * @param limit
	 * @param offset
	 * @throws QuotaException
	 */
	protected void loadTags(int limit, int offset) {
		String tagUrl = divisionUrl + "/tags.json?limit=" + limit + "&fields=id,name,externalId&offset=" + offset;
		log.debug(tagUrl);
		try {
			String resp = showpadUtil.executeGet(tagUrl);
			JSONObject json = JSONObject.fromObject(resp);
			log.info(json);
			JSONObject metaResp = json.getJSONObject("meta");
			if (!"200".equals(metaResp.getString("code")))
				throw new IOException(metaResp.getString("message"));

			JSONObject response = json.getJSONObject("response");
			JSONArray items = response.getJSONArray("items");
			for (int x=0; x < items.size(); x++) {
				ShowpadTagVO tagVo = new ShowpadTagVO(items.getJSONObject(x), divisionId);
				showpadTags.put(tagVo.getName(), tagVo);
			}

		} catch (IOException | NullPointerException ioe) {
			failures.add(ioe);
			log.error("could not load showpad tags", ioe);
		}

		log.info("loaded " + showpadTags.size() + " showpad tags");
	}


	/**
	 * Saves (adds & removes) the desired tags to the passed showpad asset (header)
	 * If the desired tag does not exist in Showpad, it must be added there first.
	 * @param vo
	 * @throws QuotaException 
	 */
	public void saveTags(MediaBinDeltaVO vo, StringBuilder header) throws InvalidDataException {
		Map<String,ShowpadTagVO> existingTags = loadAssetTags(vo.getShowpadId(), null, true); //suppress404 because the asset may be new
		Set<String> tagsToAdd = getDesiredTags(vo);
		Set<String> desiredTags = new HashSet<>(tagsToAdd); //preserve this list for deletions

		//loop the tags the asset already has, removing them from the "need to add" list
		for (String tag : existingTags.keySet())
			tagsToAdd.remove(tag);

		//add what's left on the "need to add" list as new tags; both to the Asset, and to the Division in Showpad if they don't already exist
		for (String tagNm : tagsToAdd) {
			//do not create or bind empty tags
			if (StringUtil.checkVal(tagNm).trim().isEmpty() || "null".equalsIgnoreCase(tagNm)) continue;

			tagNm = tagNm.trim();
			log.info("asset needs tag " + tagNm);
			ShowpadTagVO tagVo = showpadTags.get(tagNm);
			if (tagVo == null) {
				//add it to the global list for the next iteration to leverage
				tagVo = createTag(tagNm, SMT_MEDIABIN_EXTERNALID);
				showpadTags.put(tagNm, tagVo);
			} else {
				//if we are going to assign a tag, make sure we assume ownership of it so we can later remove it.
				//the mediabin process (here-in) is proclaimed authoritive with regard to tag ownership.
				//other scripts (like smt-product) can only authorititatively remove tags they create.
				checkExternalId(tagVo);
			}

			if (header.length() > 0) header.append(",");
			header.append("<").append(tagVo.getId()).append(">; rel=\"Tag\"");
		}

		deleteUnwantedMBTags(vo, existingTags, desiredTags);
	}


	/**
	 * Tests the tag to ensure it has the proper externalId - sets it in Showpad if not
	 * @param showpadTagVO
	 */
	protected void checkExternalId(ShowpadTagVO tagVo) {
		if (tagVo == null || SMT_MEDIABIN_EXTERNALID.equals(tagVo.getExternalId()))
			return;

		//fire an update at this tag (to showpad) to take ownership of it (make it a Mediabin tag!)
		tagVo.setExternalId(SMT_MEDIABIN_EXTERNALID);
		saveTagExternalId(tagVo);
		showpadTags.put(tagVo.getName(), tagVo);
	}


	/**
	 * updates a Tag in showpad to set the externalId value on it
	 * @param tagVo
	 */
	protected void saveTagExternalId(ShowpadTagVO tagVo) {
		String url = showpadApiUrl + "/tags/" + tagVo.getId() + ".json";
		Map<String, String> params = new HashMap<>();
		params.put("externalId", tagVo.getExternalId());
		try {
			showpadUtil.executePost(url, params);
			log.debug("updated tag " + tagVo.getId());
		} catch (IOException e) {
			log.error("could not update tag with id=" + tagVo.getId(), e);
		}
	}


	/**
	 * delete unwanted MEDIABIN tags from the asset.  There won't be any tags if this is an "add" scenario.
	 * @param vo
	 * @param assignedTags
	 * @param desiredTags
	 */
	protected void deleteUnwantedMBTags(MediaBinDeltaVO vo, Map<String, ShowpadTagVO> assignedTags,
			Set<String> desiredTags) {
		//if the asset had no existing tags, we're done.  There aren't any we need to worry about removing
		if (assignedTags == null || assignedTags.isEmpty()) return;

		Map<String, ShowpadTagVO> tagsToDelete = new HashMap<>();

		//loop through the tags already assigned
		for (ShowpadTagVO tag : assignedTags.values()) {
			if (!desiredTags.contains(tag.getName()) && SMT_MEDIABIN_EXTERNALID.equals(tag.getExternalId())) {
				//do not delete any that aren't smt-mediabin tags; meaning they 
				//were created by someone or something else and are not ours to delete.
				tagsToDelete.put(tag.getName(), tag);
				log.debug("unlinking mediabin tag: " + tag.getName());
			}
		}
		unlinkAssetFromTags(vo.getShowpadId(), tagsToDelete.values());
	}


	/**
	 * configures a set of tags we put on every mediabin asset - based on business rules
	 * @param vo
	 * @return
	 */
	protected Set<String> getDesiredTags(MediaBinDeltaVO vo) {
		Set<String> desiredTags = new HashSet<>();
		desiredTags.add("eos");
		if (!StringUtil.isEmpty(sourceConstant))
			desiredTags.add(sourceConstant); //an additional static tag for private assets.  This could be turned into a String[] if need be.

		//assign the tags this asset SHOULD have, attempt to backfill those from the known list of tags already in Showpad
		FileType ft = new FileType(vo.getFileNm());
		desiredTags.add(ft.getFileExtension());
		if (!StringUtil.isEmpty(vo.getLanguageCode()))
			desiredTags.addAll(Arrays.asList(vo.getLanguageCode().split(DSMediaBinImporterV2.TOKENIZER)));
		if (!StringUtil.isEmpty(vo.getLiteratureTypeTxt()))
			desiredTags.addAll(Arrays.asList(vo.getLiteratureTypeTxt().split(DSMediaBinImporterV2.TOKENIZER)));
		if (!StringUtil.isEmpty(vo.getProdNm()))
			desiredTags.addAll(Arrays.asList(vo.getProdNm().split(DSMediaBinImporterV2.TOKENIZER)));

		//add any tags assigned from other areas of the application
		desiredTags.addAll(vo.getDesiredTags());

		return removeFunkyChars(desiredTags);
	}


	/**
	 * replace any non-breaking spaces with regular spaces - 
	 * this causes hell on the eyes, and creates duplicates!
	 * This can't be done in a stream or forEach because it could result in duplicates, which violates the contract of Set.
	 * @param desiredTags
	 * @return
	 */
	private Set<String> removeFunkyChars(Set<String> desiredTags) {
		if (desiredTags == null || desiredTags.isEmpty()) return desiredTags;
		Set<String> tags = new HashSet<>(desiredTags.size());

		for (String tag : desiredTags) {
			if (tag.indexOf("\u00A0") > -1) {
				tag = tag.replace("\u00A0","\u0020"); //unicode non-breaking space w/regular space
			}
			tags.add(tag);
		}

		return tags;
	}


	/**
	 * returns a list of tags already attached to this asset
	 * @param showpadId
	 * @return
	 * @throws QuotaException 
	 * @throws InvalidDataException 
	 */
	protected Map<String, ShowpadTagVO> loadAssetTags(String showpadId, String externalId, boolean suppress404) 
			throws InvalidDataException {
		if (StringUtil.isEmpty(showpadId)) return Collections.emptyMap();

		Map<String,ShowpadTagVO> tags = new HashMap<>();
		String tagUrl = showpadApiUrl + "/assets/" + showpadId + "/tags.json?limit=1000&suppress_response_codes=true&fields=id,name,externalId";
		if (externalId != null) tagUrl += "&externalId=" + externalId; //filters to only tags on this asset with this externalId
		try {
			String resp = showpadUtil.executeGet(tagUrl);
			JSONObject json = JSONObject.fromObject(resp);
			log.info("tags=" + json);
			JSONObject metaResp = json.getJSONObject("meta");
			if (!"200".equals(metaResp.getString("code")))
				throw new IOException(metaResp.getString("message"));

			JSONObject response = json.getJSONObject("response");
			JSONArray items = response.getJSONArray("items");
			for (int x=0; x < items.size(); x++) {
				ShowpadTagVO tagVo = new ShowpadTagVO(items.getJSONObject(x), divisionId);
				tags.put(tagVo.getName(), tagVo);
			}

		} catch (IOException | NullPointerException ioe) {
			if (!suppress404 && "Not Found".equals(ioe.getMessage()))
				throw new InvalidDataException("Asset not found in Showpad.  Delete Showpad pointers & update Mediabin record on SMT's side to recreate: " + showpadId, ioe);
		}

		log.info("loaded " + tags.size() + " showpad tags: " + tags);
		return tags;
	}


	/**
	 * creates a new tag within Showpad
	 * returns the ID of the newly minted tag.
	 * @param showpadId
	 * @param tagNm
	 * @param tagId
	 * @throws QuotaException 
	 */
	protected ShowpadTagVO createTag(String tagNm, String externalId) {
		String tagUrl = divisionUrl + "/tags.json";
		ShowpadTagVO tag = new ShowpadTagVO(null, tagNm, divisionId, externalId);

		Map<String,String> params = new HashMap<>();
		params.put("name", tagNm);
		if (externalId != null) params.put("externalId", externalId);

		try {
			String resp = showpadUtil.executePost(tagUrl, params);
			JSONObject json = JSONObject.fromObject(resp);
			log.info(json);
			JSONObject metaResp = json.getJSONObject("meta");
			if (!"201".equals(metaResp.getString("code")))
				throw new IOException(metaResp.getString("message"));

			JSONObject response = json.getJSONObject("response");
			tag.setId(response.getString("id"));

		} catch (IOException | NullPointerException ioe) {
			failures.add(ioe);
			log.error("could not create showpad tag " + tagNm, ioe);
		}

		log.info("created tag " + tag.getName() + " with id=" + tag.getId());
		return tag;
	}


	/**
	 * iterates the list of tags, calling showpad to unlink each one from the given asset.
	 * @param showpadAssetId
	 * @param tags
	 */
	protected void unlinkAssetFromTags(String showpadAssetId, Collection<ShowpadTagVO> tags) {
		for (ShowpadTagVO tag : tags) {
			//if we don't know this tag by ID, get it from the Division's list.  It must exist at the Division level if it's bound to an Asset.
			if (tag.getId() == null) tag = showpadTags.get(tag.getName());

			//unbind the tag from the asset
			executeAssetTagXR(tag.getId(), showpadAssetId, "unlink");
		}
	}


	/**
	 * binds new tags to an asset, taking care to add these tags to the Divsion if they're not already there.
	 * @param showpadAssetId
	 * @param tags
	 * @throws QuotaException
	 */
	protected void linkAssetToProductTags(String showpadAssetId, Collection<ShowpadTagVO> tags) {
		for (ShowpadTagVO tag : tags) {
			//do not create or bind empty tags
			if (StringUtil.checkVal(tag.getName()).trim().isEmpty()) continue;

			//if we don't know this tag by ID, get it from the Division's list if it already exists there
			if (StringUtil.isEmpty(tag.getId()) && showpadTags.containsKey(tag.getName()))
				tag = showpadTags.get(tag.getName());

			//if the tag is not there, it needs to be added to the Division before we can bind assets to it.
			if (StringUtil.isEmpty(tag.getId())) {
				tag = createTag(tag.getName(), SMT_PRODUCT_EXTERNALID);
				showpadTags.put(tag.getName(), tag);
			}

			//bind the tag to the asset
			executeAssetTagXR(tag.getId(), showpadAssetId, "link");
		}
	}


	/**
	 * Executes the HTTP call for binding or unbinding tags from assets.
	 * It's the same call both ways; Showpad figures out the rest.
	 * @param tagId
	 * @param showpadAssetId
	 * @throws QuotaException
	 */
	protected void executeAssetTagXR(String tagId, String showpadAssetId, String method) {
		String url = showpadApiUrl + "/tags/" + tagId + "/assets/" + showpadAssetId + ".json?suppress_response_codes=true&method=" + method;
		try {
			String resp = showpadUtil.executeGet(url);
			JSONObject metaResp = JSONObject.fromObject(resp).getJSONObject("meta");
			if (!"200".equals(metaResp.getString("code")))
				throw new IOException(metaResp.getString("description"));

			log.debug("asset-tag altered by executing: " + url);
		} catch (IOException e) {
			failures.add(e);
			log.error("could not update tag binding to showpad asset", e);
		}
	}


	public List<Exception> getFailures() {
		return failures;
	}


	public String getSourceConstant() {
		return sourceConstant;
	}


	public void setSourceConstant(String sourceConstant) {
		this.sourceConstant = sourceConstant;
	}

	/**
	 * getting method used by TagUpdater to perform tag maintenance - nothing used by the general workflow
	 * @return
	 */
	protected Map<String, ShowpadTagVO> getShowpadTags() {
		return showpadTags;
	}
}
