package com.fastsigns.action.franchise;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.fastsigns.action.approval.ApprovalFacadeAction;
import com.fastsigns.action.approval.vo.AbstractChangeLogVO;
import com.fastsigns.action.approval.vo.ApprovalVO;
import com.fastsigns.action.approval.vo.CareerLogVO;
import com.fastsigns.action.vo.CareersVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserRoleVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: KeyStoneCareersAction.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> This Class handles maintenance of the Job Postings in 
 * the webedit admintool.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Billy Larsen
 * @version 1.0
 * @since Oct 9, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class KeyStoneCareersAction extends SBActionAdapter {
	
	/**
	 * Constants used to determine workflow.  Sent back on request.
	 */
	public static final int ADD_JOB = 1;
	public static final int EDIT_JOB = 3;
	public static final int DELETE_JOB = 5;
	public static final int SUBMIT_JOB = 7;
	public static final int COPY_JOB = 9;
	
	public KeyStoneCareersAction(){
		super();
	}
	
	public KeyStoneCareersAction(ActionInitVO actionInit){
		super(actionInit);
	}
	
	/**
	 * Method responsible for deleting career opportunities from the database.
	 */
	@Override
	public void delete(SMTServletRequest req) throws ActionException {
		log.debug("Begninng delete method");

		String msg = "msg.deleteSuccess";
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		StringBuilder redir = new StringBuilder(page.getFullPath());
		redir.append("?");
		CareersVO c = new CareersVO(req);
		if(c.getJobPostingId() != null){
			StringBuilder sb = new StringBuilder();
			sb.append("delete from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
			sb.append("FTS_JOB_POSTING where JOB_POSTING_ID = ?");
			PreparedStatement ps = null;
			log.debug(sb.toString() + " | " + c.getJobPostingId());
			try{
				ps = dbConn.prepareStatement(sb.toString());
				ps.setString(1, c.getJobPostingId());
				ps.executeUpdate();
			} catch(SQLException sqle){
				msg = "msg.cannotDelete";
				log.error("An error occured during Career Delete", sqle);
			} finally{
				try{
					ps.close();
				} catch(Exception e){
					msg = "msg.cannotDelete";

				}
			}
		} else {
			msg = "msg.cannotDelete";
		}
		//finish the redirect
		redir.append("&msg=").append(msg);
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, redir.toString());
	}
	
	/**
	 * Method responsible for copying career opportunities in the database.
	 */
	@Override
	public void copy(SMTServletRequest req) throws ActionException {
		log.debug("Begninng copy method");
		String msg = "msg.copySuccess";
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		StringBuilder redir = new StringBuilder(page.getFullPath());
		redir.append("?");
		String newJobId = new UUIDGenerator().getUUID();
		CareersVO c = new CareersVO(req);
		if(c.getJobPostingId() != null){
			PreparedStatement ps = null;
			try{
				String franId = req.getParameter("jobFranId");
				log.debug(getCopySql(franId) + " | " + c.getJobPostingId());
				ps = dbConn.prepareStatement(getCopySql(franId));
				ps.setTimestamp(1, Convert.getCurrentTimestamp());
				ps.setString(2, newJobId);
				ps.setString(3, c.getJobPostingId());
				ps.executeUpdate();
				redir.append("jobPostingId=").append(newJobId);
			} catch(SQLException sqle){
				log.error("An error occured during Career Delete", sqle);
			} finally{
				try{
					ps.close();
				} catch(Exception e){
					msg = "msg.cannotCopy";

				}
			}
		} else {
			msg = "msg.cannotCopy";
		}
		//finish the redirect
		redir.append("&msg=").append(msg);
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, redir.toString());
	}
	
	/**
	 * Method responsible for delegating actions from webedit related to career
	 * opportunities.
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		int bType = Convert.formatInteger(req.getParameter("bType"));
		String orgId = ((SiteVO)req.getAttribute("siteData")).getOrganizationId();

		//forward the request depending on the action being performed.
		switch(bType){
			case ADD_JOB:
			case EDIT_JOB:
				this.update(req);
				break;
			case DELETE_JOB:
				this.delete(req);
				break;
			case SUBMIT_JOB:
				this.submitJobs(req);
				break;
			case COPY_JOB:
				this.copy(req);
				break;
			default: 
				throw new ActionException("Invalid Operation Attempted.");
		}
		super.clearCacheByGroup(orgId + "_CAREERS");

	}
	
	/**
	 * Method responsible for retrieving careers for webedit
	 */ 
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("Beginning retrieve method");
		super.retrieve(req);
		
		//If we are adding a new one, return.  We don't need to retrieve any data here. 
		if(req.hasParameter("add"))
			return;
			
		String orgId = ((SiteVO)req.getAttribute("siteData")).getOrganizationId();
		CareersVO cvo = new CareersVO(req);
		List<CareersVO> postings = new ArrayList<CareersVO>();
		int franchiseId = Convert.formatInteger((String)req.getSession().getAttribute("webeditFranId"));
		
		//Check if we have an apprFranchiseId, if we do then we are approving and we set FranchiseId to that
		if(req.hasParameter("apprFranchiseId"))
			franchiseId = Convert.formatInteger(req.getParameter("apprFranchiseId"));
		
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
    	UserRoleVO r = (UserRoleVO) req.getSession().getAttribute(Constants.ROLE_DATA);
    	
    	//Retrieve all career Opportunities.
		StringBuilder sb = new StringBuilder();
		sb.append("select * from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("FTS_JOB_POSTING where ORGANIZATION_ID = ? ");
		
		//Sub select based on role if we're not an admin
		if(r.getRoleLevel() < 100 || req.hasParameter("apprFranchiseId"))
			sb.append("and FRANCHISE_ID = ? ");
		
		//if we're editing a specific job, add id here.
		if(cvo.getJobPostingId() != null){
			sb.append("and JOB_POSTING_ID = ? ");
		}
		sb.append("order by FRANCHISE_ID, JOB_POST_DT, JOB_TITLE_NM");
		log.debug(sb + " | " + orgId);
		PreparedStatement ps = null;
		
		try{
			int ctr = 1;
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(ctr++, orgId);
			if(r.getRoleLevel() < 100 || req.hasParameter("apprFranchiseId"))
				ps.setString(ctr++, franchiseId + "");
			if(cvo.getJobPostingId() != null){
				ps.setString(ctr++, cvo.getJobPostingId());
			}
			
			ResultSet rs = ps.executeQuery();
			while(rs.next()){
				postings.add(new CareersVO(rs));
			}
			log.debug("Retrieved " + postings.size() + " Jobs");
		} catch(SQLException sqle){
			log.error("An error was thrown while retrieving ", sqle);
		} finally {
			try {
				ps.close();
			} catch (SQLException e) {
				log.error(e);
			}
		}
		mod.setActionData(postings);
		
	}
	
	@Override
	public void update(SMTServletRequest req) throws ActionException {
		log.debug("Beginning update method");
		
		//Turn off validation so we don't escape html
		req.setValidateInput(Boolean.FALSE);

		if (AdminConstants.REQ_UPDATE.equalsIgnoreCase(req.getParameter(AdminConstants.REQUEST_TYPE))) {
			super.update(req);
			return;
		} else if(isRenewable(req)){
			renewPosting(req);
			return;
		}
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		StringBuilder redir = new StringBuilder(page.getFullPath());
		redir.append("?");
		Object msg = "msg.updateSuccess";
		CareersVO cvo = new CareersVO(req);
		String sql = null;
    	UserRoleVO r = (UserRoleVO) req.getSession().getAttribute(Constants.ROLE_DATA);

    	//Determine the script we need to run.
    	if(cvo.getJobPostingId().length() > 0){
    		sql = getUpdateSql();
    	} else{
			sql = getInsertSql();
            cvo.setJobPostingId(new UUIDGenerator().getUUID());	
		}
		try{
			int i = 1;
			log.debug(sql);
			PreparedStatement ps = dbConn.prepareStatement(sql);
			ps.setString(i++, cvo.getFranchiseId());
			ps.setString(i++, cvo.getOrganizationId());
			ps.setString(i++, cvo.getJobTitleNm());
			ps.setString(i++, cvo.getJobLocNm());
			ps.setInt(i++, cvo.getJobHours());
			ps.setString(i++, cvo.getJobDesc().toString());
			ps.setString(i++, cvo.getJobResp().toString());
			ps.setString(i++, cvo.getJobExpReq().toString());
			ps.setString(i++, cvo.getJobDsrdSkills().toString());
			ps.setString(i++, cvo.getJobAdtlComments().toString());
			ps.setString(i++, cvo.getJobContactEmail());
			ps.setTimestamp(i++, Convert.getTimestamp(cvo.getJobPostDt(), true));
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			//If this is an corporate job and admin entered
			if(r.getRoleLevel() == 100 && cvo.getFranchiseId() == null)
				ps.setInt(i++, AbstractChangeLogVO.Status.APPROVED.ordinal());
			else
				ps.setInt(i++, -1);
			ps.setString(i++, cvo.getJobAddressTxt());
			ps.setString(i++, cvo.getJobAddress2Txt());
			ps.setString(i++, cvo.getJobCityNm());
			ps.setString(i++, cvo.getJobStateCd());
			ps.setString(i++, cvo.getJobZipCd());
			ps.setString(i++, cvo.getJobCountryCd());
			ps.setString(i++, cvo.getJobPrimaryPhoneNo());
			ps.setInt(i++, 1);
			ps.setString(i++, cvo.getJobPostingId());
			ps.execute();
			
		} catch(SQLException sqle){
			log.error("Error during build.", sqle);
			msg = "msg.cannotUpdate";
		}
		//finish the redirect
		redir.append("msg=").append(msg);
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, redir.toString());
	}
	
	/**
	 * This method loops over available out of date job postings and renews
	 * them.
	 */
	private void renewPosting(SMTServletRequest req) {
		String [] renewIds = null;
		//split up the job posts array on the request.
		renewIds = StringUtil.checkVal(req.getParameter("jobPostingId")).split(",");
		
		String msg = "msg.renewSuccess";
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		StringBuilder redir = new StringBuilder(page.getFullPath());
		redir.append("?");
		StringBuilder sb = new StringBuilder();
		sb.append("update ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("FTS_JOB_POSTING set JOB_POST_DT = ?, UPDATE_DT = ? ");
		sb.append("where JOB_POSTING_ID in( ");
		
		//Loop the ids into the sql script.
		for(int i = 0; i < renewIds.length; i++){
			if(i > 0)
				sb.append(", ");
			sb.append("?");
		}
		sb.append(")");
		log.debug("Renew sql : " + sb.toString());
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sb.toString());
			ps.setTimestamp(1, Convert.getCurrentTimestamp());
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			for(int i = 0; i < renewIds.length; i++){
				ps.setString(i + 3, renewIds[i].trim());
			}
			ps.executeUpdate();
		} catch(SQLException sqle){
			log.error("An error occurred while renewing job posts");
			msg = "msg.cannotRenew";
		} finally{
			try{
				ps.close();
			} catch(Exception e){
				msg = "msg.cannotRenew";
			}
		}
		//finish the redirect
		redir.append("&msg=").append(msg);
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, redir.toString());
	}
	
	/**
	 * Helper method that determines if a job posting can be renewed.  We only
	 * renew those that are approved and have the renew flag sent back.
	 */
	private boolean isRenewable(SMTServletRequest req){
		boolean renew = Convert.formatBoolean(req.getParameter("renew"));
		boolean isApproved = Convert.formatInteger(req.getParameter("approvalFlg")) == AbstractChangeLogVO.Status.APPROVED.ordinal();
		return (renew && isApproved);
	}
	
	/**
	 * This method builds the job posting copy sql script.
	 */
	public String getCopySql(String franId) {
		StringBuilder sb = new StringBuilder();
		sb.append("insert into ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("FTS_JOB_POSTING (FRANCHISE_ID, ORGANIZATION_ID, JOB_TITLE_NM, ");
		sb.append("JOB_LOC_NM, JOB_HRS, JOB_DESC, JOB_RESP, JOB_EXP_REQ, ");
		sb.append("JOB_DSRD_SKILLS, JOB_ADTL_COMMENTS, JOB_CONTACT_EMAIL, ");
		sb.append("JOB_POST_DT, CREATE_DT, JOB_APPROVAL_FLG, JOB_POSTING_ID) ");
		sb.append("select '").append(franId).append("', ORGANIZATION_ID, JOB_TITLE_NM + ' (copy)', ");
		sb.append("JOB_LOC_NM, JOB_HRS, JOB_DESC, JOB_RESP, JOB_EXP_REQ, ");
		sb.append("JOB_DSRD_SKILLS, JOB_ADTL_COMMENTS, JOB_CONTACT_EMAIL, ");
		sb.append("JOB_POST_DT, ?, JOB_APPROVAL_FLG, ? ");
		sb.append("from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("FTS_JOB_POSTING where JOB_POSTING_ID = ?");
		return sb.toString();
	}
	
	/**
	 * This method builds the insert job posting sql script.
	 */
	public String getInsertSql() {
		StringBuilder sb = new StringBuilder();
		sb.append("insert into ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("FTS_JOB_POSTING (FRANCHISE_ID, ORGANIZATION_ID, JOB_TITLE_NM, ");
		sb.append("JOB_LOC_NM, JOB_HRS, JOB_DESC, JOB_RESP, JOB_EXP_REQ, ");
		sb.append("JOB_DSRD_SKILLS, JOB_ADTL_COMMENTS, JOB_CONTACT_EMAIL, ");
		sb.append("JOB_POST_DT, CREATE_DT, JOB_APPROVAL_FLG, JOB_ADDRESS_TXT, ");
		sb.append("JOB_ADDRESS2_TXT, JOB_CITY_NM, JOB_STATE_CD, JOB_ZIP_CD, JOB_COUNTRY_CD, ");
		sb.append("JOB_PRIMARY_PHONE_NO, FRANCHISE_LINK_FLG, JOB_POSTING_ID) ");
		sb.append("values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ");
		return sb.toString();
	}
	
	/**
	 * This method build the update job posting sql script.
	 */
	public String getUpdateSql() {
		StringBuilder sb = new StringBuilder();
		sb.append("update ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("FTS_JOB_POSTING set FRANCHISE_ID=?, ORGANIZATION_ID=?, ");
		sb.append("JOB_TITLE_NM=?, JOB_LOC_NM=?, JOB_HRS=?, JOB_DESC=?, ");
		sb.append("JOB_RESP=?, JOB_EXP_REQ=?, JOB_DSRD_SKILLS=?, JOB_ADTL_COMMENTS=?, ");
		sb.append("JOB_CONTACT_EMAIL=?, JOB_POST_DT=?, UPDATE_DT=?, ");
		sb.append("JOB_APPROVAL_FLG=?, JOB_ADDRESS_TXT=?, JOB_ADDRESS2_TXT=?, ");
		sb.append("JOB_CITY_NM=?, JOB_STATE_CD=?, JOB_ZIP_CD=?, JOB_COUNTRY_CD=?, ");
		sb.append("JOB_PRIMARY_PHONE_NO=?, FRANCHISE_LINK_FLG=? where JOB_POSTING_ID=? ");
		return sb.toString();
	}
	
	/** submits the job to the FTS admins for approval 
	 * @throws ActionException **/
	protected void submitJobs(SMTServletRequest req) throws ActionException {
		log.debug("Begninng approval submittal method");
		
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		StringBuilder redir = new StringBuilder(page.getFullPath());
		redir.append("?");
		
		ApprovalFacadeAction aA = new ApprovalFacadeAction(this.actionInit);
		aA.setDBConnection(dbConn);
		aA.setAttributes(attributes);
		ApprovalVO avo = new ApprovalVO();
		String [] pageIds = null;
		if(req.hasParameter("jobPostingId")){
			pageIds = req.getParameter("jobPostingId").split(",");
		} else if(req.hasParameter("jobsToSubmit")){
			pageIds = req.getParameter("jobsToSubmit").split(",");
		}
		List<AbstractChangeLogVO> vos = new ArrayList<AbstractChangeLogVO>();
		for(String id : pageIds){
			req.setParameter("componentId", id.trim());
			vos.add(new CareerLogVO(req));
		}
		avo.setChangeLogList(CareerLogVO.TYPE_ID, vos);
		req.setAttribute("approvalVO", avo);
		aA.update(req);
		
		//finish the redirect
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, redir.toString());
	}
	
	
}
