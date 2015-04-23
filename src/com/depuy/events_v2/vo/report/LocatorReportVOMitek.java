package com.depuy.events_v2.vo.report;

import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: LocatorReportVO.java<p/>
 * <b>Description: Makes a call to the AAMD surgeon locator web service and
 * downloads the appropriate information in XML format.  The data is formatted and returned</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 2.0
 * @since Feb 3, 2014
 ****************************************************************************/
public class LocatorReportVOMitek extends LocatorReportVO {
	private static final long serialVersionUID = 4268751733367041314L;

	public LocatorReportVOMitek() {
		super();
	}

	@Override
	protected StringBuilder getHeader() {
		StringBuilder sb = new StringBuilder(500);
		sb.append("<html><head><title>Surgeon Locator Report</title></head><body>");
		sb.append("<u>Today's Seminar:</u><br/>"); 
		sb.append(Convert.formatDate(event.getStartDate(),Convert.DATE_LONG)).append("<br/>");
		sb.append(event.getLocationDesc()).append("<br/>");
		sb.append("Speaker: ").append(sem.getSurgeon().getSurgeonName()).append("<br/><br/>");
		sb.append(event.getEventName()).append(", ").append(event.getCityName()).append(", ").append(event.getStateCode());

		sb.append("<h1>LOCAL ORTHOPAEDIC SURGEONS</h1>\n");
		
		// Load the header into
		sb.append("<p>We are pleased to supply you with a list of orthopaedic surgeons in your area who use DePuy Synthes products.  ");
		sb.append("While our database of orthopaedic surgeons is large, it is not a complete ");
		sb.append("listing of all orthopaedic surgeons in your area.  A surgeon's use of ");
		sb.append("DePuy Synthes products is the sole criterion for being listed below. No orthopaedic ");
		sb.append("surgeon has paid a fee to participate.  DePuy Synthes Inc., ");
		sb.append("does not make any recommendation or referral regarding any of these specific surgeons.</p>");
		sb.append("<p>For general information about DePuy Synthes, visit www.depuysynthes.com</p>");
		
		return sb;
	}

	
	/**
	 * Builds the url for the locator call
	 * @param address
	 * @param city
	 * @param state
	 * @param zip
	 * @return URL Formatted for AAMD Locator Request
	 */
	@Override
	protected String buildUrl() {

		StringBuilder s = new StringBuilder();
		s.append("http://www.allaboutmydoc.com/AAMD/locator?");
		s.append("display_template=/xml_display.jsp&company=1");
		s.append("&site_location=PATIENT_ACTIVATION&accept=true&country=MK&language=en");
		s.append("&address=").append(encode(event.getAddressText()));
		s.append("&city=").append(encode(event.getCityName()));
		s.append("&state=").append(encode(event.getStateCode()));
		s.append("&zip=").append(encode(event.getZipCode()));
		//if (productId.equals("4")) { //hip events query by specialty, knee by product
			s.append("&product=&specialty=").append(sem.getJointCodes());
		//} else {
			//s.append("&specialty=&product=").append(productId);
		//}
		s.append("&radius=").append(radius);
		s.append("&order=last");
		s.append("&resultCount=10");
		
		log.info("URL: " + s);
		return s.toString();
	}			
}