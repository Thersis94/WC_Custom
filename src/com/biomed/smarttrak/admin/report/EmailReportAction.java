package com.biomed.smarttrak.admin.report;

import java.sql.Date;
// Java 8
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

//WC custom
import com.biomed.smarttrak.vo.EmailLogVO;
// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.io.mail.BaseMessageVO;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.io.mail.MessageVO;
import com.siliconmtn.io.mail.stor.MessageSerializer;
import com.siliconmtn.sb.email.util.SentMessageUtil;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.user.NameComparator;
import com.smt.sitebuilder.action.SBActionAdapter;
// WebCrescendo
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: EmailReportAction.java</p>
 <p><b>Description: displays (inline) a report about emails sent from the servers..</b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Apr 3, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class EmailReportAction extends SBActionAdapter {

	/**
	 * Constructor
	 */
	public EmailReportAction() {
		super();
	}


	/**
	 * Retrieves the user list report data.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (req.hasParameter("loadData")) {
			List<EmailLogVO> data = loadSummaryData(req, null);
			putModuleData(data, getEmailCount(req), false);
		} else if (req.hasParameter("campaignLogId")) {
			putModuleData(loadSingleEmail(req, req.getParameter("campaignLogId")));
		}
	}


	/**
	 * loads a single email message for inline display
	 * @param parameter
	 * @return
	 */
	protected EmailLogVO loadSingleEmail(ActionRequest req, String campaignLogId) {
		List<EmailLogVO> data = loadSummaryData(req, campaignLogId);
		if (data == null || data.isEmpty()) return null;
		EmailLogVO vo = data.get(0);
		//load the email message body, and the config params tied to this send.
		if(!vo.isFileWritten()) {
			populateEmail(vo);
		} else {
			loadEmailArchive(vo);
		}

		return vo;
	}


	/**
	 * Helper method for loading html data from Email File.
	 * @param vo
	 */
	private void loadEmailArchive(EmailLogVO vo) {
		EmailMessageVO eml = (EmailMessageVO) MessageSerializer.getInstance(attributes, BaseMessageVO.Type.EMAIL).retrieveMessage(vo.getFilePathText());
		vo.setMessageBody(eml.getHtmlBody());
	}


	/**
	 * @param vo
	 * @param campaignLogId
	 */
	protected void populateEmail(EmailLogVO vo) {
		//call the email core to obtain and re-assemble the email as it was when the user received it.
		SentMessageUtil util = new SentMessageUtil(getDBConnection(), getAttributes());
		MessageVO eml = util.recreateMessage(vo.getCampaignInstanceId(), vo.getCampaignLogId());
		if (eml instanceof EmailMessageVO) {
			EmailMessageVO emlVo = (EmailMessageVO) eml;
			vo.setMessageBody(emlVo.getHtmlBody());
		}
	}


	/**
	 * @param site
	 * @param hasParameter
	 * @return
	 */
	protected List<EmailLogVO> loadSummaryData(ActionRequest req, String campaignLogId) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		int offset = Convert.formatInteger(req.getParameter("offset"),0);
		int limit = Convert.formatInteger(req.getParameter("limit"),10);
		String term = req.getParameter("search");
		List<EmailLogVO> data = new ArrayList<>(50);

		String sql = getGroupByQuery(campaignLogId, req.getParameter("sort"), StringUtil.checkVal(req.getParameter("order"), "desc"), term);
		log.debug(sql);

		int x=0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			if (!StringUtil.isEmpty(term)) ps.setString(++x, encKeyword(term.toUpperCase()));
			ps.setString(++x, site.getOrganizationId());
			if (campaignLogId != null) ps.setString(++x, campaignLogId);
			ps.setDate(++x,  getThresDate());
			ps.setInt(++x,  limit);
			ps.setInt(++x, offset);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				EmailLogVO vo = new EmailLogVO();
				vo.setSentDate(rs.getTimestamp("attempt_dt"));
				vo.setEmailAddress(rs.getString("email_address_txt"));
				vo.setFirstName(rs.getString("first_nm"));
				vo.setLastName(rs.getString("last_nm"));
				vo.setCampaignLogId(rs.getString("campaign_log_id"));
				vo.setCampaignInstanceId(rs.getString("campaign_instance_id"));
				vo.setOpenCnt(rs.getInt("cnt"));
				vo.setSubject(rs.getString("subject_txt"));
				vo.setFilePathText(rs.getString("file_path_txt"));
				data.add(vo);
			}

		} catch (SQLException sqle) {
			log.error("could not load email log", sqle);
		}

		log.debug("size=" + data.size());
		decryptNames(data);
		return data;
	}


	/**
	 * encrypts the search term (last name or email address) for searching
	 * @param parameter
	 * @return
	 */
	protected String encKeyword(String term) {
		try {
			return new StringEncrypter((String)getAttribute(Constants.ENCRYPT_KEY)).encrypt(term);
		} catch (Exception e) {
			return term;
		}
	}


	/**
	 * @param campaignLogId
	 * @return
	 */
	protected String getGroupByQuery(String campaignLogId, String sort, String dir, String term) {
		StringBuilder sql = new StringBuilder(250);
		sql.append("select inst.campaign_instance_id, coalesce(l.attempt_dt,l.create_dt) as attempt_dt, l.success_flg, p.email_address_txt, p.profile_id, ");
		sql.append("p.first_nm, p.last_nm, l.campaign_log_id, count(resp.email_response_id) as cnt, ");
		sql.append("l.subject_txt, l.file_path_txt ");
		sql.append("from email_campaign camp ");
		sql.append("inner join email_campaign_instance inst on camp.email_campaign_id=inst.email_campaign_id ");
		sql.append("inner join email_campaign_log l on inst.campaign_instance_id=l.campaign_instance_id ");
		sql.append("inner join profile p on l.profile_id=p.profile_id ");
		if (!StringUtil.isEmpty(term)) {
			if (StringUtil.isValidEmail(term)) {
				sql.append("and p.search_email_txt=? ");
			} else {
				sql.append("and p.search_last_nm=? ");
			}
		}
		sql.append("left join email_response resp on l.campaign_log_id=resp.campaign_log_id and resp.response_type_id='EMAIL_OPEN' ");
		sql.append("where camp.organization_id=? ");
		if (campaignLogId != null) sql.append("and l.campaign_log_id=? ");
		sql.append("and l.create_dt > ? ");
		sql.append("group by inst.campaign_instance_id, l.attempt_dt, p.profile_id, p.email_address_txt, ");
		sql.append("p.first_nm, p.last_nm, success_flg, l.campaign_log_id, l.subject_txt, file_path_txt ");
		sql.append("order by ").append(getOrderBy(sort, dir)).append(" limit ? offset ? ");
		return sql.toString();
	}


	/**
	 * returns the data limit used by both queries - put in one place so they're always the same. (count & data queries)
	 * @return
	 */
	private Date getThresDate() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -90); //90 days is the hard limit, for server-load purposes
		return Convert.formatSQLDate(cal.getTime());
	}


	/**
	 * @param parameter
	 * @return
	 */
	private String getOrderBy(String sort, String dir) {
		if ("subject".equals(sort)) {
			return "case when inst.subject_txt = '${subject}' then sl.value_txt else inst.subject_txt end " + dir;
		} else if ("openCnt".equals(sort)) {
			return "cnt " + dir;
		} else {
			return "attempt_dt " + dir;
		}
	}


	/**
	 * Retrieves total number of update records in db.
	 * @param req
	 * @param schema
	 * @return
	 */
	protected int getEmailCount(ActionRequest req) {
		String term = req.getParameter("search");
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		StringBuilder sql = new StringBuilder(150);
		sql.append("select count(*) from email_campaign_log a ");
		if (!StringUtil.isEmpty(term)) {
			sql.append("inner join profile p on a.profile_id=p.profile_id ");
			if (StringUtil.isValidEmail(term)) {
				sql.append("and p.search_email_txt=? ");
			} else {
				sql.append("and p.search_last_nm=? ");
			}
		}
		sql.append("inner join email_campaign_instance b on a.campaign_instance_id=b.campaign_instance_id ");
		sql.append("inner join email_campaign c on b.email_campaign_id=c.email_campaign_id and c.organization_id=? ");
		sql.append("where a.attempt_dt < ?");
		log.debug(sql);

		int x=0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			if (!StringUtil.isEmpty(term)) ps.setString(++x, encKeyword(term.toUpperCase()));
			ps.setString(++x, site.getOrganizationId());
			ps.setDate(++x, getThresDate());
			ResultSet rs = ps.executeQuery();
			return rs.next() ? rs.getInt(1) : 0;

		} catch (SQLException e) {
			log.error("could not load email log count", e);
		}
		return 0;
	}


	/**
	 * loop and decrypt owner names, which came from the profile table
	 * @param accounts
	 */
	protected void decryptNames(List<EmailLogVO> data) {
		new NameComparator().decryptNames(data, (String)getAttribute(Constants.ENCRYPT_KEY));
	}
}