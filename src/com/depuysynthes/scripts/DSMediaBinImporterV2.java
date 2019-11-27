package com.depuysynthes.scripts;

// JDK 1.7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.beans.PropertyChangeEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

// SMT Base Libs
import com.depuysynthes.action.MediaBinAdminAction;
import com.depuysynthes.action.MediaBinAssetVO;
import com.depuysynthes.action.MediaBinDistChannels;
import com.depuysynthes.action.MediaBinLinkAction;
import com.depuysynthes.scripts.MediaBinDeltaVO.State;
import com.depuysynthes.solr.MediaBinSolrIndex;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.io.mail.MailHandlerFactory;
import com.siliconmtn.io.mail.mta.MailTransportAgentIntfc;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.solr.SolrClientBuilder;

// Web Crescendo Libs
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;

/****************************************************************************
 * <b>Title</b>: DSMediaBinImporterV2.java<p/>
 * <b>Description: Imports data from a flat file, parses the data, and inserts the data into database
 * tables.  This class improves upon it's predecessor by tracking updates & deletions to 
 * minimize the amount of data moving around.  It also downloads files as it goes, to ensure they're
 * on LimeLight (correctly) before we proclaim their existance on the DS/DSI websites.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Aug 26, 2015
 * 
 ****************************************************************************/
public class DSMediaBinImporterV2 extends CommandLineUtil {

	/**
	 * Stores the URL for the US or International import file
	 */
	protected String importFile = "";

	/**
	 * Delimiter used in the EXP file
	 */
	protected static final String DELIMITER = "\\|";

	/**
	 * Delimiterd used in the EXP file to tokenize multiple values stuffed into a single meta-data field
	 */
	public static final String TOKENIZER = "~";

	/**
	 * minimum rows in EXP file we're willing to consider a "a good file"
	 */
	private static final int MIN_EXP_ROWS = 2500;

	/**
	 * debug mode runs individual insert queries instead of a batch query, to be able to track row failures.
	 */
	protected boolean debugMode = false; 

	// Get the type (Intl (2) or US(1))
	protected int type = 1;

	/**
	 * List of errors 
	 */
	protected List <Exception> failures = new ArrayList<>();

	private Map<String,String> languages = new HashMap<>();

	private String limeLightUrl = MediaBinLinkAction.US_BASE_URL;

	protected Map<String, Integer> dataCounts = new HashMap<>();
	
	protected final String schema;

	/**
	 * Initializes the Logger, config files and the database connection
	 * @throws Exception
	 */
	public DSMediaBinImporterV2(String[] args) {
		super(args);
		loadProperties("scripts/MediaBin.properties");
		loadDBConnection(props);
		loadLanguages();

		if (args.length > 0 && Convert.formatInteger(args[0]) > 0) type = Convert.formatInteger(args[0]);
		importFile = props.getProperty("importFile" + type);
		
		schema = props.getProperty(Constants.CUSTOM_DB_SCHEMA);

		if (type == 2) limeLightUrl = MediaBinLinkAction.INT_BASE_URL;
	}


	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		//Create an instance of the MedianBinImporter
		DSMediaBinImporterV2 dmb = new DSMediaBinImporterV2(args);
		dmb.run();
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		long startNano = System.nanoTime();
		log.info("Starting Importer for " + importFile);

		if (args.length > 1)
			debugMode = Convert.formatBoolean(args[1]);

		Map<String, MediaBinDeltaVO> masterRecords = null;
		try {
			// Attempt to retrieve and order the data
			List<Map<String, String>> looseData = loadFile(importFile);

			// Validate some data was retrieved before we go any further
			if (looseData.size() < MIN_EXP_ROWS) {
				throw new Exception("Not enough records or EXP file not found: " + importFile);
			}

			//load the manifest of assets already in our system
			masterRecords = loadManifest();

			//turn the List of loose data into a set of VOs - business rules get applied here.
			Map<String, MediaBinDeltaVO> newRecords = parseData(looseData);

			//merge the data to determine where the deltas are
			//after this step each record will be tagged 'insert','update','delete', or 'ignore'
			sortDeltas(masterRecords, newRecords);

			//inspect the payload - for EMEA if delete count is too high, fail-fast.
			auditChanges(masterRecords);

			//download (from LL) all the 'insert' and 'update' records.
			//if download fails then set the record's State to failed - failed records will be reported and not transacted-upon
			downloadFiles(masterRecords);

			//Update all existing records
			saveRecords(masterRecords, false);

			//Delete all deleted records
			deleteRecords(masterRecords);

			// Insert all new records
			saveRecords(masterRecords, true);

			//count DB records
			countDBRecords();

			//push-to-solr - all three of the above transaction sets
			syncWithSolr(masterRecords);


		} catch (Exception e) {
			log.error("Error parsing file... " + e.getMessage(), e);
			failures.add(new Exception("Error parsing file: " + e.getMessage(), e));
		}

		sendEmail(startNano, masterRecords);
		DBUtil.close(dbConn);
	}


	/**
	 * Inspect the records - if deltas are too high mark the payload suspicious and stop
	 * processing so an administrator can intervene and review.
	 * @param masterRecords
	 * @throws Exception 
	 */
	protected void auditChanges(Map<String, MediaBinDeltaVO> masterRecords) throws InvalidDataException {
		int maxUpdates = Convert.formatInteger(props.getProperty("auditMaxUpdates"),-1);
		int maxDeletes = Convert.formatInteger(props.getProperty("auditMaxDeletes"),-1);
		//if both are -1, that means there's no limit.  We're done here.
		if (maxUpdates < 0 && maxDeletes < 0) return;

		//count the master records by status
		int updCnt = 0;
		int delCnt = 0;
		for (MediaBinDeltaVO vo : masterRecords.values()) {
			if (State.Update == vo.getRecordState()) ++updCnt;
			else if (State.Delete == vo.getRecordState()) ++delCnt;
		}

		//compare the #s and take action
		if (maxUpdates > -1 && updCnt > maxUpdates) {
			throw new InvalidDataException(String.format("Too many updates (%d out of %d, threshold is %d).  Data audit failed.", updCnt, masterRecords.size(), maxUpdates));
		} else if (maxDeletes > -1 && delCnt > maxDeletes) {
			throw new InvalidDataException(String.format("Too many deletions (%d out of %d, threshold is %d).  Data audit failed.", delCnt, masterRecords.size(), maxDeletes));
		}
	}


	/**
	 * returns a collection of all the mediabin records already in our database.
	 * This gets used to perform a differential of the changes needed.
	 * @param type
	 * @return
	 */
	protected Map<String,MediaBinDeltaVO> loadManifest() {
		Map<String,MediaBinDeltaVO> data = new HashMap<>(7000); //at time of writing, this was enough capacity to avoid resizing

		StringBuilder sql = new StringBuilder(250);
		sql.append("select a.*, ");
		//only include video chapters when they've changed, because the delta's coming out of the EXP file won't have these to compare against
		sql.append("case when coalesce(b.update_dt, b.create_dt) > CURRENT_DATE - interval '1 day' then b.META_CONTENT_TXT else null end as META_CONTENT_TXT ");
		sql.append("from ").append(schema).append("dpy_syn_mediabin a ");
		sql.append("left join video_meta_content b on a.dpy_syn_mediabin_id=b.asset_id and b.asset_type='MEDIABIN' ");
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
	 * returns a count of the database records; called after we finish our updates to verify total
	 * @param type
	 * @return
	 */
	protected void countDBRecords() {
		int cnt = 0;
		String sql = StringUtil.join("select count(*) from ", schema, "dpy_syn_mediabin where import_file_cd=?");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setInt(1,  type);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				cnt = rs.getInt(1);

		} catch (SQLException sqle) {
			log.error("could not count records", sqle);
		}

		dataCounts.put("total", cnt);
		log.info("there are now " + cnt + " records in the database");
	}


	/**
	 * consumes the masterRecords list and pushes the qualifying records into Solr
	 * @param masterRecords
	 */
	protected void syncWithSolr(Map<String, MediaBinDeltaVO> masterRecords) {
		// initialize the connection to the solr server
		String baseUrl = props.getProperty(Constants.SOLR_BASE_URL);
		String collection = props.getProperty(Constants.SOLR_COLLECTION_NAME);

		SolrClient server = SolrClientBuilder.build(baseUrl, collection);

		pushToSolr(masterRecords.values(), server);

		//ask for a count of records in Solr using a typical query
		int cnt = 0;
		String queryString = SearchDocumentHandler.INDEX_TYPE + ":" + MediaBinSolrIndex.INDEX_TYPE;
		queryString += " AND importFileCd_i:" + type;
		SolrQuery qry = new SolrQuery(queryString);
		qry.setRows(50000);
		qry.setFields(SearchDocumentHandler.DOCUMENT_ID); //we only need the documentId back from Solr - JM 08.04.16
		SolrDocumentList solrData = null;
		try {
			solrData = server.query(qry).getResults();
			cnt = Convert.formatLong("" + solrData.getNumFound()).intValue();
			log.info("solr count = " + cnt);
		} catch (Exception e) {
			failures.add(e);
			log.error("could not process read Solr query", e);
		}

		//if the count in solr doesn't match what's in the database, report which ones are missing
		int dbCnt = dataCounts.get("total");
		if (dbCnt != cnt) {
			List<MediaBinDeltaVO> revisions = analyzeSolrRecords(masterRecords, solrData, true);
			//if extra solr records were present and got deleted, commit that change and re-count the records
			log.debug("revision count=" + revisions.size());
			pushToSolr(revisions, server);

			try {
				solrData = server.query(qry).getResults();
				cnt = Convert.formatLong("" + solrData.getNumFound()).intValue();
				log.info("solr re-count = " + cnt);
			} catch (Exception e) {
				failures.add(e);
				log.error("could not process read Solr query", e);
			}
			if (dbCnt != cnt) {
				//this time when we re-sort, don't go to Solr, which would put us in a loop; just report them as failed.
				analyzeSolrRecords(masterRecords, solrData, false);
			}
		}

		dataCounts.put("solr", cnt);
	}


	/**
	 * accepts a list of records to transact, and the server to run them against
	 * @param masterRecords
	 * @param server
	 */
	private void pushToSolr(Collection<MediaBinDeltaVO> records, SolrClient server) {
		//bucketize what needs to be done so we can hit Solr in two batch transactions
		List<String> deletes = new ArrayList<>(records.size());
		List<MediaBinAssetVO> adds = new ArrayList<>(records.size());
		for (MediaBinDeltaVO vo : records) {
			switch (vo.getRecordState()) {
				case Delete:
				case Failed: //fails come from 404's at LimeLight; we don't want these showing up on DS if the linked file is MIA
					log.debug("deleting from Solr: " + vo.getDpySynMediaBinId());
					deletes.add(vo.getDpySynMediaBinId());
					break;
				case Update:
				case Insert:
					//case NewDownload: //we want the file contents to be re-indexed here, even though there are no meta-data changes
					log.debug("adding to Solr: " + vo.getDpySynMediaBinId());
					adds.add(vo);
					break;
				default:
			}
		}

		//fire the deletes to solr first
		if (deletes.size() > 0) {
			try {
				server.deleteById(deletes);
			} catch (Exception e) {
				log.error("could not delete records from Solr", e);
				failures.add(e);
			}
		}

		//fire the adds & updates to Solr the same way the offline indexer does
		if (adds.size() > 0) {
			MediaBinSolrIndex idx = new MediaBinSolrIndex();
			idx.indexFiles(adds, server, "");
		}

		//commit the changes using a softCommit
		if (adds.size() > 0 || deletes.size() > 0) {
			try {
				server.commit(true, true, true);
			} catch (Exception e) {
				log.error("could not commit solr changes", e);
				failures.add(e);
			}
		}
	}


	/**
	 * cross-compares what's in Solr against what's in the database to report
	 * to the admin where the missing records are
	 * @param masterRecords
	 * @param server
	 * @param queryString
	 */
	private List<MediaBinDeltaVO> analyzeSolrRecords(Map<String, MediaBinDeltaVO> masterRecords, 
			SolrDocumentList solrData, boolean retrySolr) {
		List<MediaBinDeltaVO> changes = new ArrayList<>();
		log.debug("analyzeSolrRecords retry=" + retrySolr);
		try {
			//iterate solr against DB
			for (SolrDocument sd : solrData) {
				if (!masterRecords.containsKey(StringUtil.checkVal(sd.getFieldValue(SearchDocumentHandler.DOCUMENT_ID)))) {
					log.warn("masterRecords does not have " + sd.getFieldValue(SearchDocumentHandler.DOCUMENT_ID) + " which needs to be deleted from Solr");
					//these should be deleted from Solr
					if (retrySolr) {
						MediaBinDeltaVO vo = new MediaBinDeltaVO();
						vo.setDpySynMediaBinId("" + sd.getFieldValue(SearchDocumentHandler.DOCUMENT_ID));
						vo.setRecordState(State.Delete);
						log.warn("need to delete extra solr record for " + vo.getDpySynMediaBinId());
						changes.add(vo);
					} else {
						failures.add(new Exception("Record exists in Solr but not database, and could't be deleted: " + sd.getFieldValue(SearchDocumentHandler.DOCUMENT_ID)));
					}
				}
			}

			//iterate DB against Solr
			dbLoop:
				for (MediaBinDeltaVO vo: masterRecords.values()) {
					//make sure only 'good' records get pushed to Solr. - this check filters out failed records, including 404's at LL. 
					if (!vo.isUsable()) {
						log.warn("DB record contains bad data and should not be added to Solr.  Likely a simple 404@LL: " + vo.getDpySynMediaBinId());
						continue;
					}

					String id = StringUtil.checkVal(vo.getDpySynMediaBinId());
					for (SolrDocument sd : solrData) {
						if (id.equals(StringUtil.checkVal(sd.getFieldValue(SearchDocumentHandler.DOCUMENT_ID)))) {
							continue dbLoop;
						}
					}
					if (retrySolr) {
						//bug-fix: the record goes back into the Insert pool, but we aren't accounting for it in the Insert Count reported in the email.  
						//tag the record so we can put an asterics in the report email. -JM 08.14.16
						vo.setActionDesc("*");
						vo.setRecordState(State.Insert);
						log.warn("need to add missing solr record for " + vo.getDpySynMediaBinId());
						changes.add(vo);
					} else {
						failures.add(new Exception("Record exists in database but not Solr and couldn't be added: " + vo.getDpySynMediaBinId()));
					}
				}

		} catch (Exception e) {
			failures.add(e);
			log.error("could not report Solr missing entries", e);
		}
		return changes;
	}


	/**
	 * iterate through the new records and merge them into the master data if there are changes to that record.
	 * then loop through the 
	 * @param masterRecords
	 * @param newRecords
	 */
	private void sortDeltas(Map<String, MediaBinDeltaVO> masterRecords, Map<String, MediaBinDeltaVO> newRecords) {
		//loop the new records.  
		//If the record exists in the master data mark it as an update
		//if the record does not exist in the master data mark it as an insert
		for (MediaBinDeltaVO vo : newRecords.values()) {
			if (masterRecords.containsKey(vo.getDpySynMediaBinId())) { //legacy tracking#
				MediaBinDeltaVO mr = masterRecords.get(vo.getDpySynMediaBinId());
				setUpdateFields(vo, mr);
				masterRecords.put(vo.getDpySynMediaBinId(), vo);
				continue;
			}

			//combined primary key - importFileCd+tracking# - adds support for global assets, 
			//which present themselves in both EXP files. -JM 08.16.16
			String combinedKey = "" + vo.getImportFileCd() + vo.getDpySynMediaBinId();

			if (masterRecords.containsKey(combinedKey)) {
				MediaBinDeltaVO mr = masterRecords.get(combinedKey);
				vo.setDpySynMediaBinId(combinedKey); //preserve the combined key
				setUpdateFields(vo, mr);

			} else {
				vo.setRecordState(State.Insert);
				//Give all new assets a combined primary key of importFileCd+tracking#
				//Supports global assets where the tracking# is the same in both feeds. - JM 08.16.16
				vo.setDpySynMediaBinId(combinedKey);
			}
			masterRecords.put(vo.getDpySynMediaBinId(), vo);
		}
		//NOTE: the default State for masterRecords is Delete, so the ones NOT impacted 
		//by the above iteration are already earmarked for deletion, which is correct.
		//Only the records in the incoming EXP file should persist in the system.
	}


	/**
	 * compared the new/existing VOs to determine if we have changes to capture.
	 * @param vo
	 * @param mr
	 */
	private void setUpdateFields(MediaBinDeltaVO vo, MediaBinDeltaVO mr) {
		//check to see if the data has changed, which implies we have an update
		if (! vo.lexicographyEquals(mr)) {
			vo.setRecordState(State.Update);

		} else if (vo.geteCopyRevisionLvl() != null && !vo.geteCopyRevisionLvl().equals(mr.geteCopyRevisionLvl())) {
			//the file on LL should have changed, but there's nothing in the meta-data we need to save
			vo.addDelta(new PropertyChangeEvent(vo,"eCopyRevisionLvl",mr.geteCopyRevisionLvl(), vo.geteCopyRevisionLvl()));
			vo.setRecordState(State.Update);

		} else {
			//nothing changed, ignore this record.  99% of time this is the default use case
			vo.setRecordState(State.Ignore);
		}

		//pass the checksum of the existing file to the new VO, so we can compare it to the new file
		vo.setChecksum(mr.getChecksum());
		//pass the video chapters as well
		vo.setVideoChapters(mr.getVideoChapters());
	}


	/**
	 * call out to LL and download the asset.  Then write it to the dropbox folder
	 * @param dropboxFolder
	 * @param vo
	 * @param fileNm
	 */
	protected void downloadFiles(Map<String, MediaBinDeltaVO> masterRecords) {
		String dropboxFolder = (String) props.get("downloadDir");
		boolean fileExists = true; //true - we don't check the file system by default

		for (MediaBinDeltaVO vo : masterRecords.values()) {
			//escape certain chars on the asset path/name.  Note we cannot do a full URLEncode because of the directory separators
			String fileUrl = StringUtil.replace(vo.getAssetNm(), " ","%20");
			fileUrl = StringUtil.replace(fileUrl, "#","%23");
			vo.setLimeLightUrl(limeLightUrl + fileUrl);
			vo.setFileName(dropboxFolder + StringUtil.replace((type == 1 ? "US" : "EMEA") + "/" + vo.getAssetNm(), "/", File.separator));

			//check for files on disk. If we have the file we need then that's good enough. - use in development to get around extensive http calls on repeated trial runs.
			if (Convert.formatBoolean(props.getProperty("honorExistingFiles"))) {
				File f = new File(vo.getFileName());
				fileExists = f.exists();
				if (!fileExists || fileOnDiskChanged(vo, f)) {
					vo.setFileChanged(true);
					downloadFile(vo);
				}
				continue;
			}

			if (!fileExists || fileOnLLChanged(vo))
				downloadFile(vo);
		}
	}


	/**
	 * if we're going to honor existing files, then we need to at least look at file size to gauge whether the file is changed or not.
	 * This is a factor when we reprocess across Divisions...we don't need to make 5k http calls, but we may have files to upload
	 * @param vo
	 * @param f
	 * @return
	 */
	private boolean fileOnDiskChanged(MediaBinDeltaVO vo, File f) {
		String checksum = vo.getChecksum();
		if (StringUtil.isEmpty(checksum)) fileOnLLChanged(vo); //if the checksum is empty go out to LL for the header (only)

		//mark the file as changed if the size on disk doesn't match the header's content-length
		long oldSz = Convert.formatLong(vo.getChecksum().split("\\|\\|")[1]);
		/**
		 * NOTE there is an extreme edge-case here, that a file could be changed but exactly the same size.  
		 * We don't use honorExistingFiles outside of reprocessing (which is manually done) so this is not a concern.
		 */
		return oldSz != f.length();
	}


	/**
	 * call out to LL and download the asset.  Then write it to the dropbox folder
	 * @param dropboxFolder
	 * @param vo
	 * @param fileNm
	 */
	protected void downloadFile(MediaBinDeltaVO vo) {
		log.info("retrieving " + vo.getLimeLightUrl());
		try {
			SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
			InputStream is = conn.getConnectionStream(vo.getLimeLightUrl(), new HashMap<String, Object>());

			if (404 == conn.getResponseCode())
				throw new FileNotFoundException();

			if (200 != conn.getResponseCode())
				throw new IOException();

			//write the file to our repository
			String fullPath = vo.getFileName(); 
			String parentDir = fullPath.substring(0, fullPath.lastIndexOf(File.separator));
			File dir = new File(parentDir);
			if (!dir.exists()) dir.mkdirs();

			File f = new File(fullPath);
			try (FileOutputStream fos = new FileOutputStream(f)) {
				int nRead, byteCnt = 0;
				byte[] byteBuffer = new byte[8192];
				while ((nRead = is.read(byteBuffer)) != -1) {
					byteCnt += nRead;
					fos.write(byteBuffer, 0, nRead);
				}
				fos.flush();
				int kbCnt = byteCnt;
				try { kbCnt = byteCnt/1000; } catch (Exception e) {}
				vo.setFileSizeNo(kbCnt);
				log.debug("wrote file " + fullPath + " kb=" + kbCnt + " bytes=" + byteCnt);
			}
		} catch (FileNotFoundException fnfe) {
			vo.setRecordState(State.Failed);
			String msg = makeMessage(vo, "File not found on LimeLight");
			failures.add(new Exception(msg));
		} catch (IOException ioe) {
			vo.setRecordState(State.Failed);
			String msg = makeMessage(vo, "Network error downloading from LimeLight: " + ioe.getMessage());
			failures.add(new Exception(msg));
		} catch (Exception e) {
			vo.setRecordState(State.Failed);
			String msg = makeMessage(vo, "Unknown error downloading from LimeLight: " + e.getMessage());
			failures.add(new Exception(msg));
		}

		//if we successfully downloaded a new file for a record with no meta-data changes,
		//we need to flag it so Solr gets updated.  We also need to update the checksum column in the DB
		if (State.Ignore == vo.getRecordState()) {
			vo.setRecordState(State.Update);
			vo.setErrorReason("File on LL was updated");
		}
	}


	/**
	 * call out to LL using a HEAD request to verify the file still exists
	 * @param dropboxFolder
	 * @param vo
	 * @param fileNm
	 */
	private boolean fileOnLLChanged(MediaBinDeltaVO vo) {
		log.info("checking headers on " + vo.getLimeLightUrl());
		boolean changed = false;
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(vo.getLimeLightUrl()).openConnection();
			conn.setRequestMethod("HEAD");

			if (HttpURLConnection.HTTP_OK == conn.getResponseCode()) {
				String lastMod = StringUtil.checkVal(conn.getHeaderField("Last-Modified"), new Date().toString());
				String checksum = lastMod + "||" + conn.getHeaderField("Content-Length");
				log.debug(checksum);
				changed = !checksum.equals(vo.getChecksum());
				vo.setChecksum(checksum);
				//use the file's datestamp for modified date on our side
				vo.setModifiedDt(Convert.formatDate("EEEE, dd MMM yyyy hh:mm:ss z", lastMod));
				if (!changed) {
					vo.setErrorReason("File on LL did not change");
				}
			} else {
				changed = true;
			}
			//cleanup at the TCP level so Keep-Alives can be leveraged at the IP level
			conn.getInputStream().close();
			conn.disconnect();

		} catch (Exception e) {
			//ignore these, because by returning true we're going to make a second
			//call out to LL to retrieve the file, which will not be found, and be recorded
			//as a failure (properly)
			changed = true;
		}
		vo.setFileChanged(changed);
		return changed;
	}


	/**
	 * turns a simple error into somewhat of a debug statement - for informational
	 * purposes in the outgoing email.
	 * @param vo
	 * @param err
	 * @param url
	 * @return
	 */
	public static String makeMessage(MediaBinDeltaVO vo, String err) {
		StringBuilder msg = new StringBuilder(200);
		msg.append("<font color='red'>").append(err).append(":</font><br/>");
		msg.append("Tracking number: ").append(vo.getTrackingNoTxt()).append("<br/>");
		if (vo.getEcopyTrackingNo() != null)
			msg.append("eCopy Tracking Number: ").append(vo.getEcopyTrackingNo()).append("<br/>");
		msg.append("File Name: <a href=\"").append(vo.getLimeLightUrl()).append("\">").append(vo.getFileNm()).append("</a>");

		return msg.toString();
	}


	/**
	 * parses the import file.  Import text file format - first row contains column headers:
	 * 1> EMAIL_ADDRESS_TXT|PASSWORD_TXT|FIRST_NM|LAST_NM
	 * 2> jmckain@siliconmtn.com|mckain|James|McKain
	 * 
	 * @param String importFile file path
	 * @throws Exception
	 */
	protected List<Map<String, String>> loadFile(String path) throws IOException {
		log.info("starting file parser");

		// Set the importFile so we can access it for the success email
		// append a randomized value to the URL to bypass upstream network caches
		importFile = path + "?t=" + System.currentTimeMillis();
		URL url = new URL(importFile);
		BufferedReader buffer = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-16"));

		return parseFile(buffer);
	}


	/**
	 * separates parsing the file from obtaining it (above).
	 * @param buffer
	 * @return
	 * @throws IOException
	 */
	protected List<Map<String, String>> parseFile(BufferedReader buffer) throws IOException {
		//possibly create a Writer object to store the EXP file onto persistent disk, for archiving.
		BufferedWriter writer = makeArchiveWriter();

		// first row contains column names; must match UserDataVO mappings
		String line = StringUtil.checkVal(buffer.readLine());
		String tokens[] = new String[0];
		if (line != null) tokens = line.split(DELIMITER, -1);
		String[] columns = new String[tokens.length];
		for (int i = 0; i < tokens.length; i++) {
			columns[i] = tokens[i];
		}

		//write the header line to disk
		writer.write(line);
		writer.newLine();

		String rowStr = null;
		Map<String, String> entry = null;
		List<Map<String, String>> data = new ArrayList<>();
		// Map<String,Integer> colSizes = new HashMap<String,Integer>();

		// execution in this loop WILL throw NoSuchElementException.
		// This is not trapped so you can cleanup data issue prior to import
		for (int y = 0; (rowStr = buffer.readLine()) != null; y++) {
			//write the line to disk
			writer.write(rowStr);
			writer.newLine();

			tokens = rowStr.split(DELIMITER, -1);

			// test quality of data
			if (tokens.length != columns.length) {
				log.error("Not loading row# " + y + "  " + rowStr);
				String msg = rowStr.indexOf('|') > -1 ? rowStr.substring(0,rowStr.indexOf('|')) : rowStr;
				failures.add(new Exception("Skipped EXP row# " + y + ", it has " + tokens.length + " columns instead of " + columns.length + ":<br/>" + msg));
				continue;
			}

			entry = new HashMap<>(20);
			for (int x = 0; x < tokens.length; x++) {
				String value = StringUtil.checkVal(tokens[x].trim());

				// remove surrounding quotes if they exist
				if (value.startsWith("\"") && value.endsWith("\""))
					value = value.substring(1, value.length() - 1);

				if (value.equalsIgnoreCase("null")) value = null;

				entry.put(columns[x], value);
			}
			data.add(entry);
			entry = null;
		}
		// close the archive file
		writer.close();

		dataCounts.put("exp-raw", data.size());
		log.info("file size is " + data.size() + " rows");
		return data;
	}


	/**
	 * Creates a FileWriter used for writing the contents of the EXP file to persistent disk.
	 * @return
	 * @throws IOException
	 */
	private BufferedWriter makeArchiveWriter() throws IOException {
		String archivePath = props.getProperty("expArchivePath");
		if (archivePath == null || archivePath.isEmpty()) {
			OutputStream nullOS = new OutputStream() { @Override public void write(int b) {/* does nothing */} };
			return new BufferedWriter(new OutputStreamWriter(nullOS));
		}

		String fileName = type + "-Metadata.exp-" + Convert.formatDate(new Date(), Convert.DATE_TIME_NOSPACE_PATTERN);
		log.info("archiving EXP file to " + archivePath + fileName);

		Path dstPath = Paths.get(archivePath + fileName);
		return Files.newBufferedWriter(dstPath, StandardCharsets.UTF_16);
	}


	/**
	 * Inserts the data in the supplied list of maps into the database
	 * @param data
	 * @return
	 */
	public Map<String, MediaBinDeltaVO> parseData(List<Map<String,String>> data) {
		List<String> acceptedAssets = new ArrayList<>();
		acceptedAssets.addAll(java.util.Arrays.asList(MediaBinAdminAction.VIDEO_ASSETS));
		acceptedAssets.addAll(java.util.Arrays.asList(MediaBinAdminAction.PDF_ASSETS));

		String tn = "", pkId = "";
		String[] requiredOpCo = MediaBinDistChannels.getDistChannels(type);
		Map<String, MediaBinDeltaVO> records = new HashMap<>(data.size());

		// Loop the list and parse out each map item for inserting into the db
		MediaBinDeltaVO vo;
		for (Map<String, String> row : data) {
			try {
				// load the tracking number, support eCopy and MediaBin file layouts
				tn = StringUtil.checkVal(row.get("Tracking Number"));
				if (!tn.isEmpty()) {
					pkId = tn;
					if (type == 1) pkId = StringUtil.checkVal(row.get("Business Unit ID")) + pkId; //US assets get business unit as part of pKey.

				} else {
					//no legacy#, use eCopy
					tn = StringUtil.checkVal(row.get("eCopy Tracking Number"));
					pkId = splitTrackingNo(tn);
				}

				//for INTL, use the file name as a tracking number (final fallback).
				//NOTE: once eCopy launch this becomes unreachable code.  All assets will have one of the two above.
				if ((type == 2 || type == 3) && tn.isEmpty()) {
					tn  = loadLegacyTrackingNumberFromFileName(row);
					pkId = tn;
				}

				// Make sure the files are for one of our websites, and in the File Types we're authorized to use.
				if (!isOpcoAuthorized(row.get("Distribution Channel"), requiredOpCo, tn) ||
						!isAssetTypeAuthorized(row.get("Asset Type"), acceptedAssets)) {

					if (debugMode) { //if we're in debug mode, report why we're skipping this record.
						String reason = " || ";
						if (StringUtil.isEmpty(row.get("Distribution Channel"))) {
							reason += "No dist channel";
						} else if (!isAssetTypeAuthorized(row.get("Asset Type"), acceptedAssets)) {
							reason += "wrong asset type: " + row.get("Asset Type");
						} else {
							reason += "unauthorized opCo: " + row.get("Distribution Channel");
						}
						log.info("skipping asset " + row.get("Asset Name") + reason);
					}
					continue;
				}

				vo = new MediaBinDeltaVO();

				//still no tracking number, this asset is invalid!
				if (tn.isEmpty())
					throw new Exception("Tracking number missing for " + row.get("Name"));

				vo.setTrackingNoTxt(tn);
				vo.setDpySynMediaBinId(pkId);
				vo.setImportFileCd(type);
				vo.setRecordState(State.Insert);

				//pluck the tracking#s off the end of the Anatomy field, if data exists
				if (StringUtil.checkVal(row.get("Anatomy")).indexOf(TOKENIZER) > 0) {
					String[] vals = StringUtil.checkVal(row.get("Anatomy")).split(TOKENIZER);
					Set<String> newVals = new LinkedHashSet<String>(vals.length);
					for (String s : vals) {
						if (s.startsWith("DSUS")) continue; //remove tracking#s
						newVals.add(s.trim().replaceAll(", ", ","));
					}
					row.put("Anatomy", StringUtil.getDelimitedList(newVals.toArray(new String[newVals.size()]), false, TOKENIZER));
				}

				//determine Modification Date for the record. -- displays in site-search results
				//note modDt now gets overwritten with the Last Modified header coming back from LimeLight. -JM 11-23-15
				Date modDt = Convert.formatDate(Convert.DATE_TIME_SLASH_PATTERN_FULL_12HR, row.get("Check In Time"));
				if (modDt == null) modDt = Convert.formatDate(Convert.DATE_TIME_SLASH_PATTERN_FULL_12HR, row.get("Insertion Time"));
				//NOTE Asset Description overlaps with assetDesc below...INT uses the fallback field for assetDesc instead of this one.
				//replace possible MM.DD.YYYY notations with MM/DD/YYYY for consistency - deviace introduced by Pierre 08/21/19
				Date expirationDt = Convert.formatDate("MM/dd/yyyy", StringUtil.replace(row.get("Asset Description"),".","/"));

				// Insert the record
				vo.setAssetNm(StringUtil.checkVal(row.get("Asset Name")).replace('\\','/'));
				vo.setAssetDesc(StringUtil.checkVal(row.get("Asset Description"), row.get("SOUS - Literature Category")));
				vo.setAssetType(row.get("Asset Type").toLowerCase());
				vo.setBodyRegionTxt(parseBodyRegion(row));
				vo.setBusinessUnitNm(StringUtil.checkVal(row.get("BUSINESS UNIT"),row.get("SOUS - Business Unit")));
				vo.setBusinessUnitId(Convert.formatInteger(row.get("Business Unit ID")));
				vo.setDownloadTypeTxt(StringUtil.checkVal(row.get("SOUS - Literature Category"))); // download_type_txt
				vo.setLanguageCode(parseLanguage(row.get("SOUS - Language"))); // language_cd
				vo.setLiteratureTypeTxt(StringUtil.checkVal(row.get("Literature Type"), row.get("SOUS - Literature Category")));
				vo.setModifiedDt(Convert.getTimestamp(modDt, true));
				vo.setExpirationDt(Convert.getTimestamp(expirationDt, false));
				vo.setFileNm(row.get("Name"));
				vo.setDimensionsTxt(row.get("Dimensions (pixels)"));
				vo.setFileSizeNo(Convert.formatInteger(row.get("Original File Size")));
				vo.setProdFamilyNm(getProductFamily(row));
				vo.setProdNm(StringUtil.checkVal(row.get("Product Name"), row.get("SOUS - Product Name")));
				vo.setRevisionLvlTxt(StringUtil.checkVal(row.get("Revision Level"), row.get("Current Revision")));
				vo.seteCopyRevisionLvl(StringUtil.checkVal(row.get("eCopy Revision Level"), null));
				vo.setOpCoNm(row.get("Distribution Channel"));
				vo.setTitleTxt(row.get("Title"));
				vo.setDuration(Convert.formatDouble(row.get("Media Play Length (secs.)")));
				vo.setAnatomy(StringUtil.checkVal(row.get("Anatomy"), null)); //used on DSI.com
				vo.setDescription(StringUtil.checkVal(row.get("Description"), null)); //used on DSI.com
				vo.setMetaKeywords(parseKeywords(row, type)); //used on DSI.com
				vo.setEcopyTrackingNo(row.get("eCopy Tracking Number")); //this is only used for reporting file-related issues in the admin email

				//ensure this isn't one we already have
				if (! records.containsKey(vo.getDpySynMediaBinId())) {
					records.put(vo.getDpySynMediaBinId(), vo);
				} else {
					failures.add(new Exception("A duplicate record exists in the EXP file for " + tn));
				}

			} catch (Exception e) {
				log.error("*************** Could not transpose EXP row for " + tn, e);
				String msg = "Error processing data: " + e.getMessage();
				for (String s : row.keySet()) {
					if (row.get(s).length() > 0) {
						log.error(s + "=" + row.get(s));
						msg += "<br/>" + s + "=" + row.get(s);
					}
				}
				failures.add(new Exception(msg, e));
				log.error("*************** end failed row ******************");
			}
		}

		dataCounts.put("exp-eligible", records.size());
		log.info(records.size() + " total VOs created from EXP data");
		return records;
	}


	/**
	 * tokenizes the tracking# on a comma.  ADAPTIV introduced support for mulitple, but only the first one is the primary.
	 * @param tn
	 * @return
	 */
	private String splitTrackingNo(String tn) {
		if (StringUtil.isEmpty(tn) || tn.indexOf(',') == -1) {
			return tn;
		} else {
			return tn.split(",")[0];
		}
	}


	/**
	 * determines if the assets is visible to our operating companies
	 * @param distChannel
	 * @param allowedOpCoNames
	 * @return
	 */
	protected boolean isOpcoAuthorized(String distChannel, String[] allowedOpCoNames, String tn) {
		//tn is not used here, but is in subclasses
		return StringUtil.stringContainsItem(distChannel, allowedOpCoNames);
	}

	/**
	 * determines if the asset's file type is visible to our operating companies
	 * @param distChannel
	 * @param allowedOpCoNames
	 * @return
	 */
	protected boolean isAssetTypeAuthorized(String assetType, List<String> allowedTypes) {
		return assetType != null && allowedTypes.contains(assetType.toLowerCase());
	}


	/**
	 * Inserts or updates the data in the supplied list of maps into the database
	 * @param data
	 * @return
	 */
	public void saveRecords(Map<String, MediaBinDeltaVO> masterRecords, boolean isInsert) {
		// Build the SQL Statement
		StringBuilder sql = new StringBuilder(350);
		if (isInsert) {
			sql.append("insert into ").append(schema).append("dpy_syn_mediabin ");
			sql.append("(asset_nm, asset_desc, asset_type, body_region_txt, ");
			sql.append("business_unit_nm, business_unit_id, download_type_txt, language_cd, literature_type_txt, ");
			sql.append("modified_dt, file_nm, dimensions_txt, orig_file_size_no, prod_family, ");
			sql.append("prod_nm, revision_lvl_txt, opco_nm, title_txt, tracking_no_txt, ");
			sql.append("import_file_cd, duration_length_no, anatomy_txt, desc_txt, meta_kywds_txt, ");
			sql.append("file_checksum_txt, ecopy_revision_lvl_txt, expiration_dt, dpy_syn_mediabin_id) ");
			sql.append("values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)" );
		} else {
			sql.append("update ").append(schema).append("dpy_syn_mediabin ");
			sql.append("set asset_nm=?, asset_desc=?, asset_type=?, body_region_txt=?, ");
			sql.append("business_unit_nm=?, business_unit_id=?, download_type_txt=?, language_cd=?, literature_type_txt=?, ");
			sql.append("modified_dt=?, file_nm=?, dimensions_txt=?, orig_file_size_no=?, prod_family=?, ");
			sql.append("prod_nm=?, revision_lvl_txt=?, opco_nm=?, title_txt=?, tracking_no_txt=?, ");
			sql.append("import_file_cd=?, duration_length_no=?, anatomy_txt=?, desc_txt=?, ");
			sql.append("meta_kywds_txt=?, file_checksum_txt=?, ecopy_revision_lvl_txt=?, expiration_dt=? ");
			sql.append("where dpy_syn_mediabin_id=?");
		}
		log.debug(sql);

		int cnt = 0;
		for (MediaBinDeltaVO vo : masterRecords.values()) {
			if ((isInsert && vo.getRecordState() != State.Insert) || (!isInsert && vo.getRecordState() != State.Update)) 
				continue;

			//these run as individual statements because it's rare that there's more than a handful to process, and we can capture the row-level failures
			try (PreparedStatement ps  = dbConn.prepareStatement(sql.toString())) {
				ps.setString(1, vo.getAssetNm());
				ps.setString(2, vo.getAssetDesc());
				ps.setString(3, vo.getAssetType());
				ps.setString(4, vo.getBodyRegionTxt());
				ps.setString(5, vo.getBusinessUnitNm());
				ps.setString(6, "" + vo.getBusinessUnitId());
				ps.setString(7, vo.getDownloadTypeTxt());
				ps.setString(8, vo.getLanguageCode());
				ps.setString(9, vo.getLiteratureTypeTxt());
				ps.setTimestamp(10, Convert.getTimestamp(vo.getModifiedDt(), true));
				ps.setString(11, vo.getFileNm());
				ps.setString(12, vo.getDimensionsTxt());
				ps.setInt(13, vo.getFileSizeNo());
				ps.setString(14, vo.getProdFamilyNm());
				ps.setString(15, vo.getProdNm());
				ps.setString(16, vo.getRevisionLvlTxt());
				ps.setString(17, vo.getOpCoNm());
				ps.setString(18, vo.getTitleTxt());
				ps.setString(19, vo.getTrackingNoTxt());
				ps.setInt(20, vo.getImportFileCd());
				ps.setDouble(21, vo.getDuration());
				ps.setString(22, vo.getAnatomy());
				ps.setString(23, vo.getDescription());
				ps.setString(24, vo.getMetaKeywords());
				ps.setString(25, vo.getChecksum());
				ps.setString(26, vo.geteCopyRevisionLvl());
				ps.setDate(27, Convert.formatSQLDate(vo.getExpirationDt()));
				ps.setString(28, vo.getDpySynMediaBinId());
				ps.executeUpdate();
				log.debug((isInsert ? "Inserted: " : "Updated: ") + vo.getDpySynMediaBinId());
				++cnt;
			} catch (SQLException sqle) {
				vo.setRecordState(State.Failed);
				//create a custom exception that contains the data/record, so we can report it in the email
				String msg = sqle.getMessage() + "<br/>" + StringUtil.getToString(vo, false, 0, "<br/>");
				failures.add(new Exception(msg, sqle));
			}
		}

		dataCounts.put((isInsert ? "inserted" : "updated"), cnt);
	}


	/**
	 * Inserts or updates the data in the supplied list of maps into the database
	 * @param data
	 * @return
	 */
	public void deleteRecords(Map<String, MediaBinDeltaVO> masterRecords) {
		int cnt = 0;
		// Build the SQL Statement
		StringBuilder sql = new StringBuilder(350);
		sql.append("delete from ").append(schema).append("dpy_syn_mediabin ");
		sql.append("where dpy_syn_mediabin_id in ('~'");
		for (MediaBinDeltaVO vo : masterRecords.values()) {
			if (vo.getRecordState() == State.Delete) {
				sql.append(",?");
				++cnt;
			}
		}
		sql.append(")");
		log.debug(sql);

		if (cnt > 0) { //don't run the query if we don't need to
			cnt = 0;
			try (PreparedStatement ps  = dbConn.prepareStatement(sql.toString())) {
				for (MediaBinDeltaVO vo : masterRecords.values()) {
					if (vo.getRecordState() == State.Delete) 
						ps.setString(++cnt, vo.getDpySynMediaBinId());
				}
				cnt = ps.executeUpdate();
			} catch (SQLException sqle) {
				failures.add(sqle);
			}
		}

		dataCounts.put("deleted", cnt);
	}


	/**
	 * parses the tracking number from the old MediaBin file format
	 * 
	 * This should be removed once Angie has tracking numbers populated for all legacy INT assets. 
	 * They're the only ones falling-back to Name and max 18 chars.
	 * @param data
	 * @return
	 */ 
	private String loadLegacyTrackingNumberFromFileName(Map<String, String> data) {
		String tn = StringUtil.checkVal(data.get("Name"));
		if (tn.lastIndexOf(".") > -1) 
			tn = tn.substring(0, tn.lastIndexOf("."));

		if (tn.length() > 18) tn = tn.substring(0, 18); //INT assets only use the first 18chars
		return tn;
	}

	/**
	 * Looks at multiple columns and returns when one of the columns has data
	 * @param row
	 * @return
	 */
	private String getProductFamily(Map<String, String> row) {
		String pf = StringUtil.checkVal(row.get("Product Family"));
		if (pf.length() > 0) return pf;

		pf = StringUtil.checkVal(row.get("SOUS - Product Family CMF"));
		if (pf.length() > 0) return pf;

		pf = StringUtil.checkVal(row.get("SOUS - Product Family Spine"));
		if (pf.length() > 0) return pf;

		pf = StringUtil.checkVal(row.get("SOUS - Product Family Trauma"));
		if (pf.length() > 0) return pf;

		return pf;
	}

	/**
	 * Returns the appropriate language code for the language and type
	 * specified.
	 * @param lang
	 * @param type
	 * @return
	 */
	private String parseLanguage(String lang) {
		if (lang == null) return null;
		return languages.get(lang.toUpperCase());
	}

	/**
	 * Loads language map.
	 * These correlate to the values being passed in the EXT files at the time 
	 * of this writing.  Add as necessary.
	 */
	private void loadLanguages() {
		languages.put("CZECH","cs");
		languages.put("DANISH","dk");
		languages.put("DUTCH","nl");
		languages.put("ENGLISH","en");
		languages.put("ESTONIAN","et");
		languages.put("GREEK","gr");
		languages.put("FINNISH","fl");
		languages.put("FRENCH","fr");
		languages.put("GERMAN","de");
		languages.put("HUNGARIAN","hu");
		languages.put("ITALIAN","it");
		languages.put("NORWEGIAN","no");
		languages.put("POLISH","pl");
		languages.put("PORTUGUESE","pt");
		languages.put("RUSSIAN","ru");
		languages.put("SPANISH","es");
		languages.put("SWEDISH","sv");
		languages.put("TURKISH","tr");
	}


	/**
	 * parses one of 4 EXP-file columns into the Body Region field
	 * @param row
	 * @return
	 */
	private String parseBodyRegion(Map<String, String> row) {
		String retVal = StringUtil.checkVal(row.get("Body Region"), null);
		if (retVal != null) return retVal;

		retVal = StringUtil.checkVal(row.get("SOUS - Body Region CMF"), null);
		if (retVal != null) return retVal;

		retVal = StringUtil.checkVal(row.get("SOUS - Body Region Spine"), null);
		if (retVal != null) return retVal;

		retVal = StringUtil.checkVal(row.get("SOUS - Body Region Trauma"), null);
		if (retVal != null) return retVal;

		return "";
	}


	/**
	 * parses keywords for US Vs EMEA into the Keywords field
	 * @param row
	 * @return
	 */
	private String parseKeywords(Map<String, String> row, int type) {
		String retVal = StringUtil.checkVal(row.get("Keywords"));
		if (retVal.length() > 0) return retVal;

		//use EMEA fallback
		if (type == 2) {
			retVal = StringUtil.checkVal(row.get("SOUS - Promotion Material Number"));

			//concat a second value for EMEA when present
			String promo = StringUtil.checkVal(row.get("SOUS - Project number"), null);
			if (promo != null) {
				if (retVal.length() > 0) retVal += ", ";
				retVal += promo;
			}

			retVal = retVal.replaceAll(TOKENIZER, ", ");
		}

		if (retVal.isEmpty()) return null;
		return retVal;
	}

	/**
	 * Sends an email to the person specified in the properties file as to whether 
	 * the insert was a success or a failure.
	 */
	private void sendEmail(long startNano, Map<String, MediaBinDeltaVO> masterRecords) {
		try {
			// Build the email message
			EmailMessageVO msg = new EmailMessageVO(); 
			msg.addRecipients(props.getProperty("adminEmail" + type).split(","));
			String subjectBase = StringUtil.checkVal(props.getProperty("emailSubject"), "SMT MediaBin Import -"); //allow the config file to override the default subject
			String opCo = type == 1 ? " US" : " EMEA";
			if (3 == type) opCo = " EMEA Private Assets";
			msg.setSubject(subjectBase + opCo);
			msg.setFrom("appsupport@siliconmtn.com");

			StringBuilder html= new StringBuilder(1000);
			html.append("<h3>Import File Name: " + importFile + "</h3><h4>");
			html.append("EXP rows: ").append(dataCounts.get("exp-raw")).append("<br/>");
			html.append("Eligible: ").append(dataCounts.get("exp-eligible")).append("<br/><br/>");
			html.append("Existing: ").append(dataCounts.get("existing")).append("<br/>");
			html.append("Added: ").append(dataCounts.get("inserted")).append("<br/>");
			html.append("Updated: ").append(dataCounts.get("updated")).append("<br/>");
			html.append("Deleted: ").append(dataCounts.get("deleted")).append("<br/><br/>");
			html.append("DB Total: ").append(dataCounts.get("total")).append("<br/>");
			if (dataCounts.containsKey("solr")) 
				html.append("Solr Total: ").append(dataCounts.get("solr")).append("<br/>");

			long timeSpent = System.nanoTime()-startNano;
			double millis = timeSpent / (double) 1000000;
			if (millis > (60*1000)) {
				html.append("Execution Time: ").append(Math.round(millis / (60*1000))).append(" minutes");
			} else {
				html.append("Execution Time: ").append(millis / 1000).append(" seconds");
			}
			html.append("</h4>");

			if (failures.size() > 0) {
				html.append("<b>The following issues were reported:</b><br/><br/>");

				// loop the errors and display them
				for (int i=0; i < failures.size(); i++) {
					html.append(failures.get(i).getMessage()).append("<hr/>\r\n");
					log.warn(failures.get(i).getMessage());
				}
			}

			//add-in for showpad stats
			addSupplementalDetails(html);

			//create tables for each of our 3 transition states; Insert, Update, Delete
			addSummaryTable(html, masterRecords, State.Insert);
			addSummaryTable(html, masterRecords, State.Update);
			addSummaryTable(html, masterRecords, State.Delete);

			msg.setHtmlBody(html.toString());

			MailTransportAgentIntfc mail = MailHandlerFactory.getDefaultMTA(props);
			mail.sendMessage(msg);
		} catch (Exception e) {
			log.error("Could not send completion email, ", e);
		}
	}


	/**
	 * @param html
	 */
	protected void addSupplementalDetails(StringBuilder html) {
		//does nothing here, but gets overwritten by the Showpad decorator 
		//to add valueable stats to the admin email
	}


	/**
	 * format the data into a pretty HTML table - to include in the email
	 * @param msg
	 * @param masterRecords
	 * @param st
	 */
	private void addSummaryTable(StringBuilder msg, Map<String, MediaBinDeltaVO> masterRecords, State st) {
		if (masterRecords == null) return; //occurs when we have a fatal exception like can't download EXP file or connect database. -JM 08.25.16

		//first determine if there's any output to actually print
		int cnt = 0;
		for (MediaBinDeltaVO vo : masterRecords.values()) {
			if (st != vo.getRecordState()) continue; //only print the ones we want
			++cnt;
		}
		if (cnt == 0) return;

		msg.append("<h4>").append(st.toString()).append(" Summary</h4>");
		msg.append("<table border='1' width='95%' align='center'><thead><tr>");
		msg.append("<th>SMT Tracking Number</th>");
		msg.append("<th>eCopy Tracking Number</th>");
		msg.append("<th>File Name</th>");
		if (State.Update == st) msg.append("<th>Changes</th>");
		msg.append("</tr></thead>\r<tbody>");

		for (MediaBinDeltaVO vo : masterRecords.values()) {
			if (st != vo.getRecordState()) continue; //only print the ones we want
			msg.append("<tr>");
			//getActionDesc holds an asterisk when we have to re-add records to Solr unexpectedly.  - for SMT monitoring.  -JM 08.04.16
			msg.append("<td nowrap>").append(StringUtil.checkVal(vo.getActionDesc())).append(StringUtil.checkVal(vo.getDpySynMediaBinId())).append("</td>");
			msg.append("<td nowrap>").append(StringUtil.checkVal(vo.getEcopyTrackingNo())).append("</td>");
			if (State.Delete == st) {
				msg.append("<td>").append(vo.getFileNm()).append("</td>");
			} else {
				msg.append("<td><a href=\"").append(vo.getLimeLightUrl()).append("\">").append(vo.getFileNm()).append("</a></td>");
			}
			if (State.Update == st) {
				msg.append("<td>");
				if (vo.deltaList() != null) {
					if (vo.getErrorReason() != null) msg.append("<font color=\"red\">").append(vo.getErrorReason()).append("</font><br/>");
					for (PropertyChangeEvent e : vo.deltaList()) {
						msg.append(e.getPropertyName()).append("<br/>");
						//msg.append("old=").append(e.getOldValue()).append("<br/>");
						//msg.append("new=").append(e.getNewValue()).append("<br/>");
					}
				}
				msg.append("</td>");
			}
			msg.append("</tr>\r");
		}
		msg.append("</tbody></table>");
	}


	public Integer getDataCount(String type) {
		return Convert.formatInteger(dataCounts.get(type));
	}
}
