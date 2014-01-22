package com.fastsigns.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
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

import com.fastsigns.action.approval.vo.AbstractChangeLogVO;
import com.fastsigns.action.approval.vo.ChangeLogVO;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.SMTMail;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
/****************************************************************************
 * <b>Title</b>: ChangeLogReport.java<p/>
 * <b>Description: Simple class that is used to email FASTSIGNS eTeam on a 
 * schedule with pending changes.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Billy Larsen
 * @version 1.0
 * @since Jan 26, 2011
 ****************************************************************************/
public class ChangeLogReport {
	private Properties p;
	private Connection dbConn = null;
	private static final String DESTINATION_DB_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	protected static Logger log = null;
	private ChangeLogReportVO vo;
	private ProfileManager pm;
	private StringEncrypter se;
	private Map<String, Object> attributes;
	private Map<String, String> types;
	/**
	 * Insertion point to run stand alone
	 * @param args
	 */
	public static void main(String[] args) {
		PropertyConfigurator.configure("scripts/fts_log4j.properties");
		log = Logger.getLogger(ChangeLogReport.class);
		
		ChangeLogReport clr = new ChangeLogReport();
		clr.run();
	}
	/**
	 * Contains the general run logic.
	 */
	private void run() {
		vo = new ChangeLogReportVO();
		removeOrphanedEntries();
		log.debug("Setting Data");
		vo.setData(getData());
		log.debug("Preparing email");
		sendNotifications();
	}
	/**
	 * Constructor that loads and initializes class fields.
	 */
	public ChangeLogReport() {
		p = new Properties();
		try {
			InputStream is = new FileInputStream("scripts/fts_changelog_config.properties");
			p.load(is);
			attributes = loadAttributes();
			pm = ProfileManagerFactory.getInstance(attributes);
			se = new StringEncrypter( p.getProperty("wc.db.encKey"));
			
			dbConn = getDBConnection(p.getProperty("wc.db.user"), p.getProperty("wc.db.password"), DESTINATION_DB_DRIVER, p.getProperty("wc.db.url"));
			
		} catch (FileNotFoundException e) { 
			log.error("file not found", e);
		} catch (IOException e) { 
			log.error("io exception", e);
		} catch (EncryptionException e) {
			log.error("encryption", e);
		} catch (DatabaseException de) {
			log.error("no database connection", de);
		}
	}
	/**
	 * Build mail object and send the file.  If no pending changes sends an email
	 * with text only.
	 */
	private void sendNotifications() {
		try {
			SMTMail mail = new SMTMail(attributes.get("SMTP_EMAIL_SERVER").toString());
			mail.setUser(attributes.get("SMTP_EMAIL_USER").toString());
			mail.setPassword(attributes.get("SMTP_EMAIL_PASSWORD").toString());
			mail.setPort(Integer.valueOf(attributes.get("SMTP_EMAIL_PORT").toString()));
			mail.setRecpt(attributes.get("SMTP_EMAIL_ETEAM").toString().split(","));
			mail.setSubject("Web Edit Approval Report");
			mail.setFrom(attributes.get("SMTP_EMAIL_ETEAM").toString());
			if(vo.getCount() > 0)
				mail.addAttachment(vo.getFileName(), vo.generateReport());
			else
				mail.setTextBody("No Pending Submissions");
			
			mail.postMail();
			log.info("Email Sent");
			
		} catch (Exception me) {
			log.error("could not send approval process email to: " + attributes.get("SMTP_EMAIL_ETEAM"), me);
		}
		return;
	}
	/**
	 * Runs the SQL query that gathers pending change logs.
	 * @return List of pending ChangeLogVO Objects
	 */
	private List<AbstractChangeLogVO> getData() {
		List<AbstractChangeLogVO> vos = null;
		PreparedStatement ps = null;
		try{
			log.info("exportFile: " + vo.getFileName());
			
			vos = new ArrayList<AbstractChangeLogVO>();
			ps = dbConn.prepareStatement(query());
			ResultSet rs = ps.executeQuery();
			while(rs.next()){
				AbstractChangeLogVO v = getChangeLogVO(rs.getString("TYPE_ID"));
				v.setData(rs);
				vos.add(v);
			}

		} catch (SQLException sqle) {
			log.error(sqle);
		} finally {
				try{ ps.close(); } catch(Exception e) {}
		}
		
		 // Decrypt name fields and store in the related vo.
		for(AbstractChangeLogVO cl : vos) {
			try {
				UserDataVO p = pm.getProfile(cl.getSubmitterId(), dbConn, ProfileManager.PROFILE_ID_LOOKUP);
				cl.setSubmitterName(StringUtil.checkVal(se.decrypt(p.getFirstName())) + " " + StringUtil.checkVal(se.decrypt(p.getLastName())));
				
			} catch (com.siliconmtn.exception.DatabaseException e) {
				e.printStackTrace();
			} catch (EncryptionException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
		return vos;
	}

	/**
	 * 
	 * @param userName Login Account
	 * @param pwd Login password info
	 * @param driver Class to load
	 * @param url JDBC URL to call
	 * @return Database Connection object
	 * @throws DatabaseException
	 */
	private Connection getDBConnection(String userName, String pwd,
			String driver, String url) throws DatabaseException {
		// Load the Database jdbc driver
		try {
			Class.forName(driver);
		} catch (ClassNotFoundException cnfe) {
			throw new DatabaseException("Unable to find the Database Driver", cnfe);
		}
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(url, userName, pwd);
		} catch (SQLException sqle) {
			log.error("could not establish dbConn", sqle);
			throw new DatabaseException("Error Connecting to Database", sqle);
		}
		return conn;
	}
	/**
	 * Builds the query used to retrieve the pending changelogs.
	 * @return String representing the sql query
	 */
	private String query() {
		String c = p.getProperty("db.custom");
		String sql = "select a.type_id, a.desc_txt, a.submitted_dt, a.update_dt, "
				+ "a.submitter_id, b.franchise_id as modfranchise_id, b.option_nm, "
				+ "b.option_desc, c.new_center_image_url, c.FRANCHISE_ID as franchise_id, d.PAGE_DISPLAY_NM from "
				+ c + "FTS_CHANGELOG a left outer join " + c + "FTS_CP_MODULE_OPTION b "
				+ "on a.COMPONENT_ID = Cast(b.cp_module_option_id as nvarchar(32)) "
				+ "left outer join " + c + "FTS_FRANCHISE c on c.FRANCHISE_ID = b.FRANCHISE_ID "
				+ "or Cast(c.FRANCHISE_ID as nvarchar(32)) = a.COMPONENT_ID " 
				+ "left outer join PAGE d on a.COMPONENT_ID = d.PAGE_ID "
				+ "where a.status_no = 0 order by update_dt desc, submitted_dt desc";
		return sql;
	}
	/**
	 * Loads the fields from the config file into a Map for ease of access.
	 * @return Map of config attributes.
	 */
	private Map<String, Object> loadAttributes(){
		Map<String, Object> temp = new HashMap<String, Object>();
		temp.put("encKey", p.getProperty("wc.db.encKey"));
		temp.put("SMTP_EMAIL_SERVER", p.getProperty("wc.smtp.server"));
		temp.put("SMTP_EMAIL_USER", p.getProperty("wc.smtp.user"));
		temp.put("SMTP_EMAIL_PASSWORD", p.getProperty("wc.smtp.password"));
		temp.put("SMTP_EMAIL_PORT", p.getProperty("wc.smtp.port"));
		temp.put("SMTP_EMAIL_ETEAM", p.getProperty("fts.email.eteamemail"));
		temp.put("customDb", p.getProperty("db.custom"));
		temp.put("mainDb", p.getProperty("db.main"));
		return temp;
	}
	/**
	 * String to remove all orphaned Page Changelogs. 
	 * @return
	 */
	public String orphanPagesRemoval(){
		return "delete from " + attributes.get("customDb") + "FTS_CHANGELOG where STATUS_NO=0 " +
				"and TYPE_ID='sitePg' and not exists(select * from PAGE where PAGE_ID = COMPONENT_ID and LIVE_START_DT > '2100-01-01 00:00:00.000')";
	}
	/**
	 * String to remove all orphaned Module Changelogs. 
	 * @return
	 */
	public String orphanModuleRemoval(){
		return "delete from " + attributes.get("customDb") + "FTS_CHANGELOG where STATUS_NO=0 and " +
				"TYPE_ID='ctrPgModule' and not exists(select * from  " + attributes.get("customDb") + 
				"fts_cp_module_option a	inner join " + attributes.get("customDb") + "fts_cp_module_franchise_xr b " +
				"on a.cp_module_option_id=b.cp_module_option_id or a.parent_id=b.cp_module_option_id " +
				"inner join " + attributes.get("customDb") + "fts_cp_location_module_xr c on " +
				"c.cp_location_module_xr_id=b.cp_location_module_xr_id where a.approval_flg=100 " +
				"and b.CP_MODULE_OPTION_ID = COMPONENT_ID)";
	}
	
	private void removeOrphanedEntries(){		
		PreparedStatement ps = null;
		try{
				ps = dbConn.prepareStatement(orphanPagesRemoval());
				ps.execute();
				log.debug("Pages Removed");
				ps = dbConn.prepareStatement(orphanModuleRemoval());
				ps.execute();
				log.debug("Modules Removed");
		} catch(Exception e){
			e.printStackTrace();
		} finally{
			try{ps.close();}catch(Exception e){}
		}
	}
	
	/**
	 * Factory Method that returns the appropriate ChangeLogVO.
	 * 
	 * @param req
	 * @return
	 */
	private AbstractChangeLogVO getChangeLogVO(String typeId) {
		ChangeLogVO cL = null;
		try {
			Thread t = Thread.currentThread();
			ClassLoader cl = t.getContextClassLoader();
			Class<?> load = cl.loadClass(types.get(typeId));
			cL = (ChangeLogVO) load.newInstance();
		} catch (ClassNotFoundException cnfe) {
			log.error("Unable to find className", cnfe);
		} catch (Exception e) {
			log.error("Unable to create ChangeLog.", e);
		}
		return cL;
	}
	
}
