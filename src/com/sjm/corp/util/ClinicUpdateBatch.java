package com.sjm.corp.util;

// JDK 1.7
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Apache Log4j
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// SMTBaseLibs 2.0
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.CommandLineParser;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.SMTMail;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.parser.PropertyParser;
import com.smt.sitebuilder.action.dealer.DealerLocationVO;

/****************************************************************************
 * <b>Title</b>: ClinicUpdateBatch.java <p/>
 * <b>Project</b>: SB_ANS_Medical <p/>
 * <b>Description: </b> <p>Weekly batch report that sends an email to the 
 * clinic country admin for all changes that occured the previous week
 * <p/>
 * <p>
 * Params:<br/>
 * -s Date to start the report <br/>
 * -e Date to end the report.  DEFAULT: current date - one day<br/>
 * -d Report duration (in days).  Alternate method to be used if start date is not specified. 
 * Used for the batch where as the start date would be utilized at the command line.
 * Starts at the end date and subtract the number of days in the duration.
 * DEFAULT: 7 days <br/>
 * </p>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Apr 6, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class ClinicUpdateBatch {
	private static Logger log = Logger.getLogger("ClinicUpdateBatch");
	private Date startDate = null;
	private Date endDate = Convert.formatStartDate(new Date());
	private int duration = 0;
	private Connection conn = null;
	private Set<String> countries = new HashSet<String>();
	private Map<String, Object> config = null;
	protected Throwable error = null;
	
	/**
	 * 
	 */
	public static final int DEFAULT_REPORT_DURATION = 7; 
	
	/**
	 * 
	 */
	public ClinicUpdateBatch(String[] args) {
		PropertyConfigurator.configure("scripts/sjm_corp_log4j.properties");
		try {
			initializeParams(args);
			config = PropertyParser.assignParams("scripts/sjm_corp_config.properties");
			conn = this.getDBConnection();
		} catch (Exception e) {
			e.printStackTrace();
			error = e;
		} 
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		ClinicUpdateBatch cub = new ClinicUpdateBatch(args);
		cub.processRequest();
	}
	
	/**
	 * 
	 */
	public void processRequest() {
		try {
			List<DealerLocationVO> data = this.getUpdates();
			Map<String, String> cAdmins = this.getCountryAdmins();
			
			//loop the country admins and locations and format the emails
			this.sendEmails(data, cAdmins);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param locs
	 * @param cAdmins
	 * @throws MailException
	 */
	public void sendEmails(List<DealerLocationVO> locs, Map<String, String> cAdmins) 
	throws MailException {
		log.debug("Admins: " + cAdmins.size());
		Set<String> s = cAdmins.keySet();
		for (Iterator<String> iter= s.iterator(); iter.hasNext(); ) {
			
			String cntry = iter.next();
			String emailAddress = cAdmins.get(cntry);
			int ctr = 0;
			StringBuilder sb = new StringBuilder();
			for (int i=0; i < locs.size(); i++) {
				DealerLocationVO dlr = locs.get(i);
				if (! cntry.equalsIgnoreCase(dlr.getCountry())) continue;
				
				boolean optOut = false;
				if ("OPT_OUT".equalsIgnoreCase(dlr.getRegionCode())) optOut = true;
				ctr++;
				String rType = "Clinic Update";
				if (dlr.getUpdateDate() == null) rType = "Clinic Register";
				sb.append("<b>Clinic Name: </b>").append(dlr.getLocationName()).append("\n<br/>");
				sb.append("<b>Address: </b>").append(dlr.getAddress()).append("\n<br/>");
				sb.append("<b>City, Postal Code:</b> ").append(dlr.getCity()).append(", ");
				sb.append(dlr.getZipCode()).append("\n<br/>");
				sb.append("<b>Phone: </b>").append(dlr.getPhone()).append("\n<br/>");
				sb.append("<b>Website:</b> ").append(dlr.getWebsite()).append("\n<br/>");
				sb.append("<b>Pacemaker: </b>").append(Convert.formatBoolean(dlr.getBarCode())).append("\n<br/>");
				sb.append("<b>Defibrillator:</b> ").append(Convert.formatBoolean(dlr.getCassValidateFlag())).append("\n<br/>");
				sb.append("<b>Opt Out:</b> ").append(optOut).append("\n<br/>");
				sb.append("<b>Request Type:</b> ").append(rType).append("\n<br/>");
				sb.append("**********************************************\n<br/>");
			}
			
			if (sb.length() > 0) this.sendEmail(sb.toString(), emailAddress, ctr);
			
			// Country Count
		}
	}
	
	/**
	 * Sends the email
	 * @param dlr
	 * @param html
	 * @param email
	 * @throws MailException
	 */
	public void sendEmail(String html, String email, int count) 
	throws MailException {
		SMTMail mail = new SMTMail();
		mail.setSmtpServer((String) config.get("smtpServer"));
		mail.setPort(Convert.formatInteger((String)config.get("smtpPort")));
		mail.setUser((String) config.get("smtpUser"));
		mail.setPassword((String) config.get("smtpPassword"));
		mail.setSubject("Weekly Clinic Updates for St. Jude Medical International Clinic Locator");
		mail.setReplyTo("cliniclocator@sjm.com");
		mail.setFrom("cliniclocator@sjm.com");
		//mail.setRecpt(new String[] {email});
		// TODO 2014-03-10 Remove when testing finished.
		mail.setRecpt(new String[] {"dave@siliconmtn.com"});
		
		String hVal = "<h3>The report below highlights your clinic approval/update requests for the week.</h3>";
		hVal += "To access the St. Jude Medical Clinic Locator tool, visit ";
		hVal += "www.sjmcliniclocator.com/clinic_admin</a>.\n<br/>\n<br/>";
		hVal += "Number of clinics modified: " + count + "\n<br/>\n<br/>";
		hVal += "**********************************************\n<br/>";
		hVal += html;
		
		System.out.println(hVal);
		mail.setHtmlBody(hVal);
		mail.postMail();
	}
	
	/**
	 * Retrieves a list of locations updated or created
	 * @return
	 * @throws SQLException
	 */
	public List<DealerLocationVO> getUpdates() throws SQLException {
		String s = "select b.* from dealer a inner join dealer_location b ";
		s += "on a.dealer_id = b.dealer_id where organization_id = 'SJM_CORP_LOC' ";
		s += "and dealer_type_id = 5 and ";
		s += "(b.create_dt between ? and ? or b.update_dt between ? and ?) ";
		s += "order by location_nm ";
		log.debug("Batch SQL: " + s);
		
		List<DealerLocationVO> data = new ArrayList<DealerLocationVO>();
		PreparedStatement ps = conn.prepareStatement(s);
		ps.setDate(1, Convert.formatSQLDate(startDate));
		ps.setDate(2, Convert.formatSQLDate(endDate));
		ps.setDate(3, Convert.formatSQLDate(startDate));
		ps.setDate(4, Convert.formatSQLDate(endDate));
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			data.add(new DealerLocationVO(rs));
			countries.add(rs.getString("country_cd"));
		}
		
		log.debug("Number of Updates: " + data.size());
		
		return data;
	}
	
	/**
	 * Retrieves a collection of country codes and the email address for the admin
	 * @return
	 * @throws EncryptionException 
	 */
	public Map<String, String> getCountryAdmins() 
	throws SQLException, EncryptionException {
		Map<String, String> admins = new HashMap<String, String>();
		
		if (countries.size() > 0) {
			StringBuilder s = new StringBuilder();
			int i=0; 
			
			for(Iterator<String> iter = countries.iterator(); iter.hasNext(); i++) {
				if (i > 0) s.append(",");
				s.append(StringUtil.checkVal(iter.next(), true));
			}
			
			String sql = "select attrib_txt_1, email_address_txt from profile_role a ";
			sql += "inner join profile b on a.profile_id = b.profile_id ";
			sql += "where attrib_txt_1 in (" + s + ") ";
			log.debug("Country Admins in: " + sql + "|" + s);
			
			StringEncrypter se = new StringEncrypter((String)config.get("encryptKey"));
			PreparedStatement ps = conn.prepareStatement(sql);
			//ps.setString(1, s.toString());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				admins.put(rs.getString(1), se.decrypt(rs.getString(2)));
			}
		}
		log.debug("Country Admin Size: " + admins);
		return admins;
	}
	
	/**
	 * 
	 * @param args
	 * @throws InvalidDataException 
	 */
	public void initializeParams(String[] args) throws InvalidDataException {
		Map<String, Object> params = CommandLineParser.switchParser(args);
		log.debug("Params [s]: " + params.get("s"));
		log.debug("Params: [e]: " + params.get("e"));
		log.debug("Params: [d]: " + params.get("d"));
		
		// Calculate the report end date
		if (params.get("e") != null) {
			endDate = Convert.formatEndDate((Date)params.get("e"));
		}
		
		// Calculate the start date.  Passed date takes precedence over the 
		// duration
		if (params.get("s") != null) {
			startDate = (Date) params.get("s");
		} else {
			duration = Convert.formatInteger((String)params.get("d"), DEFAULT_REPORT_DURATION);
			startDate = Convert.formatDate(endDate, Calendar.DAY_OF_YEAR, duration * -1);
		}
		
		log.debug("Start Date: " + startDate);
		log.debug("End Date: " + endDate);
		log.debug("Duration: " + duration);
	}

	/**
	 * Creates a DB connection using the provided properties
	 * @param p
	 * @return
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public Connection getDBConnection() 
	throws InvalidDataException, DatabaseException {
		String driver = (String) config.get("dbDriver");
		String url = (String) config.get("dbUrl");
		String dbUser = (String) config.get("dbUser");
		String pwd = (String) config.get("dbPassword");
		log.debug("dbDriver|dbUrl|dbUser|dbPassword: " + driver + "|" + url + "|" + dbUser + "|" + pwd);
		DatabaseConnection dbc = new DatabaseConnection(driver,url,dbUser,pwd);
		
		return dbc.getConnection();
	}
}
