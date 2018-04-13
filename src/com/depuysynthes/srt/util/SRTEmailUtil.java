package com.depuysynthes.srt.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.depuysynthes.srt.vo.SRTProjectVO;
import com.depuysynthes.srt.vo.SRTRequestAddressVO;
import com.depuysynthes.srt.vo.SRTRequestVO;
import com.depuysynthes.srt.vo.SRTRosterVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.sb.email.util.EmailCampaignBuilderUtil;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: SRTEmailUtil.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Utility class for building Milestone Emails.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 1.0
 * @since Mar 10, 2017
 ****************************************************************************/
public class SRTEmailUtil {
	private Logger log;
	private EmailCampaignBuilderUtil ecbu;
	private SMTDBConnection dbConn = null;
	private Map<String, Object> attributes = null;

	/**
	 * Constructor takes Connection and attributes Map for building emails.
	 * @param conn
	 * @param attributes
	 */
	public SRTEmailUtil(SMTDBConnection conn, Map<String, Object> attributes) {
		this.log = Logger.getLogger(this.getClass());
		this.dbConn = conn;
		this.attributes = attributes;
		this.ecbu = new EmailCampaignBuilderUtil(dbConn, attributes);
	}


	/**
	 * Single point entry for sending emails.
	 * @param projectId
	 * @param campaignInstanceId
	 */
	public void sendEmail(String projectId, String campaignInstanceId) {
		SRTProjectVO project = loadProject(projectId);
		sendEmails(project, campaignInstanceId);
	}

	/**
	 * Helper method that builds the Project Retrieval Sql complete with creator
	 * info and optional assigned info if available.
	 * 
	 * TODO - Need to pair necessary fields to actual fields on query to
	 * retrieve.
	 * @return
	 */
	protected String getProjectSql() {
		String customDb = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(600);
		sql.append("select p.*, r.*, u.*, a.* ").append(DBUtil.FROM_CLAUSE).append(customDb);
		sql.append("DPY_SYN_SRT_PROJECT p ");
		sql.append(DBUtil.INNER_JOIN).append(customDb).append("DPY_SYN_SRT_REQUEST r ");
		sql.append("on p.request_id = r.request_id ");
		sql.append(DBUtil.INNER_JOIN).append(customDb).append("DPY_SYN_SRT_ROSTER u ");
		sql.append("on r.roster_id = u.roster_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(customDb).append("DPY_SYN_SRT_REQUEST_ADDRESS a ");
		sql.append("on r.request_id = a.request_id ");
		sql.append(DBUtil.WHERE_CLAUSE).append("p.project_id = ?");

		return sql.toString();
	}

	/**
	 * Load Project Info.
	 * @param projectId
	 * @return
	 */
	public SRTProjectVO loadProject(String projectId) {
		SRTProjectVO p = null;
		String sql = getProjectSql();
		List<Object> params = new ArrayList<>();
		params.add(projectId);

		try(PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, projectId);
			ResultSet rs = ps.executeQuery();

			while(rs.next()) {
				p = new SRTProjectVO(rs);
				SRTRequestVO r = new SRTRequestVO(rs);
				r.setRequestor(new SRTRosterVO(rs));
				r.setRequestAddress(new SRTRequestAddressVO(rs));
				p.setRequest(r);
			}
		} catch (SQLException e) {
			log.error("Error Processing Code", e);
		}

		decryptProjectData(p);

		return p;
	}

	/**
	 * Helper method that builds the Project Link for the email.
	 * @param t
	 * @return
	 */
	protected String buildProjectLink(SRTProjectVO p) {
		//Build Url
		StringBuilder url = new StringBuilder(100);
		url.append(SRTUtil.SrtPage.PROJECT.getUrlPath());
		url.append("?projectId=").append(p.getProjectId());

		return url.toString();
	}


	/**
	 * Helper method that decrypts Profile Data on the SRTProjectVO.
	 * @param p
	 */
	protected void decryptProjectData(SRTProjectVO p) {
		try {
			StringEncrypter se = new StringEncrypter((String)attributes.get(Constants.ENCRYPT_KEY));
			p.setEngineerNm(SRTUtil.decryptName(p.getEngineerNm(), se));
			p.setDesignerNm(SRTUtil.decryptName(p.getDesignerNm(), se));
			p.setQualityEngineerNm(SRTUtil.decryptName(p.getQualityEngineerNm(), se));
			ProfileManagerFactory.getInstance(attributes).populateRecords(dbConn, Arrays.asList(p.getRequest().getRequestor()));
		} catch (EncryptionException | DatabaseException e) {
			log.error(e);
		}
	}

	/**
	 * Build EmailCampaign Message Objects to be Enqueued.
	 * @param project
	 * @param campaignInstanceId 
	 */
	protected void sendEmails(SRTProjectVO project, String campaignInstanceId) {
		Map<String, Object> emailParams = buildEmailProps(project);

		//Build Recipient Map.  Right now assuming all emails go to original requestor.
		SRTRosterVO roster = project.getRequest().getRequestor();
		Map<String, String> recipients = new HashMap<>();
		recipients.put(roster.getProfileId(), roster.getEmailAddress());

		//Send Email.
		ecbu.sendMessage(emailParams, recipients, campaignInstanceId);
	}

	/**
	 * Builds Email Config map from Project Record
	 * (contains Request and Requestor Profile data).
	 * 
	 * TODO - Need params that should be available to Email.
	 * @param p
	 * @return
	 */
	protected Map<String, Object> buildEmailProps(SRTProjectVO p) {
		SRTRosterVO roster = p.getRequest().getRequestor();
		Map<String, Object> config = new HashMap<>();
		config.put("firstName", StringUtil.checkVal(roster.getFirstName()));
		config.put("lastName", StringUtil.checkVal(roster.getLastName()));
		return config;
	}
}