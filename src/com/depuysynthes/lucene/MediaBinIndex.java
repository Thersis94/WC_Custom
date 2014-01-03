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
import java.util.List;
import java.util.Properties;

// log4j 1.2-15
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
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
	
	protected final String[] ORGANIZATION_IDS = new String[] { "","DPY_SYN", "DPY_SYN_EMEA" }; //array[idx] correspondes to import_file_cd in the DB
	
	
	public MediaBinIndex() {
		log = Logger.getLogger(this.getClass());
        ldm = new DocumentMap();
        fileManager = new FileManager();
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
    	DocumentHandler dh = new DocumentHandlerImpl(ldm.getClassName("pdf"));
        Document doc = null;
        
        for (int i = 0; i < metaData.size(); i++) {
    		MediaBinAssetVO vo = metaData.get(i);
    		String orgId = ORGANIZATION_IDS[vo.getImportFileCd()];
    		
    		byte[] fileBytes = loadFile(vo, fileRepos);
    		if (fileBytes == null || fileBytes.length == 0) continue;
    		
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
    		
    		log.info("adding PDF to index: url=" + vo.getActionUrl() + ", org=" + orgId);
    		try {
	    		doc = dh.getDocument(fileBytes);
	    		doc.add(new StringField(DocumentHandler.ORGANIZATION, 	orgId,				Field.Store.YES));
	    		//doc.add(new StringField(DocumentHandler.COUNTRY, 		country,			Field.Store.YES));
		        doc.add(new StringField(DocumentHandler.LANGUAGE, 		"en",				Field.Store.YES));
		        doc.add(new StringField(DocumentHandler.ROLE, 			"000",				Field.Store.YES));
		        doc.add(new TextField(DocumentHandler.SITE_PAGE_URL,	vo.getActionUrl(),	Field.Store.YES));
		        //doc.add(new TextField(DocumentHandler.DOCUMENT_URL, 	vo.getActionUrl(),	Field.Store.YES));
		        doc.add(new StringField(DocumentHandler.DOCUMENT_ID, 	vo.getDpySynMediaBinId(),	Field.Store.YES));
		        doc.add(new TextField(DocumentHandler.FILE_NAME, 		vo.getFileNm(),		Field.Store.YES));
		        doc.add(new TextField(DocumentHandler.TITLE, 			vo.getTitleTxt(),	Field.Store.YES));
		        doc.add(new TextField(DocumentHandler.SUMMARY, 			summary,			Field.Store.YES));
		        //doc.add(new TextField(DocumentHandler.SECTION,		divisionNm,			Field.Store.YES));
		        doc.add(new StringField(DocumentHandler.MODULE_TYPE,	"DOWNLOAD",			Field.Store.YES));
		        		        
		        Date start = Convert.formatDate(Calendar.getInstance().getTime(), Calendar.MONTH, -1);
	    		Date end = Convert.formatDate(Calendar.getInstance().getTime(), Calendar.MONTH, 1);
		        doc.add(new TextField(DocumentHandler.START_DATE, 	Convert.formatDate(start, Convert.DATE_NOSPACE_PATTERN),		Field.Store.YES));
	            doc.add(new TextField(DocumentHandler.END_DATE,		Convert.formatDate(end, Convert.DATE_NOSPACE_PATTERN),			Field.Store.YES));
	            doc.add(new TextField(DocumentHandler.UPDATE_DATE,	Convert.formatDate(vo.getModifiedDt(), Convert.DATE_NOSPACE_PATTERN),	Field.Store.YES));
		        writer.addDocument(doc);
    		} catch (Exception e) {
    			log.error("Unable to index asset " + vo.getDpySynMediaBinId(),e);
    		}
    	}
    }
    
    private byte[] loadFile(MediaBinAssetVO vo, String fileRepos) {
    	byte[] data = null;
    	try {
    		String fileNm = StringUtil.replace(vo.getRevisionLvlTxt() + "/" + vo.getAssetNm(), "/", File.separator);
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
		sql.append(")");
		log.debug(sql);
		
		int i = 0;
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql.toString());
			for (String at: MediaBinAdminAction.PDF_ASSETS) ps.setString(++i, at);
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
}
