package com.depuysynthesinst;

// SMTBaseLibs
import java.util.Map;

import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title: </b>DSIUserDataVO.java <p/>
 * <b>Project: </b>DSI-WS2 <p/>
 * <b>Description: Wraps UserDataVO with some getters for Registration questions,
 * so invoking actions don't need to know we got the data from Registration (fields).</b>
 * </p>
 * <b>Copyright: </b>Copyright (c) 2015<p/>
 * <b>Company: </b>Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0<p/>
 * @since Jun 11, 2015<p/>
 *<b>Changes: </b>
 * Jun 11, 2015: David Bargerhuff: Created class.
 ****************************************************************************/
public class DSIUserDataVO extends UserDataVO {
	
	private static final long serialVersionUID = 6745777068563527968L;
	
	/**
	 * Register_field_id values from the database/registration portlet
	 */
	public enum RegField {
		DSI_SYNTHES_ID,
		DSI_TTLMS_ID,
		DSI_GRAD_DT,
		DSI_MY_ASSIGNMENTS,
		DSI_PROG_ELIGIBLE,
		DSI_ACAD_NM,
		c0a80241b71c9d40a59dbd6f4b621260, //profession
		c0a80241b71d27b038342fcb3ab567a0, //specialty
		;
	}
	
	/**
	 * 
	 */
	public DSIUserDataVO(UserDataVO vo) {
		super();
		super.setData(vo.getDataMap());
	}

	/**
	 * @return the synthesId
	 */
	public String getSynthesId() {
		return StringUtil.checkVal(super.getAttribute(RegField.DSI_SYNTHES_ID.toString()));
	}

	/**
	 * @param synthesId the synthesId to set
	 */
	public void setSynthesId(String synthesId) {
		super.addAttribute(RegField.DSI_SYNTHES_ID.toString(), synthesId);
	}

	/**
	 * @return the dsiId
	 */
	public String getDsiId() {
		return super.getProfileId();
	}

	/**
	 * @return the ttLmsId
	 */
	public String getTtLmsId() {
		return StringUtil.checkVal(super.getAttribute(RegField.DSI_TTLMS_ID.toString()));
	}

	/**
	 * @param ttLmsId the ttLmsId to set
	 */
	public void setTtLmsId(String ttLmsId) {
		super.addAttribute(RegField.DSI_TTLMS_ID.toString(), ttLmsId);
	}

	/**
	 * @return the hospital
	 */
	public String getHospital() {
		return StringUtil.checkVal(super.getAttribute(RegField.DSI_ACAD_NM.toString()));
	}

	/**
	 * @param hospital the hospital to set
	 */
	public void setHospital(String hospital) {
		super.addAttribute(RegField.DSI_ACAD_NM.toString(), hospital);
	}

	/**
	 * @return the specialty
	 */
	public String getSpecialty() {
		return StringUtil.checkVal(super.getAttribute(RegField.c0a80241b71d27b038342fcb3ab567a0.toString()));
	}

	/**
	 * @param specialty the specialty to set
	 */
	public void setSpecialty(String specialty) {
		super.addAttribute(RegField.c0a80241b71d27b038342fcb3ab567a0.toString(), specialty);
	}

	/**
	 * @return the profession
	 */
	public String getProfession() {
		return StringUtil.checkVal(super.getAttribute(RegField.c0a80241b71c9d40a59dbd6f4b621260.toString()));
	}

	/**
	 * @param profession the profession to set
	 */
	public void setProfession(String profession) {
		super.addAttribute(RegField.c0a80241b71c9d40a59dbd6f4b621260.toString(), profession);
	}

	/**
	 * @return the eligible
	 */
	public boolean isEligible() {
		return Convert.formatBoolean(super.getAttribute(RegField.DSI_PROG_ELIGIBLE.toString()));
	}

	/**
	 * @param eligible the eligible to set
	 */
	public void setEligible(boolean eligible) {
		super.addAttribute(RegField.DSI_PROG_ELIGIBLE.toString(), eligible);
	}
	public void setEligible(double eligible) {
		setEligible(Convert.formatBoolean(Double.valueOf(eligible)).booleanValue());
	}

	/**
	 * @return the verified
	 */
	public boolean isVerified() {
		return true;
	}

	/**
	 * @param verified the verified to set
	 */
	public void setVerified(boolean verified) {
		//TODO
	}
	public void setVerified(double verified) {
		setVerified(Convert.formatBoolean(Double.valueOf(verified)).booleanValue());
	}
	
	
	/**
	 * some static methods to isolate business logic without creating tons of Objects we really don't need.
	 * These are called from DSIRoleMgr
	 */
	public static String getProfession(UserDataVO user) {
		if (user == null) return null;
		return StringUtil.checkVal(user.getAttribute(RegField.c0a80241b71c9d40a59dbd6f4b621260.toString()));
	}
	
	
	public static void setAttributesFromMap(UserDataVO userVo, Map<Object, Object> data) {
		//check for null
		if (data == null || data.size() == 0) return;
		//check for errors
		Object o = data.get("ERROR");
		if (Convert.formatInteger(o.toString()) != 0) return;
		
		//populate key fields if they're present
		DSIUserDataVO user = new DSIUserDataVO(userVo);
		if (data.containsKey("TTLMSID")) user.setTtLmsId(o.toString());
		if (data.containsKey("ELIGIBLEPROGRAM")) user.setEligible(Double.valueOf(o.toString()));
		if (data.containsKey("VERIFIED")) user.setVerified(Double.valueOf(o.toString()));
		if (data.containsKey("SYNTHESID")) user.setSynthesId(o.toString());
	}
	
	public static void setTTLMSID(UserDataVO user, double d) {
		user.addAttribute(RegField.DSI_TTLMS_ID.toString(), Convert.formatInteger(Double.valueOf(d).intValue()));
	}
}
