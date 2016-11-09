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

	/**
	 * a Constant put into the 'externalId' field to "tag the tags" that are product related.
	 */
	public static final String SMT_PRODUCT_EXTERNALID = "smt-product";

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
	 * 
	 */
	public ShowpadTagManager(String apiUrl, String divisionId, String divisionUrl, ShowpadApiUtil util) throws QuotaException {
		this.showpadApiUrl = apiUrl;
		this.divisionId = divisionId;
		this.divisionUrl = divisionUrl;
		this.showpadUtil = util;
		showpadTags = new HashMap<>(1000);
		loadShowpadTagList();
	}


	/**
	 * Load a list of tags already at Showpad
	 * If we try to add a tag to an asset without using it's ID, and it already existing in the system, it will fail.
	 * @throws QuotaException 
	 */
	private void loadShowpadTagList() throws QuotaException {
		String tagUrl = divisionUrl + "/tags.json?limit=100000&id=" + divisionId + "&fields=id,name,externalId";
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
	public void addTags(MediaBinDeltaVO vo, StringBuilder header) throws QuotaException, InvalidDataException {
		Map<String,ShowpadTagVO> assignedTags = null;
		if (vo.getShowpadId() != null) assignedTags = loadAssetTags(vo.getShowpadId(), null, true); //suppress404 because the asset may be new
		Set<String> desiredTags = new HashSet<>();
		desiredTags.add("mediabin"); //a static tag for all assets, identifies their source

		//assign the tags this asset SHOULD have, attempt to backfill those from the known list of tags already in Showpad
		FileType ft = new FileType(vo.getFileNm());
		desiredTags.add(ft.getFileExtension());
		if (vo.getLanguageCode() != null && vo.getLanguageCode().length() > 0)
			desiredTags.addAll(Arrays.asList(vo.getLanguageCode().split(DSMediaBinImporterV2.TOKENIZER)));
		if (vo.getBusinessUnitNm() != null && vo.getBusinessUnitNm().length() > 0)
			desiredTags.addAll(Arrays.asList(vo.getBusinessUnitNm().split(DSMediaBinImporterV2.TOKENIZER)));
		if (vo.getLiteratureTypeTxt() != null && vo.getLiteratureTypeTxt().length() > 0)
			desiredTags.addAll(Arrays.asList(vo.getLiteratureTypeTxt().split(DSMediaBinImporterV2.TOKENIZER)));

		//loop the tags the asset already has, removing them from the "need to add" list
		if (assignedTags != null) {
			for (String tag : assignedTags.keySet())
				desiredTags.remove(tag);
		}

		//add what's left on the "need to add" list as new tags; both to the Asset, and to Showpad if they're new
		for (String tagNm : desiredTags) {
			if (tagNm == null || tagNm.isEmpty()) continue;
			log.info("asset needs tag " + tagNm);
			if (showpadTags.get(tagNm) == null) {
				//add it to the global list for the next iteration to leverage
				showpadTags.put(tagNm, createTag(tagNm, null));
			}

			if (header.length() > 0) header.append(",");
			header.append("<").append(showpadTags.get(tagNm).getId()).append(">; rel=\"Tag\"");
		}
	}


	/**
	 * returns a list of tags already attached to this asset
	 * @param showpadId
	 * @return
	 * @throws QuotaException 
	 * @throws InvalidDataException 
	 */
	private Map<String, ShowpadTagVO> loadAssetTags(String showpadId, String externalId, boolean suppress404) 
			throws QuotaException, InvalidDataException {
		Map<String,ShowpadTagVO> tags = new HashMap<>();
		String tagUrl = showpadApiUrl + "/assets/" + showpadId + "/tags.json?suppress_response_codes=true&fields=id,name,externalId";
		if (externalId != null) tagUrl += "&externalId=" + externalId; //filters to only tags on this asset with this externalId
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
	protected ShowpadTagVO createTag(String tagNm, String externalId) throws QuotaException {
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
	public void updateProductTags(MediaBinDeltaVO mbAsset) throws QuotaException, InvalidDataException {
		String showpadId = mbAsset.getShowpadId();
		if (showpadId == null || showpadId.isEmpty()) 
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
			if (tag.getExternalId() == null || !SMT_PRODUCT_EXTERNALID.equals(tag.getExternalId()))
				tagsToDelete.remove(tag.getName());
		}

		//remove from the 'add' list any tags we want to keep that are already tied to the asset.
		for (ShowpadTagVO tVo : assignedTags.values())
			tagsToAdd.remove(tVo.getName());
		
		//do the work
		log.debug("asset=" + mbAsset.getDpySynMediaBinId() + ", unlinking assets " + tagsToDelete.keySet());
		log.debug("asset=" + mbAsset.getDpySynMediaBinId() + ", linking assets " + tagsToAdd.keySet());
		unlinkAssetFromTags(showpadId, tagsToDelete.values());
		linkAssetToTags(showpadId, tagsToAdd.values());
	}
	
	
	/**
	 * iterates the list of tags, calling showpad to unlink each one from the given asset.
	 * @param showpadAssetId
	 * @param tags
	 */
	private void unlinkAssetFromTags(String showpadAssetId, Collection<ShowpadTagVO> tags) throws QuotaException {
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
	private void linkAssetToTags(String showpadAssetId, Collection<ShowpadTagVO> tags) throws QuotaException {
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
	private void executeAssetTagXR(String tagId, String showpadAssetId, String method) throws QuotaException {
		String url = showpadApiUrl + "/tags/" + tagId + "/assets/" + showpadAssetId + ".json?method=" + method;
		try {
			showpadUtil.executeGet(url);
			log.debug("asset-tag altered by executing: " + url);
		} catch (IOException e) {
			failures.add(e);
			log.error("could not update tag binding to showpad asset", e);
		}
	}
	
	
	public List<Exception> getFailures() {
		return failures;
	}
}