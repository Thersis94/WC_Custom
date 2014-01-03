package com.ansmed.sb.patient;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.parser.StringEncoder;
import com.smt.sitebuilder.action.SBModuleVO;

/****************************************************************************
 * <b>Title</b>:MDJournalVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Mar 5, 2009
 * <b>Changes: </b>
 ****************************************************************************/
public class MDJournalVO extends SBModuleVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String repJournalId = null;
	private String surgeonId = null;
	private String surgeonName = null;
	private String reasonForVisit = null;
	private String spokeAbout = null;
	private String whom = null;
	private String whatNext = null;
	private String who = null;
	private String repName = null;
	private String salesRepId = null;
	private Date inServiceDate = null;
	private List<String> currencies = new ArrayList<String>();
	
	/**
	 * 
	 */
	public MDJournalVO() {
		
	}
	
	
	public void setData(ResultSet rs) {
		StringEncoder se = new StringEncoder();
		DBUtil db = new DBUtil();
		this.repJournalId = db.getStringVal("rep_journal_id", rs);
		this.surgeonId = db.getStringVal("surgeon_id", rs);
		this.reasonForVisit = db.getStringVal("reason_visit_txt", rs);
		this.spokeAbout = se.decodeValue(db.getStringVal("about_txt", rs));
		this.whom = db.getStringVal("whom_sales_rep_nm", rs);
		this.who = db.getStringVal("who_sales_rep_nm", rs);
		this.whatNext = se.decodeValue(db.getStringVal("next_step_txt", rs));
		this.repName = db.getStringVal("sales_rep_nm", rs);
		this.salesRepId = db.getStringVal("sales_rep_id", rs);
		surgeonName = db.getStringVal("first_nm", rs) + " " + db.getStringVal("last_nm", rs);
		this.addCurrency(db.getStringVal("resource_nm", rs));
		this.setInServiceDate(db.getDateVal("in_service_dt", rs));
		this.setCreateDate(db.getDateVal("create_dt", rs));
		
	}
	
	/**
	 * 
	 * @param currency
	 */
	public void addCurrency(String currency) {
		if (currency != null)
			currencies.add(currency);
	}
	
	/**
	 * Returns a comma delimited list of currencies
	 * @return
	 */
	public String getCurrencies() {
		StringBuffer c = new StringBuffer();
		for (int i=0; i < currencies.size(); i++) {
			if (i > 0) c.append(", ");
			c.append(currencies.get(i));
		}
		
		return c.toString();
	}

	/**
	 * @return the repJournalId
	 */
	public String getRepJournalId() {
		return repJournalId;
	}

	/**
	 * @param repJournalId the repJournalId to set
	 */
	public void setRepJournalId(String repJournalId) {
		this.repJournalId = repJournalId;
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
	 * @return the reasonForVisit
	 */
	public String getReasonForVisit() {
		return reasonForVisit;
	}

	/**
	 * @param reasonForVisit the reasonForVisit to set
	 */
	public void setReasonForVisit(String reasonForVisit) {
		this.reasonForVisit = reasonForVisit;
	}
	
	/**
	 * @return the spokeAbout
	 */
	public String getSpokeAbout() {
		return spokeAbout;
	}

	/**
	 * @param spokeAbout the spokeAbout to set
	 */
	public void setSpokeAbout(String spokeAbout) {
		this.spokeAbout = spokeAbout;
	}

	/**
	 * @return the whom
	 */
	public String getWhom() {
		return whom;
	}

	/**
	 * @param whom the whom to set
	 */
	public void setWhom(String whom) {
		this.whom = whom;
	}

	/**
	 * @return the whatNext
	 */
	public String getWhatNext() {
		return whatNext;
	}

	/**
	 * @param whatNext the whatNext to set
	 */
	public void setWhatNext(String whatNext) {
		this.whatNext = whatNext;
	}

	/**
	 * @return the who
	 */
	public String getWho() {
		return who;
	}

	/**
	 * @param who the who to set
	 */
	public void setWho(String who) {
		this.who = who;
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
	 * @return the inServiceDate
	 */
	public Date getInServiceDate() {
		return inServiceDate;
	}


	/**
	 * @param inServiceDate the inServiceDate to set
	 */
	public void setInServiceDate(Date inServiceDate) {
		this.inServiceDate = inServiceDate;
	}

}
