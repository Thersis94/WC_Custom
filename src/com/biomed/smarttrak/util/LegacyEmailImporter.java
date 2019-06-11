package com.biomed.smarttrak.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.biomed.smarttrak.vo.EmailLogVO;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.UUIDGenerator;

/****************************************************************************
 * <b>Title</b>: LinkChecker.java<p/>
 * <b>Description: Reads data from DB then runs regex looking for embedded URLs.
 * URLs compiled are then sent an HTTP HEAD request to check if they are valid.
 * The outcomes are logged to a separate database table for reporting and maintenance.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Apr 01, 2017
 ****************************************************************************/
public class LegacyEmailImporter extends CommandLineUtil {

	String legacyCampaignInstance;

	/**
	 * default constructor
	 * @param args
	 */
	public LegacyEmailImporter(String[] args) {
		super(args);
		loadProperties("scripts/bmg_smarttrak/markdown.properties");
		loadDBConnection(props);
		legacyCampaignInstance = props.getProperty("legacyCampaignInstance");
	}


	/**
	 * main method
	 * @param args
	 */
	public static void main(String[] args) {
		LegacyEmailImporter eui = new LegacyEmailImporter(args);
		eui.run();
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 * call this method from main() to iterate the enum and execute all tables
	 */
	@Override
	public void run() {
		List<EmailLogVO> data;
		int rowCnt = getDataCount();
		log.info("need to insert: " + rowCnt);
		int start = 0;
		int limit = 5000; //batch 5k at a time - avoids stack overflow issues
		do {
			data = loadData(limit, start);
			saveCampaignLog(data);
			saveSendParams(data);
			saveEmailOpens(data);
			start += limit;
			log.info("finished " + start);

		} while (start < rowCnt);
	}


	/**
	 * @param data
	 */
	private void saveCampaignLog(List<EmailLogVO> data) {
		String sql = "insert into email_campaign_log (campaign_log_id, campaign_instance_id, profile_id, create_dt, attempt_dt, success_flg) values (?,?,?,CURRENT_TIMESTAMP,?,1)";
		for (EmailLogVO vo : data) {
			try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
				ps.setString(1, vo.getCampaignLogId());
				ps.setString(2,  legacyCampaignInstance);
				ps.setString(3,  vo.getProfileId());
				ps.setTimestamp(4,  Convert.formatTimestamp(vo.getSentDate() != null ? vo.getSentDate() : new Date()));
				ps.executeUpdate();

			} catch (Exception e) {
				log.error("could not save email_campaign_log for " + vo.getCampaignLogId(), e);
			}
		}
	}


	/**
	 * @param data
	 */
	private void saveSendParams(List<EmailLogVO> data) {
		UUIDGenerator uuid = new UUIDGenerator();
		String sql = "insert into SENT_EMAIL_PARAM (sent_email_param_id, campaign_log_id, key_nm, value_txt, create_dt) values (?,?,?,?,CURRENT_TIMESTAMP)";
		int x = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			for (EmailLogVO vo : data) {
				++x;
				//msg body
				ps.setString(1, uuid.getUUID());
				ps.setString(2,  vo.getCampaignLogId());
				ps.setString(3, "subject");
				ps.setString(4,  vo.getSubject());
				ps.addBatch();
				//msg subject
				ps.setString(1, uuid.getUUID());
				ps.setString(2,  vo.getCampaignLogId());
				ps.setString(3, "body");
				String body = vo.getMessageBody();
				if (body.indexOf("<body") > -1) {
					body = body.replaceAll("(?s)(.*)?<body([^>]*)>(.*)?</body>(.*)?","$3"); //keep only what's inside the <body> tag
					body = body.replaceAll("(?s)<style([^>]*)>([^<>]*)?</style>",""); //remove nested <style> tags.
				}
				ps.setString(4,  body);
				ps.addBatch();

				if (x % 500 == 0 || x == data.size()) {
					ps.executeBatch();
					log.debug("comitted at " + x);
				}
			}

		} catch (Exception e) {
			log.error("could not save sent_message_log", e);
		}
	}


	/**
	 * @param data
	 */
	private void saveEmailOpens(List<EmailLogVO> data) {
		UUIDGenerator uuid = new UUIDGenerator();
		String sql = "insert into email_response (email_response_id, campaign_log_id, response_type_id, create_dt) values (?,?,'EMAIL_OPEN',?)";
		for (EmailLogVO vo : data) {
			try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
				//NOTE: these will appear as dups in the database, but they're not.  We need an entry for each time the user 
				//opened the email, and we don't have open dates except for the most recent.
				for (int x=vo.getOpenCnt(); x>0; x--) {
					ps.setString(1, uuid.getUUID());
					ps.setString(2,  vo.getCampaignLogId());
					ps.setTimestamp(3,  Convert.formatTimestamp(vo.getSentDate() != null ? vo.getSentDate() : new Date()));
					ps.addBatch();
				}
				ps.executeBatch();

			} catch (Exception e) {
				log.error("could not save email_response for " + vo.getCampaignLogId(), e);
			}
		}
	}


	/**
	 * @return
	 */
	private int getDataCount() {
		StringBuilder sql = new StringBuilder(200);
		sql.append("select count(*) from biomedgps.emaillog_emaillog a ");
		sql.append("inner join biomedgps.profiles_user b on lower(a.email)=lower(b.email) ");
		sql.append("inner join custom.biomedgps_user c on cast(b.id as varchar)=c.user_id ");
		sql.append("where a.last_update > '").append(props.getProperty("minEmailSqlDt")).append("' and c.profile_id is not null ");
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			return rs.next() ? rs.getInt(1) : 0;
		} catch (SQLException sqle) {
			log.error("could not get row count", sqle);
		}
		return 0;
	}


	/**
	 * @return
	 */
	private List<EmailLogVO> loadData(int limit, int offset) {
		List<EmailLogVO> records = new ArrayList<>(100000);
		StringBuilder sql = new StringBuilder(200);
		sql.append("select a.*, c.profile_id from biomedgps.emaillog_emaillog a ");
		sql.append("inner join biomedgps.profiles_user b on lower(a.email)=lower(b.email) ");
		sql.append("inner join custom.biomedgps_user c on cast(b.id as varchar)=c.user_id ");
		sql.append("where a.last_update > '").append(props.getProperty("minEmailSqlDt")).append("' and c.profile_id is not null ");
		sql.append("order by a.id limit ").append(limit).append(" offset " ).append(offset);
		log.debug(sql);

		String lastId = "";
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
//				EmailLogVO vo = new EmailLogVO();
//				vo.setProfileId(rs.getString("profile_id"));
//				vo.setCampaignLogId("SMARTTRAK_" + rs.getInt("id"));
//				vo.setSubject(rs.getString("subject"));
//				vo.setOpenCnt(rs.getInt("view_count"));
//				vo.setMessageBody(minify(rs.getString("message")));
//				vo.setSentDate(rs.getDate("last_update"));
//
//				if (!lastId.equals(vo.getCampaignLogId())) {
//					records.add(vo);
//					lastId = vo.getCampaignLogId();
//				} else {
//					//these can be ignored - We're picking up dups on the profileId side, not the email side. (same profile_id, multiple legacy user accounts)
//				}
			}

		} catch (SQLException sqle) {
			log.error("could not load lookups from: " + sql, sqle);
		}
		log.debug("loaded " + records.size() + " records");
		return records;
	}


	/**
	 * a feeble attempt to strip wasted space from the large email bodies we're importing
	 * @param string
	 * @return
	 */
	private String minify(String s) {
		return s != null ? s.replaceAll("\\s{2,}"," ") : null;
	}
}