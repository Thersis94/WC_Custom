package com.depuysynthes.locator;

import java.io.Serializable;

/*****************************************************************************
 <b>Title: </b>EducationBean.java
 <b>Project: </b>
 <b>Description: </b>
 <b>Copyright: </b>(c) 2000 - 2018 SMT, All Rights Reserved
 <b>Company: Silicon Mountain Technologies</b>
 @author cobalt
 @version 1.0
 @since May 17, 2018
 <b>Changes:</b> 
 ***************************************************************************/
public class EducationBean implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -378604108565694870L;
	private int surgeonId;
	private String educationText;
	private String educationCityName;
	private String educationStateCode;
	private int yearStart;
	private int yearEnd;
	
	/**
	 * @return the surgeonId
	 */
	public int getSurgeonId() {
		return surgeonId;
	}

	/**
	 * @param surgeonId the surgeonId to set
	 */
	public void setSurgeonId(int surgeonId) {
		this.surgeonId = surgeonId;
	}

	/**
	 * @return the educationText
	 */
	public String getEducationText() {
		return educationText;
	}

	/**
	 * @param educationText the educationText to set
	 */
	public void setEducationText(String educationText) {
		this.educationText = educationText;
	}

	/**
	 * @return the educationCityName
	 */
	public String getEducationCityName() {
		return educationCityName;
	}

	/**
	 * @param educationCityName the educationCityName to set
	 */
	public void setEducationCityName(String educationCityName) {
		this.educationCityName = educationCityName;
	}

	/**
	 * @return the educationStateCode
	 */
	public String getEducationStateCode() {
		return educationStateCode;
	}

	/**
	 * @param educationStateCode the educationStateCode to set
	 */
	public void setEducationStateCode(String educationStateCode) {
		this.educationStateCode = educationStateCode;
	}

	/**
	 * @return the yearStart
	 */
	public int getYearStart() {
		return yearStart;
	}

	/**
	 * @param yearStart the yearStart to set
	 */
	public void setYearStart(int yearStart) {
		this.yearStart = yearStart;
	}

	/**
	 * @return the yearEnd
	 */
	public int getYearEnd() {
		return yearEnd;
	}

	/**
	 * @param yearEnd the yearEnd to set
	 */
	public void setYearEnd(int yearEnd) {
		this.yearEnd = yearEnd;
	}

}
