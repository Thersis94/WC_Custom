package com.depuy.forefront.action.vo;

import java.io.Serializable;
import java.sql.ResultSet;

import com.depuy.forefront.action.ProgramAction;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: TreatmentCalendarVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 20, 2012
 ****************************************************************************/
public class TreatCalItemVO implements Serializable {

	private static final long serialVersionUID = 8763041035462448250L;
	private String treatCalItemId = null;
	private String programId = null;
	private String hospitalId = null;
	private String hospitalName = null;
	private String entryName = null;
	private String bodyText = null;
	private String summaryText = null;

	/**
	 * Variable for use in the _XR
	 */
	private String treatCalId = null;
	private Integer orderNo = 0;
	
	
	public TreatCalItemVO() {
		super();
	}
	
	public TreatCalItemVO(ResultSet rs) {
		DBUtil db = new DBUtil();
		setTreatCalItemId(db.getStringVal("treat_cal_item_id", rs));
		setProgramId(db.getStringVal("program_id", rs));
		setHospitalId(db.getStringVal("hospital_id", rs));
		setHospitalName(db.getStringVal("hospital_nm", rs));
		setEntryName(db.getStringVal("entry_nm", rs));
		
		setSummaryText(db.getStringVal("item_summary_txt", rs));
		if (summaryText == null || summaryText.length() == 0)
			setSummaryText(db.getStringVal("summary_txt", rs));
		
		setBodyText(db.getStringVal("body_txt", rs));

		treatCalId = db.getStringVal("treat_cal_xr_id", rs);
		orderNo = db.getIntegerVal("order_no", rs);
		db = null;
	}
	
	public TreatCalItemVO(SMTServletRequest req) {
		if (req.hasParameter("treatCalItemId")) setTreatCalItemId(req.getParameter("treatCalItemId"));
		programId = (String) req.getSession().getAttribute(ProgramAction.PROGRAM_ID);
		if (req.hasParameter("hospitalId")) setHospitalId(req.getParameter("hospitalId"));
		setEntryName(req.getParameter("entryName"));
		setSummaryText(req.getParameter("summaryText"));
		setBodyText(req.getParameter("bodyText"));

		treatCalId = StringUtil.checkVal(req.getParameter("treatCalId"), null);
		orderNo = Convert.formatInteger(req.getParameter("orderNo"), 0);
		
	}
	
	public String getTreatCalId() {
		return treatCalId;
	}

	public void setTreatCalId(String treatCalId) {
		this.treatCalId = treatCalId;
	}

	public String getBodyText() {
		return bodyText;
	}

	public void setBodyText(String bodyText) {
		this.bodyText = bodyText;
	}

	public String getEntryName() {
		return entryName;
	}

	public void setEntryName(String entryName) {
		this.entryName = entryName;
	}

	public String getSummaryText() {
		return summaryText;
	}

	public void setSummaryText(String summaryText) {
		this.summaryText = summaryText;
	}

	public String getHospitalId() {
		return hospitalId;
	}

	public void setHospitalId(String hospitalId) {
		this.hospitalId = hospitalId;
	}

	public String getTreatCalItemId() {
		return treatCalItemId;
	}

	public void setTreatCalItemId(String treatCalItemId) {
		this.treatCalItemId = treatCalItemId;
	}

	public String getProgramId() {
		return programId;
	}

	public void setProgramId(String programId) {
		this.programId = programId;
	}

	public Integer getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(Integer orderNo) {
		this.orderNo = orderNo;
	}

	public String getHospitalName() {
		return hospitalName;
	}

	public void setHospitalName(String hospitalName) {
		this.hospitalName = hospitalName;
	}
	
}
