package com.depuy.sitebuilder.datafeed;

// JDK 1.5.0
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

// SMT Base Luibs
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: DataVO.java<p/>
 * <b>Description: </b> Holds the data for a row of info for the ID Media
 * feed.  Exports the data as a fixed width row of data 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2007<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Apr 21, 2007
 ****************************************************************************/
public class DataVO implements Serializable {
	private static final long serialVersionUID = 1l;
	private String phoneNumber = "";
	private String zipCode = "";
	private String clientCode = "";
	private String estimateNumber = "";
	private int eligibleResponse = 0;
	private int lead = 0;
	private int qualifiedLead = 0;
	private Date attemptDate = null;
	
	// Ancillary vars to be stored, but not used in the output
	private String profileId = null;
	private String customerId = null;
	private String resultCode = null;
	private int optIn = 0;
	private int yesAnswer = 0;
	
	// Default codes
	public static final String CLIENT_CODE = "DEPU";
	public static final String ESTIMATE_CODE = "2207";
	public static final String ELIGIBLE_CODE = "00";
	public static final String LEAD_CODE = "00";
	public static final String QUALIFIED_CODE = "00";
	
	// Output fixed width data sizes
	public static final int PHONE_NUMBER_LENGTH = 10;
	public static final int ZIP_CODE_LENGTH = 5;
	public static final int CALL_DATE_LENGTH = 8;
	public static final int CALL_TIME_LENGTH = 8;
	public static final int CLIENT_CODE_LENGTH = 4;
	public static final int ESTIMATE_NUMBER_LENGTH = 4;
	public static final int ELIGIBLE_RESPONSE_LENGTH = 2;
	public static final int LEAD_LENGTH = 2;
	public static final int QUALIFIED_LEAD_LENGTH = 2;
	
	/**
	 * Produces the fixed with format for outputting the text file
	 * @return
	 */
	public String toFixedString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.getFixedPhoneNumber());
		sb.append(this.getFixedZipCode());
		sb.append(this.getFixedCallDate());
		sb.append(this.getFixedCallTime());
		sb.append(StringUtil.checkVal(this.getFixedClientCode(), DataVO.CLIENT_CODE));
		sb.append(StringUtil.checkVal(this.getFixedEstimateNumber(), DataVO.ESTIMATE_CODE));
		sb.append(StringUtil.checkVal(this.getFixedEligibleResponses(),DataVO.ELIGIBLE_CODE));
		sb.append(StringUtil.checkVal(this.getFixedLead(), DataVO.LEAD_CODE));
		sb.append(StringUtil.checkVal(this.getFixedQualifiedLead(), DataVO.QUALIFIED_CODE));
		
		// Replace the nulls with the sapce character
		String spaceFilled = sb.toString().replaceAll("\u0000", " ");
		return spaceFilled;
	}
	
	/**
	 * Creates the fixed length String for the phone number
	 * @return
	 */
	public String getFixedPhoneNumber() {
		if (phoneNumber == null) phoneNumber = "";
		StringBuilder sb = new StringBuilder(phoneNumber);
		sb.setLength(DataVO.PHONE_NUMBER_LENGTH);
		return sb.toString();
	}
	
	/**
	 * Creates the fixed length String for the zipCode
	 * @return
	 */
	public String getFixedZipCode() {
		if (zipCode == null) zipCode = "";
		StringBuilder sb = new StringBuilder(zipCode);
		sb.setLength(DataVO.ZIP_CODE_LENGTH);
		return sb.toString();
	}
	
	/**
	 * Creates the fixed length String for the call Date
	 * @return
	 */
	public String getFixedCallDate() {
		String fmtDate = "";
		Calendar cal = new GregorianCalendar();
		if (attemptDate != null) {
			cal.setTime(attemptDate);
			fmtDate += StringUtil.padLeft("" + (cal.get(Calendar.MONTH) + 1), '0', 2);
			fmtDate += "/";
			fmtDate += StringUtil.padLeft("" + cal.get(Calendar.DAY_OF_MONTH),'0', 2);
			fmtDate += "/";
			
			String year = "" + cal.get(Calendar.YEAR);
			fmtDate += year.substring(2);
		}
		
		StringBuilder sb = new StringBuilder(fmtDate);
		sb.setLength(DataVO.CALL_DATE_LENGTH);
		return sb.toString();
	}
	
	/**
	 * Creates the fixed length String for the call time
	 * @return
	 */
	public String getFixedCallTime() {
		String fmtDate = "";
		Calendar cal = new GregorianCalendar();
		
		if (attemptDate != null) {
			cal.setTime(attemptDate);
			fmtDate += StringUtil.padLeft("" + cal.get(Calendar.HOUR_OF_DAY), '0', 2);
			fmtDate += ":";
			fmtDate += StringUtil.padLeft("" + cal.get(Calendar.MINUTE), '0', 2);
			fmtDate += ":";
			fmtDate += StringUtil.padLeft("" + cal.get(Calendar.SECOND), '0', 2);
		}
		
		StringBuilder sb = new StringBuilder(fmtDate);
		sb.setLength(DataVO.CALL_TIME_LENGTH);
		return sb.toString();
	}
	
	/**
	 * Creates the fixed length String for the clientCode
	 * @return
	 */
	public String getFixedClientCode() {
		if (clientCode == null) clientCode = "";
		StringBuilder sb = new StringBuilder(clientCode);
		sb.setLength(DataVO.CLIENT_CODE_LENGTH);
		return sb.toString();
	}
	
	/**
	 * Creates the fixed length String for the estimateNumber
	 * @return
	 */
	public String getFixedEstimateNumber() {
		if (estimateNumber == null) estimateNumber = "";
		StringBuilder sb = new StringBuilder(estimateNumber);
		sb.setLength(DataVO.ESTIMATE_NUMBER_LENGTH);
		return sb.toString();
	}
	
	/**
	 * Creates the fixed length String for the eligibleResponse
	 * @return
	 */
	public String getFixedEligibleResponses() {
		StringBuilder sb = new StringBuilder("0" + eligibleResponse);
		sb.setLength(DataVO.ELIGIBLE_RESPONSE_LENGTH);
		return sb.toString();
	}
	
	/**
	 * Creates the fixed length String for the lead
	 * @return
	 */
	public String getFixedLead() {
		StringBuilder sb = new StringBuilder("0" + lead);
		sb.setLength(DataVO.LEAD_LENGTH);
		return sb.toString();
	}
	
	/**
	 * Creates the fixed length String for the qualified lead
	 * @return
	 */
	public String getFixedQualifiedLead() {
		StringBuilder sb = new StringBuilder("0" + qualifiedLead);
		sb.setLength(DataVO.QUALIFIED_LEAD_LENGTH);
		return sb.toString();
	}

	/**
	 * @return the clientCode
	 */
	public String getClientCode() {
		return clientCode;
	}
	/**
	 * @return the eligibleResponse
	 */
	public int getEligibleResponse() {
		return eligibleResponse;
	}
	/**
	 * @return the estimateNumber
	 */
	public String getEstimateNumber() {
		return estimateNumber;
	}
	/**
	 * @return the lead
	 */
	public int getLead() {
		return lead;
	}
	/**
	 * @return the phoneNumber
	 */
	public String getPhoneNumber() {
		return phoneNumber;
	}
	/**
	 * @return the qualifiedLead
	 */
	public int getQualifiedLead() {
		return qualifiedLead;
	}
	/**
	 * @return the zipCode
	 */
	public String getZipCode() {
		return zipCode;
	}

	/**
	 * @param clientCode the clientCode to set
	 */
	public void setClientCode(String clientCode) {
		this.clientCode = clientCode;
	}
	/**
	 * @param eligibleResponse the eligibleResponse to set
	 */
	public void setEligibleResponse(int eligibleResponse) {
		this.eligibleResponse = eligibleResponse;
	}
	/**
	 * @param estimateNumber the estimateNumber to set
	 */
	public void setEstimateNumber(String estimateNumber) {
		this.estimateNumber = estimateNumber;
	}
	/**
	 * @param lead the lead to set
	 */
	public void setLead(int lead) {
		this.lead = lead;
	}
	/**
	 * @param phoneNumber the phoneNumber to set
	 */
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	/**
	 * @param qualifiedLead the qualifiedLead to set
	 */
	public void setQualifiedLead(int qualifiedLead) {
		this.qualifiedLead = qualifiedLead;
	}
	/**
	 * @param zipCode the zipCode to set
	 */
	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
	}

	/**
	 * @return the attemptDate
	 */
	public Date getAttemptDate() {
		return attemptDate;
	}

	/**
	 * @param attemptDate the attemptDate to set
	 */
	public void setAttemptDate(Date attemptDate) {
		this.attemptDate = attemptDate;
	}

	/**
	 * @return the optIn
	 */
	public int getOptIn() {
		return optIn;
	}

	/**
	 * @return the profileId
	 */
	public String getProfileId() {
		return profileId;
	}

	/**
	 * @param optIn the optIn to set
	 */
	public void setOptIn(int optIn) {
		this.optIn = optIn;
	}

	/**
	 * @param profileId the profileId to set
	 */
	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}

	/**
	 * @return the resultCode
	 */
	public String getResultCode() {
		return resultCode;
	}

	/**
	 * @param resultCode the resultCode to set
	 */
	public void setResultCode(String resultCode) {
		this.resultCode = resultCode;
	}

	/**
	 * @return the customerId
	 */
	public String getCustomerId() {
		return customerId;
	}

	/**
	 * @param customerId the customerId to set
	 */
	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}
	
	public void addYesAnswer() {
		yesAnswer++;
	}
	
	public int getYesAnswer() {
		return yesAnswer;
	}
	
}
