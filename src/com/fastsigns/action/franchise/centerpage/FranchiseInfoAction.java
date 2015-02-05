package com.fastsigns.action.franchise.centerpage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.fastsigns.action.LogAction;
import com.fastsigns.action.approval.ApprovalFacadeAction;
import com.fastsigns.action.approval.vo.AbstractChangeLogVO;
import com.fastsigns.action.approval.vo.ApprovalVO;
import com.fastsigns.action.approval.vo.CenterImageLogVO;
import com.fastsigns.action.approval.vo.WhiteBoardLogVO;
import com.fastsigns.action.franchise.CenterPageAction;
import com.fastsigns.action.franchise.vo.ButtonVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBActionAdapter;
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
					sendToApprovalAction(req);
					break;
				case CenterPageAction.FRANCHISE_BUTTON_UPDATE:
					updateFranchiseButton(req);
					super.clearCacheByGroup(siteId);
					break;
				case CenterPageAction.FRANCHISE_DESC_UPDATE:
					updateFranchiseDesc(req);
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
		 * Update the dealer Location info.  Here we perform some replacements
		 * on the description template to make it reflect the center data.
		 */
		s = new StringBuilder();
		s.append("update DEALER_LOCATION set ");
		s.append("location_desc = replace(cast(DESC_TXT as nvarchar(4000)), '[location]', location_nm) ");
		s.append("from ").append(customDb).append("fts_franchise a ");
		s.append("inner join ").append(customDb).append("FTS_LOCATION_DESC_OPTION b ");
		s.append("on a.LOCATION_DESC_OPTION_ID = b.LOCATION_DESC_OPTION_ID ");
		s.append("inner join DEALER_LOCATION c ");
		s.append("on a.FRANCHISE_ID = c.DEALER_LOCATION_ID ");
		s.append("where FRANCHISE_ID = ? ");
		
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setInt(1, Convert.formatInteger(CenterPageAction.getFranchiseId(req)));
			
			ps.executeUpdate();
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
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
		
		//Log the update for Approval Action.
		req.setParameter("apprFranchiseId", CenterPageAction.getFranchiseId(req));
		req.setParameter("subStatus", AbstractChangeLogVO.Status.PENDING.toString());
		LogAction lA = new LogAction(this.actionInit);
		lA.setDBConnection(dbConn);
		lA.setAttributes(attributes);
		lA.logChange(req, new CenterImageLogVO(req));
		lA = null;
	}
	
	/**
	 * This method updates the Social Media Links the franchise is using on the 
	 * Franchise Table.
	 * @param req
	 */
	private void updateSocialMediaLinks(SMTServletRequest req) throws SQLException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
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
			ps.setString(8, CenterPageAction.getFranchiseId(req));
			ps.executeUpdate();
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
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
		
		//Log the update for Approval Action
		req.setParameter("apprFranchiseId", CenterPageAction.getFranchiseId(req));
		LogAction lA = new LogAction(this.actionInit);
		lA.setDBConnection(dbConn);
		lA.setAttributes(attributes);
		lA.logChange(req, new WhiteBoardLogVO(req));		
		lA = null;
	}
	
	public void forwardToLog(SMTServletRequest req){
		LogAction lA = new LogAction(this.actionInit);
		lA.setDBConnection(dbConn);
		lA.setAttributes(attributes);
		lA.logChange(req, new WhiteBoardLogVO(req));		
		lA = null;
	}
	
	/**
	 * Handles Submitting Changes for Approval by Forwarding to the Approval 
	 * Facade Action
	 * @param req
	 * @throws ActionException
	 */
	public void sendToApprovalAction(SMTServletRequest req) throws ActionException{
		req.setParameter("approvalType", "3");
		req.setAttribute("approvalVO", buildVO(req));
		ApprovalFacadeAction aA = new ApprovalFacadeAction(this.actionInit);
		aA.setDBConnection(dbConn);
		aA.setAttributes(attributes);
		aA.update(req);
		aA = null;
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
				avo.setChangeLogList(CenterImageLogVO.TYPE_ID, new CenterImageLogVO(req));
				break;
			case CenterPageAction.WHITEBOARD_UPDATE:
				avo.setChangeLogList(WhiteBoardLogVO.TYPE_ID, new WhiteBoardLogVO(req));
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
