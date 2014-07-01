package com.depuy.scripts;

// JDK 1.6.x
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

// Log4j 1.2.15
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

// SMT Base Libs
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.io.mail.MailHandlerFactory;
import com.siliconmtn.io.mail.mta.MailTransportAgentIntfc;

/****************************************************************************
 * <b>Title</b>: DePuyLeadReportEmail.java <p/>
 * <b>Project</b>: SB_DePuy <p/>
 * <b>Description: </b> Gets Leads/Qualified Leads for the the DePuy data feed
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Jun 20, 2013<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class DePuyLeadReportEmail {
	
	/**
	 * List of errors 
	 */
	List <Exception> failures = new ArrayList<Exception>();

	// Member Variables
    protected static final Logger log = Logger.getLogger(DePuyLeadReportEmail.class);
    private Connection conn = null;
    private Properties props = null;
    
	/**
	 * 
	 */
	public DePuyLeadReportEmail() throws DatabaseException, InvalidDataException {
		// initialize the logger, properties and DB connection
		BasicConfigurator.configure();
		loadProperties();
		getDBConnection();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DePuyLeadReportEmail dlre = null;
		
		StringBuilder s = new StringBuilder();
		try {
			dlre = new DePuyLeadReportEmail();
			log.debug("Starting DePuy Report");
			log.error(dlre.getSQLQuery());
			System.exit(0);
			List<ReportVO> data = dlre.buildReport();
			s = dlre.formatReport(data);
			
			log.debug("Report: " + s);
			
		} catch (Exception e) {
			if (dlre != null) dlre.failures.add(e);
			log.error("Error generating the report", e);
		}
		
		// Send the report
		dlre.sendEmail(s);
		
		log.debug("DePuy Report Complete");
	}
	
	/**
	 * Formats the report into an HTML Table
	 * @param data
	 * @return
	 */
	public StringBuilder formatReport(List<ReportVO> data) {
		StringBuilder s = new StringBuilder();
		s.append("<table border='1' style='border-collapse: collapse;'>");
		s.append("<tr style='background:black;color: white;'>");
		s.append("<th style='padding:5px;'>Date Range</th>");
		s.append("<th style='padding:5px;'>Knee Lead</th>");
		s.append("<th style='padding:5px;'>Knee Qualified (3,4,5)</th>");
		s.append("<th style='padding:5px;'>Knee Qualified (4,5)</th>");
		s.append("<th style='padding:5px;'>Hip Lead</th>");
		s.append("<th style='padding:5px;'>Hip Qualified (3,4,5)</th>");
		s.append("<th style='padding:5px;'>Hip Qualified(4,5) </th></tr>");
		
		for (int i = 0; i < data.size(); i++) {
			ReportVO r = data.get(i);
			s.append("<tr><td style='padding:5px;'>").append(r.name).append("</td>");
			s.append("<td style='padding:5px;'>").append(r.kneeLead).append("</td>");
			s.append("<td style='padding:5px;'>").append(r.kneeQualLead345).append("</td>");
			s.append("<td style='padding:5px;'>").append(r.kneeQualLead45).append("</td>");
			s.append("<td style='padding:5px;'>").append(r.hipLead).append("</td>");
			s.append("<td style='padding:5px;'>").append(r.hipLead345).append("</td>");
			s.append("<td style='padding:5px;'>").append(r.hipLead45).append("</td></tr>");
		}
		
		s.append("</table>");
		
		return s;
	}
	
	/**
	 * Queries the database and places the data into a collection of report vos
	 * @return
	 * @throws SQLException
	 */
	public List<ReportVO> buildReport() throws SQLException {
		String sql = this.getSQLQuery();
		PreparedStatement ps = conn.prepareStatement(sql);
		ResultSet rs = ps.executeQuery();
		List<ReportVO> data = new ArrayList<ReportVO>();
		
		while(rs.next()) {
			data.add(new ReportVO(rs));
		}
		
		return data;
	}
	
	/**
	 * Builds the sql statement for the report
	 * @return
	 */
	public String getSQLQuery() {
		StringBuilder s = new StringBuilder();
		s.append("select '7/1/2011 to ' + convert(varchar, getdate(), 101), ");
		s.append("sum(case when PRODUCT_CD = 'KNEE' and LEAD_TYPE_ID = 5 then 1 else 0 end) as knee_lead, ");
		s.append("sum(case when PRODUCT_CD = 'KNEE' and LEAD_TYPE_ID = 10 and ATTEMPT_DT > '2012-01-01' then 1 else 0 end) as knee_qualified_lead_345, ");
		s.append("sum(case when PRODUCT_CD = 'KNEE' and LEAD_TYPE_ID = 10 and ATTEMPT_DT < '2012-01-01' then 1 else 0 end) as knee_qualified_lead_45, ");
		s.append("sum(case when PRODUCT_CD = 'HIP' and LEAD_TYPE_ID = 5 then 1 else 0 end) as hip_lead, ");
		s.append("sum(case when PRODUCT_CD = 'HIP' and LEAD_TYPE_ID = 10 and ATTEMPT_DT > '2012-01-01' then 1 else 0 end) as hip_qualified_lead_345, ");
		s.append("sum(case when PRODUCT_CD = 'HIP' and LEAD_TYPE_ID = 10 and ATTEMPT_DT < '2012-01-01' then 1 else 0 end) as hip_qualified_lead_45 ");
		s.append("from data_feed.dbo.customer ");
		s.append("where attempt_dt > '2011-07-01' and LEAD_TYPE_ID > = 5 and PRODUCT_CD in ('HIP', 'KNEE') ");
		s.append("union ");
		s.append("select convert(varchar, DATEADD(week, -2, GETDATE()), 101) + ' to ' + convert(varchar, getdate(), 101), ");
		s.append("sum(case when PRODUCT_CD = 'KNEE' and LEAD_TYPE_ID = 5 then 1 else 0 end) as knee_lead, ");
		s.append("sum(case when PRODUCT_CD = 'KNEE' and LEAD_TYPE_ID = 10 then 1 else 0 end) as knee_qualified_lead, ");
		s.append("0, ");
		s.append("sum(case when PRODUCT_CD = 'HIP' and LEAD_TYPE_ID = 5 then 1 else 0 end) as hip_lead, ");
		s.append("sum(case when PRODUCT_CD = 'HIP' and LEAD_TYPE_ID = 10 then 1 else 0 end) as hip_qualified_lead, ");
		s.append("0 ");
		s.append("from data_feed.dbo.customer ");
		s.append("where attempt_dt > DATEADD(week, -2, GETDATE()) and LEAD_TYPE_ID > = 5 and PRODUCT_CD in ('HIP', 'KNEE') ");
		log.debug("SQL: " + s);
		
		return s.toString();
	}
	
	/**
	 * Sends an email to the person specified in the properties file as to whether 
	 * the insert was a success or a failure.
	 */
	private void sendEmail(StringBuilder report) {
		try {
			// Build the email message
			EmailMessageVO msg = new EmailMessageVO(); 
			List<String> recips = Arrays.asList(props.getProperty("adminEmail").split(","));
			for (int i=0; i < recips.size(); i++) {
				log.debug("Recipient: " + recips.get(i));
				msg.addRecipient(recips.get(i));
			}
			
			msg.setSubject("DePuy CRM Leads Report");
			msg.setFrom("appsupport@siliconmtn.com");
			
			StringBuilder html= new StringBuilder();
			html.append("<h3>Leads / Qualified Leads Report</h3>");
			html.append(report.toString());
			
			// loop the errors and display them
			if (failures != null && failures.size() > 0) {
				html.append("<b>Report failed to load due to these errors:</b><ol>");
				for (int i=0; i < failures.size(); i++)
					html.append("<li>").append(failures.get(i).getMessage()).append("</li>");
				html.append("</ol>");
			}
			msg.setHtmlBody(html.toString());
			
			MailTransportAgentIntfc mail = MailHandlerFactory.getDefaultMTA(props);
			mail.sendMessage(msg);
		} catch (Exception e) {
			log.error("Could not send report email, ", e);
		}
	}
	
	/**
	 * Load the properties file in order to get the database and email 
	 */
	private void loadProperties() {
		FileInputStream fis = null;
		props = new Properties();
		try {
			fis = new FileInputStream("scripts/LeadReport.properties");
			props.load(fis);
		} catch (IOException e) {
			log.error("Could not load config file", e);
			System.exit(-1);
		} finally {
			try { fis.close(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Connects to the database based on the values in the property file
	 * @throws Exception
	 */
	private void getDBConnection() throws DatabaseException, InvalidDataException {
		DatabaseConnection dbc = new DatabaseConnection();
		dbc.setDriverClass((String) props.get("dbDriver"));
		dbc.setUrl((String) props.get("dbUrl"));
		dbc.setUserName((String) props.get("dbUser"));
		dbc.setPassword((String) props.get("dbPassword"));
		conn = dbc.getConnection();
	}
	
	/**
	 * Helper vo to place data into for each row returned from the result set
	 * @author james
	 *
	 */
	class ReportVO {
		String name = null;
		int kneeLead = 0;
		int kneeQualLead45 = 0;
		int kneeQualLead345 = 0;
		int hipLead = 0;
		int hipLead45 = 0;
		int hipLead345 = 0;
		
		/**
		 * Creates a new VO and populates it with a row from the db
		 * @param rs
		 */
		ReportVO(ResultSet rs) throws SQLException {
			name = rs.getString(1);
			kneeLead = rs.getInt(2);
			kneeQualLead345 = rs.getInt(3);
			kneeQualLead45 = rs.getInt(4);
			hipLead = rs.getInt(5);
			hipLead345 = rs.getInt(6);
			hipLead45 = rs.getInt(7);
		}
	}
}
