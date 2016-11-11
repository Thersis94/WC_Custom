package com.depuysynthes.scripts.showpad;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import com.depuysynthes.scripts.DSMediaBinImporterV2;
import com.depuysynthes.scripts.MediaBinDeltaVO;
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
	
	/*
	 * a string constant we record for items that fail to add to Showpad - 
	 * this enables us to 
	 */
	protected static final String FAILED_PROCESSING = "FAILED_PROCESSING";

	protected static Logger log = Logger.getLogger(ShowpadDivisionUtil.class);
	protected Properties props = null;
	private ShowpadApiUtil showpadUtil;
	private Map<String, String> insertTicketQueue = new HashMap<>();
	private Map<String, String> divisionAssets = new HashMap<>(8000);
	private Map<String, String> inserts = new HashMap<>();
	private Map<String, String> updates = new HashMap<>();
	private int dbCount = 0;
	private int deleteCount = 0;
	private String divisionId;
	private String divisionNm;
	private String divisionUrl;
	private String showpadApiUrl;
	private Connection dbConn;
	
	protected ShowpadTagManager tagMgr;

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
		this.showpadApiUrl = (String)props.get("showpadApiUrl");
		this.divisionUrl = showpadApiUrl + "/divisions/" + divisionId;
		this.showpadUtil = util;
		this.dbConn = conn;

		//get a list of tags already at Showpad, so when we save the assets these are preloaded
		tagMgr = new ShowpadTagManager(showpadApiUrl, divisionId, divisionUrl, showpadUtil);
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
		boolean isShowpadUpdate = !StringUtil.isEmpty(vo.getShowpadId()); 

		//this asset can be ignored if we have it saved and there is no state change
		if (isShowpadUpdate && State.Ignore == vo.getRecordState()) {
			log.info("no changes needed to " + vo.getDpySynMediaBinId());
			return;
			
		} else if (isShowpadUpdate && !FAILED_PROCESSING.equals(vo.getShowpadId())) {
			//this record needs to be updated, and didn't previous fail to add to Showpad.
			//failures need to be treated as Adds (below), meaning we'll try to add them as new even though they failed last time.

			//send as an 'update' to Showpad
			postUrl = showpadApiUrl + "/assets/" + vo.getShowpadId() + ".json";

		} else {
			//check if this file is already in Showpad before treating it as new
			vo.setShowpadId(findShowpadId(title));

			if (!StringUtil.isEmpty(vo.getShowpadId())) {
				//if the file is already there, and doesn't need updating, simply move on.
				//first capture it as an insert so we'll have it in our database next time.
				if (State.Ignore == vo.getRecordState()) {
					inserts.put(vo.getDpySynMediaBinId(), vo.getShowpadId());
					log.info("no changes needed to " + vo.getDpySynMediaBinId() + ", adding to the insert roster for our DB.");
					return;
				}

				//do an update instead of an insert
				postUrl = showpadApiUrl + "/assets/" + vo.getShowpadId() + ".json";
				isShowpadUpdate = true;
			} else {
				//send an 'add' to the division for the given asset
				postUrl = divisionUrl + "/assets.json";
			}
		}

		log.info("url=" + postUrl);
		params.put("name", title);
		params.put("resourcetype", ShowpadResourceType.getResourceType(fType)); //Showpad Constant for all assets
		params.put("suppress_response_codes","true"); //forces a 200 response header
		params.put("description", vo.getDownloadTypeTxt());
		params.put("isSensitive", "false");
		params.put("isShareable", "true");
		params.put("isDivisionShared", "false");
		params.put("releasedAt", Convert.formatDate(vo.getModifiedDt(), Convert.DATE_TIME_SLASH_PATTERN));

		//add any Link objects (Tags) we need to have attached to this asset
		StringBuilder header = new StringBuilder(200);
		try {
			tagMgr.addTags(vo, header);
		} catch (InvalidDataException e1) {
			failures.add(e1);
			log.warn("asset not found on Showpad: " + vo.getDpySynMediaBinId());
		}

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
		insertRecords();

		//run the updates
		updateRecords();
	}
	
	
	/**
	 * runs SQL insert queries for the records we're adding
	 */
	private void insertRecords() {
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
	}
	
	
	/**
	 * runs SQL update queries for the records we're updating
	 */
	private void updateRecords() {
		StringBuilder sql = new StringBuilder(200);
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
		if (pkId == null || pkId.isEmpty()) return; //nothing to delete

		//delete using the base /assets/ url, not the division url
		String url = showpadApiUrl + "/assets/" + pkId + ".json";
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
	 * @throws QuotaException 
	 */ 
	protected void cleanupShowpadDups(Set<String> assetNames, Set<String> localShowpadIds) 
			throws QuotaException {
		Map<String, String> showpadAssets = new HashMap<>(5000);
		
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
					String url = showpadApiUrl + "/assets/" + asset.getString("id") + ".json";
					showpadUtil.executeDelete(url);
				/*
				 * This would purge files that are not ours to delete; never run against the production J&J Account.
				 	} else if (!assetNames.contains(assetNm)) {
					//delete from Showpad - files that shouldn't be there
					log.info("deleting rogue asset: " + assetNm + " id=" + asset.getString("id"));
					String url = showpadApiUrl + "/assets/" + asset.getString("id") + ".json";
					showpadUtil.executeDelete(url);
				 */
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

		log.info("need to delete " + localShowpadIds.size() + " showpad records from local SQL database:");
		for (String s : localShowpadIds)
			System.err.println("'" + s + "',"); //intentional use here; log redirection so we can copy/paste w/o the log4J garb.

		log.info("loaded " + showpadAssets.size() + " showpad assets");
	}


	/*************************************************************
	 * 					SHOWPAD TICKET FUNCTIONS
	 *************************************************************/

	/**
	 * runs a loop around the ticket queue checking for status changes.  Returns only
	 * once the queue is empty, which may take some time (on occasation)
	 * @param masterRecords
	 * @throws QuotaException 
	 */
	public void processTicketQueue(Map<String, MediaBinDeltaVO> masterRecords) throws QuotaException {
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
			Set<String> removes = testForCompletion(masterRecords);
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
	 * queries Showpad for status changes for our queued assets.
	 * @return
	 * @throws QuotaException
	 */
	private Set<String> testForCompletion(Map<String, MediaBinDeltaVO> masterRecords) throws QuotaException {
		Set<String> removes = new HashSet<>();
		for (Map.Entry<String,String> row : insertTicketQueue.entrySet()) {
			String assetId;
			String ticketId = row.getKey();
			try {
				assetId = getAssetIdFromTicket(ticketId);
				if (assetId != null) {
					log.info("found assetId=" + assetId + " for ticket=" + ticketId);
					inserts.put(insertTicketQueue.get(ticketId), assetId);
					//set the assetId onto the master record for downstream processing to use
					masterRecords.get(insertTicketQueue.get(ticketId)).setShowpadId(assetId);
					
					removes.add(ticketId);
					log.info("finished processing ticket " + ticketId + ", its now assetId=" + assetId);
				}
			} catch (InvalidDataException e) {
				//this asset failed lookup.  or maybe failed adding to Showpad.
				//remove it and set the ID=null, we'll try it again tomorrow.
				inserts.remove(row.getValue());
				removes.add(ticketId);
			}
		}
		return removes;
	}


	/**
	 * queries a ticket to check status and capture assetId once complete.
	 * @param ticketId
	 * @return
	 * @throws QuotaException 
	 */
	private String getAssetIdFromTicket(String ticketId) throws InvalidDataException, QuotaException {
		String ticketUrl = showpadApiUrl + "/tickets/" + ticketId + ".json?fields=status,asset";
		try {
			String resp = showpadUtil.executeGet(ticketUrl);
			JSONObject json = JSONObject.fromObject(resp);
			log.info(json);
			JSONObject metaResp = json.getJSONObject("meta");
			if (!"200".equals(metaResp.getString("code")))
				throw new IOException(metaResp.getString("message"));

			JSONObject response = json.getJSONObject("response");
			String status = response.optString("status");
			if ("failed".equalsIgnoreCase(status)) return FAILED_PROCESSING;

			JSONObject asset = response.getJSONObject("asset");
			if (asset != null && !asset.isNullObject() && !StringUtil.isEmpty(asset.optString("id")))
				return asset.getString("id");

			log.info(ticketId + " is not finished yet, status=" + status);

		} catch (IOException | NullPointerException ioe) {
			failures.add(ioe);
			log.error("could not load showpad assetId from queue ticket", ioe);
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
	protected static String makeShowpadAssetName(MediaBinDeltaVO vo, FileType fType ) {
		String title = StringUtil.checkVal(vo.getTitleTxt(), vo.getFileNm());
		title += " - " + vo.getTrackingNoTxt() + "." + fType.getFileExtension();
		title = StringUtil.replace(title, "\"", ""); //remove double quotes, which break the JSON structure
		title = StringUtil.replace(title, "/", "-").trim(); //Showpad doesn't like slashes either, which look like directory structures
		return title;
	}

	
	/*************************************************************
	 * 					UTILITY FUNCTIONS
	 *************************************************************/
	public String getDivisionId() {
		return divisionId;
	}

	public String getDivisionNm() {
		return divisionNm;
	}

	public List<Exception> getFailures() {
		if (tagMgr != null) failures.addAll(tagMgr.getFailures()); //include failures from the tagMgr in our report. 
		return failures;
	}

	public void setDivisionAssets(Map<String, String> divisionAssets) {
		this.divisionAssets = divisionAssets;
	}
	
	public Map<String, String> getDivisionAssets() {
		return divisionAssets;
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
	public ShowpadTagManager getTagManager() {
		return tagMgr;
	}
}