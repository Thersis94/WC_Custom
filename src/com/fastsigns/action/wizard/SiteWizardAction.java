package com.fastsigns.action.wizard;

// JDK 1.6.x
import java.net.URLEncoder;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;


// FASTSIGNS Libs
import com.fastsigns.action.franchise.vo.FranchiseVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.UUIDGenerator;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.dealer.DealerLocationVO;
import com.smt.sitebuilder.admin.action.SiteAction;
import com.smt.sitebuilder.admin.action.data.PageModuleVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: SiteWizardAction.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Feb 22, 2011<p/>
 * <b>Changes: Apr 17. 2012, Billy Larsen, Updated SiteWizardAction to be compatible with new site.</b>
 ****************************************************************************/
public abstract class SiteWizardAction extends SBActionAdapter implements FSSiteWizardIntfc {
	
	// Hours, 3 Button, center image and text, Modules and Map
	protected List<PageModuleVO> defDisplay = new LinkedList<PageModuleVO>();
	protected List<PageModuleVO> secDisplay = new LinkedList<PageModuleVO>();
	protected List<PageModuleVO> emptyColDisplay = new LinkedList<PageModuleVO>();
	
	/*
	 * These are variables set in the localization bundles for country specific id's
	 */
	protected String FS_SITE_ID = "FTS";
	protected String FS_GROUP = "FAST_SIGNS";
	protected String emailSuffix = "@fastsigns.com";
	
	/*
	 * These are the messages sent back to the user, set in the localization bundle for each country.
	 */
	protected String posMsg1 = "You have successfully created the site: ";
	protected String posMsg2 = "You have successfully updated the site: ";
	protected String negMsg1 = "Unable to add new site: ";
	protected String negMsg2 = ".  Please contact the system administrator for assistance";
	protected String negMsg3 = " because it already exists";
	protected String negMsg4 = "Unable to add new site, Franchise ID or Franchise Location Id contained letters: ";
	
	protected Integer centerId = null;
	public static final String EMPTY_COL_LABEL = "Empty Column Layout";
	
	/**
	 * Default Constructor.
	 */
	public SiteWizardAction() {
	}
	
	/**
	 * @param actionInit
	 */
	public SiteWizardAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(SMTServletRequest req) throws ActionException {
		//grab the Id to identify the id of consultation portlet
		centerId = Convert.formatInteger(req.getParameter("dealerLocationId"));
		
		// Assign the display types
		this.assignTypes();
		
		// Store desired workflow directive
		int workflow = Integer.parseInt(req.getParameter("subRule"));
		String msg = negMsg1 + ": " + req.getParameter("dealerName") + negMsg3;
		try {
			if(!Convert.formatBoolean(req.getParameter("isWizard"))){
				dbConn.setAutoCommit(false);
			}
			if(workflow == 1)
				insertOrReturn(req);
			else
				updateDealer(req);
			msg = posMsg1 + req.getParameter("dealerName");
			if(!Convert.formatBoolean(req.getParameter("isWizard"))){
				dbConn.commit();
				dbConn.setAutoCommit(true);
			}

		} catch(InvalidDataException ide) {
			log.error("Unable to add new website for franchise",ide);
			msg = negMsg1 + ": " + req.getParameter("dealerName") + negMsg3;
		} catch(NumberFormatException e) {
			log.error("Franchise dealer ID contains letters and cannot be used",e);
			msg = negMsg4 + req.getParameter("dealerLocationId") +", " + req.getParameter("dealerId");
		} catch(SQLException sqle) {
			log.error("Location already exists", sqle);
			msg = negMsg1 + req.getParameter("dealerName") + negMsg3;
		} catch (Exception e) {
			log.error("Unable to add new website for franchise",e);
			msg = negMsg1 + req.getParameter("dealerName") + negMsg2;
		}
		
		// Flush Cache
		this.clearCacheByGroup(req.getParameter("organizationId") + "_1");
		
		// Build the redirect
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			this.sendRedirect(page.getFullPath(), msg, req);
	}
		
	/* (non-Javadoc)
	 * @see com.fastsigns.action.wizard.FSSiteWizardIntfc#insertOrReturn(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void insertOrReturn(SMTServletRequest req) throws Exception {
		log.debug("Starting Site Wizard");
		attributes.put(AdminConstants.ADMIN_MODULE_DATA, new ModuleVO());
		String dealerLocId = req.getParameter("dealerLocationId");
		req.setParameter("locationName", req.getParameter("dealerName"));
		FranchiseVO vo = new FranchiseVO(req);
		log.debug("Franchise VO: " + vo + ", aliasPath = " + vo.getLocationAlias());
		
		// Check to make sure the franchise doesn't exist when no change is selected.
		this.checkForExistingEntry(dealerLocId);

		//Check to make sure the dealer location doesn't exist
		this.checkforExisingDealerInfo(dealerLocId, req);
		
		// Create org
		this.addOrganization(vo, req);
		
		// Add the center page portlet
		String centerActionId = addCenterPage(vo.getFranchiseId());
		
		// Create website
		this.addWebsite(vo, req);		
		
		// Add the franchise to the dealer location table with insert "true" 
		this.addDealerLocation(req, "true");
		
		// Add an entry in the franchise table
		this.addFranchiseEntry(vo);
		
		// Update Layout information and add the secondary layout
		String layoutId = this.updateLayout(vo.getFranchiseId(), centerActionId);
		String secLayoutId = this.addSecondaryLayout(req); 
		String emptyColLayoutId = this.addEmptyColLayout(req); 
		
		// Associate the main modules and the center image/text to the layouts
		associateCenterPage(layoutId, vo.getFranchiseId(), centerActionId, 1);
		associateCenterPage(emptyColLayoutId, vo.getFranchiseId(), centerActionId, 2);
		associateCenterPage(secLayoutId, vo.getFranchiseId(), centerActionId, 3);
		
		// Change the theme from the default to the new theme
		this.assignTheme(vo);
		
		// Add the home page using the default layout
		this.addHomePage(layoutId, vo.getFranchiseId(), req);
		
		// Add a redirect to the system for the franchise to alias redirect
		this.addRedirect(vo.getFranchiseId(), vo.getLocationAlias(), req);
		
	}
	
	public void updateDealer(SMTServletRequest req) throws ActionException, SQLException {
			this.addDealerLocation(req, "false");
	}
		
	/**
	 * This method returns if a dealer exists in the database.
	 * @param dlrId
	 * @return
	 * @throws SQLException
	 */
	public boolean dealerExists(String dlrId) throws SQLException{
		StringBuilder s = new StringBuilder();
		s.append("select * from dealer where dealer_id = ?");
		
		log.debug("Dealer Check SQL: " + s + "|" + dlrId);
		PreparedStatement ps = dbConn.prepareStatement(s.toString());
		ps.setString(1, dlrId);
		
		ResultSet rs = ps.executeQuery();
		if (rs.next()){
			return true;
		}
		return false;
	}
	
	/**
	 * This method deletes the given dealer and dealerLocation from the database.
	 * @param locId
	 * @param dlrId
	 */
	public void deleteDealerLocation(String locId, String dlrId){
		StringBuilder s = new StringBuilder();
		s.append("delete from dealer_location ");
		s.append("where dealer_location_id = ?");
		log.debug("Dealer delete SQL: " + s + "|" + locId);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(1, locId);
			ps.executeUpdate();
		} catch(Exception e) {
			log.error("Unable to delete location: ", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		s = new StringBuilder();
		s.append("delete from dealer ");
		s.append("where dealer_id = ?");
		log.debug("Dealer delete SQL: " + s + "|" + dlrId);
		ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(1, dlrId);
			ps.executeUpdate();
		} catch(Exception e) {
			log.error("Unable to delete dealer: " + e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
	}
	
	/**
	 * This method checks to see if the given dealerLocation exists.  If so depending on
	 * the rule chosen we either return and continue with execution or put the dealer
	 * information on the request and return to the SiteWizard page.
	 * @param id
	 * @param req
	 * @param workFlow
	 * @return
	 * @throws SQLException
	 * @throws InvalidDataException
	 */
	public void checkforExisingDealerInfo(String id, SMTServletRequest req) 
	throws SQLException, InvalidDataException {
		Integer.parseInt(id);

		StringBuilder s = new StringBuilder();
		s.append("select * from dealer_location b ");
		s.append("inner join dealer a on a.DEALER_ID = b.DEALER_ID ");
		s.append("where dealer_location_id = ?");
		
		log.debug("Dealer Check SQL: " + s + "|" + id);
		PreparedStatement ps = dbConn.prepareStatement(s.toString());
		ps.setString(1, id);
		
		ResultSet rs = ps.executeQuery();
		if (rs.next()){
			DealerLocationVO dlvo = new DealerLocationVO(rs);
			req.setAttribute("dlrLoc", dlvo);
			log.debug(rs.getString("dealer_nm"));
			req.setAttribute("dlrNm", rs.getString("dealer_nm"));
			throw new InvalidDataException("Dealer Already Exists");
		}
		ps.close();
	}
	
	/* (non-Javadoc)
	 * @see com.fastsigns.action.wizard.FSSiteWizardIntfc#checkForExistingEntry(java.lang.String)
	 */
	@Override
	public void checkForExistingEntry(String id) throws SQLException, InvalidDataException, NumberFormatException {
		Integer.parseInt(id);
		StringBuilder s = new StringBuilder();
		String cdb = (String) this.getAttribute(Constants.CUSTOM_DB_SCHEMA);
		s.append("select organization_id from organization where organization_id like ? ");
		s.append("union ");
		s.append("select site_id from site where site_id like ? ");
		s.append("union ");
		s.append("select franchise_id from ").append(cdb).append("fts_franchise where franchise_id = ? ");

		
		log.debug("Franchise Check SQL: " + s + "|" + id);
		
		PreparedStatement ps = dbConn.prepareStatement(s.toString());
		ps.setString(1, "%" + id + "%");
		ps.setString(2, "%" + id + "%");
		ps.setInt(3, Convert.formatInteger(id));
		
		ResultSet rs = ps.executeQuery();
		if (rs.next()) throw new InvalidDataException("Franchise Already Exists");
		ps.close();
	}
	
	/**
	 * Adds the franchise to the dealer location tables. Implemented in 
	 * localized site wizard.
	 * @param vo
	 * @throws Exception
	 */
	public abstract void addDealerLocation(SMTServletRequest req, String insert) 
	throws ActionException, SQLException;
	
	/**
	 * Replaces [location] with the dealerName in the description.
	 * @return
	 */
	public String getLocationDesc(String dealerName) throws SQLException {
		String s = "select desc_txt from " + attributes.get(Constants.CUSTOM_DB_SCHEMA);
		s += "fts_location_desc_option where location_desc_option_id = 1";
		log.debug("Get Loc Desc SQL: " + s);
		
		String desc = null;
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				desc = rs.getString(1);
				desc = desc.replace("[location]", dealerName);
			}
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		return desc;
	}
	
	/**
	 * Adds an entry to the FS franchise table. Implemented in 
	 * localized site wizard.
	 * @param vo
	 * @throws SQLException
	 */
	public abstract void addFranchiseEntry(FranchiseVO vo) throws SQLException;
	
	/* (non-Javadoc)
	 * @see com.fastsigns.action.wizard.FSSiteWizardIntfc#associateCenterPage(java.lang.String, java.lang.String, java.lang.String, int)
	 */
	@Override
	public void associateCenterPage(String layoutId, String fId, String centerActionId, int type) 
	throws Exception {
			List<PageModuleVO> current = secDisplay;
			if (type == 1) current = defDisplay;
			if (type == 2) current = emptyColDisplay;
			log.debug("***********************: " + secDisplay.size() + "|" + defDisplay.size() + "|" + emptyColDisplay.size() + "|" + current.size());
			
			for (PageModuleVO vo : current) {
				
				if (vo.getActionId() == null)
					vo.setActionId(centerActionId);
					
	            String pageModuleId = new UUIDGenerator().getUUID();
	            StringBuilder sb = new StringBuilder();
	            sb.append("insert into page_module ");
	            sb.append("(module_display_id,template_id,action_id, display_column_no,");
	            sb.append("order_no, module_action_nm, param_nm, create_dt, page_module_id) ");
	            sb.append("values (?,?,?,?,?,?,?,?,?)");
				
				PreparedStatement ps = null;
		        try {
		            ps = dbConn.prepareStatement(sb.toString());
		            ps.setString(1, vo.getModuleDisplayId());
		            ps.setString(2, layoutId);
		            ps.setString(3, vo.getActionId());
		            ps.setInt(4, vo.getDisplayColumn());
		            ps.setInt(5, vo.getDisplayOrder());
		            ps.setString(6, null);
		            ps.setString(7, vo.getParamName());
		            ps.setTimestamp(8, Convert.getCurrentTimestamp());
		            ps.setString(9, pageModuleId);
		            ps.executeUpdate();
		            
		    		StringBuffer sql = new StringBuffer();
		    		sql.append("insert into page_module_role(page_module_role_id, page_module_id, role_id, create_dt) ");
		    		sql.append("values (?,?,?,?)");
		    		
		    		
		            for (String role : vo.getRoles().keySet()) {
		            	ps = dbConn.prepareStatement(sql.toString());
		                ps.setString(1, new UUIDGenerator().getUUID());
		                ps.setString(2, pageModuleId);
		                ps.setString(3, role);
		                ps.setTimestamp(4, Convert.getCurrentTimestamp());
		                ps.executeUpdate();
		            }
		        } finally {
		        	try {
		        		ps.close();
		        	} catch(Exception e) {	}
		        }
			}
	}
	
	/* (non-Javadoc)
	 * @see com.fastsigns.action.wizard.FSSiteWizardIntfc#getSecondaryLayoutId(java.lang.String, java.lang.String)
	 */
	@Override
	public String getSecondaryLayoutId(String siteId, String name) throws Exception {
		String sql = "select template_id from template where site_id = ? and layout_nm = ?";
		//log.debug("Get Sec Template ID SQL: " + sql + "|" + siteID + "|" + alias);
		
		PreparedStatement ps = dbConn.prepareStatement(sql);
		String tId = "";
		ps.setString(1, siteId);
		ps.setString(2, name);
		ResultSet rs = ps.executeQuery();
		if (rs.next()) tId = rs.getString(1);
		return tId;

	}
	
	/**
	 * Creates the empty column layout (single col, no left or right rails) for a site
	 * @param req
	 * @return The templateId for that layout
	 * @throws Exception
	 */
	abstract public String addEmptyColLayout(SMTServletRequest req)
		throws Exception;
	
	/* (non-Javadoc)
	 * @see com.fastsigns.action.wizard.FSSiteWizardIntfc#updateLayout(java.lang.String, java.lang.String)
	 */
	@Override
	public String updateLayout(String fId, String actionId) throws SQLException {
		String siteId = FS_SITE_ID + "_" + fId + "_1";
		String tIdSql = "select template_id from template where site_id = '" + siteId + "'";
		String tId = null;
		
		Statement s = dbConn.createStatement();
		ResultSet rs = s.executeQuery(tIdSql);
		if (rs.next()) tId = rs.getString(1);
		
		String sql = "update template set columns_no=3, default_column_no=2";
		sql += " where template_id = '" + tId + "'";
		
		s = dbConn.createStatement();
		s.executeUpdate(sql);
		
		s.close();
		
		// Associate the Center hours, buttons and small map in right column
		
		return tId;
	}
	
	/* (non-Javadoc)
	 * @see com.fastsigns.action.wizard.FSSiteWizardIntfc#addWebsite(com.fastsigns.action.franchise.vo.FranchiseVO, com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void addWebsite(FranchiseVO vo, SMTServletRequest req) throws Exception {
		String email = vo.getFranchiseId() + emailSuffix;
		String dp = URLEncoder.encode("/cms/main.jsp", "UTF-8");
		SMTActionInterface sai = new SiteAction(this.actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		req.setParameter("actionId", "SITE");
		req.setParameter("noEmailNote", "true");
		req.setParameter("organizationId", FS_SITE_ID + "_" + vo.getFranchiseId());
		req.setParameter("siteName", "Center Page " + vo.getFranchiseId());
		req.setParameter("adminEmail", email);
		req.setParameter("adminName", "Webmaster");
		req.setParameter("mainEmail", email);
		req.setParameter("languageCode", "en");
		req.setParameter("allowAliasPathFlag", "0");
		req.setParameter("aliasPathName", vo.getLocationAlias());
		req.setParameter("aliasPathParentId", FS_SITE_ID + "_7");
		req.setParameter("documentPath", dp);
		sai.update(req);
	}
	/* (non-Javadoc)
	 * @see com.fastsigns.action.wizard.FSSiteWizardIntfc#addCenterPage(java.lang.String)
	 */
	@Override
	public String addCenterPage(String franchiseId) throws SQLException {
		String s = "insert into sb_action (action_nm, action_desc, organization_id, ";
		s += "module_type_id, action_id, attrib1_txt, create_dt) values (?,?,?,?,?,?,?)";
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s);
			ps.setString(1, "Center Page Portlet");
			ps.setString(2, "Center Page Portlet");
			ps.setString(3, FS_SITE_ID + "_"+ franchiseId);
			ps.setString(4, "FTS_CENTER_PAGE");
			ps.setString(5, FS_SITE_ID + "_CENTER_PAGE_" + franchiseId);
			ps.setString(6, franchiseId + "");
			ps.setTimestamp(7, Convert.getCurrentTimestamp());
			ps.executeUpdate();
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		
		// Return the action id
		return FS_SITE_ID + "_CENTER_PAGE_" + franchiseId;
	}
	
	/**
	 * This method adds the center organization to the database.
	 * @param vo
	 */
	public abstract void addOrganization(FranchiseVO vo, SMTServletRequest req) throws Exception;
	
	/* (non-Javadoc)
	 * @see com.fastsigns.action.wizard.FSSiteWizardIntfc#makePageModule(boolean, boolean, boolean, java.lang.String, java.lang.String, java.lang.String, int, int)
	 */
	@Override
	public PageModuleVO makePageModule(boolean isPublic, boolean isRegistered, boolean isAdmin, String actionId, String displayPgId, String paramNm, int col, int order) {
		PageModuleVO pm = new PageModuleVO();
		pm.setActionId(actionId);
		pm.setModuleDisplayId(displayPgId);
		pm.setParamName(paramNm);
		pm.setDisplayColumn(col);
		pm.setDisplayOrder(order);
		pm.setRoles(makeRoles(isPublic, isRegistered, isAdmin)); //add default Public roles, some modules will override this
		return pm;
	}
	
	/**
	 * 
	 * @param fId
	 * @param alias
	 * @param req
	 * @throws ActionException
	 */
	public void addRedirect(String fId, String alias, SMTServletRequest req) 
	throws ActionException {
		// Set the parameters needed by the Action
		
		StringBuilder sql = new StringBuilder();
		sql.append("insert into site_redirect (site_redirect_id, site_id, ");
		sql.append("redirect_alias_txt, destination_url, active_flg, global_flg, ");
		sql.append("permanent_redir_flg, log_redir_flg, create_Dt) values(?,?,?,?,?,?,?,?,?)");
		UUIDGenerator g = new UUIDGenerator();
		String [] fids = new String[]{ "/" + fId,  "/" + fId,  "/" + alias};
		String [] sids = new String[]{FS_SITE_ID + "_7", FS_SITE_ID + "_4", FS_SITE_ID + "_4"};
		String [] aliass = new String[]{"/" + alias, "/locator?dlrSub=true&dlrInfoSub=true&dealerLocationId=" + fId, "/locator?dlrSub=true&dlrInfoSub=true&dealerLocationId=" + fId};
		String [] ids = new String[]{ g.getUUID(), FS_SITE_ID + "_" + fId + "_MOBILE", FS_SITE_ID + "_" + fId + "MOBILE_ALIAS"};
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sql.toString());
			for(int i = 0; i < ids.length; i++){
				ps.setString(1, ids[i]);
				ps.setString(2, sids[i]);
				ps.setString(3, fids[i]);
				ps.setString(4, aliass[i]);
				ps.setInt(5, 1);
				ps.setInt(6, 0);
				ps.setInt(7, 1);
				ps.setInt(8, 0);
				ps.setTimestamp(9, Convert.getCurrentTimestamp());
				ps.executeUpdate();
			}
		} catch(Exception e) {
			log.debug("Could not make redirects for center " + fId, e);
		}

	}
	
	public void setCenterId(Integer val){
		centerId = val;
	}
	
	public Integer getCenterId(){
		return centerId;
	}
}
