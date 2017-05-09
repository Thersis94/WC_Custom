package com.depuysynthes.scripts.showpad;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.depuysynthes.scripts.DSMediaBinImporterV2;
import com.depuysynthes.scripts.MediaBinDeltaVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.FileType;
import com.siliconmtn.util.StringUtil;

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
	 * adds the desired tags to the passed showpad asset (header)
	 * If the desired tag does not exist in Showpad, it must be added there first.
	 * @param vo
	 * @throws QuotaException 
	 */
	public void addTags(MediaBinDeltaVO vo, StringBuilder header) throws InvalidDataException {
		Map<String,ShowpadTagVO> assignedTags = null;
		if (vo.getShowpadId() != null) assignedTags = loadAssetTags(vo.getShowpadId(), null, true); //suppress404 because the asset may be new
		Set<String> tagsToAdd = getDesiredTags(vo);
		Set<String> desiredTags = new HashSet<>(tagsToAdd); //preserve this list for deletions

		//loop the tags the asset already has, removing them from the "need to add" list
		if (assignedTags != null) {
			for (String tag : assignedTags.keySet()) {
				tagsToAdd.remove(tag);
				//this line can be removed after a few runs in production, ~03.31.2017.  We only need it to clean up the legacy data
				checkExternalId(showpadTags.get(tag));
			}
		}

		//add what's left on the "need to add" list as new tags; both to the Asset, and to the Division in Showpad if they don't already exist
		for (String tagNm : tagsToAdd) {
			if (StringUtil.isEmpty(tagNm)) continue;
			tagNm = tagNm.trim();
			log.info("asset needs tag " + tagNm);
			ShowpadTagVO tagVo = showpadTags.get(tagNm);
			if (tagVo == null) {
				//add it to the global list for the next iteration to leverage
				tagVo = createTag(tagNm, SMT_MEDIABIN_EXTERNALID);
				showpadTags.put(tagNm, tagVo);
			} else {
				checkExternalId(tagVo);
			}

			if (header.length() > 0) header.append(",");
			header.append("<").append(tagVo.getId()).append(">; rel=\"Tag\"");
		}

		deleteUnwantedMBTags(vo, assignedTags, desiredTags);
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

		//fire some tag deletions to this existing asset
		//make a 'tagsToDelete' list by subtracting what we want (to add) from what we have
		Map<String, ShowpadTagVO> tagsToDelete = new HashMap<>(assignedTags);

		//don't delete any we want
		for (String tagNm : desiredTags)
			tagsToDelete.remove(tagNm);

		//loop through the tags we want to delete and first check that they're ours.  We can't delete tags that aren't ours.
		List<ShowpadTagVO> tags = new ArrayList<>(tagsToDelete.size());
		for (ShowpadTagVO tagVo : tagsToDelete.values()) {
			if (SMT_MEDIABIN_EXTERNALID.equals(tagVo.getExternalId())) {
				tags.add(tagVo);
				log.debug("unlinking mediabin tag: " + tagVo.getName());
			}
		}
		unlinkAssetFromTags(vo.getShowpadId(), tags);
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
	 * configures a set of tags we put on every mediabin asset - based on business rules
	 * @param vo
	 * @return
	 */
	protected Set<String> getDesiredTags(MediaBinDeltaVO vo) {
		Set<String> desiredTags = new HashSet<>();
		desiredTags.add("mediabin");
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

		return desiredTags;
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
	 * oversees application of product-catalog related tags to mediabin assets at Showpad.
	 * handles the load, merge, delete, add, and update transactions as applicable.
	 * Called from ShowpadProductDecorator
	 * @param mbAsset
	 * @throws QuotaException
	 */
	public void updateProductTags(MediaBinDeltaVO mbAsset) throws InvalidDataException {
		String showpadId = mbAsset.getShowpadId();
		if (StringUtil.isEmpty(showpadId)) 
			throw new InvalidDataException("Asset not found in Showpad: " + mbAsset.getTrackingNoTxt());

		Map<String, ShowpadTagVO> assignedTags = loadAssetTags(showpadId, null, false); //do not suppress404, asset should exist at this point
		Map<String, ShowpadTagVO> tagsToAdd = new HashMap<>();

		//put all the tags we want on the 'add' list, then we'll remove the ones that already exist
		for (ShowpadTagVO tag : mbAsset.getTags())
			tagsToAdd.put(tag.getName(), tag);

		//make a 'tagsToDelete' list by subtracting what we want (to add) from what we have
		Map<String, ShowpadTagVO> tagsToDelete = new HashMap<>(assignedTags);
		for (String tagNm : tagsToAdd.keySet()) {
			//don't delete any we want
			tagsToDelete.remove(tagNm);
		}
		for (ShowpadTagVO tag : assignedTags.values()) {
			//do not delete any that aren't smt-product tags; meaning they 
			//were created by someone else or something else and are not ours to delete.
			if (!SMT_PRODUCT_EXTERNALID.equals(tag.getExternalId()))
				tagsToDelete.remove(tag.getName());
		}

		//remove from the 'add' list any tags we want to keep that are already tied to the asset.
		for (ShowpadTagVO tVo : assignedTags.values())
			tagsToAdd.remove(tVo.getName());

		//do the work
		log.debug("asset=" + mbAsset.getDpySynMediaBinId() + ", previous tags: " + assignedTags.keySet());
		log.debug("asset=" + mbAsset.getDpySynMediaBinId() + ", unlinking tags: " + tagsToDelete.keySet());
		log.debug("asset=" + mbAsset.getDpySynMediaBinId() + ", linking tags: " + tagsToAdd.keySet());
		unlinkAssetFromTags(showpadId, tagsToDelete.values());
		linkAssetToTags(showpadId, tagsToAdd.values());
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
	protected void linkAssetToTags(String showpadAssetId, Collection<ShowpadTagVO> tags) {
		for (ShowpadTagVO tag : tags) {
			//if we don't know this tag by ID, get it from the Division's list if it already exists there
			if (tag.getId() == null && showpadTags.containsKey(tag.getName()))
				tag = showpadTags.get(tag.getName());

			//if the tag is not there, it needs to be added to the Division before we can bind assets to it.
			if (tag.getId() == null) {
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
}