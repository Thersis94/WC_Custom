package com.depuy.scripts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.util.SMTMail;
import com.smt.sitebuilder.report.email.RollupVO;

/****************************************************************************
 * <b>Title</b>: EmailCampaignReport.java <p/>
 * <b>Project</b>: SB_DePuy <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Sep 19, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class EmailCampaignReport {
	private Connection conn = null;
	private static final Logger log = Logger.getLogger("EmailCampaignReport");
	
	/**
	 * 
	 */
	public EmailCampaignReport() throws Exception {
		// initialize the logger
		BasicConfigurator.configure();
		
		// Get the database connection
		DatabaseConnection dbc = new DatabaseConnection();
		dbc.setDriverClass("com.microsoft.sqlserver.jdbc.SQLServerDriver");
		dbc.setUrl("jdbc:sqlserver://192.168.3.120:2007;selectMethod=cursor;responseBuffering=adaptive");
		dbc.setUserName("sb_user");
		dbc.setPassword("sqll0gin");
		conn = dbc.getConnection();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		EmailCampaignReport ecr = new EmailCampaignReport();
		
		log.debug("starting report");
		
		// Retrieve the data
		List<RollupVO> data = ecr.getData();
		log.debug("Retrieved log reports: " + data.size());
		
		// Format the report
		byte[] report = ecr.generateReport(data);
		log.debug("Completed formatting report");
		
		// send the email
		ecr.sendEmail(report);
		
		log.debug("Complete");
	}
	
	/**
	 * 
	 * @param attach
	 * @throws MailException
	 */
	public void sendEmail(byte[] attach) throws MailException {
		SMTMail email = new SMTMail();
		email.setUser("ac47580");
		email.setPassword("h0rchata_987");
		email.setPort(25);
		email.setSmtpServer("62.13.128.194");
		
		email.setSubject("Monthly Email Campaign Report");
		email.setFrom("info@siliconmtn.com");
		email.addRecipient("james@siliconmtn.com");
		email.addAttachment("report.xls", attach);
		email.postMail();
	}
	
	/**
	 * Formats the data into a tab delimited format
	 * @param data
	 * @return
	 */
	public byte[] generateReport(List<RollupVO> data) {
		StringBuilder sb = new StringBuilder();
		sb.append("Campaign Name").append("\t");
		sb.append("Success").append("\t");
		sb.append("Failed").append("\t");
		sb.append("Opened").append("\t");
		sb.append("Uniuqe Opens").append("\t");
		sb.append("Redirects").append("\t");
		sb.append("Unique Redirects").append("\t");
		sb.append("Unique User Redirects").append("\t\n");
		
		for (int i=0; i < data.size(); i++) {
			RollupVO r = data.get(i);
			
			sb.append(r.getInstanceName()).append("\t");
			sb.append(r.getEmailSuccessNumber()).append("\t");
			sb.append(r.getEmailBounceNumber()).append("\t");
			sb.append(r.getEmailOpenNumber()).append("\t");
			sb.append(r.getUniqueUserOpenNumber()).append("\t");
			sb.append(r.getEmailRedirectNumber()).append("\t");
			sb.append(r.getUniqueRedirectNumber()).append("\t");
			sb.append(r.getUniqueRedirectUser()).append("\t\n");
		}
		
		return sb.toString().getBytes();
	}
	
	/**
	 * 
	 * @return
	 * @throws SQLException
	 */
	public List<RollupVO> getData() throws SQLException {
		String sql = "select * from email_campaign_report a ";
		sql += "inner join email_campaign_instance b on a.campaign_instance_id = b.campaign_instance_id ";
		sql += "inner join email_campaign c on b.email_campaign_id = c.email_campaign_id ";
		sql += "where c.email_campaign_id in ('c0a80223b3ce644e36b5b55867ebffd0','c0a80223b3cf12116dc6a93f5ebbd12e ') ";
		sql += "order by instance_nm, c.email_campaign_id  ";
		log.debug("Data SQL: " + sql);
		
		PreparedStatement ps = conn.prepareStatement(sql);
		ResultSet rs = ps.executeQuery();
		List<RollupVO> data = new ArrayList<RollupVO>();
		String id = "", currId = "";
		RollupVO totals = new RollupVO();
		totals.setInstanceName("Total:");
		
		for (int i=0; rs.next(); i++) {
			id = rs.getString("email_campaign_id");
			log.debug("Adding data for : " + rs.getString("instance_nm"));
			RollupVO curr = new RollupVO(rs);
			
			if (i > 0 && ! id.equals(currId)) {
				data.add(totals);
				
				totals = new RollupVO();
				totals.setInstanceName("Total:");
			}
			
			// Update the subtotals
			totals.setEmailSuccessNumber(totals.getEmailSuccessNumber() + curr.getEmailSuccessNumber());
			totals.setEmailBounceNumber(totals.getEmailBounceNumber() + curr.getEmailBounceNumber());
			totals.setEmailOpenNumber(totals.getEmailOpenNumber() + curr.getEmailOpenNumber());
			totals.setUniqueUserOpenNumber(totals.getUniqueUserOpenNumber() + curr.getUniqueUserOpenNumber());
			totals.setEmailRedirectNumber(totals.getEmailRedirectNumber() + curr.getEmailRedirectNumber());
			totals.setUniqueRedirectNumber(totals.getUniqueRedirectNumber() + curr.getUniqueRedirectNumber());
			totals.setUniqueRedirectUser(totals.getUniqueRedirectUser() + curr.getUniqueRedirectUser());
			data.add(curr);
			
			currId = id;
		}
		
		data.add(totals);
		return data;
	}

}
