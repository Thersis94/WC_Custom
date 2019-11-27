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
import com.depuysynthes.scripts.DSPrivateAssetsImporter;
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
	protected ShowpadApiUtil showpadUtil;
	protected Map<String, String> insertTicketQueue = new HashMap<>();
	protected Map<String, String> divisionAssets = new HashMap<>(8000);
	protected Map<String, String> inserts = new HashMap<>();
	protected Map<String, String> updates = new HashMap<>();
	protected int dbCount;
	protected int deleteCount;
	protected int failCount;
	protected String divisionId;
	protected String divisionNm;
	protected String divisionUrl;
	protected String showpadApiUrl;
	protected Connection dbConn;

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
	public ShowpadDivisionUtil(Properties props, String divisionId, String divisionNm, ShowpadApiUtil util, Connection conn) {
		this.props = props;
		this.divisionId = divisionId;
		this.divisionNm = divisionNm;
		this.showpadApiUrl = (String)props.get("showpadApiUrl");
		this.divisionUrl = showpadApiUrl + "/divisions/" + divisionId;
		this.showpadUtil = util;
		this.dbConn = conn;
		createTagManager();
	}


	/**
	 * construct a new DivisionUtil based on the given arguments - and an override for the static asset Tag.
	 * These Divisions mirror those in Showpad and are loaded from the script's config file
	 * @param props
	 * @param divisionId
	 * @param divisionNm
	 * @param util
	 * @throws QuotaException
	 */
	public ShowpadDivisionUtil(Properties props, String divisionId, String divisionNm, ShowpadApiUtil util, 
			Connection conn, String sourceTag) {
		this(props, divisionId, divisionNm, util, conn);
		tagMgr.setSourceConstant(sourceTag);
	}


	/**
	 * get a list of tags already at Showpad, so when we save the assets these are preloaded
	 */
	protected void createTagManager() {
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
	public void pushAsset(MediaBinDeltaVO vo) {
		vo.setShowpadId(divisionAssets.get(vo.getDpySynMediaBinId()));
		String postUrl;
		FileType fType = new FileType(vo.getFileNm());
		String title = makeShowpadAssetName(vo);
		boolean isUpdate = !StringUtil.isEmpty(vo.getShowpadId()); 

		//this asset can be ignored if we have it saved and there is no state change
		if (isUpdate && State.Ignore == vo.getRecordState()) {
			log.info("no changes needed to " + vo.getDpySynMediaBinId());
			return;

		} else if (isUpdate && !FAILED_PROCESSING.equals(vo.getShowpadId())) {
			//this record needs to be updated, and didn't previous fail to add to Showpad.
			//failures need to be treated as Adds (below), meaning we'll try to add them as new because they failed last time (they don't exist).

			//send as an 'update' to Showpad
			postUrl = showpadApiUrl + "/assets/" + vo.getShowpadId() + ".json";

		} else {
			//check if this file is already in Showpad before treating it as new
			//showpad puts file extension on for us when we add assets, but we need to use it when searching by name
			vo.setShowpadId(findShowpadId(title + "." + fType.getFileExtension()));

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
				isUpdate = true;
			} else {
				//send an 'add' to the division for the given asset
				postUrl = divisionUrl + "/assets.json";
			}
		}

		pushToShowpad(isUpdate, postUrl, title, fType, vo);
	}


	/**
	 * pushes the asset to the API Util - called from above 'pushAsset' method
	 * Note: Always send 'dates' to the API in Seconds -JM- 08/21/19 (per Showpad support)
	 * @param postUrl
	 * @param title
	 * @param fType
	 * @param vo
	 * @throws QuotaException
	 */
	protected void pushToShowpad(boolean isUpdate, String postUrl, String title, FileType fType, MediaBinDeltaVO vo) {
		Map<String, String> params = new HashMap<>();
		log.info("url=" + postUrl);
		params.put("name", title);
		params.put("resourcetype", ShowpadResourceType.getResourceType(fType)); //Showpad Constant for all assets
		params.put("suppress_response_codes","true"); //forces a 200 response header
		params.put("isDivisionShared", "false");

		//distinguish some values for EMEA private assets only - trigger off the tag passed from that script marking them as 'internal'
		if (DSPrivateAssetsImporter.INTERNAL_TAG.equals(tagMgr.getSourceConstant())) {
			params.put("isSensitive", "true");
			params.put("isShareable", "false");
			params.put("isDownloadable", "false");
		} else {
			params.put("isSensitive", "false");
			params.put("isShareable", "true");
			params.put("isDownloadable", "true");
		}

		if (vo.getExpirationDt() != null) {
			params.put("expiresAt", Long.toString(vo.getExpirationDt().getTime()/1000));
		} else {
			//important to pass null here to flush any values that may exist upstream. (we have no way of knowing)
			params.put("expiresAt", "null");
		}

		if (vo.getDownloadTypeTxt() != null)
			params.put("description", vo.getDownloadTypeTxt());

		//add any Link objects (Tags) we need to have attached to this asset
		StringBuilder header = new StringBuilder(200);
		try {
			tagMgr.addTags(vo, header);
		} catch (InvalidDataException e1) {
			failures.add(e1);
			log.warn("asset not found on Showpad: " + vo.getDpySynMediaBinId());
		}

		File mbFile = null;
		if (vo.isFileChanged() || StringUtil.isEmpty(vo.getShowpadId()))
			mbFile = new File(vo.getFileName());

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

		readShowpadResponse(isUpdate, vo, json);
	}


	/**
	 * reads the data returned from the API to determine if we successfully updated or created an asset, or if it
	 * was queued for processing.  Queued requests ebcome tickets - we'll check on them later and pause until they're processed.
	 * @param isShowpadUpdate
	 * @param vo
	 * @param json
	 */
	protected void readShowpadResponse(boolean isShowpadUpdate, MediaBinDeltaVO vo, JSONObject json) {
		//check to see whether a Ticket or Asset got created.  If it's a Ticket, we'll need to re-query after a little while
		//to get the actual AssetID of the asset.  A Ticket means the request was queued, which is common for videos and larger PDFs
		if (!isShowpadUpdate) {
			String assetId = null;
			String ticketId = null;
			JSONObject jsonResp = json.getJSONObject("response");
			String type = jsonResp.optString("resourcetype");
			if ("Asset".equalsIgnoreCase(type)) {
				assetId = jsonResp.optString("id");
			} else if ("Ticket".equalsIgnoreCase(type)) {
				//look for an asset
				JSONObject asset =jsonResp.has("asset") ? jsonResp.optJSONObject("asset") : null;
				assetId = (asset != null && !asset.isNullObject()) ? asset.optString("id") : "";

				//if there is no asset, capture the ticketId
				ticketId = jsonResp.getString("id");
			}

			if (StringUtil.isEmpty(assetId)) {
				//queue this one for later
				insertTicketQueue.put(ticketId, vo.getDpySynMediaBinId());
			} else {
				vo.setShowpadId(assetId);
			}
			inserts.put(vo.getDpySynMediaBinId(), assetId);

		} else { //remove the showpadId on updates so we don't create a dup in the SMT database (we need inserts only)
			updates.put(vo.getDpySynMediaBinId(), vo.getShowpadId());
		}

		//if we don't have this Showpad ID captured yet, do so now.
		if (!StringUtil.isEmpty(vo.getShowpadId()) && StringUtil.isEmpty(divisionAssets.get(vo.getDpySynMediaBinId())))
			divisionAssets.put(vo.getDpySynMediaBinId(), vo.getShowpadId());
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
			for (Map.Entry<String, String> entry : inserts.entrySet()) {
				ps.setString(1, divisionId);
				ps.setString(2, entry.getValue());
				ps.setString(3, entry.getKey());
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
	public void deleteAsset(MediaBinDeltaVO vo) {
		String pkId = divisionAssets.get(vo.getDpySynMediaBinId());
		if (StringUtil.isEmpty(pkId) || FAILED_PROCESSING.equals(pkId)) return; //nothing to delete

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


	/*************************************************************
	 * 					SHOWPAD TICKET FUNCTIONS
	 *************************************************************/

	/**
	 * runs a loop around the ticket queue checking for status changes.  Returns only
	 * once the queue is empty, which may take some time (on occasation)
	 * @param masterRecords
	 * @throws QuotaException 
	 */
	public void processTicketQueue() {
		//continue processing the queue until it's empty; meaning Showpad has processed all our assets
		int count = insertTicketQueue.size();
		int runCount = 0;
		while (count > 0) {
			if (runCount > 0) {
				try {
					log.info("sleeping 30 seconds");
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					log.fatal("could not put thread to sleep", e);
					Thread.currentThread().interrupt();
					break;
				}
			}
			Set<String> removes = testForCompletion();
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
	private Set<String> testForCompletion() {
		Set<String> removes = new HashSet<>();
		for (Map.Entry<String,String> row : insertTicketQueue.entrySet()) {
			String assetId;
			String ticketId = row.getKey();
			try {
				assetId = getAssetIdFromTicket(ticketId);
				if (assetId != null) {
					log.info("found assetId=" + assetId + " for ticket=" + ticketId);
					inserts.put(insertTicketQueue.get(ticketId), assetId);
					//set the assetId onto the mapping of division assets
					divisionAssets.put(insertTicketQueue.get(ticketId), assetId);

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
	private String getAssetIdFromTicket(String ticketId) throws InvalidDataException {
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
	protected String findShowpadId(String fileName) {
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
					log.warn("duplicate found!  delete " + asset.getString("id") + " in favor of " + showpadId);
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
	 * loads all the assets for this Division - used by the ReconcileReport
	 * @return
	 */
	protected Map<String, MediaBinDeltaVO> getAllAssets() {
		Map<String, MediaBinDeltaVO> assets = new HashMap<>(8000);

		int fetchSize = 1000;
		int offset=0;
		do {
			loadAssets(fetchSize, offset, assets);
			offset += fetchSize;
			//if we've retrieve less than the maximum amount of tags, we're done.  If the #s are equal we need to iterate.
		} while (assets.size() == offset);

		//remove any that are status=deleted...they came from the trash!
		List<MediaBinDeltaVO> vos = new ArrayList<>(assets.values());
		for (MediaBinDeltaVO vo : vos) {
			if (State.ShowpadTrash == vo.getRecordState())
				assets.remove(vo.getShowpadId());
		}

		log.info("loaded " + assets.size() + " showpad assets");
		return assets;
	}


	/**
	 * loads a list of tags for the given division.  Takes into consideration the range limit (1000) and offset (for repeated calls).
	 * @param limit
	 * @param offset
	 * @throws QuotaException
	 */
	protected void loadAssets(int limit, int offset, Map<String, MediaBinDeltaVO> assets) {
		String tagUrl = divisionUrl + "/assets.json?limit=" + limit + "&fields=id,name,archivedAt&offset=" + offset;
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
				JSONObject item = items.getJSONObject(x);
				MediaBinDeltaVO vo = new MediaBinDeltaVO(item);

				//if archivedAt is not empty, it means this item is in the trash.  Tag it so we can toss it out.
				if (!StringUtil.isEmpty(item.optString("archivedAt")) && !"null".equalsIgnoreCase(item.optString("archivedAt")))
					vo.setRecordState(State.ShowpadTrash);

				assets.put(vo.getShowpadId(), vo);
			}

		} catch (IOException | NullPointerException ioe) {
			failures.add(ioe);
			log.error("could not load showpad tags", ioe);
		}
	}


	/**
	 * escapes special chars that Showpad is sensitive to seeing in asset names
	 * @param vo
	 * @param fType
	 * @return
	 */
	protected String makeShowpadAssetName(MediaBinDeltaVO vo) {
		StringBuilder name = new StringBuilder(100);
		//start with title
		String title = vo.getTitleTxt();
		if (!StringUtil.isEmpty(title)) {
			title = StringUtil.replace(title, "/", "-").trim(); //Showpad doesn't like slashes, which look like directory structures
			title = StringUtil.replace(title, "\"", ""); //remove double quotes, which break the JSON structure
			name.append(title).append(" - ");
		}

		//append "- INTERNAL" to internal assets only
		if (DSPrivateAssetsImporter.INTERNAL_TAG.equals(tagMgr.getSourceConstant())) {
			name.append("INTERNAL - ");
		}

		//add tracking number
		String trackingNo = vo.getTrackingNoTxt();
		if (!StringUtil.isEmpty(trackingNo)) {
			//remove all non-alphanumerics
			trackingNo = StringUtil.removeNonAlphaNumeric(trackingNo);
			//remove LR, low, high keywords
			trackingNo = trackingNo.replaceAll("LR", "");
			trackingNo = trackingNo.replaceAll("low", "");
			trackingNo = trackingNo.replaceAll("high", "");
			name.append(trackingNo);
		}

		log.debug("title: " + name);
		return name.toString(); 
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

	public ShowpadTagManager getTagManager() {
		return tagMgr;
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
	public int getFailCount() {
		return failCount;
	}
	public void setFailCount(int failCount) {
		this.failCount = failCount;
	}
}