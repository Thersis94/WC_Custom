package com.depuysynthesinst.lms.client;

// SMTBaseLibs 2
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title: </b>DSIUserDataVO.java <p/>
 * <b>Project: </b>DSI-WS2 <p/>
 * <b>Description: </b>
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

	/**
	 * 
	 */
	private static final long serialVersionUID = 6745777068563527968L;
	private String synthesId;
	private String dsiId;
	private String ttLmsId;
	private String hospital;
	private String specialty;
	private boolean eligible;
	private String profession;
	private boolean verified;
	
	/**
	 * 
	 */
	public DSIUserDataVO() {
		
	}

	/**
	 * @return the synthesId
	 */
	public String getSynthesId() {
		return synthesId;
	}

	/**
	 * @param synthesId the synthesId to set
	 */
	public void setSynthesId(String synthesId) {
		this.synthesId = synthesId;
	}

	/**
	 * @return the dsiId
	 */
	public String getDsiId() {
		return dsiId;
	}

	/**
	 * @param dsiId the dsiId to set
	 */
	public void setDsiId(String dsiId) {
		this.dsiId = dsiId;
	}

	/**
	 * @return the ttLmsId
	 */
	public String getTtLmsId() {
		return ttLmsId;
	}

	/**
	 * @param ttLmsId the ttLmsId to set
	 */
	public void setTtLmsId(String ttLmsId) {
		this.ttLmsId = ttLmsId;
	}

	/**
	 * @return the hospital
	 */
	public String getHospital() {
		return hospital;
	}

	/**
	 * @param hospital the hospital to set
	 */
	public void setHospital(String hospital) {
		this.hospital = hospital;
	}

	/**
	 * @return the specialty
	 */
	public String getSpecialty() {
		return specialty;
	}

	/**
	 * @param specialty the specialty to set
	 */
	public void setSpecialty(String specialty) {
		this.specialty = specialty;
	}

	/**
	 * @return the profession
	 */
	public String getProfession() {
		return profession;
	}

	/**
	 * @param profession the profession to set
	 */
	public void setProfession(String profession) {
		this.profession = profession;
	}

	/**
	 * @return the eligible
	 */
	public boolean isEligible() {
		return eligible;
	}

	/**
	 * @param eligible the eligible to set
	 */
	public void setEligible(boolean eligible) {
		this.eligible = eligible;
	}
	
	public void setEligible(double eligible) {
		this.eligible = Convert.formatBoolean(Double.valueOf(eligible));
	}

	/**
	 * @return the verified
	 */
	public boolean isVerified() {
		return verified;
	}

	/**
	 * @param verified the verified to set
	 */
	public void setVerified(boolean verified) {
		this.verified = verified;
	}

	public void setVerified(double verified) {
		Convert.formatBoolean(Double.valueOf(verified));
	}
}
