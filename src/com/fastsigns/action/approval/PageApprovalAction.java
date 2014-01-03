package com.fastsigns.action.approval;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import com.fastsigns.action.approval.vo.AbstractChangeLogVO;
import com.fastsigns.action.approval.vo.PageLogVO;
import com.fastsigns.action.franchise.FranchisePageAction;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.content.ContentVO;
import com.smt.sitebuilder.admin.action.PageModuleRoleAction;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
/****************************************************************************
 * <b>Title</b>: PageApprovalAction.java
 * <p/>
 * <b>Project</b>: SB_FastSigns
 * <p/>
 * <b>Description: </b> This class performs all page specific approval actions
 * regarding submission, approval and denial requests.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Oct 9, 2012
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class PageApprovalAction extends ApprovalTemplateAction {
	/** 
	 * Called from ApprovalAction to approve a page and the 
	 * child CONTENT portlet(s) 
	 * @throws ActionException 
	 */
	@Override
	public void approveRequest(List<AbstractChangeLogVO> vos, SMTServletRequest req) throws ActionException {
		log.debug("Beginning Approve Page Process...");
		String groupId = "FAST_SIGNS";
		String countryCode = ((SiteVO)req.getAttribute("siteData")).getCountryCode();
		if(!countryCode.equals("US"))
			groupId +="_" + countryCode;
		for(AbstractChangeLogVO v: vos){
			//change page's startDate.  This only happens once, when the page is first created.
			//updated to the page only require refreshing the page_module table.
			if (Convert.formatBoolean(req.getParameter("updatePage"))) {
				String sql = "update page set live_start_dt=? where page_id=?";
				PreparedStatement ps = null;
				try {
					ps = dbConn.prepareStatement(sql);
					ps.setTimestamp(1, Convert.getCurrentTimestamp());
					ps.setString(2, v.getComponentId());
					ps.executeUpdate();
				} catch (SQLException sqle) {
					log.error(sqle);
					throw new ActionException(sqle);
				} finally {
					try { ps.close(); } catch (Exception e) {}
				}
			}
			//add roles to the page_module
			PageVO page = new PageVO();
			page.setPageId(v.getComponentId());
			FranchisePageAction fPA = new FranchisePageAction(this.actionInit);
			fPA.setDBConnection(dbConn);
			fPA.setAttributes(attributes);
			ContentVO c = fPA.getContent(page, null, groupId);
			
			//flush everything from the page except this one module
			String sql = "delete from page_module where page_id=? and action_id != ?";
			PreparedStatement ps = null;
			try {
				ps = dbConn.prepareStatement(sql);
				ps.setString(1, v.getComponentId());
				ps.setString(2, c.getActionId());
				ps.executeUpdate();
			} catch (SQLException sqle) {
				log.error(sqle);
				throw new ActionException(sqle);
			} finally {
				try { ps.close(); } catch (Exception e) {}
			}
			
			//create roles for all permission levels to our page_module
			SMTActionInterface aac = new PageModuleRoleAction(this.actionInit);
	        aac.setDBConnection(dbConn);
			req.setAttribute("pageModuleId", c.getAttribute("pmid"));
	        aac.setAttributes(attributes);
	        aac.update(req);
	        
	        String siteId = req.getParameter("siteId");
	        if (req.getParameter("apprFranchiseId") != null){ 
	        	siteId = "FTS_";
	        	if(!countryCode.equals("US"))
	        		siteId = siteId + countryCode + "_";
	        	siteId += req.getParameter("apprFranchiseId") + "_1";
	        }
	        log.debug("clearing cache for siteId=" + siteId);
	        super.clearCacheByGroup(siteId);
			v.setStatusNo(AbstractChangeLogVO.Status.APPROVED.ordinal());
		}
		logger.logChange(req, vos);

	}
	/**
	 * Changes the live_start_date of the page from 2200 to back to the Not 
	 * submitted status of 2100 and updates the changelog.
	 * @param req
	 */
	@Override
	public void denyRequest(List<AbstractChangeLogVO> vos, SMTServletRequest req) {
		log.debug("Beginning Page Cleanup Process...");
		String sql = "update page set live_start_dt=?, update_dt=? where page_id=?";
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sql);	
			for(AbstractChangeLogVO vo : vos){
				ps.setTimestamp(1, Convert.formatTimestamp(Convert.DATE_SLASH_PATTERN, "01/01/2100"));
				ps.setTimestamp(2, Convert.getCurrentTimestamp());
				ps.setString(3, vo.getComponentId());
				ps.addBatch();
			}
			ps.executeBatch();
			} catch (SQLException e) {
				log.debug(e);
			} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		//Update status'.
		updateStatus(vos, AbstractChangeLogVO.Status.DENIED.ordinal());
		logger.logChange(req, vos);
	}
	/**
	 * When a Page is submitted, this method sets the live_start_dt from 2100 to 
	 * 2200.
	 * @param req
	 * @throws ActionException
	 */
	@Override
	public void submitRequest(List<AbstractChangeLogVO> vos, SMTServletRequest req) {
		log.debug("Beginning Submit Page Process...");
		String startDate = req.getParameter("startDate");
		String sql = "update page set live_start_dt=?, update_dt=? where page_id=?";
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql);
			for (AbstractChangeLogVO vo : vos) {
				ps.setTimestamp(1, Convert.formatTimestamp(Convert.DATE_SLASH_PATTERN, startDate));
				ps.setTimestamp(2, Convert.getCurrentTimestamp());
				ps.setString(3, vo.getComponentId());
				ps.addBatch();
			}
				ps.executeBatch();
			log.debug("submitted " + vos.size() + " pages for approval");
		} catch (SQLException e) {
			log.debug(e);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		logger.logChange(req, vos);
	}

	@Override
	public String getHFriendlyType() {
		return PageLogVO.FRIENDLY_NAME;
	}

	@Override
	public String getDbTypeId() {
		return PageLogVO.TYPE_ID;
	}

}
