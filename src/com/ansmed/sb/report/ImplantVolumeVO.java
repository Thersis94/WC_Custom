package com.ansmed.sb.report;

import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSiteBuilderVO;

/*****************************************************************************
 <p><b>Title</b>: ImplantVolumeVO.java</p>
 <p>Description: <b/></p>
 <p>Copyright: Copyright (c) 2000 - 2009 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author David Bargerhuff
 @version 1.0
 @since May 07, 2009
 Last Updated:
 ***************************************************************************/

public class ImplantVolumeVO extends AbstractSiteBuilderVO {
	private static final long serialVersionUID = 1l;
	private String repFirstNm = null;
	private String repLastNm = null;
	private String surgeonFirstNm = null;
	private String surgeonLastNm = null;
	private String titleNm = null;
	private String surgeonId = null;
	private int sjmPercent = 0;
	private int bscPercent = 0; 
	private int mdtPercent = 0;
	private int unknownPercent = 0;
	private int sjmTrials = 0;
	private int sjmPerms = 0;
	private int sjmTrialsVolume = 0;
	private int sjmPermsVolume = 0;
	private int bscTrialsVolume = 0;
	private int bscPermsVolume = 0;
	private int mdtTrialsVolume = 0;
	private int mdtPermsVolume = 0;
	private int unknownTrialsVolume = 0;
	private int unknownPermsVolume = 0;
	private int totalTrialsVolume = 0;
	private int totalPermsVolume = 0;

	/**
	 * 
	 */
	public ImplantVolumeVO() {
	}
	
	/**
	 * Initializes the VO to the params provided in the row object
	 * @param rs
	 */
	public ImplantVolumeVO(ResultSet rs) {
		super();
		setData(rs);
	}
	
	/**
	 * Sets the VO to the params provided in the row object
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		StringEncoder se = new StringEncoder();
		DBUtil db = new DBUtil();
		repFirstNm = se.decodeValue(db.getStringVal("rep_first_nm", rs));
		repLastNm = se.decodeValue(db.getStringVal("rep_last_nm", rs));
		surgeonFirstNm = se.decodeValue(db.getStringVal("phys_first_nm", rs));
		surgeonLastNm = se.decodeValue(db.getStringVal("phys_last_nm", rs));
		titleNm = se.decodeValue(db.getStringVal("title_nm", rs));
		addImplantData(db.getStringVal("business_plan_id",rs), db.getStringVal("value_txt",rs));

	}
	
	/**
	 * Calculates implant volume for each implant brand used, based on
	 * trials and perms data.
	 */
	public void setVolume() {
		
		int sjmTrialsEst = this.getSjmTrials();
		int sjmPermsEst = this.getSjmPerms();
		
		Integer sjmPct = this.getSjmPercent();
		
		Double calcTrialsPctTotal = 0.0;
		Double calcPermsPctTotal = 0.0;
		
		if (sjmPct > 0) {
			calcTrialsPctTotal = sjmTrialsEst/sjmPct.doubleValue();
			calcPermsPctTotal = sjmPermsEst/sjmPct.doubleValue();
		} 
		
		//Remaining trials numbers are calculated using the computed total. 
		Double mdtTrialsUsed = calcTrialsPctTotal * this.getMdtPercent();
		Double bscTrialsUsed = calcTrialsPctTotal * this.getBscPercent();
		Double unkTrialsUsed = calcTrialsPctTotal * this.getUnknownPercent();
		
		//Remaining perms numbers are calculated using the computed total.
		Double mdtPermsUsed = calcPermsPctTotal * this.getMdtPercent();
		Double bscPermsUsed = calcPermsPctTotal * this.getBscPercent();
		Double unkPermsUsed = calcPermsPctTotal * this.getUnknownPercent();
		
		this.setSjmTrialsVolume(sjmTrialsEst);
		this.setMdtTrialsVolume(mdtTrialsUsed.intValue());
		this.setBscTrialsVolume(bscTrialsUsed.intValue());
		this.setUnknownTrialsVolume(unkTrialsUsed.intValue());
		
		this.setSjmPermsVolume(sjmPermsEst);
		this.setMdtPermsVolume(mdtPermsUsed.intValue());
		this.setBscPermsVolume(bscPermsUsed.intValue());
		this.setUnknownPermsVolume(unkPermsUsed.intValue());
		
		this.setTotalTrialsVolume(sjmTrialsEst + mdtTrialsUsed.intValue() + 
				bscTrialsUsed.intValue() +	unkTrialsUsed.intValue());
		
		this.setTotalPermsVolume(sjmPermsEst + mdtPermsUsed.intValue() + 
				bscPermsUsed.intValue() + unkPermsUsed.intValue());
	}
	
	/**
	 * Sets the competitor implant number and adds number to the total volume.
	 * @param rs
	 */
	public void addImplantData(String field, String value) {
		field = StringUtil.checkVal(field);
		value = StringUtil.checkVal(value);
		//log.debug("addImplantData: field/value: " + field + " / " + value);
		
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
		} else if (field.startsWith("trialQ")) {
			this.setSjmTrials(val);
		} else if (field.startsWith("permQ")) {
			this.setSjmPerms(val);
		}		 
	}
	
	/**
	 * @return the repFirstNm
	 */
	public String getRepFirstNm() {
		return repFirstNm;
	}

	/**
	 * @param repFirstNm the repFirstNm to set
	 */
	public void setRepFirstNm(String repFirstNm) {
		this.repFirstNm = repFirstNm;
	}

	/**
	 * @return the repLastNm
	 */
	public String getRepLastNm() {
		return repLastNm;
	}

	/**
	 * @param repLastNm the repLastNm to set
	 */
	public void setRepLastNm(String repLastNm) {
		this.repLastNm = repLastNm;
	}

	/**
	 * @return the surgeonFirstNm
	 */
	public String getSurgeonFirstNm() {
		return surgeonFirstNm;
	}

	/**
	 * @param surgeonFirstNm the surgeonFirstNm to set
	 */
	public void setSurgeonFirstNm(String surgeonFirstNm) {
		this.surgeonFirstNm = surgeonFirstNm;
	}

	/**
	 * @return the surgeonLastNm
	 */
	public String getSurgeonLastNm() {
		return surgeonLastNm;
	}

	/**
	 * @param surgeonLastNm the surgeonLastNm to set
	 */
	public void setSurgeonLastNm(String surgeonLastNm) {
		this.surgeonLastNm = surgeonLastNm;
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
	 * @return the unknownPercent
	 */
	public int getUnknownPercent() {
		return unknownPercent;
	}

	/**
	 * @param unknownPercent the unknownPercent to set
	 */
	public void setUnknownPercent(int unknownPercent) {
		this.unknownPercent = unknownPercent;
	}

	/**
	 * @return the totalVolume
	 */
	public int getTotalTrialsVolume() {
		return totalTrialsVolume;
	}

	/**
	 * @param totalVolume the totalVolume to set
	 */
	public void setTotalTrialsVolume(int totalTrialsVolume) {
		this.totalTrialsVolume = totalTrialsVolume;
	}

	/**
	 * @return the totalPermsVolume
	 */
	public int getTotalPermsVolume() {
		return totalPermsVolume;
	}

	/**
	 * @param totalPermsVolume the totalPermsVolume to set
	 */
	public void setTotalPermsVolume(int totalPermsVolume) {
		this.totalPermsVolume = totalPermsVolume;
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
	 * @return the sjmTrials
	 */
	public int getSjmTrials() {
		return sjmTrials;
	}

	/**
	 * @param sjmTrials the sjmTrials to set
	 */
	public void setSjmTrials(int sjmTrials) {
		this.sjmTrials = this.sjmTrials + sjmTrials;
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
		this.sjmPerms = this.sjmPerms + sjmPerms;
	}

	/**
	 * @return the sjmTrialsVolume
	 */
	public int getSjmTrialsVolume() {
		return sjmTrialsVolume;
	}

	/**
	 * @param sjmTrialsVolume the sjmTrialsVolume to set
	 */
	public void setSjmTrialsVolume(int sjmTrialsVolume) {
		this.sjmTrialsVolume = sjmTrialsVolume;
	}

	/**
	 * @return the sjmPermsVolume
	 */
	public int getSjmPermsVolume() {
		return sjmPermsVolume;
	}

	/**
	 * @param sjmPermsVolume the sjmPermsVolume to set
	 */
	public void setSjmPermsVolume(int sjmPermsVolume) {
		this.sjmPermsVolume = sjmPermsVolume;
	}

	/**
	 * @return the bscTrialsVolume
	 */
	public int getBscTrialsVolume() {
		return bscTrialsVolume;
	}

	/**
	 * @param bscTrialsVolume the bscTrialsVolume to set
	 */
	public void setBscTrialsVolume(int bscTrialsVolume) {
		this.bscTrialsVolume = bscTrialsVolume;
	}

	/**
	 * @return the bscPermsVolume
	 */
	public int getBscPermsVolume() {
		return bscPermsVolume;
	}

	/**
	 * @param bscPermsVolume the bscPermsVolume to set
	 */
	public void setBscPermsVolume(int bscPermsVolume) {
		this.bscPermsVolume = bscPermsVolume;
	}

	/**
	 * @return the mdtTrialsVolume
	 */
	public int getMdtTrialsVolume() {
		return mdtTrialsVolume;
	}

	/**
	 * @param mdtTrialsVolume the mdtTrialsVolume to set
	 */
	public void setMdtTrialsVolume(int mdtTrialsVolume) {
		this.mdtTrialsVolume = mdtTrialsVolume;
	}

	/**
	 * @return the mdtPermsVolume
	 */
	public int getMdtPermsVolume() {
		return mdtPermsVolume;
	}

	/**
	 * @param mdtPermsVolume the mdtPermsVolume to set
	 */
	public void setMdtPermsVolume(int mdtPermsVolume) {
		this.mdtPermsVolume = mdtPermsVolume;
	}

	/**
	 * @return the unknownTrialsVolume
	 */
	public int getUnknownTrialsVolume() {
		return unknownTrialsVolume;
	}

	/**
	 * @param unknownTrialsVolume the unknownTrialsVolume to set
	 */
	public void setUnknownTrialsVolume(int unknownTrialsVolume) {
		this.unknownTrialsVolume = unknownTrialsVolume;
	}

	/**
	 * @return the unknownPermsVolume
	 */
	public int getUnknownPermsVolume() {
		return unknownPermsVolume;
	}

	/**
	 * @param unknownPermsVolume the unknownPermsVolume to set
	 */
	public void setUnknownPermsVolume(int unknownPermsVolume) {
		this.unknownPermsVolume = unknownPermsVolume;
	}
	
}
