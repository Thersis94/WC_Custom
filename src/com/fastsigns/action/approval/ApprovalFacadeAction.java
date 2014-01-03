package com.fastsigns.action.approval;

import java.sql.PreparedStatement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fastsigns.action.LogAction;
import com.fastsigns.action.approval.vo.AbstractChangeLogVO;
import com.fastsigns.action.approval.vo.ApprovalVO;
import com.fastsigns.action.approval.vo.ChangeLogVO;
import com.fastsigns.action.franchise.CenterPageAction;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;

import com.siliconmtn.security.UserRoleVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.FacadeActionAdapter;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: ApprovalAction.java
 * <p/>
 * <b>Project</b>: SB_FastSigns
 * <p/>
 * <b>Description: </b> This class works as the Primary Interface to all 
 * Approval Requests.  Requests enter here, are verified and forwarded to the 
 * proper action.  
 * 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Sept 20, 2012
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class ApprovalFacadeAction extends FacadeActionAdapter {

	public static final int APPROVE = 1;
	public static final int DENY = 2;
	public static final int SUBMIT = 3;
	public static final int DELETE_ALL_MODULES = 30;

	public ApprovalFacadeAction() {
	}

	public ApprovalFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * This method handles filtering the incoming changelogs and forwarding them
	 * to the proper Action.
	 */
	public void update(SMTServletRequest req) throws ActionException {
		log.debug("Beginning approval Process");
		ApprovalVO avo = (ApprovalVO) req.getAttribute("approvalVO");
		Set<String> keys = avo.getVals().keySet();
		/*
		 * This loop grabs each Request type from the ApprovalVO, extracts the 
		 * list of Requests, verify's the lists and forwards them to each approval
		 * action.
		 */
		for(String key : keys){
			//Extract Requests
			List<AbstractChangeLogVO> vos = avo.getChangeLogList(key);
			if(vos.size() > 0){
				//Verify VOs
				verifyVos(vos, req);
				Integer approvalType = vos.get(0).getApprovalType();
				log.debug("Approval type: " + approvalType);
				if (approvalType > 0) {
					Object msg = "msg.updateSuccess";
					//Retrieve the ApprovalAction for each Type.
					ApprovalAction aa = (ApprovalAction) getActionInstance(vos.get(0).getActionClassPath());
					aa.setAttributes(attributes);
					aa.setDBConnection(dbConn);
					//send Approvals to the proper action type.
					switch (approvalType) {
					case APPROVE:
						aa.approveRequest(vos, req);
						break;
					case DENY:
						aa.denyRequest(vos, req);
						break;
					case SUBMIT:
						aa.submitRequest(vos, req);
						break;
					case DELETE_ALL_MODULES:
						this.removePendingModules(req);
					}
					String siteId = getSiteId(req);
//					// turn off string encoding since this is a secure call
					req.setValidateInput(Boolean.FALSE);
					//Send Emails if necessary
					for(AbstractChangeLogVO vo: vos)
						if (vo.getStatusNo() != AbstractChangeLogVO.Status.PENDING.ordinal())
							aa.sendEmail(vo);
		
					PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
					String redir = page.getFullPath() + "?";
					req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
					req.setAttribute(Constants.REDIRECT_URL, redir + "msg=" + msg);
					log.debug("clearing cache group for site: " + siteId);
					super.clearCacheByGroup(siteId);
				}
			}
		}
	}
	
	/**
	 * Here we perform any verification on the vos that needs done. 
	 * @param vos
	 * @param req
	 */
	private void verifyVos(List<AbstractChangeLogVO> vos, SMTServletRequest req) {
		UserRoleVO role = (UserRoleVO) req.getSession().getAttribute(Constants.ROLE_DATA);
		for(AbstractChangeLogVO v : vos){

			/*
			 * Here we check that we have a reviewerId for the emails on all 
			 * approval and denial requests.  Requests for approval don't require
			 * this as they haven't been reviewed yet.
			 */
			
			if(StringUtil.checkVal(v.getReviewerId()).length() <= 0 && v.getApprovalType() != SUBMIT){
				v.setReviewerId(role.getProfileId());
			}
		}
		
	}

	/**
	 * When a Franchise user wishes to take back pending submissions for
	 * approval this removes all pending modules for a given Franchise and
	 * removes changelogs that deal with them.
	 * 
	 * @param req
	 * @throws SQLException
	 */
	private void removePendingModules(SMTServletRequest req) {
		log.debug("Beginning Module Cleanup Process...");
		List<String> ids = new ArrayList<String>();
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String franchiseId = StringUtil.checkVal(CenterPageAction.getFranchiseId(req));
		StringBuilder sb = new StringBuilder();
		//Retrieve all modules with pending requests. 
		sb.append("select PARENT_ID from ").append(customDb);
		sb.append("fts_cp_module_option where FRANCHISE_ID = ? ");
		sb.append("and PARENT_ID is not null and PARENT_ID <> 0 ");
		sb.append("and APPROVAL_FLG = 100");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, franchiseId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				ids.add(rs.getString("PARENT_ID"));
			}
		} catch (SQLException e) {
			log.error(e);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {
			}
		}
		//reset modules to be pre-approval request. 
		if (ids.size() > 0) {
			sb = new StringBuilder();
			sb.append("update ").append(customDb);
			sb.append("fts_cp_module_option set APPROVAL_FLG = 0 where APPROVAL_FLG = 100 ");
			sb.append("and PARENT_ID = ?");
			try {
				ps = dbConn.prepareStatement(sb.toString());
				for (String s : ids) {
					ps.setString(1, s);
					ps.addBatch();
				}
				ps.executeBatch();
				log.debug("purged " + ids.size() + " pending modules");
				LogAction la = new LogAction(actionInit);
				la.setDBConnection(dbConn);
				la.setAttributes(attributes);
				la.deleteFromChangelog(ids);
			} catch (SQLException e) {
				log.error(e);
			} finally {
				try {
					ps.close();
				} catch (Exception e) {
				}
			}
		}
	}

	/**
	 * Retrieve the Site Id for use with cache clearing.
	 * @param req
	 * @return
	 */
	private String getSiteId(SMTServletRequest req) {
		String orgId = ((SiteVO) req.getAttribute("siteData"))
				.getOrganizationId();
		String siteId = orgId + "_" + CenterPageAction.getFranchiseId(req)
				+ "_1";
		if (req.getParameter("apprFranchiseId") != null)
			siteId = orgId + "_" + req.getParameter("apprFranchiseId") + "_1";
		return siteId;
	}
	
	/**
	 * Retrieve the changelogs matching the keys 
	 * @param cids
	 * @param filter
	 * @return
	 */
	public Map<String, AbstractChangeLogVO> getChangeLogStatus(List<String> cids, Integer filter){
		Map<String, AbstractChangeLogVO> vos = new HashMap<String, AbstractChangeLogVO>();
		PreparedStatement ps = null;
		StringBuilder sb = new StringBuilder();
		sb.append("select * from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sb.append("FTS_CHANGELOG where ");
		if(filter != null)
			sb.append("STATUS_NO = ? and ");
		sb.append("COMPONENT_ID in(");
		for(int i = 0; i < cids.size(); i++){
			sb.append("?");
			if(i != cids.size() - 1)
				sb.append(",");
		}
		sb.append(") order by SUBMITTED_DT");
		int i = 1;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			if(filter != null)
				ps.setInt(i++, filter.intValue());
			for(String s : cids)
				ps.setString(i++, s);
			ResultSet rs = ps.executeQuery();
			while(rs.next())
				vos.put(rs.getString("COMPONENT_ID"), new ChangeLogVO(rs));
		} catch(Exception e){
			
		} finally {try{ps.close();} catch(Exception e){}}
		return vos;
	}

}