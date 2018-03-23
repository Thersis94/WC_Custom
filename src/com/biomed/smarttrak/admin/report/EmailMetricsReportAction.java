package com.biomed.smarttrak.admin.report;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
<p><b>Title</b>: EmailMetricsReportAction.java</p>
<p><b>Description: Creates a report vo for email metrics</b></p>
<p> 
<p>Copyright: (c) 2000 - 2018 SMT, All Rights Reserved</p>
<p>Company: Silicon Mountain Technologies</p>
@author Eric Damschroder
@version 1.0
@since Mar 23, 2018
<b>Changes:</b> 
***************************************************************************/

public class EmailMetricsReportAction extends SBActionAdapter{
	
	private final String OPEN_ID = "EMAIL_OPEN";
	private final String CAMPAIGN_INSTANCE_ID = "campaignInsanceId";
	private final String ACCOUNT_ID = "accountId";
	private final String START_DATE = "startDt";
	private final String END_DATE = "endDt";
	
	public EmailMetricsReportVO buildReport(ActionRequest req) throws ActionException {
		Map<String, Object> data = new HashMap<>();
		List<EmailMetricsVO> emails = getEmails(req);
		data.put("emails", emails);
		EmailMetricsReportVO report = new EmailMetricsReportVO(data);
		report.setFileName(buildFileName(req, emails));
		return report;
	}
	
	
	/**
	 * Customize the file name if possible to reflect the data in the report.
	 * @param req
	 * @param emails
	 * @return
	 */
	private String buildFileName(ActionRequest req, List<EmailMetricsVO> emails) {
		StringBuilder fileName = new StringBuilder(256);
		
		// If this is empty we can't customize the title, just return a default title.
		if (emails.isEmpty()) return "Email metrics Report.xls";
		
		if (req.hasParameter(ACCOUNT_ID)) fileName.append(emails.get(0).getAccountName()).append("'s ");
		fileName.append("Email Metrics Report");
		if (req.hasParameter(CAMPAIGN_INSTANCE_ID)) fileName.append(" of ").append(emails.get(0).getCampaignName());
		if (req.hasParameter(START_DATE) && req.hasParameter(END_DATE)) {
			fileName.append(" ").append(req.getParameter(START_DATE)).append(" to ").append(req.getParameter(END_DATE));
		} else if (req.hasParameter(START_DATE)){
			fileName.append(" After ").append(req.getParameter(START_DATE));
		} else if (req.hasParameter(END_DATE)) {
			fileName.append(" Before ").append(req.getParameter(END_DATE));
		} else {
			fileName.append(" From the Last 30 Days");
		}
		fileName.append(".xls");
		
		return fileName.toString();
	}


	/**
	 * Load all emails needed for the report.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	private List<EmailMetricsVO> getEmails(ActionRequest req) throws ActionException {
		String orgId = ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getOrganizationId();
		try (PreparedStatement ps = dbConn.prepareStatement(buildReportSQL(req))) {
			int x = 1;
			ps.setString(x++, orgId);
			if (req.hasParameter(ACCOUNT_ID)) ps.setString(x++, req.getParameter(ACCOUNT_ID));
			if (req.hasParameter(CAMPAIGN_INSTANCE_ID)) ps.setString(x++, req.getParameter(CAMPAIGN_INSTANCE_ID));
			if (req.hasParameter(START_DATE)) ps.setDate(x++, Convert.formatSQLDate(req.getParameter(START_DATE)));
			if (req.hasParameter(END_DATE)) ps.setDate(x++, Convert.formatSQLDate(req.getParameter(END_DATE)));
			
			return parseResults(ps.executeQuery());
			
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}
	
	
	/**
	 * Create the list of emails from the result set.
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	private List<EmailMetricsVO> parseResults(ResultSet rs) throws ActionException {
		List<EmailMetricsVO> emails = new ArrayList<>();
		String currentInstance = "";
		EmailMetricsVO email = null;
		try {
			while (rs.next()) {
				if (!currentInstance.equals(rs.getString("email_address_txt"))) {
					addEmail(email, emails);
					email = new EmailMetricsVO();
					email.setAccountName(rs.getString("account_nm"));
					email.setCampaignName(rs.getString("instance_nm"));
					email.setEmailAddress(rs.getString("email_address_txt"));
					email.setNotesText(rs.getString("notes_txt"));
					currentInstance = rs.getString("email_address_txt");
				}
				addCounts(rs, email);
			}
			addEmail(email, emails);
			StringEncrypter se = new StringEncrypter((String) attributes.get(Constants.ENCRYPT_KEY));
			for (EmailMetricsVO metric : emails) 
				metric.setEmailAddress(se.decrypt(metric.getEmailAddress()));

			return emails;
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Determine which counts need to be updated and do so.
	 * @param rs
	 * @param email
	 * @throws SQLException
	 */
	private void addCounts(ResultSet rs, EmailMetricsVO email) throws SQLException {
		if (OPEN_ID.equals(rs.getString("response_type_id"))) {
			email.setOpens(rs.getInt("count"));
		} else if (0 == rs.getInt("success_flg")) {
			email.setFails(rs.getInt("sent"));
		}
		email.addToTotal(rs.getInt("sent"));
	}


	/**
	 * Add the email to the list.
	 * @param email
	 * @param emails
	 */
	private void addEmail(EmailMetricsVO email, List<EmailMetricsVO> emails) {
		if (email != null)
			emails.add(email);
	}


	/**
	 * Create the report sql.
	 * @param req
	 * @return
	 */
	private String buildReportSQL(ActionRequest req) {
		StringBuilder sql =  new StringBuilder(500);
		sql.append("select a.account_nm, p.email_address_txt, inst.campaign_instance_id, count(distinct l.campaign_log_id) as sent, inst.instance_nm, ");
		sql.append("l.success_flg, count(inst.campaign_instance_id) as count, resp.response_type_id, l.notes_txt ");
		sql.append("from email_campaign camp ");
		sql.append("inner join email_campaign_instance inst on camp.email_campaign_id=inst.email_campaign_id ");
		sql.append("inner join email_campaign_log l on inst.campaign_instance_id=l.campaign_instance_id ");
		sql.append("inner join profile p on l.profile_id=p.profile_id ");
		sql.append("left join email_response resp on l.campaign_log_id=resp.campaign_log_id and resp.response_type_id='EMAIL_OPEN' ");
		sql.append("inner join custom.biomedgps_user u on u.profile_id = p.profile_id ");
		sql.append("inner join custom.biomedgps_account a on u.account_id = a.account_id ");
		sql.append("where camp.organization_id= ? ");
		if (req.hasParameter(ACCOUNT_ID)) sql.append("and a.account_id = ? ");
		if (req.hasParameter(CAMPAIGN_INSTANCE_ID)) sql.append("and inst.campaign_instance_id = ? ");
		if (req.hasParameter(START_DATE)) sql.append("and l.attempt_dt > ? ");
		if (req.hasParameter(END_DATE)) sql.append("and l.attempt_dt < ? ");
		if (!req.hasParameter(START_DATE) && !req.hasParameter(END_DATE)) sql.append("and l.attempt_dt < CURRENT_DATE and l.attempt_dt > CURRENT_DATE -30 ");
		sql.append("group by a.account_nm, p.email_address_txt, inst.campaign_instance_id, inst.instance_nm, ");
		sql.append("l.success_flg, resp.response_type_id,inst.campaign_instance_id, l.notes_txt ");
		sql.append("order by account_nm, campaign_instance_id, email_address_txt ");
		log.debug(sql);
		return sql.toString();
	}

}
