package com.ansmed.sb.patient;

// JDK 1.6.0
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.SMTMail;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

// SB Libs
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>:MDJournalAction.java<p/>
 * <b>Description: </b> Manages the data for the MD Journal Entries
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Feb 16, 2009
 * <b>Changes: </b>
 ****************************************************************************/
public class MDJournalAction extends SBActionAdapter {
	public static final String JOURNAL_UPDATE_SUCCESS_MSG = "You have successfully saved the journal information";
	public static final String JOURNAL_UPDATE_ERROR_MSG = "Unable to save the journal information";
	
	/**
	 * 
	 */
	public MDJournalAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public MDJournalAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}
	
	/**
	 * Inserts the selected currencies
	 * @param currencies
	 * @param id
	 * @throws ActionException
	 */
	public void insertCurrencies(List<String> currencies, String id) throws ActionException {
		String schema = (String) this.getAttribute("customDbSchema");
		StringBuffer sql = new StringBuffer();
		sql.append("insert into ").append(schema).append("ans_xr_journal_resource ");
		sql.append("(rep_journal_resource_id, rep_journal_id, resource_type_id, ");
		sql.append("create_dt) values (?,?,?,?) ");
		log.debug("MD Journal Resources Insert SQL: " + sql + "|" + id);
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			for (int i=0; i < currencies.size(); i++) {
				log.debug("Res" + currencies.get(i));
				ps.setString(1, new UUIDGenerator().getUUID());
				ps.setString(2,id);
				ps.setString(3,currencies.get(i));
				ps.setTimestamp(4, Convert.getCurrentTimestamp());
				ps.executeUpdate();
			}
		} catch (Exception e) {
			log.error("Unable to Delete MD Journal Currencies", e);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
	}
	
	/**
	 * Deletes any existing currencies on an update
	 * @param id
	 * @throws ActionException
	 */
	public void deleteCurrencies(String id) throws ActionException {
		String schema = (String) this.getAttribute("customDbSchema");
		StringBuffer sql = new StringBuffer();
		sql.append("delete from ").append(schema).append("ans_xr_journal_resource ");
		sql.append("where rep_journal_id = ?");
		log.debug("MD Journal Currencies Delete SQL: " + sql);
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, id);
			ps.executeUpdate();
		} catch (Exception e) {
			log.error("Unable to Delete MD Journal Currencies", e);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
	}
	
	/**
	 * 
	 */
	public void retrieve(ActionRequest req) throws ActionException {
		Boolean searchSubmitted = Convert.formatBoolean(req.getParameter("searchSubmitted"));
		if (searchSubmitted) {
			this.createReport(req);
			
			ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
			AbstractSBReportVO rpt = new MDJournalReport();
			rpt.setData(mod.getActionData());
			rpt.setFileName("MDJournalReport." + req.getParameter("reportType", "html"));
			log.debug("Mime Type: " + rpt.getContentType());
			
			req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
			req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
		} else {
			log.info("Starting MD Journal Retrieval");
			ActionInterface sai = new StimTrackerAction();
			sai.setAttributes(attributes);
			sai.setDBConnection(dbConn);
			sai.retrieve(req);
		}
	}
	
	/**
	 * Generates the report for the MD Journal
	 * @param req
	 */
	public void createReport(ActionRequest req) {
		// Format the Dates
		String salesRepId = StringUtil.checkVal(req.getParameter("salesRepId"));
		String surgeonId = StringUtil.checkVal(req.getParameter("surgeonId"));
		Date startDate = null;
		Date endDate = null;
		if (StringUtil.checkVal(req.getParameter("startDate")).length() > 0)
			startDate = Convert.formatStartDate(req.getParameter("startDate"));
		if (StringUtil.checkVal(req.getParameter("endDate")).length() > 0)
			endDate = Convert.formatEndDate(req.getParameter("endDate"));
		log.debug("Dates: " + startDate + "|" + endDate);
		
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuffer sql = new StringBuffer();
		sql.append("select * from ").append(schema).append("ans_rep_journal a ");
		sql.append("left outer join ").append(schema).append("ans_xr_journal_resource b ");
		sql.append("on a.rep_journal_id = b.rep_journal_id ");
		sql.append("left outer join ").append(schema).append("ans_resource_type c ");
		sql.append("on b.resource_type_id = c.resource_type_id ");
		sql.append("inner join ").append(schema).append("ans_surgeon d ");
		sql.append("on a.surgeon_id = d.surgeon_id ");
		if (salesRepId.length() > 0) sql.append("and a.sales_rep_id = ? ");
		if (surgeonId.length() > 0) sql.append("and d.surgeon_id = ? ");
		if (startDate != null) sql.append("and a.create_dt >= ? ");
		if (endDate != null) sql.append("and a.create_dt <= ? ");
		sql.append("order by a.create_dt desc, a.rep_journal_id ");
		log.debug("MD Journal Report SQL: " + sql);
		
		// Loop the data and store the data in the map
		int ctr = 0;
		PreparedStatement ps = null;
		Map<String, MDJournalVO> data = new LinkedHashMap<String, MDJournalVO>();
		try {
			ps = dbConn.prepareStatement(sql.toString());
			if (salesRepId.length() > 0) ps.setString(++ctr, salesRepId);
			if (surgeonId.length() > 0) ps.setString(++ctr, surgeonId);
			if (startDate != null) ps.setDate(++ctr, Convert.formatSQLDate(startDate));
			if (endDate != null) ps.setDate(++ctr, Convert.formatSQLDate(endDate));
			ResultSet rs = ps.executeQuery();
			
			while(rs.next()) {
				String id = rs.getString("rep_journal_id");
				MDJournalVO vo = new MDJournalVO();
				if (data.containsKey(id)) {
					vo = data.get(id);
					vo.addCurrency(rs.getString("resource_nm"));
				} else {
					vo.setData(rs);
				}
				
				data.put(id, vo);
			}
		} catch(SQLException sqle) {
			log.error("Could not generate MD Journal", sqle);
		}
		
		// Save the action data
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		mod.setDataSize(data.size());
		mod.setActionData(data.values());
	}

	/**
	 * 
	 */
	public void build(ActionRequest req) throws ActionException {
		log.debug("Updating MD Journal");
		String msg = JOURNAL_UPDATE_SUCCESS_MSG;
		String schema = (String) this.getAttribute("customDbSchema");
		String repJournalId = StringUtil.checkVal(req.getParameter("repJournalId"));
		Boolean isUpdate = Boolean.FALSE;
		if (repJournalId.length() > 0) isUpdate = Boolean.TRUE;
		StringBuffer sql = new StringBuffer();
		
		String physician = StringUtil.checkVal(req.getParameter("physician"));
		String reason = StringUtil.checkVal(req.getParameter("reasonForVisit"));
		String[] whoStaff = req.getParameterValues("who");
		
		// check required fields (primarily for mobile site due to lack of js support)
		if (! isValidRequiredFields(physician, reason, whoStaff)) {
			String errMsg = "You must enter data for all required fields";
			this.setRedirectURL(req, errMsg, false);
			return;
		}
		
		// check in-service date for validity
		String inServiceDate = StringUtil.checkVal(req.getParameter("inServiceDate"));
		if (! isValidInserviceDate(inServiceDate)) {
			String errMsg = "You must enter a valid in-service date";
			this.setRedirectURL(req, errMsg, false);
			return;
		}	
		
		String repName = StringUtil.checkVal(req.getParameter("repName"));
				
		//Process the physician
		int physIndex = physician.indexOf("|");
		String physId = physician.substring(0,physIndex);
		String physName = physician.substring(physIndex + 1);
		
		//Process the 'who'(Staff to Visit) names/email addresses
		List<String> emailStaff = new ArrayList<String>();
		emailStaff = this.parseVals(whoStaff);
		int emailSize = emailStaff.size();
		
		// Get the visit staff delimited name string off of the List and remove.
		String whoName = emailStaff.get(emailSize - 1);
		emailStaff.remove(emailSize - 1);
				
		//Process the 'whom'(Staff to Handle) name/email address
		String whomStaff = StringUtil.checkVal(req.getParameter("whom"));
		String whomName = "";
		if (whomStaff.length() > 0) {
			int index = whomStaff.indexOf("|");
			whomName = whomStaff.substring(0,index);
			// Add the email address to the list of email addresses if it isn't a duplicate
			if (! emailStaff.contains(whomStaff.substring(index + 1))) {
				emailStaff.add(whomStaff.substring(index + 1));
			}
		}
		
		//Process the resources
		String[] resources = req.getParameterValues("currency");
		StringBuffer resNames = new StringBuffer("");
		List<String> resTypes = new ArrayList<String>();
		
		if (resources != null && resources.length > 0) {
			for (int i = 0; i < resources.length; i ++) {
				String r = resources[i];
				int index = r.indexOf("|");
				resNames.append(r.substring(0,index));
				resTypes.add(r.substring(index + 1));
				if ((i + 1) < resources.length) {
					resNames.append(", ");
				}
			}
		}
		
		if (isUpdate) {
			sql.append("update ").append(schema).append("ans_rep_journal ");
			sql.append("set surgeon_id = ?, reason_visit_txt = ?, about_txt = ?,");
			sql.append("next_step_txt = ?, who_sales_rep_nm = ?, whom_sales_rep_nm = ? ");
			sql.append("sales_rep_nm = ?, sales_rep_id = ?, in_service_dt = ?, create_dt = ? ");
			sql.append("where rep_journal_id = ?");

		} else {
			repJournalId = new UUIDGenerator().getUUID();
			sql.append("insert into ").append(schema).append("ans_rep_journal ");
			sql.append("(surgeon_id, reason_visit_txt, about_txt, next_step_txt, ");
			sql.append("who_sales_rep_nm, whom_sales_rep_nm, sales_rep_nm, ");
			sql.append("sales_rep_id, in_service_dt, create_dt, rep_journal_id) ");
			sql.append("values (?,?,?,?,?,?,?,?,?,?,?)");
		}
		
		log.debug("MD Journal Build SQL: " + sql);
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, physId);
			ps.setString(2, reason);
			ps.setString(3, req.getParameter("spokeAbout"));
			ps.setString(4, req.getParameter("whatNext"));
			ps.setString(5, whoName);
			ps.setString(6, whomName);
			ps.setString(7, repName);
			ps.setString(8, req.getParameter("salesRepId"));
			ps.setTimestamp(9, Convert.formatTimestamp(Convert.DATE_SLASH_PATTERN, inServiceDate));
			ps.setTimestamp(10, Convert.getCurrentTimestamp());
			ps.setString(11, repJournalId);
			ps.executeUpdate();
			
			// Delete the existing currencies
			if (isUpdate) this.deleteCurrencies(repJournalId);
			
			// If there are currencies, add them
			if (resTypes.size() > 0) insertCurrencies(resTypes,repJournalId);
			
			// Process email
			sendEmail(req, repName, physName, emailStaff, whoName, whomName, resNames.toString(), inServiceDate);
	    				
		} catch (SQLException sqle) {
			log.error("Unable to update MD Journal", sqle);
			msg = JOURNAL_UPDATE_ERROR_MSG;
		} catch (MailException me) {
    		log.error("Error sending MD Journal submission notification email: ", me);
    	} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
				
		// Build the redir url and set redirect on request
    	this.setRedirectURL(req, msg, true);
	}
	
	/**
	 * Parses | delimited values.
	 * @param staff
	 * @return
	 */
	public List<String> parseVals(String[] vals) {
		List<String> valData = new ArrayList<String>();
		StringBuffer sbN = new StringBuffer("");
		for (int i = 0; i < vals.length; i++) {
			if (StringUtil.checkVal(vals[i]).length() > 0) {
				int index = vals[i].indexOf("|");
				if (index > -1) {
					sbN.append(vals[i].substring(0,index));
					valData.add(vals[i].substring(index + 1));
				} else {
					sbN.append(vals[i]);
				}
				if ((i + 1) < vals.length) {
					sbN.append(",");
				}
			} 
		}
		valData.add(sbN.toString());
		return valData;
	}
	
	/**
	 * Checks required fields and date field for valid values
	 * @param physician
	 * @param reason
	 * @param whoStaff
	 * @return
	 */
	private boolean isValidRequiredFields(String physician, String reason, String[] whoStaff) {
		boolean isValid = true;
		int phys = physician.length();
		int reas = reason.length();
		int who = 0;
		if (whoStaff != null) who = whoStaff.length;
		// check the required fields (supports the mobile site).
		if (phys == 0 || reas == 0 || who == 0) {
			isValid = false;
		}
		return isValid;
	}
	
	/**
	 * Checks for valid inServiceDate if a date was submitted
	 * @param inServiceDate
	 * @return
	 */
	private boolean isValidInserviceDate(String inServiceDate) {
		boolean isValid = true;
		if (inServiceDate.length() > 0 && Convert.formatDate(Convert.DATE_SLASH_PATTERN, inServiceDate) == null) {
			isValid = false;
		}
		return isValid;
	}
	
	/**
	 * Builds the appropriate return url
	 * @param req
	 * @param msg
	 */
	private void setRedirectURL(ActionRequest req, String msg, boolean success) {
		String url = null;
		url = req.getRequestURI();
		log.debug("requestURI: " + url);
		if (success) {
			url = url + "?msg=" + msg;
		} else {
			url = url + this.buildPostUrl(req);
			url = url + "&msg=" + msg;
		}
		log.debug("redirect url: " + url);
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url);
	}
	
	/**
	 * 
	 * @param req
	 * @param repName
	 * @param physName
	 * @param visitStaff
	 * @param visitName
	 * @param handleName
	 * @param resName
	 * @param inServiceDate
	 * @throws MailException
	 */
	private void sendEmail(ActionRequest req, String repName, String physName, 
			List<String> visitStaff, String visitName, String handleName, String resName, String inServiceDate) 
			throws MailException {
		
		log.debug("Processing journal email...");
		StringEncoder se = new StringEncoder();
		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);

		StringBuffer msg = new StringBuffer();
		msg.append("<table style=\"width:750px;border:solid 1px black;\">");
		msg.append("<tr style=\"background:#E1EAFE;\"><th colspan='2'>").append("MD Journal Submission");
		msg.append("</th></tr>");
		msg.append("<tr style=\"background:#c0d2ec;\"><td style=\"padding-right:10px;\">Website");
		msg.append("</td><td>");
		msg.append(site.getSiteName()).append("</td></tr>");
		
		String color = "#E1EAFE";
		String color1 = "#c0d2ec";
		
		msg.append("<tr style=\"background:").append(color);
		msg.append(";\"><td style=\"padding-right:10px;\" nowrap valign=\"top\">");
		msg.append("Physician").append("</td><td>").append(physName).append("&nbsp;</td></tr>");

		msg.append("<tr style=\"background:").append(color1);
		msg.append(";\"><td style=\"padding-right:10px;\" nowrap valign=\"top\">");
		msg.append("SJM Staff at Visit").append("</td><td>").append(visitName).append("&nbsp;</td></tr>");
		
		msg.append("<tr style=\"background:").append(color);
		msg.append(";\"><td style=\"padding-right:10px;\" nowrap valign=\"top\">");
		msg.append("Reason for Visit").append("</td><td>").append(req.getParameter("reasonForVisit")).append("&nbsp;</td></tr>");

		msg.append("<tr style=\"background:").append(color1);
		msg.append(";\"><td style=\"padding-right:10px;\" nowrap valign=\"top\">");
		msg.append("Resources").append("</td><td>").append(se.decodeValue(resName)).append("&nbsp;</td></tr>");
		
		msg.append("<tr style=\"background:").append(color);
		msg.append(";\"><td style=\"padding-right:10px;\" nowrap valign=\"top\">");
		msg.append("Topic of Discussion").append("</td><td>").append(se.decodeValue(req.getParameter("spokeAbout"))).append("&nbsp;</td></tr>");
		
		msg.append("<tr style=\"background:").append(color1);
		msg.append(";\"><td style=\"padding-right:10px;\" nowrap valign=\"top\">");
		msg.append("SJM Staff to Handle").append("</td><td>").append(handleName).append("&nbsp;</td></tr>");
		
		msg.append("<tr style=\"background:").append(color);
		msg.append(";\"><td style=\"padding-right:10px;\" nowrap valign=\"top\">");
		msg.append("Action Plan").append("</td><td>").append(se.decodeValue(req.getParameter("whatNext"))).append("&nbsp;</td></tr>");
        
		msg.append("<tr style=\"background:").append(color1);
		msg.append(";\"><td style=\"padding-right:10px;\" nowrap valign=\"top\">");
		msg.append("Date of In-Service").append("</td><td>");
		msg.append(inServiceDate).append("&nbsp;</td></tr>");
		
		msg.append("<tr style=\"background:").append(color);
		msg.append(";\"><td style=\"padding-right:10px;\" nowrap valign=\"top\">");
		msg.append("Entered By").append("</td><td>").append(repName).append("&nbsp;</td></tr>");
		msg.append("</table>");
		msg.append("<br>");
		
		for (int i = 0; i < visitStaff.size(); i++) {
			SMTMail mail = new SMTMail((String)getAttribute(Constants.CFG_SMTP_SERVER));
			mail.setUser((String)getAttribute(Constants.CFG_SMTP_USER));
			mail.setPassword((String)getAttribute(Constants.CFG_SMTP_PASSWORD));
			mail.setRecpt(new String[] {visitStaff.get(i)});
			mail.setSubject("MD Journal Submission");
			mail.setFrom(site.getMainEmail());
			mail.setHtmlBody(msg.toString());
			log.debug("Mail Info: " + mail.toString());
			mail.postMail();			
		}
	}
}
