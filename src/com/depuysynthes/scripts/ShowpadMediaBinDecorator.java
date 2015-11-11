package com.depuysynthes.scripts;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.depuysynthes.scripts.MediaBinDeltaVO.State;
import com.siliconmtn.io.FileType;
import com.siliconmtn.security.OAuth2TokenViaCLI;
import com.siliconmtn.security.OAuth2TokenViaCLI.Config;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: ShowpadMediaBinDecorator.java<p/>
 * <b>Description: Extends the MediaBin importer to record and push assets to Showpad at the same time.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Oct 30, 2015
 ****************************************************************************/
public class ShowpadMediaBinDecorator extends DSMediaBinImporterV2 {

	private ShowpadApiUtil showpadUtil;
	private Map<String, String> showpadTags = new HashMap<>(500);
	private Map<String, String> ticketQueue = new HashMap<>();

	/**
	 * @param args
	 * @throws IOException 
	 */
	public ShowpadMediaBinDecorator(String[] args) throws IOException {
		super(args);

		//setup the oAuth util now that the config file has been loaded
		showpadUtil = new ShowpadApiUtil(new OAuth2TokenViaCLI(new HashMap<Config, String>(){
			private static final long serialVersionUID = -8625615784451892590L;
			{
				put(Config.USER_ID, props.getProperty("showpadAcctName"));
				put(Config.API_KEY, props.getProperty("showpadApiKey"));
				put(Config.API_SECRET, props.getProperty("showpadApiSecret"));
				put(Config.TOKEN_CALLBACK_URL, props.getProperty("showpadCallbackUrl"));
				put(Config.TOKEN_SERVER_URL, props.getProperty("showpadTokenUrl"));
				put(Config.AUTH_SERVER_URL,  props.getProperty("showpadAuthUrl"));
				put(Config.KEYSTORE, "showpad");
			}}, Arrays.asList(props.getProperty("showpadScopes").split(","))));
	}

	public static void main(String[] args) throws Exception {
		//Create an instance of the MedianBinImporter
		ShowpadMediaBinDecorator dmb = new ShowpadMediaBinDecorator(args);
		dmb.run();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		dataCounts.put("showpad", 1); //used as a boolean in the report email to print showpad stats

		//get a list of tags already at Showpad, so when we save the assets these are preloaded
		loadShowpadTagList();

		super.run();
	}

	

	/**
	 * Load a list of tags already at Showpad
	 * If we try to add a tag to an asset without using it's ID, and it already existing in the system, it will fail.
	 */
	private void loadShowpadTagList() {
		String tagUrl = props.getProperty("showpadApiUrl") + "/tags.json?limit=100000&fields=id,name";
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
	 * override the saveRecords method to push the records to Showpad after 
	 * super.saveRecords() saves them to the database.
	 */
	public void saveRecords(Map<String, MediaBinDeltaVO> masterRecords, boolean isInsert) {
		super.saveRecords(masterRecords, isInsert);

		//confirm we have something to add or update
		if (getDataCount((isInsert ? "inserted" : "updated")) == 0) return;

		//the below logic will process both inserts & updates at once.  
		//Block here for updates so we don't process the records twice.
		//Insert runs after deletes & updates, so wait for the 'inserts' invocation so 
		//all the mediabin records are already in our database.
		if (!isInsert) return;

		String postUrl;
		int cnt = 0;

		//push all changes to Showpad
		for (MediaBinDeltaVO vo : masterRecords.values()) {
			//we need to sort out what gets pushed to Showpad on our own.
			//if it's failed, being deleted, or unchanged and already in Showpad, skip it.
			State s = vo.getRecordState();
			if (s == State.Failed || s == State.Delete ||  (s == State.Ignore && vo.getShowpadId() != null))
				continue;

			boolean isShowpadUpdate = (vo.getShowpadId() != null && vo.getShowpadId().length() > 0); 

			if (isShowpadUpdate) { //send as an 'update' to Showpad
				postUrl = props.getProperty("showpadApiUrl") + "/assets/" + vo.getShowpadId() + ".json";
			} else {
				postUrl = props.getProperty("showpadApiUrl") + "/assets.json";
			}

			Map<String, String> params = new HashMap<>();
			String title = StringUtil.checkVal(vo.getTitleTxt(), vo.getFileNm());
			title += " - " + vo.getTrackingNoTxt();
			params.put("name", title);
			params.put("resourcetype", getResourceType(vo.getFileNm())); //Showpad Constant for all assets
			params.put("suppress_response_codes","true"); //forces a 200 response header
			params.put("description", vo.getDownloadTypeTxt());
			params.put("isSensitive", "false");
			params.put("isShareable", "true");
			params.put("releasedAt", Convert.formatDate(vo.getLastUpdate(), Convert.DATE_TIME_SLASH_PATTERN));

			//add any Link objects (Tags) we need to have attached to this asset
			//TODO make header attachments, not form.
			String linkHeader = generateTags(vo, params);

			try {
				File mbFile = new File(props.get("downloadDir") + vo.getFileName());
				log.info("sending to showpad: " + vo.getDpySynMediaBinId());
				String resp = showpadUtil.executePostFile(postUrl, params, mbFile, linkHeader);
				JSONObject json = JSONObject.fromObject(resp);
				JSONObject metaResp = json.getJSONObject("meta");
				log.info(json);
				if (!StringUtil.checkVal(metaResp.optString("code")).startsWith("20")) //trap all but a 200 or 201
					throw new IOException(metaResp.optString("message"));

				//check to see whether a Ticket or Asset got created.  If it's a Ticket, we'll need to re-query after a little while
				//to get the actual AssetID of the asset.  A Ticket means the request was queued, which is common for videos and larger PDFs
				//determine if we need the ID, and move on
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
					
					if (assetId.length() == 0) {
						//queue this one for later
						ticketQueue.put(ticketId, vo.getDpySynMediaBinId());
					} else {
						vo.setShowpadId(assetId);
					}
				} else { //remove the showpadId on updates so we don't create a dup in the SMT database (we need inserts only)
					vo.setShowpadId(null);
				}

				++cnt;
				//TODO remove this development limiter
				if (cnt == 10) break;

			} catch (Exception ioe) {
				vo.setShowpadId(null);
				String msg = makeMessage(vo, "Could not push file to showpad: " + ioe.getMessage());
				failures.add(new Exception(msg));
				log.error("could not push file to showpad", ioe);
			}

			log.info("completed: " + vo.getFileNm());
		}
		
		//process the ticket queue
		processTicketQueue(masterRecords);

		//save the newly created records to our database
		insertNewRecords(masterRecords);

		dataCounts.put("showpad-" + (isInsert ? "inserted" : "updated"), cnt);
	}
	
	
	
	/**
	 * runs a loop around the ticket queue checking for status changes.  Returns only
	 * once the queue is empty, which may take some time (on occasation)
	 * @param masterRecords
	 */
	private void processTicketQueue(Map<String, MediaBinDeltaVO> masterRecords) {
		//continue processing the queue until it's empty; meaning Showpad has processed all our assets
		while (ticketQueue.size() > 0) {
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				break;
			}
			
			for (String ticketId : ticketQueue.keySet()) {
				String assetId = getAssetIdFromTicket(ticketId);
				if (assetId != null) {
					masterRecords.get(ticketQueue.get(ticketId)).setShowpadId(assetId);
					ticketQueue.remove(ticketId);
					log.info("finished processing ticket " + ticketId + ", its now assetId=" + assetId);
				}
			}
			
			if (ticketQueue.size() == 0) break;
		}
	}
	
	
	
	/**
	 * queries a ticket to check status and capture assetId once complete.
	 * @param ticketId
	 * @return
	 */
	private String getAssetIdFromTicket(String ticketId) {
		String ticketUrl = props.getProperty("showpadApiUrl") + "/tickets/" + ticketId + ".json?fields=status,asset";
		try {
			String resp = showpadUtil.executeGet(ticketUrl);
			JSONObject json = JSONObject.fromObject(resp);
			log.info(json);
			JSONObject metaResp = json.getJSONObject("meta");
			if (!"200".equals(metaResp.getString("code")))
				throw new IOException(metaResp.getString("message"));

			JSONObject response = json.getJSONObject("response");
			JSONObject asset = response.getJSONObject("asset");
			if (asset != null && !asset.isNullObject() && asset.optString("id").length() > 0)
				return asset.getString("id");
			
			log.info(ticketId + " is not finished yet, status=" + response.optString("status"));

		} catch (IOException | NullPointerException ioe) {
			failures.add(ioe);
			log.error("could not load showpad tags", ioe);
		}

		return null;
	}

	

	/**
	 * adds the desired tags to the passed showpad asset
	 * If the desired tag does not exist in Showpad, it must be added (there) first.
	 * @param vo
	 */
	private String generateTags(MediaBinDeltaVO vo, Map<String, String> params) {
		Map<String,String> assignedTags = null;
		if (State.Update == vo.getRecordState()) assignedTags = loadAssetTags(vo.getShowpadId());
		Set<String> desiredTags = new HashSet<>();

		//assign the tags this asset SHOULD have, attempt to backfill those from the known list of tags already in Showpad
		FileType ft = new FileType(vo.getFileNm());
		desiredTags.add(ft.getFileExtension());
		if (vo.getBodyRegionTxt() != null && vo.getBodyRegionTxt().length() > 0)
			desiredTags.addAll(Arrays.asList(vo.getBodyRegionTxt().split(MB_TOKENIZER)));
		if (vo.getBusinessUnitNm() != null && vo.getBusinessUnitNm().length() > 0)
			desiredTags.addAll(Arrays.asList(vo.getBusinessUnitNm().split(MB_TOKENIZER)));
		if (vo.getLiteratureTypeTxt() != null && vo.getLiteratureTypeTxt().length() > 0)
			desiredTags.addAll(Arrays.asList(vo.getLiteratureTypeTxt().split(MB_TOKENIZER)));
		if (vo.getProdFamilyNm() != null && vo.getProdFamilyNm().length() > 0)
			desiredTags.addAll(Arrays.asList(vo.getProdFamilyNm().split(MB_TOKENIZER)));
		if (vo.getProdNm() != null && vo.getProdNm().length() > 0)
			desiredTags.addAll(Arrays.asList(vo.getProdNm().split(MB_TOKENIZER)));

		//loop the tags the asset already has, removing them from the "need to add" list
		if (assignedTags != null) {
			for (String tag : assignedTags.keySet())
				desiredTags.remove(tag);
		}

		//add what's left on the "need to add" list as new tags; both to the Asset, and to Showpad if they're new
		StringBuilder tagHeader = new StringBuilder();
		for (String tagNm : desiredTags) {
			if (tagNm == null || tagNm.length() == 0) continue;
			log.info("need tag " + tagNm + ", current id=" + showpadTags.get(tagNm));
			if (showpadTags.get(tagNm) == null) {
				//add it to the global list for the next iteration to leverage
				showpadTags.put(tagNm, createTag(tagNm));
			}

			if (tagHeader.length() > 0) tagHeader.append(",");
			tagHeader.append(showpadTags.get(tagNm)).append(";rel=\"Tag\"");
		}
		return tagHeader.toString();
	}



	/**
	 * creates a new tag within Showpad
	 * returns the ID of the newly minted tag.
	 * @param showpadId
	 * @param tagNm
	 * @param tagId
	 */
	private String createTag(String tagNm) {
		String tagId = null;
		String tagUrl = props.getProperty("showpadApiUrl") + "/tags.json";

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

		log.info("created tag " + tagNm + " with id=" + tagId);
		return tagId;
	}


	/**
	 * returns a list of tags already attached to this asset
	 * @param showpadId
	 * @return
	 */
	private Map<String, String> loadAssetTags(String showpadId) {
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
	 * writes the newly pushed Showpad assets to SMT's database, so next time
	 * we'll have showpadIDs for them and can run update transactions instead of insert.
	 * @param masterRecords
	 */
	private void insertNewRecords(Map<String, MediaBinDeltaVO> masterRecords) {
		StringBuilder sql = new StringBuilder(200);
		sql.append("insert into ").append(props.get(Constants.CUSTOM_DB_SCHEMA)).append("DPY_SYN_SHOWPAD ");
		sql.append("(DPY_SYN_SHOWPAD_ID, DPY_SYN_MEDIABIN_ID, CREATE_DT) values(?,?,?)");
		log.debug(sql);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (MediaBinDeltaVO vo : masterRecords.values()) {
				if (vo.getShowpadId() == null || vo.getShowpadId().length() == 0) continue;
				ps.setString(1, vo.getShowpadId());
				ps.setString(2, vo.getDpySynMediaBinId());
				ps.setTimestamp(3, Convert.getCurrentTimestamp());
				ps.addBatch();
			}
			ps.executeBatch();

		} catch (SQLException sqle) {
			failures.add(sqle);
		}
	}


	/**
	 * return a predefined resource type based on the file extention
	 * @param fileName
	 * @return
	 */
	private String getResourceType(String fileName) {
		FileType fType = new FileType(fileName);
		switch (fType.getFileExtension()) {
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


	/**
	 * overloaded to include the showpad DB table.
	 * @param type
	 * @return
	 */
	protected Map<String,MediaBinDeltaVO> loadManifest() {
		Map<String,MediaBinDeltaVO> data = new HashMap<>(7000); //at time of writing, this was enough capacity to avoid resizing

		StringBuilder sql = new StringBuilder(250);
		sql.append("select a.*, b.META_CONTENT_TXT, s.DPY_SYN_SHOWPAD_ID ");
		sql.append("from ").append(props.get(Constants.CUSTOM_DB_SCHEMA)).append("DPY_SYN_MEDIABIN a ");
		sql.append("left join video_meta_content b on a.dpy_syn_mediabin_id=b.asset_id and b.asset_type='MEDIABIN' ");
		sql.append("left join ").append(props.get(Constants.CUSTOM_DB_SCHEMA)).append("DPY_SYN_SHOWPAD s ");
		sql.append("on a.dpy_syn_mediabin_id=s.dpy_syn_mediabin_id ");
		sql.append("where a.import_file_cd=?");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1,  type);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				MediaBinDeltaVO vo = new MediaBinDeltaVO(rs);
				vo.setRecordState(State.Delete); //by default these will get deleted.  sortRecords will override this value as appropriate
				data.put(vo.getDpySynMediaBinId(), vo);
			}

		} catch (SQLException sqle) {
			log.error("could not load manifest", sqle);
		}

		dataCounts.put("existing", data.size());
		log.info("loaded " + data.size() + " assets into manifest");
		return data;
	}



	/**
	 * override the deleteRecords methods to push deletions to Showpad after
	 * super.deleteRecords() saves them to the database.
	 */
	public void deleteRecords(Map<String, MediaBinDeltaVO> masterRecords) {
		super.deleteRecords(masterRecords);

		//confirm we have something to delete
		if (getDataCount("deleted") == 0) return;

		int cnt = 0;
		// Build the SQL Statement
		StringBuilder sql = new StringBuilder(350);
		sql.append("delete from ").append(props.getProperty(Constants.CUSTOM_DB_SCHEMA)).append("dpy_syn_showpad ");
		sql.append("where dpy_syn_showpad_id in ('~'");
		for (MediaBinDeltaVO vo : masterRecords.values()) {
			if (vo.getRecordState() == State.Delete) {
				sql.append(",?");
				++cnt;
			}
		}
		sql.append(")");
		log.debug(sql);

		if (cnt > 0) { //don't run the query if we don't need to
			cnt = 1;
			try (PreparedStatement ps  = dbConn.prepareStatement(sql.toString())) {
				for (MediaBinDeltaVO vo : masterRecords.values()) {
					if (vo.getRecordState() == State.Delete)  {
						try {
							//push deletions to Showpad
							String url = props.getProperty("showpadApiUrl") + "/assets/" + vo.getShowpadId() + ".json";
							String resp = showpadUtil.executeDelete(url);
							log.info("showpad delete response: " + resp);

							//if success, delete it from the DB as well
							ps.setString(cnt++, vo.getDpySynMediaBinId());

						} catch (Exception e) {
							String msg = makeMessage(vo, "Could not delete file from showpad: " + e.getMessage());
							failures.add(new Exception(msg));
							log.error("could not delete from showpad", e);
						}
					}
				}
				cnt = ps.executeUpdate();
			} catch (SQLException sqle) {
				failures.add(sqle);
			}
		}
		dataCounts.put("showpad-deleted", cnt);
	}


	/**
	 * returns a count of the database records; called after we finish our updates to verify total
	 * @param type
	 * @return
	 */
	protected void countDBRecords() {
		super.countDBRecords();

		int cnt = 0;
		StringBuilder sql = new StringBuilder(100);
		sql.append("select count(*) from ").append(props.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("dpy_syn_showpad a inner join ").append(props.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("dpy_syn_mediabin b on a.dpy_syn_mediabin_id=b.dpy_syn_mediabin_id and b.import_file_cd=?");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1, type);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				cnt = rs.getInt(1);

		} catch (SQLException sqle) {
			log.error("could not count records", sqle);
		}

		dataCounts.put("showpad-total", cnt);
		log.info("there are now " + cnt + " records in the showpad database");
	}
}