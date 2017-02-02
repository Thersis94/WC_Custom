package com.depuy.forefront.action.vo;

import java.io.Serializable;
import java.sql.ResultSet;
import com.depuy.forefront.action.ProgramAction;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;

public class StageVO implements Serializable {

	private static final long serialVersionUID = 123131231L;
	private String stageId = null;
	private String programId = null;
	private String stageName = null;
	private String urlAliasText = null;
	private Integer surgeryWeekNo = null;
	
	public StageVO(){
		
	}
	
	public StageVO(ResultSet rs) {
		DBUtil db = new DBUtil();
		stageId = db.getStringVal("stage_id", rs);
		programId = db.getStringVal("program_id", rs);
		stageName = db.getStringVal("stage_nm", rs);
		urlAliasText = db.getStringVal("url_alias_txt", rs);
		surgeryWeekNo = db.getIntegerVal("surgery_week_no", rs);
		db = null;
	}
	
	public StageVO(ActionRequest req) {
		stageId = req.getParameter("stageId");
		programId = (String) req.getSession().getAttribute(ProgramAction.PROGRAM_ID);
		stageName = req.getParameter("stageName");
		urlAliasText = req.getParameter("urlAliasText");
		surgeryWeekNo = Convert.formatInteger(req.getParameter("surgeryWeekNo"),0, false);
	}
	
	/**
	 * @return the stageId
	 */
	public String getStageId() {
		return stageId;
	}

	/**
	 * @param stageId the stageId to set
	 */
	public void setStageId(String stageId) {
		this.stageId = stageId;
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
	 * @return the stageName
	 */
	public String getStageName() {
		return stageName;
	}

	/**
	 * @param stageName the stageName to set
	 */
	public void setStageName(String stageName) {
		this.stageName = stageName;
	}

	/**
	 * @return the urlAliasText
	 */
	public String getUrlAliasText() {
		return urlAliasText;
	}

	/**
	 * @param categoryName the urlAliasText to set
	 */
	public void setUrlAliasText(String urlAliasText) {
		this.urlAliasText = urlAliasText;
	}

	/**
	 * @return the surgeryWeekNo
	 */
	public Integer getSurgeryWeekNo() {
		return surgeryWeekNo;
	}

	/**
	 * @param surgeryWeekNo the surgeryWeekNo to set
	 */
	public void setSurgeryWeekNo(Integer surgeryWeekNo) {
		this.surgeryWeekNo = surgeryWeekNo;
	}

}
