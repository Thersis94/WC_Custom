package com.ansmed.sb.report;

//JDK 1.6.0
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

// SB Libs
import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;

// SB ANS Libs
import com.ansmed.sb.physician.SurgeonVO;

//PD4ML libs
import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.pdf.ITextRenderer;

/****************************************************************************
 * <b>Title</b>:ImplanterReportVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Sept 22, 2010
 * <b>Change Log: </b>
 ****************************************************************************/
public class ImplanterReportVO extends AbstractSBReportVO {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String PDF = "application/pdf";
	private static final String BASE_RESOURCE_URL = "http://www.siliconmtn.com";
	
	private List<SurgeonVO> data = null;
	
	/**
	 * 
	 */
	public ImplanterReportVO() {
        super();		
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#generateReport()
	 */
	@Override
	public byte[] generateReport() {		
		if (this.getContentType().equalsIgnoreCase(PDF)) {
			return this.generatePdfReport();
		} else {
			return this.generateStandardReport().toString().getBytes();
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object info) {
		if (info instanceof List<?>) {
			data = (List<SurgeonVO>)info;
		}
	}
	
	@Override
	public void setFileName(String f) {
		super.setFileName(f);
		if (f.endsWith(".xls")) {
	        setContentType("application/vnd.ms-excel");
	        isHeaderAttachment(Boolean.TRUE);
		} else if (f.endsWith(".pdf")) {
	        setContentType(PDF);
	        isHeaderAttachment(Boolean.TRUE);
		} else {
			setContentType("text/html");
		}
	}
	
	/**
	 * Returns a formatted phone number or blank space.
	 * @param number
	 * @return
	 */
	private String getFormattedNumber(String number) {
		if (number == null) return "";
		PhoneNumberFormat pnf = new PhoneNumberFormat(number,3);
		return pnf.getFormattedNumber();
	}
	
	/**
	 * Creates the report in PDF format
	 * @return
	 */
	private byte[] generatePdfReport() {
		StringBuffer sb = new StringBuffer();
		//set the header and set up the table
		this.addPdfHeader(sb);
		sb.append("<table class=\"sForm\" style=\"font-size:14;\" border=\"0\">\n");
		
		//loop/add the data
		if (data != null && data.size() > 0) {
			int count = 0;
			for(SurgeonVO sv : data) {
				count++;
				this.addPdfRow(sv, sb, count);
			}
		} else {
			sb.append("<tr><td colspan=\"6\">No physician information found.</td></tr>");
		}
		
		// add tagline, close table and close HTML
		this.addTagLine(sb);
		sb.append("</table></body></html>");
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	// Render the PDF file and attach to the output stream
		try {
			byte[] b = sb.toString().getBytes();
			
			// pass to Tidy to create a clean HTML doc
			Tidy tidy = new Tidy();
			tidy.setXHTML(true);    			    
			ByteArrayInputStream bais = new ByteArrayInputStream(b);
			Document doc = tidy.parseDOM(bais, baos);
			
			//reset the output stream so we can reuse it when rendering the document.
			baos.reset();
			
			// render the document
	        ITextRenderer renderer = new ITextRenderer();
	        String fontPath = (String)getAttributes().get("fontPath");
	        log.debug("fontPath: " + fontPath);
			renderer.getFontResolver().addFont(fontPath + "font/trebuchet/Trebuchet-MS.TTF", true);
			renderer.getFontResolver().addFont(fontPath + "font/trebuchet/Trebuchet-MS-Bold.TTF", true);
		    renderer.setDocument(doc, BASE_RESOURCE_URL);
	        renderer.layout();
	        renderer.createPDF(baos);
		} catch (Exception e) {
			log.error("Error creating PDF File", e);
		}
		return baos.toByteArray();
	}
	
	/**
	 * Creates the report in standard format
	 * @return
	 */
	private StringBuffer generateStandardReport() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("<table border=\"1\">");
		this.addStandardHeader(sb);
		
		if (data != null && data.size() > 0) {
			//loop/add the data
			for(SurgeonVO sv : data) {
				this.addStandardRow(sv, sb);
			}
		} else {
			sb.append("<tr><td colspan=\"6\">No physician information found.</td></tr>");
		}
		
		//Close the table.
		sb.append("</table>");	
		log.debug("report size: " + sb.length());
		return sb;
	}
	
	/**
	 * Sets the header for the PDF version of the report.
	 * @param sb
	 */
	private void addPdfHeader(StringBuffer sb) {
		sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
		sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n");
		sb.append("<head>\n");
		sb.append("<title></title>\n");
		this.addPdfStyles(sb);
		sb.append("</head>\n");
		sb.append("<body>\n");
	}
	
	/**
	 * Adds styles used by the PDF version of the report.
	 * @param sb
	 */
	private void addPdfStyles(StringBuffer sb) {
		// Standard Form Layouts
		sb.append("<style type='text/css' media='print'>\n");
		sb.append("body { margin: 0; padding: 0; font: normal 9pt \"Trebuchet MS\", ");
		sb.append("Verdana, Arial, Helvetica; color: #000; line-height: 1.3em; }\n");		
		sb.append("table.sForm { width:100%; margin-left:10px; border-collapse:collapse; }\n");
		sb.append("table.sForm td { vertical-align: top; }\n");
		sb.append("td.sForm { text-align:right; font-weight:bold; }\n");
		sb.append("td.sFormLeft { text-align:left; font-weight:bold; white-space: nowrap; }\n");
		sb.append("tr.row_0 { background:#eaedf3; height:20px; }\n");
		sb.append("tr.row_1 { background:#fff; height:20px; }\n");
		sb.append("table.sForm th,  table.sForm th a { color: #fff;	background:#eb7001;	");
		sb.append("height:20px; text-decoration:none;font-weight:bold; ");
		sb.append("font-family: \"Trebuchet MS\", Verdana, Arial, Helvetica; text-align:left; }\n");
		sb.append(".implanter h2 { font-family: \"Trebuchet MS\", Verdana, Arial, Helvetica; ");
		sb.append("font-size: 130%;	font-weight: bold; color: #006c56; padding: 0 0 2px 0; margin: 0; }\n");		
		sb.append("</style>\n");
	}
	
	/**
	 * Adds tag line text to report
	 * @param sb
	 */
	private void addTagLine(StringBuffer sb) {
		// adds the SJM tagline
		sb.append("<tr><td colspan=\"5\" style=\"font-size:11px;\">");
		sb.append("This list is compiled by SJM as a reference tool for locating ");
		sb.append("certain physicians. The physicians included on this list are ");
		sb.append("those<br /> who have used and are familiar with SJM' products. ");
		sb.append("No physician has paid or received a fee to be included in our database.</td></tr>\n");
	}
	
	/**
	 * Adds a data row to the PDF version of the report.
	 * @param sv
	 * @param sb
	 * @param count
	 */
	private void addPdfRow(SurgeonVO sv, StringBuffer sb, int count) {
		if ((count % 8) == 1) {
			if (count > 1) {
				this.addTagLine(sb);
			} 
			sb.append("<tr><td colspan=\"5\"><table><tr><td><img src=\"/binary/org/ANS-Extranet/SalesNet/images/common/SJM_logo.jpg\" border=\"0\" alt=\"SJM Logo\" title=\"SJM Logo\"/></td></tr>\n");
			sb.append("<tr><td style=\"vertical-align:middle;\" class=\"implanter\"><h2>&nbsp;Local Area Spinal Cord Stimulator Implanters</h2></td></tr>\n");
			sb.append("</table></td></tr><tr><td colspan=\"5\">&nbsp;</td></tr>\n");
		}
	
		if ((count % 2) == 1) sb.append("<tr style=\"height:175px;\">");
		
		// add physician data block
		sb.append("<td style=\"width:15px;\">&nbsp;</td>");
		sb.append("<td style=\"width:400px;\" valign=\"top\">");
		sb.append("<b>").append(sv.getFirstName()).append("&nbsp;").append(sv.getLastName());
		sb.append(", ").append(sv.getTitle()).append("</b><br/>");
		if (StringUtil.checkVal(sv.getClinic().getClinicName()).length() > 0) sb.append(sv.getClinic().getClinicName()).append("<br/>");
		if (StringUtil.checkVal(sv.getClinic().getAddress()).length() > 0) sb.append(sv.getClinic().getAddress()).append("<br/>");
		if (StringUtil.checkVal(sv.getClinic().getAddress2()).length() > 0) sb.append(sv.getClinic().getAddress2()).append("<br/>");
		if (StringUtil.checkVal(sv.getClinic().getCity()).length() > 0) sb.append(sv.getClinic().getCity()).append(",&nbsp;");
		if (StringUtil.checkVal(sv.getClinic().getState()).length() > 0) sb.append(sv.getClinic().getState()).append(",&nbsp;");
		if (StringUtil.checkVal(sv.getClinic().getZipCode()).length() > 0) sb.append(sv.getClinic().getZipCode()).append("<br/>");
		if (StringUtil.checkVal(sv.getClinic().getWorkNumber()).length() > 0) sb.append(getFormattedNumber(sv.getClinic().getWorkNumber())).append("<br/>");
		if (StringUtil.checkVal(sv.getClinic().getFaxNumber()).length() > 0) sb.append(getFormattedNumber(sv.getClinic().getFaxNumber())).append("<br/>");
		
		if (StringUtil.checkVal(sv.getWebsite()).length() > 0) sb.append("http://").append(sv.getWebsite()).append("<br/>");
		if (sv.getSpanishFlag() == 1) sb.append("Fluent in Spanish<br/>");
		if (StringUtil.checkVal(sv.getSpecialtyName()).length() > 0) sb.append("Specialty: ").append(sv.getSpecialtyName()).append("<br/>");
		sb.append("</td>");
		
		if ((count % 2) == 0) sb.append("<td style=\"width:15px;\">&nbsp;</td></tr>\n");

	}
	
	/**
	 * Adds the standard header text to the report.
	 * @param sb
	 */
	private void addStandardHeader(StringBuffer sb) {
		//adds a standard HTML table header
		sb.append("<tr>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Physician Name</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Clinic Location</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Address</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">City</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">State</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Zip Code</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Phone</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Fax</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Website</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Fluent in Spanish</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Specialty</td>");
		sb.append("</tr>");
	}
	
	/**
	 * Adds a data row to the standard version of the report.
	 * @param sv
	 * @param sb
	 */
	private void addStandardRow(SurgeonVO sv, StringBuffer sb) {
		sb.append("<tr>");
		sb.append("<td>").append(sv.getFirstName()).append(" ").append(sv.getLastName());
		if (StringUtil.checkVal(sv.getTitle()).length() > 0) {
			sb.append(", ").append(sv.getTitle());
		}
		sb.append("</td>");
		sb.append("<td>").append(sv.getClinic().getClinicName()).append("</td>");
		sb.append("<td>").append(sv.getClinic().getAddress());
		if (StringUtil.checkVal(sv.getClinic().getAddress2()).length() > 0) {
			sb.append(", ").append(sv.getClinic().getAddress2());
		}
		sb.append("</td>");
		
		sb.append("<td>").append(sv.getClinic().getCity()).append("</td>");
		sb.append("<td>").append(sv.getClinic().getState()).append("</td>");
		sb.append("<td>").append(sv.getClinic().getZipCode()).append("</td>");
		
		sb.append("<td>").append(this.getFormattedNumber(sv.getClinic().getWorkNumber())).append("</td>");
		sb.append("<td>").append(this.getFormattedNumber(sv.getClinic().getFaxNumber())).append("</td>");
		sb.append("<td>");
		if (StringUtil.checkVal(sv.getWebsite()).length() > 0) {
			sb.append("http://").append(StringUtil.checkVal(sv.getWebsite()));
		}
		sb.append("</td>");
		sb.append("<td>");
		if (sv.getSpanishFlag() != null && sv.getSpanishFlag() == 1) {
			sb.append("Yes");
		}
		sb.append("</td>");
		sb.append("<td>").append(StringUtil.checkVal(sv.getSpecialtyName())).append("</td>");
		sb.append("</tr>");
	}
}