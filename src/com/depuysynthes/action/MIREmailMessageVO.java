package com.depuysynthes.action;

import java.util.Map;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.databean.FilePartDataBean;

import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/****************************************************************************
 * <b>Title:</b> MIREmailMessageVO.java<br/>
 * <b>Description:</b> EmailMessageVO that incorperates building the HTML embedded in the message - from the form submission.
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2017<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Sep 8, 2017
 ****************************************************************************/
public class MIREmailMessageVO extends EmailMessageVO {

	private static final long serialVersionUID = 4872242395454435789L;
	private static final String REGION_EMAILS = "dpySynMirRegionEmails";
	private static final String PARAM_REGION = "region";
	private static final String PARAM_SUBREGION = "subregion";
	private static final String REGION_CANADA = "North-America-Canada";
	private static final String SUBREGION_CANADA = "canada";

	private MIRSubmissionVO vo;
	private SiteVO site;

	/**
	 * default constructor requires two parameters - so this object can do it's job
	 * @param attributes 
	 */
	public MIREmailMessageVO(MIRSubmissionVO vo, ActionRequest req, Map<String, Object> attributes) {
		super();
		this.vo = vo;
		site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		setSubject("MIR Submission");
		setInstance(InstanceName.DEPUY);
		//attach any uploaded files:
		if (req.hasFiles()) {
			for (FilePartDataBean file : req.getFiles())
				addAttachment(file.getFileName(), file.getFileData());
		}

		String rcpt = parseRecipient(req.getParameter(PARAM_REGION),
				req.getParameter(PARAM_SUBREGION), (String)attributes.get(REGION_EMAILS));

		try {
			addRecipient(rcpt);
			setFrom(site.getMainEmail());
		} catch (InvalidDataException e) {
			log.error("could not set recipient emails", e);
		}

	}


	/**
	 * Parses sb_config against the region given on the request to determine whom we send the email to.
	 * Default is included in sb_config as a catch-all to address problems with the system.
	 * @param parameter
	 * @param string
	 * @return
	 */
	private String parseRecipient(String region, String subRegion, String jsonObjStr) {
		String email = null;
		String mailRegion = region;

		// If subregion is Canada, use a different region to obtain a Canadian-specific recipient.
		if (SUBREGION_CANADA.equalsIgnoreCase(subRegion))
			mailRegion = REGION_CANADA;

		//this array comes from sb_config - we can trust it to not be null.
		JSONArray regionArr = JSONArray.fromObject(jsonObjStr);
		for (int x=0; x < regionArr.size(); x++) {
			JSONObject obj = regionArr.getJSONObject(x);
			if (obj.optString(PARAM_REGION).equalsIgnoreCase(mailRegion)) {
				return obj.optString("email");
			} else if ("default".equals(obj.optString(PARAM_REGION))) {
				//save the default incase we need a fallback plan.
				email = obj.optString("email");
			}
		}
		return StringUtil.checkVal(email, site.getAdminEmail());
	}


	/**
	 * builds & returns the HTML report/contents of the email
	 */
	@Override
	public String getHtmlBody() {
		//use the html if already built.  We use rather than ignore the superclass body field so messages can be intercepted properly (which adds to the html body)
		if (!StringUtil.isEmpty(super.getHtmlBody())) return super.getHtmlBody();

		StringBuilder html = new StringBuilder(5000);
		html.append("<h4 style=\"font-family:'Times New Roman', Times, serif\">A visitor to ").append(site.getSiteAlias()).append(" has submitted a ");
		html.append("Medical Information Request.</h4>");
		html.append("<table  style=\"font-family:'Times New Roman', Times, serif;font-size:12pt;width:100%;min-width:100%;border:solid 1px black;\">");
		html.append("<tr><th colspan='2'>Medical Information Request</th></tr>");

		int rowCnt = 0;
		// section 1 - HCP information
		addHtmlRow(html, ++rowCnt, "Website", site.getSiteName());
		addHtmlRow(html, ++rowCnt, "Region", vo.getRegion());
		addHtmlRow(html, ++rowCnt, "Sub-Region", vo.getSubregion());
		addHtmlRow(html, ++rowCnt, "HCP Type", StringUtil.checkVal(vo.getHcpType()));
		addHtmlRow(html, ++rowCnt, "HCP Type (Other)", vo.getHcpTypeOther());
		addHtmlRow(html, ++rowCnt, "HCP's Title", vo.getHcpTitle());
		addHtmlRow(html, ++rowCnt, "HCP's First Name", trimValue(vo.getFirstName()));
		addHtmlRow(html, ++rowCnt, "HCP's Last Name", trimValue(vo.getLastName()));
		addHtmlRow(html, ++rowCnt, "HCP's Specialty", vo.getHcpSpecialty());
		addHtmlRow(html, ++rowCnt, "HCP's Hospital / Institution / Office", vo.getHcpInstitution());
		addHtmlRow(html, ++rowCnt, "Consent", vo.getConsentFlg());
		// section 2 - Contact Information
		addHtmlRow(html, ++rowCnt, "Desired Response Method", vo.getResponseType());
		addHtmlRow(html, ++rowCnt, "Desired Response Method (Other)", vo.getResponseTypeOther());
		addHtmlRow(html, ++rowCnt, "Street Address", trimValue(vo.getAddress()));
		addHtmlRow(html, ++rowCnt, "City", trimValue(vo.getCity()));
		addHtmlRow(html, ++rowCnt, "State / Province", trimValue(vo.getState()));
		addHtmlRow(html, ++rowCnt, "ZIP / Postal Code", trimValue(vo.getZipCode()));
		addHtmlRow(html, ++rowCnt, "Country", vo.getCountryCode());
		addHtmlRow(html, ++rowCnt, "Telephone", StringUtil.checkVal(vo.getMainPhone()));
		addHtmlRow(html, ++rowCnt, "Fax", vo.getMobilePhone());
		addHtmlRow(html, ++rowCnt, "Email Address", vo.getEmailAddress());
		addHtmlRow(html, ++rowCnt, "J&amp;J Contact / Sales Rep", vo.getJjRep());
		addHtmlRow(html, ++rowCnt, "Contact's Email Address", vo.getJjRepEmail());
		// section 3 - Product Information
		addHtmlRow(html, ++rowCnt, "Medical Device Company(s)", vo.getProductCompanies());
		addHtmlRow(html, ++rowCnt, "Medical Device Company (Other)", vo.getProductCompanyOther());
		addHtmlRow(html, ++rowCnt, "Product(s)", vo.getMergedProducts());
		addHtmlRow(html, ++rowCnt, "Product (Other)", vo.getProductOther());
		addHtmlRow(html, ++rowCnt, "Part Number(s)", vo.getPartNumber());
		addHtmlRow(html, ++rowCnt, "Question", vo.getQuestion());

		html.append("</table><br/>");
		super.setHtmlBody(html.toString());
		return super.getHtmlBody();
	}

	/**
	 * Trims the String argument and returns a String representing the
	 * trimmed value.  If the String argument is null or empty, the String
	 * passed in the argument is returned.
	 * @param text
	 * @return
	 */
	private String trimValue(String text) {
		if (StringUtil.isEmpty(text)) return text;
		return text.trim();
	}

	/**
	 * Builder method for generating a row for the HTML table.
	 * @param html
	 * @param i
	 * @param string
	 * @param siteName
	 */
	private void addHtmlRow(StringBuilder html, int x, String colName, String colValue) {
		String color = (x % 2 == 0) ? "#C0D2EC" : "#E1EAFE";
		html.append("<tr style=\"background:").append(color).append(";\"><td style=\"padding-right:10px;font-size:12pt;font-family:'Times New Roman', Times, serif\">").append(colName);
		html.append("</td><td style=\"font-size:12pt;font-family:'Times New Roman', Times, serif\">").append(StringUtil.checkVal(colValue)).append("</td></tr>");
	}
}