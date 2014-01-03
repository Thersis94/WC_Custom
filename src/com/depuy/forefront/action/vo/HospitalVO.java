package com.depuy.forefront.action.vo;

import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBModuleVO;

public class HospitalVO extends SBModuleVO{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String hospitalId = null;
	private String hospitalName = null;
	private Date createDate = null;
	private Date updateDate = null;
	
	public HospitalVO(){
		
	}
	
	public HospitalVO(ResultSet rs){
		setData(rs);
	}
	
	public HospitalVO(SMTServletRequest req){
		setData(req);
	}
	
	public void setData(ResultSet rs){
		DBUtil db = new DBUtil();
		hospitalId = db.getStringVal("hospital_id", rs);
		hospitalName = db.getStringVal("hospital_nm", rs);
		createDate = db.getDateVal("create_dt", rs);
		updateDate = db.getDateVal("update_dt", rs);
		db = null;
	}
	
	public void setData(SMTServletRequest req){
		hospitalId = req.getParameter("hospitalId");
		hospitalName = req.getParameter("hospitalName");
		createDate = Convert.formatDate(req.getParameter("createDate"));
		updateDate = Convert.formatDate(req.getParameter("updateDate"));
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
	 * @return the hospitalName
	 */
	public String getHospitalName() {
		return hospitalName;
	}

	/**
	 * @param hospitalName the hospitalName to set
	 */
	public void setHospitalName(String hospitalName) {
		this.hospitalName = hospitalName;
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
	
}
