/**
 *
 */
package com.biomed.smarttrak.vo;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.data.Node;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: GapCompanyVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> VO Manages all the data for a Gap Company Record.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Feb 6, 2017
 ****************************************************************************/
public class GapCompanyVO {

	//Region Codes
	public static final String US = "US";
	public static final String OUS = "OUS";

	public enum StatusClass {
		APPROVED(StatusVal.USA, StatusVal.OUSA),
		DISCONTINUED(StatusVal.USD, StatusVal.OUSD),
		IN_DEVELOPMENT(StatusVal.USID, StatusVal.OUSID),
		NO_PRODUCT(StatusVal.USG, StatusVal.OUSG);

		private StatusVal us;
		private StatusVal intl;
		StatusClass(StatusVal us, StatusVal intl) {
			this.us = us;
			this.intl = intl;
		}

		public StatusVal getStatusVal(String region) {
			if(US.contentEquals(region)) {
				return us;
			} else if(OUS.equals(region)) {
				return intl;
			} else {
				return null;
			}
		}
	}

	//Status Codes
	public enum StatusVal { USA(10), OUSA(10), USID(5), OUSID(5), USD(2), OUSD(2), USG(0), OUSG(0);

		//Score Weight for given Status.
		private int score;
		StatusVal(int score) {
			this.score = score;
		}
		public int getScore() {
			return score;
		}
		public String getClassName() {
			return this.toString().toLowerCase();
		}
	}

	private Map<String, StatusVal> regulations;
	private List<GapCellVO> cells;
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

		//If portfolioNo is -1, attempt to summarize regulations scores.
		if(portfolioNo == -1) {
			for(StatusVal s : this.regulations.values()) {
				portfolioNo += s.getScore();
			}
		}

		return portfolioNo;
	}

	/**
	 * Add a regulation status for a given column, statusId and Region.
	 * @param columnId
	 * @param statusId
	 * @param regionId
	 */
	public void addRegulation(String columnId, String statusTxt, int regionId) {

		//Generate a Region based Column Key.
		String rKey = regionId == 1 ? US : OUS;
		String regKey = columnId + "-" + rKey;

		//Check if a Status has already been calculated.
		StatusVal status = regulations.get(regKey);

		//Retrieve Status
		StatusVal tStatus = getStatus(statusTxt, rKey);

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
	public StatusVal getStatus(String statusTxt, String rKey) {
		if(!StringUtil.isEmpty(statusTxt)) {
			try {
				return StatusClass.valueOf(statusTxt).getStatusVal(rKey);
			} catch(Exception e) {
				return getGapStatus(rKey);
			}
		}
		return getGapStatus(rKey);
	}

	/**
	 * Helper method that gets a Gap Status for the given Region.
	 * @param rKey
	 * @return
	 */
	public StatusVal getGapStatus(String rKey) {
		if(US.equals(rKey)) {
			return StatusVal.USG;
		} else {
			return StatusVal.OUSG;
		}
	}

	/**
	 * Method returns Regulation Status for given colRegId.
	 * @param colRegId ga_column_id-regionName
	 * @return
	 */
	public StatusVal getRegulation(String colRegId) {
		StatusVal s = regulations.get(colRegId);
		if(s == null && US.equals(colRegId.split("-")[1])) {
			s = StatusVal.USG;
		} else if(s == null && OUS.equals(colRegId.split("-")[1])) {
			s = StatusVal.OUSG;
		}
		return s;
	}

	/**
	 * Build Cell Data and Store on the Company Row.
	 * @param columns
	 */
	public void buildCellData(Collection<Node> columns) {
		cells = new ArrayList<>();
		for(Node col : columns) {
			StatusVal usReg = this.getRegulation(col.getNodeId() + "-US");
			StatusVal ousReg = this.getRegulation(col.getNodeId() + "-OUS");

			cells.add(new GapCellVO(usReg, ousReg, col.getNodeId()));
		}
	}

	/**
	 * @return the cells
	 */
	public List<GapCellVO> getCells() {
		return cells;
	}

	/**
	 * @param regulations the regulations to set.
	 */
	public void setRegulations(Map<String, StatusVal> regulations) {
		this.regulations = regulations;
	}

	/**
	 * @param cells the cells to set.
	 */
	public void setCells(List<GapCellVO> cells) {
		this.cells = cells;
	}

	/**
	 * @param portfolioNo the portfolioNo to set.
	 */
	public void setPortfolioNo(int portfolioNo) {
		this.portfolioNo = portfolioNo;
	}
}