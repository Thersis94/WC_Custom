package com.fastsigns.action;

import java.util.Map;

import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.contact.ContactDataModuleVO;

/*****************************************************************************
 <p><b>Title</b>: EventPostalLeadsReportVO.java</p>
 <p>Description: <b/>compiles leads data for postal and email sends</p>
 <p>Copyright: Copyright (c) 2000 - 2006 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Nov 6, 2006
 ***************************************************************************/

public class SAFReportVO extends AbstractSBReportVO {
    private static final long serialVersionUID = 1l;
    private ContactDataModuleVO vo = null;


    /**
     * 
     */
    public SAFReportVO() {
        super();
        setContentType("application/vnd.ms-excel");
        isHeaderAttachment(Boolean.TRUE);
        setFileName("SAF-Report.xls");
    }
    
    /**
     * format the Contact Submission into a meaningful report
     * @throws SQLException
     */
    public void setData(Object o) {
    	vo = (ContactDataModuleVO) o;
    }
    
	public byte[] generateReport() {
		log.debug("starting generateReport()");
		setFileName("SAF Report - " + vo.getFirstName() + " " + vo.getLastName() + ".xls");
		Map<String, String> d = vo.getExtData();

		StringBuffer rpt = new StringBuffer(this.getHeader());
		rpt.append("<tr><td>Name: </td><td>").append(vo.getFirstName()).append(" ").append(vo.getLastName()).append("</td></tr>");
		rpt.append("<tr><td>Email: </td><td>").append(vo.getEmailAddress()).append("</td></tr>");
		rpt.append("<tr><td>Company: </td><td>").append(StringUtil.checkVal(d.get("c0a80237b0c703fd4020174ce3a74dfd"))).append("</td></tr>");

		PhoneNumberFormat pnf = new PhoneNumberFormat(vo.getMainPhone(), PhoneNumberFormat.PAREN_FORMATTING);
		rpt.append("<tr><td>Phone: </td><td>").append(pnf.getFormattedNumber()).append("</td></tr>");

		pnf = new PhoneNumberFormat(StringUtil.checkVal(d.get("c0a8023721541f6fe2ace856c70113f0")), PhoneNumberFormat.PAREN_FORMATTING);
		rpt.append("<tr><td>Fax: </td><td>").append(pnf.getFormattedNumber()).append("</td></tr>");

		rpt.append("<tr><td>Address: </td><td>").append(vo.getLocation().getCompleteAddress()).append("</td></tr>");
		rpt.append("<tr><td>Sales Contact: </td><td>").append(StringUtil.checkVal(d.get("c0a8023721541f6fe2ace856c69913f0"))).append("</td></tr>");
		rpt.append("<tr><td>Sign Type: </td><td>").append(StringUtil.checkVal(d.get("c0a80237214f632f9a16a15e3b629c58"))).append("</td></tr>");
		rpt.append("<tr><td>Sign Quality: </td><td>").append(StringUtil.checkVal(d.get("c0a8023721541f6fe2ace856c52213f0"))).append("</td></tr>");
		rpt.append("<tr><td>Requested Completion Date: </td><td>").append(StringUtil.checkVal(d.get("c0a80237215abba6c0da9428fd936f8c"))).append("</td></tr>");
		rpt.append("<tr><td>Customer Providing Artwork?: </td><td>").append(StringUtil.checkVal(d.get("c0a8023721565d1bdd5add6a42b2f3c8"))).append("</td></tr>");
		rpt.append("<tr><td>Desired Width: </td><td>").append(StringUtil.checkVal(d.get("c0a8023721587c01854c54f919f8073"))).append("</td></tr>");
		rpt.append("<tr><td>Desired Height: </td><td>").append(StringUtil.checkVal(d.get("c0a802372158184c63997ded6321a2df"))).append("</td></tr>");
		rpt.append("<tr><td>Brief Project Description: </td><td>").append(StringUtil.checkVal(d.get("c0a802372158e14b3400741780e58cf8"))).append("</td></tr>");

		rpt.append("<tr><td colspan='3'>File(s): </td></tr>");
		//iter the files
		try{
		String[] files = d.get("7f000101580d3209dd677866f73ed913").split("\r\n");
		for (String s : files) {
			String[] data = s.split(", ");
			if (data == null || data.length < 2) continue;
			rpt.append("<tr><td>&nbsp;</td><td>").append(data[0]).append("</td><td>").append(data[1]).append("</td></tr>");
		}
		} catch(NullPointerException e){
			log.debug("Error, could not retrieve files." + e);
		}

		
		rpt.append("<tr><td>&nbsp;</td></tr>");
		rpt.append("<tr><td>(internal use only)</td></tr>");
		rpt.append("<tr><td>Transaction Stage: </td><td>").append(StringUtil.checkVal(d.get("7f0001019c4932bc3629f3987f43b5ec"))).append("</td></tr>");
		rpt.append("<tr><td>Transaction Errors/Notes: </td><td>").append(StringUtil.checkVal(d.get("7f000101ed12428e6f503d8d58e4ef90"))).append("</td></tr>");


		rpt.append(this.getFooter());
		log.debug("report=" + rpt);
		return rpt.toString().getBytes();
	}
	
	private StringBuffer getHeader() {
		StringBuffer hdr = new StringBuffer();
		hdr.append("<table border='1'>\r");
		hdr.append("<tr><td colspan='2' style='background-color: #ccc;'><b>Send-a-File/Request-a-Quote Report</b></td></tr>\r");
		return hdr;
	}
	
	private StringBuffer getFooter() {
		return new StringBuffer("</table>");
	}
	
}
