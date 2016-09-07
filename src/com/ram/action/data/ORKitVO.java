package com.ram.action.data;

import java.sql.ResultSet;
import java.text.SimpleDateFormat;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>ORKitVO.java<p/>
 * <b>Description: Holds all information related to a particular kit without
 * loading it into the user's cart</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since September 6, 2016
 * <b>Changes: </b>
 ****************************************************************************/

public class ORKitVO {
	
	private String kitId;
	private String hospitalName;
	private String operatingRoom;
	private String surgeryDt;
	private String surgeonNm;
	private String resellerNm;
	private String caseId;
	private int numProducts;
	private int finalizedFlg;
	private String salesSignature;
	private String adminSignature;
	private String otherId;
	private String repId;
	
	public ORKitVO(){}
	
	public ORKitVO(ResultSet rs) {
		setData(rs);
	}
	
	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		setKitId(StringUtil.checkVal(db.getStringVal("RAM_KIT_INFO_ID", rs)));
		hospitalName = StringUtil.checkVal(db.getStringVal("HOSPITAL_NM", rs));
		operatingRoom = StringUtil.checkVal(db.getStringVal("OPERATING_ROOM", rs));
		surgeonNm = StringUtil.checkVal(db.getStringVal("SURGEON_NM", rs));
		resellerNm = StringUtil.checkVal(db.getStringVal("RESELLER_NM", rs));
		caseId = StringUtil.checkVal(db.getStringVal("CASE_ID", rs));
		otherId = StringUtil.checkVal(db.getStringVal("OTHER_ID", rs));
		repId = StringUtil.checkVal(db.getStringVal("REP_ID", rs));
		if (db.getDateVal("SURGERY_DT", rs) != null)
			surgeryDt = new SimpleDateFormat("MM-dd-yyyy").format(db.getDateVal("SURGERY_DT", rs));
		setNumProducts(db.getIntVal("NUM_PRODUCTS", rs));
		finalizedFlg = db.getIntVal("FINALIZED_FLG", rs);
		
	}
	
	public String getKitId() {
		return kitId;
	}

	public void setKitId(String kitId) {
		this.kitId = kitId;
	}

	public String getHospitalName() {
		return hospitalName;
	}
	public void setHospitalName(String hospitalName) {
		this.hospitalName = hospitalName;
	}
	public String getOperatingRoom() {
		return operatingRoom;
	}
	public void setOperatingRoom(String operatingRoom) {
		this.operatingRoom = operatingRoom;
	}
	public String getSurgeryDt() {
		return surgeryDt;
	}
	public void setSurgeryDt(String surgeryDt) {
		this.surgeryDt = surgeryDt;
	}
	public String getSurgeonNm() {
		return surgeonNm;
	}
	public void setSurgeonNm(String surgeonNm) {
		this.surgeonNm = surgeonNm;
	}
	public String getResellerNm() {
		return resellerNm;
	}
	public void setResellerNm(String resellerNm) {
		this.resellerNm = resellerNm;
	}
	public String getCaseId() {
		return caseId;
	}
	public void setCaseId(String caseId) {
		this.caseId = caseId;
	}

	public int getNumProducts() {
		return numProducts;
	}

	public void setNumProducts(int numProducts) {
		this.numProducts = numProducts;
	}

	public int getFinalizedFlg() {
		return finalizedFlg;
	}

	public void setFinalizedFlg(int finalizedFlg) {
		this.finalizedFlg = finalizedFlg;
	}
	
	public boolean isFinalized() {
		return Convert.formatBoolean(finalizedFlg);
	}

	public String getSalesSignature() {
		return salesSignature;
	}

	public void setSalesSignature(String salesSignature) {
		this.salesSignature = salesSignature;
	}

	public String getAdminSignature() {
		return adminSignature;
	}

	public void setAdminSignature(String adminSignature) {
		this.adminSignature = adminSignature;
	}

	public String getOtherId() {
		return otherId;
	}

	public void setOtherId(String otherId) {
		this.otherId = otherId;
	}

	public String getRepId() {
		return repId;
	}

	public void setRepId(String repId) {
		this.repId = repId;
	}
}
