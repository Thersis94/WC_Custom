package com.ansmed.sb.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.ansmed.sb.physician.ClinicVO;
import com.ansmed.sb.physician.SurgeonVO;
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.SMTMail;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>:WeeklyReport.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2008<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Jan 10, 2008
 ****************************************************************************/
public class WeeklyReport {
	Logger log = Logger.getLogger(WeeklyReport.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		WeeklyReport wr = new WeeklyReport();
		StringBuffer report = new StringBuffer();

		// Get the Config
		Properties p = wr.loadConfiguration("scripts/ans_config.properties");
		PropertyConfigurator.configure("scripts/ans_log4j.properties");
		report.append("\n").append("Starting Weekly Physician Change Report").append("\n");
		String schema = p.getProperty("sbANSSchema");
		wr.log.info("Starting Weekly Physician Change Report");
		
		// Assign the end date to today and the duration to define the start date
		Date end = new Date();
		Integer duration = Integer.valueOf(-8);
		Calendar cal = Calendar.getInstance();
		cal.setTime(end);
		cal.add(Calendar.DAY_OF_YEAR, duration);
		
		report.append("Starting Date: ").append(cal.getTime()).append("\n");
		report.append("\n").append("Ending Date: ").append(end).append("\n");
		
		// Get the DB connection
		Connection conn = wr.getDBConnection(p);
		
		// retrieve Physicians changed
		List<String> phys = wr.getSurgeonList(conn, schema, cal.getTime(), end);
		report.append("\n").append("Number of Physicians Modified: ").append(phys.size()).append("\n");
		
		// loop the physicians and get details for each
		for (int i=0; i < phys.size(); i++) {
			wr.formatData(report, wr.getSurgeon(conn, schema, phys.get(i)), cal.getTime(), schema, conn);
		}
		
		// Send the email report
		wr.sendEmail(p, report);
		
		// Close the DB conn
		try {
			conn.close();
		} catch(Exception e) {}
		
		wr.log.info("Report Complete");
	}
	
	/**
	 * Formats the text for the report
	 * @param sb
	 * @param vo
	 */
	public void formatData(StringBuffer sb, SurgeonVO vo, Date start, String schema, Connection conn) {
		sb.append("\n*******************************************\n");
		sb.append("Physician Name: ").append(vo.getFirstName()).append(" ").append(vo.getLastName()).append("\n");
		sb.append(this.getTransactionType(vo.getSurgeonId(), schema, start, conn)).append("\n");
		sb.append("Sales Rep: ").append(vo.getSalesRepName()).append("\n");
		sb.append("Email Address: ").append(vo.getEmailAddress()).append("\n");
		sb.append("Website: ").append(vo.getWebsite()).append("\n");
		sb.append("Spouse Name: ").append(vo.getSpouseName()).append("\n");
		sb.append("Children: ").append(vo.getChildrenName()).append("\n");
		
		List<ClinicVO> clinics = vo.getClinics();
		for (int i=0; i < clinics.size(); i++) {
			ClinicVO clinic = clinics.get(i);
			sb.append("\n");
			sb.append("Clinic Name: ").append(clinic.getClinicName()).append("\n");
			sb.append(clinic.getAddress()).append("\n");
			
			if (StringUtil.checkVal(clinic.getAddress2()).length() > 0)
				sb.append(clinic.getAddress2()).append("\n");
			
			sb.append(clinic.getCity()).append(" ").append(clinic.getState());
			sb.append(", ").append(clinic.getZipCode()).append("\n");
		}
	}
	
	/**
	 * Send the email report
	 * @param p
	 * @param sb
	 */
	public void sendEmail(Properties p, StringBuffer sb) {
		// COnfigure the Mail settings
		SMTMail mail = new SMTMail();
		mail.setSmtpServer(p.getProperty("smtpServer"));
		mail.setPort(Convert.formatInteger(p.getProperty("smtpPort")));
		mail.setUser(p.getProperty("smtpUser"));
		mail.setPassword(p.getProperty("smtpPassword"));
		mail.setFrom("info@siliconmtn.com");
		mail.setSubject("Weekly Physician Transaction Report");
		mail.setReplyTo("info@siliconmtn.com");
		mail.setRecpt(new String[] { p.getProperty("smtpRecipient") });
		mail.setTextBody(sb.toString());
		
		// Send the email
		try {
			mail.postMail();
		} catch (Exception e) {
			log.error("Error sending email", e);
		}
	}
	
	/**
	 * Returns the surgeon and clinic info for the provided physician
	 * @param conn
	 * @param schema
	 * @param id
	 * @return
	 */
	public SurgeonVO getSurgeon(Connection conn, String schema, String id) {
		StringBuffer s = new StringBuffer();
		s.append("select a.*, b.*, c.first_nm as first, c.last_nm as last from ").append(schema).append("ans_surgeon a ");
		s.append("inner join ").append(schema).append("ans_clinic b ");
		s.append("on a.surgeon_id = b.surgeon_id ");
		s.append("inner join ").append(schema).append("ans_sales_rep c ");
		s.append("on a.sales_rep_id = c.sales_rep_id ");
		s.append("where a.surgeon_id = ? ");
		log.debug("Phys Detail SQL: " + s + "|" + id);
		
		PreparedStatement ps = null;
		SurgeonVO vo = new SurgeonVO();
		try {
			ps = conn.prepareStatement(s.toString());
			ps.setString(1, id);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				vo.setData(rs);
				vo.setSalesRepName(rs.getString("first") + " " + rs.getString("last"));
			}
		} catch (SQLException e) {
			log.error("Error getting physician list", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		return vo;
	}
	
	/**
	 * Retrieves the transaction types for that physician
	 * @param id
	 * @param schema
	 * @param start
	 * @param conn
	 * @return
	 */
	public String getTransactionType(String id, String schema, Date start, Connection conn) {
		StringBuffer s = new StringBuffer();
		s.append("select transaction_type_nm from ").append(schema);
		s.append("ans_transaction where surgeon_id = ? and create_dt >= ? ");
		
		PreparedStatement ps = null;
		StringBuffer types = new StringBuffer("Types of Transactions: ");
		try {
			ps = conn.prepareStatement(s.toString());
			ps.setString(1, id);
			ps.setDate(2, Convert.formatSQLDate(start));
			ResultSet rs = ps.executeQuery();
			for (int i=0; rs.next(); i++) {
				if (i > 0 && StringUtil.checkVal(rs.getString(1)).length() > 0) 
					types.append(", ");
				
				types.append(StringUtil.checkVal(rs.getString(1)));
			}
		} catch (Exception e) {
			log.error("Error retrieving transaction types: " + id, e);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		
		return types.toString();
	}
		
	/**
	 * Returns a list of surgeon IDs for any surgeons updated during the 
	 * specified time period
	 * @param conn
	 * @param schema
	 * @param start
	 * @param end
	 * @return
	 */
	public List<String> getSurgeonList(Connection conn, String schema, Date start, Date end) {
		StringBuffer sql = new StringBuffer();
		sql.append("select surgeon_id from ");
		sql.append(schema).append("ans_transaction ");
		sql.append("where create_dt between ? and ? ");
		sql.append("group by surgeon_id ");
		
		List<String> data = new ArrayList<String>();
		log.debug("Find Physicians SQL: " + sql + "|" + start + "|" + end);
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql.toString());
			ps.setDate(1, Convert.formatSQLDate(start));
			ps.setDate(2, Convert.formatSQLDate(end));
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				data.add(rs.getString(1));
			}
		} catch (SQLException e) {
			log.error("Error getting physician list", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		
		return data;
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
	
	/**
	 * Creates a DB connection using the provided properties
	 * @param p
	 * @return
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public Connection getDBConnection(Properties p) 
	throws InvalidDataException, DatabaseException {
		String driver = p.getProperty("dbDriver");
		String url = p.getProperty("dbUrl");
		String dbUser = p.getProperty("dbUser");
		String pwd = p.getProperty("dbPassword");
		
		DatabaseConnection dbc = new DatabaseConnection(driver,url,dbUser,pwd);
		Connection conn = dbc.getConnection();
		return conn;
	}

}
