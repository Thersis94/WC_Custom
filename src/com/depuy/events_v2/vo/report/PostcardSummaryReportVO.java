package com.depuy.events_v2.vo.report;

import com.depuy.events.vo.CoopAdVO;
import com.depuy.events_v2.CoopAdsActionV2;
import com.depuy.events_v2.vo.DePuyEventSeminarVO;
import com.depuy.events_v2.vo.DePuyEventSurgeonVO;
import com.depuy.events_v2.vo.PersonVO;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.event.vo.EventEntryVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.security.UserDataVO;

/*****************************************************************************
 <p><b>Title</b>: PostcardSummaryReportVO.java</p>
 <p>A comprehensive summary for the entire Seminar and all data points.</p>
 <p>Copyright: Copyright (c) 2000 - 2014 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Jane 20, 2014
 ***************************************************************************/

public class PostcardSummaryReportVO extends AbstractSBReportVO {
    private static final long serialVersionUID = 1l;
    private DePuyEventSeminarVO sem = new DePuyEventSeminarVO();

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
	    this.sem = (DePuyEventSeminarVO) o;
    }
    
	public byte[] generateReport() {
		log.debug("starting PostcardSummaryReport");
		
		StringBuffer rpt = new StringBuffer(this.getHeader());
		rpt.append("<tr><td>Product</td><td align='center'>").append(sem.getJointLabel()).append("</td></tr>\r");
		rpt.append("<tr><td>Postcard Type</td><td align='center'>").append(sem.getEvents().get(0).getEventTypeDesc()).append("</td></tr>\r");
		rpt.append("<tr><td>Seminar Promotion #</td><td align='center'>").append(sem.getRSVPCodes()).append("</td></tr>\r");
		for (PersonVO p : sem.getPeople()) {
			rpt.append("<tr><td>").append(p.getRoleCode()).append("</td><td align='center'>")
				.append(StringUtil.checkVal(p.getFirstName())).append(" ").append(StringUtil.checkVal(p.getLastName()))
				.append(" (").append(p.getEmailAddress()).append(")").append("</td></tr>\r");
		}
		
		for (EventEntryVO event : sem.getEvents()) {
			rpt.append("<tr><td colspan='2'>&nbsp;</td></tr>");
			rpt.append("<tr><td>Seminar #").append(event.getRSVPCode()).append("</td><td>").append(event.getEventName()).append("</td></tr>\r");
			rpt.append("<tr><td>Seminar Date/Time</td><td>").append(Convert.formatDate(event.getStartDate(),Convert.DATE_LONG)).append(" ").append(event.getLocationDesc()).append("</td></tr>");
			rpt.append("<tr><td>Seminar Location</td><td>").append(event.getCityName()).append(", ").append(event.getStateCode()).append(" ").append(event.getZipCode()).append("</td></tr>\r");
			rpt.append("<tr><td>Product</td><td>").append(sem.getJointLabel()).append("</td></tr>\r");
			rpt.append("<tr><td>Venue Location</td><td align='center'>").append(event.getEventDesc()).append("</td></tr>\r");
			rpt.append("<tr><td>Venue Name</td><td align='center'>").append(event.getEventName()).append("</td></tr>\r");
			rpt.append("<tr><td>Refreshment Choice</td><td align='center'>").append(event.getServiceText()).append("</td></tr>\r");
			rpt.append("<tr><td>Venue Address</td><td align='center'>").append(event.getAddressText())
						.append("<br/>").append(event.getAddress2Text())
						.append("<br/>").append(event.getCityName()).append(" " ).append(event.getStateCode()).append(", " ).append(event.getZipCode()).append("</td></tr>\r");
			rpt.append("<tr><td colspan='2'>&nbsp;</td></tr>");
		}
		rpt.append("<tr><td>Date of Seminar</td><td align='center'>").append(Convert.formatDate(sem.getEarliestEventDate(), Convert.DATE_LONG)).append("</td></tr>\r");
		rpt.append("<tr><td>RSVP Deadline</td><td align='center'>").append(Convert.formatDate(sem.getRSVPDate(), Convert.DATE_LONG)).append("</td></tr>\r");
		rpt.append("<tr><td>Initial Invitation Send Date</td><td style='background-color: yellow;' align='center'>").append(Convert.formatDate(sem.getPostcardSendDate(), Convert.DATE_LONG)).append("</td></tr>\r");
		rpt.append("<tr><td>Additional Postcards Send Date:<br><font size='-1'>Additional cards should have the letter \"S\" at the end of the seminar#</font></td>");
		rpt.append("<td align='center'>").append(Convert.formatDate(sem.getAddtlPostcardSendDate(), Convert.DATE_LONG)).append("</td></tr>\r");
		rpt.append("<tr><td valign='top'>Additional Postcards</td><td>");
		UserDataVO owner = sem.getOwner();
		rpt.append("Send additional postcards to:<br>").append(StringUtil.checkVal(owner.getFirstName()));
		rpt.append(" ").append(StringUtil.checkVal(owner.getLastName())).append("<br>").append(StringUtil.checkVal(owner.getAddress())).append("<br>");
		rpt.append(StringUtil.checkVal(owner.getCity())).append(", ").append(StringUtil.checkVal(owner.getState())).append(" ").append(StringUtil.checkVal(owner.getZipCode()));
		rpt.append("</td></tr>\r");
		rpt.append("<tr><td colspan='2'>&nbsp;</td></tr>\r");
		
		
		rpt.append("<tr><td colspan='2' style='background-color: #ccc;'><b>Speaker Information</td></tr>\r");
		for (DePuyEventSurgeonVO surg : sem.getSurgeonList()) {
			rpt.append("<tr><td>Speaker Name:</td><td align='center'>").append(surg.getSurgeonName()).append("</td></tr>\r");
			rpt.append("<tr><td>The Field Marketing Director has reviewed the Speaker Guidelines with speaker?:</td><td align='center'>").append(surg.getSeenGuidelinesFlg() == 1 ? "yes" : "no").append("</td></tr>\r");
			rpt.append("<tr><td>Years practicing:</td><td align='center'>").append(surg.getExperienceYrs()).append("</td></tr>\r");
			rpt.append("<tr><td>Years at current practice:</td><td align='center'>").append(surg.getPractYrs()).append("</td></tr>\r");
			rpt.append("<tr><td>Employed by hospital?:</td><td align='center'>").append(surg.getHospEmployeeFlg() == 1 ? "yes" : "no").append("</td></tr>\r");
			rpt.append("<tr><td>Hospital Address:</td><td align='center'>").append(surg.getHospAddress()).append("</td></tr>\r");
			rpt.append("<tr><td>Practice Address:</td><td align='center'>").append(surg.getPractLocation().getFormattedLocation()).append("</td></tr>\r");
			rpt.append("<tr><td>Practice Phone:</td><td align='center'>").append(surg.getPractPhone()).append("</td></tr>\r");
			rpt.append("<tr><td>Speaker/Office Email(s):</td><td align='center'>").append(surg.getPractEmail()).append("</td></tr>\r");
			rpt.append("<tr><td>Secondary Contact:</td><td align='center'>").append(surg.getSecPhone()).append("</td></tr>\r");
			rpt.append("<tr><td>Secondary Contact Email:</td><td align='center'>").append(surg.getSecEmail()).append("</td></tr>\r");
			rpt.append("<tr><td>Practice Website:</td><td align='center'>").append(StringUtil.checkVal(surg.getPractWebsite())).append("</td></tr>\r");
			rpt.append("<tr><td>Speaker Photo:</td><td align='center'>").append(surg.getLogoFileUrl()).append("</td></tr>\r");
			rpt.append("<tr><td>Speaker Bio:</td><td align='center'>").append(StringUtil.checkVal(surg.getSurgeonBio())).append("</td></tr>\r");
			rpt.append("<tr><td colspan='2'>&nbsp;</td></tr>\r");
		}
		
		
		rpt.append("<tr><td colspan='2' style='background-color: #ccc;'><b>Ad Information</td></tr>\r");
		
		//add the Co-Op Ad data
		if (sem.getAllAds() != null && !sem.getAllAds().isEmpty() ) {
			for (CoopAdVO ad : sem.getAllAds() ){
				int adSts = Convert.formatInteger(ad.getStatusFlg(), 0).intValue();
				
				rpt.append("<tr><td colspan='2'>&nbsp;</td></tr>\r");
				rpt.append("<tr><td colspan='2' style='background-color: #ccc;'><b>Newspaper Ad</td></tr>\r");
				rpt.append("<tr><td>Ad Type:</td><td align='center'>").append(StringUtil.checkVal(ad.getAdType())).append("</td></tr>\r");
				rpt.append("<tr><td>Sponsored Newspaper:</td><td align='center'>").append(StringUtil.checkVal(ad.getNewspaper1Text())).append(" (").append(ad.getNewspaper1Phone()).append(")</td></tr>\r");
				rpt.append("<tr><td>Coordinator approved ad?:</td><td align='center'>").append((adSts == CoopAdsActionV2.CLIENT_APPROVED_AD) ? "Yes" : "No").append("</td></tr>\r");
				rpt.append("<tr><td>Approved Paper:</td><td align='center'>").append(StringUtil.checkVal(ad.getApprovedPaperName())).append("</td></tr>\r");
				rpt.append("<tr><td>Total Cost:</td><td align='center'>").append(ad.getTotalCostNo()).append("</td></tr>\r");
				//calculate cost of ad to territory or surgeon
				if ("CFSEM".equalsIgnoreCase(sem.getEvents().get(0).getEventTypeCd())) {
					int surgSts = Convert.formatInteger(ad.getSurgeonStatusFlg(), 0).intValue();
					rpt.append("<tr><td>Speaker approved ad?:</td><td align='center'>").append((surgSts == CoopAdsActionV2.SURG_APPROVED_AD) ? "Yes" : "No").append("</td></tr>\r");
					rpt.append("<tr><td>Speaker paid for ad?:</td><td align='center'>").append((surgSts == CoopAdsActionV2.SURG_PAID_AD) ? "Yes" : "No").append("</td></tr>\r");
					rpt.append("<tr><td>Ad Cost to Speaker:</td><td align='center'>").append(ad.getCostToRepNo()).append("</td></tr>\r");
				} else {
					rpt.append("<tr><td>Ad Cost to Territory:</td><td align='center'>").append(ad.getCostToRepNo()).append("</td></tr>\r");
				}
				rpt.append("<tr><td>Ad File:</td><td align='center'><a href=\"").append(sem.getBaseUrl()).append("/ads/").append(ad.getAdFileUrl()).append("\" target='_blank'>").append(ad.getAdFileUrl()).append("</a></td></tr>\r");
			}
		}
		//add the Radio Ad data
//		if (sem.getRadioAd() != null && sem.getRadioAd().getCoopAdId() != null) {
//			CoopAdVO ad = sem.getRadioAd();
//			rpt.append("<tr><td colspan='2'>&nbsp;</td></tr>\r");
//			rpt.append("<tr><td colspan='2' style='background-color: #ccc;'><b>Radio Ad</td></tr>\r");
//			rpt.append("<tr><td>Radio Station:</td><td align='center'>").append(StringUtil.checkVal(ad.getNewspaper1Text())).append(" (").append(StringUtil.checkVal(ad.getNewspaper1Phone())).append(")</td></tr>\r");
//			rpt.append("<tr><td>Contact Name:</td><td align='center'>").append(StringUtil.checkVal(ad.getNewspaper2Text())).append(" (").append(StringUtil.checkVal(ad.getNewspaper2Phone())).append(")</td></tr>\r");
//			rpt.append("<tr><td>Ad Deadline:</td><td align='center'>").append(StringUtil.checkVal(ad.getAdDatesText())).append("</td></tr>\r");
//		}
		
		rpt.append(this.getFooter());
		return rpt.toString().getBytes();
	}
	
	private StringBuffer getHeader() {
		StringBuffer hdr = new StringBuffer();
		hdr.append("<table border='1'>\r");
		hdr.append("<tr><td colspan='2' style='background-color: #ccc;'><b>Seminar Information</b></td></tr>\r");
		return hdr;
	}
	
	private StringBuffer getFooter() {
		return new StringBuffer("</table>");
	}
		
}
