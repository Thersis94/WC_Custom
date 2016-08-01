package com.depuysynthesinst.lms;

import java.io.Serializable;

import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: MyLMSCourseVO.java<p/>
 * <b>Description: Represents a JSON response from the LMS for TTLMS-USR-LOGIN SOAP call.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 20, 2015
 ****************************************************************************/
public class MyLMSCourseVO implements Serializable {

	private static final long serialVersionUID = 6773908665240584811L;
	private int C_ID;
	private String C_NAME;
	private String C_DESCRIPTION;
	private double INDEVELOPMENT;
	private boolean COURSECOMPLETE;
	private String BODYREGION;
	private int POINTS;
	private boolean FORPOINTS; 
	private int POINTSEARNABLE;
	private String COURSECOMPLETEDATE;
	
	public MyLMSCourseVO() {
	}

	public int getId() {
		return C_ID;
	}

	public String getName() {
		return C_NAME;
	}

	public String getDescription() {
		return C_DESCRIPTION;
	}

	public boolean isInDevelopment() {
		return INDEVELOPMENT > 0;
	}

	public String getBODYREGION() {
		return BODYREGION;
	}

	public int getPOINTS() {
		return POINTS;
	}

	public boolean isForPoints() {
		return FORPOINTS;
	}

	public int getPointsEarnable() {
		return POINTSEARNABLE;
	}

	public String getCOURSECOMPLETEDATE() {
		return COURSECOMPLETEDATE;
	}

	public void setC_ID(int c_ID) {
		C_ID = c_ID;
	}

	public void setC_NAME(String c_NAME) {
		C_NAME = c_NAME;
	}

	public void setC_DESCRIPTION(String c_DESCRIPTION) {
		C_DESCRIPTION = c_DESCRIPTION;
	}

	public void setINDEVELOPMENT(double iNDEVELOPMENT) {
		INDEVELOPMENT = iNDEVELOPMENT;
	}

	public void setBODYREGION(String bODYREGION) {
		BODYREGION = bODYREGION;
	}

	public void setPOINTS(int pOINTS) {
		POINTS = pOINTS;
	}

	public void setFORPOINTS(boolean fORPOINTS) {
		FORPOINTS = fORPOINTS;
	}

	public void setPOINTSEARNABLE(int pOINTSEARNABLE) {
		POINTSEARNABLE = pOINTSEARNABLE;
	}

	public boolean isComplete() {
		return COURSECOMPLETE;
	}

	public void setCOURSECOMPLETE(boolean cOURSECOMPLETE) {
		COURSECOMPLETE = cOURSECOMPLETE;
	}

	public void setCOURSECOMPLETEDATE(String cOURSECOMPLETEDATE) {
		this.COURSECOMPLETEDATE = cOURSECOMPLETEDATE;
	}

	public String toString() {
		return StringUtil.getToString(this);
	}
}
