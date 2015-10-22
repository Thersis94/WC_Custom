package com.fastsigns.action.franchise.centerpage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.fastsigns.action.approval.WebeditApprover;
import com.fastsigns.action.approval.WebeditApprover.WebeditType;
import com.fastsigns.action.franchise.CenterPageAction;
import com.fastsigns.action.franchise.SocialProfileMapAction;
import com.fastsigns.action.franchise.vo.ButtonVO;
import com.fastsigns.action.franchise.vo.FranchiseVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.PhoneNumberFormat;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.approval.ApprovalController;
import com.smt.sitebuilder.approval.ApprovalController.SyncStatus;
import com.smt.sitebuilder.approval.ApprovalController.SyncTransaction;
import com.smt.sitebuilder.approval.ApprovalException;
import com.smt.sitebuilder.approval.ApprovalVO;
import com.smt.sitebuilder.approval.ApprovalController.ModuleType;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: FranchiseInfoAction <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> This class handles all the Franchise Data related calls 
 * for CenterPageAction.  
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Billy Larsen
 * @version 1.0
 * @since Feb. 11, 2013<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class FranchiseInfoAction extends SBActionAdapter {

	public static final String LOCATION_HANDLE = "[location]";
	public static final String PHONE_NO_HANDLE = "[telephone number]";
	
	public FranchiseInfoAction(ActionInitVO avo){
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
				case CenterPageAction.FRANCHISE_MAIN_IMAGE_APPROVE:
					// This is no longer needed
					//sendToApprovalAction(req);
					break;
				case CenterPageAction.FRANCHISE_BUTTON_UPDATE:
					updateFranchiseButton(req);
					super.clearCacheByGroup(siteId);
					break;
				case CenterPageAction.FRANCHISE_DESC_UPDATE:
					updateFranchiseDesc(req);
					super.clearCacheByGroup(siteId);
					break;
				case CenterPageAction.FRANCHISE_CUSTOM_DESC_UPDATE:
					updateDescText(req);
					super.clearCacheByGroup(siteId);
					break;
				case CenterPageAction.FRANCHISE_CUSTOM_DESC_DELETE:
					deleteDescText(req);
					super.clearCacheByGroup(siteId);
					break;
				case CenterPageAction.FRANCHISE_MAIN_IMAGE_UPDATE:
					updateMainImage(req);
					//super.clearCacheGroup(siteId);	this requires admin approval now, so don't flush just yet.
					break;
				case CenterPageAction.FRANCHISE_SOCIAL_MEDIA_LINKS:
					updateSocialMediaLinks(req);
					super.clearCacheByGroup(siteId);
					break;
				case CenterPageAction.WHITEBOARD_UPDATE:
					updateWhiteboard(req);
					//super.clearCacheGroup(siteId);	this requires admin approval now, so don't flush just yet.
					break;
				case CenterPageAction.RESELLER_UPDATE:
					updateResellerButtons(req);
					super.clearCacheByGroup(siteId);
					break;
				case CenterPageAction.RAQSAF_UPDATE:
					updateRAQSAF(req);
					super.clearCacheByGroup(siteId);
					break;
				case CenterPageAction.GLOBAL_MODULE_UPDATE:
					updateGlobalModule(req);
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
	 * Update the USE_GLOBAL_MODULE flag for the submitted center
	 * This flag determines whether global assets and modules (items with
	 * franchise id of -1) should appear on the center's main page.
	 * @param req
	 */
	private void updateGlobalModule(SMTServletRequest req) throws SQLException {
		log.debug("Beginning global module preference update");
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		String franchiseId = CenterPageAction.getFranchiseId(req);
		int useGlobalModules = Convert.formatInteger(req.getParameter("useGlobalModules"));
		
		sql.append("UPDATE ").append(customDb).append("FTS_FRANCHISE ");
		sql.append("SET USE_GLOBAL_MODULES_FLG = ? WHERE FRANCHISE_ID = ?");
		log.debug(sql.toString() + "|" + useGlobalModules + "|" + franchiseId);
		
		PreparedStatement ps = null;
		try {
		ps = dbConn.prepareStatement(sql.toString());
		
		ps.setInt(1, useGlobalModules);
		ps.setString(2, franchiseId);
		
		if (ps.executeUpdate() < 1) 
			log.error("No centers updated when attempting to change global module preferences for center " + franchiseId);
	
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
	}
	
	/**
	 * Update the reseller buttons that can appear on the center's homepage
	 * @param req
	 * @throws SQLException
	 */
	private void updateResellerButtons(SMTServletRequest req) throws SQLException {
		log.debug("Beginning Reseller Button Update.");
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(160);
		String franchiseId = CenterPageAction.getFranchiseId(req);
		
		sql.append("UPDATE ").append(customDb).append("FTS_FRANCHISE ");
		sql.append("SET RESELLER_BUTTON_ID = ?, RESELLER_BUTTON_LINK = ? ");
		sql.append("WHERE FRANCHISE_ID = ?");
		
		log.debug(sql.toString() + "|" + req.getParameter("resellerTypeId") + ", " + req.getParameter("resellerLink") + ", " + franchiseId);
		
		PreparedStatement ps = null;

		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, req.getParameter("resellerTypeId"));
			ps.setString(2, req.getParameter("resellerLink"));
			ps.setString(3, franchiseId);
			
			if (ps.executeUpdate() < 1) 
				log.error("Franchise " + franchiseId + " was unable to update it's reseller button.");
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
	}
	
	/**
	 * Update whether or not a center 
	 * @param req
	 * @throws SQLException
	 */
	private void updateRAQSAF(SMTServletRequest req) throws SQLException {
		log.debug("Beginning Reseller Button Update.");
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		String franchiseId = CenterPageAction.getFranchiseId(req);
		
		sql.append("UPDATE ").append(customDb).append("FTS_FRANCHISE ");
		sql.append("SET USE_RAQSAF = ? WHERE FRANCHISE_ID = ?");
		
		log.debug(sql.toString() + "|" + req.getParameter("resellerTypeId") + ", " + franchiseId);
		
		PreparedStatement ps = dbConn.prepareStatement(sql.toString());

		ps.setString(1, req.getParameter("raqsaf"));
		ps.setString(2, franchiseId);
		
		if (ps.executeUpdate() < 1) 
			log.error("Franchise " + franchiseId + " was unable to update it's RAQ/SAF button.");
	}

	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		
	}
	
	@Override
	public void build (SMTServletRequest req) throws ActionException {
		
	}
	
	/**
	 * This method updates the buttons that appear under the left menu on the 
	 * franchise page.
	 * @param req
	 */
	public void updateFranchiseButton(SMTServletRequest req) throws SQLException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder s = new StringBuilder();
		s.append("insert into ").append(customDb).append("fts_franchise_button_xr ");
		s.append("(center_button_id, franchise_id, create_dt) ");
		s.append("values (?,?,?) ");
		
		StringBuilder del = new StringBuilder();
		del.append("delete from ").append(customDb).append("fts_franchise_button_xr ");
		del.append("where franchise_id = ? ");
		
		PreparedStatement ps = null;
		PreparedStatement psDel = null;
		try {
			// Delete the existing entries in the Button Associative table
			ps = dbConn.prepareStatement(s.toString());
			psDel = dbConn.prepareStatement(del.toString());
			psDel.setString(1, CenterPageAction.getFranchiseId(req));
			psDel.executeUpdate();
			
			// Loop through the buttons selected and add them to the associative table
			String[] buttonIds = req.getParameterValues("franchiseButtonId");
			for (int i=0; buttonIds != null && i < buttonIds.length; i++) {
				ps.setString(1, buttonIds[i]);
				ps.setString(2, CenterPageAction.getFranchiseId(req));
				ps.setTimestamp(3, Convert.getCurrentTimestamp());
				ps.executeUpdate();
			}
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
	}
	
	/**
	 * This method updates the Franchise Description on a Franchise.
	 * @param req
	 */
	public void updateFranchiseDesc(SMTServletRequest req) throws SQLException {
		
		//This first query stores the selected description on the franchise table.
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder s = new StringBuilder();
		s.append("update ").append(customDb).append("fts_franchise set ");
		s.append("location_desc_option_id = ?, update_dt = ? where franchise_id = ? ");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setInt(1, Convert.formatInteger(req.getParameter("optionDescId")));
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, CenterPageAction.getFranchiseId(req));
			
			ps.executeUpdate();
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		
		/*
		 * The phone number (and possible other info) may be put into descriptions.
		 * Since the phone number needs to be formatted before put into the description,
		 * grab it here.
		 */
		FranchiseLocationInfoAction flia = new FranchiseLocationInfoAction(actionInit);
		flia.setAttributes(attributes);
		flia.setDBConnection(dbConn);
		FranchiseVO franchise = flia.getLocationInfo(CenterPageAction.getFranchiseId(req), false);

		/*
		 * Update the dealer Location info.  
		 */
		s = new StringBuilder();
		s.append("update DEALER_LOCATION set ");
		s.append("location_desc = replace(replace(cast(DESC_TXT as nvarchar(4000)),'");
		s.append(PHONE_NO_HANDLE).append("',?), '").append(LOCATION_HANDLE).append("', location_nm) ");
		s.append("from ").append(customDb).append("fts_franchise a ");
		s.append("inner join ").append(customDb).append("FTS_LOCATION_DESC_OPTION b ");
		s.append("on a.LOCATION_DESC_OPTION_ID = b.LOCATION_DESC_OPTION_ID ");
		s.append("inner join DEALER_LOCATION c ");
		s.append("on a.FRANCHISE_ID = c.DEALER_LOCATION_ID ");
		s.append("where a.FRANCHISE_ID = ? ");
		
		try {
			int i = 0;
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(++i, franchise.getFormattedPhoneNumber(PhoneNumberFormat.NATIONAL_FORMAT));
			ps.setInt(++i, Convert.formatInteger(CenterPageAction.getFranchiseId(req)));
			
			ps.executeUpdate();
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
	}
	
	/**
	 * Inserts/Updates the custom location description description for a franchise
	 * @param req
	 * @throws SQLException
	 */
	public void updateDescText(SMTServletRequest req) throws SQLException{
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(200);
		
		//If there is no Id yet, it's a new record. Else, it's an update.
		Integer dId = Convert.formatInteger(req.getParameter("optionDescId"), -1);
		boolean insert = (dId < 0);
		
		if (insert){
			sql.append("insert into ").append(customDb).append("FTS_LOCATION_DESC_OPTION ");
			sql.append("(COUNTRY_CODE,FRANCHISE_ID,CREATE_DT,DESC_TXT) values (?,?,?,?) ");
		}else{
			sql.append("update ").append(customDb).append("FTS_LOCATION_DESC_OPTION ");
			sql.append("set COUNTRY_CODE=?,FRANCHISE_ID=?, UPDATE_DT=?, DESC_TXT=? ");
			sql.append("where LOCATION_DESC_OPTION_ID=? ");
		}
		
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			int i=0;
			ps.setString(++i, ((SiteVO)req.getAttribute("siteData")).getCountryCode());
			ps.setInt(++i, Convert.formatInteger(CenterPageAction.getFranchiseId(req)));
			ps.setTimestamp(++i, Convert.getCurrentTimestamp());
			ps.setString(++i, req.getParameter("descText"));
			//primary key is an autonumber, so it isn't added here for inserts
			if (!insert)
				ps.setInt(++i, dId);
			
			ps.execute();
		}
	}
	
	/**
	 * Removes a center's custom description.
	 * @param req
	 * @throws SQLException
	 */
	public void deleteDescText(SMTServletRequest req) throws SQLException{
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		int descId = Convert.formatInteger(req.getParameter("optionDescId"), -1);
		int currId = Convert.formatInteger(req.getParameter("cDescOpt"), -2);
		
		//Primary key is required for the delete
		if (descId < 0){
			log.error("Missing location_desc_option_id, unable to delete.");
			return;
		}
		
		//change selected description if it's the one targeted for deletion (to satisfy FTS_FRANCHISE constraint)
		if (currId == descId){
			StringBuilder upd = new StringBuilder(230);
			upd.append("update ").append(customDb).append("FTS_FRANCHISE ");
			upd.append("set LOCATION_DESC_OPTION_ID = ff.LOCATION_DESC_OPTION_ID ");
			upd.append("from (select top 1 flo.LOCATION_DESC_OPTION_ID "); 
			upd.append("from ").append(customDb).append("FTS_LOCATION_DESC_OPTION flo ");
			upd.append("where flo.COUNTRY_CODE=? ) as ff where FRANCHISE_ID=? ");
			
			try(PreparedStatement ps = dbConn.prepareStatement(upd.toString())){
				int i=0;
				ps.setString(++i, ((SiteVO)req.getAttribute("siteData")).getCountryCode());
				ps.setInt(++i, Convert.formatInteger(CenterPageAction.getFranchiseId(req)));
				ps.execute();
			}
		}
		
		//Delete the record from the desc option table
		StringBuilder del = new StringBuilder(120);
		del.append("delete from ").append(customDb).append("FTS_LOCATION_DESC_OPTION ");
		del.append("where LOCATION_DESC_OPTION_ID=? ");
		del.append("and FRANCHISE_ID is not null "); //prevent deletion of global descriptions
		
		try(PreparedStatement ps = dbConn.prepareStatement(del.toString())){
			int i=0;
			ps.setInt(++i, descId);
			ps.execute();
		}
	}
	
	/**
	 * This method updates the Right Rail Center Image.  We set the new center
	 * image field as this action requires approval.
	 * @param req
	 */
	public void updateMainImage(SMTServletRequest req) throws InvalidDataException, SQLException {
		String centerImageUrl = req.getParameter("centerImageUrl");
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder s = new StringBuilder();
		s.append("update ").append(customDb).append("fts_franchise set ");
		s.append("new_center_image_url = ?, new_center_image_alt_txt = ?, update_dt = ? where franchise_id = ? ");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(1, centerImageUrl);
			ps.setString(2, req.getParameter("centerImageAlt"));
			ps.setTimestamp(3, Convert.getCurrentTimestamp());
			ps.setString(4, CenterPageAction.getFranchiseId(req));
			ps.executeUpdate();
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		
		// Only create a new sync record if we are are not editing the modified image
		if (!Convert.formatBoolean(req.getParameter("pendingImgChange")))
			buildSyncEntry(req, WebeditType.CenterImage);
	}
	
	/**
	 * This method updates the Social Media Links the franchise is using on the 
	 * Franchise Table.
	 * @param req
	 */
	private void updateSocialMediaLinks(SMTServletRequest req) throws SQLException, ActionException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String fId = CenterPageAction.getFranchiseId(req);
		StringBuilder s = new StringBuilder();
		s.append("update ").append(customDb).append("fts_franchise set ");
		s.append("facebook_url = ?, twitter_url=?, linkedin_url=?, foursquare_url=?, pinterest_url=?, google_plus_url=?, ");
		s.append("update_dt = ? where franchise_id = ? ");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(1, req.getParameter("facebookUrl"));
			ps.setString(2, req.getParameter("twitterUrl"));
			ps.setString(3, req.getParameter("linkedinUrl"));
			ps.setString(4, req.getParameter("foursquareUrl"));
			ps.setString(5, req.getParameter("pinterestUrl"));
			ps.setString(6, req.getParameter("googlePlusUrl"));
			ps.setTimestamp(7, Convert.getCurrentTimestamp());
			ps.setString(8, fId);
			ps.executeUpdate();
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		
		//update the social media map
		String corpOrg = ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getOrganizationId();
		SocialProfileMapAction spm = new SocialProfileMapAction(actionInit);
		spm.setAttributes(attributes);
		spm.setDBConnection(dbConn);
		spm.updateFranchiseMap(req, corpOrg+"_"+fId); 
	}	
	
	/**
	 * This method updates the Franchise table and sets the updates whiteboard
	 * text in the new whiteboard text field to await approval.
	 * @param req
	 */
	public void updateWhiteboard(SMTServletRequest req){
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder s = new StringBuilder();
		s.append("update ").append(customDb).append("FTS_FRANCHISE ");
		s.append("set NEW_WHITE_BOARD_TEXT = ?, UPDATE_DT = ? where FRANCHISE_ID = ? ");
		
		PreparedStatement ps = null;
		
		try{
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(1, req.getParameter("whiteboardText"));
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, CenterPageAction.getFranchiseId(req));
			ps.executeUpdate();
		} catch(SQLException sqle){
			log.debug(sqle);
		} finally {
			try {ps.close();} 
			catch (Exception e) {}
		}
		
		// Only create a new sync record if we are are not editing the modified whiteboard
		if (!Convert.formatBoolean(req.getParameter("pendingWBChange")))
			buildSyncEntry(req, WebeditType.Whiteboard);
	}
	
	/**
	 * Build a sync entry to track the action that is being edited
	 * @param req
	 * @param approvalType
	 */
	private void buildSyncEntry(SMTServletRequest req, WebeditApprover.WebeditType approvalType) {
		ApprovalController controller = new ApprovalController(dbConn, getAttributes());
		WebeditApprover app = new WebeditApprover(dbConn, getAttributes());
		String franchiseId = CenterPageAction.getFranchiseId(req);
		ApprovalVO approval = new ApprovalVO();
		
		approval.setWcKeyId(franchiseId+"_"+approvalType);
		approval.setItemDesc(approvalType.toString());
		approval.setItemName("Center " + franchiseId + " " + approvalType.getLabel());
		approval.setModuleType(ModuleType.Webedit);
		approval.setSyncStatus(SyncStatus.PendingUpdate);
		approval.setSyncTransaction(SyncTransaction.Create);
		approval.setOrganizationId(((SiteVO)req.getAttribute("siteData")).getOrganizationId()+"_"+franchiseId);
		approval.setUserDataVo((UserDataVO) req.getSession().getAttribute(Constants.USER_DATA));
		approval.setCreateDt(Convert.getCurrentTimestamp());
		
		try {
			controller.process(approval);
			app.submit(approval);
		} catch (ApprovalException e) {
			e.printStackTrace();
		}
		
		
	}
	
	/**
	 * This is a helper method to build the appropriate ApprovalVO container 
	 * with the correct pieces.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	public ApprovalVO buildVO(SMTServletRequest req) throws ActionException{
		Integer bType = Convert.formatInteger(req.getParameter("bType"));
		ApprovalVO avo = new ApprovalVO();
		switch(bType) {
			case CenterPageAction.FRANCHISE_MAIN_IMAGE_UPDATE:
			case CenterPageAction.FRANCHISE_MAIN_IMAGE_APPROVE:
				break;
			case CenterPageAction.WHITEBOARD_UPDATE:
				break;
		}
		return avo;
	}
	
	/**
	 * Returns a list of ButtonVO's for the Left side Buttons under menu.
	 * @param id
	 * @return
	 */
	public List<ButtonVO> getButtonInfo(String id) {
		log.debug("Getting buttons for store number: " + id);
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder s = new StringBuilder();
		s.append("select * from ").append(customDb).append("FTS_FRANCHISE_BUTTON_XR a ");
		s.append("inner join ").append(customDb).append("FTS_CENTER_BUTTON b ");
		s.append("on a.CENTER_BUTTON_ID = b.CENTER_BUTTON_ID ");
		s.append("where FRANCHISE_ID = ? order by order_no");
		log.debug(s + "|" + id);
		
		PreparedStatement ps = null;
		List<ButtonVO> buttons = new ArrayList<ButtonVO>();
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(1, id);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				buttons.add(new ButtonVO(rs));
			}
			
		} catch (Exception e) {
			log.error("Unable to get button info", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		return buttons;
	}
}
