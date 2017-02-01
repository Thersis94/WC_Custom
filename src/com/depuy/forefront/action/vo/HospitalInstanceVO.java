package com.depuy.forefront.action.vo;

import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBModuleVO;

public class HospitalInstanceVO extends SBModuleVO{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String hospitalInstId = null;
	private String hospitalId = null;
	private String programId = null;
	private String siteId = null;
	private String routineHeaderText = null;
	private String actionPlanHeaderText = null;
	private Date createDate = null;
	private Date updateDate = null;
	private HospitalVO hospitalVO = null;
	private ProgramVO programVO = null;
	
	public HospitalInstanceVO(){
		
	}
	
	public HospitalInstanceVO(ResultSet rs){
		setData(rs);
	}
	
	public HospitalInstanceVO(ActionRequest req){
		setData(req);
	}
	
	public void setData(ResultSet rs){
		DBUtil db = new DBUtil();
		hospitalInstId = db.getStringVal("hospital_inst_id", rs);
		hospitalId = db.getStringVal("hospital_id", rs);
		programId = db.getStringVal("program_id", rs);
		siteId = db.getStringVal("site_id", rs);
		routineHeaderText = db.getStringVal("routine_header_txt", rs);
		actionPlanHeaderText = db.getStringVal("action_plan_header_txt", rs);
		createDate = db.getDateVal("create_dt", rs);
		updateDate = db.getDateVal("update_dt", rs);
		hospitalVO = new HospitalVO(rs);
		programVO = new ProgramVO(rs);
		db = null;
	}
	
	public void setData(ActionRequest req){
		hospitalInstId = req.getParameter("hospitalInstId");
		hospitalId = req.getParameter("hospitalId");
		programId = req.getParameter("programId");
		siteId = req.getParameter("siteId");
		routineHeaderText = req.getParameter("routineHeaderText");
		actionPlanHeaderText = req.getParameter("actionPlanHeaderText");
		createDate = Convert.formatDate(req.getParameter("createDate"));
		updateDate = Convert.formatDate(req.getParameter("updateDate"));
	}

	/**
	 * @return the hospitalInstId
	 */
	public String getHospitalInstId() {
		return hospitalInstId;
	}

	/**
	 * @param hospitalInstId the hospitalInstId to set
	 */
	public void setHospitalInstId(String hospitalInstId) {
		this.hospitalInstId = hospitalInstId;
	}

	/**
	 * @return the hospitalId
	 */
	public String getHospitalId() {
		return hospitalId;
	}

	/**
	 * @param hospitalId the hospitalId to set
	 */
	public void setHospitalId(String hospitalId) {
		this.hospitalId = hospitalId;
	}

	/**
	 * @return the programId
	 */
	public String getProgramId() {
		return programId;
	}

	/**
	 * @param programId the programId to set
	 */
	public void setProgramId(String programId) {
		this.programId = programId;
	}

	/**
	 * @return the siteId
	 */
	public String getSiteId() {
		return siteId;
	}

	/**
	 * @param siteId the siteId to set
	 */
	public void setSiteId(String siteId) {
		this.siteId = siteId;
	}

	/**
	 * @return the routineHeaderText
	 */
	public String getRoutineHeaderText() {
		return routineHeaderText;
	}

	/**
	 * @param routineHeaderText the routineHeaderText to set
	 */
	public void setRoutineHeaderText(String routineHeaderText) {
		this.routineHeaderText = routineHeaderText;
	}

	/**
	 * @return the actionPlanHeaderText
	 */
	public String getActionPlanHeaderText() {
		return actionPlanHeaderText;
	}

	/**
	 * @param actionPlanHeaderText the actionPlanHeaderText to set
	 */
	public void setActionPlanHeaderText(String actionPlanHeaderText) {
		this.actionPlanHeaderText = actionPlanHeaderText;
	}

	/**
	 * @return the createDate
	 */
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @return the updateDate
	 */
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	/**
	 * @return the hospitalVO
	 */
	public HospitalVO getHospitalVO() {
		return hospitalVO;
	}

	/**
	 * @param hospitalVO the hospitalVO to set
	 */
	public void setHospitalVO(HospitalVO hospitalVO) {
		this.hospitalVO = hospitalVO;
	}
	
	/**
	 * @return the programVO
	 */
	public ProgramVO getProgramVO() {
		return programVO;
	}

	/**
	 * @param hospitalVO the programVO to set
	 */
	public void setProgramVO(ProgramVO programVO) {
		this.programVO = programVO;
	}

	public String getHospitalAlias() {
		return StringUtil.removeWhiteSpace(hospitalId) +"-"+ StringUtil.removeWhiteSpace(this.programId);
	}

}
