package com.universal.util;

// JDK 7
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Apache Log4j
import org.apache.log4j.PropertyConfigurator;

// SMTBaseLibs
import com.siliconmtn.commerce.ShoppingCartItemVO;
import com.siliconmtn.commerce.ShoppingCartVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.io.ftp.SFTPClient;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.io.mail.MailHandlerFactory;
import com.siliconmtn.io.mail.mta.MailTransportAgentIntfc;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WebCrescendo 2.0 libs
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: AbandonedCartExporter.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Script that exports abandoned cart data for transmission
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Aug 28, 2014<p/>
 * <b>Changes: </b>
 * <b>Aug 28, 2014; David Bargerhuff: Created class 
 ****************************************************************************/
public class AbandonedCartExporter extends CommandLineUtil {

	private static final String DELIM_FIELD = "|";
	private static final String DELIM_VALUE = ";";
	private static final Integer DEFAULT_SITES_LIMIT = Integer.valueOf(10);
	private static final char TERMINATOR = 0xa;
	private static final String NEWLINE = Character.toString(TERMINATOR);
	private Date dateStart = null;
	private Date dateEnd = null;
	private Map<String, UserDataVO> users = null;
	private List<AbandonedCartVO> carts = null;
	private List<String> messageLog = null;
	private Map<String,String> brandMap = null;
	private String propertiesPath = "C:/Users/beaker/gitHome/git/WC_Custom/scripts/usa_abandoned_carts.properties"; 
	private String logPropertiesPath = "C:/Users/beaker/gitHome/git/WC_Custom/scripts/usa_abandoned_carts_log4j.properties";

	public AbandonedCartExporter(String[] args) {
		super(args);
		messageLog = new ArrayList<>();
		brandMap = new LinkedHashMap<>();
		PropertyConfigurator.configure(logPropertiesPath);
	}

	public static void main(String[] args) {
		AbandonedCartExporter ace = new AbandonedCartExporter(args);
		ace.run();
		if (ace.dbConn != null) {
			DBUtil.close(ace.dbConn);
		}
	}

	@Override
	public void run() {

		// load properties
		this.loadProperties(propertiesPath);
		if (props == null) {
			// notify admin FAIL/Properties
			addMessage("Failed to load properties file. Check log!");
			sendEmail(1);
			return;
		}

		// load dbConn
		this.loadDBConnection(props);
		if (dbConn == null) {
			// notify admin FAIL/Database
			addMessage("Failed to establish a database connection. Check log!");
			sendEmail(1);
			return;
		}

		// loads the brand map from the properties file.
		loadBrandMap();

		// format the start/end dates for this run
		formatDates();

		// process abandoned carts
		processCarts();

	}

	/**
	 * Formats the start/end date for this process.
	 */
	private void formatDates() {
		log.info("formatting dates...");
		// Use 'yesterday' for start/end date range
		Calendar cal = Calendar.getInstance();
		dateStart = Convert.formatStartDate(cal.getTime());
		dateEnd = Convert.formatEndDate(cal.getTime());
		log.info("Start/End dates used: " + dateStart + "-" + dateEnd);
	}

	/**
	 * Controlling method that utilizes other methods to load carts, load profiles, 
	 * build reports, and send admin email.
	 */
	protected void processCarts() {
		log.info("processing carts...");
		long baseTime = Calendar.getInstance().getTimeInMillis();
		StringBuilder statusMsg = null;
		int failCnt = 0;
		for (Map.Entry<String, String> entry : brandMap.entrySet()) {
			statusMsg = new StringBuilder();
			try {
				//1. retrieve carts
				loadCarts(entry.getKey());

				// 2. retrieve profiles assoc with carts
				loadProfiles();

				// 3. build export
				StringBuilder fileName = new StringBuilder();
				fileName.append(entry.getValue()).append("_").append(baseTime);
				fileName.append(props.getProperty("reportFileExtension"));
				buildExportFile(fileName.toString());

			} catch (SQLException sqle) {
				statusMsg.append("Error loading cart information, ");
				statusMsg.append(sqle);
				failCnt++;
			} catch (DatabaseException de) {
				statusMsg.append("Error retrieving profile information, " + de);
				statusMsg.append(de);
				failCnt++;
			} catch (FileNotFoundException fnfe) {
				statusMsg.append("File not found attempting to write report, " + fnfe);
				statusMsg.append(fnfe);
				failCnt++;
			} catch (IOException ioe) {
				statusMsg.append("Error writing report data stream, " + ioe);
				statusMsg.append(ioe);
				failCnt++;
			}

			if (statusMsg.length() == 0) {
				statusMsg.append(entry.getKey()).append(": ");
				if (carts.isEmpty()) {
					statusMsg.append("No abandoned carts found for this source.");
				} else {
					statusMsg.append("Successfully built abandoned cart report for this source, ");
					statusMsg.append(carts.size()).append(" abandoned cart(s).");
				}
			}
			addMessage(statusMsg.toString());
		}
		log.info(messageLog);

		// 4. send admin email
		sendEmail(failCnt);

	}

	/**
	 * Retrieves persisted (serialized) cart objects from the Object table based on 
	 * source ID and non-null profile IDs.
	 * @return
	 * @throws SQLException
	 */
	private void loadCarts(String sourceId) throws SQLException {
		log.info("Retrieving abandoned carts...");
		// reset carts * profiles
		carts = new ArrayList<>();

		// build query
		StringBuilder sql = new StringBuilder(250);
		sql.append("select * from OBJECT_STOR where 1=1 ");
		sql.append("and PROFILE_ID is not null and SOURCE_ID = ? ");
		sql.append("and UPDATE_DT between ? and ? ");
		sql.append("order by SOURCE_ID, PROFILE_ID, UPDATE_DT desc");
		log.info("Using cart query SQL: " + sql);
		log.info("sourceId|start|end: " + sourceId + "|" + 
				Convert.formatSQLDate(dateStart) + "|" + Convert.formatSQLDate(dateEnd));

		// execute query
		int index = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(++index, sourceId);
			ps.setDate(++index,  Convert.formatSQLDate(dateStart));
			ps.setDate(++index,  Convert.formatSQLDate(dateEnd));
			ResultSet rs = ps.executeQuery();

			// parse results
			ObjectInputStream ois = null;
			while (rs.next()) {
				AbandonedCartVO acv = new AbandonedCartVO();
				ShoppingCartVO cart = null;
				try {
					ois = new ObjectInputStream(new ByteArrayInputStream(rs.getBytes("object")));
					cart = (ShoppingCartVO) ois.readObject();
					acv.setCart(cart);
					acv.setObjectId(rs.getString("object_id"));
					acv.setProfileId(rs.getString("profile_id"));

					UserDataVO userVo = new UserDataVO();
					userVo.setProfileId(acv.getProfileId());
					users.put(userVo.getProfileId(), userVo);
					
					acv.setSourceId(rs.getString("source_id"));
					acv.setCreateDate(rs.getTimestamp("update_dt"));
					carts.add(acv);

				} catch (IOException ioe) {
					addMessage("Failed to read cart for object ID: " + rs.getString("object_id"));
				} catch (ClassNotFoundException cnfe) {
					addMessage("Class not found, " + cnfe);
				}
			}
		}
		log.info("Abandoned carts found: " + carts.size());
	}

	/**
	 * Retrieves user profiles based on a List of user profile IDs and loads the 
	 * profiles into a Map.
	 * @throws DatabaseException 
	 */
	private void loadProfiles() throws DatabaseException {
		if (carts.isEmpty()) return;
		Map<String,Object> config = new HashMap<>();
		config.put(Constants.ENCRYPT_KEY, props.getProperty("encryptKey"));
		ProfileManager pm = ProfileManagerFactory.getInstance(config);
		pm.populateRecords(dbConn, new ArrayList<>(users.values()));
		log.info("User profiles loaded: " + users.size());
	}

	/**
	 * Builds the export file for abandoned carts.
	 * @param baseTime
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void buildExportFile(String reportFileName) throws IOException {
		if (carts.isEmpty()) return;
		StringBuilder outFile = new StringBuilder();
		outFile.append(buildFileHeader());
		String email = null;
		for (AbandonedCartVO cart : carts) {
			email = lookupEmail(cart.getProfileId());
			if (email.length() == 0) continue;
			outFile.append(cart.getObjectId()).append(DELIM_FIELD); // cart object ID used as sessionid
			outFile.append(DELIM_FIELD); // placeholder for Universal_uid which is not impl yet.
			outFile.append(addProductsFromCart(cart.getCart())).append(DELIM_FIELD); // SKUS, semi-colon delimited
			outFile.append(email).append(DELIM_FIELD); // user's email address
			//outFile.append(Convert.formatDate(cart.getCreateDate(), Convert.DATE_TIME_SLASH_PATTERN)); // cart update date
			outFile.append(Convert.formatDate(cart.getCreateDate(), Convert.DATE_TIME_DASH_PATTERN)); // cart update date
			outFile.append(NEWLINE);
		}

		// write local file
		boolean doFlag = Convert.formatBoolean(props.getProperty("copyFilesLocally"));
		log.info("local copy enabled: " + doFlag);
		if (doFlag) {
			// write the file
			File f = new File(props.getProperty("reportFilePath") + reportFileName);
			FileOutputStream fos = null;
			fos = new FileOutputStream(f);
			try {
				fos.write(outFile.toString().getBytes());
				fos.flush();
				fos.close();
			} finally {
				try {
					fos.close();
				} catch (Exception e) {log.error("Error closing FileOutputStream, ", e);}
			}
		}

		// ftp file
		doFlag = Convert.formatBoolean(props.getProperty("sftpEnabled"));
		log.info("SFTP enabled: " + doFlag);
		if (doFlag) {
			moveFile(outFile.toString().getBytes(), reportFileName);
		}

	}

	/**
	 * Builds the file header
	 * @return
	 */
	private StringBuilder buildFileHeader() {
		StringBuilder hdr = new StringBuilder(80);
		hdr.append("sessionid").append(DELIM_FIELD);
		hdr.append("Universal_uid").append(DELIM_FIELD);
		hdr.append("skus").append(DELIM_FIELD);
		hdr.append("email").append(DELIM_FIELD);
		hdr.append("entrydate");
		hdr.append(NEWLINE);
		return hdr;
	}

	/**
	 * Builds a String of comma-delimited product ID values.
	 * @param cart
	 * @return
	 */
	private StringBuilder addProductsFromCart(ShoppingCartVO cart) {
		log.info("adding product SKUs...");
		Map<String, ShoppingCartItemVO> items = cart.getItems();
		StringBuilder skus = new StringBuilder();
		int cnt = 0;
		for (Map.Entry<String, ShoppingCartItemVO> entry: items.entrySet()) {
			ShoppingCartItemVO item = entry.getValue();
			if (cnt > 0) skus.append(DELIM_VALUE);
			if (item.getProductId().indexOf('_') > -1) {
				skus.append(item.getProductId().substring(0,item.getProductId().indexOf('_')));
			} else {
				skus.append(item.getProductId());
			}
			cnt++;
		}
		return skus;
	}

	/**
	 * Retrieves a user's data from the profile map based on the profile ID passed in
	 * and gets the user's email address.
	 * @param profileId
	 * @return The user's email address or an empty String.
	 */
	private String lookupEmail(String profileId) {
		UserDataVO user = users.get(profileId);
		String email = null;
		if (user != null) {
			email = user.getEmailAddress();
		}
		return StringUtil.checkVal(email);
	}

	/**
	 * Sends email admin. 
	 * @param successCnt
	 * @param failCnt
	 */
	private void sendEmail(int failCnt) {
		log.info("sending email...");
		try {
			// Build the email message
			EmailMessageVO msg = new EmailMessageVO();
			msg.setFrom("scriptMaster@siliconmtn.com");
			msg.addRecipient(props.getProperty("adminEmail"));
			String result = failCnt == 0 ? "Success: " : "FAILED: ";
			msg.setSubject(result + "Universal Abandoned Cart Export");

			StringBuilder html= new StringBuilder();
			html.append("<p>");
			for (String logMsg : messageLog){
				html.append(logMsg).append("</br>");
			}
			html.append("</p>");

			msg.setHtmlBody(html.toString());
			MailTransportAgentIntfc mail = MailHandlerFactory.getDefaultMTA(props);
			mail.sendMessage(msg);

		} catch (Exception e) {
			log.error("Error sending admin email, ", e);
		}
	}

	/**
	 * Moves file to an SFTP host.
	 * @param data
	 * @param reportFileName
	 * @throws IOException
	 */
	private void moveFile(byte[] data, String reportFileName) throws IOException {
		String host = props.getProperty("sftpHost");
		log.info("SFTP'ing file to: " + host);
		String reportFullPath = props.getProperty("sftpReportFilePath") + reportFileName;

		// Connect to the SFTP Server
		SFTPClient s = new SFTPClient();
		int port = Convert.formatInteger(props.getProperty("sftpPort"));
		try {
			s.connect(host, port, props.getProperty("sftpUser"), props.getProperty("sftpPassword"));

			// Transfer the data
			s.writeData(data, reportFullPath);
			log.info("Successfully wrote file: " + reportFullPath);
			addMessage("Successfully SFTP'd " + reportFileName + " to " + host);
		} catch(IOException ioe) {
			addMessage("Failed to SFTP " + reportFileName + " to " + host);
			addMessage("Error cause: " + ioe.getMessage());
			log.error("Error SFTP'ing file: " + reportFullPath + ", ", ioe);
		} finally {
			// Close the connection
			s.disconnect();
			log.info("Disconnected from SFTP host.");
		}
	}

	/**
	 * Adds a message to the message log
	 * @param msg
	 */
	private void addMessage(String msg) {
		messageLog.add(msg);
	}

	/**
	 * Lookup table mapping site IDs to brand name.  Used in report name
	 * generation.
	 */
	private void loadBrandMap() {
		// retrieve the number of sites limit
		int sitesNo = Convert.formatInteger(props.getProperty("sourceSitesNumber"),DEFAULT_SITES_LIMIT);
		String keyPrefix = StringUtil.checkVal(props.getProperty("organizationPrefix"));
		// loop sites source properties to load brand map
		for (int i = 1; i <= sitesNo; i++) {
			if (props.getProperty(keyPrefix+i) != null) {
				brandMap.put(keyPrefix+i, props.getProperty(keyPrefix+i));
			} else {
				// we're done...bail
				break;
			}
		}

		if (log.isDebugEnabled()) {
			for (Map.Entry<String, String> entry: brandMap.entrySet())
				log.debug("brandMap key/val: " + entry.getKey() + "|" + entry.getValue());
		}
	}

}
