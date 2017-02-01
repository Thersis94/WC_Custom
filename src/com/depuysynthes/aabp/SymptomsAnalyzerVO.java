/**
 *
 */
package com.depuysynthes.aabp;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: SymptomsAnalyzerVO.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Helper VO that manages extracting Lookup Params off
 * the Request Object.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author raptor
 * @version 1.0
 * @since Dec 7, 2015
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class SymptomsAnalyzerVO {

	private String pain;
	private String onset;
	private boolean legPain;
	private boolean armPain;
	private boolean curve;
	private boolean hunchback;

	/**
	 * 
	 */
	public SymptomsAnalyzerVO() {
	}

	public SymptomsAnalyzerVO(ActionRequest req) {
		setData(req);
	}

	/**
	 * Pull data out of fields on request.
	 * @param req
	 */
	private void setData(ActionRequest req) {
		pain = StringUtil.checkVal(req.getParameter("pain"));
		onset = StringUtil.checkVal(req.getParameter("onset"));
		legPain = Convert.formatBoolean(req.getParameter("leg_pain"));
		armPain = Convert.formatBoolean(req.getParameter("arm_pain"));
		curve = Convert.formatBoolean(req.getParameter("curve"));
		hunchback = Convert.formatBoolean(req.getParameter("hunchback"));
	}

	//Getters
	public String getPain() {return pain;}
	public String getOnset() {return onset;}
	public boolean isLegPain() {return legPain;}
	public boolean isArmPain() {return armPain;}
	public boolean isCurve() {return curve;}
	public boolean isHunchback() {return hunchback;}

	//SETTERS
	public void setPain(String pain) {this.pain = pain;}
	public void setOnset(String onset) {this.onset = onset;}
	public void setLegPain(boolean legPain) {this.legPain = legPain;}
	public void setArmPain(boolean armPain) {this.armPain = armPain;}
	public void setCurve(boolean curve) {this.curve = curve;}
	public void setHunchback(boolean hunchback) {this.hunchback = hunchback;}
}