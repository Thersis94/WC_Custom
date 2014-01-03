package com.ansmed.sb.action;

// JDK 1.6.0
import java.io.Serializable;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Date;

import com.ansmed.sb.physician.SurgeonVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/*****************************************************************************
<p><b>Title</b>: PhysQualDataVO.java</p>
<p>Description: VO to hold the Qualification Data used to determine what eventTypes
a surgeon has visibility to.  Also implements rules to test these qualifications.</p>
<p>Copyright: Copyright (c) 2000 - 2009 SMT, All Rights Reserved</p>
<p>Company: Silicon Mountain Technologies</p>
@author James McKain
@version 1.0
@since Jun 24, 2009
Last Updated:
 ***************************************************************************/
public class PhysQualDataVO implements Serializable {
	private static final long serialVersionUID = 100L;

	// Member variables
	private Boolean isFellowship = Boolean.FALSE;
	private int specialtyId = 0;
	private String specialtyNm = "";
	private Date scsStartDate = null;
	private int implantCnt = 0;
	private String surgeonId = "";
	private String eventType = ""; //used by JSTL while looping the events list
	private int sjmPercent = 0;
	private int bscPercent = 0;
	private int mdtPercent = 0;
	private int unkPercent = 0;
	private SurgeonVO surgeonVO = null;
	
	// Member vars for use by PhysExtQualDataAction
	private Boolean isAltData = Boolean.FALSE;
	private int sjmTrials = 0;
	private int sjmPerms = 0;
	private int bsxTrials = 0;
	private int bsxPerms = 0;
	private int mdtTrials = 0;
	private int mdtPerms = 0;

	/**
	 * @param rs
	 */
	public PhysQualDataVO() {
	}
	
	/**
	 * Sets the standard fields used by various actions using query results.
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		surgeonId = db.getStringVal("surgeon_id", rs);
		specialtyId = db.getIntVal("specialty_id", rs);
		scsStartDate = db.getDateVal("scs_start_dt", rs);
		isFellowship = (2 == db.getIntVal("surgeon_type_id", rs));
		surgeonVO = new SurgeonVO(rs);
		db = null;
	}
	
	/**
	 * Sets certain standard fields used by various actions using the request.
	 * @param req
	 */
	public void setData(SMTServletRequest req) {
		if (req.getParameter("scsStartDate") != null)
			scsStartDate = Convert.formatDate(req.getParameter("scsStartDate"));
		
		if (req.getParameter("specialtyId") != null)
			specialtyId = Convert.formatInteger(req.getParameter("specialtyId"));
	}
	
	/**
	 * Sets the fields used with PhysExtQualDataAction.  Handles the 'trend data'
	 * values that a rep can enter for a physician so that a physician can 
	 * qualify for events based on the 'trend data' which might be more current
	 * than the plan data.
	 * @param rs
	 */
	public void setAlternateData(ResultSet rs) {
		// set the common qualification info
		setData(rs);
		
		// set the alternate data 
		DBUtil db = new DBUtil();
		sjmTrials = db.getIntVal("sjm_trials_no", rs);
		sjmPerms = db.getIntVal("sjm_perms_no", rs);
		bsxTrials = db.getIntVal("bsx_trials_no", rs);
		bsxPerms = db.getIntVal("bsx_perms_no", rs);
		mdtTrials = db.getIntVal("mdt_trials_no", rs);
		mdtPerms = db.getIntVal("mdt_perms_no", rs);
		setImplantCnt(sjmTrials + sjmPerms + bsxTrials + bsxPerms + mdtTrials + mdtPerms);
		setIsAltData(Boolean.TRUE);
		db = null;
	}
	
	/**
	 * Used when calculating qualifications based on business plan data.  Adds
	 * implant data percent values.
	 * @param field
	 * @param value
	 */
	public void addImplantData(String field, String value) {
		
		field = StringUtil.checkVal(field);
		value = StringUtil.checkVal(value);
		
		if (field.length() == 0) return;
		
		int val = 0;
		if (value.length() > 0) {
			val = Convert.formatInteger(value.trim()).intValue();
		}
		if (field.equalsIgnoreCase("bostonscientific")) {
			this.setBscPercent(val);
		} else if (field.equalsIgnoreCase("medtronic")) {
			this.setMdtPercent(val);
		} else if (field.equalsIgnoreCase("sjmmedical")) {
			this.setSjmPercent(val);
		} else if (field.equalsIgnoreCase("unknown")) {
			this.setUnknownPercent(val);
		} else if (field.startsWith("trialQ") || field.startsWith("permQ")) {
			this.setImplantCnt(implantCnt + val);
		}		 
	}
	
	/**
	 * Calculates total implant count based on SJM implant count, SJM market
	 * share data, and competitor implant market share data.
	 */
	public void setImplantCountUsingMarketShare() {
		
		int sjmUsed = this.getImplantCnt();
				
		Integer sjmPct = this.getSjmPercent();
		
		Double calcPctTotal = 0.0;
		
		if (sjmPct > 0) {
			calcPctTotal = sjmUsed/sjmPct.doubleValue();
		} 
		
		//Remaining implant numbers are calculated using the computed total. 
		int mdtUsed = ((Double)(calcPctTotal * this.getMdtPercent())).intValue();
		int bscUsed = ((Double)(calcPctTotal * this.getBscPercent())).intValue();
		int unkUsed = ((Double)(calcPctTotal * this.getUnknownPercent())).intValue();
		
		this.setImplantCnt(sjmUsed + mdtUsed + bscUsed + unkUsed);
		
	}

	public void setImplantCnt(int c) {
		implantCnt = c;
	}
	
	public int getSpecialtyId() {
		return specialtyId;
	}

	/**
	 * @return the specialtyNm
	 */
	public String getSpecialtyNm() {
		return specialtyNm;
	}

	/**
	 * @param specialtyNm the specialtyNm to set
	 */
	public void setSpecialtyNm(String specialtyNm) {
		this.specialtyNm = specialtyNm;
	}

	public Date getScsStartDate() {
		return scsStartDate;
	}

	public int getImplantCnt() {
		return implantCnt;
	}
	
	public String getSurgeonId() {
		return surgeonId;
	}
	
	public int getYearsExperience() {
		int retVal = 0;
		if (scsStartDate != null) {
			Calendar scs = Calendar.getInstance();
			int yr = scs.get(Calendar.YEAR);			
			scs.setTime(scsStartDate);
			retVal = yr - scs.get(Calendar.YEAR);
		}
		
		return retVal;		
	}

	public void setSurgeonId(String s) {
		this.surgeonId = s;
	}
	
	public void setEventType(String type) {
		this.eventType = type;
	}
	
	/**
	 * Uses eventType of the event(s) to determine whether this physician
	 * is qualified to view these "types" of events.  
	 * e.g. Fellows are not qualified to see Innovation or T&P series events.
	 * 
	 * SPECIALTIES:
	 *  1	Anesthesiologist
		2	Ortho Spine
		3	Neurosurgeon
		8	Neurologist
		16	HCP, Referral source or Other
		21	Physiatrist (PM&R)
	 * @return
	 */
	public Boolean isQualified() {
		Boolean retVal = Boolean.FALSE;
		//determine if the physician is qualified for this eventType
		
		/* NOT YET IN EFFECT.  Thes are updated rules reflecting 
		 * updated eventType values.
		 
		if (eventType.equalsIgnoreCase("Fellows-Beginner - Anatomic lab")) {
			retVal = (isFellowship);
			
		} else if (eventType.equalsIgnoreCase("Foundations in Neuromodulation-Beginner - Anatomic lab")) {
			retVal = (implantCnt < 6 && (specialtyId == 1 || specialtyId == 2 || 
						specialtyId == 3 || specialtyId == 21));
			
		} else if (eventType.equalsIgnoreCase("Theory and Practice-Intermediate - Anatomic lab")) {
			retVal = (this.getYearsExperience() > 0 && implantCnt > 5 && 
						(specialtyId == 1 || specialtyId == 2 || specialtyId == 3 || 
						specialtyId == 21));
			
		} else if (eventType.equalsIgnoreCase("Theory and Practice-PM&R")) {
			retVal = (implantCnt > 5 && getYearsExperience() > 0 && specialtyId == 21);
			
		} else if (eventType.equalsIgnoreCase("Correlations in Neuromodulation-Intermediate/Advanced - Discussion based")) {
			retVal = (implantCnt > 10 && getYearsExperience() > 1 && 
						(specialtyId == 1 || specialtyId == 2 || specialtyId == 3 || 
						specialtyId == 21));
			
		} else if (eventType.equalsIgnoreCase("Innovations-Discussion based")) {
			retVal = (implantCnt > 19 && getYearsExperience() > 3 && getYearsExperience() < 6 && 
						(specialtyId == 1 || specialtyId == 2 || specialtyId == 3 || 
						specialtyId == 21));
			
		} else if (eventType.equalsIgnoreCase("Innovations-Advanced")) {
			retVal = (implantCnt > 25 && getYearsExperience() > 5 && 
						(specialtyId == 1 || specialtyId == 2 || specialtyId == 3 || 
						specialtyId == 21));	
		}
		*/
		
		// ORIGINAL rules...
		//these business rules were provided by ANS, 06/23/09
		if (eventType.equalsIgnoreCase("Fellowship-Novice Fellowship Events")) {
			retVal = (isFellowship);
			
		} else if (eventType.equalsIgnoreCase("Foundations-Anatomic lab")) {
			retVal = (implantCnt < 6 && (specialtyId == 1 || specialtyId == 2 || 
						specialtyId == 3 || specialtyId == 21));
			
		} else if (eventType.equalsIgnoreCase("Theory & Practice-Anatomic lab")) {
			retVal = (this.getYearsExperience() > 0 && implantCnt > 5 && 
						(specialtyId == 1 || specialtyId == 2 || specialtyId == 3 || 
						specialtyId == 21));
			
		} else if (eventType.equalsIgnoreCase("Theory & Practice-PM&R")) {
			retVal = (implantCnt > 5 && getYearsExperience() > 0 && specialtyId == 21);
			
		} else if (eventType.equalsIgnoreCase("Correlations-Discussion based")) {
			retVal = (implantCnt > 10 && getYearsExperience() > 1 && 
						(specialtyId == 1 || specialtyId == 2 || specialtyId == 3 || 
						specialtyId == 21));
			
		} else if (eventType.equalsIgnoreCase("Innovations-Discussion based")) {
			retVal = (implantCnt > 19 && getYearsExperience() > 3 && getYearsExperience() < 6 && 
						(specialtyId == 1 || specialtyId == 2 || specialtyId == 3 || 
						specialtyId == 21));
			
		} else if (eventType.equalsIgnoreCase("Innovations-Advanced")) {
			retVal = (implantCnt > 25 && getYearsExperience() > 5 && 
						(specialtyId == 1 || specialtyId == 2 || specialtyId == 3 || 
						specialtyId == 21));	
		}
		
		return retVal;
	}
	
	/**
	 * Returns qualification test boolean value.
	 * @return
	 */
	public Boolean getQualified() { return isQualified(); }
	
	/**
	 * determines whether we need to request QualData from the user or not.
	 * @return
	 */
	public Boolean isQualDataComplete() {
		return (specialtyId > 0 && scsStartDate != null && implantCnt >= 0);
	}
	
	/**
	 * Returns boolean value reflecting whether or not the qualification
	 * data is complete.
	 * @return
	 */
	public Boolean getQualDataComplete() { return isQualDataComplete(); }
	
	/**
	 * Returns the boolean value reflecting whether or not this physician's 
	 * type is Fellowship. 
	 * @return
	 */
	public Boolean isFellowship() {
		return this.isFellowship;
	}
	
	/**
	 * Returns values of this object as a String.
	 */
	public String toString() {
		return "specialtyId=" + specialtyId + ", implants=" + implantCnt + 
				", fellowship=" + isFellowship + ", scsStartDt=" + scsStartDate +
				", yrsExp=" + this.getYearsExperience();
	}

	/**
	 * @return the sjmPercent
	 */
	public int getSjmPercent() {
		return sjmPercent;
	}

	/**
	 * @param sjmPercent the sjmPercent to set
	 */
	public void setSjmPercent(int sjmPercent) {
		this.sjmPercent = sjmPercent;
	}

	/**
	 * @return the bscPercent
	 */
	public int getBscPercent() {
		return bscPercent;
	}

	/**
	 * @param bscPercent the bscPercent to set
	 */
	public void setBscPercent(int bscPercent) {
		this.bscPercent = bscPercent;
	}

	/**
	 * @return the mdtPercent
	 */
	public int getMdtPercent() {
		return mdtPercent;
	}

	/**
	 * @param mdtPercent the mdtPercent to set
	 */
	public void setMdtPercent(int mdtPercent) {
		this.mdtPercent = mdtPercent;
	}

	/**
	 * @return the unkPercent
	 */
	public int getUnknownPercent() {
		return unkPercent;
	}

	/**
	 * @param unkPercent the unkPercent to set
	 */
	public void setUnknownPercent(int unkPercent) {
		this.unkPercent = unkPercent;
	}

	/**
	 * @return the sjmTrials
	 */
	public int getSjmTrials() {
		return sjmTrials;
	}

	/**
	 * @param sjmTrials the sjmTrials to set
	 */
	public void setSjmTrials(int sjmTrials) {
		this.sjmTrials = sjmTrials;
	}

	/**
	 * @return the sjmPerms
	 */
	public int getSjmPerms() {
		return sjmPerms;
	}

	/**
	 * @param sjmPerms the sjmPerms to set
	 */
	public void setSjmPerms(int sjmPerms) {
		this.sjmPerms = sjmPerms;
	}

	/**
	 * @return the bsxTrials
	 */
	public int getBsxTrials() {
		return bsxTrials;
	}

	/**
	 * @param bsxTrials the bsxTrials to set
	 */
	public void setBsxTrials(int bsxTrials) {
		this.bsxTrials = bsxTrials;
	}

	/**
	 * @return the bsxPerms
	 */
	public int getBsxPerms() {
		return bsxPerms;
	}

	/**
	 * @param bsxPerms the bsxPerms to set
	 */
	public void setBsxPerms(int bsxPerms) {
		this.bsxPerms = bsxPerms;
	}

	/**
	 * @return the mdtTrials
	 */
	public int getMdtTrials() {
		return mdtTrials;
	}

	/**
	 * @param mdtTrials the mdtTrials to set
	 */
	public void setMdtTrials(int mdtTrials) {
		this.mdtTrials = mdtTrials;
	}

	/**
	 * @return the mdtPerms
	 */
	public int getMdtPerms() {
		return mdtPerms;
	}

	/**
	 * @return the isAltData
	 */
	public Boolean getIsAltData() {
		return isAltData;
	}

	/**
	 * @param isAltData the isAltData to set
	 */
	public void setIsAltData(Boolean isAltData) {
		this.isAltData = isAltData;
	}

	/**
	 * @param mdtPerms the mdtPerms to set
	 */
	public void setMdtPerms(int mdtPerms) {
		this.mdtPerms = mdtPerms;
	}

	/**
	 * @return the surgeonVO
	 */
	public SurgeonVO getSurgeonVO() {
		return surgeonVO;
	}

	/**
	 * @param surgeonVO the surgeonVO to set
	 */
	public void setSurgeonVO(SurgeonVO surgeonVO) {
		this.surgeonVO = surgeonVO;
	}
	
}
