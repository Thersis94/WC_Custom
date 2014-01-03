package com.ansmed.sb.patient;

import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.security.StringEncrypter;
import com.smt.sitebuilder.action.SBModuleVO;

/****************************************************************************
 * <b>Title</b>:PatientVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Feb 18, 2009
 * <b>Changes: </b>
 ****************************************************************************/
public class PatientVO extends SBModuleVO {
	private static final long serialVersionUID = 1L;
	private String stimTrackerId = null;
	private String firstName = null;
	private String lastName = null;
	private String phoneNumber = null;
	private String surgicalType = null;
	private String region = null;
	private String surgeonId = null;
	private String surgeonName = null;
	private String entryLead = null;
	private String finalLead = null;
	private String patientStatus = null;
	private String trialFacility = null;
	private String permFacility = null;
	private String insurance = null;
	private String comments = null;
	private String referringPhys = null;
	private String otherPhys = null;
	
	private Date prepVideoDate = null;
	private Date patientEduationDate = null;
	private Date trialDate = null;
	private Date trialRemovalDate = null;
	private Date permConsultDate = null;
	private Date permDate = null;
	
	/**
	 * 
	 */
	public PatientVO() {
		super();
	}
	
	
	public void setData(ResultSet rs, String enckey) {

		try {
			StringEncrypter se = new StringEncrypter(enckey);
			DBUtil db = new DBUtil();
			firstName = se.decrypt(db.getStringVal("patient_first_nm", rs));
			lastName = se.decrypt(db.getStringVal("patient_last_nm", rs));
			phoneNumber = db.getStringVal("patient_phone_no", rs);
			surgeonName = db.getStringVal("first_nm", rs) + " " +  db.getStringVal("last_nm", rs);
			region = db.getStringVal("region_nm", rs);
			stimTrackerId = db.getStringVal("stim_tracker_id", rs);
			
			surgeonId = db.getStringVal("surgeon_id", rs);
			entryLead = db.getStringVal("entry_lead_txt", rs);
			finalLead = db.getStringVal("final_lead_txt", rs);
			patientStatus = db.getStringVal("procedure_type_txt", rs);
			trialFacility = db.getStringVal("trial_facility_txt", rs);
			permFacility = db.getStringVal("perm_facility_txt", rs);
			comments = db.getStringVal("comments_txt", rs);
			this.createDate = db.getDateVal("create_dt", rs);
			this.updateDate = db.getDateVal("update_dt", rs);
			insurance = db.getStringVal("insurance_txt", rs);
			prepVideoDate = db.getDateVal("prep_video_dt", rs);
			patientEduationDate = db.getDateVal("education_dt", rs);
			trialDate = db.getDateVal("trial_dt", rs);
			trialRemovalDate = db.getDateVal("trial_removal_dt", rs);
			permConsultDate = db.getDateVal("perm_consult_dt", rs);
			permDate = db.getDateVal("perm_dt", rs);
			referringPhys = db.getStringVal("refer_phys_nm", rs);
			otherPhys = db.getStringVal("other_refer_nm", rs);
		} catch(Exception e) {
			e.printStackTrace();
			return;
		}
	}

	/**
	 * @return the firstName
	 */
	public String getFirstName() {
		return firstName;
	}

	/**
	 * @param firstName the firstName to set
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	/**
	 * @return the lastName
	 */
	public String getLastName() {
		return lastName;
	}

	/**
	 * @param lastName the lastName to set
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	/**
	 * @return the phoneNumber
	 */
	public String getPhoneNumber() {
		return phoneNumber;
	}

	/**
	 * @param phoneNumber the phoneNumber to set
	 */
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}


	/**
	 * @return the surgicalType
	 */
	public String getSurgicalType() {
		return surgicalType;
	}


	/**
	 * @param surgicalType the surgicalType to set
	 */
	public void setSurgicalType(String surgicalType) {
		this.surgicalType = surgicalType;
	}


	/**
	 * @return the region
	 */
	public String getRegion() {
		return region;
	}


	/**
	 * @param region the region to set
	 */
	public void setRegion(String region) {
		this.region = region;
	}


	/**
	 * @return the surgeonId
	 */
	public String getSurgeonId() {
		return surgeonId;
	}


	/**
	 * @param surgeonId the surgeonId to set
	 */
	public void setSurgeonId(String surgeonId) {
		this.surgeonId = surgeonId;
	}


	/**
	 * @return the surgeonName
	 */
	public String getSurgeonName() {
		return surgeonName;
	}


	/**
	 * @param surgeonName the surgeonName to set
	 */
	public void setSurgeonName(String surgeonName) {
		this.surgeonName = surgeonName;
	}


	/**
	 * @return the prepVideoDate
	 */
	public Date getPrepVideoDate() {
		return prepVideoDate;
	}


	/**
	 * @param prepVideoDate the prepVideoDate to set
	 */
	public void setPrepVideoDate(Date prepVideoDate) {
		this.prepVideoDate = prepVideoDate;
	}


	/**
	 * @return the patientEduationDate
	 */
	public Date getPatientEduationDate() {
		return patientEduationDate;
	}


	/**
	 * @param patientEduationDate the patientEduationDate to set
	 */
	public void setPatientEduationDate(Date patientEduationDate) {
		this.patientEduationDate = patientEduationDate;
	}


	/**
	 * @return the trialDate
	 */
	public Date getTrialDate() {
		return trialDate;
	}


	/**
	 * @param trialDate the trialDate to set
	 */
	public void setTrialDate(Date trialDate) {
		this.trialDate = trialDate;
	}


	/**
	 * @return the trialRemovalDate
	 */
	public Date getTrialRemovalDate() {
		return trialRemovalDate;
	}


	/**
	 * @param trialRemovalDate the trialRemovalDate to set
	 */
	public void setTrialRemovalDate(Date trialRemovalDate) {
		this.trialRemovalDate = trialRemovalDate;
	}


	/**
	 * @return the permConsultDate
	 */
	public Date getPermConsultDate() {
		return permConsultDate;
	}


	/**
	 * @param permConsultDate the permConsultDate to set
	 */
	public void setPermConsultDate(Date permConsultDate) {
		this.permConsultDate = permConsultDate;
	}

	/**
	 * @return the patientStatus
	 */
	public String getPatientStatus() {
		return patientStatus;
	}


	/**
	 * @param patientStatus the patientStatus to set
	 */
	public void setPatientStatus(String patientStatus) {
		this.patientStatus = patientStatus;
	}
	
	/**
	 * @return the insurance
	 */
	public String getInsurance() {
		return insurance;
	}


	/**
	 * @param insurance the insurance to set
	 */
	public void setInsurance(String insurance) {
		this.insurance = insurance;
	}


	/**
	 * @return the comments
	 */
	public String getComments() {
		return comments;
	}


	/**
	 * @param comments the comments to set
	 */
	public void setComments(String comments) {
		this.comments = comments;
	}


	/**
	 * @return the stimTrackerId
	 */
	public String getStimTrackerId() {
		return stimTrackerId;
	}


	/**
	 * @param stimTrackerId the stimTrackerId to set
	 */
	public void setStimTrackerId(String stimTrackerId) {
		this.stimTrackerId = stimTrackerId;
	}


	/**
	 * @return the permDate
	 */
	public Date getPermDate() {
		return permDate;
	}


	/**
	 * @param permDate the permDate to set
	 */
	public void setPermDate(Date permDate) {
		this.permDate = permDate;
	}
	
	/**
	 * @return the entryLead
	 */
	public String getEntryLead() {
		return entryLead;
	}


	/**
	 * @param entryLead the entryLead to set
	 */
	public void setEntryLead(String entryLead) {
		this.entryLead = entryLead;
	}


	/**
	 * @return the finalLead
	 */
	public String getFinalLead() {
		return finalLead;
	}


	/**
	 * @param finalLead the finalLead to set
	 */
	public void setFinalLead(String finalLead) {
		this.finalLead = finalLead;
	}


	/**
	 * @return the trialFacility
	 */
	public String getTrialFacility() {
		return trialFacility;
	}


	/**
	 * @param trialFacility the trialFacility to set
	 */
	public void setTrialFacility(String trialFacility) {
		this.trialFacility = trialFacility;
	}


	/**
	 * @return the permFacility
	 */
	public String getPermFacility() {
		return permFacility;
	}


	/**
	 * @param permFacility the permFacility to set
	 */
	public void setPermFacility(String permFacility) {
		this.permFacility = permFacility;
	}


	/**
	 * @return the referringPhys
	 */
	public String getReferringPhys() {
		return referringPhys;
	}


	/**
	 * @param referringPhys the referringPhys to set
	 */
	public void setReferringPhys(String referringPhys) {
		this.referringPhys = referringPhys;
	}


	/**
	 * @return the otherPhys
	 */
	public String getOtherPhys() {
		return otherPhys;
	}


	/**
	 * @param otherPhys the otherPhys to set
	 */
	public void setOtherPhys(String otherPhys) {
		this.otherPhys = otherPhys;
	}

	
}
