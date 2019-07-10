package com.mts.util;

// JDK 1.8.x
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Log4j 1.2.17
import org.apache.log4j.Logger;

import com.mts.publication.data.AssetVO;
import com.mts.publication.data.AssetVO.AssetType;
// MTS Libs
import com.mts.publication.data.CategoryVO;
import com.mts.publication.data.IssueVO;
import com.mts.publication.data.MTSDocumentVO;

// WC Libs
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.html.tool.HTMLFeedParser;
import com.siliconmtn.util.ClassUtils;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.content.DocumentVO;
import com.smt.sitebuilder.action.metadata.WidgetMetadataVO;

/****************************************************************************
 * <b>Title</b>: DataMigrationUtil.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Importer of MTS Core data 
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 1, 2019
 * @updates:
 ****************************************************************************/

public class DataMigrationUtil {

	private static final Logger log = Logger.getLogger(DataMigrationUtil.class);
	private Map<String, String> userMap = new HashMap<>();
	private Map<String, String> catMap = new HashMap<>();
	private Set<String> missingCats = new HashSet<>();
	//the xr relationships between the articles and the categories
	private Set<WidgetMetadataVO> categories = new HashSet<>();
	
	private List<AssetVO> assets = new ArrayList<>();
	
	// Static Constants
	private static final String CUSTOM_SCHEMA = "custom.";
	private static final String ISSUE_KEY = "issue";
	private static final String PRI_CATEGORY_KEY = "article_category";
	private static final String ISSUE_COVER_IMAGE = "issue_cover_image";
	private static final String ISSUE_PDF = "issue_pdf";
	private static final String MTS_ORG = "MTS";
	private static final String MTS_DOC_FOLDER = "MED_TECH_STRATEGIST";
	private static final String CREATE_BY_ID = "MTS_USER_1";
	private static final String HTTPS_MTS_URL = "https://www.innovationinmedtech.com/wp-content/uploads/";
	private static final String HTTP_MTS_URL = "http://www.innovationinmedtech.com/wp-content/uploads/";
	private static final String BINARY_MTS_URL = "/binary/org/MTS/uploads/";
	
	
	// Turn on/off items to process
	private static final boolean PROC_CAT = false;
	private static final boolean PROC_DOCS = true;
	private static final boolean PROC_DOC_CAT = true;
	private static final boolean PROC_ASSET = true;
	private static final boolean PROC_ISSUES = true;
	private static final boolean PROC_ART = true;
	
	
	/**
	 * 
	 */
	public DataMigrationUtil() {
		super();
		assignMaps();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		log.info("Starting Migration");
		
		DataMigrationUtil dmu = new DataMigrationUtil();
		Connection srcConn = dmu.getSourceConnection();
		Connection destConn = dmu.getDestConnection();
		
		// Migrate the cats
		if (PROC_CAT) dmu.migrateCategories(srcConn, destConn);
		log.info("Categories Processed");
		
		// Migrate the issues
		if (PROC_ISSUES) dmu.migrateIssues(srcConn, destConn);
		log.info("Issues Processed");
		
		// Migrate the articles
		List<MTSDocumentVO> docs = new ArrayList<>();
		if (PROC_ART) docs = dmu.migrateArticles(srcConn);
		DBProcessor dbCore = new DBProcessor(destConn, "core.");
		dbCore.setGenerateExecutedSQL(true);
		DBProcessor dbCustom = new DBProcessor(destConn, CUSTOM_SCHEMA);
		dbCustom.setGenerateExecutedSQL(true);
		int errorCount = 0;
		for(MTSDocumentVO doc : docs) {
			
			if(StringUtil.isEmpty(doc.getActionName()) && StringUtil.isEmpty(doc.getDocument())) {
				log.info("skip save on empty document with id of: " + doc.getActionId());
				errorCount++;
				continue;
			}
			
			try {
				// Assign the SB Action entry
				SBActionVO avo = new SBActionVO(doc);
				if (PROC_DOCS) dbCore.insert(avo);
				
				// Assign the wc document entry
				DocumentVO dvo = (DocumentVO)ClassUtils.cloneBean(doc, true);
				if (PROC_DOCS) dbCore.insert(dvo);
				
				// Assign the MTS Document info
				if (PROC_DOCS) dbCustom.insert(doc);
			} catch (Exception e) {
				log.error("Core: " + dbCore.getExecutedSql());
				log.error("Custom: " + dbCustom.getExecutedSql());
				log.error("Unable to add documents: " + doc.getActionId() + "|" + doc.getActionName(), e);
			}
		}
		log.info("Documents Processed: " + docs.size());
		log.info("errors while processing docs " + errorCount);
		
		// Add the categories and assets
		if (PROC_DOC_CAT) dbCore.executeBatch(new ArrayList<>(dmu.categories), true);
		log.info("Document Categories Processed");
		
		if (PROC_ASSET) dbCustom.executeBatch(dmu.assets, true);
		log.info("Assets Processed");
		
		srcConn.close();
		destConn.close();
		log.info("Migration Completed");
	}
	
	/**
	 * Assigns the categories and users for mapping
	 */
	public void assignMaps() {
		// User Mapping
		userMap.put("kayleen brown", "MTS_USER_1");
		userMap.put("david filmore", "MTS_USER_2");
		userMap.put("tracy neilssien", "MTS_USER_6");
		userMap.put("tracy schaaf", "MTS_USER_6");
		userMap.put("nancy dvorin", "MTS_USER_3");
		userMap.put("david cassak", "MTS_USER_7");
		userMap.put("stephen levin", "MTS_USER_8");
		userMap.put("wendy diller", "MTS_USER_9");
		userMap.put("mary stuart", "MTS_USER_10");
		userMap.put("mary thompson", "MTS_USER_11");
		userMap.put("colin miller", "MTS_USER_12");
		
		// Category mapping
		catMap.put("business strategies", "BUS_STRAT");
		catMap.put("emerging markets", "EMERG_MARKETS");
		catMap.put("executive interviews", "EXECUTIVE_INTERVIEWS");
		catMap.put("executive interview", "EXECUTIVE_INTERVIEWS");
		catMap.put("investors & dealmaking", "INVENT_DEALS");
		catMap.put("perspective & commentary", "PERSPECTIVE_COMM");
		catMap.put("perspective", "PERSPECTIVE_COMM");
		catMap.put("under the lens", "PERSPECTIVE_COMM");
		catMap.put("providers & payors", "PROVIDER_PAYER");
		catMap.put("regulatory & reimbursement", "REG_REIMBURS");
		catMap.put("startups to watch", "STARTUP_WATCH");
		catMap.put("startups to watc", "STARTUP_WATCH");
		catMap.put("aesthetics/dermatology", "aestheticsdermatology");
		catMap.put("dermatology", "aestheticsdermatology");
		catMap.put("aesthetics", "aestheticsdermatology");
		catMap.put("biopharma/biomaterials", "biopharmabiomaterials");
		catMap.put("biomaterials", "biopharmabiomaterials");
		catMap.put("biopharma", "biopharmabiomaterials");
		catMap.put("cardiovascular/vascular", "cardiovascularvascular");
		catMap.put("cardiovascular", "cardiovascularvascular");
		catMap.put("vascular", "cardiovascularvascular");
		catMap.put("cardiovascular monitoring", "cardiovascularvascular");
		catMap.put("heart failure", "cardiovascularvascular");
		catMap.put("critical care/emergency medicine/icu", "critical-careemergency-medicine");
		catMap.put("emergency medicine", "critical-careemergency-medicine");
		catMap.put("icu", "critical-careemergency-medicine");
		catMap.put("critical care", "critical-careemergency-medicine");
		catMap.put("diabetes", "diabetes");
		catMap.put("diabetes devices","diabetes");
		catMap.put("diagnostics", "diagnostics");
		catMap.put("digital health", "digital-health");
		catMap.put("gastroenterology", "gastroenterology");
		catMap.put("imaging", "imaging");	
		catMap.put("neurology/neurovascular", "neurologyneurovascular");
		catMap.put("neurology", "neurologyneurovascular");
		catMap.put("neurovascular", "neurologyneurovascular");
		catMap.put("oncology", "oncology");
		catMap.put("ophthalmology", "ophthalmology");
		catMap.put("orthopedics/spine", "orthopedicsspine");
		catMap.put("orthopedics","orthopedicsspine");
		catMap.put("spine industry","orthopedicsspine");
		catMap.put("spine","orthopedicsspine");
		catMap.put("patient monitoring", "patient-monitoring");
		catMap.put("regenerative medicine/cell therapy", "regenerative-medicine-therapy");
		catMap.put("regenerative medicine", "regenerative-medicine-therapy");
		catMap.put("cell therapy", "regenerative-medicine-therapy");
		catMap.put("respiratory/rnt", "respiratory-ent");
		catMap.put("rnt", "respiratory-ent");
		catMap.put("respiratory", "respiratory-ent");
		catMap.put("surgery/robotics", "surgery-robotics");
		catMap.put("surgery robotics", "surgery-robotics");
		catMap.put("surgery", "surgery-robotics");
		catMap.put("robotics", "surgery-robotics");
		catMap.put("urology/renal", "urology-renal");
		catMap.put("renal", "urology-renal");
		catMap.put("urology", "urology-renal");
		catMap.put("women's health", "womens-health");
		catMap.put("wound management", "wound-management");
		catMap.put("aestheticsdermatology", "aestheticsdermatology");
		catMap.put("biopharmabiomaterials", "biopharmabiomaterials");
		catMap.put("business-strategies", "BUS_STRAT");
		catMap.put("cardiovascularvascular", "cardiovascularvascular");
		catMap.put("critical-careemergency-medicine", "critical-careemergency-medicine");
		catMap.put("diabetes", "diabetes");
		catMap.put("diagnostics", "diagnostics");
		catMap.put("digital-health", "digital-health");
		catMap.put("emerging-markets", "EMERG_MARKETS");
		catMap.put("executive-interviews", "EXECUTIVE_INTERVIEWS");
		catMap.put("gastroenterology", "gastroenterology");
		catMap.put("imaging", "imaging");
		catMap.put("investorsdealmaking", "INVENT_DEALS");
		catMap.put("neurologyneurovascular", "neurologyneurovascular");
		catMap.put("oncology", "oncology");
		catMap.put("ophthalmology", "ophthalmology");
		catMap.put("orthopedicsspine", "orthopedicsspine");
		catMap.put("patient-monitoring", "patient-monitoring");
		catMap.put("perspective-commentary", "PERSPECTIVE_COMM");
		catMap.put("providerspayors", "PROVIDER_PAYER");
		catMap.put("regenerative-medicinecell-therapy", "regenerative-medicine-therapy");
		catMap.put("regulatory-reimbursement", "REG_REIMBURS");
		catMap.put("respiratory-ent", "respiratory-ent");
		catMap.put("start-ups-to-watch", "STARTUP_WATCH");
		catMap.put("surgery-robotics", "surgery-robotics");
		catMap.put("urology-renal", "urology-renal");
		catMap.put("womens-health", "womens-health");
		catMap.put("wound-management", "wound-management");
		
		
	}
	
	/**
	 * 
	 * @param srcConn
	 * @param destConn
	 * @throws SQLException
	 * @throws com.siliconmtn.db.util.DatabaseException
	 */
	public List<MTSDocumentVO> migrateArticles(Connection srcConn) 
	throws SQLException {
		StringBuilder sql = new StringBuilder(128);
		HTMLFeedParser fp = new HTMLFeedParser();
		
		// Get the articles
		sql.append("select  * ");
		sql.append("from wp_1fvbn80q5v_posts where post_type = 'article' ");
		
		List<MTSDocumentVO> docs = new ArrayList<>();
		try (PreparedStatement ps = srcConn.prepareStatement(sql.toString()); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				
				MTSDocumentVO doc = new MTSDocumentVO();
				doc.setActionId(rs.getString("ID"));
				doc.setActionGroupId(rs.getString("ID"));
				doc.setDocumentId(rs.getString("ID"));
				doc.setDocumentSourceCode("CUSTOMER_IMPORT");
				
				if(! StringUtil.isEmpty(rs.getString("post_content"))) {
					doc.setDocument(fp.replaceDocumentPaths(HTTPS_MTS_URL, BINARY_MTS_URL, fp.replaceDocumentPaths(HTTP_MTS_URL, BINARY_MTS_URL, StringUtil.checkVal(rs.getString("post_content")))));
				}else {
					doc.setDocument(rs.getString("post_content"));
				}
				
				
				doc.setDocumentFolderId(MTS_DOC_FOLDER);
				doc.setFileType("html");
				doc.setUniqueCode(RandomAlphaNumeric.generateRandom(6).toUpperCase());
				doc.setOrganizationId(MTS_ORG);
				doc.setModuleTypeId("DOCUMENT");
				doc.setActionDesc(rs.getString("post_title"));
				doc.setActionName(rs.getString("post_title"));
				doc.setPendingSyncFlag(0);
				doc.setCreateById("MTS_USER_1");
				doc.setDirectAccessPath(rs.getString("post_name"));
				doc.setCreateDate(rs.getDate("post_date"));
				doc.setPublishDate(rs.getDate("post_date"));
				doc.setFileSize(StringUtil.checkVal(doc.getDocument()).length());
				
				// Parse the title
				this.parseArticleTitle(doc);
				
				// Load the other categories
				getArticleMetaData(srcConn, doc);
				/*log.info("###");*/
				
				// Get the article Categories
				getArticleCategories(srcConn, doc);
				
				// Add the document to the collection
				docs.add(doc);
			}
		}
		
		return docs;
	}
	
	/**
	 * Maps the categories (Channels and markets) as well as issue
	 * @param srcConn
	 * @param doc
	 * @throws SQLException
	 */
	public void getArticleCategories(Connection srcConn, MTSDocumentVO doc) throws SQLException {
				
		StringBuilder cat = new StringBuilder();
		cat.append("select b.taxonomy, c.slug, b.term_id as issue_id from wp_1fvbn80q5v_term_relationships a ");
		cat.append("inner join wp_1fvbn80q5v_term_taxonomy b on a.term_taxonomy_id = b.term_taxonomy_id ");
		cat.append("inner join wp_1fvbn80q5v_terms c on b.term_id = c.term_id ");
		cat.append("where object_id = ? and taxonomy in ('article_category', 'issue') ");
		cat.append("order by taxonomy DESC");
		
		try(PreparedStatement ps = srcConn.prepareStatement(cat.toString())) {
			ps.setInt(1, Convert.formatInteger(doc.getActionId()));
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				switch (rs.getString("taxonomy")) {
					case ISSUE_KEY :
						doc.setIssueId(rs.getString("issue_id"));
						break;
					case PRI_CATEGORY_KEY :
						WidgetMetadataVO dcat = new WidgetMetadataVO();
						dcat.setSbActionId(doc.getActionId());
						dcat.setWidgetMetadataId(catMap.get(StringUtil.checkVal(rs.getString("slug"))));
						dcat.setCreateDate(new Date());
						dcat.setCreateById(CREATE_BY_ID);
						categories.add(dcat);
						break;
				}
			}
		}
	}
	
	/**
	 * Gets the article pdfs and 
	 * @param srcConn
	 * @param doc
	 * @throws SQLException
	 */
	public void getArticleMetaData(Connection srcConn, MTSDocumentVO doc) throws SQLException {
		String s = "select  * from wp_1fvbn80q5v_posts where post_parent = ? ";
		try(PreparedStatement ps = srcConn.prepareStatement(s)) {
			ps.setString(1, doc.getActionId());
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				AssetVO avo = new AssetVO();
				String targetUrl = StringUtil.checkVal(rs.getString("guid"));
				String completeUrl = targetUrl.replaceAll(HTTP_MTS_URL, BINARY_MTS_URL).replaceAll(HTTPS_MTS_URL, BINARY_MTS_URL);

				if ("application/pdf".equalsIgnoreCase(rs.getString("post_mime_type"))) {
					String docName = StringUtil.isEmpty(rs.getString("post_name")) ? rs.getString("post_name") : "unnamed-pdf";
					avo.setDocumentName(docName);
					avo.setObjectKeyId(doc.getDocumentId());
					avo.setDocumentAssetId(rs.getInt("ID")+"");
					avo.setAssetType(AssetType.PDF_DOC);
					avo.setDocumentPath(completeUrl);
					assets.add(avo);
				}else if("image/jpeg".equalsIgnoreCase(rs.getString("post_mime_type"))){
					String docName = StringUtil.isEmpty(rs.getString("post_name")) ? rs.getString("post_name") : "unnamed-image";
					avo.setDocumentName(docName);
					avo.setObjectKeyId(doc.getDocumentId());
					avo.setDocumentAssetId(rs.getInt("ID")+"");
					avo.setAssetType(AssetType.GEN_IMAGE);
					avo.setDocumentPath(completeUrl);	
					assets.add(avo);
				}
			}
		}
	}
	
	/**
	 * 
	 * @param srcConn
	 * @param issue
	 * @throws SQLException
	 */
	public void getIssueMetaData(Connection srcConn, IssueVO issue) throws SQLException {
		StringBuilder s = new StringBuilder(200);
		s.append("select  *, post_mime_type from wp_1fvbn80q5v_termmeta a ");
		s.append("inner join wp_1fvbn80q5v_posts b on a.meta_value = b.id ");
		s.append("where term_id = ? and meta_key in ('issue_pdf', 'issue_cover_image');");
		
		try(PreparedStatement ps = srcConn.prepareStatement(s.toString())) {
			ps.setString(1, issue.getIssueId());
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				AssetVO avo = new AssetVO();
				String targetUrl = StringUtil.checkVal(rs.getString("guid"));
				String completeUrl = targetUrl.replaceAll(HTTP_MTS_URL, BINARY_MTS_URL).replaceAll(HTTPS_MTS_URL, BINARY_MTS_URL);
				
				switch (rs.getString(1)) {
					case ISSUE_COVER_IMAGE:
						String coverName = StringUtil.isEmpty(rs.getString("post_name")) ? rs.getString("post_name") : "unnamed-cover-img";
						avo.setDocumentName(coverName);
						avo.setObjectKeyId(issue.getIssueId());
						avo.setDocumentAssetId(rs.getInt("ID")+"");
						avo.setAssetType(AssetType.COVER_IMG);
						avo.setDocumentPath(completeUrl);
						assets.add(avo);
						
						break;
					case ISSUE_PDF:
						String pdfName = StringUtil.isEmpty(rs.getString("post_name")) ? rs.getString("post_name") : "unnamed-issue-pdf";
						avo.setDocumentName(pdfName);
						avo.setObjectKeyId(issue.getIssueId());
						avo.setDocumentAssetId(rs.getInt("ID")+"");
						avo.setAssetType(AssetType.PDF_DOC);
						assets.add(avo);
						break;
					default:
						break;
				}
			}
		}
	}
	
	/**
	 * Grabs the category and the title and the author
	 * @param doc
	 */
	public void parseArticleTitle(MTSDocumentVO doc) {
		
		
		String title = doc.getActionName();
		
		if (StringUtil.isEmpty(title)) {
			log.error("cant save this doc:  " + doc.getDocumentId());
			return;
		}
		
		int idx = getIDX(title);
				
		// Get the first section for the cat
		if (idx > -1) {
			String cat = StringUtil.removeNonAlphaNumeric(title.substring(0, idx), false).toLowerCase().trim();
				String catCode = catMap.get(cat);
				
				if (catCode != null) {
					WidgetMetadataVO dcat = new WidgetMetadataVO();
					dcat.setSbActionId(doc.getActionId());
					dcat.setWidgetMetadataId(catCode);
					dcat.setCreateById(CREATE_BY_ID);
					dcat.setCreateDate(new Date());
					categories.add(dcat);
				} else {
					WidgetMetadataVO dcat = new WidgetMetadataVO();
					dcat.setSbActionId(doc.getActionId());
					dcat.setWidgetMetadataId("PERSPECTIVE_COMM");
					dcat.setCreateById(CREATE_BY_ID);
					dcat.setCreateDate(new Date());
					categories.add(dcat);
					
					if (! StringUtil.isEmpty(cat)) missingCats.add(cat);
				}
			
		}
		
		// Get the title
		String pTitle = "";
		
		if (title.indexOf(" by ") > -1) {
			//clip the cats off the front
			if(idx > -1) {
				pTitle = title.substring(idx + 2);
			}else {
				pTitle = title;
			}
			
			int eIdx = pTitle.lastIndexOf("by");
			if (eIdx > -1) {
				//clip the by off the back
				pTitle = pTitle.substring(0, eIdx).trim();
				
			}
		} else {
			if(idx > -1) {
				//clip the cats off the front
				pTitle = title.substring(idx + 1);
			}else {
				//do nothing as the there were no cats or by line
				pTitle = title;
			}
		}

		//remove all double quotes and trim.
		pTitle = pTitle.replaceAll("[\"\u201c\u201d]", "").trim();
		//if the last char is a , remove it
		if (",".equals(pTitle.substring(pTitle.length() - 1))){
			pTitle = pTitle.substring(0, pTitle.length() - 1);
		}

		doc.setActionName(pTitle);

		// Get the author
		int aidx = title.lastIndexOf(" by ");
		if (aidx > -1) {
			String author = StringUtil.checkVal(title.substring(aidx + 4));
			doc.setAuthorId(userMap.get(author.trim().toLowerCase()));
		}

	}
	
	/**
	 * calculates the index where the title starts
	 * @param title 
	 * @return
	 */
	private int getIDX(String title) {
		int idx = title.indexOf('–');
		if (idx < 0) idx = title.indexOf('—');
		if (idx < 0) idx = title.indexOf('-');
		if (idx < 0) idx = title.indexOf(':');
		
		
		if (title.indexOf("Arrhythmia Monitoring -") > -1 ) {
			idx = 23;
		}
		if (title.indexOf("“Digital Innovation: ") > -1 ) {
			idx = 20;
		}
		if (title.indexOf("Start-Ups to Watch:") > -1 || title.indexOf("Start-Ups to Watch –") > -1 || title.indexOf("Start-Ups to Watch -") > -1 || title.indexOf("Start-Ups To Watch:") > -1 || title.indexOf("Start-Ups To Watch -") > -1    ) {
			idx = 18;
		} 
		if (title.indexOf("Industry Spotlight:") > -1 || title.indexOf("Technology Trends:") > -1 || title.indexOf("Conference Preview:") > -1  ) {
			idx = 19;
		}
		if (title.indexOf("Industry Outlook: ") > -1 ) {
			idx = 17;
		}
		if (title.indexOf("Under the Lens:") > -1 || title.indexOf("Market Outlook:") > -1 ) {
			idx = 15;
		}
		if (title.indexOf("Market Update:") > -1 ) {
			idx = 14;
		}
		if (title.indexOf("Inside Story:") > -1 ) {
			idx = 13;
		}
		if (title.indexOf("Perspective:") > -1 ) {
			idx = 12;
		}
		if (title.indexOf("\"In Brief:") > -1 ) {
			idx = 10;
		}
		if (title.indexOf("In Brief:") > -1 ) {
			idx = 9;
		}
		if (title.indexOf("Special Start-Up Offer -") > -1 || title.indexOf("In Memoriam:") > -1 || title.indexOf("EMT Preview:") > -1 ) {
			idx = -1;
		}
		
		return idx;
	}

	/**
	 * Migrate the issues for a given article
	 * @param srcConn
	 * @param destConn
	 * @throws SQLException
	 * @throws com.siliconmtn.db.util.DatabaseException
	 */
	public void migrateIssues(Connection srcConn, Connection destConn) 
	throws SQLException {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select * from wp_1fvbn80q5v_term_taxonomy a ");
		sql.append("inner join wp_1fvbn80q5v_terms b on a.term_id = b.term_id ");
		sql.append("where taxonomy = 'issue'  ");
		sql.append("order by a.term_id, name ");
		
		List<IssueVO> issues = new ArrayList<>();
		try (PreparedStatement ps = srcConn.prepareStatement(sql.toString()); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				IssueVO issue = new IssueVO();
				String name = StringUtil.checkVal(rs.getString("name"));
				
				int index = name.lastIndexOf('-');
				if (index == -1) index = name.indexOf(',', name.indexOf("No."));
				if (index > -1) {
					issue.setIssueDate(Convert.formatDate(Convert.DATE_FULL_MONTH, name.substring(index + 2)));
					issue.setCreateDate(issue.getIssueDate());
				}
				
				if (name.contains(",")) {
					String[] items = name.split("\\,");
					for (String item : items) {
						
						if (StringUtil.checkVal(item).toLowerCase().contains("vol.")) {
							issue.setVolume(StringUtil.removeNonNumeric(item));
						} else if (StringUtil.checkVal(item).toLowerCase().contains("no.")) {
							if (item.indexOf('-') > -1)
								item = StringUtil.removeNonNumeric(item.substring(0, item.indexOf('-')));
							issue.setNumber(StringUtil.removeNonNumeric(item));
						} 
					}
				}
				
				issue.setIssueId(rs.getString("term_id"));
				issue.setPublicationId(MTS_DOC_FOLDER);
				issue.setName(name);
				issue.setEditorId("MTS_USER_3");
				issue.setSeoPath(rs.getString("slug"));
				issue.setApprovalFlag(1);
				
				// Add the Issue Metadata
				getIssueMetaData(srcConn, issue);
				
				issues.add(issue);
			}
		}
		
		
		log.info("Number of issues: " + issues.size());
		DBProcessor db = new DBProcessor(destConn, CUSTOM_SCHEMA);
		
		try {
			db.executeBatch(issues, true);
		} catch (Exception e) {
			throw new SQLException(e);
		}
	}
	
	/**
	 * Pulls the categories from the WP DB and copies to WC
	 * 
	 * @param srcConn
	 * @param destConn
	 * @throws SQLException
	 * @throws com.siliconmtn.db.util.DatabaseException
	 */
	public void migrateCategories(Connection srcConn, Connection destConn) 
	throws SQLException {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select * from wp_1fvbn80q5v_term_taxonomy a ");
		sql.append("inner join wp_1fvbn80q5v_terms b on a.term_id = b.term_id ");
		sql.append("where taxonomy = 'article_category' ");
		sql.append("order by name");
		//TODO  this will need updated if we use it too add the top level and sencond level categories
		List<CategoryVO> cats = new ArrayList<>();
		try (PreparedStatement ps = srcConn.prepareStatement(sql.toString()); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				CategoryVO cat = new CategoryVO();
				cat.setCategoryCode(rs.getString("term_id"));
				//TODO need to lookup if market or channel
				cat.setGroupCode("MARKETS");
				cat.setParentCode("MARKETS");
				cat.setSlug(rs.getString("slug"));
				cat.setDescription(rs.getString("description"));
				cat.setName(rs.getString("name"));
				
				cats.add(cat);
			}
			
			log.info("Number of entries: " + cats.size());
			DBProcessor db = new DBProcessor(destConn, CUSTOM_SCHEMA);
			try {
				db.executeBatch(cats, true);
			} catch (Exception e) {
				throw new SQLException(e);
			}
		}
	}
	
	/**
	 * Gets the connection to the MySQL Source
	 * @return
	 * @throws InvalidDataException 
	 * @throws DatabaseException 
	 */
	public Connection getSourceConnection() throws DatabaseException {
		DatabaseConnection dc = new DatabaseConnection();
		dc.setDriverClass("com.mysql.cj.jdbc.Driver");
		dc.setUrl("jdbc:mysql://playstation:3306/medtechinno?useUnicode=true&characterEncoding=UTF8&zeroDateTimeBehavior=convertToNull&useOldAliasMetadataBehavior=true");
		dc.setUserName("smtdev");
		dc.setPassword("smtrul3s");
		Connection conn = null;
		
		try {
			conn = dc.getConnection();
		} catch (Exception e) {
			throw new DatabaseException(e);
		}
		
		return conn;
	}

	/**
	 * Gets the connection to the MySQL Source
	 * @return
	 * @throws InvalidDataException 
	 * @throws DatabaseException 
	 */
	public Connection getDestConnection() throws DatabaseException {
		DatabaseConnection dc = new DatabaseConnection();
		dc.setDriverClass("org.postgresql.Driver");
		dc.setUrl("jdbc:postgresql://sonic:5432/webcrescendo_mts5_sb?defaultRowFetchSize=25&amp;prepareThreshold=3");
		dc.setUserName("ryan_user_sb");
		dc.setPassword("sqll0gin");
		Connection conn = null;
		
		try {
			conn = dc.getConnection();
		} catch (Exception e) {
			throw new DatabaseException(e);
		}
		
		return conn;
	}
}

