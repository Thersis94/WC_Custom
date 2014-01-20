package com.depuy.events_v2.vo.report;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.event.EventRsvpComparator;
import com.smt.sitebuilder.action.event.vo.EventRsvpVO;

/****************************************************************************
 * <b>Title</b>: SigninReportVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Mar 8, 2013
 ****************************************************************************/
public class SigninReportVO extends AbstractSBReportVO {
	private static final long serialVersionUID = 1L;
	private String eventName = null;
	private String eventAddress = null;
	private String eventCode = null;
	private String eventDate = null;
	private List<EventRsvpVO> dataSet = null;
	
	public SigninReportVO(SMTServletRequest req) {
        super();
        setContentType("application/ms-word");
        isHeaderAttachment(Boolean.TRUE);
        setFileName("Sign-in Sheet.doc");
		
        //get what we need off the request and hold it until the report gets generated
        eventName = req.getParameter("eventNm").trim();
        eventAddress = req.getParameter("eventLocn");
        eventCode = req.getParameter("eventCode");
        eventDate = req.getParameter("eventDt");
        log.debug(eventName + "|" + eventAddress + "|" + eventCode + "|" + eventDate);
	}
	
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		StringBuilder doc = new StringBuilder();
		doc.append("<html><head></head><body>\n");
		doc.append("<img src='http://events.depuy.com/binary/org/DEPUY/DPY_EVENTS/images/logo.png' width='200' height='52'/>\n");
		
		doc.append("<br/><center><h3>");
		doc.append("Seminar ").append(eventCode).append(" Sign in Sheet</h3>");
		doc.append("<p>Please sign in below<p/>\n");
		doc.append("<p>").append(eventName).append(", \n");
		doc.append(eventAddress).append("</p>\n");
		Date d = Convert.formatDate(Convert.DATE_SLASH_SHORT_PATTERN, eventDate);
		doc.append("<p>").append(Convert.formatDate(d, Convert.DATE_LONG)).append("</p>\n");		
		doc.append("</center>");
		
		doc.append("<table cellspacing='0' cellpadding='0' border='1' bordercolor='black' width='100%'>\n");
		doc.append("<tr><th>Last Name</th><th>First Name</th><th>Signature</th><th># of attendees</th></tr>\n");
		
		for (EventRsvpVO vo : dataSet) {
			doc.append("<tr><td style='padding: 5px 10px;'>").append(StringUtil.capitalizePhrase(vo.getUser().getLastName())).append("</td>");
			doc.append("<td style='padding: 5px 10px;'>").append(StringUtil.capitalizePhrase(vo.getUser().getFirstName())).append("</td>");
			doc.append("<td width='250' style='padding: 5px 10px;'> </td><td width='80' style='padding: 5px 10px;'> </td></tr>\n");
		}
		
		doc.append("</table>\n");
		doc.append("</body></html>\n");
		return doc.toString().getBytes();
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#setData(java.lang.Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void setData(Object o) {
		this.dataSet = (List<EventRsvpVO>) o;
		
		//sort the list by user's name
		Collections.sort(dataSet, new EventRsvpComparator());
		
	}
}
