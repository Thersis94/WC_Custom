package com.depuysynthes.scripts.showpad.xls;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.depuysynthes.action.MediaBinAssetVO;
import com.depuysynthes.scripts.DSMediaBinImporterV2;
import com.depuysynthes.scripts.DSPrivateAssetsImporter;
import com.depuysynthes.scripts.MediaBinDeltaVO;
import com.depuysynthes.scripts.MediaBinDeltaVO.State;
import com.depuysynthes.scripts.showpad.ShowpadApiUtil;
import com.depuysynthes.scripts.showpad.ShowpadDivisionUtil;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.parser.MapExcelParser;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: AbstractShowpadIngest.java<br/>
 * <b>Description</b>: Extends the MediaBin importer to record and push assets to Showpad at the same time.
 * This file models ShowpadMediaBinDecorator, but deviates in supporting the simplified Excel meta-data file,
 * instead of EXP files.  It's only used for Showpad pushes. 
 * 
 * <b>Copyright:</b> Copyright (c) 2020<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Feb 01, 2020
 ****************************************************************************/
public abstract class AbstractShowpadIngest extends DSMediaBinImporterV2 {

	static final String BR = "<br/>";
	static final String HR_NL = "<hr/>\r\n";
	private static final Pattern EOS_REGEX = Pattern.compile("^[0-9]{1,6}\\-[0-9]{1,6}$");

	protected ShowpadApiUtil showpadApi;
	protected List<ShowpadDivisionUtil> divisions = new ArrayList<>();

	/**
	 * abstract constructor - not visible publicly
	 * @param args
	 * @throws IOException 
	 */
	protected AbstractShowpadIngest(String[] args) throws IOException {
		super(args);
		MIN_EXP_ROWS = 0;
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		//if the file hasn't changed since the last run, do nothing
		if (!hasFileChanged()) {
			closeDBConnection();
			return;
		}

		//flag upstream to skip Solr
		props.setProperty(Constants.SOLR_BASE_URL, "");

		try {
			showpadApi = ShowpadApiUtil.makeInstance(props, null);
		} catch (IOException e) {
			log.error("could not connect to Showpad", e);
		}
		//load the divisions
		loadShowpadDivisionList();
		super.run();
	}



	/**
	 * Load a list of tags already at Showpad
	 * If we try to add a tag to an asset without using it's ID, and it already existing in the system, it will fail.
	 * @throws QuotaException 
	 */
	protected void loadShowpadDivisionList() {
		String[] divs = props.getProperty("showpadDivisions" + type).split(",");
		for (String d : divs) {
			String[] div = d.split("=");
			ShowpadDivisionUtil util = new ShowpadDivisionUtil(props, div[1], div[0], showpadApi, dbConn);
			util.setEOSRun(true); //affects how files are named
			divisions.add(util);
			log.debug("created division " + div[0] + " with id " + div[1]);
		}
		log.info("loaded " + divisions.size() + " showpad divisions");
	}


	/**
	 * get the http header off the import file and compare to the last run.
	 * The script is designed to run every 5mins but only "do work" when the file changes. (when they've uploaded a new one)
	 * @return
	 */
	protected boolean hasFileChanged() {
		Path checksumFilePath = new File(props.getProperty("checksumFile" + type)).toPath();
		String todaysDate = Convert.formatDate(new Date(), Convert.DATE_DASH_PATTERN);
		String lastFullDate = null;
		String lastChecksum = null;
		try {
			String fileContents = new String(Files.readAllBytes(checksumFilePath));
			if (!StringUtil.isEmpty(fileContents)) {
				lastChecksum = fileContents.substring(fileContents.indexOf('~')+1);
				lastFullDate = fileContents.substring(0, fileContents.indexOf('~'));
			}
		} catch (IOException e) {
			log.error("could not read checksum file: " + e.getMessage());
		}

		MediaBinDeltaVO vo = new MediaBinDeltaVO();
		vo.setLimeLightUrl(importFile + "?t=" + Math.random());
		vo.setChecksum(lastChecksum);


		if (fileOnLLChanged(vo) || !todaysDate.equals(lastFullDate)) {
			vo.setFileChanged(true); //either of the above yields a true here
			
			// If there's no checksum that means we got a non-200 http response. 
			// We don't want to process any farther - exit quietly and let the script continue trying (on schedule) until the file becomes available
			if (StringUtil.isEmpty(vo.getChecksum()))
				return false;

			try {
				Files.write(checksumFilePath, (todaysDate +"~"+vo.getChecksum()).getBytes());
			} catch (IOException e) {
				log.error("could not write checksum file", e);
			}
		}
		return vo.isFileChanged();
	}


	/**
	 * overloaded to include the showpad DB table.
	 * retrieve the showpad data and store it into a Map for each Division
	 * @param type
	 * @return
	 */
	@Override
	protected Map<String,MediaBinDeltaVO> loadManifest() {
		StringBuilder sql = new StringBuilder(250);
		sql.append("select division_id, asset_id, dpy_syn_mediabin_id ");
		sql.append("from ").append(schema).append("DPY_SYN_SHOWPAD ");
		sql.append("where division_id in (");
		DBUtil.preparedStatmentQuestion(divisions.size(), sql);
		sql.append(") order by division_id");
		log.debug(sql);

		int x = 0;
		Map<String, Map<String, String>> divisionAssets = new HashMap<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (ShowpadDivisionUtil util : divisions) 
				ps.setString(++x, util.getDivisionId());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String division = rs.getString(1);
				Map<String, String> assets = divisionAssets.get(division);
				if (assets == null) assets = new HashMap<>();
				assets.put(rs.getString(3), rs.getString(2));
				divisionAssets.put(division, assets);
			}
		} catch (SQLException sqle) {
			log.error("could not load showpad assets from DB", sqle);
		}
		log.info("loaded " + divisionAssets.size() + " divisions from the database");

		//marry the divisionAssets to their respective util object
		for (Map.Entry<String, Map<String, String>> entry : divisionAssets.entrySet()) {
			for (ShowpadDivisionUtil util : divisions) {
				if (util.getDivisionId().equals(entry.getKey())) {
					util.setDivisionAssets(entry.getValue());
					log.info("gave " + entry.getValue().size() + " assets to division=" + entry.getKey());
					break;
				}
			}
		}

		//lean on the superclass to load the roster of assets
		return super.loadManifest();
	}


	/**
	 * override the saveRecords method to push the records to Showpad after 
	 * super.saveRecords() saves them to the database.
	 */
	@Override
	public void saveRecords(Map<String, MediaBinDeltaVO> masterRecords, boolean isInsert) {
		super.saveRecords(masterRecords, isInsert);

		//the below logic will process both inserts & updates at once.  
		//Block here for updates so we don't process the records twice.
		//Insert runs after deletes & updates, so wait for the 'inserts' invocation so 
		//all the mediabin records are already in our database.
		if (!isInsert) return;

		//push all changes to Showpad
		for (MediaBinDeltaVO vo : masterRecords.values()) {
			//we need to sort out what gets pushed to Showpad on our own.
			//if it's failed, being deleted, or unchanged and already in Showpad, skip it.
			State s = vo.getRecordState();
			if (s == State.Failed || s == State.Delete)
				continue;

			loopFileThroughDivisions(vo);

			log.info("completed: " + vo.getFileNm());
		}

		//process the ticket queue for each division
		for (ShowpadDivisionUtil util : divisions)
			util.processTicketQueue();

		//save the newly created records to our database for each division
		for (ShowpadDivisionUtil util : divisions)
			util.saveDBRecords();
	}


	/**
	 * called for each mediabin asset in the stack - push it out to each of the Divisions we're managing
	 * @param vo
	 * @throws QuotaException
	 */
	protected void loopFileThroughDivisions(MediaBinDeltaVO vo) {
		for (ShowpadDivisionUtil util : divisions) {
			util.pushAsset(vo);	
		}
	}



	/**
	 * override the deleteRecords methods to push deletions to Showpad after
	 * super.deleteRecords() saves them to the database.
	 */
	@Override
	public void deleteRecords(Map<String, MediaBinDeltaVO> masterRecords) {
		super.deleteRecords(masterRecords);

		//confirm we have something to delete
		if (getDataCount("deleted") == 0) return;

		List<String> deletedIds = deleteFromShowpad(masterRecords);

		//fail-fast if there's nothing to do
		if (deletedIds == null || deletedIds.isEmpty()) return;

		// Build the SQL Statement
		StringBuilder sql = new StringBuilder(350);
		sql.append("delete from ").append(schema).append("dpy_syn_showpad ");
		sql.append("where division_id in (");
		DBUtil.preparedStatmentQuestion(divisions.size(), sql);
		sql.append(") and dpy_syn_mediabin_id=?");
		log.debug(sql);

		try (PreparedStatement ps  = dbConn.prepareStatement(sql.toString())) {
			for (String id : deletedIds) {
				int x = 1;
				for (ShowpadDivisionUtil util : divisions)
					ps.setString(x++, util.getDivisionId());
				ps.setString(x, id);
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException sqle) {
			failures.add(sqle);
		}
	}


	/**
	 * deletes all the 'deleted' records from Showpad.
	 * Returns a list of IDs that were deleted, so we can delete them from the local database as well.
	 * @param masterRecords
	 * @return
	 * @throws QuotaException
	 */
	protected List<String> deleteFromShowpad(Map<String, MediaBinDeltaVO> masterRecords) {
		List<String> data = new ArrayList<>(100);
		for (MediaBinDeltaVO vo : masterRecords.values()) {
			if (vo.getRecordState() != State.Delete)  continue;

			//push deletions to Showpad
			for (ShowpadDivisionUtil util : divisions)
				util.deleteAsset(vo);

			//if success, delete it from the DB as well
			data.add(vo.getDpySynMediaBinId());
		}
		return data;
	}


	/**
	 * returns a count of the database records; called after we finish our updates to verify total
	 * @param type
	 * @return
	 */
	@Override
	protected void countDBRecords() {
		super.countDBRecords();

		StringBuilder sql = new StringBuilder(150);
		sql.append("select count(*), division_id, case when asset_id='FAILED_PROCESSING' then 1 else 0 end as status from ");
		sql.append(schema).append("dpy_syn_showpad where division_id in (");
		DBUtil.preparedStatmentQuestion(divisions.size(), sql);
		sql.append(") group by division_id, case when asset_id='FAILED_PROCESSING' then 1 else 0 end");
		log.debug(sql);

		int x = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (ShowpadDivisionUtil util : divisions)
				ps.setString(++x, util.getDivisionId());
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				setRecordCounts(rs);
		} catch (SQLException sqle) {
			log.error("could not count records", sqle);
		}
	}


	/**
	 * sets the counter values (to be reported in the email) into each of the division utils. 
	 * @param rs
	 * @throws SQLException
	 */
	protected void setRecordCounts(ResultSet rs) throws SQLException {
		for (ShowpadDivisionUtil util : divisions) {
			if (rs.getString(2).equals(util.getDivisionId()) && rs.getInt("status") == 0) {
				util.setDbCount(rs.getInt(1));
			} else if (rs.getString(2).equals(util.getDivisionId()) && rs.getInt("status") == 1) {
				util.setFailCount(rs.getInt(1));
			}
		}
	}


	/**
	 * @param html
	 */
	@Override
	protected void addSupplementalDetails(StringBuilder html) {
		//does nothing here, but gets overwritten by the Showpad decorator 
		//to add valueable stats to the admin email
		for (ShowpadDivisionUtil util : divisions) {
			html.append("<h3>Showpad ").append(util.getDivisionNm()).append(" Division</h3>");
			html.append("Added: ").append(util.getInsertCount()).append(BR);
			html.append("Updated: ").append(util.getUpdateCount()).append(BR);
			html.append("Deleted: ").append(util.getDeleteCount()).append(BR);
			html.append("Total: ").append(util.getDbCount()).append(BR);
			html.append("Failed to Ingest: ").append(util.getFailCount()).append(BR).append(BR);

			List<Exception> failures = util.getFailures();
			if (!failures.isEmpty()) {
				html.append("<b>The following issues were reported:</b>").append(BR).append(BR);

				// loop the errors and display them
				for (int i=0; i < failures.size(); i++) {
					html.append(failures.get(i).getMessage()).append(HR_NL);
					log.warn(failures.get(i).getMessage());
				}
			}

			html.append(HR_NL);
		}

		addExpiringSoonAssets(html);
	}


	/**
	 * Add a table to the report of assets expiring soon (or expired).
	 * Color orange for expiring <3mos.  Color red for expired.
	 * @param html
	 */
	protected void addExpiringSoonAssets(StringBuilder msg) {
		String sql = StringUtil.join("select tracking_no_txt, title_txt, expiration_dt from ", 
				schema, "dpy_syn_mediabin where expiration_dt <= current_date+90 ",
				"and expiration_dt is not null and import_file_cd=? ", 
				"order by expiration_dt, tracking_no_txt, title_txt");
		log.debug(sql);
		List<MediaBinAssetVO> assets = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setInt(1, type);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				MediaBinAssetVO vo = new MediaBinAssetVO();
				vo.setTrackingNoTxt(rs.getString(1));
				vo.setTitleTxt(rs.getString(2));
				vo.setExpirationDt(rs.getDate(3));
				assets.add(vo);
			}

		} catch (SQLException sqle) {
			log.error("could not load expiring assets", sqle);
		}

		if (assets.isEmpty()) return;

		//add a html table to the email body
		final String expiresSoon = "<font color=\"orange\">%s</font>";
		final String expired = "<font color=\"red\">%s</font>";
		final Date today = Calendar.getInstance().getTime();

		msg.append("<h4>Asset Expiration</h4>");
		msg.append("<table border='1' width='95%' align='center'><thead><tr>");
		msg.append("<th>Tracking Number</th>");
		msg.append("<th>Expiration</th>");
		msg.append("<th>Title</th>");
		msg.append("</tr></thead>\r<tbody>");

		for (MediaBinAssetVO vo : assets) {
			String mask = vo.getExpirationDt().after(today) ? expiresSoon : expired;
			msg.append("<tr><td>").append(String.format(mask, vo.getTrackingNoTxt())).append("</td>");
			msg.append("<td>").append(String.format(mask, Convert.formatDate(vo.getExpirationDt(), "MM/dd/yyyy"))).append("</td>");
			msg.append("<td>").append(String.format(mask, vo.getTitleTxt())).append("</td></tr>\r");
		}
		msg.append("</tbody></table>\r");
		msg.append("<p>Color Code: ").append(String.format(expiresSoon, "Expiring within 3mos. ")).append(String.format(expired, " Expired.  Hidden from Showpad.</p>"));
		msg.append(HR_NL);
	}


	/**
	 * Load the file via http, then parse it into a meaningful structure
	 * @param String importFile path
	 * @throws Exception
	 */
	@Override
	protected List<Map<String, String>> loadFile(String path) throws IOException {
		log.info("starting file parser");

		// Set the importFile so we can access it for the success email
		// append a randomized value to the URL to bypass upstream network caches
		importFile = path + "?t=" + System.currentTimeMillis();

		byte[] data = new SMTHttpConnectionManager().retrieveData(importFile);
		return transposeFile(data);
	}


	/**
	 * transpose from Excel to a Map so the upstream flows can be leveraged
	 * @param buffer
	 * @return
	 * @throws IOException
	 */
	protected List<Map<String, String>> transposeFile(byte[] fileContents) throws IOException {
		MapExcelParser parser = new MapExcelParser();
		List<Map<Integer, Object>> data;
		try {
			data = parser.parseData(fileContents, 0);
		} catch (Exception e) {
			throw new IOException("could not read file contents", e); 
		}

		// first row contains column names, separate it from the data
		Map<Integer, Object> header = data.get(0);
		data.remove(0);

		List<Map<String, String>> assets = new ArrayList<>();
		Map<String, String> asset;
		for (Map<Integer, Object> row : data) {
			asset = new HashMap<>();
			for (int x=0; x < header.size(); x++) {
				asset.put(String.valueOf(header.get(x)), StringUtil.checkVal(row.get(x),null));
			}
			assets.add(asset);
		}

		dataCounts.put("exp-raw", assets.size());
		log.info("file size is " + assets.size() + " rows");
		return assets;
	}


	/**
	 * Transpose the map into beans.  This is overwritten because our column names 
	 * aren't the same as the EXP files
	 */
	@Override
	public Map<String, MediaBinDeltaVO> parseData(List<Map<String, String>> data) {
		Map<String, MediaBinDeltaVO> records = new HashMap<>(data.size());

		// Loop the list and parse out each map item for inserting into the db
		String tn = null;
		MediaBinDeltaVO vo;
		for (Map<String, String> row : data) {
			try {
				tn = row.get("EOS Tracking Number");
				if (StringUtil.isEmpty(tn))
					throw new Exception("Tracking number missing for " + row.get("Title"));

				vo = new MediaBinDeltaVO();
				vo.setTrackingNoTxt(tn);
				vo.setEcopyTrackingNo(tn); //this is only used for reporting file-related issues in the admin email
				cleanupEOSNumbers(vo);
				vo.setDpySynMediaBinId(vo.getTrackingNoTxt()); //this gets turned into a combinedKey later, during reconcile

				//don't accept duplicates
				if (records.containsKey(vo.getTrackingNoTxt()))
					failures.add(new Exception("A duplicate record exists in the EXP file for " + tn));

				vo.setImportFileCd(type);
				vo.setRecordState(State.Insert);
				vo.setAssetNm(StringUtil.checkVal(row.get("Asset URL")).replace('\\','/'));
				vo.setFileNm(getFileName(row.get("Asset URL")));
				Date expirationDt = Convert.formatDate(Convert.DATE_LONG_DAY_OF_WEEK_PATTERN, row.get("Expiration Date"));
				vo.setExpirationDt(Convert.getTimestamp(expirationDt, false));
				vo.setOpCoNm(row.get("Business Unit (DS, Mentor...)"));
				vo.setProdFamilyNm(row.get("Line of business (Trauma, Spine…)"));
				vo.setProdNm(row.get("Brand"));
				vo.setLiteratureTypeTxt(row.get("Primary Material Type"));
				vo.setTitleTxt(row.get("Title"));
				vo.setLanguageCode(parseLanguage(row.get("Language")));
				vo.setBusinessUnitNm(row.get("Country/Region"));
				vo.setDownloadTypeTxt(row.get("Audience")); //Internal (private asset!) or External
				vo.setModifiedDt(Convert.getCurrentTimestamp());
				records.put(vo.getDpySynMediaBinId(), vo);

				//move a couple of these fields over as tags
				vo.addDesiredTag(vo.getBusinessUnitNm());
				vo.addDesiredTag(vo.getProdFamilyNm());

				if (!"external".equalsIgnoreCase(vo.getDownloadTypeTxt()))
					vo.addDesiredTag(DSPrivateAssetsImporter.INTERNAL_TAG);

			} catch (Exception e) {
				log.error("*************** Could not transpose EXP row for " + tn, e);
				StringBuilder msg = new StringBuilder(500);
				msg.append("Error processing data: ").append(e.getMessage());
				for (Map.Entry<String, String> entry : row.entrySet()) {
					if (!StringUtil.isEmpty(entry.getValue())) {
						log.error(entry.getKey() + "=" + entry.getValue());
						msg.append(BR).append(entry.getKey()).append("=").append(entry.getValue());
					}
				}
				failures.add(new Exception(msg.toString(), e));
				log.error("*************** end failed row ******************");
			}
		}

		dataCounts.put("exp-eligible", records.size());
		return records;
	}


	/**
	 * if the tracking# format matches EOS, separate the tracking# from the revision level.
	 * @param vo
	 */
	private void cleanupEOSNumbers(MediaBinDeltaVO vo) {
		//ensure we're only working against the EOS formatted numbers
		if (!EOS_REGEX.matcher(vo.getTrackingNoTxt()).matches()) return;

		String[] parts = vo.getTrackingNoTxt().split("-");
		if (parts.length == 2) {
			vo.setTrackingNoTxt(parts[0]);
			vo.seteCopyRevisionLvl(parts[1]);
		}
	}


	/**
	 * extract the file name from the URL path
	 * @param string
	 * @return
	 */
	private String getFileName(String path) {
		return path.substring(path.lastIndexOf('/')+1);
	}


	/**
	 * Assess and possibly download the files from LL or the NFS (for private assets)
	 * @param dropboxFolder
	 * @param vo
	 * @param fileNm
	 */
	@Override
	protected void downloadFiles(Map<String, MediaBinDeltaVO> masterRecords) {
		String dropboxFolder = (String) props.get("downloadDir") + "EMEA/";
		String dropboxFolderPrivate = (String) props.get("downloadDirPrivAssets" + type);

		for (MediaBinDeltaVO vo : masterRecords.values()) {
			if (!"external".equalsIgnoreCase(vo.getDownloadTypeTxt())) {
				//this is a private asset, look for it locally
				vo.setFileName(dropboxFolderPrivate + StringUtil.replace(vo.getAssetNm(), "/", File.separator));
				checkInternalFile(vo);

			} else {
				//escape certain chars on the asset path/name.  Note we cannot do a full URLEncode because of the directory separators
				String fileUrl = StringUtil.replace(vo.getAssetNm(), " ","%20");
				fileUrl = StringUtil.replace(fileUrl, "#","%23");
				vo.setLimeLightUrl(fileUrl);
				String folderPath = vo.getAssetNm().substring(vo.getAssetNm().indexOf("/INT%20Mobile/")+15);
				vo.setFileName(dropboxFolder + StringUtil.replace(folderPath, "/", File.separator));
				log.debug("public asset disk path=" + vo.getFileName());

				if (fileOnLLChanged(vo))
					downloadFile(vo);
			}
		}
	}


	/**
	 * equivalent of downloading from LimeLight - for internal (private) assets (which are on the NFS/disk)
	 * @param vo
	 */
	private void checkInternalFile(MediaBinDeltaVO vo) {
		//check for the file on disk.  if it's not there, flag this as an error.
		File f = new File(vo.getFileName());
		if (!f.exists()) {
			vo.setRecordState(State.Failed);
			String msg = makeMessage(vo, "File not found");
			failures.add(new Exception(msg));
			return;
		}

		//get modification date and file size.  Changes to either need to trigger an update to Showpad
		String checksum = new StringBuilder(15).append("||").append(f.length()).toString();
		if (!checksum.equals(vo.getChecksum())) {
			vo.setFileChanged(true);
			vo.setChecksum(checksum);
			vo.setModifiedDt(new Date(f.lastModified()));
			if (State.Ignore == vo.getRecordState()) {
				vo.setRecordState(State.Update);
				vo.setErrorReason("File on disk was updated");
			}
		}
	}
}