package com.fastsigns.action.franchise;

// JDK
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


// SMT Base Libs
import com.fastsigns.action.franchise.centerpage.FranchiseInfoAction;
import com.fastsigns.action.franchise.centerpage.FranchiseLocationInfoAction;
import com.fastsigns.action.franchise.centerpage.ModuleOptionAction;
import com.fastsigns.action.franchise.centerpage.PortletLoaderAction;
import com.fastsigns.action.franchise.vo.ButtonVO;
import com.fastsigns.action.franchise.vo.CenterModuleOptionVO;
import com.fastsigns.action.franchise.vo.CenterModuleVO;
import com.fastsigns.action.franchise.vo.FranchiseContainer;
import com.fastsigns.action.franchise.vo.FranchiseTimeVO;
import com.fastsigns.action.franchise.vo.FranchiseVO;
import com.fastsigns.security.FastsignsSessVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.tools.EmailFriendAction;
import com.smt.sitebuilder.approval.ApprovalController;
import com.smt.sitebuilder.approval.ApprovalVO;
import com.smt.sitebuilder.approval.ApprovalController.SyncStatus;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: CenterPageAction.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Nov 19, 2010<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class CenterPageAction extends SimpleActionAdapter {
	/*
	 * Constants used to control flow through update/build calls.
	 */
	public static final int LOCATION_UPDATE = 1;
	public static final int MODULE_OPTION_UPDATE = 2;
	public static final int MODULE_ASSOC = 3;
	public static final int FRANCHISE_BUTTON_UPDATE = 4;
	//public static final int FRANCHISE_RIGHT_IMAGE_UPDATE = 5;
	public static final int FRANCHISE_MAIN_IMAGE_APPROVE = 12;
	public static final int FRANCHISE_MAIN_IMAGE_UPDATE = 6;
	public static final int FRANCHISE_SOCIAL_MEDIA_LINKS = 13;
	public static final int FRANCHISE_DESC_UPDATE = 7;
	public static final int FRANCHISE_CUSTOM_DESC_UPDATE = 21;
	public static final int FRANCHISE_CUSTOM_DESC_DELETE = 22;
	public static final int MODULE_LOC_DELETE = 8;
	public static final int MODULE_ADD = 9;
	public static final int MODULE_REARRANGE = 14;
	public static final int MODULE_DELETE = 15;
	public static final int MODULE_APPROVE = 10;
	public static final int MODULE_SUBMIT = 11;
	public static final int MODULE_VIEW_UPDATE = 16;
	public static final int DELETE_ALL_MODULES = 30;
	public static final int WHITEBOARD_UPDATE = 35;
	public static final int RESELLER_UPDATE = 17;
	public static final int RAQSAF_UPDATE = 18;
	public static final int GLOBAL_MODULE_UPDATE = 19;
	/**
	 * 
	 */
	public CenterPageAction() {
		
	}

	/**
	 * @param arg0
	 */
	public CenterPageAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	/**
	 * Method controls access and flow through the rest of the Class, diverting 
	 * calls as needed both within and outside the class.
	 */
	public void update(SMTServletRequest req) throws ActionException {
		log.debug("Updating Center Page Info");
		String msg = "msg.updateSuccess";
		Integer bType = Convert.formatInteger(req.getParameter("bType"));
		log.debug("type=" + bType);
		log.debug(((SiteVO)req.getAttribute("siteData")).getCountryCode());
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		String redir = page.getFullPath() + "?";
		//turn off string encoding since this is an administrative (& secure) method call
		req.setValidateInput(Boolean.FALSE);
		
		// Determine which data is being updated
		try {
			switch(bType) {
				case MODULE_APPROVE:
				case FRANCHISE_MAIN_IMAGE_APPROVE:
				case MODULE_SUBMIT:
				case DELETE_ALL_MODULES:
				case MODULE_ASSOC:
				case MODULE_DELETE:
				case MODULE_LOC_DELETE:
				case MODULE_OPTION_UPDATE:
				case MODULE_ADD:
				case MODULE_REARRANGE:
				case MODULE_VIEW_UPDATE:
					ModuleOptionAction moa = new ModuleOptionAction(this.actionInit);
					moa.setDBConnection(dbConn);
					moa.setAttributes(attributes);
					moa.update(req);
					break;
				case LOCATION_UPDATE:
					FranchiseLocationInfoAction fla = new FranchiseLocationInfoAction(this.actionInit);
					fla.setDBConnection(dbConn);
					fla.setAttributes(attributes);
					fla.update(req);
					break;
				case FRANCHISE_BUTTON_UPDATE:
				case FRANCHISE_DESC_UPDATE:
				case FRANCHISE_CUSTOM_DESC_UPDATE:
				case FRANCHISE_CUSTOM_DESC_DELETE:
				case FRANCHISE_MAIN_IMAGE_UPDATE:
				case FRANCHISE_SOCIAL_MEDIA_LINKS:
				case WHITEBOARD_UPDATE:
				case CenterPageAction.RESELLER_UPDATE:
				case RAQSAF_UPDATE:
				case GLOBAL_MODULE_UPDATE:
					FranchiseInfoAction fia = new FranchiseInfoAction(this.actionInit);
					fia.setDBConnection(dbConn);
					fia.setAttributes(attributes);
					fia.update(req);
					break;
			}
		} catch(Exception e) {
			log.error("Error Updating Center Page", e);
			msg = "msg.cannotUpdate";
		}

		log.debug("Sending Redirect to: " + redir);
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, redir + "msg=" + msg);
	}
	
	/**
	 * This method handles saving data from the public site.
	 */
	public void build(SMTServletRequest req) throws ActionException {
		
		//If request is from a poll
		if (Convert.formatBoolean(req.getParameter("pollSubmit"))) {
			this.buildPoll(req);
			
		//If request is from Email a Friend
		} else if (Convert.formatBoolean(req.getParameter("isEaf"))) {
			this.sendEmailFriend(req);
			
		//If request is from a Testimonial
		} else if (Convert.formatBoolean(req.getParameter("isTestimonial"))) {
			log.debug("saving testimonial");
			try {
				retrieve(req);
				ModuleOptionAction moa = new ModuleOptionAction(this.actionInit);
				moa.build(req);
			} catch (Exception e) {
				throw new ActionException(e);
			}
			
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			StringBuilder url = new StringBuilder(page.getRequestURI());
			url.append("?pmid=").append(req.getParameter("pmid"));
			url.append("&testimonialForm=true&printerFriendlyTheme=true&hidePf=true&submitted=true");
			super.sendRedirect(url.toString(), null, req);
		} 
		
		
		/*
		 * If this is an embedded Action, forward to the PortletLoaderAction
		 */
		else if(req.hasParameter("actionOptionId")) {
			PortletLoaderAction pla = new PortletLoaderAction(this.actionInit);
			pla.setDBConnection(dbConn);
			pla.setAttributes(attributes);
			pla.build(req);
		} 
		
		// If the req does not fit any of the above, call the update method
		else {
			update(req);
		}
		
	}
	
	/**
	 * This method handles forwarding an Email a Friend call from the a Center
	 * Page to the EmailFriendAction.
	 * @param req
	 * @throws ActionException
	 */
	public void sendEmailFriend(SMTServletRequest req) throws ActionException {
		SiteVO site = (SiteVO)req.getAttribute("siteData");
		ActionInitVO ai = new ActionInitVO();

		//Here we use the same actionId for all Fastsigns branded orgs.
		ai.setActionId("c0a801653d87314d380475ef5668ddfb");
		ai.setName("Tell Someone About Us");
		
		//If the site is Signwave branded, use AU's ActionId.
		if(site.getCountryCode().equals("AU")){
			ai.setActionId("0a0014137c77504fed1c4b27b4e52892");
		}
			
		
		log.debug(" Sending Email ...");
		
		SMTActionInterface sai = new EmailFriendAction(ai);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		sai.build(req);
		
		//setting the actionId of the email a friend to the redirect url.
		String url = StringUtil.checkVal(req.getAttribute(Constants.REDIRECT_URL));
		url += "&emailActionId=" + ai.getActionId();
		req.setAttribute(Constants.REDIRECT_URL, url);
	}
	
	/**
	 * This method handles updating a poll to reflect the new Vote that was just
	 * submitted.
	 * @param req
	 */
	public void buildPoll(SMTServletRequest req) {
		SiteVO site = (SiteVO)req.getAttribute("siteData");
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		int attrId = Convert.formatInteger(req.getParameter("optionAttrId"));
		int moduleOptionId = Convert.formatInteger(req.getParameter("moduleOptionId"));
		
		StringBuilder sb = new StringBuilder();
		sb.append("update ").append(customDb).append("fts_cp_option_attr ");
		sb.append("set attrib_value_txt =  cast((cast(attrib_value_txt as int) + 1) as varchar(20)) ");
		sb.append("where cp_module_option_id = ? and cp_option_attr_id = ? ");
		log.debug("Poll SQL: " + sb + "|" + attrId + "|" + moduleOptionId);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setInt(1, moduleOptionId);
			ps.setInt(2, attrId);
			ps.executeUpdate();
			
		} catch (SQLException sqle) {
			log.error("Unable to add poll", sqle);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		super.clearCacheByGroup(site.getSiteId());

		//send the redirect and the session id
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, page.getFullPath());
		req.getSession().setAttribute("moduleOptionId", moduleOptionId);
		log.debug("Setting session id: " + req.getParameter("moduleOptionId") + " and redirect " + page.getFullPath());
	}
	
	/**
	 * This method is used to set a franchises Right Image.  It was used on the 
	 * old site, not used anymore.  
	 * @param req
	 */
	@Deprecated
	public void updateFranchiseRightImage(SMTServletRequest req) throws SQLException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder s = new StringBuilder();
		s.append("update ").append(customDb).append("fts_franchise set ");
		s.append("right_image_id = ?, update_dt = ? where franchise_id = ? ");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setInt(1, Convert.formatInteger(req.getParameter("rightImageId")));
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, getFranchiseId(req));
			
			ps.executeUpdate();

		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@SuppressWarnings("unchecked")
	public void retrieve(SMTServletRequest req) throws ActionException {
		
		if (AdminConstants.REQ_LIST.equalsIgnoreCase(req.getParameter(AdminConstants.REQUEST_TYPE))) {
			super.retrieve(req);
			return;
		}
		//Instantiate container for Franchise.
		FranchiseContainer fc = new FranchiseContainer();
		req.setAttribute(SB_ACTION_ID, actionInit.getActionId());
		FranchiseLocationInfoAction fla = new FranchiseLocationInfoAction(this.actionInit);
		fla.setDBConnection(dbConn);
		fla.setAttributes(attributes);
		FranchiseInfoAction fia = new FranchiseInfoAction(this.actionInit);
		fia.setDBConnection(dbConn);
		fia.setAttributes(attributes);
		// Get the SB Module data
		super.retrieve(req);
		String orgId = ((SiteVO)req.getAttribute("siteData")).getOrganizationId();
		String franchiseId = StringUtil.checkVal(getFranchiseId(req));
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		String id = StringUtil.checkVal(mod.getAttribute(ModuleVO.ATTRIBUTE_1), franchiseId);
		log.debug("ID Value: " + id);
		if (id.length() == 0) return;
		if (mod.isCacheable() && orgId.matches(".*[0-9].*")) mod.setCacheGroup(orgId + "_1");
		else if (mod.isCacheable()) mod.setCacheGroup(orgId + "_" + id + "_1");
		Boolean isPreview = Convert.formatBoolean(req.getAttribute(Constants.PAGE_PREVIEW), false);
		
        String previewApiKey = ApprovalController.generatePreviewApiKey(attributes);
        req.setParameter(Constants.PAGE_PREVIEW, previewApiKey);
		if(req.hasParameter("reloadMenu"))
			isPreview = Convert.formatBoolean(req.getParameter("reloadMenu"));
		Boolean isKeystone = Convert.formatBoolean(req.getAttribute("isKeystone"), false);

		// Retrieve location info
		FranchiseVO f = fla.getLocationInfo(id, (isPreview || isKeystone));
		
		String cc = StringUtil.checkVal(f.getCountryCode());
		
		// Set Franchise Times
		fc.setTimes(new FranchiseTimeVO((Map<FranchiseTimeVO.DayType, String>) f.getAttributes().get("times"), cc));
			
		// retrieve buttons
		List<ButtonVO> buttons = fia.getButtonInfo(id);
		
		// Set the map data
		fc.setMapData(fla.setMapData(f));
		
		// Get the module Data
		Map<String, CenterModuleVO> modules = getModuleData(id, req, f.getUseGlobalMod());
		
		// Add the data to the module container
		fc.setModuleData(modules);
		fc.setButtons(buttons);
		fc.setFranchiseLocation(f);
		
		//set custom fields used by franchise views.
		fc.setCustomVals(PageContainerFactory.getInstance(StringUtil.checkVal(((SiteVO)req.getAttribute("siteData")).getLocale()), ((SiteVO)req.getAttribute("siteData")).getMobileFlag() == 1).getCustomVals());
		this.putModuleData(fc);
		req.getSession().setAttribute("FranchiseCountryCd", f.getCountryCode());
		
	}
	
	
	
	/**
	 * retrieves all of the data for the center page display
	 * @param id
	 * @return
	 */
	public Map<String, CenterModuleVO> getModuleData(String franId, SMTServletRequest req, int useGlobalModules) {
		Boolean isKeystone = Convert.formatBoolean(req.getAttribute("isKeystone"), false);				//In Webedit
		Boolean isPreview = Convert.formatBoolean(req.getAttribute(Constants.PAGE_PREVIEW), false);				//In Preview mode
		if (isPreview) {
			req.setAttribute("isKeystone", true); //this builds the query we need
			isKeystone = true; //This ensures that the most recent versions of the modules will appear on the site.
		}
		
		Integer locationId = Convert.formatInteger(req.getParameter("locationId"), 0);
		StringBuilder s = formatQuery(req);
		PreparedStatement ps = null;
		Map<String, CenterModuleVO> data = new LinkedHashMap<String, CenterModuleVO> ();
		int i = 0;
		Integer modTypeId = null;
		log.debug("isPreview: :" + isPreview);
		try {
			ps = dbConn.prepareStatement(s.toString());
			if (!isKeystone || (isKeystone && locationId == 0)) {
				ps.setString(++i, SyncStatus.Approved.toString());
				ps.setString(++i, SyncStatus.Declined.toString());
				ps.setInt(++i, Convert.formatInteger(franId));
			} else {
				if (locationId > 0) {
					ps.setInt(++i, locationId);
					log.debug("LocId: " + locationId);
				}
				
				ps.setInt(++i, Convert.formatInteger(req.getParameter("locationId")));
				ps.setInt(++i, Convert.formatInteger(req.getParameter("moduleId")));
				log.debug("MocId: " + Convert.formatInteger(req.getParameter("moduleId")));
				ps.setInt(++i, Convert.formatInteger(franId));
				
				if (req.getParameter("optionId") != null){
					ps.setInt(++i, Convert.formatInteger(req.getParameter("optionId")));
					log.debug("OptId: " + Convert.formatInteger(req.getParameter("optionId")));
				}
			}
			//log.debug(locationId + ", " + Convert.formatInteger(req.getParameter("moduleId")) + ", " + Convert.formatInteger(franId));
			ResultSet rs = ps.executeQuery();
			int lastLocnId = -1;
			CenterModuleVO cmVo = null;
			for (i=0; rs.next(); i++) {
				//log.debug("pkId=" + rs.getString("cp_module_option_id") + " parId=" + rs.getString("parent_id"));
				if (!isKeystone || (isKeystone && locationId == 0)) {
					//tabulate the RS by module location
					if (rs.getInt("cp_location_id") != lastLocnId) {
						if (i > 0)	data.put("MODULE_" + cmVo.getModuleLocationId(), cmVo);
						cmVo = new CenterModuleVO(rs, isKeystone);
						
					} else {
						if (isKeystone || StringUtil.checkVal(rs.getString("WC_SYNC_STATUS_CD")).length() == 0) {
							CenterModuleOptionVO opt = new CenterModuleOptionVO(rs);
							// If we are in webedit we add the sync data
							if (isKeystone) opt.setSyncData(new ApprovalVO(rs));
							cmVo.addOption(opt);
							
	//						for(CenterModuleOptionVO vo : cmVo.getModuleOptions().values())
	//						log.debug(vo.getApprovalFlag());
						}
					}
					
					lastLocnId = rs.getInt("cp_location_id");
					
				} else {
					//this view results in a List<CenterModuleOptionVO>.  cheap and easy!
					if (i == 0) cmVo = new CenterModuleVO(rs, isKeystone);
					else cmVo.addOption(new CenterModuleOptionVO(rs));
					if (modTypeId == null) modTypeId = rs.getInt("fts_cp_module_type_id");
					log.debug(rs.getString("CP_MODULE_FRANCHISE_XR_ID"));
				}
			}
			
			// Add the straggler
			if (cmVo != null) data.put("MODULE_" + cmVo.getModuleLocationId(), cmVo);
			
			// If we are using global modules we add the global assets to the modules here.
			if (useGlobalModules == 1 && !req.hasParameter("edit")) {
				appendGlobalAssets(data, isPreview);
			}
			
		} catch (Exception e) {
			log.error("Unable to get franchise info", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		/* THIS BLOCK IS ALL DEBUG CODE...*/
		if (log.isDebugEnabled()) {
			for (String val: data.keySet()) {
				CenterModuleVO vo = data.get(val);
				log.debug("Val: " + val + "|" + vo.getPageLocationId() + "|" + vo.getModuleLocationId() + "|" + vo.getModuleOptions().size());
			}
		}

		req.setAttribute("moduleTypeId", modTypeId);
		return data;
	}
	
	/**
	 * Get all global assets from the database and prepend them to the list of assets
	 */
	private void appendGlobalAssets(Map<String, CenterModuleVO> data, boolean preview) throws SQLException {
		log.debug("Gathering global assets.|"+preview);
		StringBuilder sql = new StringBuilder(430);
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		sql.append("SELECT * FROM ").append(customDb).append("FTS_CP_MODULE_OPTION cmo  ");
		sql.append("left join ").append(customDb).append("FTS_CP_MODULE_TYPE_XR mtx ");
		sql.append("on mtx.FTS_CP_MODULE_TYPE_ID = cmo.FTS_CP_MODULE_TYPE_ID ");
		sql.append("left join ").append(customDb).append("FTS_CP_MODULE m on m.CP_MODULE_ID = mtx.CP_MODULE_ID ");
		sql.append("left join ").append(customDb).append("FTS_CP_MODULE_OPTION child on child.PARENT_ID = cmo.CP_MODULE_OPTION_ID ");
		sql.append("WHERE cmo.FRANCHISE_ID = -1 and m.ORG_ID = 'FTS' ");
		if (preview) {
			sql.append("and ((cmo.APPROVAL_FLG = 1 and child.CP_MODULE_OPTION_ID is null) or cmo.APPROVAL_FLG = 100) ");
		} else {
			sql.append("and cmo.APPROVAL_FLG = 1 ");
		}
		sql.append("ORDER BY m.CP_MODULE_ID, cmo.CREATE_DT DESC");
		log.debug(sql);
		
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		ps = dbConn.prepareStatement(sql.toString());
		rs = ps.executeQuery();

		Integer lastModTypeId = 0;
		CenterModuleVO cmVo = null;
		Map<Integer, CenterModuleVO> globalModules = new HashMap<Integer, CenterModuleVO>();
		while (rs.next()) {
			if (!lastModTypeId.equals(rs.getInt("CP_MODULE_ID"))) {
				if (cmVo != null) globalModules.put(lastModTypeId, cmVo);

				lastModTypeId = rs.getInt("CP_MODULE_ID");
				cmVo = new CenterModuleVO();
				cmVo.addOption(new CenterModuleOptionVO(rs));
				log.debug(cmVo.getModuleOptions().size()+"|NEW");
			} else {
				cmVo.addOption(new CenterModuleOptionVO(rs));
				log.debug(cmVo.getModuleOptions().size()+"|ESTABLISHED");
			}
		}

		CenterModuleVO center = null;
		CenterModuleVO globalModule = null;
		Map<String, CenterModuleOptionVO> options;
		for (String key : data.keySet()) {
			center = data.get(key);
			log.debug(key+"|"+center.getModuleId()+"|"+center.getModuleName());
			globalModule = globalModules.get(center.getModuleId());
			if (globalModule != null) {
				options = globalModule.getModuleOptions();
				options.putAll(center.getModuleOptions());
				center.setModuleOptions(options);
			}
		}
	}

	/** the query used to retrieve the module data
	 *  this is a runtime decision that gets overwritten by the KeystoneCenterPageAction (a subclass of this)
	 *  BEWARE OF POLYMORPHISM, THIS QUERY IS NOT ALWAYS CALLLED WHEN WITHIN KEYSTONE!
	 * @param isApproval
	 * @param modOptId
	 * @param orderBy
	 * @return
	 */
	protected StringBuilder formatQuery(SMTServletRequest req) {
		Boolean isKeystone = Convert.formatBoolean(req.getAttribute("isKeystone"));
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		
		/*
		 * Verify if we are on a mobile site.  If not, check if we're working
		 * with a mobile site in webedit.
		 */
		boolean isMobile = site.getMobileFlag() == 1 ? true : false;
		if(!isMobile && site.getAliasPathName() != null && site.getAliasPathName().equals("webedit"))
			isMobile = Convert.formatBoolean((String)req.getSession().getAttribute("webeditIsMobile"));
		
		StringBuilder s = new StringBuilder();
		s.append("select *, c.create_dt as 'option_create_dt', c.franchise_id as 'option_franchise_id', ");
		s.append("c.cp_module_option_id as 'mod_opt_id', h.DISPLAY_PATH_TXT as 'DISPLAY_PATH_TXT_OVERRIDE' ");
		s.append("from ").append(customDb).append("FTS_CP_MODULE f ");
		s.append("inner join ").append(customDb).append("FTS_CP_LOCATION_MODULE_XR a ");
		s.append("on f.CP_MODULE_ID = a.CP_MODULE_ID ");
		s.append("inner join ").append(customDb).append("fts_cp_location g ");
		s.append("on a.cp_location_id = g.cp_location_id ");
		s.append("left outer join ").append(customDb).append("FTS_CP_MODULE_FRANCHISE_XR b ");
		s.append("on a.CP_LOCATION_MODULE_XR_ID = b.CP_LOCATION_MODULE_XR_ID ");
		s.append("left outer join ").append(customDb).append("FTS_CP_MODULE_OPTION c ");
		s.append("on b.CP_MODULE_OPTION_ID = c.CP_MODULE_OPTION_ID ");
		if (isKeystone) s.append(" or c.parent_id=b.cp_module_option_id ");
		s.append("left outer join ").append(customDb).append("FTS_CP_OPTION_ATTR d ");
		s.append("on c.CP_MODULE_OPTION_ID = d.CP_MODULE_OPTION_ID ");
		s.append("left outer join ").append(customDb).append("FTS_CP_MODULE_TYPE e ");
		s.append("on c.FTS_CP_MODULE_TYPE_ID = e.FTS_CP_MODULE_TYPE_ID ");
		s.append("left outer join ").append(customDb).append("FTS_CP_MODULE_DISPLAY h ");
		s.append("on a.FTS_CP_MODULE_DISPLAY_ID = h.FTS_CP_MODULE_DISPLAY_ID ");
		s.append("left join WC_SYNC ws on (c.CP_MODULE_OPTION_ID =  WC_KEY_ID or ");
		s.append("(c.PARENT_ID =  WC_ORIG_KEY_ID and WC_ORIG_KEY_ID != '0' and WC_ORIG_KEY_ID is not null)) and WC_SYNC_STATUS_CD not in (?,?) ");
		s.append("where a.FRANCHISE_ID = ? "); 
		
		if(isMobile) {
			s.append("and MOBILE_FLG = 1 ");
		} else {
			s.append("and (MOBILE_FLG = 0 or MOBILE_FLG is null) ");
		}
		
		s.append("order by g.ORDER_NO, b.order_no, d.ORDER_NO, c.option_nm");
		
		log.debug("CP Module Retrieve SQL: " + s);
		return s;
	}
	
	/**
	 * This method was added to allow for retrieval of the FranchiseId by any
	 * party.  The code was redundant across multiple classes so it was all
	 * placed here.  We also differentiate from the Franchise Id used by webedit (webeditFranId)
	 * and the sites Franchise Id (FranchiseId) as there was a bug that resulted in 
	 * improper results when navigating/editing items between webedit and the public site.
	 * @param req
	 * @return
	 */
	public static String getFranchiseId(SMTServletRequest req, boolean skipSession) {
		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
		String franId = null;
		
		if (site.getAliasPathName() != null && site.getAliasPathName().equals("webedit")) {
			franId =  (String) req.getSession().getAttribute("webeditFranId");
		}
		if (franId == null && !skipSession) {
			franId =  (String) req.getSession().getAttribute(FastsignsSessVO.FRANCHISE_ID);
		}
		if (franId == null) {
			//make a final attempt via regEx against siteId
			//the Matcher "finds" the entire SiteId (start ^, to finish $), 
			//and replaces it with the webId it finds nested within it.
			//The string is returning a subset of itself, basically.
			franId = site.getSiteId().replaceAll("^(.*)_([\\d]{1,5})_(.*)$", "$2");
		}
		
		return franId;
	}
	
	/**
	 * overloaded to bypass session dependance.
	 * @param req
	 * @return
	 */
	public static String getFranchiseId(SMTServletRequest req) {
		return getFranchiseId(req, false);
	}
}
