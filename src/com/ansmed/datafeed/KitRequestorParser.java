package com.ansmed.datafeed;

//JDK 1.6.0
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.lang.Integer;
import java.lang.StringBuilder;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;


// SMT Base Libraries
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.NumberFormat;
import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.StringUtil;

// SiteBuilder Libraries
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.constants.Constants;

// ANSMED Libraries
import com.ansmed.sb.physician.ClinicVO;
import com.ansmed.sb.physician.SurgeonVO;
import com.ansmed.datafeed.LabelMaker;


//Log4J 1.2.8
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/****************************************************************************
 * <b>Title</b>: KitRequestorParser.java<p/>
 * <b>Description: </b> Processes the data feed for Kit Requestor reports. 
 * <p/>
 * <b>Copyright:</b> (c) 2008<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 2.0
 * @since Mar. 06, 2008
 ****************************************************************************/
public class KitRequestorParser {

	private static Logger log = Logger.getLogger(KitRequestorParser.class);
	private Connection conn = null;
	private Properties parserConfig = new Properties();
	private InputStream inStream = null;
	private String configPath = "scripts/ans_config.properties";
	private Map<String,Object> config = new HashMap<String,Object>();
	
	private String dbDriver;
	private String dbUrl;
	private String dbUser;
	private String dbPassword;
	private String sbSchema;
	private String sbANSSchema;
	private String orgId;
	private String moduleType;
	private String actionName;
	private String strSearchRadius;
	private Integer integerSearchRadius;
	private String strSurgeonLimit;
	private Integer integerSurgeonLimit;
	private String strTemplatePath;
	private String strXMLWrapperFileName;
	private String strXMLBodyFileName;
	private String strPatientFileName;
	private String strInvalidFileName;
	private int invalidProfileCount = 0;
	private Calendar cal;
	private String dateStart;
	private String dateEnd;
	private StringBuilder sbXMLWrapper = new StringBuilder();
	private StringBuilder sbXMLBody = new StringBuilder();
	private StringBuilder templateDate = new StringBuilder();
	private StringBuilder xmlEmptyCell = new StringBuilder("<w:tc>\n<w:p>\n</w:p>\n</w:tc>\n");
	private StringBuilder xmlEmptyRow = new StringBuilder("<w:tr>\n<w:tc>\n<w:p>\n</w:p>\n</w:tc>\n<w:tc>\n<w:p>\n</w:p>\n</w:tc>\n</w:tr>\n");
	private StringBuilder xmlLineBreak = new StringBuilder("<w:br w:type=\"text-wrapping\"/>");
		
	/* **************************************************************** */
	
	public KitRequestorParser() {
		// If default constructor is called, use today's date.
		this(new Date(GregorianCalendar.getInstance().getTimeInMillis()),new Date(GregorianCalendar.getInstance().getTimeInMillis()));
		
	}
	
	public KitRequestorParser(Date start, Date end) {
		
		PropertyConfigurator.configure("scripts/ans_log4j.properties");
		
		dateStart = start.toString() + " 00:00:00";  
		dateEnd = end.toString() + " 23:59:59";
		
		log.info("Processing date: " + dateStart + " - " + dateEnd);
		log.debug("Start date: " + dateStart);
		log.debug("End date: " + dateEnd);
		
		// Build/Format the template display date.
		cal = GregorianCalendar.getInstance();
		
		templateDate.append("<w:t>");
		templateDate.append(getCalendarMonthName(cal.get(Calendar.MONTH)));
		templateDate.append(" ");
		templateDate.append(cal.get(Calendar.DAY_OF_MONTH));
		templateDate.append(", ");
		templateDate.append(cal.get(Calendar.YEAR));
		templateDate.append("</w:t>\n");
		
		log.debug("Template #DATE# field date: " + templateDate.toString());
		
		// Load the config file.  Taken from com.depuy.datafeed.DataFeedUtil
		try {
			inStream = new FileInputStream(configPath);
			parserConfig.load(inStream);
			log.debug("Successfully loaded config file");
		} catch (FileNotFoundException e){
			log.error("Unable to find configuration file.");
			System.exit(-1);
		} catch (IOException ioe) {
			log.error("Unable to access configuration file.");
			System.exit(-1);
		}
		finally {
			if (inStream != null) {
				try {
					inStream.close();
				} catch (Exception e) {
					log.error("Could not close file input stream.");
				}
			}
		}
		
		// Get the db connection properties.
		dbDriver = parserConfig.getProperty("dbDriver");
		dbUrl = parserConfig.getProperty("dbUrl");
		dbUser = parserConfig.getProperty("dbUser");
		dbPassword = parserConfig.getProperty("dbPassword");
		sbSchema = parserConfig.getProperty("sbSchema");
		sbANSSchema = parserConfig.getProperty("sbANSSchema");
				
		// Get surgeon search properties.
		strSearchRadius = parserConfig.getProperty("searchRadius");
		integerSearchRadius = Convert.formatInteger(strSearchRadius);
		strSurgeonLimit = parserConfig.getProperty("surgeonLimit");
		integerSurgeonLimit = Convert.formatInteger(strSurgeonLimit);
		
		// Get document template properties.
		strTemplatePath = parserConfig.getProperty("templatePath");
		strXMLWrapperFileName = parserConfig.getProperty("XMLWrapperTemplate");
		strXMLBodyFileName = parserConfig.getProperty("XMLBodyTemplate");
		strPatientFileName = parserConfig.getProperty("patientFileName");
		strInvalidFileName = parserConfig.getProperty("invalidFileName");
		
		// Get other pertinent properties.
		orgId = parserConfig.getProperty("org_id");
		moduleType = parserConfig.getProperty("module_type");
		actionName = parserConfig.getProperty("action_name");

				
		// Get a database connection.
		DatabaseConnection dbc = new DatabaseConnection(dbDriver,dbUrl,dbUser,dbPassword);
		try {
			conn = dbc.getConnection();
			log.debug("Got a database connection.");
		} catch (Exception de) {
			log.error("Couldn't get a database connection. ",de);
			System.exit(-1);
		}
		
		// Load the XML wrapper template file
		try {
			FileReader fr = new FileReader(strTemplatePath + strXMLWrapperFileName);
			BufferedReader br = new BufferedReader(fr);
			String strIn = "";
			
			while((strIn = br.readLine()) != null) {
				sbXMLWrapper.append(strIn).append("\n");
			}
			
			log.debug("Loaded XML wrapper template file.");
			log.debug(sbXMLWrapper);
			
			fr.close();
			
		} catch (FileNotFoundException fe) {
			log.error("Can't find XML wrapper template file. Printing stack trace, exiting.", fe);
			System.exit(-1);
			
		} catch (IOException ioe) {
			log.error("Can't read XML wrapper template file. Printing stack trace, exiting.", ioe);
			System.exit(-1);	
		}
		
		// Load the XML body template file
		try {
			FileReader fr1 = new FileReader(strTemplatePath + strXMLBodyFileName);
			BufferedReader br1 = new BufferedReader(fr1);
			String strIn1 = "";
			
			while((strIn1 = br1.readLine()) != null) {
				sbXMLBody.append(strIn1).append("\n");
			}
			
			log.debug("Loaded template file.");
			log.debug(sbXMLBody);
		
			fr1.close();
			
		} catch (FileNotFoundException fe) {
			log.error("Can't find template file. Printing stack trace, exiting.", fe);
			System.exit(-1);
			
		} catch (IOException ioe) {
			log.error("Can't read template file. Printing stack trace, exiting.", ioe);
			System.exit(-1);
		}
		
		//config data for ProfileManagerFactory
		config.put(Constants.ENCRYPT_KEY, parserConfig.getProperty("encryptKey"));
		config.put(Constants.GEOCODE_URL, parserConfig.getProperty("geoUrl"));
		
	}
	
	/**
	 * Main method.
	 * @param args
	 */
	public static void main(String[] args) {
		String startOn = null;
		String endOn = null;
		Date rangeStart = null;
		Date rangeEnd = null;
		StringBuilder dateRange = new StringBuilder();
		Calendar reportDay = GregorianCalendar.getInstance();
		// Report day date is set to yesterday's date.
		reportDay.roll(Calendar.DATE,false);
		
		if (args != null && args.length > 0) {
			if (args[0] != null && args[0].length() > 0) {
				if ((args.length > 1) && args[1] != null && args[1].length() > 0) {
					// Use arg[0] and arg[1] as start and end dates respectively.
					startOn = args[0];
					endOn = args[1];
					dateRange.append(startOn);
					dateRange.append("-").append(endOn);
				} else {
					// Use arg[0] as the start/end date.
					startOn = args[0];
					dateRange.append(startOn);
					dateRange.append("-").append(startOn);
				}
			}
		}
		
		if (startOn != null) {
			if (endOn != null) {
				rangeStart = Date.valueOf(startOn);
				rangeEnd = Date.valueOf(endOn);
			} else {
				rangeStart = Date.valueOf(startOn);
				rangeEnd = Date.valueOf(startOn);
			}
		} else {
			// Sets the end date to yesterday.
			rangeEnd = new java.sql.Date(reportDay.getTimeInMillis());
			// Sets the start date to six days prior to yesterday.
			reportDay.add(Calendar.DAY_OF_MONTH, -6);
			rangeStart = new java.sql.Date(reportDay.getTimeInMillis());
			dateRange.append(rangeStart.toString());
			dateRange.append("-").append(rangeEnd.toString());
		}
		
		KitRequestorParser krp = new KitRequestorParser(rangeStart, rangeEnd);
		
		LabelMaker labels = new LabelMaker("ANSavery30Wrapper.xml","ANSavery30row.xml","ANSavery30section.xml");

		String actionId = null;
		List<String> listProfileIds;
		List<UserDataVO> listProfileVO;
		StringBuilder fileData = new StringBuilder();
		StringBuilder invalidFileData = new StringBuilder();
		
		// Get appropriate action_id
		actionId = krp.retrieveActionId();
		log.debug("Action_Id value: " + actionId);
		
		// Get profile Id
		listProfileIds = krp.retrieveProfileIds(actionId);
		log.debug("listProfileIds size: " + listProfileIds.size());
		
		// If we have profileId's then process data.  Otherwise don't.
		if((!listProfileIds.isEmpty()) && (listProfileIds.size() > 0)) {
			// Get profiles as UserDataVO objects.
			listProfileVO = krp.retrieveProfiles(listProfileIds);
			log.info("Processing " + listProfileVO.size() + " profiles.");
			
			// If we have profile VO's then process them.  Otherwise don't.
			int profileCount = 0;
			if ((!listProfileVO.isEmpty()) && (listProfileVO.size() > 0)) {
			
				Double latitude = 0.00;
				Double longitude = 0.00;
				UserDataVO tmpProfileVO = null;
				List<SurgeonVO> tmpListSurgeonVO = new ArrayList<SurgeonVO>();
				Iterator<UserDataVO> iterProfileVO = listProfileVO.iterator();

				// Iterate through the profile objects.
				while(iterProfileVO.hasNext()) {
					profileCount += 1;
					tmpProfileVO = iterProfileVO.next();
					log.debug("Processing #: " + profileCount + " - profileId: " + tmpProfileVO.getProfileId());
					
					// If the user has a valid address, process the user profile.
					if(tmpProfileVO.getLocation().isCompleteAddress() && krp.validateProfileZipCode(tmpProfileVO.getZipCode())) {
						
						log.debug("Matchcode: " + tmpProfileVO.getMatchCode());
						latitude = tmpProfileVO.getLatitude();
						longitude = tmpProfileVO.getLongitude();
						
						//Get certain number of surgeons located within certain radius of patient.
						int searchRadius = krp.integerSearchRadius.intValue();
						int clinicLimit = krp.integerSurgeonLimit.intValue();
						
						// Get clinics
						tmpListSurgeonVO = krp.querySurgeons(latitude,longitude,searchRadius,clinicLimit);
						
						if (tmpListSurgeonVO != null && !tmpListSurgeonVO.isEmpty()) {

							// Create the patient's file data.
							fileData.append(krp.buildPatient(tmpProfileVO,tmpListSurgeonVO,clinicLimit,labels));
							
							// Append the page break to all patient reports except for the last one.
							if(profileCount < listProfileVO.size()) {
								fileData.append("<w:p>\n<w:br w:type=\"page\"/>\n</w:p>\n");
							}
							
						} else { // No clinics were found within the max search radius for the user.
							invalidFileData.append(krp.addInvalidProfileData(tmpProfileVO,"clinic"));
						}
						
					} else { // The user doesn't have a valid address.
						invalidFileData.append(krp.addInvalidProfileData(tmpProfileVO,"address"));
					}
					
					log.debug("Finished processing profileId: #" + tmpProfileVO.getProfileId());
					
				}
				
				// Report file...
				String fileDataString = krp.sbXMLWrapper.toString();
				fileData = new StringBuilder(fileDataString.replace("#reports#",fileData.toString()));
			
				StringBuilder fileName = new StringBuilder(krp.strPatientFileName);
				fileName.append("_").append(dateRange).append(krp.parserConfig.getProperty("fileExt"));

				// Invalid file...
				StringBuilder invalidFileName = new StringBuilder(krp.strInvalidFileName);
				invalidFileName.append("_").append(dateRange).append(krp.parserConfig.getProperty("fileExt"));
				
				// Label file...
				StringBuilder labelData = labels.getLabelFile();
				StringBuilder labelFileName = new StringBuilder("PatientLabels");
				labelFileName.append("_").append(dateRange).append(krp.parserConfig.getProperty("fileExt"));
				
				StringBuilder zipFileName = new StringBuilder(krp.parserConfig.getProperty("ftpFileName"));
				zipFileName.append("_").append(dateRange).append(krp.parserConfig.getProperty("ftpExt"));
				krp.createZipFile(fileName,fileData,invalidFileName.toString(),invalidFileData,
						labelFileName, labelData, zipFileName.toString());
				
				/*// LOCAL FILE creation
				krp.createLocalFile(fileName.toString(), fileData, krp.parserConfig.getProperty("filePath"));
				krp.createLocalFile(invalidFileName.toString(), invalidFileData, krp.parserConfig.getProperty("filePath"));
				krp.createLocalFile(labelFileName.toString(), labelData, krp.parserConfig.getProperty("filePath"));
				*/
				
				log.info("Processed " + profileCount + " profiles.");
				log.info("Valid profiles: " + (profileCount - krp.getInvalidProfileCount()));
				log.info("Invalid profiles: " + krp.getInvalidProfileCount());
				
			} else {
				log.error("No profiles VO's could be obtained for valid profileId's.");
			}
		} else {
			log.info("No profileId's exist for the specified date range.");
		}
		log.info("Exiting KitRequestorParser.");
		
	}
	
	/**
	 * Retrieves the correct action_id from the database.
	 * @return
	 */
	protected String retrieveActionId() {
		
		StringBuilder strSql = new StringBuilder();
		strSql.append("select ACTION_ID from ");
		strSql.append(sbSchema + "SB_ACTION ");
		strSql.append("where ORGANIZATION_ID = ? ");
		strSql.append("and MODULE_TYPE_ID = ? ");
		strSql.append("and ACTION_NM = ? ");
		
		log.debug("Method: retrieveActionId: SQL query: " + strSql.toString());

		String actionId = null;
		ResultSet rsActionId = null;
		
		try {
			PreparedStatement pStmt = conn.prepareStatement(strSql.toString());
			pStmt.setString(1,orgId);
			pStmt.setString(2,moduleType);
			pStmt.setString(3,actionName);
			
			rsActionId = pStmt.executeQuery();
			while (rsActionId.next()) {
				actionId = rsActionId.getString("ACTION_ID");
			}
			
		} catch (SQLException se){
			log.error("SQLException: method: retrieveProfileIds: ", se);
		}
		
		return actionId;
	}
	
	/**
	 * Retrieves patient profile ID's from the database.
	 * @param action
	 * @return
	 */
	protected List<String> retrieveProfileIds(String action) {
		
		StringBuilder strSql = new StringBuilder();
		strSql.append("select distinct PROFILE_ID from ");
		strSql.append(sbSchema);
		strSql.append("CONTACT_SUBMITTAL where ACTION_ID = ? and CREATE_DT >= ?");
		strSql.append(" and CREATE_DT <= ?");
		
		log.debug("Method: retrieveProfileIds: SQL query: " + strSql.toString());

		List<String> lProfileIds = new ArrayList<String>();
		ResultSet rsProfileIds = null;
		
		try {
			PreparedStatement pStmt = conn.prepareStatement(strSql.toString());
			pStmt.setString(1,action);
			pStmt.setString(2,dateStart);
			pStmt.setString(3,dateEnd);
			rsProfileIds = pStmt.executeQuery();
			while (rsProfileIds.next()) {
				lProfileIds.add(rsProfileIds.getString("PROFILE_ID"));
			}
			
		} catch (SQLException se){
			log.error("SQLException: method: retrieveProfileIds: ", se);
		}
		
		return lProfileIds;
	}

	/**
	 * Retrieves profile information for a given list of profile IDs.
	 * @param tmpListProfileIds
	 * @return
	 */
	protected List<UserDataVO> retrieveProfiles(List<String> tmpListProfileIds) {
		
		List<UserDataVO> listUserDataVO = new ArrayList<UserDataVO>();

		ProfileManager pm = ProfileManagerFactory.getInstance(config);
		try {
			listUserDataVO = pm.searchProfile(conn, tmpListProfileIds);
		} catch (DatabaseException de){
			log.error("DatabaseException - retrieveProfiles.");
		}
		
		log.debug("Method: retrieveProfiles: size of listUserDataVO list: " + listUserDataVO.size());
		
		return listUserDataVO;
	}
	
	/**
	 * Performs a spatial search to retrieve list of surgeon value objects within
	 * a given radius.
	 * @param latitude
	 * @param longitude
	 * @param radius
	 * @return
	 */
	protected List<SurgeonVO> querySurgeons(Double latitude, Double longitude, int radius, int limit) {

		List<SurgeonVO> data = new ArrayList<SurgeonVO>();
		Double lat = latitude;
		Double lng = longitude;

		String customDbSchema = sbANSSchema;
		StringBuilder sql = new StringBuilder();
		sql.append("select a.surgeon_id, title_nm, first_nm, middle_nm, last_nm, ");
		sql.append("suffix_nm, a.website_url, clinic_nm, address_txt, address2_txt, ");
		sql.append("city_nm, state_cd, zip_cd, latitude_no, longitude_no, area_cd, ");
		sql.append("exchange_no, line_no, b.clinic_id ");
		sql.append(",round((sqrt(power(").append(lat).append("-latitude_no,2) + power(");
		sql.append(lng).append(" - longitude_no,2)) /3.14159265)*180,1) as distance ");
		sql.append("from ").append(customDbSchema).append("ans_surgeon a ");
		sql.append("inner join ").append(customDbSchema).append("ans_clinic b ");
		sql.append("on a.surgeon_id = b.surgeon_id ");
		sql.append("left outer join ").append(customDbSchema).append("ans_phone c ");
		sql.append("on b.clinic_id = c.clinic_id and c.phone_type_id = 'WORK_PHONE' ");
				
		Integer distance = Convert.formatInteger(radius, 25);
		double radDegree = distance * .014;
		sql.append("where Latitude_no > ").append(NumberFormat.round((lat - radDegree),8)).append(" and ");
		sql.append("Latitude_no < ").append(NumberFormat.round((lat + radDegree),8)).append(" and ");
		sql.append("Longitude_no > ").append(NumberFormat.round((lng - radDegree),8)).append(" and ");
		sql.append("Longitude_no < ").append(NumberFormat.round((lng + radDegree),8));
		sql.append(" and status_id = 1 and surgeon_type_id = 0 and locator_display_flg = 1 ");
		sql.append("order by distance");
		
		log.debug("Locator search sql: " + sql.toString());
		
		PreparedStatement ps = null;
				
		try {
			ps = conn.prepareStatement(sql.toString());
			ResultSet rs = ps.executeQuery();
			for(int i = 1; (i <= limit && rs.next()); i++) {
				SurgeonVO vo = new SurgeonVO(rs);
				data.add(vo);
			}

		} catch (SQLException e) {
			log.error("Error retrieving surgeons value objects.",e);
		}
		
		// Return the locations
		return data;
	}
	
	/**
	 * Takes profile and merges clinics associated with profile.
	 * @param udv
	 * @param lSurgeonVO
	 * @param limit
	 * @return
	 */
	protected String buildPatient(UserDataVO udv, List<SurgeonVO> lSurgeonVO, int limit, LabelMaker labelMaker) {
		
		Iterator<SurgeonVO> iterSurgeonVO = lSurgeonVO.iterator();
		List<StringBuilder> patientClinics = new ArrayList<StringBuilder>();
		SurgeonVO sv = null;
		ClinicVO cv = null;
		PhoneVO phv = null;
		
		StringBuilder patientGreeting = new StringBuilder();
		StringBuilder patientName = new StringBuilder();
		StringBuilder patientStreetAddr = new StringBuilder();
		StringBuilder patientCityStateZip = new StringBuilder();
		StringBuilder patientAddr = new StringBuilder();
		StringBuilder clinicBody = new StringBuilder();
		
		String patientBody = new String(sbXMLBody.toString());
		
		// Build the patient's first name.
		patientGreeting.append("<w:t>Dear ");
		patientGreeting.append(replaceXML(StringUtil.capitalizePhrase(udv.getFirstName().trim())));
		patientGreeting.append(",</w:t>\n");
		
		// Build the patient's address.
		patientName.append("<w:t>").append(replaceXML(StringUtil.capitalizePhrase(udv.getFirstName().trim())));
		patientName.append(" ").append(replaceXML(StringUtil.capitalizePhrase(udv.getLastName().trim())));
		patientName.append("</w:t>\n");
		
		StringBuilder labelName = new StringBuilder(patientName);
		
		patientName.append(xmlLineBreak).append("\n");
		
		StringBuilder labelAddress = new StringBuilder();
		
		patientStreetAddr.append("<w:t>");
		patientStreetAddr.append(replaceXML(StringUtil.capitalizeAddress(udv.getAddress().trim())));
		labelAddress.append(patientStreetAddr);
				
		if ((udv.getAddress2() != null) && (udv.getAddress2().length() > 0)) {
			patientStreetAddr.append(", " + replaceXML(StringUtil.capitalizeAddress(udv.getAddress2().trim())));
			labelAddress.append(", " + replaceXML(StringUtil.capitalizeAddress(udv.getAddress2().trim())));
			patientStreetAddr.append("</w:t>\n");
			labelAddress.append("</w:t>\n");
			patientStreetAddr.append(xmlLineBreak).append("\n");
			
		} else {
			patientStreetAddr.append("</w:t>\n");
			labelAddress.append("</w:t>\n");
			patientStreetAddr.append(xmlLineBreak).append("\n");
		}
		
		patientCityStateZip.append("<w:t>");
		patientCityStateZip.append(replaceXML(StringUtil.capitalizePhrase(udv.getCity().trim()))).append(", ");		
		patientCityStateZip.append(udv.getState()).append("  ");			
		patientCityStateZip.append(udv.getZipCode());
		patientCityStateZip.append("</w:t>\n");
		StringBuilder labelCityStateZip = new StringBuilder(patientCityStateZip);
		patientCityStateZip.append(xmlLineBreak).append("\n");
		
		patientAddr.append(patientName).append(patientStreetAddr).append(patientCityStateZip);

		labelMaker.addLabel(labelName, labelAddress, labelCityStateZip);
		
		// Iterate SurgeonVO list to get Clinic.  If profile data is invalid, skip.
		int counter = 1;
		while(iterSurgeonVO.hasNext() && (counter <= limit)) {
			
			sv = iterSurgeonVO.next();
			cv = sv.getClinic();
			
			//iterate Clinic and get first valid phone number
			Iterator<PhoneVO> iterClinic = cv.getPhones().iterator();
			StringBuilder clinicPhone = new StringBuilder();
			int phoneCount = 0;
			while(iterClinic.hasNext() && phoneCount < 1) {
				
				phv = iterClinic.next();
				
				if (phv.isValidPhone()) {
					phv.setFormatType(PhoneNumberFormat.DOT_FORMATTING);
					clinicPhone.append(phv.getFormattedNumber());
					phoneCount++;
					
				}
			}
			
			// Retrieve patient clinics and add to list.
			patientClinics.add(buildClinics(cv,sv,clinicPhone));
			
			counter++;
			
		}
		
		log.debug(patientGreeting.toString());
		log.debug(patientAddr.toString());
		log.debug(clinicBody.toString());
		
		// Format the table rows for patient clinics list
		clinicBody = buildRows(patientClinics,limit);
		
		patientBody = patientBody.replace("#date#", templateDate.toString());
		patientBody = patientBody.replace("#address#", patientAddr.toString());
		patientBody = patientBody.replace("#patient#", patientGreeting.toString());
		patientBody = patientBody.replace("#clinics#", clinicBody.toString());
			
		return patientBody;
		
	}
	
	/**
	 * Formats info about each clinic as an HTML table and appends to body.
	 * @param cvo
	 * @param svo
	 * @param cPhone
	 * @return
	 */
	protected StringBuilder buildClinics(ClinicVO cvo, SurgeonVO svo, StringBuilder cPhone) {
		
		StringBuilder body = new StringBuilder();
		boolean hasClinicName = false;
		
		if ((cvo.getClinicName() != null) && (cvo.getClinicName().trim().length() > 0)) {
			hasClinicName = true;
		} else {
			hasClinicName = false;
		}
		
		// Start the table cell
		//body.append("<w:tc>\n<w:p>\n<w:r>\n");
		body.append("<w:tc>\n<w:p>\n<w:pPr>\n<w:pStyle w:val=\"tableBody\"/>\n</w:pPr>\n<w:r>\n");
		
		if (hasClinicName) {
			body.append("<w:t>").append(replaceXML((cvo.getClinicName().trim())));
			body.append("</w:t>\n");
			body.append(xmlLineBreak).append("\n");
		}
		
		body.append("<w:t>").append(replaceXML(svo.getFirstName().trim())).append(" ");
		body.append(replaceXML(svo.getLastName().trim()));
		if ((svo.getTitle() != null) && (svo.getTitle().trim().length() > 0)) {
			body.append(", ").append(replaceXML(svo.getTitle().trim())).append("</w:t>\n");
			body.append(xmlLineBreak).append("\n");
		} else {
			body.append("</w:t>\n").append(xmlLineBreak).append("\n");
		}
		
		body.append("<w:t>").append(replaceXML(cvo.getAddress().trim()));
		if ((cvo.getAddress2() != null) && (cvo.getAddress2().trim().length() > 0)) {
			body.append(", ").append(replaceXML(cvo.getAddress2().trim()));
		}
		if ((cvo.getAddress3() != null) && (cvo.getAddress3().trim().length() > 0)) {
			body.append(", ").append(replaceXML(cvo.getAddress3().trim()));
		}
		body.append("</w:t>\n").append(xmlLineBreak).append("\n");
		
		body.append("<w:t>");
		if ((cvo.getCity() != null) && cvo.getCity().length() > 0) {
			body.append(replaceXML(cvo.getCity().trim())).append(", ");
		} else {
			body.append("No city, ");
		}
		
		if ((cvo.getState() != null) && cvo.getState().length() > 0) {
			body.append(cvo.getState()).append(" ");
		} else {
			body.append("No state ");
		}
		
		if ((cvo.getZipCode() != null) && cvo.getZipCode().length() > 0) {
			body.append(cvo.getZipCode()).append(" ").append("</w:t>\n");
		} else {
			body.append("No zipcode.").append("</w:t>\n");
		}
		body.append(xmlLineBreak).append("\n");
		
		// Valid phone number check.
		if (cPhone != null && cPhone.length() > 0) {
			body.append("<w:t>").append(cPhone).append("</w:t>\n");
		} else {
			body.append("<w:t>No phone number listed.</w:t>\n");
		}
		body.append(xmlLineBreak).append("\n");
		
		body.append("<w:t>").append(svo.getDistance()).append(" miles.</w:t>\n");
		
		// If there wasn't a clinic name, then end the inner table with blank.
		if (!hasClinicName) {
			body.append("<w:t></w:t>\n");
		}

		// Close the table cell
		body.append("</w:r>\n</w:p>\n</w:tc>\n");
		
		return body;
	}
	
	/**
	 * Builds the XML table rows for patient clinic list.
	 * @param clinics
	 * @param max
	 * @return
	 */
	protected StringBuilder buildRows(List<StringBuilder> clinics, int max) {

		int size = clinics.size();
		int rowLimit = max/2;
		int numRows = 0;
		int nextCell = 0;
		
		
		if (size < rowLimit) {
			numRows = size;
		} else {
			numRows = rowLimit;
		}
		
		log.debug("clinic size = " + size);
		log.debug("numRows = " + numRows);

		StringBuilder rows = new StringBuilder();
		
		if(size > 0) {
			
			for (int i = 1; i <= numRows; i++) {
				
				rows.append("<w:tr>\n").append(clinics.get(i - 1));
				
				nextCell = i + rowLimit;
				log.debug("nextCell = " + nextCell);
				if (nextCell <= size) {
					rows.append(clinics.get(nextCell - 1));
				} else {
					rows.append(xmlEmptyCell).append("\n");
				}
				
				rows.append("</w:tr>\n");
				
				if (i < rowLimit && rowLimit != 1) {
					rows.append(xmlEmptyRow).append("\n");
				}
				
			}
			
		}
		
		return rows;
	}

	/**
	 * Returns month name based on Calendar month int passed as param.
	 * @param month
	 * @return
	 */
	protected String getCalendarMonthName(int month) {
		
		String monthName = "";
		
		switch (month) {
        case 0:  monthName = "January"; break;
        case 1:  monthName = "February"; break;
        case 2:  monthName = "March"; break;
        case 3:  monthName = "April"; break;
        case 4:  monthName = "May"; break;
        case 5:  monthName = "June"; break;
        case 6:  monthName = "July"; break;
        case 7:  monthName = "August"; break;
        case 8:  monthName = "September"; break;
        case 9:  monthName = "October"; break;
        case 10: monthName = "November"; break;
        case 11: monthName = "December"; break;
        default: monthName = "Invalid month."; break; 
		}
				
		return monthName;
	}

	/**
	 * Adds profile to invalid profile file data.
	 * @param profile
	 * @param msg
	 */
	protected StringBuilder addInvalidProfileDataXML(UserDataVO profile, String msg) {

		StringBuilder buf = new StringBuilder();
		
		setInvalidProfileCount();
		buf.append("<w:t>");
		buf.append(profile.getFirstName()).append(" ").append(profile.getLastName()).append(" : ");
		buf.append(profile.getCity()).append(",").append(profile.getState());
		buf.append(",").append(profile.getZipCode()).append(" : ");
		buf.append("profileId: " + profile.getProfileId()).append(" : ");
		if(msg.equals("address")) {
			buf.append("Invalid address information found for patient.\n\n");
		} else if (msg.equals("clinic")) {
			buf.append("No clinics found within ").append(this.integerSearchRadius.intValue());
			buf.append(" miles of this patient.\n\n");
		}
		buf.append("</w:t>\n");
		buf.append(xmlLineBreak);
		log.debug(msg + " - profileId: " + profile.getProfileId());

		return buf;
	}
	
	protected StringBuilder addInvalidProfileData(UserDataVO profile, String msg) {

		StringBuilder buf = new StringBuilder();
		
		setInvalidProfileCount();
		buf.append(profile.getFirstName()).append(" ").append(profile.getLastName()).append(" : ");
		buf.append(profile.getCity()).append(",").append(profile.getState());
		buf.append(",").append(profile.getZipCode()).append(" : ");
		buf.append("profileId: " + profile.getProfileId()).append(" : ");
		if(msg.equals("address")) {
			buf.append("Invalid address information found for patient.\n\n");
		} else if (msg.equals("clinic")) {
			buf.append("No clinics found within ").append(this.integerSearchRadius.intValue());
			buf.append(" miles of this patient.\n\n");
		}

		log.debug(msg + " - profileId: " + profile.getProfileId());

		return buf;
	}
	
	/**
	 * 
	 * @param dataFileName
	 * @param fileData
	 * @param invalidFileName
	 * @param invalidFileData
	 * @param ftpFileName
	 */
	protected void createZipFile(StringBuilder dataFileName, StringBuilder fileData, 
			String invalidFileName,	StringBuilder invalidFileData, 
			StringBuilder labelFileName, StringBuilder labelFileData, String ftpFileName) {
		
		String host = parserConfig.getProperty("ftpHost");
		int port = Convert.formatInteger(parserConfig.getProperty("ftpPort")).intValue();
		String user = parserConfig.getProperty("ftpUser");
		String pwd = parserConfig.getProperty("ftpPassword");

		ZipStreamWriter zStream = new ZipStreamWriter();
		// Create the zip stream in the ZipStreamWriter.
		try {
			zStream.createZipStream();
			log.debug("Created zip stream.");
		} catch (IOException ioe) {
			log.error("Could not create zip stream.", ioe);
		}
		
		// Add the filename and file data to the zip stream.
		try {
			zStream.addEntry(dataFileName.toString(), fileData.toString().getBytes());
			log.debug("Adding patient file zip entry to zip stream.");
		} catch (IOException ioe) {
			log.error("Cannot add file entry to zip stream.", ioe);
		}
		
		if((invalidFileData != null) && (invalidFileData.length() > 0)) {
			try {
				zStream.addEntry(invalidFileName, invalidFileData.toString().getBytes());
				log.debug("Adding 'invalidFile' zip entry to zip stream.");
			} catch (IOException ioe) {
				log.error("Cannot add 'invalidFile' entry to zip stream.", ioe);
			}
		}
		
		try {
			zStream.addEntry(labelFileName.toString(), labelFileData.toString().getBytes());
			log.debug("Adding label file zip entry to zip stream.");
		} catch (IOException ioe) {
			log.error("Cannot add 'label' file entry to zip stream.", ioe);
		}
		
		try {
			zStream.close();
			zStream.ftpFile(host, port, user, pwd, ftpFileName);
			log.debug("Successfully FTP'd zipped file to host.");
		} catch (IOException ioe) {
			log.error("Could not FTP file to host.", ioe);
		}
	}
	
	/**
	 * Checks length of zipcode to filter out international addresses.
	 * @param zcode
	 * @return
	 */
	protected boolean validateProfileZipCode(String zcode) {
		
		int zLength = 0;
		if (zcode != null && zcode.length() > 0) {

			zLength = zcode.trim().length();

			// Valid US zipcodes are either 5 numbers or 5 + '-' + 4 in length.
			if ((zLength > 5 && zLength < 10) || zLength > 10) {
				return false;
			} else {
				return true;
			}
		} else {
			return false;
		}
		
	}
	
	/**
	 * Writes a file to the file system given the specified path.
	 * @param fileName
	 * @param fileData
	 * @param path
	 */
	protected void createLocalFile(String fileName, StringBuilder fileData, String path) {
		
		FileOutputStream fos = null;
				
		try {
			fos = new FileOutputStream(path + fileName);
			fos.write(fileData.toString().getBytes());
			fos.close();
			
		} catch(FileNotFoundException fnfe) {
			log.error("File is in use or was not found.", fnfe);
		} catch(IOException ioe) {
			log.error("IOException attempting to write file.", ioe);
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (Exception e) {
					log.error("Could not close file output stream.", e);
				}
			}
		}
	}
		
	
	/**
	 * Increments invalid profile counter.
	 */
	protected void setInvalidProfileCount(){
		invalidProfileCount++;
	}
	
	/**
	 * Returns invalid profile count.
	 * @return
	 */
	protected int getInvalidProfileCount(){
		return invalidProfileCount;
	}
	
	/**
	 * replaces XML reserved characters with their HTML-equiv values.
	 * @param val
	 * @return
	 */
	protected String replaceXML(String val) {
		val = StringUtil.checkVal(val);
		val = StringUtil.replace(val, "&", "&amp;");
		val = StringUtil.replace(val, "'", "&apos;");
		val = StringUtil.replace(val, "’", "&apos;");
		val = StringUtil.replace(val, "´", "&apos;");
		val = StringUtil.replace(val, "\"", "&quot;");
		val = StringUtil.replace(val, ">", "&gt;");
		val = StringUtil.replace(val, "<", "&lt;");
		val = StringUtil.replace(val, "ï", "&#239;");
		val = StringUtil.replace(val, "Ï", "&#207;");
		val = StringUtil.replace(val, "¿", "&#191;");
		val = StringUtil.replace(val, "¼", "&#188;");
		val = StringUtil.replace(val, "½", "&#189;");
		val = StringUtil.replace(val, "ã", "&#227;");
		val = StringUtil.replace(val, "©", "&#169;");
		val = StringUtil.replace(val, "£", "&#163;");
		val = StringUtil.replace(val, "‰", "");
		val = StringUtil.replace(val, "¡", "");
		val = StringUtil.replace(val, "¨","");
		val = StringUtil.replace(val, "º", "&#176;");
		
		return val;
	}
				
}
