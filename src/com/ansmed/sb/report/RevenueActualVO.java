package com.ansmed.sb.report;

import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBModuleVO;

import com.ansmed.sb.physician.ActualsVO;

/****************************************************************************
 * <b>Title</b>:RevenueActualVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Jun 17, 2009
 * <b>Changes: </b>
 ****************************************************************************/
public class RevenueActualVO extends SBModuleVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String surgeonId = null;
	private String repFirstName = null;
	private String repLastName = null;
	private String surgeonFirstName = null;
	private String surgeonLastName = null;
	private String salesRepId = null;
	private String titleNm = null;
	private int trialQ1 = 0;
	private int trialQ2 = 0;
	private int trialQ3 = 0;
	private int trialQ4 = 0;
	private int permQ1 = 0;
	private int permQ2 = 0;
	private int permQ3 = 0;
	private int permQ4 = 0;
	private int totalTrials = 0;
	private int totalPerms = 0;
	private int forecastDollars = 0;
	private float percentGrowth = 0.0f;
	private ActualsVO actualsData = null;
	private int rank = 0;
		
	/**
	 * 
	 */
	public RevenueActualVO() {
		actualsData = new ActualsVO();
	}
	
	/**
	 * 
	 * @param rs
	 */
	public RevenueActualVO(ResultSet rs) {
		super();
		actualsData = new ActualsVO();
		setData(rs);
	}
	
	public void setData(ResultSet rs) {
		StringEncoder se = new StringEncoder();
		DBUtil db = new DBUtil();
		surgeonId = db.getStringVal("surgeon_id", rs);
		repFirstName = se.decodeValue(db.getStringVal("rep_first_nm", rs));
		repLastName = se.decodeValue(db.getStringVal("rep_last_nm", rs));
		salesRepId = db.getStringVal("sales_rep_id", rs);
		surgeonFirstName = se.decodeValue(db.getStringVal("phys_first_nm", rs));
		surgeonLastName =  se.decodeValue(db.getStringVal("phys_last_nm", rs));
		titleNm = db.getStringVal("title_nm", rs);
		rank = db.getIntVal("rank_no", rs);
		addPlanData(db.getStringVal("business_plan_id", rs),db.getStringVal("value_txt", rs));
		
	}
	
	public void addPlanData(String field, String value) {
		field = StringUtil.checkVal(field);
		if (field.equalsIgnoreCase("physAnlRevGoal")) {
			// Sanitize the string before converting to double.
			value = StringEncoder.removeNonAlphaNumeric(value);
			value = StringUtil.replace(value, "k","000");
			this.setForecastDollars(Convert.formatInteger(value).intValue());
		} else {
			int val = Convert.formatInteger(value).intValue();
			if (field.equalsIgnoreCase("trialQ1")) {
				this.setTrialQ1(val);
				this.totalTrials = this.totalTrials + val;
			} else if (field.equalsIgnoreCase("trialQ2")) {
				this.setTrialQ2(val);
				this.totalTrials = this.totalTrials + val;
			} else if (field.equalsIgnoreCase("trialQ3")) {
				this.setTrialQ3(val);
				this.totalTrials = this.totalTrials + val;
			} else if (field.equalsIgnoreCase("trialQ4")) {
				this.setTrialQ4(val);
				this.totalTrials = this.totalTrials + val;
			} else if (field.equalsIgnoreCase("permQ1")) {
				this.setPermQ1(val);
				this.totalPerms = this.totalPerms + val;
			} else if (field.equalsIgnoreCase("permQ2")) {
				this.setPermQ2(val);
				this.totalPerms = this.totalPerms + val;
			} else if (field.equalsIgnoreCase("permQ3")) {
				this.setPermQ3(val);
				this.totalPerms = this.totalPerms + val;
			} else if (field.equalsIgnoreCase("permQ4")) {
				this.setPermQ4(val);
				this.totalPerms = this.totalPerms + val;
			}
		}
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
	 * @return the salesRepId
	 */
	public String getSalesRepId() {
		return salesRepId;
	}

	/**
	 * @param salesRepId the salesRepId to set
	 */
	public void setSalesRepId(String salesRepId) {
		this.salesRepId = salesRepId;
	}

	/**
	 * @return the surgeonFirstName
	 */
	public String getSurgeonFirstName() {
		return surgeonFirstName;
	}

	/**
	 * @param surgeonFirstName the surgeonFirstName to set
	 */
	public void setSurgeonFirstName(String surgeonFirstName) {
		this.surgeonFirstName = surgeonFirstName;
	}

	/**
	 * @return the surgeonLastName
	 */
	public String getSurgeonLastName() {
		return surgeonLastName;
	}

	/**
	 * @param surgeonLastName the surgeonLastName to set
	 */
	public void setSurgeonLastName(String surgeonLastName) {
		this.surgeonLastName = surgeonLastName;
	}
	
	/**
	 * @return the repLastName
	 */
	public String getRepLastName() {
		return repLastName;
	}


	/**
	 * @param repLastName the repLastName to set
	 */
	public void setRepLastName(String repLastName) {
		this.repLastName = repLastName;
	}


	/**
	 * @return the titleNm
	 */
	public String getTitleNm() {
		return titleNm;
	}


	/**
	 * @param titleNm the titleNm to set
	 */
	public void setTitleNm(String titleNm) {
		this.titleNm = titleNm;
	}
	
	/**
	 * @return the rank
	 */
	public int getRank() {
		return rank;
	}

	/**
	 * @param rank the rank to set
	 */
	public void setRank(int rank) {
		this.rank = rank;
	}


	/**
	 * @return the repFirstName
	 */
	public String getRepFirstName() {
		return repFirstName;
	}


	/**
	 * @param repFirstName the repFirstName to set
	 */
	public void setRepFirstName(String repFirstName) {
		this.repFirstName = repFirstName;
	}

	/**
	 * @return the actualsData
	 */
	public ActualsVO getActualsData() {
		return actualsData;
	}


	/**
	 * @param actualsData the actualsData to set
	 */
	public void setActualsData(ActualsVO actualsData) {
		this.actualsData = actualsData;
	}

	/**
	 * @return the trialQ1
	 */
	public int getTrialQ1() {
		return trialQ1;
	}

	/**
	 * @param trialQ1 the trialQ1 to set
	 */
	public void setTrialQ1(int trialQ1) {
		this.trialQ1 = trialQ1;
	}

	/**
	 * @return the trialQ2
	 */
	public int getTrialQ2() {
		return trialQ2;
	}

	/**
	 * @param trialQ2 the trialQ2 to set
	 */
	public void setTrialQ2(int trialQ2) {
		this.trialQ2 = trialQ2;
	}

	/**
	 * @return the trialQ3
	 */
	public int getTrialQ3() {
		return trialQ3;
	}

	/**
	 * @param trialQ3 the trialQ3 to set
	 */
	public void setTrialQ3(int trialQ3) {
		this.trialQ3 = trialQ3;
	}

	/**
	 * @return the trialQ4
	 */
	public int getTrialQ4() {
		return trialQ4;
	}

	/**
	 * @param trialQ4 the trialQ4 to set
	 */
	public void setTrialQ4(int trialQ4) {
		this.trialQ4 = trialQ4;
	}

	/**
	 * @return the permQ1
	 */
	public int getPermQ1() {
		return permQ1;
	}

	/**
	 * @param permQ1 the permQ1 to set
	 */
	public void setPermQ1(int permQ1) {
		this.permQ1 = permQ1;
	}

	/**
	 * @return the permQ2
	 */
	public int getPermQ2() {
		return permQ2;
	}

	/**
	 * @param permQ2 the permQ2 to set
	 */
	public void setPermQ2(int permQ2) {
		this.permQ2 = permQ2;
	}

	/**
	 * @return the permQ3
	 */
	public int getPermQ3() {
		return permQ3;
	}

	/**
	 * @param permQ3 the permQ3 to set
	 */
	public void setPermQ3(int permQ3) {
		this.permQ3 = permQ3;
	}

	/**
	 * @return the permQ4
	 */
	public int getPermQ4() {
		return permQ4;
	}

	/**
	 * @param permQ4 the permQ4 to set
	 */
	public void setPermQ4(int permQ4) {
		this.permQ4 = permQ4;
	}

	/**
	 * @return the forecastDollars
	 */
	public int getForecastDollars() {
		return forecastDollars;
	}

	/**
	 * @param forecastDollars the forecastDollars to set
	 */
	public void setForecastDollars(int forecastDollars) {
		this.forecastDollars = forecastDollars;
	}

	/**
	 * @return the percentGrowth
	 */
	public float getPercentGrowth() {
		return percentGrowth;
	}

	/**
	 * @param percentGrowth the percentGrowth to set
	 */
	public void setPercentGrowth(float percentGrowth) {
		this.percentGrowth = percentGrowth;
	}
	
	/**
	 * 
	 * @return
	 */
	public float getPercentGrowthFromActuals() {
		
		float percentage = 0.0f;
		if (this.actualsData == null) return percentage;
		
		ActualsVO av = null;
		av = this.actualsData;
		int prevYr = av.getTotalPerms();
		int currYr = this.getTotalPerms();
		
		if (prevYr > 0) {
			if (prevYr > currYr) {
				// Percentage decrease
				percentage = -(prevYr - currYr)/prevYr;
			} else {
				// Percentage increase
				percentage = (currYr - prevYr)/prevYr;
			}
			return 0.0f;
		} else {
			return percentage;	
		}
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
		this.totalTrials = totalTrials;
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
		this.totalPerms = totalPerms;
	}

}
