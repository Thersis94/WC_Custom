package com.fastsigns.action.wizard.siteCopy;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fastsigns.action.franchise.CenterPageAction;
import com.fastsigns.action.wizard.SiteWizardAction;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.db.util.RecordDuplicator;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.FileWriterException;
import com.smt.sitebuilder.action.FileLoader;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.content.ContentAction;
import com.smt.sitebuilder.action.dealer.DealerInfoAction;
import com.smt.sitebuilder.action.file.FileSystemBatch;
import com.smt.sitebuilder.admin.action.PageModuleAction;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.db.OrgCopyUtil;
import com.smt.sitebuilder.util.RecordDuplicatorUtility;

public abstract class FranchiseSiteCopyWizard extends SBActionAdapter {
	public static String customDb = null;
	public String oldFranId = null;
	public String newFranId = null;
	public String FS_PREFIX = "FTS_";
	public Map<String, Object> replaceVals = null;
	public String FS_GROUP = "FAST_SIGNS";
	public String emailSuffix = "@fastsigns.com";
	public String msg = null;
	public String siteWizardPath = "";
	public FranchiseSiteCopyWizard() {

	}

	public FranchiseSiteCopyWizard(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Handles the cloning of Franchises.  We proxy out to SiteWizardAction to 
	 * create the initial center after we've seeded the request object with the
	 * appropriate info.  Then we proceed to copy all the custom data related
	 * to the old Franchise into the new Franchise.  This method is final as
	 * we don't want any subclasses to play with how franchises are copied.
	 */
	public final void build(SMTServletRequest req) throws ActionException {	
		customDb = (String) this.getAttribute(Constants.CUSTOM_DB_SCHEMA);
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		String redir = page.getFullPath() + "?";
		oldFranId = CenterPageAction.getFranchiseId(req);
		newFranId = req.getParameter("dealerLocationId");
		replaceVals = generateMap(req, oldFranId, newFranId);

		msg = "Copy Did not complete successfully.";
		try {
			if (!franchiseExists(newFranId)) {
				
				/*
				 * Turn off the autocommit in case something fails and proceed
				 * to copy all the information in order of dependencies.
				 */
				req.setParameter("isWizard", "true");
				seedDealerInformation(req);
				forwardToWizard(req);
				// wizard
				dbConn.setAutoCommit(false);

				cloneFranchiseButtons();
				cloneModules();
				updateModules();
				cloneModuleAttributes();
				cloneModuleLocations();
				cloneLocationOptionXR();
				cloneFranchisePages(req);
				copySiteFolders();
				msg = "Franchise " + newFranId + " created successfully";
				dbConn.commit();
				dbConn.setAutoCommit(true);
			} else{
				msg += " Center already exists.";
			}
		} catch (Exception e) {
			try {
				
				//Rollback if anything goes wrong.
				dbConn.rollback();
			} catch (SQLException e1) {
				log.debug(e1);
			}
			log.error("Franchise Copy Failed", e);
		}
		log.debug("Sending Redirect to: " + redir);
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, redir + "msg=" + msg);
	}

	/**
	 * Make initial call to the SiteWizardAction for this country and let it do
	 * the heavy lifting of making the site with the seeded data.  We will then
	 * update the data.
	 * @param req
	 */
	private void forwardToWizard(SMTServletRequest req) {
		SiteWizardAction swa;
		log.debug("Forwarding to " + siteWizardPath);
		try {			
			Thread t = Thread.currentThread();
			ClassLoader cl = t.getContextClassLoader();
			Class<?> load = cl.loadClass(siteWizardPath);
			swa = (SiteWizardAction)load.newInstance();
			swa.setDBConnection(dbConn);
			swa.setAttributes(attributes);
			swa.setActionInit(actionInit);
			swa.build(req);
		} catch (Exception e) {
			log.error(e);
        } 
	}

	/**
	 * Method spoofs Dealer/Location data from the database as if it was entered
	 * in the form to be forwarded to the appropriate wizard.  We use the wizard
	 * because the wizard code is already verified and we don't want to re-write
	 * a bunch of code.
	 * @param req
	 * @throws SQLException 
	 */
	private void seedDealerInformation(SMTServletRequest req) throws SQLException {
		oldFranId = CenterPageAction.getFranchiseId(req);
		newFranId = req.getParameter("dealerLocationId");	
		StringBuilder sql = new StringBuilder();
		sql.append("select * from DEALER_LOCATION a inner join DEALER b ");
		sql.append("on a.DEALER_ID = b.DEALER_ID where a.DEALER_LOCATION_ID = ?");
		PreparedStatement ps = null;
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, oldFranId);
			ResultSet rs = ps.executeQuery();
			
			/*
			 * If we have a result, seed the request with the proper data to 
			 * simulate a new center creation.
			 */
			if(rs.next()){
				log.debug("alias = " + rs.getString("LOCATION_ALIAS_NM").replace(oldFranId, newFranId));
				req.setParameter("locationAliasName", rs.getString("LOCATION_ALIAS_NM").replace(oldFranId, newFranId));
				req.setParameter("locationOwnerName", rs.getString("LOCATION_OWNER_NM"));
				req.setParameter("dealerName", rs.getString("DEALER_NM"));
				req.setParameter("phone", rs.getString("PRIMARY_PHONE_NO"));
				req.setParameter("address", rs.getString("ADDRESS_TXT"));
				req.setParameter("fax", rs.getString("FAX_NO"));
				req.setParameter("address2", rs.getString("ADDRESS2_TXT"));
				req.setParameter("weekdayHours", rs.getString("WEEKDAY_HRS_TXT"));
				req.setParameter("city", rs.getString("CITY_NM"));
				req.setParameter("saturdayHours", rs.getString("SATURDAY_HRS_TXT"));
				req.setParameter("country", rs.getString("COUNTRY_CD"));
				req.setParameter("sundayHours", rs.getString("SUNDAY_HRS_TXT"));
				req.setParameter("county", rs.getString("COUNTY_NM"));
				req.setParameter("zip", rs.getString("ZIP_CD"));
				req.setParameter("emailAddress", rs.getString("EMAIL_ADDRESS_TXT").replace(oldFranId, newFranId));
				req.setParameter("website", rs.getString("WEBSITE_URL").replace(oldFranId, newFranId));
				req.setParameter("state", rs.getString("STATE_CD"));
			}
	}

	/**
	 * This method checks to see if the new Franchise number we wish to create
	 * already exists in the system.
	 * @param franchiseId
	 * @return
	 */
	private boolean franchiseExists(String franchiseId) {
		StringBuilder sb = new StringBuilder();
		sb.append("select * from ").append(customDb).append("FTS_FRANCHISE ");
		sb.append("where FRANCHISE_ID = ?");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, franchiseId);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				return true;
		} catch (SQLException e) {
			log.debug(e);
			return true;
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				log.debug(e);
			}

		}
		msg="Center already Exists.  Franchise Copy Terminated";
		return false;
	}

	/**
	 * This method creates the initial replaceVals map and seeds it with the
	 * necessary data for the Franchise copy.
	 * @param req
	 * @param oldFranId
	 * @param newFranId
	 * @return
	 */
	private Map<String, Object> generateMap(SMTServletRequest req, String oldFranId, String newFranId) {
		Map<String, Object> vals = new HashMap<String, Object>();

		/*
		 * Add the Site Ids to the map.
		 */
		Map<Object, Object> siteIds = new HashMap<Object, Object>();
		siteIds.put(FS_PREFIX + oldFranId + "_1", FS_PREFIX + newFranId + "_1");
		siteIds.put(FS_PREFIX + oldFranId + "_2", FS_PREFIX + newFranId + "_2");
		vals.put(OrgCopyUtil.SITE_ID_KEY, siteIds);

		/*
		 * Add the Org Ids to the map.
		 */
		Map<Object, Object> orgs = new HashMap<Object, Object>();
		orgs.put(FS_PREFIX + oldFranId, FS_PREFIX + newFranId);
		vals.put(OrgCopyUtil.ORGANIZATION_ID_KEY, orgs);

		/*
		 * Add the Franchise Ids to the map.
		 */
		Map<Object, Object> franchiseId = new HashMap<Object, Object>();
		franchiseId.put(oldFranId, newFranId);
		vals.put("FRANCHISE_ID", franchiseId);
		
		return vals;
	}

	/**
	 * Adds the franchise web site for the provided franchise.
	 * 
	 * @param vo
	 * @throws ActionException
	 * @throws IOException
	 */
	public void copyDealerInfo(SMTServletRequest req) throws ActionException {
		log.debug("Preparing to clone Dealer Objects");
		SBActionAdapter dla = new DealerInfoAction(actionInit);
		dla.setDBConnection(dbConn);
		dla.setAttributes(attributes);
		req.setParameter("type", "1");
		req.setAttribute("replaceVals", replaceVals);
		dla.copy(req);
		req.setParameter("type", "0");
		dla.copy(req);
	}

	/**
	 * This method retrieves the template information for the newly created
	 * templates.  This is used in the Page clone.
	 * @return
	 */
	public Map<String, String> getTemplates() {
		Map<String, String> templates = new HashMap<String, String>();
		StringBuilder sql = new StringBuilder();
		sql.append("select a.TEMPLATE_ID as OLD_ID, b.TEMPLATE_ID as NEW_ID ");
		sql.append("from TEMPLATE a left outer join TEMPLATE b ");
		sql.append("on a.LAYOUT_NM = b.LAYOUT_NM and b.SITE_ID = ? where a.SITE_ID = ?");
		log.debug(sql.toString());
		PreparedStatement ps = null;
		log.debug(sql.toString());
		try{
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, FS_PREFIX + newFranId + "_1");
			ps.setString(2, FS_PREFIX + oldFranId + "_1");
			ResultSet rs = ps.executeQuery();
			while(rs.next())
				templates.put(rs.getString("OLD_ID"), rs.getString("NEW_ID"));
		} catch(SQLException sqle) {
			
		}
		return templates;
	}
	
	/**
	 * This method clones the pages for the old center and adds them to the new 
	 * center.  We retrieve the templates and replace the other relevant data
	 * then re-insert the data back in the database.  We return a map of the old
	 * pageId to new pageId for use later.
	 * @return Map of oldPageId->newPageId
	 * @throws Exception 
	 */
	public void cloneFranchisePages(SMTServletRequest req) throws Exception{
		
		/*
		 * Build our where clause and forward the call to the RecordDuplicator to
		 * copy all the pages.
		 */
		Map<String, String> templates = getTemplates();
		StringBuilder sql = new StringBuilder();
		sql.append("SITE_ID = '").append(FS_PREFIX + oldFranId + "_1").append("' and LIVE_START_DT < GETDATE() ");
		sql.append("and PAGE_ALIAS_NM != 'home' order by PARENT_ID");
		replaceVals.put("TEMPLATE_ID", templates);
		log.debug("Got templates");
		RecordDuplicator dup = new RecordDuplicator(dbConn, "PAGE", "PAGE_ID");
		dup.setWhereSQL(sql.toString());
		dup.setReplaceVals(replaceVals);
		replaceVals.put("PAGE_ID", dup.copyRecords());
		log.debug("Got Pages");
		fixPageParents();
		clonePageRoles();
		clonePortlets(req);
		clonePageModules(req);
	}
	
	/**
	 * This method fixes the parent child relationships of the newly created pages.
	 * @param parents
	 * @throws SQLException
	 */
	@SuppressWarnings("rawtypes")
	public void fixPageParents() throws SQLException {
		Map<String, String> parents = new HashMap<String, String>();
		StringBuilder sql = new StringBuilder();
		sql.append("select * from PAGE where SITE_ID = ? and PARENT_ID is not null ");
		sql.append("and PARENT_ID != ''");
		PreparedStatement ps = dbConn.prepareStatement(sql.toString());
		ps.setString(1, FS_PREFIX + newFranId + "_1");
		ResultSet rs = ps.executeQuery();

		/*
		 * If the parent key is getting replaced, add it to the map.
		 */
		while(rs.next()){
			parents.put(rs.getString("PAGE_ID"), (String) ((Map)replaceVals.get("PAGE_ID")).get(rs.getString("PARENT_ID")));
		}
		
		/*
		 * If we have any parents to replace, update the pages here.
		 */
		if(parents.size() > 0){
			ps.close();
			sql = new StringBuilder();
			sql.append("update PAGE set PARENT_ID = ? where PAGE_ID = ?");
			ps = dbConn.prepareStatement(sql.toString());
			for(String pId : parents.keySet()){
				ps.setString(1, parents.get(pId));
				ps.setString(2, pId);
				ps.addBatch();
			}
			ps.executeBatch();
		}
		ps.close();
	}
	
	/**
	 * This method clones all the new pages new Page Roles.
	 * @throws Exception
	 */
	public void clonePageRoles() throws Exception {
		RecordDuplicator dup = new RecordDuplicator(dbConn, "PAGE_ROLE", "PAGE_ROLE_ID");
		dup.setReplaceVals(replaceVals);
		dup.setWhereSQL(dup.buildWhereListClause("PAGE_ID"));
		dup.copyRecords();
	}

	/**
	 * This method clones the portlets on the pages (Currently just content)
	 * @param req
	 * @throws Exception
	 */
	public void clonePortlets(SMTServletRequest req) throws Exception {
		req.setAttribute(RecordDuplicatorUtility.SB_ACTION_ID, "");
		req.setParameter(RecordDuplicatorUtility.SB_ACTION_ID, "");
		req.setParameter("moduleTypeId", "CONTENT");
		attributes.put("replaceVals", replaceVals);
		ContentAction ca = new ContentAction(this.actionInit);
		ca.setDBConnection(dbConn);
		ca.setAttributes(attributes);
		ca.copy(req);
		log.debug(replaceVals.get("SB_ACTION"));
	}

	/**
	 * This method clones all the page modules and binds them to the new pages.
	 * @param req
	 * @throws Exception
	 */
	public void clonePageModules(SMTServletRequest req) throws Exception {
		req.setParameter("ignoreTemplates", "true");
		attributes.put("replaceVals", replaceVals);
		PageModuleAction pma = new PageModuleAction(this.actionInit);
		pma.setDBConnection(dbConn);
		pma.setAttributes(attributes);
		pma.copy(req);
		log.debug(replaceVals.get("SB_ACTION"));
	}

	/**
	 * This method clones the custom Franchise Table.
	 * @throws Exception
	 */
	public void cloneFranchise() throws Exception {
		log.debug("Preparing to clone Franchise Objects");
		RecordDuplicator dup = new RecordDuplicator(dbConn, "FTS_FRANCHISE", "FRANCHISE_ID");
		dup.addWhereClause("FRANCHISE_ID", oldFranId);
		dup.setSchemaNm(customDb);
		dup.setReplaceVals(replaceVals);
		replaceVals.put("FRANCHISE_ID", dup.copyRecords());
	}
	
	/**
	 * This method clones the custom Franchise Button Table.
	 * @throws Exception
	 */
	public void cloneFranchiseButtons() throws Exception{
		log.debug("Preparing to clone Franchise Button Objects");
		RecordDuplicator dup = new RecordDuplicator(dbConn, "FTS_FRANCHISE_BUTTON_XR", "DEALERLOC_BUTTON_XR_ID");
		dup.addWhereClause("FRANCHISE_ID", oldFranId);
		dup.setSchemaNm(customDb);
		dup.setReplaceVals(replaceVals);
		dup.copyRecords();
	}
	
	/**
	 * This method clones the old Franchise's custom Modules for the new Franchise.
	 * @throws Exception
	 */
	public void cloneModules() throws Exception {
		log.debug("Preparing to clone Module Objects");
		RecordDuplicator dup = new RecordDuplicator(dbConn, "FTS_CP_MODULE_OPTION", "CP_MODULE_OPTION_ID");
		dup.addWhereClause("FRANCHISE_ID", oldFranId);
		dup.addWhereClause("APPROVAL_FLG", 1);
		dup.setSchemaNm(customDb);
		dup.setReplaceVals(replaceVals);
		replaceVals.put("CP_MODULE_OPTION_ID", dup.copyRecords());
	}
	
	/**
	 * This method updates all the modules that were just copied so that their
	 * binary data comes from the new Franchise rather than the old Franchise.
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public void updateModules() throws Exception {
		log.debug("Preparing to update Module Information");
		StringBuilder sb = new StringBuilder();
		sb.append("update ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("FTS_CP_MODULE_OPTION ");
		sb.append("set ARTICLE_TXT = replace(ARTICLE_TXT, '" + oldFranId + "', '");
		sb.append(newFranId + "') where CP_MODULE_OPTION_ID in (");
		Map<String, String> optionIds = (Map<String, String>) replaceVals.get("CP_MODULE_OPTION_ID");
		Collection<String> ids = (Collection<String>) optionIds.values();
		Iterator<String> iter = ids.iterator();
		int i = 0;
		while(iter.hasNext()) {
			String key = iter.next();
			sb.append("'" + key + "'");
			if(i != ids.size() - 1)
				sb.append(", ");
			i++;
		}
		sb.append(")");
		log.debug("SQL = " + sb.toString());
		PreparedStatement ps = dbConn.prepareStatement(sb.toString());
		ps.executeUpdate();
		ps.close();
	}
	
	/**
	 * This method attempts to clone any Module Attributes that may be tied to
	 * a Franchises recently copied Modules.
	 * @throws Exception
	 */
	public void cloneModuleAttributes() throws Exception {
		log.debug("Preparing to clone Module Option Attributes");
		StringBuilder sql = new StringBuilder();
		sql.append("CP_MODULE_OPTION_ID in (select CP_MODULE_OPTION_ID from ");
		sql.append(customDb).append("FTS_CP_MODULE_OPTION where ");
		sql.append("FRANCHISE_ID = ").append(oldFranId).append(" and APPROVAL_FLG = 1) ");
		sql.append("and ATTR_PARENT_ID is null and ACTIVE_FLG = 1");
		RecordDuplicator dup = new RecordDuplicator(dbConn, "FTS_CP_OPTION_ATTR", "CP_OPTION_ATTR_ID");
		dup.setWhereSQL(sql.toString());
		dup.setSchemaNm(customDb);
		dup.setReplaceVals(replaceVals);
		dup.copyRecords();
	}
	
	/**
	 * This Method copies the custom Module Locations Table.
	 * @throws Exception
	 */
	public void cloneModuleLocations() throws Exception {
		log.debug("Preparing to clone Module Location XR");
		RecordDuplicator dup = new RecordDuplicator(dbConn, "FTS_CP_LOCATION_MODULE_XR", "CP_LOCATION_MODULE_XR_ID");
		dup.addWhereClause("FRANCHISE_ID", oldFranId);
		dup.setSchemaNm(customDb);
		dup.setReplaceVals(replaceVals);
		replaceVals.put("CP_LOCATION_MODULE_XR_ID", dup.copyRecords());
	}
	
	/**
	 * This method copies the module locations of the old Franchise so the new
	 * Franchise looks the same.
	 * @throws Exception
	 */
	private void cloneLocationOptionXR() throws Exception {
		log.debug("Preparing to clone Module Module Franchise XR");

		StringBuilder sql = new StringBuilder();
		sql.append("CP_LOCATION_MODULE_XR_ID in (select CP_LOCATION_MODULE_XR_ID from ");
		sql.append(customDb).append("FTS_CP_LOCATION_MODULE_XR where FRANCHISE_ID = ");
		sql.append(oldFranId).append(")");
		
		RecordDuplicator dup = new RecordDuplicator(dbConn, "FTS_CP_MODULE_FRANCHISE_XR", "CP_MODULE_FRANCHISE_XR_ID");
		dup.setWhereSQL(sql.toString());
		dup.setSchemaNm(customDb);
		dup.setReplaceVals(replaceVals);
		dup.copyRecords();
	}
	
	/**
	 * Copy all the files from the old Franchise for use with the new Franchise
	 * @param sSite
	 * @param sOrg
	 * @param dSite
	 * @param dOrg
	 */
	private void copySiteFolders() {
		log.debug("copying site folders...");
    	String pathToBinary = (String) getAttribute("pathToBinary");
    	String orgAlias = (String) getAttribute("orgAlias");
    	
    	String sourceSite = pathToBinary + orgAlias + FS_PREFIX + oldFranId + "/";
    	String destSite = pathToBinary + orgAlias + FS_PREFIX + newFranId + "/";
    	log.debug("sourceSite | destSite: " + sourceSite + "|" + destSite);
    	
    	// Copy the file from the source site to the destination site
    	FileLoader fl = new FileLoader(attributes);
    	try {
    		fl.copy(sourceSite, destSite);
    		copyFranchiseBinary();
    	} catch (FileWriterException fwe) {
    		log.error("Unable to copy Site Directory " + sourceSite + "|" + destSite, fwe);
    	}
	}
	
	/**
	 * Loads the File Manager with file/folder data for this org.
	 * @param req
	 */
	private void copyFranchiseBinary() {
		FileSystemBatch fsb = new FileSystemBatch(Boolean.FALSE);		
		String path = getAttribute("pathToBinary") + "/org/";
		fsb.process(FS_PREFIX + newFranId + "/", dbConn, FS_PREFIX + newFranId, path);
	}
}
