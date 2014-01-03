package com.depuy.events.vo.report;

import com.depuy.events.CoopAdsAction;
import com.depuy.events.vo.CoopAdVO;
import com.depuy.events.vo.DePuyEventAddtlPostcardVO;
import com.depuy.events.vo.DePuyEventEntryVO;
import com.depuy.events.vo.DePuyEventPostcardVO;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.security.UserDataVO;

/*****************************************************************************
 <p><b>Title</b>: PostcardSummaryReportVO.java</p>
 <p>compiles a report for post cards sent</p>
 <p>Copyright: Copyright (c) 2000 - 2006 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Nov 6, 2006
 ***************************************************************************/

public class PostcardSummaryReportVO extends AbstractSBReportVO {
    private static final long serialVersionUID = 1l;
    private DePuyEventPostcardVO postcard = new DePuyEventPostcardVO();

    public PostcardSummaryReportVO() {
        super();
        setContentType("application/vnd.ms-excel");
        isHeaderAttachment(Boolean.TRUE);
        setFileName("Postcard-Summary.xls");
    }
    
    /**
     * Assigns the event postcard data retrieved from the parent action
     * variables
     * @param data (List<DePuyEventPostcardVO>)
     * @throws SQLException
     */
    public void setData(Object o) {
    	DePuyEventPostcardVO postcard = (DePuyEventPostcardVO) o;
    	this.postcard = postcard;
    }
    
	public byte[] generateReport() {
		log.debug("starting PostcardSummaryReport");
		
		StringBuffer rpt = new StringBuffer(this.getHeader());
		rpt.append("<tr><td>Product</td><td align='center'>").append(postcard.getProductName()).append("</td></tr>\r");
		rpt.append("<tr><td>Postcard Type</td><td align='center'>").append(postcard.getDePuyEvents().get(0).getEventTypeDesc()).append("</td></tr>\r");
		rpt.append("<tr><td>Seminar Promotion #(s)</td><td align='center'>").append(postcard.getRSVPCodes()).append("</td></tr>\r");
		
		for (DePuyEventEntryVO event : postcard.getDePuyEvents()) {
			rpt.append("<tr><td colspan='2'>&nbsp;</td></tr>");
			rpt.append("<tr><td>Seminar #").append(event.getRSVPCode()).append("</td><td>").append(event.getEventName()).append("</td></tr>\r");
			rpt.append("<tr><td>Seminar Date/Time</td><td>").append(Convert.formatDate(event.getStartDate(),Convert.DATE_LONG)).append(" ").append(event.getLocationDesc()).append("</td></tr>");
			rpt.append("<tr><td>Seminar Location</td><td>").append(event.getCityName()).append(", ").append(event.getStateCode()).append(" ").append(event.getZipCode()).append("</td></tr>\r");
			rpt.append("<tr><td>Product</td><td>").append(postcard.getProductName()).append("</td></tr>\r");
			rpt.append("<tr><td>Language</td><td>").append((postcard.getLanguage().equals("es")) ? "Spanish" : "English").append("</td></tr>\r");
			
			//print surgeon's bio for CPSEM's that are not shoulder.  (these will be the only ones populating this field)
			if (event.getSurgeonBioText() != null && event.getSurgeonBioText().length() > 0) 
				rpt.append("<tr><td>Surgeon's Bio</td><td>").append(event.getSurgeonBioText()).append("</td></tr>\r");
			
			rpt.append("<tr><td colspan='2'>&nbsp;</td></tr>");
		}

		rpt.append("<tr><td>Date of Seminar</td><td align='center'>").append(Convert.formatDate(postcard.getEarliestEventDate(), Convert.DATE_LONG)).append("</td></tr>\r");
		rpt.append("<tr><td>RSVP Deadline</td><td align='center'>").append(Convert.formatDate(postcard.getRSVPDate(), Convert.DATE_LONG)).append("</td></tr>\r");
		//rpt.append("<tr><td>Style of Postcard</td><td align='center'>").append(((postcard.getPostcardTypeFlg() == 2) ? "bulleted" : "paragraph")).append("</td></tr>\r");
		rpt.append("<tr><td>Initial Invitation Send Date</td><td style='background-color: yellow;' align='center'>").append(Convert.formatDate(postcard.getPostcardSendDate(), Convert.DATE_LONG)).append("</td></tr>\r");
		//rpt.append("<tr><td>Reminder Invites Send Date:<br><font size='-1'>Reminder cards should have the letter \"R\" at the end of the event# and should not include the RSVP deadline</font></td><td align='center'>").append(Convert.formatDate(postcard.getReminderPostcardSendDate())).append("</td></tr>\r");
		rpt.append("<tr><td colspan='2'>&nbsp;</td></tr>\r");
		rpt.append("<tr><td>Additional Postcards Send Date:<br><font size='-1'>Additional cards should have the letter \"S\" at the end of the seminar#</font></td>");
		rpt.append("<td align='center'>").append(Convert.formatDate(postcard.getAddtlPostcardSendDate(), Convert.DATE_LONG)).append("</td></tr>\r");
		rpt.append("<tr><td valign='top'>Additional Postcards</td><td>");
		
		for (DePuyEventEntryVO eventVO : postcard.getDePuyEvents()) {
			for (DePuyEventAddtlPostcardVO eap : eventVO.getAddtlPostcards()) {
				if (eap.getPostcardQnty() == 0) continue;
				
				rpt.append("<p>Print ").append(eap.getPostcardQnty()).append(" postcards with the surgeon name \"").append(eap.getSurgeonName()).append("\" and seminar ").append(eventVO.getRSVPCode()).append("</p>\r");
			}

		}
		UserDataVO owner = postcard.getOwner();
		rpt.append("Send additional postcards to:<br>").append(StringUtil.checkVal(owner.getFirstName()));
		rpt.append(" ").append(StringUtil.checkVal(owner.getLastName())).append("<br>").append(StringUtil.checkVal(owner.getAddress())).append("<br>");
		rpt.append(StringUtil.checkVal(owner.getCity())).append(", ").append(StringUtil.checkVal(owner.getState())).append(" ").append(StringUtil.checkVal(owner.getZipCode()));
		rpt.append("</td></tr>\r");
		rpt.append("<tr><td colspan='2'>&nbsp;</td></tr>\r");
		rpt.append("<tr><td colspan='2' style='background-color: #ccc;'><b>Legal Filings</td></tr>\r");
		
		//pre-auth stuff, slightly different labeling for Mitek!
		if ("ORTHOVISC".equalsIgnoreCase(postcard.getProductName())) {
			rpt.append("<tr><td>Speaker Agreement</td><td align='center'><a href=\"").append(postcard.getAuthorizationText()).append("\" target='_blank'>").append(postcard.getAuthorizationText()).append("</a></td></tr>\r");
			rpt.append("<tr><td>Venue Location(s)</td><td align='center'>").append(postcard.getVenueText()).append("</td></tr>\r");
			rpt.append("<tr><td>Approving Manager:</td><td align='center'>").append(postcard.getPcAttribute1()).append("</td></tr>\r");
		} else {
			rpt.append("<tr><td>Authorization File</td><td align='center'><a href=\"").append(postcard.getAuthorizationText()).append("\" target='_blank'>").append(postcard.getAuthorizationText()).append("</a></td></tr>\r");
			rpt.append("<tr><td>Speaker's Bio</td><td align='center'><a href=\"").append(postcard.getPresenterBioText()).append("\" target='_blank'>").append(postcard.getPresenterBioText()).append("</a></td></tr>\r");
			rpt.append("<tr><td>Surgeon's CV</td><td align='center'><a href=\"").append(postcard.getPcAttribute4()).append("\" target='_blank'>").append(postcard.getPcAttribute4()).append("</a></td></tr>\r");
			rpt.append("<tr><td>Surgeon's Experience</td><td align='center'>").append(postcard.getPresenterExperienceText()).append("</td></tr>\r");
			rpt.append("<tr><td>Surgeon's Address</td><td align='center'>").append(postcard.getPresenterAddressText()).append("</td></tr>\r");
			rpt.append("<tr><td>Venue Location(s)</td><td align='center'>").append(postcard.getVenueText()).append("</td></tr>\r");
			rpt.append("<tr><td>TGM's Email</td><td align='center'>").append(postcard.getPcAttribute1()).append("</td></tr>\r");
			rpt.append("<tr><td>Sales Rep's Email</td><td align='center'>").append(postcard.getPcAttribute2()).append("</td></tr>\r");
			rpt.append("<tr><td>Surgeon's Email</td><td align='center'>").append(postcard.getPresenterEmailText()).append("</td></tr>\r");
			String attrib3 = "No";
			if (Convert.formatBoolean(postcard.getPcAttribute3())) attrib3 = "Yes";
			rpt.append("<tr><td>The Field Marketing Director has reviewed the Surgeon Guidelines with speaker?</td><td align='center'>").append(attrib3).append("</td></tr>\r");
			
			String complianceReviewed = "No";
			if (Convert.formatBoolean(postcard.getOptInFlag().toString())) complianceReviewed = "Yes";
			rpt.append("<tr><td>Owner has reviewed the Healthcare Compliance Training?</td><td align='center'>").append(complianceReviewed).append("</td></tr>\r");
		}
		
		
		
		//add the Co-Op Ad data
		if (postcard.getCoopAd() != null && postcard.getCoopAd().getCoopAdId() != null) {
			CoopAdVO ad = postcard.getCoopAd();
			rpt.append("<tr><td colspan='2'>&nbsp;</td></tr>\r");
			rpt.append("<tr><td colspan='2' style='background-color: #ccc;'><b>Co-Op Ads Program</td></tr>\r");
			rpt.append("<tr><td>Co-Op Ad Approved:</td><td align='center'>").append((ad.getStatusFlg() == 3) ? "Yes" : "No").append("</td></tr>\r");
			if ("ORTHOVISC".equalsIgnoreCase(postcard.getProductName())) {
				rpt.append("<tr><td>Desired Newspaper #1:</td><td align='center'>").append(StringUtil.checkVal(ad.getNewspaper1Text())).append("</td></tr>\r");
				rpt.append("<tr><td>Desired Newspaper #2:</td><td align='center'>").append(StringUtil.checkVal(ad.getNewspaper2Text())).append("</td></tr>\r");
				rpt.append("<tr><td>Desired Newspaper #3:</td><td align='center'>").append(StringUtil.checkVal(ad.getNewspaper3Text())).append("</td></tr>\r");
			} else {
				rpt.append("<tr><td>Sponsored Newspaper:</td><td align='center'>").append(StringUtil.checkVal(ad.getNewspaper1Text())).append(" (").append(ad.getNewspaper1Phone()).append(")</td></tr>\r");
			}
			rpt.append("<tr><td>Approved Paper:</td><td align='center'>").append(StringUtil.checkVal(ad.getApprovedPaperName())).append("</td></tr>\r");
			rpt.append("<tr><td>Total Cost:</td><td align='center'>").append(ad.getTotalCostNo()).append("</td></tr>\r");
			
			//calculate cost of ad to territory or surgeon
			if ("CFSEM".equalsIgnoreCase(postcard.getDePuyEvents().get(0).getEventTypeCd())) {
				rpt.append("<tr><td>Surgeon approved ad?:</td><td align='center'>").append((ad.getSurgeonStatusFlg() == 1) ? "Yes" : "No").append("</td></tr>\r");
				rpt.append("<tr><td>Surgeon paid for ad?:</td><td align='center'>").append((ad.getStatusFlg() == CoopAdsAction.CLIENT_PAYMENT_RECD) ? "Yes" : "No").append("</td></tr>\r");
				rpt.append("<tr><td>Ad Cost to Surgeon:</td><td align='center'>").append(ad.getCostToRepNo()).append("</td></tr>\r");
				rpt.append("<tr><td>Surgeon Name:</td><td align='center'>").append(ad.getSurgeonName()).append("</td></tr>\r");
				rpt.append("<tr><td>Surgeon Title:</td><td align='center'>").append(ad.getSurgeonTitle()).append("</td></tr>\r");
				rpt.append("<tr><td>Surgeon Photo:</td><td align='center'><a href=\"").append(ad.getSurgeonImageUrl()).append("\" target='_blank'>").append(ad.getSurgeonImageUrl()).append("</a></td></tr>\r");
				rpt.append("<tr><td>Surgical Experience:</td><td align='center'>").append(ad.getSurgicalExperience()).append("</td></tr>\r");
				rpt.append("<tr><td>Clinic Name:</td><td align='center'>").append(ad.getClinicName()).append("</td></tr>\r");
				rpt.append("<tr><td>Clinic Address:</td><td align='center'>").append(ad.getClinicAddress()).append("</td></tr>\r");
				rpt.append("<tr><td>Clinic Hours:</td><td align='center'>").append(ad.getClinicHours()).append("</td></tr>\r");
				rpt.append("<tr><td>Office Phone:</td><td align='center'>").append(ad.getClinicPhone()).append("</td></tr>\r");
				rpt.append("<tr><td>Surgeon/Office Email(s):</td><td align='center'>").append(ad.getSurgeonEmail()).append("</td></tr>\r");
				
			} else {
				rpt.append("<tr><td>Ad Cost to Territory:</td><td align='center'>").append(ad.getCostToRepNo()).append("</td></tr>\r");
			}
			
			rpt.append("<tr><td>Seminar(s) (in desired order):</td><td align='center'>").append(ad.getEventCodes()).append("</td></tr>\r");
			rpt.append("<tr><td>Ad File:</td><td align='center'><a href=\"").append(ad.getAdFileUrl()).append("\" target='_blank'>").append(ad.getAdFileUrl()).append("</a></td></tr>\r");
		}
		//add the Radio Ad data
		if (postcard.getRadioAd() != null && postcard.getRadioAd().getCoopAdId() != null) {
			CoopAdVO ad = postcard.getRadioAd();
			rpt.append("<tr><td colspan='2'>&nbsp;</td></tr>\r");
			rpt.append("<tr><td colspan='2' style='background-color: #ccc;'><b>Radio Ad</td></tr>\r");
			rpt.append("<tr><td>Radio Station:</td><td align='center'>").append(StringUtil.checkVal(ad.getNewspaper1Text())).append(" (").append(ad.getNewspaper1Phone()).append(")</td></tr>\r");
			rpt.append("<tr><td>Contact Name:</td><td align='center'>").append(StringUtil.checkVal(ad.getNewspaper2Text())).append(" (").append(ad.getNewspaper2Phone()).append(")</td></tr>\r");
			rpt.append("<tr><td>Ad Deadline:</td><td align='center'>").append(StringUtil.checkVal(ad.getAdDatesText())).append("</td></tr>\r");
			rpt.append("<tr><td>Seminar(s) (in desired order):</td><td align='center'>").append(ad.getEventCodes()).append("</td></tr>\r");
			
		}
		rpt.append(this.getFooter());
		
		return rpt.toString().getBytes();
	}
	
	private StringBuffer getHeader() {
		StringBuffer hdr = new StringBuffer();
		hdr.append("<table border='1'>\r");
		hdr.append("<tr><td colspan='2' style='background-color: #ccc;'><b>Seminar Mailing Information</b></td></tr>\r");
		return hdr;
	}
	
	private StringBuffer getFooter() {
		return new StringBuffer("</table>");
	}
		
}
