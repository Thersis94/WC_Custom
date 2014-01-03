package com.ansmed.sb.physician;

import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.parser.StringEncoder;
import com.smt.sitebuilder.action.SBModuleVO;

/****************************************************************************
 * <b>Title</b>:ActualsVO.java<p/>
 * <b>Description: </b> Contains the physician's actuals data for the previous
 * calendar year.  Quarterly values are summed.  The table column names 
 * "perms_no" and "revisions_no" are contrived table names because they
 * are aggregated from values in two or more columns at the time of the query. 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Apr 27, 2009
 * <b>Changes: </b>
 ****************************************************************************/
public class ActualsVO extends SBModuleVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String actualId = null;
	private String surgeonId = null;
	private String surgeonName = null;
	private String repName = null;
	private String regionId = null;
	private int q1Trials = 0;
	private int q2Trials = 0;
	private int q3Trials = 0;
	private int q4Trials = 0;
	private int totalTrials = 0;
	private int q1Revisions = 0;
	private int q2Revisions = 0;
	private int q3Revisions = 0;
	private int q4Revisions = 0;
	private int totalRevisions = 0;
	private int q1Perms = 0;
	private int q2Perms = 0;
	private int q3Perms = 0;
	private int q4Perms = 0;
	private int totalPerms = 0;
	private float dollars = 0.0f;

		
	/**
	 * 
	 */
	public ActualsVO() {
	}
	
	public ActualsVO(ResultSet rs) {
		super();
		this.setData(rs);
	}
	
	
	public void setData(ResultSet rs) {
		StringEncoder se = new StringEncoder();
		DBUtil db = new DBUtil();
		this.actualId = db.getStringVal("physician_actual_id", rs);
		this.surgeonId = db.getStringVal("surgeon_id", rs);
		this.surgeonName = se.decodeValue(db.getStringVal("physician_nm", rs));
		this.repName = se.decodeValue(db.getStringVal("rep_nm", rs));
		this.regionId = db.getStringVal("region_id", rs);
		addTrials(db.getIntVal("quarter_no", rs), db.getIntVal("trial_no", rs));
		addRevisions(db.getIntVal("quarter_no", rs), db.getIntVal("revisions_no", rs));
		addPerms(db.getIntVal("quarter_no", rs), db.getIntVal("perms_no", rs));
		this.dollars = db.getFloatVal("physician_dollar_no", rs);
		//System.out.println("surgeonId: " + surgeonId);
		///System.out.println("dollars: " + this.dollars);
	}
	
	
	public void addTrials(int quarter, int trials) {
		//System.out.println("quarter/trials: " + quarter + "/" + trials);
		switch(quarter) {
		case 1:
			setQ1Trials(trials);
			break;
		case 2:
			setQ2Trials(trials);
			break;
		case 3:
			setQ3Trials(trials);
			break;
		case 4:
			setQ4Trials(trials);
			break;
		}
		setTotalTrials(trials);
	}
	
	public void addRevisions(int quarter, int revisions) {
		//System.out.println("quarter/revisions: " + quarter + "/" + revisions);
		switch(quarter) {
		case 1:
			setQ1Revisions(revisions);
			break;
		case 2:
			setQ2Revisions(revisions);
			break;
		case 3:
			setQ3Revisions(revisions);
			break;
		case 4:
			setQ4Revisions(revisions);
			break;
		}
		setTotalRevisions(revisions);
	}
	
	public void addPerms(int quarter, int perms) {
		//System.out.println("quarter/perms: " + quarter + "/" + perms);
		switch(quarter) {
		case 1:
			setQ1Perms(perms);
			break;
		case 2:
			setQ2Perms(perms);
			break;
		case 3:
			setQ3Perms(perms);
			break;
		case 4:
			setQ4Perms(perms);
			break;
		}
		setTotalPerms(perms);
		
	}
	
	/**
	 * @return the actualId
	 */
	public String getActualId() {
		return actualId;
	}


	/**
	 * @param actualId the actualId to set
	 */
	public void setActualId(String actualId) {
		this.actualId = actualId;
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
	 * @return the repName
	 */
	public String getRepName() {
		return repName;
	}

	/**
	 * @param repName the repName to set
	 */
	public void setRepName(String repName) {
		this.repName = repName;
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
	 * @return the regionId
	 */
	public String getRegionId() {
		return regionId;
	}


	/**
	 * @param regionId the regionId to set
	 */
	public void setRegionId(String regionId) {
		this.regionId = regionId;
	}


	/**
	 * @return the q1Trials
	 */
	public int getQ1Trials() {
		return q1Trials;
	}


	/**
	 * @param trials the q1Trials to set
	 */
	public void setQ1Trials(int trials) {
		q1Trials = q1Trials + trials;
	}


	/**
	 * @return the q2Trials
	 */
	public int getQ2Trials() {
		return q2Trials;
	}


	/**
	 * @param trials the q2Trials to set
	 */
	public void setQ2Trials(int trials) {
		q2Trials = q2Trials + trials;
	}


	/**
	 * @return the q3Trials
	 */
	public int getQ3Trials() {
		return q3Trials;
	}


	/**
	 * @param trials the q3Trials to set
	 */
	public void setQ3Trials(int trials) {
		q3Trials = q3Trials + trials;
	}


	/**
	 * @return the q4Trials
	 */
	public int getQ4Trials() {
		return q4Trials;
	}


	/**
	 * @param trials the q4Trials to set
	 */
	public void setQ4Trials(int trials) {
		q4Trials = q4Trials + trials;
	}


	/**
	 * @return the totalTrials
	 */
	public int getTotalTrials() {
		return totalTrials;
	}


	/**
	 * @param totalTrials the totalTrials to set
	 */
	public void setTotalTrials(int totalTrials) {
		this.totalTrials = this.totalTrials + totalTrials;
	}


	/**
	 * @return the q1Revisions
	 */
	public int getQ1Revisions() {
		return q1Revisions;
	}


	/**
	 * @param revisions the q1Revisions to set
	 */
	public void setQ1Revisions(int revisions) {
		q1Revisions = q1Revisions + revisions;
	}


	/**
	 * @return the q2Revisions
	 */
	public int getQ2Revisions() {
		return q2Revisions;
	}


	/**
	 * @param revisions the q2Revisions to set
	 */
	public void setQ2Revisions(int revisions) {
		q2Revisions = q2Revisions + revisions;
	}


	/**
	 * @return the q3Revisions
	 */
	public int getQ3Revisions() {
		return q3Revisions;
	}


	/**
	 * @param revisions the q3Revisions to set
	 */
	public void setQ3Revisions(int revisions) {
		q3Revisions = q3Revisions + revisions;
	}


	/**
	 * @return the q4Revisions
	 */
	public int getQ4Revisions() {
		return q4Revisions;
	}


	/**
	 * @param revisions the q4Revisions to set
	 */
	public void setQ4Revisions(int revisions) {
		q4Revisions = q4Revisions + revisions;
	}


	/**
	 * @return the totalRevisions
	 */
	public int getTotalRevisions() {
		return totalRevisions;
	}


	/**
	 * @param totalRevisions the totalRevisions to set
	 */
	public void setTotalRevisions(int totalRevisions) {
		this.totalRevisions = this.totalRevisions + totalRevisions;
	}


	/**
	 * @return the q1Perms
	 */
	public int getQ1Perms() {
		return q1Perms;
	}


	/**
	 * @param perms the q1Perms to set
	 */
	public void setQ1Perms(int perms) {
		q1Perms = q1Perms + perms;
	}


	/**
	 * @return the q2Perms
	 */
	public int getQ2Perms() {
		return q2Perms;
	}


	/**
	 * @param perms the q2Perms to set
	 */
	public void setQ2Perms(int perms) {
		q2Perms = q2Perms + perms;
	}


	/**
	 * @return the q3Perms
	 */
	public int getQ3Perms() {
		return q3Perms;
	}


	/**
	 * @param perms the q3Perms to set
	 */
	public void setQ3Perms(int perms) {
		q3Perms = q3Perms + perms;
	}


	/**
	 * @return the q4Perms
	 */
	public int getQ4Perms() {
		return q4Perms;
	}


	/**
	 * @param perms the q4Perms to set
	 */
	public void setQ4Perms(int perms) {
		q4Perms = q4Perms + perms;
	}


	/**
	 * @return the totalPerms
	 */
	public int getTotalPerms() {
		return totalPerms;
	}


	/**
	 * @param totalPerms the totalPerms to set
	 */
	public void setTotalPerms(int totalPerms) {
		this.totalPerms = this.totalPerms + totalPerms;
	}


	/**
	 * @return the dollars
	 */
	public float getDollars() {
		return dollars;
	}


	/**
	 * @param dollars the dollars to set
	 */
	public void setDollars(float dollars) {
		this.dollars = dollars;
	}

}
