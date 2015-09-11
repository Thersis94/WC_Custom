package com.depuysynthes.lucene;

// JDK 1.7.x
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.xml.sax.ContentHandler;

// Apche SolrJ 4.9
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;

// Apache Tika 1.5
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;





// SMT Base Libs
import com.depuysynthes.action.MediaBinAdminAction;
import com.depuysynthes.action.MediaBinAssetVO;
import com.depuysynthes.scripts.MediaBinDeltaVO;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SMTAbstractIndex;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
 * <b>Title</b>: MediaBinSolrIndex.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> This class gets invoked by the Solr Index Builder (batch)
 * It adds the DS product catalogs to the Solr Indexes to be usable in site search.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since May 19, 2013<p/>
 * @updates:
 * 	JM 07.16.14
 * 		Added ASSET_LANGUAGE as a non-core language field, so we could use it in View filters w/o affecting the Indexer.
 * JC 09/02/14 
 * 		Copied the file and modified for the Solr Indexer
 * JM 03.03.15
 * 		Implemented purgeIndexItems method from Interface
 ****************************************************************************/
public class MediaBinSolrIndex extends SMTAbstractIndex {
	private Map<String,String> busUnits = null;

	/**
	 * Base url information for the redirection
	 */
	public static final String BASE_REDIR_URL = "/json?amid=MEDIA_BIN_AJAX&mbid=";

	/**
	 * Index type for this index.  This value is stored in the INDEX_TYPE field
	 */
	public static final String INDEX_TYPE = "MEDIA_BIN";

	public enum MediaBinField {
		AssetType("assetType_s"),
		AssetDesc("assetDesc_s"),
		TrackingNo("trackingNumber_s"),
		VideoChapters("videoChapters_s"),
		DownloadType("downloadType_s"),
		DSOrderNo("dsOrderNo_i"),
		ImportFileCd("importFileCd_i");
		MediaBinField(String s) { this.metaDataField = s; }
		private String metaDataField = null;
		public String getField() { return metaDataField; }
	}

	public MediaBinSolrIndex() {
		loadBusUnits();
	}
	
			
	/**
	 * Initializes the Business Units
	 */
	public MediaBinSolrIndex(Properties config) {
		super(config);
		loadBusUnits();
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.lucene.custom.SMTCustomIndexIntfc#addIndexItems(java.sql.Connection, com.siliconmtn.cms.CMSConnection, org.apache.lucene.index.IndexWriter)
	 */
	@Override
	public void addIndexItems(HttpSolrServer server) {
		log.info("Indexing DePuySynthes MediaBin PDF assets");
		List<MediaBinAssetVO> metaData = loadMetaData(dbConn, config.getProperty(Constants.CUSTOM_DB_SCHEMA));
		indexFiles(metaData, server, StringUtil.checkVal(config.getProperty("mediabinFiles")));
	}

	/**
	 * Flattens out the hierarchy and stores all fields in the content fields
	 * @param metaData Collection of meta-data corresponding to each document in the media bin repository
	 * @param server
	 * @param fileRepos
	 */
	public void indexFiles(List<MediaBinAssetVO> metaData, HttpSolrServer server, String fileRepos) {
		int cnt = 0;
		for (int i = 0; i < metaData.size(); i++) {
			SolrInputDocument doc = new SolrInputDocument();
			MediaBinAssetVO vo = metaData.get(i);
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

			// Get the Organization ID
			List<String> opCoList = Arrays.asList(vo.getOpCoNm().split("~"));
			if (opCoList == null || opCoList.size() == 0) continue; //not authorized for any; we should never hit this.
			List<String> orgList  = new ArrayList<String>();
			if (opCoList.contains("INTDS.com")) orgList.add("DPY_SYN_EMEA");
			if (opCoList.contains("DSI.com")) orgList.add("DPY_SYN_INST");
			if (opCoList.contains("USDS.com")) orgList.add("DPY_SYN");

			//ensure click-to URLs bounce through our redirector for version control.  leading slash added by WC's View.
			String fileNm = "&name=" + StringEncoder.urlEncode(vo.getFileNm());
			vo.setActionUrl(BASE_REDIR_URL + vo.getDpySynMediaBinId() + fileNm);

			//ensure a decent name is presented
			if (vo.getTitleTxt() == null || vo.getTitleTxt().length() == 0)
				vo.setTitleTxt(vo.getLiteratureTypeTxt());

			String fileName = StringUtil.checkVal(vo.getFileNm());
			int dotIndex = fileName.lastIndexOf(".");
			log.debug("adding '" + vo.getAssetType() + "' to index: url=" + vo.getActionUrl() + ", org=" + orgList);
			try {
				doc.setField(SearchDocumentHandler.INDEX_TYPE, INDEX_TYPE);
				doc.setField(SearchDocumentHandler.ORGANIZATION, orgList); //multiValue field
				doc.setField(SearchDocumentHandler.LANGUAGE, StringUtil.checkVal(vo.getLanguageCode(), "en"));
				doc.setField(SearchDocumentHandler.ROLE, SecurityController.PUBLIC_ROLE_LEVEL);
				doc.setField(SearchDocumentHandler.SITE_PAGE_URL, vo.getActionUrl()); //need to fix DSI phase 2 code and regression test before removing this
				doc.setField(SearchDocumentHandler.DOCUMENT_URL, vo.getActionUrl());
				doc.setField(SearchDocumentHandler.DOCUMENT_ID, vo.getDpySynMediaBinId());
				doc.setField(SearchDocumentHandler.TITLE, vo.getTitleTxt());
				doc.setField(SearchDocumentHandler.SUMMARY, getSummary(vo));
				doc.setField(SearchDocumentHandler.FILE_NAME, fileName);
				doc.setField(SearchDocumentHandler.FILE_SIZE, vo.getFileSizeNo());
				doc.setField(SearchDocumentHandler.DURATION, parseDuration(vo.getDuration()));
				doc.setField(SearchDocumentHandler.SECTION, parseBusinessUnit(vo.getBusinessUnitNm()));
				//downloadType is used exclusively for DS and EMEA-DS as a cosmetic value
				doc.setField(MediaBinField.DownloadType.getField(), parseDownloadType(vo.getDownloadTypeTxt(), vo.isVideo()));
				
				doc.setField(SearchDocumentHandler.META_KEYWORDS, vo.getMetaKeywords());
				doc.setField(SearchDocumentHandler.MODULE_TYPE, INDEX_TYPE + "_" + (vo.isVideo() ? "VIDEO" : "DOWNLOAD"));
				doc.setField(SearchDocumentHandler.UPDATE_DATE, df.format(vo.getModifiedDt()));
				doc.setField(SearchDocumentHandler.CONTENTS, vo.isVideo() ? "" : parseFile(vo, fileRepos));
				doc.setField(MediaBinField.TrackingNo.getField(), vo.getTrackingNoTxt()); //DSI uses this to align supporting images and tag favorites
				doc.setField(MediaBinField.AssetType.getField(), getAssetType(vo));
				doc.setField(MediaBinField.AssetDesc.getField(), vo.getAssetDesc());
				doc.setField(MediaBinField.DSOrderNo.getField(), vo.isVideo() ? 25 : 30); //used for moduleType sequencing on DS only
				doc.setField(MediaBinField.ImportFileCd.getField(), vo.getImportFileCd());
				if (vo.isVideo())
					doc.setField(MediaBinField.VideoChapters.getField(), vo.getVideoChapters());

				//turn the flat/delimited hierarchy into a structure that PathHierarchyTokenizer will understand
				for (String s : StringUtil.checkVal(vo.getAnatomy()).split("~")) {
					//need to tokenize the levels and trim spaces from each, the MB team are slobs!
					StringBuilder sb = new StringBuilder();
					for (String subStr : s.split(",")) {
						sb.append(StringUtil.checkVal(subStr).trim()).append(SearchDocumentHandler.HIERARCHY_DELIMITER);
					}
					if (sb.length() >= SearchDocumentHandler.HIERARCHY_DELIMITER.length()) 
						sb.deleteCharAt(sb.length()-SearchDocumentHandler.HIERARCHY_DELIMITER.length());
					doc.addField(SearchDocumentHandler.HIERARCHY, sb.toString());
				}

				if (fileName.length() > 0 && dotIndex > -1 && (dotIndex + 1) < fileName.length())
					doc.setField(SearchDocumentHandler.FILE_EXTENSION, fileName.substring(++dotIndex));

				server.add(doc);
				++cnt;
				if ((i % 100) == 0 && i > 0) {
					//server.commit(false, false, true);
					log.info("Added " + i + " records");
				}
			} catch (Exception e) {
				log.error("Unable to index asset " + vo.getDpySynMediaBinId(), e);
			}
		}
		log.info("Added " + cnt + " records");
	}

	private String getAssetType(MediaBinAssetVO vo) {
		if ("multimedia file".equalsIgnoreCase(vo.getAssetType())) {
			return StringUtil.checkVal(vo.getAssetDesc()).toLowerCase();
		} else {
			return vo.getAssetType();
		}
	}

	/**
	 * Figures out the appropriate summary for the given document
	 * @param vo Document Meta-Data
	 * @return
	 */
	private String getSummary(MediaBinAssetVO vo) {
		String summary = "";
		//DSI work-around.  DS.com should never have been using AssetDesc, but 
		//since it was validated that way we can't change it.
		if (vo.getOpCoNm().indexOf("DSI.com") > -1) {
			summary = StringUtil.checkVal(vo.getDescription());
		}

		if (summary.length() == 0) summary = StringUtil.checkVal(vo.getAssetDesc());
		if (summary.length() == 0) {
			summary = StringUtil.checkVal(vo.getProdFamilyNm());
			if (summary.length() > 0) summary += " | ";
			summary += StringUtil.checkVal(vo.getProdNm());
		}

		return summary;
	}

	/**
	 * Parses the file (text or binary) into an indexable String.  This method
	 * calls a detector (based upon the stream to the local file) and auto-detects
	 * the correct parser based upon the file detection.  The data is converted to a 
	 * String Object and returned to the calling class to be added to the index
	 * @param vo MediaBin meta data
	 * @param fileRepos Location of the file data
	 * @return
	 */
	private String parseFile(MediaBinAssetVO vo, String fileRepos) {
		String data = "";
		String fileNm = null;
		try { //catch NPEs in the file name, before we attempt to open the file
			fileNm = StringUtil.replace(vo.getRevisionLvlTxt() + "/" + vo.getAssetNm(), "/", File.separator);
			
			//remain backwards compatible, this class is used by both the V1 and V2 Mediabin importers
			if (vo instanceof MediaBinDeltaVO) fileNm = ((MediaBinDeltaVO)vo).getFileName();
			
		} catch (Exception e) { 
			return data; 
		}

		try (InputStream input = new BufferedInputStream(new FileInputStream(new File(fileRepos + fileNm)))) {
			log.debug("loading file: " + fileRepos + fileNm);
			Metadata metadata = new Metadata();
			Detector detector = new DefaultDetector();
			detector.detect(input, metadata);
			AutoDetectParser adp = new AutoDetectParser(detector);
			ContentHandler handler = new BodyContentHandler(1000*1024*1024);
			adp.parse(input, handler, metadata, new ParseContext());
			data = handler.toString();
		} catch (Exception e) {
			log.error("could not load file for " + vo.getDpySynMediaBinId() + "|" + vo.isVideo());
		}

		return data;
	}


	/**
	 * load a list of assets from the meta-data stored in the database.
	 * Files on the file-system that are not in the retrieved meta-data should be ignored by the indexer. 
	 * @param conn
	 * @param orgId
	 */
	private List<MediaBinAssetVO> loadMetaData(Connection conn, String dbSchema) {
		List<MediaBinAssetVO> data = new ArrayList<MediaBinAssetVO>();
		StringBuilder sql = new StringBuilder();
		sql.append("select * from ").append(dbSchema).append("DPY_SYN_MEDIABIN a ");
		sql.append("left join video_meta_content b on a.dpy_syn_mediabin_id=b.asset_id and b.asset_type='MEDIABIN' ");
		sql.append("where lower(a.asset_type) in (null"); //loop all pdf types
		for (int x=MediaBinAdminAction.PDF_ASSETS.length; x > 0; x--) sql.append(",?");
		for (int y=MediaBinAdminAction.VIDEO_ASSETS.length; y > 0; y--) sql.append(",?");
		sql.append(")");
		log.debug(sql);

		int i = 0;
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql.toString());
			for (String at: MediaBinAdminAction.PDF_ASSETS) ps.setString(++i, at);
			for (String vt: MediaBinAdminAction.VIDEO_ASSETS) ps.setString(++i, vt);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				data.add(new MediaBinAssetVO(rs));

		} catch (SQLException sqle) {
			log.error("could not load MediaBin meta-data from DB", sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}

		log.info("loaded " + data.size() + " records from the meta-data");
		return data;
	}

	/**
	 * Parses the duration (seconds) into a String representing
	 * hours and minutes in HH:MM format.
	 * @param duration
	 * @return
	 */
	private String parseDuration(double duration) {
		if (duration == 0) return "";
		StringBuilder dur = new StringBuilder();
		int hours = (int) duration / 3600;
		int minutes = (int) (duration % 3600) / 60;
		int seconds = (int) (duration % 3600) % 60;

		//only include hours if needed
		if (hours > 0) {
			if (hours < 10) dur.append("0");
			dur.append(hours).append(":");
		}

		//append the minutes
		if (minutes < 10) dur.append("0");
		dur.append(minutes).append(":");

		//append the seconds
		if (seconds < 10) dur.append("0");
		dur.append(seconds);

		log.debug("video duration parsed is: " + dur.toString());
		return dur.toString();
	}


	/**
	 * Parses the source business unit name into a consistent naming schema using 
	 * based on the key/value pairs in the business unit map.  The source name is
	 * split on a tilde and each token is parsed according to the business unit map.
	 * The final result is a single or pipe-delimited value.  Empty tokens are ignored.  
	 * @param busUnit
	 * @return
	 */
	private List<String> parseBusinessUnit(String busUnit) {
		String tmp = StringUtil.checkVal(busUnit).toUpperCase();
		String[] tokens = tmp.split("~");
		Set<String> data = new HashSet<>();

		for (int i = 0; i < tokens.length; i++) {
			// skip token if empty
			if (tokens[i].length() == 0) {
				continue;
			} else if (tokens[i].contains("BIO")) {
				data.add(busUnits.get("BIO"));
			} else if (tokens[i].contains("CMF")) {
				data.add(busUnits.get("CMF"));
			} else if (tokens[i].contains("CODMAN")) {
				data.add(busUnits.get("CODMAN"));
			} else if (tokens[i].contains("HIP")) {
				data.add(busUnits.get("HIP"));
			} else if (tokens[i].contains("KNEE")) {
				data.add(busUnits.get("KNEE"));
			} else if (tokens[i].contains("MITEK")) {
				data.add(busUnits.get("MITEK"));
			} else if (tokens[i].contains("POWERTOOLS")) {
				data.add(busUnits.get("TOOLS"));
			} else if (tokens[i].contains("SHOULDER")) {
				data.add(busUnits.get("SHOULDER"));
			} else if (tokens[i].contains("SPINE")) {
				data.add(busUnits.get("SPINE"));
			} else if (tokens[i].contains("TRAUMA")) {
				data.add(busUnits.get("TRAUMA"));
			} else {
				data.add(busUnits.get("OTHER"));
			}
		}
		return new ArrayList<String>(data);
	}

	/**
	 * Parses download type and returns the download type text either as an empty String,
	 * a single-value String, or a comma-delimited String.
	 * @param downloadType
	 * @param isVideo
	 * @return
	 */
    private String parseDownloadType(String downloadType, boolean isVideo) {
    	String tmp = StringUtil.checkVal(downloadType).replace("~", ",");
    	if (isVideo) {
    		// this is a video, set the type as 'Video' or add 'Video' to the existing type(s).
    		if (tmp.length() == 0) {
    			tmp = "Video";
    		} else {
    			if (! tmp.toLowerCase().contains("video")) tmp += ",Video";
    		}
    	} else {
    		// not a video, set type as 'Other' if no type was supplied.
    		if (tmp.length() == 0) {
    			tmp = "Other";
    		}
    	}
    	return tmp;
    }

	/**
	 * Helper method for loading the business units look-up map.  This map provides
	 * consistent business unit values for indexing by business unit name.
	 */
	private void loadBusUnits() {
		busUnits = new HashMap<>();
		busUnits.put("BIO", "Biomaterials");
		busUnits.put("CMF", "CMF");
		busUnits.put("CODMAN", "Codman Neuro");
		busUnits.put("HIP", "Hip Reconstruction");
		busUnits.put("KNEE", "Knee Reconstruction");
		busUnits.put("MITEK", "Mitek Sports Medicine");
		busUnits.put("SHOULDER", "Shoulder Reconstruction");
		busUnits.put("SPINE", "Spine");
		busUnits.put("TOOLS", "Power Tools");
		busUnits.put("TRAUMA", "Trauma");
		busUnits.put("OTHER", "Other");
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTAbstractIndex#getIndexType()
	 */
	@Override
	public String getIndexType() {
		return MediaBinSolrIndex.INDEX_TYPE;
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#purgeIndexItems(org.apache.solr.client.solrj.impl.HttpSolrServer)
	 */
	@Override
	public void purgeIndexItems(HttpSolrServer server) throws IOException {
		try {
			server.deleteByQuery(SearchDocumentHandler.INDEX_TYPE + ":" + getIndexType());
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
}
