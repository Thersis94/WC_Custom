package com.depuy.events.vo;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import com.depuy.events_v2.CoopAdsActionV2;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSiteBuilderVO;

/****************************************************************************
 * <b>Title</b>: CoopAdsVO.java<p/>
 * <b>Description: holds Co-op ads data for the DePuy Events/Postcards.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2008<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since May 14, 2008
 * @Updates
 * 	 - JM 03-06-13 - added surgeon and clinic attributes to support CFSEM Events
 ****************************************************************************/
public class CoopAdVO extends AbstractSiteBuilderVO {
	private static final long serialVersionUID = 1l;
	private String coopAdId = null;
	private String eventPostcardId = null;
	private String newspaper1Text = null;
	private String newspaper2Text = null;
	private String newspaper3Text = null;
	private String newspaper1Phone = null;
	private String newspaper2Phone = null;
	private String newspaper3Phone = null;
	private String adDatesText = null;
	private Double totalCostNo = null;
	private Double costToRepNo = null;
	private String approvedPaperName = null;
	private String adFileUrl = null;
	private String territoryNo = null;
	private Integer statusFlg = null;
	private Map<String, Integer> eventCodes = new HashMap<String, Integer>();
	private String adType = null;

	// added for Seminars site rewrite, Jan 2014. - JM
	private String contactName = null;
	private String contactEmail = null;
	private String instructionsText = null;

	private Integer surgeonStatusFlg = null; // the surgeon must approve CFSEM independently of the Rep
	private String surgeonName = null;
	private String surgeonTitle = null;
	private String surgeonEmail = null; // used to send approval notification email
	private String surgeonImageUrl = null;
	private String clinicName = null;
	private String clinicAddress = null;
	private String clinicPhone = null;
	private String clinicHours = null;
	private String surgicalExperience = null;
	private String invoiceFile = null;

	public CoopAdVO() {
	}

	public CoopAdVO(ResultSet rs) {
		this.setData(rs);
	}

	public CoopAdVO(SMTServletRequest req) {
		this.setData(req);
	}

	public void setData(SMTServletRequest req) {
		coopAdId = req.getParameter("coopAdId");
		eventPostcardId = req.getParameter("eventPostcardId");
		newspaper1Text = req.getParameter("newspaper1Text");
		newspaper2Text = req.getParameter("newspaper2Text");
		newspaper3Text = req.getParameter("newspaper3Text");
		totalCostNo = Convert.formatDouble(StringUtil.replace(req.getParameter("totalCostNo"), ",", ""));
		costToRepNo = Convert.formatDouble(StringUtil.replace(req.getParameter("costToRepNo"), ",", ""));
		approvedPaperName = req.getParameter("approvedPaperName");
		newspaper1Phone = req.getParameter("newspaper1Phone");
		newspaper2Phone = req.getParameter("newspaper2Phone");
		newspaper3Phone = req.getParameter("newspaper3Phone");
		adDatesText = req.getParameter("adDatesText");
		territoryNo = req.getParameter("territoryNo");
		adType = req.getParameter("adType");
		if (req.hasParameter("adStatusFlg"))
			statusFlg = Convert.formatInteger(req.getParameter("adStatusFlg"), 0);

		// added for Seminars site rewrite, Jan 2014. - JM
		setContactName(req.getParameter("contactName"));
		setContactEmail(req.getParameter("contactEmail"));
		setInstructionsText(req.getParameter("instructionsText"));

		// adFileUrl will get set by the file upload
		// statusFlg will get set by the action
		surgeonStatusFlg = Convert.formatInteger(req.getParameter("surgeonStatusFlag"));
		surgeonName = req.getParameter("surgeonName");
		surgeonTitle = req.getParameter("surgeonTitle");
		setSurgeonEmail(req.getParameter("surgeonEmail"));
		// surgeonImageUrl will get set by file upload
		clinicName = req.getParameter("clinicName");
		clinicAddress = req.getParameter("clinicAddress");
		clinicPhone = req.getParameter("clinicPhone");
		clinicHours = req.getParameter("clinicHours");
		surgicalExperience = req.getParameter("surgicalExperience");
    }
    
    public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		coopAdId = db.getStringVal("coop_ad_id", rs);
		eventPostcardId = db.getStringVal("event_postcard_id", rs);
		newspaper1Text = db.getStringVal("newspaper1_txt", rs);
		newspaper2Text = db.getStringVal("newspaper2_txt", rs);
		newspaper3Text = db.getStringVal("newspaper3_txt", rs);
		totalCostNo = db.getDoubleVal("total_cost_no", rs);
		costToRepNo = db.getDoubleVal("cost_to_rep_no", rs);
		approvedPaperName = db.getStringVal("approved_paper_nm", rs);
		adFileUrl = db.getStringVal("ad_file_url", rs);
		statusFlg = db.getIntegerVal("status_flg", rs);
		newspaper1Phone = db.getStringVal("newspaper1_phone_no", rs);
		newspaper2Phone = db.getStringVal("newspaper2_phone_no", rs);
		newspaper3Phone = db.getStringVal("newspaper3_phone_no", rs);
		adDatesText = db.getStringVal("run_dates_txt", rs);
		territoryNo = db.getStringVal("territory_no", rs);
		adType = db.getStringVal("ad_type_txt", rs);
		contactName = db.getStringVal("contact_nm", rs);
		contactEmail = db.getStringVal("contact_email_txt", rs);
		instructionsText = db.getStringVal("instructions_txt", rs);

		surgeonStatusFlg = db.getIntegerVal("surgeon_status_flg", rs);
		surgeonName = db.getStringVal("surgeon_nm", rs);
		surgeonTitle = db.getStringVal("surgeon_title_txt", rs);
		setSurgeonEmail(db.getStringVal("surgeon_email_txt", rs));
		surgeonImageUrl = db.getStringVal("surgeon_img_url", rs);
		clinicName = db.getStringVal("clinic_nm", rs);
		clinicAddress = db.getStringVal("clinic_address_txt", rs);
		clinicPhone = db.getStringVal("clinic_phone_txt", rs);
		clinicHours = db.getStringVal("clinic_hours_txt", rs);
		surgicalExperience = db.getStringVal("surg_experience_txt", rs);
		invoiceFile = db.getStringVal("invoice_path_txt", rs);
		db = null;
    }

	public String getCoopAdId() {
		return coopAdId;
	}

	public void setCoopAdId(String coopAdId) {
		this.coopAdId = coopAdId;
	}

	public String getEventPostcardId() {
		return eventPostcardId;
	}

	public void setEventPostcardId(String eventPostcardId) {
		this.eventPostcardId = eventPostcardId;
	}

	public String getNewspaper1Text() {
		return newspaper1Text;
	}

	public void setNewspaper1Text(String newspaper1Text) {
		this.newspaper1Text = newspaper1Text;
	}

	public String getNewspaper2Text() {
		return newspaper2Text;
	}

	public void setNewspaper2Text(String newspaper2Text) {
		this.newspaper2Text = newspaper2Text;
	}

	public Double getTotalCostNo() {
		return totalCostNo;
	}

	public void setTotalCostNo(Double totalCostNo) {
		this.totalCostNo = totalCostNo;
	}

	public Double getCostToRepNo() {
		return costToRepNo;
	}

	public void setCostToRepNo(Double costToRepNo) {
		this.costToRepNo = costToRepNo;
	}

	public String getApprovedPaperName() {
		return approvedPaperName;
	}

	public void setApprovedPaperName(String approvedPaperName) {
		this.approvedPaperName = approvedPaperName;
	}

	public String getAdFileUrl() {
		return adFileUrl;
	}

	public void setAdFileUrl(String adFileUrl) {
		this.adFileUrl = adFileUrl;
	}

	public Integer getStatusFlg() {
		return statusFlg;
	}

	public void setStatusFlg(Integer statusFlg) {
		this.statusFlg = statusFlg;
	}

	public void addEvent(String eventEntryId, int orderNo) {
		eventCodes.put(eventEntryId, orderNo);
	}

	public String getEventCodes() {
		StringBuffer codes = new StringBuffer();

		for (String key : eventCodes.keySet())
			codes.append(key).append(", ");

		if (codes.length() > 2)
			codes = new StringBuffer(codes.substring(0, codes.length() - 2));

		return codes.toString();
	}
	
	/**
	 * used by reports to cosmetically label the statusNo
	 * @return
	 */
	public String getStatusName() {
		switch (getStatusFlg()) {
			case CoopAdsActionV2.CLIENT_SUBMITTED: return "Pending Creation";
			case CoopAdsActionV2.PENDING_CLIENT_APPROVAL: return "Pending Coordinator Approval";
			case CoopAdsActionV2.CLIENT_APPROVED_AD: return "Coordinator Approved Ad";
			case CoopAdsActionV2.CLIENT_DECLINED_AD: return "Coordinator Declined Ad";
			case CoopAdsActionV2.CLIENT_PAYMENT_RECD: return "Ad Payment Received";
			default: return "";
		}
	}
	
	/**
	 * ugly JSP helpers to determine which events to pre-populate the form with
	 * @return
	 */
	public String getFirstEventCode() {
		return getEventCodeByOrder(1);
	}

	public String getSecondEventCode() {
		return getEventCodeByOrder(2);
	}

	private String getEventCodeByOrder(int order) {
		for (String key : eventCodes.keySet())
			if (eventCodes.get(key) == order)
				return key;

		return "";
	}

	public String getNewspaper1Phone() {
		return newspaper1Phone;
	}

	public void setNewspaper1Phone(String newspaper1Phone) {
		this.newspaper1Phone = newspaper1Phone;
	}

	public String getNewspaper2Phone() {
		return newspaper2Phone;
	}

	public void setNewspaper2Phone(String newspaper2Phone) {
		this.newspaper2Phone = newspaper2Phone;
	}

	public String getAdDatesText() {
		return adDatesText;
	}

	public void setAdDatesText(String adDatesText) {
		this.adDatesText = adDatesText;
	}

	public String getTerritoryNo() {
		return territoryNo;
	}

	public void setTerritoryNo(String territoryNo) {
		this.territoryNo = territoryNo;
	}

	public String getAdType() {
		return adType;
	}

	public void setAdType(String adType) {
		this.adType = adType;
	}

	public String getNewspaper3Text() {
		return newspaper3Text;
	}

	public void setNewspaper3Text(String newspaper3Text) {
		this.newspaper3Text = newspaper3Text;
	}

	public String getNewspaper3Phone() {
		return newspaper3Phone;
	}

	public void setNewspaper3Phone(String newspaper3Phone) {
		this.newspaper3Phone = newspaper3Phone;
	}

	/*
	 * the optional 2nd newspaper is required if the submitter has requested
	 * addtl ads, so we key off that for displaying info in the view.
	 */
	public boolean getHasAddtlAds() {
		return (newspaper2Text != null && newspaper2Text.length() > 0);
	}

	public String getSurgeonName() {
		return surgeonName;
	}

	public void setSurgeonName(String surgeonName) {
		this.surgeonName = surgeonName;
	}

	public String getSurgeonTitle() {
		return surgeonTitle;
	}

	public void setSurgeonTitle(String surgeonTitle) {
		this.surgeonTitle = surgeonTitle;
	}

	public String getSurgeonImageUrl() {
		return surgeonImageUrl;
	}

	public void setSurgeonImageUrl(String surgeonImageUrl) {
		this.surgeonImageUrl = surgeonImageUrl;
	}

	public String getClinicName() {
		return clinicName;
	}

	public void setClinicName(String clinicName) {
		this.clinicName = clinicName;
	}

	public String getClinicAddress() {
		return clinicAddress;
	}

	public void setClinicAddress(String clinicAddress) {
		this.clinicAddress = clinicAddress;
	}

	public String getClinicPhone() {
		return clinicPhone;
	}

	public void setClinicPhone(String clinicPhone) {
		this.clinicPhone = clinicPhone;
	}

	public String getClinicHours() {
		return clinicHours;
	}

	public void setClinicHours(String clinicHours) {
		this.clinicHours = clinicHours;
	}

	public String getSurgicalExperience() {
		return surgicalExperience;
	}

	public void setSurgicalExperience(String surgicalExperience) {
		this.surgicalExperience = surgicalExperience;
	}

	public void setEventCodes(Map<String, Integer> eventCodes) {
		this.eventCodes = eventCodes;
	}

	public String getSurgeonEmail() {
		return surgeonEmail;
	}

	public void setSurgeonEmail(String surgeonEmail) {
		this.surgeonEmail = surgeonEmail;
	}

	public Integer getSurgeonStatusFlg() {
		return surgeonStatusFlg;
	}

	public void setSurgeonStatusFlg(Integer surgeonStatusFlg) {
		this.surgeonStatusFlg = surgeonStatusFlg;
	}

	public String getInstructionsText() {
		return instructionsText;
	}

	public void setInstructionsText(String instructionsText) {
		this.instructionsText = instructionsText;
	}

	public String getContactName() {
		return contactName;
	}

	public void setContactName(String contactName) {
		this.contactName = contactName;
	}

	public String getContactEmail() {
		return contactEmail;
	}

	public void setContactEmail(String contactEmail) {
		this.contactEmail = contactEmail;
	}

	/**
	 * @return the invoiceFile
	 */
	public String getInvoiceFile() {
		return invoiceFile;
	}

	/**
	 * @param invoiceFile the invoiceFile to set
	 */
	public void setInvoiceFile(String invoiceFile) {
		this.invoiceFile = invoiceFile;
	}

}
