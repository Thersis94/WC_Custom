package com.fastsigns.action.approval;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.fastsigns.action.approval.vo.AbstractChangeLogVO;
import com.fastsigns.action.approval.vo.PageModuleLogVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.admin.action.PageModuleRoleAction;
import com.smt.sitebuilder.common.SiteVO;
/****************************************************************************
 * <b>Title</b>: PageModuleApprovalAction.java
 * <p/>
 * <b>Project</b>: SB_FastSigns
 * <p/>
 * <b>Description: </b> This class performs all page module specific approval 
 * actions regarding submission, approval and denial requests.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Nov 27, 2012
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class PageModuleApprovalAction extends ApprovalTemplateAction {

	public PageModuleApprovalAction() {
	}

	public PageModuleApprovalAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/**
	 * On Approve, remove all page modules that are not the one being approved.
	 * Send call out to PageModuleRoleAction to add new permissions to approved
	 * page module.
	 */
	@Override
	public void approveRequest(List<AbstractChangeLogVO> vos, SMTServletRequest req)
			throws ActionException {
		log.debug("Beginning Page Module Approval Process...");
		PageModuleRoleAction pmra = new PageModuleRoleAction(this.actionInit);
		pmra.setDBConnection(dbConn);
		pmra.setAttributes(this.attributes);
		
		//Get Role Id's for new Content
		req.setParameter("roleId", getRoleIds(req), true);
		PreparedStatement ps = null;
		
		try {
			for (AbstractChangeLogVO vo : vos) {
				PageModuleLogVO pmvo = (PageModuleLogVO) vo;
				
				//Delete old SB_Actions related to previous content.
				ps = dbConn.prepareStatement(getApproveSBActionDelete());
				ps.setString(1, pmvo.getPageId());
				ps.setString(2, pmvo.getComponentId());
				ps.executeUpdate();
				ps.close();
				
				//Delete old Page Modules
				ps = dbConn.prepareStatement(getApproveDelete());
				ps.setString(1, pmvo.getPageId());
				ps.setString(2, pmvo.getComponentId());
				ps.executeUpdate();
				
				//Place PageModuleId on request for role update and send to PageModuleRoleAction
				req.setAttribute("pageModuleId", vo.getComponentId());
				pmra.update(req);
				log.debug("Page Module Update");
				updateStatus(vos, AbstractChangeLogVO.Status.APPROVED.ordinal());
				logger.logChange(req, vos);
			}
		} catch (Exception e) { log.debug("Error Approving Page Module Occured", e);} 
		finally {try {ps.close();} catch (Exception e) {}}
		
	}

	/**
	 * Remove the denied Page Modules Content entry, SBAction entry and PageModule
	 * entry.
	 */
	@Override
	public void denyRequest(List<AbstractChangeLogVO> vos, SMTServletRequest req)
			throws ActionException {
		log.debug("Beginning Page Module Approval Process...");
		PreparedStatement ps = null;
		for(AbstractChangeLogVO vo : vos){
			PageModuleLogVO pmvo = (PageModuleLogVO) vo;
			try {
				//Deleting SBAction
				ps = dbConn.prepareStatement(getDenySBActionDelete());
				ps.setString(1, pmvo.getComponentId());
				ps.executeUpdate();
				ps.close();
				
				//Deleting Page Module
				ps = dbConn.prepareStatement(getDenyPageModDelete());
				ps.setString(1, pmvo.getComponentId());
				ps.executeUpdate();
				updateStatus(vos, AbstractChangeLogVO.Status.DENIED.ordinal());
				logger.logChange(req, vos);
			} catch (SQLException e) {
				log.error(e);
			} finally {
				try {
					ps.close();
				} catch (Exception e) {}
			}
		}
		
	}

	/**
	 * Only need to insert a record of the request in the Log Table.
	 */
	@Override
	public void submitRequest(List<AbstractChangeLogVO> vos, SMTServletRequest req)
			throws ActionException {
		logger.logChange(req, vos);
	}
	
	/**
	 * Build the Role Ids for the PageModuleRoleAction Insert
	 * @param req
	 * @return
	 */
	public String [] getRoleIds (SMTServletRequest req){
		String [] roles = new String [4];
		
		roles[0] = "0";
		roles[1] = "10";
		roles[2] = "100";
		String orgId = ((SiteVO)req.getAttribute("siteData")).getOrganizationId();
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement("select ROLE_ID from ROLE where ORGANIZATION_ID = ?");
			ps.setString(1, orgId);
			ResultSet rs = ps.executeQuery();
			if(rs.next())
				roles[3] = rs.getString("role_id");
		} catch(Exception e){
			
		} finally {
			try { ps.close();} catch(Exception e){}
			}
		return roles;
		
	}

	@Override
	public String getHFriendlyType() {
		return PageModuleLogVO.FRIENDLY_NAME;
	}

	@Override
	public String getDbTypeId() {
		return PageModuleLogVO.TYPE_ID;
	}
	
	/**
	 * Builds the delete SQL String for removing the old Page Module when approving Page Modules
	 * @return
	 */
	public String getApproveDelete(){
		StringBuilder s = new StringBuilder();
		s.append("delete from PAGE_MODULE where PAGE_ID = ? ");
		s.append("and PAGE_MODULE_ID != ? ");
		return s.toString();
	}
	
	/**
	 * Builds the delete SQL String for Deleting SBAction when denying Page Modules
	 * @return
	 */
	public String getApproveSBActionDelete(){
		StringBuilder s = new StringBuilder();
		s.append("delete from SB_ACTION where ACTION_ID in(");
		s.append("select ACTION_ID from PAGE_MODULE where PAGE_ID = ? ");
		s.append("and PAGE_MODULE_ID != ?)");
		return s.toString();
	}
	
	/**
	 * Builds the delete SQL String for Deleting SBAction when denying Page Modules
	 * @return
	 */
	public String getDenySBActionDelete(){
		StringBuilder s = new StringBuilder();
		s.append("delete from SB_ACTION where ACTION_ID in(");
		s.append("select ACTION_ID from PAGE_MODULE where PAGE_MODULE_ID = ?)");
		return s.toString();
	}
	
	/**
	 * Builds the delete SQL String for Deleting Page Modules when denying Page Modules
	 * @return
	 */
	public String getDenyPageModDelete(){
		StringBuilder s = new StringBuilder();
		s.append("delete from PAGE_MODULE where PAGE_MODULE_ID = ? ");
		return s.toString();
	}

}
