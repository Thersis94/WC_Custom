package com.fastsigns.action.franchise.centerpage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.fastsigns.action.approval.WebeditApprover.WebeditType;
import com.fastsigns.action.franchise.CenterPageAction;
import com.fastsigns.action.franchise.vo.CenterModuleOptionVO;
import com.fastsigns.action.franchise.vo.OptionAttributeVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.SMTMail;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.approval.ApprovalController;
import com.smt.sitebuilder.approval.ApprovalException;
import com.smt.sitebuilder.approval.ApprovalController.ModuleType;
import com.smt.sitebuilder.approval.ApprovalController.SyncStatus;
import com.smt.sitebuilder.approval.ApprovalController.SyncTransaction;
import com.smt.sitebuilder.approval.ApprovalVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: ModuleOptionAction <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> This class handles all the module related calls for 
 * CenterPageAction.  
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Billy Larsen
 * @version 1.0
 * @since Feb. 11, 2013<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class ModuleOptionAction extends SBActionAdapter{
	//This is a list of the modules that only allow one item at a time
	final String modList = "11,12,81,";
	
	public ModuleOptionAction(ActionInitVO avo){
		super(avo);
	}
	
	@Override
	public void update(SMTServletRequest req) throws ActionException {
		log.debug("Saving Center Page Info");
		String msg = "msg.updateSuccess";
		
		//This variable holds the build Type requested by the view.
		Integer bType = Convert.formatInteger(req.getParameter("bType"));
		log.debug("type=" + bType);
		log.debug(((SiteVO)req.getAttribute("siteData")).getCountryCode());
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		String redir = page.getFullPath() + "?";
		String siteId = ((SiteVO)req.getAttribute("siteData")).getOrganizationId() + "_" + CenterPageAction.getFranchiseId(req) + "_1";
		if (req.getParameter("apprFranchiseId") != null)
			siteId = ((SiteVO)req.getAttribute("siteData")).getOrganizationId() + "_" + req.getParameter("apprFranchiseId") + "_1";
		
		//turn off string encoding since this is an administrative (& secure) method call
		req.setValidateInput(Boolean.FALSE);
		
		// Determine which data is being updated
		try {
			switch(bType) {
				case CenterPageAction.MODULE_SUBMIT:
					submitForApproval(req);
					break;
				case CenterPageAction.MODULE_ASSOC:
					msg = "msg.selectOptions";
					try {
						this.addNewModule(req);
					} catch (InvalidDataException ide) {
						msg = "msg.selectError"; 
						break;
					}
					redir += "assoc=true&locationId=" + req.getParameter("locationId") + "&moduleId=" + req.getParameter("moduleId") + "&type=" + req.getParameter("type") + "&";
					super.clearCacheByGroup(siteId);
					break;
				case CenterPageAction.MODULE_DELETE:
					deleteModuleOption(req);
					super.clearCacheByGroup(siteId);
					redir += "assoc=true&locationId=" + req.getParameter("locationId") + "&moduleId=" + req.getParameter("moduleId") + "&";
					redir += "type=" + StringEncoder.urlEncode(req.getParameter("type")) + "&";
					break;
				case CenterPageAction.MODULE_LOC_DELETE:
					this.deleteModuleLocation(req);
					super.clearCacheByGroup(siteId);
					break;
				case CenterPageAction.MODULE_ADD:
					this.saveModuleOption(req);
					redir += "assoc=true&locationId=" + req.getParameter("locationId") + "&moduleId=" + req.getParameter("moduleId") + "&";
					redir += "type=" + StringEncoder.urlEncode(req.getParameter("type")) + "&";
					String parentId = StringUtil.checkVal(req.getParameter("parentId"), null);
					//if the module we just edited was already pending approval, we must remove that flag (the new module will now be the one needing approval)
					if (parentId != null  && Convert.formatInteger(req.getParameter("approvalFlag"), 0).intValue() == 100)
						this.revokeApprovalSubmission(req);
					
					// The comma at the end of the parameter ensures that we won't get partial matches
					// Such as matching the 8 in 81
					if (!modList.contains(req.getParameter("moduleId")+",")) {
						req.setParameter("skipDelete", "true");
					}
					
					// Determine whether we are dealing with a edit of the original or an edit of an edit.
					if (parentId == null) {
						req.setParameter("parentModuleId", req.getParameter("moduleOptionId"));
					} else {
						req.setParameter("parentModuleId", parentId);
					}
					if (Convert.formatBoolean(req.getParameter("insOnly")))
						break;
					
				case CenterPageAction.MODULE_OPTION_UPDATE:
					this.updateModuleOptions(req);
					super.clearCacheByGroup(siteId);
					break;
				case CenterPageAction.MODULE_REARRANGE:
					rearrangeModuleLayout(req);
					super.clearCacheByGroup(siteId);
					break;
				case CenterPageAction.MODULE_VIEW_UPDATE:
					updateModuleView(req);
					super.clearCacheByGroup(siteId);
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
	 * Submit the module assets for approval
	 * @throws ActionException 
	 */
	private void submitForApproval(SMTServletRequest req) throws SQLException, ActionException {
		log.debug("Beginning Request Module Option Approval Process...");
		String opts = req.getParameter("modOptsToSubmit");
		if (opts == null || opts.length() == 0) return;
		
		List<ApprovalVO> apprVOs = new ArrayList<>();
		String[] ids = req.getParameter("modOptsToSubmit").split(",");
		StringBuilder sql = new StringBuilder(60);
		
		sql.append("SELECT * FROM WC_SYNC WHERE WC_KEY_ID in (");
		for (int i=0; i<ids.length; i++) {
			sql.append("?");
			if (i < ids.length-1) {
				sql.append(",");
			} else {
				sql.append(") ");
			}
		}
		sql.append("and WC_SYNC_STATUS_CD = ?");
		
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int i;
			for (i=0; i<ids.length; i++) {
				ps.setString(i+1, ids[i]);
			}
			ps.setString(i+1, SyncStatus.InProgress.toString());
			
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				ApprovalVO approval = new ApprovalVO(rs);
				approval.setUserDataVo((UserDataVO) req.getSession().getAttribute(Constants.USER_DATA));
				approval.setSyncTransaction(SyncTransaction.Submit);
				apprVOs.add(approval);
			}
			
			ApprovalController con = new ApprovalController(dbConn, getAttributes());
			con.process(apprVOs.toArray(new ApprovalVO[apprVOs.size()]));
			

			req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
			req.setAttribute(Constants.REDIRECT_URL, ((PageVO) req.getAttribute(Constants.PAGE_DATA)).getFullPath());
			
		} catch (Exception e) {
			log.error("Unable to submit all jobs for approval.", e);
			throw new ActionException(e);
		}
		
	}

	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		
	}
	
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		/*
		 * If request is a testimonial then save the response as new Module Option
		 * and send out and announcement to franchise Owners that a testimonial
		 * has been submitted and redirect back to the Center Page.
		 */
		if (Convert.formatBoolean(req.getParameter("isTestimonial"))) {
			log.debug("saving testimonial");
			try {
				this.saveModuleOption(req);
				this.sendTestimonalAnnouncement(req);
			} catch (Exception e) {
				throw new ActionException(e);
			}
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			StringBuilder url = new StringBuilder(page.getRequestURI());
			url.append("?pmid=").append(req.getParameter("pmid"));
			url.append("&testimonialForm=true&printerFriendlyTheme=true&hidePf=true&submitted=true");

			super.sendRedirect(url.toString(), null, req);
		} else {
			update(req);
		}
	}
	
	/**
	 * This method deletes a module from a given location.
	 * @param req
	 * @throws SQLException
	 */
	public void deleteModuleLocation(SMTServletRequest req) throws SQLException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String locationId = req.getParameter("locationId");
		StringBuilder s = new StringBuilder();
		s.append("delete from ").append(customDb).append("fts_cp_location_module_xr ");
		s.append("where cp_location_module_xr_id = ?");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(1, locationId);
			ps.executeUpdate();
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
	}
	
	/**
	 * This method deletes a specific Module Option.
	 * @param req
	 */
	public void deleteModuleOption(SMTServletRequest req) {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String optionId = req.getParameter("deleteId");
		StringBuilder s = new StringBuilder();
		s.append("delete from ").append(customDb).append("fts_cp_module_option ");
		s.append("where cp_module_option_id = ?");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(1, optionId);
			ps.executeUpdate();
		} catch (SQLException e) {
			log.debug(e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
	}
	
	/**
	 * This method adds a module to a given location on the center page.
	 * @param req
	 */
	public void addNewModule(SMTServletRequest req) throws SQLException, InvalidDataException {
		boolean isMobile = Convert.formatBoolean((String)req.getSession().getAttribute("webeditIsMobile"));
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		Integer locnId = Convert.formatInteger(req.getParameter("locationId"));
		String franId = StringUtil.checkVal(CenterPageAction.getFranchiseId(req));
		
		//test to see if a module already exists in this slot.  Throw an error if so
		if (this.getCpLocnModXRPkId(locnId, franId, isMobile) != null)
			throw new InvalidDataException("Module already exists");
		
		StringBuilder s = new StringBuilder();
		s.append("insert into ").append(customDb).append("fts_cp_location_module_xr ");
		s.append("(cp_location_id, franchise_id, cp_module_id, create_dt) ");
		s.append("values (?,?,?,?) ");
		log.debug("add Module SQL: " + s + "|" + locnId + "|" + franId + "|" +req.getParameter("moduleId"));
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setInt(1, locnId);
			ps.setString(2, franId);
			ps.setString(3, StringUtil.checkVal(req.getParameter("moduleId")));
			ps.setTimestamp(4, Convert.getCurrentTimestamp());
			ps.executeUpdate();
			
			//retrieve the inserted pkId so we can forward the user to the "edit module" screen directly
			Integer pkId = this.getCpLocnModXRPkId(locnId, franId, isMobile);
			req.setParameter("locationId", pkId.toString());
			log.debug(req.getParameter("moduleNm_" + req.getParameter("moduleId")));
			req.setParameter("type", req.getParameter("moduleNm_" + req.getParameter("moduleId")));
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
	}

	/**
	 * This method updates the attributes tied to a module (poll/survey/list)
	 * @param req
	 * @param optVO
	 */
	private void updateModuleAttributes(SMTServletRequest req, CenterModuleOptionVO optVO){
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		//loop over the attributes and add them to a list for processing
		String [] ints = req.getParameterValues("attrNumber");
		List<OptionAttributeVO> vos = new ArrayList<OptionAttributeVO>();
		for(String s : ints){
			OptionAttributeVO v = getNextUpdate(req, Convert.formatInteger(s));
			if(v != null)
			vos.add(v);
		}
		
		//write all attributes in list to db
		StringBuilder attrStr = new StringBuilder();
		attrStr.append("insert into ").append(customDb).append("FTS_CP_OPTION_ATTR ");
		attrStr.append("(CP_MODULE_OPTION_ID, ATTR_KEY_CD, ATTRIB_VALUE_TXT, ORDER_NO, CREATE_DT, ");
		attrStr.append("ACTIVE_FLG, ATTR_PARENT_ID) values(?,?,?,?,?,?,?)");
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(attrStr.toString());
			for(int i = 0; i < vos.size(); i++){
				OptionAttributeVO vo = vos.get(i);
				ps.setString(1, optVO.getModuleOptionId());
				ps.setString(2, (String)vo.getKey());
				ps.setInt(3, Convert.formatInteger((String)vo.getValue()));
				ps.setInt(4, Convert.formatInteger(vo.getOrderNo()));
				ps.setTimestamp(5, Convert.getCurrentTimestamp());
				ps.setInt(6, 1);
				if(vo.getParentId() != null)
					ps.setString(7, vo.getParentId());
				else
					ps.setString(7, null);
				ps.addBatch();
			}
			ps.executeBatch();
		} catch(SQLException sqle){
			log.error(sqle);
		} finally{
			try{ps.close();}catch(Exception e){}
		}
	}

	/**
	 * retrieves the primary key of an inserted record
	 * @param locationId
	 * @param franchiseId
	 * @return
	 */
	private Integer getCpLocnModXRPkId(Integer locationId, String franchiseId, boolean isMobile) {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		Integer pkId = null;
		StringBuilder s = new StringBuilder();
		s.append("select cp_location_module_xr_id from ").append(customDb);
		s.append("fts_cp_location_module_xr a ");
		s.append("inner join ").append(customDb).append("FTS_CP_MODULE b ");
		s.append("on a.CP_MODULE_ID = b.CP_MODULE_ID ");
		s.append("where cp_location_id=? and franchise_id=? and MOBILE_FLG =?");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setInt(1, locationId);
			ps.setString(2, franchiseId);
			ps.setInt(3, Convert.formatInteger(isMobile));
			ResultSet rs = ps.executeQuery();
			if (rs.next()) pkId = rs.getInt(1);
		} catch (SQLException sqle) {
			log.error(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		return pkId;
	}

	/**
	 * This method handles updating the Module Options on a page.
	 * @param req
	 */
	public void updateModuleOptions(SMTServletRequest req) throws SQLException {
		// If we are dealing with an omnipresent global module asset we skip the assignment phase
		if ("g".equals(req.getParameter("globalFlg"))) return;

		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String[] options = req.getParameterValues("selectedElements");
		String locationId = req.getParameter("locationId");
		boolean skipDelete = Convert.formatBoolean(req.getParameter("skipDelete"), false);
		StringBuilder dSql = new StringBuilder(100);
		
		if (!skipDelete) {
			dSql.append("delete from ").append(customDb).append("FTS_CP_MODULE_FRANCHISE_XR ");
			dSql.append("where CP_LOCATION_MODULE_XR_ID = ? ");
		}
		
		StringBuilder iSql = new StringBuilder(170);
		iSql.append("insert into ").append(customDb).append("FTS_CP_MODULE_FRANCHISE_XR ");
		iSql.append("(cp_location_module_xr_id, cp_module_option_id, order_no, create_dt) ");
		iSql.append("values (?,?,?,?)");
		
		PreparedStatement psIns = null;
		PreparedStatement psDel = null;
		try {
			// Delete the existing records
			// This step will be skipped when we are adding a new asset to a module that allows multiple assets.
			if (!skipDelete) {
				psDel = dbConn.prepareStatement(dSql.toString());
				psDel.setString(1, locationId);
				psDel.executeUpdate();
			}
			
			// Add new records
			psIns = dbConn.prepareStatement(iSql.toString());
			for (int i = 0; i < options.length; i++) {
				String[] opts = options[i].split("~"); //split is key ~ order-by-index
				int idx = i+1; //default ordering
				if (opts.length == 2) idx = Convert.formatInteger(opts[1], idx);
				
				if (req.hasParameter("parentModuleId"))
					opts[0] = req.getParameter("parentModuleId");
				
				psIns.setString(1, locationId);
				psIns.setString(2, opts[0]);
				psIns.setInt(3, idx);
				psIns.setTimestamp(4, Convert.getCurrentTimestamp());
				psIns.addBatch();
				log.debug("added assoc for " + opts[0]);
			}
			
			// Execute the batched transactions
			psIns.executeBatch();
			
		} finally {
			try {
				psIns.close();
				psDel.close();
			} catch(Exception e) {}
		}
	}

	/**
	 * 
	 * This method is used to build the OptionAttributeVO's used on polls and 
	 * Column Lists.
	 * @param req
	 * @param i
	 * @return
	 */
	private OptionAttributeVO getNextUpdate(SMTServletRequest req, int i){
		OptionAttributeVO v = null;
		String label = StringUtil.checkVal(req.getParameter("labelText_" + i));
		String value = StringUtil.checkVal(req.getParameter("attrValue_" + i), "0");
		int orderNo = Convert.formatInteger(StringUtil.checkVal(req.getParameter("orderNo_" + i), "1"));
		String id = StringUtil.checkVal(req.getParameter("attrId_" + i), null); 
		if(label.length() > 0){
			v = new OptionAttributeVO(label, value);
			v.setOrderNo(orderNo);
			v.setParentId(id);
		}
		return v;
	}
	
	/**
	 * This method handles saving changes to a Module Option.
	 * @param req
	 * @throws SQLException
	 */
	private void saveModuleOption(SMTServletRequest req) throws SQLException {
		final String customDb = String.valueOf(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		StringBuilder sb = new StringBuilder();
		Integer franchiseId = Convert.formatInteger(CenterPageAction.getFranchiseId(req));	//Get Franchise Id
		String orgId = req.getParameter("organizationId");					//Get Org Id
	
		//If orgId not found, look for it on the siteVO
		if(orgId == null){
			SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
			orgId = site.getAliasPathOrgId();
		}
		log.debug("franId=" + franchiseId + " and orgId=" + orgId);

		//UserRoleVO role = (UserRoleVO) req.getSession().getAttribute(Constants.ROLE_DATA);
		CenterModuleOptionVO vo = new CenterModuleOptionVO(req);
		boolean isInsert = false;
		//modules that get created without parents and are not submitted should be identifiable
		boolean isParent = (StringUtil.checkVal(vo.getModuleOptionId(),null) == null 
				&& StringUtil.checkVal(vo.getParentId(),null)==null);
		// Determine if this is an omnipresent global asset.  These are treated differently from normal assets
		String globalAsset = StringUtil.checkVal(req.getParameter("globalFlg"));
		
		//log.info("is insert " + isInsert + " is parent " + isParent);
		//PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		//log.info("page id " + page.toString());
		
		//if the user is not a global admin, and this is an update to an existing module,
		//treat it as a NEW module.  This behavior will ensure the module gets approved
		//before it's visible on the website.
		//if (role.getRoleLevel() < SecurityController.ADMIN_ROLE_LEVEL) {
			if (StringUtil.checkVal(vo.getParentId(),null) == null)  {
				vo.setParentId(vo.getModuleOptionId()); //link this new entry to it's predecessor
				isInsert = true;
			}
			req.setParameter("moduleParentId", StringUtil.checkVal(vo.getModuleOptionId()));
			log.debug("saving with parent=" + vo.getParentId());
		//}

		if ("g".equals(globalAsset)) {
			vo.setFranchiseId(-1);
		} else if (!"y".equals(globalAsset)){
			vo.setFranchiseId(franchiseId);
		}
		
		//build the query
		if (isInsert) {
			vo.setModuleOptionId(new UUIDGenerator().getUUID());
			req.setParameter("optionId", vo.getModuleOptionId());
			if (isParent)
				vo.setParentId(vo.getModuleOptionId());
			
			sb.append("insert into ").append(customDb);
			sb.append("FTS_CP_MODULE_OPTION (OPTION_NM, ");
			sb.append("OPTION_DESC, ARTICLE_TXT, RANK_NO, LINK_URL, FILE_PATH_URL, THUMB_PATH_URL, VIDEO_STILLFRAME_URL, ");
			sb.append("CONTENT_PATH_TXT, START_DT, END_DT, CREATE_DT, RESPONSE_TXT, FTS_CP_MODULE_ACTION_ID, FRANCHISE_ID, ");
			sb.append("FTS_CP_MODULE_TYPE_ID, APPROVAL_FLG, PARENT_ID, ORG_ID, CP_MODULE_OPTION_ID) ");
			sb.append("values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		} else {
			sb.append("update ").append(customDb);
			sb.append("FTS_CP_MODULE_OPTION set OPTION_NM=?, ");
			sb.append("OPTION_DESC=?, ARTICLE_TXT=?, RANK_NO=?, LINK_URL=?, FILE_PATH_URL=?, ");
			sb.append("THUMB_PATH_URL=?, VIDEO_STILLFRAME_URL=?, CONTENT_PATH_TXT=?, START_DT=?, END_DT=?, ");
			sb.append("CREATE_DT=?, RESPONSE_TXT=?, FTS_CP_MODULE_ACTION_ID=?, FRANCHISE_ID=? where CP_MODULE_OPTION_ID=?");
		}
		log.debug(sb);
		PreparedStatement ps = null;
		int i = 0;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(++i, vo.getOptionName());
			ps.setString(++i, vo.getOptionDesc());
			ps.setString(++i, vo.getArticleText());
			ps.setInt(++i, vo.getRankNo());
			ps.setString(++i, vo.getLinkUrl());
			ps.setString(++i, vo.getFilePath());
			ps.setString(++i, vo.getThumbPath());
			ps.setString(++i, vo.getStillFramePath());
			ps.setString(++i, vo.getContentPath());
			ps.setDate(++i, Convert.formatSQLDate(vo.getStartDate()));
			ps.setDate(++i, Convert.formatSQLDate(vo.getEndDate()));
			ps.setTimestamp(++i, Convert.getCurrentTimestamp());
			ps.setString(++i, vo.getResponseText());
			ps.setString(++i, vo.getActionId());
			if (vo.getFranchiseId() == null) {
				ps.setNull(++i, java.sql.Types.INTEGER);
			} else {
				ps.setInt(++i, vo.getFranchiseId());
			}
			if (isInsert) {
				 ps.setInt(++i, vo.getModuleTypeId()); 
				 if ("g".equals(globalAsset)) {
					 ps.setInt(++i, 100);
				 } else {
					 ps.setInt(++i, vo.getApprovalFlag());
				 }
				 ps.setString(++i, vo.getParentId());
				 ps.setString(++i, orgId);
			}
			ps.setString(++i, vo.getModuleOptionId());
			ps.executeUpdate();
			
			// Only create a sync entry if this is an insert for a non-global asset
			if (isInsert)
				buildSyncEntry(req, vo, globalAsset);
			
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		req.setParameter("selectedElements", vo.getModuleOptionId()+"~"+req.getParameter("modLocId"));
		if(vo.getModuleTypeId() == 10)
			updateModuleAttributes(req, vo);
	}
	
	/**
	 * Build a sync entry to track the action that is being edited
	 * @param req
	 * @param approvalType
	 */
	private void buildSyncEntry(SMTServletRequest req, CenterModuleOptionVO vo, String globalAsset) {
		ApprovalController controller = new ApprovalController(dbConn, getAttributes());
		ApprovalVO approval = new ApprovalVO();
		WebeditType approvalType = WebeditType.CenterModule;
		String orgId = ((SiteVO)req.getAttribute("siteData")).getOrganizationId();
		boolean isParent = ((StringUtil.checkVal(vo.getParentId(),null) == null) 
				|| vo.getModuleOptionId().equals(vo.getParentId()));

		approval.setWcKeyId(StringUtil.checkVal(vo.getModuleOptionId()));
		//Only set this if we actually have a valid parent id
		if (! isParent)
			approval.setOrigWcKeyId(vo.getParentId());
		approval.setItemDesc(approvalType.toString());
		approval.setItemName(vo.getOptionName());
		approval.setModuleType(ModuleType.Webedit);
		if ("g".equals(globalAsset)) {
			approval.setSyncStatus(isParent ? SyncStatus.PendingCreate : SyncStatus.PendingUpdate );
		} else {
			approval.setSyncStatus(SyncStatus.InProgress);
		}
		approval.setSyncTransaction(SyncTransaction.Create);

		if (!"n".equals(globalAsset)) {
			approval.setOrganizationId(orgId);
		} else {
			approval.setOrganizationId(orgId+"_"+vo.getFranchiseId());
		}
		if (vo.getModuleTypeId() == 2) {
			approval.setParentId(orgId);
		}
		
		if ( req.getParameter("testimonialForm") != null && 
				req.getParameter("testimonialForm").equals("true") &&
				req.getSession().getAttribute(Constants.USER_DATA) == null){
			UserDataVO userVo = new UserDataVO();
			userVo.setProfileId("public-Side-Submission");
			req.getSession().setAttribute(Constants.USER_DATA, userVo);
		
		}
		
		approval.setUserDataVo((UserDataVO) req.getSession().getAttribute(Constants.USER_DATA));
		approval.setCreateDt(Convert.getCurrentTimestamp());
		try {
			controller.process(approval);
		} catch (ApprovalException e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * Reset the approval flag to zero when a pending-approval module is edited
	 * This is done because once edits are made the module must be re-submitted for approval
	 * (the cycle resets!)
	 * @param req
	 * @throws SQLException
	 */
	private void revokeApprovalSubmission(SMTServletRequest req) throws SQLException {
		StringBuilder sb = new StringBuilder();
		sb.append("update ").append(String.valueOf(getAttribute(Constants.CUSTOM_DB_SCHEMA)));
		sb.append("FTS_CP_MODULE_OPTION set APPROVAL_FLG=? where parent_id=?");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setInt(1, 0);
			ps.setString(2, StringUtil.checkVal(req.getParameter("parentId")));
			ps.executeUpdate();
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
	}
	
	/**
	 * This method handles re-arranging modules on a page.  
	 * @param req
	 */
	private void rearrangeModuleLayout(SMTServletRequest req) throws SQLException {
		log.info("processing module rearrangement");
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder s = new StringBuilder();
		s.append("update ").append(customDb).append("FTS_CP_LOCATION_MODULE_XR set ");
		s.append("cp_location_id=?, create_dt=? where cp_location_module_xr_id=? ");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			
			//create a batch and execute all at once.
			int idx = 1; //CP_LOCATION_ID
			String reqVal = "";
			
			for (int x=1; (reqVal = req.getParameter("module" + x)) != null; x++) {
				String[] vals = reqVal.split("~");
				log.debug("received module" + x + "=" + reqVal + ", s=" + vals.length);
				
				if (vals.length != 2) continue;
				
				int locationId = Convert.formatInteger(vals[0]);
				if (locationId == 0) { //this is an empty module, increment the counter and continue (does not go in DB)
					idx += Convert.formatInteger(vals[1], 1);
					continue;
				}
				
				
				ps.setInt(1, idx);
				ps.setTimestamp(2, Convert.getCurrentTimestamp());
				ps.setInt(3, locationId);
				ps.addBatch();
				log.debug("saving " + locationId + " to MODULE_" + idx);
				
				//increment the cp_location_id using #columns used by this module
				idx += Convert.formatInteger(vals[1], 1);
			}
			
			ps.executeBatch();
			
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
	}
	
	/**
	 * This method handles changing the view of a Module Location.
	 * @param req
	 */
	private void updateModuleView(SMTServletRequest req) {
		String displayId = req.getParameter("displayId");
		String modLocId = req.getParameter("modLocId");
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);

		StringBuilder sql = new StringBuilder();
		sql.append("update ").append(customDb).append("FTS_CP_LOCATION_MODULE_XR ");
		sql.append("set FTS_CP_MODULE_DISPLAY_ID = ? ");
		sql.append("where CP_LOCATION_MODULE_XR_ID = ?");
		PreparedStatement ps = null;
		try {
			 ps = dbConn.prepareStatement(sql.toString());
			 ps.setString(1, displayId);
			 ps.setString(2, modLocId);
			 ps.executeUpdate();
		} catch (SQLException e) {
			log.debug(e);
		} finally {
				try {
					if (ps != null) ps.close();
				} catch (SQLException e) {
					log.debug(e);
				}
		}
		
		
		
	}
	
	/**
	 * This method handles sending an email to FS that a new testimonial has been
	 * submitted.
	 * @param req
	 */
	private void sendTestimonalAnnouncement(SMTServletRequest req) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		String[] rcpts = StringUtil.checkVal(req.getParameter("contactEmailAddress")).split(",");
		log.debug("emailing " + rcpts.length + " users");
		String subject = "MKT: ACTION REQUIRED: A center website review has been submitted for approval";
		SMTMail mail = null;
		try {
			mail = new SMTMail((String)getAttribute(Constants.CFG_SMTP_SERVER));
			mail.setUser((String)getAttribute(Constants.CFG_SMTP_USER));
			mail.setPassword((String)getAttribute(Constants.CFG_SMTP_PASSWORD));
			mail.setPort(Integer.valueOf((String)getAttribute(Constants.CFG_SMTP_PORT)));
			mail.setFrom(site.getMainEmail());
			mail.setRecpt(rcpts);
			mail.setSubject(subject);
			mail.setHtmlBody(getMessageText(req, site));
			mail.postMail();
		} catch (MailException me) {
			log.error("could not notify eteam of new testimonial", me);
		}
	}
	
	/**
	 * Returns the Message Text for Testimonial Emails.
	 * @param req
	 * @param site
	 * @return
	 */
	private String getMessageText(SMTServletRequest req, SiteVO site) {
		//Update Comany String based on Country Code
		String company = "FASTSIGNS";
		if(site.getCountryCode().equals("AU")){
			company = "SIGNWAVE";
		}
		StringBuilder msg = new StringBuilder();
		msg.append("<p>The following review was submitted to your center website by a ");
		msg.append("website visitor.  To display this review on your website, please ");
		msg.append("review and approve:</p>");
		msg.append("<p>").append(req.getParameter("articleText")).append("</p>");
		msg.append("<p>A copy of the pending review has also been sent to eteam@fastsigns.com ");
		msg.append("(" + company + " Internet Marketing).  If the review is positive, the eteam will ");
		msg.append("approve and publish on your behalf.<br/>");
		msg.append("If the review is negative, no action will be taken by the eteam.</p>");
		msg.append("<br/><p>Thanks,<br/>");
		msg.append("Renae Fogarty<br/>");
		msg.append("eteam@fastsigns.com<br/>");
		msg.append("<a href='" + site.getSiteAlias() + "/webedit'>" + site.getSiteAlias() + "/webedit</a>");
		msg.append("</p><br/><br/>");				
		return msg.toString();
	}
}
