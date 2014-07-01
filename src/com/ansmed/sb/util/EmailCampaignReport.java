package com.ansmed.sb.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: EmaillCampaignReport.java</p>
 <p>Description: <b/></p>
 <p>Copyright: Copyright (c) 2011 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author Dave Bargerhuff
 @version 1.0
 @since Apr 15, 2011
 Last Updated:
 ***************************************************************************/

public class EmailCampaignReport {
	Logger log = null;
	Map<String, Object> config = new HashMap<String, Object>();
	
	/**
	 * 
	 */
	public EmailCampaignReport() {
	}
	
	public static void main(String[] args) {
		EmailCampaignReport bp = new EmailCampaignReport();
		
		// Get the Config
		Properties p = bp.loadConfiguration("scripts/report_config.properties");
		
		bp.log = Logger.getLogger(EmailCampaignReport.class);
		PropertyConfigurator.configure("scripts/report_log4j.properties");
		
		bp.config.put(Constants.ENCRYPT_KEY, p.getProperty("encryptKey"));
		List<EmailCampaignReportVO> responses = null;
		// Connect to the database
		bp.log.info("driver/url/user/pwd: " + p.getProperty("dbDriver") +"/"+p.getProperty("dbUrl")+"/"+p.getProperty("dbUser")+"/"+p.getProperty("dbPassword"));
		Connection conn = null;
		DatabaseConnection dbc = new DatabaseConnection(p.getProperty("dbDriver"),p.getProperty("dbUrl"),p.getProperty("dbUser"),p.getProperty("dbPassword"));
		bp.log.info("Starting EmailCampaignReport");
		try {
			conn = dbc.getConnection();
			responses = bp.process(p.getProperty("encryptKey"), p.getProperty("sbANSSchema"),p.getProperty("sbSchema"), conn, false);
		} catch (Exception de) {
			de.printStackTrace();
			System.exit(-1);
		} finally {
			try {
				conn.close();
			} catch(Exception e) {}
		}
		
		try {
			bp.buildResponseReport(p, responses);
		} catch (Exception e) {
			bp.log.error("Error building report, ", e);
		}
		
	}
	
	/**
	 * Gets the sales rep info from the db and stores it to the profile table
	 * @param encKey
	 * @param schema
	 * @param conn
	 * @throws SQLException
	 */
	protected List<EmailCampaignReportVO> process(String encKey, String ansSchema, String sbSchema, Connection conn, boolean multi) 
	throws SQLException {
		
		String[] ids = {"c0a8024183e8f3b273c4771c4954d47"};
		//String id = "c0a80241a208a92410848188393b90e";
		String dStart = "2011-12-07";
		String dEnd = "2012-01-10";
		dStart = null;
		dEnd = null;
		
		log.debug("dStart: " + Convert.formatSQLDate(Convert.formatStartDate(dStart, "1/1/2000")));
		log.debug("dEnd: " + Convert.formatSQLDate(Convert.formatEndDate(dEnd)));
	
		StringBuffer sql = new StringBuffer();
		sql.append("select b.profile_id, c.campaign_instance_id, a.instance_nm, c.create_dt as send_dt, ");
		sql.append("d.create_dt as response_dt, d.response_type_id ");
		sql.append("from email_campaign_instance a inner join email_campaign_log c ");
		sql.append("on a.campaign_instance_id = c. campaign_instance_id ");
		sql.append("inner join profile b on b.profile_id = c.profile_id ");
		sql.append("left join email_response d on c.campaign_log_id = d.campaign_log_id ");
		sql.append("where 1 = 1 and c.campaign_instance_id in (");
		for (int i = 0; i < ids.length; i++) {
			sql.append("'").append(ids[i]).append("'");
			if (i < (ids.length - 1)) sql.append(",");
		}
		//sql.append("and ((d.create_dt between ? and ?) or d.create_dt is null) ");
		sql.append(") and (d.create_dt between ? and ?) ");
		sql.append("order by d.create_dt"); 
		
		log.debug("report SQL: " + sql.toString());
		
		EmailCampaignReportVO evo = null;
		List<EmailCampaignReportVO> responses = new ArrayList<EmailCampaignReportVO>();
		List<String> profiles = new ArrayList<String>();
		try {
			PreparedStatement ps = conn.prepareStatement(sql.toString());
			//ps.setString(1, id);
			ps.setDate(1, Convert.formatSQLDate(Convert.formatStartDate(dStart, "1/1/2000")));
			ps.setDate(2, Convert.formatSQLDate(Convert.formatEndDate(dEnd)));
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				evo = new EmailCampaignReportVO();
				evo.setData(rs);
				responses.add(evo);
				if (! profiles.contains(rs.getString("profile_id"))) {
					profiles.add(rs.getString("profile_id"));
				}
			}			
		} catch(Exception e) {
			log.error("Error retrieving email campaign responses, ", e);
		}
		
		// retrieve patient profile(s)
		ProfileManager pm = ProfileManagerFactory.getInstance(config);
		try {
			Map<String,UserDataVO> profileMap = pm.searchProfileMap(conn, profiles);
			for (EmailCampaignReportVO vo : responses) {
				vo.getUser().setData(profileMap.get(vo.getProfileId()).getDataMap());
			}
		} catch (Exception e) {
			log.error("Error retrieving profile data for patient(s)", e);
		}
		return responses;
	}
	
	/**
	 * 
	 * @param responses
	 */
	public void buildResponseReport(Properties p, List<EmailCampaignReportVO> responses) 
	throws Exception {
			
		StringBuffer sb = new StringBuffer();
		sb.append("<table border=\"1\"><tr>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Name</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Email Address</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Send Date</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Response Date</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Email Campaign Instance</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Instance Name</td>");
		sb.append("</tr>");
		
		for (EmailCampaignReportVO sv : responses) {
			sb.append("<tr>");
			sb.append("<td>").append(sv.getUser().getFirstName()).append(" ").append(sv.getUser().getLastName());
			sb.append("</td>");
			sb.append("</td>");
			sb.append("<td>").append(sv.getUser().getEmailAddress()).append("</td>");
			sb.append("<td>").append(sv.getSendDate()).append("</td>");
			sb.append("<td>").append(sv.getResponseDate()).append("</td>");
			sb.append("<td>").append(sv.getInstanceId()).append("</td>");
			sb.append("<td>").append(sv.getInstanceName()).append("</td>");
			sb.append("</tr>");
		}
		sb.append("</table>");
		
		writeFile(p, sb.toString().getBytes());

	}
	
	/**
	 * Writes report file to file system.
	 * @param p
	 * @param report
	 * @throws IOException
	 */
	private void writeFile(Properties p, byte[] report) 
	throws IOException {
		FileOutputStream fos = null;
		
		try {
			fos = new FileOutputStream((String)p.getProperty("reportFile"));
			fos.write(report);
			fos.close();
		} catch (IOException ioe) {
			throw new IOException(ioe.toString());
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (Exception e) {}
			}
		}
	}
	
	/**
	 * Loads the config properties to be used in the app
	 * @param path
	 * @return
	 */
	public Properties loadConfiguration(String path) {
		Properties config = new Properties();
		InputStream inStream = null;
		try {
			inStream = new FileInputStream(path);
			config.load(inStream);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (inStream != null) {
				try {
					inStream.close();
				} catch (Exception e) {}
			}
		}
		return config;
	}
}
