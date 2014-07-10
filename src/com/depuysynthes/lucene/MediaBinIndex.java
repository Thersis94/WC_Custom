package com.depuysynthes.lucene;

// JDK 1.6.x
import java.io.File;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;




// log4j 1.2-15
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;




// SMT Base Libs
import com.depuysynthes.action.MediaBinAdminAction;
import com.depuysynthes.action.MediaBinAssetVO;
import com.siliconmtn.cms.CMSConnection;
import com.siliconmtn.io.FileManager;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.lucene.DocumentHandler;
import com.smt.sitebuilder.search.lucene.DocumentHandlerImpl;
import com.smt.sitebuilder.search.lucene.DocumentMap;
import com.smt.sitebuilder.search.lucene.custom.SMTCustomIndexIntfc;

/****************************************************************************
 * <b>Title</b>: ProductCatalogIndex.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> This class gets invoked by the Lucene Index Builder (batch)
 * It adds the DS product catalogs to the Lucene Indexes to be usable in site search.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since May 19, 2013<p/>
 ****************************************************************************/
public class MediaBinIndex implements SMTCustomIndexIntfc {
	protected Logger log = null;
	private DocumentMap ldm;
	private FileManager fileManager = null;
	private Map<String,String> busUnits = null;
	
	protected final String[] ORGANIZATION_IDS = new String[] { "","DPY_SYN", "DPY_SYN_EMEA" }; //array[idx] correspondes to import_file_cd in the DB
	
	
	public MediaBinIndex() {
		log = Logger.getLogger(this.getClass());
        ldm = new DocumentMap();
        fileManager = new FileManager();
        loadBusUnits();
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.lucene.custom.SMTCustomIndexIntfc#addIndexItems(java.sql.Connection, com.siliconmtn.cms.CMSConnection, org.apache.lucene.index.IndexWriter)
	 */
	@Override
	public void addIndexItems(Connection conn, CMSConnection cmsConn, IndexWriter writer, Properties config) {
		log.info("Indexing DePuySynthes MediaBin PDF assets");
		List<MediaBinAssetVO> metaData = loadMetaData(conn, config.getProperty(Constants.CUSTOM_DB_SCHEMA));
		indexFiles(metaData, writer, StringUtil.checkVal(config.getProperty("mediabinFiles")));
	}
	
    
    /**
     * Flattens out the hierarchy and stores all fields in the content fields
     * @param conn
     * @param orgId
     * @param writer
     */
    protected void indexFiles(List<MediaBinAssetVO> metaData, IndexWriter writer, String fileRepos) {
    	//DocumentHandler dh = new DocumentHandlerImpl(ldm.getClassName("pdf"));
    	DocumentHandler dh = null;
        Document doc = null;
        boolean isVideo = false;
        for (int i = 0; i < metaData.size(); i++) {
    		MediaBinAssetVO vo = metaData.get(i);
    		byte[] fileBytes = null;
    		
    		if (vo.getAssetType().toLowerCase().startsWith("multimedia")) {
    			dh = new DocumentHandlerImpl(ldm.getClassName("video"));
    			isVideo = true;
    		} else {
    			dh = new DocumentHandlerImpl(ldm.getClassName("pdf"));
    			fileBytes = loadFile(vo, fileRepos);
    			if (fileBytes == null || fileBytes.length == 0) continue;
    		}
    		
    		String orgId = ORGANIZATION_IDS[vo.getImportFileCd()];
    		    		
    		//ensure click-to URLs bounce through our redirector for version control.  leading slash added by WC's View.
    		String fileNm = vo.getFileNm();
    		try {
    			fileNm = URLEncoder.encode(vo.getFileNm(), "UTF-8");
    		} catch (Exception e) {}
    		
    		vo.setActionUrl("json?amid=MEDIA_BIN_AJAX&mbid=" + vo.getDpySynMediaBinId() + "&name=" + fileNm);
    		
    		//ensure a decent name is presented
    		if (vo.getTitleTxt() == null || vo.getTitleTxt().length() == 0)
    			vo.setTitleTxt(vo.getLiteratureTypeTxt());
    		
    		String summary = StringUtil.checkVal(vo.getAssetDesc());
    		if (summary.length() == 0) {
    			summary = StringUtil.checkVal(vo.getProdFamilyNm());
    			if (summary.length() > 0) summary += " | ";
    			summary += StringUtil.checkVal(vo.getProdNm());
    		}
    		String fileName = StringUtil.checkVal(vo.getFileNm());
    		int dotIndex = fileName.lastIndexOf(".");
   			log.info("adding '" + vo.getAssetType() + "' to index: url=" + vo.getActionUrl() + ", org=" + orgId);
    		try {
	    		doc = dh.getDocument(fileBytes);
	    		doc.add(new StringField(DocumentHandler.ORGANIZATION, 	orgId,				Field.Store.YES));
	    		//doc.add(new StringField(DocumentHandler.COUNTRY, 		country,			Field.Store.YES));
		        //doc.add(new StringField(DocumentHandler.LANGUAGE, 		"en",				Field.Store.YES));
	    		doc.add(new StringField(DocumentHandler.LANGUAGE, 		vo.getLanguageCode(),				Field.Store.YES));
		        doc.add(new StringField(DocumentHandler.ROLE, 			"000",				Field.Store.YES));
		        doc.add(new TextField(DocumentHandler.SITE_PAGE_URL,	vo.getActionUrl(),	Field.Store.YES));
		        //doc.add(new TextField(DocumentHandler.DOCUMENT_URL, 	vo.getActionUrl(),	Field.Store.YES));
		        doc.add(new StringField(DocumentHandler.DOCUMENT_ID, 	vo.getDpySynMediaBinId(),	Field.Store.YES));
		        doc.add(new TextField(DocumentHandler.FILE_NAME, 		fileName,		Field.Store.YES));
		        doc.add(new IntField(DocumentHandler.FILE_SIZE, 		vo.getFileSizeNo(),		Field.Store.YES));
		        if (fileName.length() > 0 && dotIndex > -1 && (dotIndex + 1) < fileName.length()) {
		        	doc.add(new StringField(DocumentHandler.FILE_EXTENSION, fileName.substring(++dotIndex), Field.Store.YES));	
		        }
		        if (isVideo) {
		        	log.info("video duration (raw) is: " + vo.getDuration()); 
		        	doc.add(new StringField(DocumentHandler.DURATION,  parseDuration(vo.getDuration()), Field.Store.YES));
		        }
		        doc.add(new TextField(DocumentHandler.TITLE, 			vo.getTitleTxt(),	Field.Store.YES));
		        doc.add(new TextField(DocumentHandler.SUMMARY, 			summary,			Field.Store.YES));
		        // SECTION is used for company/division name
	        	doc.add(new TextField(DocumentHandler.SECTION, parseBusinessUnit(vo.getBusinessUnitNm()), Field.Store.YES));
		        
		        // META_KEYWORDS is used to store download text type
	        	doc.add(new TextField(DocumentHandler.META_KEYWORDS, 
	        			parseDownloadType(vo.getDownloadTypeTxt(), isVideo), Field.Store.YES));
		        
		        doc.add(new StringField(DocumentHandler.MODULE_TYPE,	"DOWNLOAD",			Field.Store.YES));
		        		        
		        Date start = Convert.formatDate(Calendar.getInstance().getTime(), Calendar.MONTH, -1);
	    		Date end = Convert.formatDate(Calendar.getInstance().getTime(), Calendar.MONTH, 1);
		        doc.add(new TextField(DocumentHandler.START_DATE,
		        		Convert.formatDate(start, Convert.DATE_NOSPACE_PATTERN),	Field.Store.YES));
	            doc.add(new TextField(DocumentHandler.END_DATE,
	            		Convert.formatDate(end, Convert.DATE_NOSPACE_PATTERN),Field.Store.YES));
	            doc.add(new TextField(DocumentHandler.UPDATE_DATE,
	            		Convert.formatDate(vo.getModifiedDt(),Convert.DATE_NOSPACE_PATTERN),Field.Store.YES));
		        writer.addDocument(doc);
    		} catch (Exception e) {
    			log.error("Unable to index asset " + vo.getDpySynMediaBinId(),e);
    		}
    		
    		// reset isVideo flag
    		isVideo = false;
    	}
    }
    
    private byte[] loadFile(MediaBinAssetVO vo, String fileRepos) {
    	byte[] data = null;
    	try {
    		String fileNm = StringUtil.replace(vo.getRevisionLvlTxt() + "/" + vo.getAssetNm(), "/", File.separator);
    		log.debug("loading file: " + fileRepos + fileNm);
    		data = fileManager.retrieveFile(fileRepos + fileNm);
    	} catch (Exception e) {
    		log.error("could not load file for " + vo.getDpySynMediaBinId(), e);
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
		sql.append("select * from ").append(dbSchema).append("DPY_SYN_MEDIABIN ");
		sql.append("where lower(asset_type) in (null"); //loop all pdf types
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
    	StringBuilder dur = new StringBuilder(5);
    	if (duration > 0) {
    		int hours = (int) duration/3600;
    		int minutes = (int) duration%3600;
    		minutes = minutes/60;
    		if (hours < 10) dur.append("0");
    		dur.append(hours).append(":");
    		if (minutes < 10) dur.append("0");
    		dur.append(minutes);
    	}
    	log.info("video duration parsed is: " + dur.toString());
    	return dur.toString();
    }
    
    /**
     * Parses the business unit name.
     * @param busUnit
     * @return
     */
    private String parseBusinessUnit(String busUnit) {
    	String tmp = StringUtil.checkVal(busUnit).toUpperCase();
    	String newStr = null;

    	if (tmp.contains("BIO")) {
    		newStr = busUnits.get("BIO");
    	} else if (tmp.contains("CMF")) {
    		newStr = busUnits.get("CMF");
    	} else if (tmp.contains("CODMAN")) {
    		newStr = busUnits.get("CODMAN");
    	} else if (tmp.contains("HIP")) {
    		newStr = busUnits.get("HIP");
    	} else if (tmp.contains("KNEE")) {
    		newStr = busUnits.get("KNEE");
    	} else if (tmp.contains("MITEK")) {
    		newStr = busUnits.get("MITEK");
    	} else if (tmp.contains("POWERTOOLS")) {
    		newStr = busUnits.get("TOOLS");
    	} else if (tmp.contains("SHOULDER")) {
    		newStr = busUnits.get("SHOULDER");
    	} else if (tmp.contains("SPINE")) {
    		newStr = busUnits.get("SPINE");
    	} else if (tmp.contains("TRAUMA")) {
    		newStr = busUnits.get("TRAUMA");
    	} else {
    		newStr = busUnits.get("OTHER");
    	}
    	
    	// call method recursively if business unit value contains multiples.
    	if (tmp.indexOf("~") > -1) {
    		if (tmp.length() > 1) {
    			newStr += "|" + parseBusinessUnit(tmp.substring(tmp.indexOf("~")+1));
    		}
    	}
    	
    	return newStr;
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
}
