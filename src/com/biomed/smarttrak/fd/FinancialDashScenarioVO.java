package com.biomed.smarttrak.fd;

import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.smt.sitebuilder.action.SBModuleVO;

/****************************************************************************
 * <b>Title</b>: FinancialDashScenarioVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Jan 25, 2017
 ****************************************************************************/

@Table(name="biomedgps_fd_scenario")
public class FinancialDashScenarioVO extends SBModuleVO {
	
	private static final long serialVersionUID = 1L;
	private String scenarioId;
	private String userId;
	private String teamId;
	private String scenarioNm;
	private String statusFlg;
	private Date refreshDt;
	private Date createDt;
	private Date updateDt;
	
	public FinancialDashScenarioVO() {
		super();
	}

	public FinancialDashScenarioVO(ResultSet rs) {
		super(rs);
		setData(rs);
	}
	
	public FinancialDashScenarioVO(ActionRequest req) {
		this();
		setData(req);
	}
	
	/**
	 * Sets data from a ResultSet
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		DBUtil util = new DBUtil();
		
		setScenarioId(util.getStringVal("SCENARIO_ID", rs));
		setUserId(util.getStringVal("USER_ID", rs));
		setTeamId(util.getStringVal("TEAM_ID", rs));
		setScenarioNm(util.getStringVal("SCENARIO_NM", rs));
		setStatusFlg(util.getStringVal("STATUS_FLG", rs));
		setRefreshDt(util.getDateVal("REFRESH_DT", rs));
		setCreateDt(util.getDateVal("CREATE_DT", rs));
		setUpdateDt(util.getDateVal("UPDATE_DT", rs));
	}

	/**
	 * Sets data from the ActionRequest
	 * 
	 * @param req
	 */
	public void setData(ActionRequest req) {
		setScenarioId(req.getParameter("scenarioId"));
		setUserId(req.getParameter("userId"));
		setTeamId(req.getParameter("teamId"));
		setScenarioNm(req.getParameter("scenarioNm"));
		setStatusFlg(req.getParameter("statusFlg"));
	}
	
	/**
	 * @return the scenarioId
	 */
	@Column(name="scenario_id", isPrimaryKey=true)
	public String getScenarioId() {
		return scenarioId;
	}

	/**
	 * @return the userId
	 */
	@Column(name="user_id")
	public String getUserId() {
		return userId;
	}

	/**
	 * @return the teamId
	 */
	@Column(name="team_id")
	public String getTeamId() {
		return teamId;
	}

	/**
	 * @return the scenarioNm
	 */
	@Column(name="scenario_nm")
	public String getScenarioNm() {
		return scenarioNm;
	}

	/**
	 * @return the statusFlg
	 */
	@Column(name="status_flg")
	public String getStatusFlg() {
		return statusFlg;
	}

	/**
	 * @return the refreshDt
	 */
	@Column(name="refresh_dt")
	public Date getRefreshDt() {
		return refreshDt;
	}

	/**
	 * @return the createDt
	 */
	@Column(name="create_dt", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDt() {
		return createDt;
	}

	/**
	 * @return the updateDt
	 */
	@Column(name="update_dt", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDt() {
		return updateDt;
	}

	/**
	 * @param scenarioId the scenarioId to set
	 */
	public void setScenarioId(String scenarioId) {
		this.scenarioId = scenarioId;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * @param teamId the teamId to set
	 */
	public void setTeamId(String teamId) {
		this.teamId = teamId;
	}

	/**
	 * @param scenarioNm the scenarioNm to set
	 */
	public void setScenarioNm(String scenarioNm) {
		this.scenarioNm = scenarioNm;
	}

	/**
	 * @param statusFlg the statusFlg to set
	 */
	public void setStatusFlg(String statusFlg) {
		this.statusFlg = statusFlg;
	}

	/**
	 * @param refreshDt the refreshDt to set
	 */
	public void setRefreshDt(Date refreshDt) {
		this.refreshDt = refreshDt;
	}

	/**
	 * @param createDt the createDt to set
	 */
	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}

	/**
	 * @param updateDt the updateDt to set
	 */
	public void setUpdateDt(Date updateDt) {
		this.updateDt = updateDt;
	}
}
