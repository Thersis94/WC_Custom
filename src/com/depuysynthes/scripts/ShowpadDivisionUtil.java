package com.depuysynthes.scripts;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import com.depuysynthes.scripts.MediaBinDeltaVO.State;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.io.FileType;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/****************************************************************************
 * <b>Title</b>: ShowpadDivisionUtil.java<p/>
 * <b>Description: This class represents a Division in Showpad.  Upon init, it load's tags and variables 
 * for the given Division, and keeps those in member vars for when assets come along.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Feb 8, 2016
 ****************************************************************************/
public class ShowpadDivisionUtil {

	protected static Logger log = Logger.getLogger(ShowpadDivisionUtil.class);
	protected Properties props = null;
	private ShowpadApiUtil showpadUtil;
	private Map<String, String> showpadTags = new HashMap<>(1000);
	private Map<String, String> insertTicketQueue = new HashMap<>();
	private Map<String, String> divisionAssets = new HashMap<>(1000);
	private Map<String, String> inserts = new HashMap<>();
	private Map<String, String> updates = new HashMap<>();
	private int dbCount = 0;
	private int deleteCount = 0;
	private String divisionId;
	private String divisionNm;
	private String divisionUrl;
	private Connection dbConn;

	/**
	 * List of errors 
	 */
	protected List <Exception> failures = new ArrayList<>();


	/**
	 * construct a new DivisionUtil based on the given arguments.
	 * These Divisions mirror those in Showpad and are loaded from the script's config file
	 * @param props
	 * @param divisionId
	 * @param divisionNm
	 * @param util
	 * @throws QuotaException
	 */
	public ShowpadDivisionUtil(Properties props, String divisionId, 
			String divisionNm, ShowpadApiUtil util, Connection conn) throws QuotaException {
		this.props = props;
		this.divisionId = divisionId;
		this.divisionNm = divisionNm;
		this.divisionUrl = props.get("showpadApiUrl") + "/divisions/" + divisionId;
		this.showpadUtil = util;
		this.dbConn = conn;

		//get a list of tags already at Showpad, so when we save the assets these are preloaded
		loadShowpadTagList();
	}



	/**
	 * Push a single asset over to Showpad, for this Division.
	 * - Does not push to Showpad if the asset is already there and has no changes to be made.
	 * - Loads asset tags and adds new tags as needed
	 * - Also checks to see if the asset is pre-existing, so we don't create a duplicate.
	 * @param vo
	 * @throws QuotaException
	 */
	public void pushAsset(MediaBinDeltaVO vo) throws QuotaException {
		vo.setShowpadId(divisionAssets.get(vo.getDpySynMediaBinId()));
		String postUrl;
		Map<String, String> params = new HashMap<>();
		FileType fType = new FileType(vo.getFileNm());
		String title = makeShowpadAssetName(vo, fType);
		boolean isShowpadUpdate = (vo.getShowpadId() != null && vo.getShowpadId().length() > 0); 

		if (isShowpadUpdate) {
			//this asset can be ignored if we have it saved and there is no state change
			if (State.Ignore == vo.getRecordState()) {
				log.info("no changes needed to " + vo.getDpySynMediaBinId());
				return;
			}

			//send as an 'update' to Showpad
			postUrl = props.getProperty("showpadApiUrl") + "/assets/" + vo.getShowpadId() + ".json";

		} else {
			//check if this file is already in Showpad before treating it as new
			vo.setShowpadId(findShowpadId(title));

			if (vo.getShowpadId() != null) {
				//if the file is already there, and doesn't need updating, simply move on.
				//first capture it as an insert so we'll have it in our database next time.
				if (State.Ignore == vo.getRecordState()) {
					inserts.put(vo.getDpySynMediaBinId(), vo.getShowpadId());
					log.info("no changes needed to " + vo.getDpySynMediaBinId() + ", adding to the insert roster for our DB.");
					return;
				}

				//do an update instead of an insert
				postUrl = props.getProperty("showpadApiUrl") + "/assets/" + vo.getShowpadId() + ".json";
			} else {
				//send an 'add' to the division for the given asset
				postUrl = divisionUrl + "/assets.json";
			}
		}

		log.info("url=" + postUrl);
		params.put("name", title);
		params.put("resourcetype", getResourceType(fType)); //Showpad Constant for all assets
		params.put("suppress_response_codes","true"); //forces a 200 response header
		params.put("description", vo.getDownloadTypeTxt());
		params.put("isSensitive", "false");
		params.put("isShareable", "true");
		params.put("isDivisionShared", "false");
		params.put("releasedAt", Convert.formatDate(vo.getModifiedDt(), Convert.DATE_TIME_SLASH_PATTERN));

		//add any Link objects (Tags) we need to have attached to this asset
		StringBuilder header = new StringBuilder(200);
		addTags(vo, header);

		log.info("uploading file: " + props.get("downloadDir") + vo.getFileName());
		File mbFile = new File(props.get("downloadDir") + vo.getFileName());
		log.info("sending to showpad: " + vo.getDpySynMediaBinId());
		JSONObject json = null;
		try {
			String resp = showpadUtil.executePostFile(postUrl, params, mbFile, header.toString());
			json = JSONObject.fromObject(resp);
			JSONObject metaResp = json.getJSONObject("meta");
			log.info(json);
			if (!StringUtil.checkVal(metaResp.optString("code")).startsWith("20")) //trap all but a 200 or 201
				throw new IOException(metaResp.optString("message"));

		} catch (IOException e) {
			String msg = DSMediaBinImporterV2.makeMessage(vo, "Could not push file to showpad: " + e.getMessage());
			failures.add(new Exception(msg));
			return;
		}

		//check to see whether a Ticket or Asset got created.  If it's a Ticket, we'll need to re-query after a little while
		//to get the actual AssetID of the asset.  A Ticket means the request was queued, which is common for videos and larger PDFs
		if (!isShowpadUpdate) {
			String assetId = null, ticketId = null;
			JSONObject jsonResp = json.getJSONObject("response");
			if ("Asset".equalsIgnoreCase(jsonResp.optString("resourcetype"))) {
				assetId = jsonResp.optString("id");
			} else if ("Ticket".equalsIgnoreCase(jsonResp.optString("resourcetype"))) {
				//look for an asset
				JSONObject asset =jsonResp.has("asset") ? jsonResp.optJSONObject("asset") : null;
				assetId = (asset != null && !asset.isNullObject()) ? asset.optString("id") : "";

				//if there is no asset, capture the ticketId
				ticketId = jsonResp.getString("id");
			}

			if (assetId == null || assetId.isEmpty()) {
				//queue this one for later
				insertTicketQueue.put(ticketId, vo.getDpySynMediaBinId());
			} else {
				vo.setShowpadId(assetId);
			}
			inserts.put(vo.getDpySynMediaBinId(), assetId);

		} else { //remove the showpadId on updates so we don't create a dup in the SMT database (we need inserts only)
			updates.put(vo.getDpySynMediaBinId(), vo.getShowpadId());
		}
	}


	/**
	 * writes the newly pushed Showpad assets to SMT's database, so next time
	 * we'll have showpadIDs for them and can run update transactions instead of insert.
	 * @param masterRecords
	 */
	public void saveDBRecords() {
		//run the inserts
		StringBuilder sql = new StringBuilder(200);
		sql.append("insert into ").append(props.get(Constants.CUSTOM_DB_SCHEMA)).append("DPY_SYN_SHOWPAD ");
		sql.append("(DIVISION_ID, ASSET_ID, DPY_SYN_MEDIABIN_ID, CREATE_DT) values(?,?,?,?)");
		log.debug(sql);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (String mediabinId : inserts.keySet()) {
				ps.setString(1, divisionId);
				ps.setString(2, inserts.get(mediabinId));
				ps.setString(3, mediabinId);
				ps.setTimestamp(4, Convert.getCurrentTimestamp());
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException sqle) {
			failures.add(sqle);
		}

		//run the updates
		sql = new StringBuilder(200);
		sql.append("update ").append(props.get(Constants.CUSTOM_DB_SCHEMA)).append("DPY_SYN_SHOWPAD ");
		sql.append("set update_dt=? where division_id=? and dpy_syn_mediabin_id=?");
		log.debug(sql);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (String mediabinId : updates.keySet()) {
				ps.setTimestamp(1, Convert.getCurrentTimestamp());
				ps.setString(2, divisionId);
				ps.setString(3, mediabinId);
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException sqle) {
			failures.add(sqle);
		}


	}


	/**
	 * deletes an asset from Showpad.  Note this call sis not division centric, but the assetIds are.
	 * Check to see if 'this' division has the given asset.  If so, fire the delete request
	 * @param vo
	 * @throws QuotaException 
	 */
	public void deleteAsset(MediaBinDeltaVO vo) throws QuotaException {
		String pkId = divisionAssets.get(vo.getDpySynMediaBinId());
		if (pkId == null || pkId.length() == 0) return; //nothing to delete

		//delete using the base /assets/ url, not the division url
		String url = props.getProperty("showpadApiUrl") + "/assets/" + pkId + ".json";
		try {
			String resp = showpadUtil.executeDelete(url);
			log.info("showpad delete response: " + resp);
			++deleteCount;
		} catch (IOException ioe) {
			String msg = DSMediaBinImporterV2.makeMessage(vo, "Could not delete file from showpad: " + ioe.getMessage());
			failures.add(new Exception(msg));
			log.error("could not delete from showpad", ioe);
		}
	}


	/**
	 * removes duplicates from Showpad by looping the list of assets and 
	 * maintaining a list of 'good' assets to keep
	protected void cleanupShowpadDups(Set<String> assetNames, Set<String> localShowpadIds) {
		Map<String, String> showpadAssets = new HashMap<>(5000);

		Set<String> assetNames = new HashSet<>(records.size());
		Set<String> localShowpadIds = new HashSet<>(records.size());
		for (MediaBinDeltaVO vo : records.values()) {
			assetNames.add(makeShowpadAssetName(vo, new FileType(vo.getFileNm())));
			if (vo.getShowpadId() != null) localShowpadIds.add(vo.getShowpadId());
		}

		//NOTE: THIS WILL INCLUDE SHOWPAD ASSETS IN THE TRASH! 
		String tagUrl = divisionUrl + "/assets.json?id=" + divisionId + "&limit=100000&fields=id,name";
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
				JSONObject asset = items.getJSONObject(x);
				String assetNm = asset.getString("name");
				if (showpadAssets.containsKey(assetNm) || assetNm.startsWith(" ")) {
					log.error("dup or blank start, deleting:" + assetNm);
					String url = props.getProperty("showpadApiUrl") + "/assets/" + asset.getString("id") + ".json";
					showpadUtil.executeDelete(url);
//				} else if (!assetNames.contains(assetNm)) {
//					//delete from Showpad - files that shouldn't be there
//					log.info("deleting rogue asset: " + assetNm + " id=" + asset.getString("id"));
//					String url = props.getProperty("showpadApiUrl") + "/assets/" + asset.getString("id") + ".json";
//					showpadUtil.executeDelete(url);
				} else {
					log.info("saving:" + assetNm);
					showpadAssets.put(assetNm, asset.getString("id"));
					localShowpadIds.remove(asset.getString("id"));
				}
			}

		} catch (IOException | NullPointerException ioe) {
			failures.add(ioe);
			log.error("could not load showpad assets", ioe);
		}

		log.info("need to delete " + localShowpadIds.size() + " showpad records");
		for (String s : localShowpadIds)
			System.err.println("'" + s + "',");

		log.info("loaded " + showpadAssets.size() + " showpad assets");
	}
	 **/


	/*************************************************************
	 * 					SHOWPAD TICKET FUNCTIONS
	 *************************************************************/

	/**
	 * runs a loop around the ticket queue checking for status changes.  Returns only
	 * once the queue is empty, which may take some time (on occasation)
	 * @param masterRecords
	 * @throws QuotaException 
	 */
	public void processTicketQueue() throws QuotaException {
		//continue processing the queue until it's empty; meaning Showpad has processed all our assets
		int count = insertTicketQueue.size();
		int runCount = 0;
		while (count > 0) {
			if (runCount > 0) {
				try {
					log.info("sleeping 30 seconds");
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
				}
			}

			Set<String> removes = new HashSet<>();
			for (String ticketId : insertTicketQueue.keySet()) {
				String assetId;
				try {
					assetId = getAssetIdFromTicket(ticketId);
					if (assetId != null) {
						log.info("found assetId=" + assetId + " for ticket=" + ticketId);
						inserts.put(insertTicketQueue.get(ticketId), assetId);
						removes.add(ticketId);
						log.info("finished processing ticket " + ticketId + ", its now assetId=" + assetId);
					}
				} catch (InvalidDataException e) {
					//this asset failed lookup.  or maybe failed adding to Showpad.
					//remove it and set the ID=null, we'll try it again tomorrow.
					inserts.remove(insertTicketQueue.get(ticketId));
					removes.add(ticketId);
				}
			}
			//remove the processed ones from our ticketQueue.
			//this cannot be done above (inline) because of concurrency issues (ConcurrentModificationException)
			for (String t : removes) {
				insertTicketQueue.remove(t);
				--count;
			}
			++runCount;
		}
		log.info("iterated " + runCount + " times waiting for the Showpad queue to empty");
	}


	/**
	 * queries a ticket to check status and capture assetId once complete.
	 * @param ticketId
	 * @return
	 * @throws QuotaException 
	 */
	private String getAssetIdFromTicket(String ticketId) throws InvalidDataException, QuotaException {
		String ticketUrl = props.getProperty("showpadApiUrl") + "/tickets/" + ticketId + ".json?fields=status,asset";
		try {
			String resp = showpadUtil.executeGet(ticketUrl);
			JSONObject json = JSONObject.fromObject(resp);
			log.info(json);
			JSONObject metaResp = json.getJSONObject("meta");
			if (!"200".equals(metaResp.getString("code")))
				throw new IOException(metaResp.getString("message"));

			JSONObject response = json.getJSONObject("response");
			String status = response.optString("status");
			if ("failed".equalsIgnoreCase(status)) throw new InvalidDataException(status);

			JSONObject asset = response.getJSONObject("asset");
			if (asset != null && !asset.isNullObject() && asset.optString("id").length() > 0)
				return asset.getString("id");

			log.info(ticketId + " is not finished yet, status=" + status);

		} catch (IOException | NullPointerException ioe) {
			failures.add(ioe);
			log.error("could not load showpad tags", ioe);
		}

		return null;
	}


	/*************************************************************
	 * 					SHOWPAD ASSET FUNCTIONS
	 *************************************************************/

	/**
	 * this can be used to obtain showpadIds from file names if this script 
	 * fails after uploading the files
	 * @param fileName
	 * @return
	 * @throws QuotaException 
	 */
	private String findShowpadId(String fileName) throws QuotaException {
		String showpadId = null;
		//encode the file Name as a URL parameter, since this is a GET request
		String findUrl = divisionUrl + "/assets.json?fields=id&id=" + divisionId + "&limit=100&name=" + StringEncoder.urlEncode(fileName);
		try {
			String resp = showpadUtil.executeGet(findUrl);
			JSONObject json = JSONObject.fromObject(resp);
			log.info(json);
			JSONObject metaResp = json.getJSONObject("meta");
			if (!"200".equals(metaResp.getString("code")))
				throw new IOException(metaResp.getString("message"));

			JSONObject response = json.getJSONObject("response");
			JSONArray items = response.getJSONArray("items");
			for (int x=0; x < items.size(); x++) {
				JSONObject asset = items.getJSONObject(x);
				//report duplicates
				if (x>0) {
					log.fatal("duplicate found!  delete " + asset.getString("id") + " in favor of " + showpadId);
				} else {
					showpadId = asset.getString("id");
				}
			}

		} catch (IOException | NullPointerException ioe) {
			failures.add(ioe);
			log.error("could not load showpad id from name", ioe);
		}
		return showpadId;
	}


	/**
	 * escapes special chars that Showpad is sensitive to seeing in asset names
	 * @param vo
	 * @param fType
	 * @return
	 */
	private String makeShowpadAssetName(MediaBinDeltaVO vo, FileType fType ) {
		String title = StringUtil.checkVal(vo.getTitleTxt(), vo.getFileNm());
		title += " - " + vo.getTrackingNoTxt() + "." + fType.getFileExtension();
		title = StringUtil.replace(title, "\"", ""); //remove double quotes, which break the JSON structure
		title = StringUtil.replace(title, "/", "-").trim(); //Showpad doesn't like slashes either, which look like directory structures
		return title;
	}


	/*************************************************************
	 * 					SHOWPAD TAG FUNCTIONS
	 *************************************************************/

	/**
	 * Load a list of tags already at Showpad
	 * If we try to add a tag to an asset without using it's ID, and it already existing in the system, it will fail.
	 * @throws QuotaException 
	 */
	private void loadShowpadTagList() throws QuotaException {
		String tagUrl = divisionUrl + "/tags.json?limit=100000&id=" + divisionId + "&fields=id,name";
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
				JSONObject tag = items.getJSONObject(x);
				showpadTags.put(tag.getString("name"), tag.getString("id"));
			}

		} catch (IOException | NullPointerException ioe) {
			failures.add(ioe);
			log.error("could not load showpad tags", ioe);
		}

		log.info("loaded " + showpadTags.size() + " showpad tags: " + showpadTags);
	}


	/**
	 * adds the desired tags to the passed showpad asset
	 * If the desired tag does not exist in Showpad, it must be added (there) first.
	 * @param vo
	 * @throws QuotaException 
	 */
	private void addTags(MediaBinDeltaVO vo, StringBuilder header) throws QuotaException {
		Map<String,String> assignedTags = null;
		if (vo.getShowpadId() != null) assignedTags = loadAssetTags(vo.getShowpadId());
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
			log.info("need tag " + tagNm + ", current id=" + showpadTags.get(tagNm));
			if (showpadTags.get(tagNm) == null) {
				//add it to the global list for the next iteration to leverage
				showpadTags.put(tagNm, createTag(tagNm));
			}

			if (header.length() > 0) header.append(",");
			header.append("<").append(showpadTags.get(tagNm)).append(">; rel=\"Tag\"");
		}
	}


	/**
	 * returns a list of tags already attached to this asset
	 * @param showpadId
	 * @return
	 * @throws QuotaException 
	 */
	private Map<String, String> loadAssetTags(String showpadId) throws QuotaException {
		Map<String,String> tags = new HashMap<>();
		String tagUrl = props.getProperty("showpadApiUrl") + "/assets/" + showpadId + "/tags.json";
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
				JSONObject tag = items.getJSONObject(x);
				showpadTags.put(tag.getString("name"), tag.getString("id"));
			}

		} catch (IOException | NullPointerException ioe) {
			failures.add(ioe);
			log.error("could not load showpad tags", ioe);
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
	protected String createTag(String tagNm) throws QuotaException {
		String tagId = null;
		String tagUrl = divisionUrl + "/tags.json";

		Map<String,String> params = new HashMap<>();
		params.put("name", tagNm);

		try {
			String resp = showpadUtil.executePost(tagUrl, params);
			JSONObject json = JSONObject.fromObject(resp);
			log.info(json);
			JSONObject metaResp = json.getJSONObject("meta");
			if (!"201".equals(metaResp.getString("code")))
				throw new IOException(metaResp.getString("message"));

			JSONObject response = json.getJSONObject("response");
			tagId = response.getString("id");

		} catch (IOException | NullPointerException ioe) {
			failures.add(ioe);
			log.error("could not create showpad tag " + tagNm, ioe);
		}
		
		//if creating the tag succeeded, save it in SMT's database
		if (tagId != null && !tagId.isEmpty())
			createTagInDatabase(tagId, tagNm);

		log.info("created tag " + tagNm + " with id=" + tagId);
		return tagId;
	}
	
	
	/**
	 * Persists tags we create a Showpad into the local database, so we have 
	 * record of what we've created, and therefore are authorized to delete (later).
	 * @param tagId
	 * @param tagNm
	 */
	private void createTagInDatabase(String tagId, String tagNm) {
		StringBuilder sql = new StringBuilder(150);
		sql.append("insert into ").append(props.get(Constants.CUSTOM_DB_SCHEMA)).append("DPY_SYN_SHOWPAD_TAG ");
		sql.append("(DIVISION_ID, TAG_ID, TAG_NM, CREATE_DT) values (?,?,?,?)");
		log.debug(sql);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, divisionId);
			ps.setString(2, tagId);
			ps.setString(3, tagNm);
			ps.setTimestamp(4, Convert.getCurrentTimestamp());
			ps.executeUpdate();
		} catch (SQLException sqle) {
			failures.add(sqle);
			log.error("could not save Showpad Tag to SMT database", sqle);
		}
	}


	/*************************************************************
	 * 					UTILITY FUNCTIONS
	 *************************************************************/

	/**
	 * return a predefined resource type based on the file extention
	 * @param fileName
	 * @return
	 */
	private String getResourceType(FileType fType) {
		switch (StringUtil.checkVal(fType.getFileExtension()).toLowerCase()) {
			case "pdf":
			case "txt":
			case "rtf":
			case "doc":
			case "docx":
			case "xls":
			case "xlsx":
			case "ppt":
			case "pps":
			case "ppsx":
			case "pptx":
				return "document";
			case "m4v":
			case "mp4":
			case "mov":
			case "mpg":
			case "mpeg":
			case "flv":
			case "asf":
			case "3gp":
			case "avi":
			case "wmv":
				return "video";
			case "mp3":
			case "m4a":
			case "wma":
			case "wav":
				return "audio";
			case "jpg":
			case "jpeg":
			case "gif":
			case "png":
			case "tiff": 
				return "image";

			default: return "asset";
		}
	}

	public String getDivisionId() {
		return divisionId;
	}

	public String getDivisionNm() {
		return divisionNm;
	}

	public List<Exception> getFailures() {
		return failures;
	}

	public void setDivisionAssets(Map<String, String> divisionAssets) {
		this.divisionAssets = divisionAssets;
	}

	public int getDbCount() {
		return dbCount;
	}

	public void setDbCount(int dbCount) {
		this.dbCount = dbCount;
	}

	public int getInsertCount() {
		return inserts.size();
	}
	public int getUpdateCount() {
		return updates.size();
	}
	public int getDeleteCount() {
		return deleteCount;
	}
}
