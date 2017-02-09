/**
 *
 */
package com.biomed.smarttrak.vo;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.db.DBUtil;

/****************************************************************************
 * <b>Title</b>: GapCompanyVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> VO Manages all the data for a Gap Company Record.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author raptor
 * @version 1.0
 * @since Feb 6, 2017
 ****************************************************************************/
public class GapCompanyVO {

	//Region Codes
	public static final String US = "US";
	public static final String OUS = "OUS";

	//Status Codes
	public enum StatusVal { usa(10), ousa(10), usid(5), ousid(5), usd(2), ousd(2), usg(0), ousg(0);

		//Score Weight for given Status.
		private int score;
		StatusVal(int score) {
			this.score = score;
		}
		public int getScore() {
			return score;
		}
	};
	private Map<String, StatusVal> regulations;
	private String companyName;
	private String shortCompanyName;
	private String companyId;
	private int portfolioNo = -1;

	public GapCompanyVO() {
		regulations = new HashMap<>();
	}

	public GapCompanyVO(ResultSet rs) {
		this();
		setData(rs);
	}

	/**
	 * Helper method set data off ResultSet.
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		companyName = db.getStringVal("company_nm", rs);
		companyId = db.getStringVal("company_id", rs);
		shortCompanyName = db.getStringVal("short_nm_txt", rs);
	}

	/**
	 * Get regulation status's
	 * @return the regulations
	 */
	public Map<String, StatusVal> getRegulations() {
		return regulations;
	}

	/**
	 * Get company name
	 * @return the companyName
	 */
	public String getCompanyName() {
		return companyName;
	}

	/**
	 * Get short company name
	 * @return the shortCompanyName
	 */
	public String getShortCompanyName() {
		return shortCompanyName;
	}

	/**
	 * Get company id
	 * @return the companyId
	 */
	public String getCompanyId() {
		return companyId;
	}

	/**
	 * Set company name
	 * @param companyName the companyName to set.
	 */
	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	/**
	 * Set short company name
	 * @param shortCompanyName the shortCompanyName to set.
	 */
	public void setShortCompanyName(String shortCompanyName) {
		this.shortCompanyName = shortCompanyName;
	}

	/**
	 * Set company id
	 * @param companyId the companyId to set.
	 */
	public void setCompanyId(String companyId) {
		this.companyId = companyId;
	}

	/**
	 * Get portfolio no for determining company relevance.
	 * @return
	 */
	public int getPortfolioNo() {
		return this.portfolioNo;
	}

	/**
	 * Add a regulation status for a given column, statusId and Region.
	 * @param columnId
	 * @param statusId
	 * @param regionId
	 */
	public void addRegulation(String columnId, int statusId, int regionId) {

		//Generate a Region based Column Key.
		String rKey = regionId == 1 ? US : OUS;
		String regKey = columnId + "-" + rKey;

		//Check if a Status has already been calculated.
		StatusVal status = regulations.get(regKey);

		//Retrieve Status
		StatusVal tStatus = getStatus(statusId, rKey);

		/*
		 * Increment portfolio score for each regulation found.  This ensures
		 * multiple products under the same column get counted.
		 */
		portfolioNo += tStatus.getScore();

		//If this is a better status, update.
		if(status == null || tStatus.getScore() > status.getScore()) {
			regulations.put(regKey, tStatus);
		}
	}

	/**
	 * Helper method that returns StatusVal appropriate for given statusId and
	 * region Key.
	 * @param statusId
	 * @param rKey
	 * @return
	 */
	public StatusVal getStatus(int statusId, String rKey) {
		StatusVal tStatus;
		switch(statusId) {

			//Discontinued Status.
			case 10:
			case 21:
			case 26:
			case 42:
				if(US.equals(rKey)) {
					tStatus = StatusVal.usd;
				} else {
					tStatus = StatusVal.ousd;
				}
				break;

			//Approved Status.
			case 9:
			case 12:
			case 13:
			case 14:
			case 43:
				if(US.equals(rKey)) {
					tStatus = StatusVal.usa;
				} else {
					tStatus = StatusVal.ousa;
				}
				break;

			//In Development Status
			case 5:
			case 6:
			case 7:
			case 8:
			case 11:
			case 15:
			case 17:
			case 18:
			case 22:
			case 23:
			case 24:
			case 25:
			case 27:
			case 28:
			case 29:
			case 30:
			case 31:
			case 32:
			case 33:
			case 34:
			case 35:
			case 36:
			case 37:
			case 40:
				if(US.equals(rKey)) {
					tStatus = StatusVal.usid;
				} else {
					tStatus = StatusVal.ousid;
				}
				break;

			//Default Gap Status.
			default:
				if(US.equals(rKey)) {
					tStatus = StatusVal.usg;
				} else {
					tStatus = StatusVal.ousg;
				}
				break;
		}

		return tStatus;
	}

	/**
	 * Method returns Regulation Status for given colRegId.
	 * @param colRegId ga_column_id-regionName
	 * @return
	 */
	public StatusVal getRegulation(String colRegId) {
		return regulations.get(colRegId);
	}
}