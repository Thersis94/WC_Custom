package com.ansmed.datafeed;

//JDK 1.6.0
import java.io.BufferedReader;
import java.io.FileInputStream;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

// SMT Base Libraries
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.NumberFormat;
import com.siliconmtn.util.SMTMail;
import com.siliconmtn.util.StringUtil;

// SiteBuilder Libraries
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.constants.Constants;

// ANSMED Libraries
import com.ansmed.sb.physician.SurgeonVO;

//Log4J 1.2.8
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/****************************************************************************
 * <b>Title</b>: KitRequestProcessor.java<p/>
 * <b>Description: </b> Processes the patient letters for patients who request
 * pain kits from ANS/SJM. 
 * <p/>
 * <b>Copyright:</b> (c) 2008<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Dec. 05, 2008
 * Change Log:
 * March 23, 2009 - Added processing of info kit requests from BRC Card form,
 * Contact Us form and Teleconference form.  Also added capture of info kit
 * video format (DVD/VHS).
 * July, October 2010 - Added processing of POYP Spanish info kit requests
 * November 2010 - Added support for Spanish language letters.  Refactored
 * code.
 * May/June 2011 - Added Spanish language letter template, refactored how send date is
 * formatted; refactored code to streamline processing.
 * Nov 2012 - Refactored to use WinZip command line for data encryption/compression per
 * SJM requirements.  Encryption is AES, 256-bit.
 ****************************************************************************/
public class KitRequestProcessor {

	private static Logger log = Logger.getLogger(KitRequestProcessor.class);
	private String configPath = "scripts/ans_config.properties";
	private Properties props = new Properties();
	private Connection conn = null;
	
	private String dbDriver;
	private String dbUrl;
	private String dbUser;
	private String dbPassword;
	private Integer searchRadius;
	private Integer surgeonLimit;
	private String strTemplatePath;
	private StringBuilder xmlEnglishBody;
	private StringBuilder xmlSpanishBody;
	private StringBuilder firstPageImageHeader;
	private StringBuilder subPageImageHeader;
	private String letterFileName;
	private String labelFileName;
	private String smtpServer;
	private Integer smtpPort;
	private String smtpUser;
	private String smtpPwd;
	private StringBuilder xmlWrapper;
	private StringBuilder xmlLabelWrapper;
	private String msg = null;
	private StringBuilder statusMsg = null;
	private boolean successStatus = false;
	
	// English labels and letters
	private ExcelFileBuilder eLabels;
	private StringBuilder eLetters;
	
	// Spanish labels and letters
	private ExcelFileBuilder sLabels;
	private StringBuilder sLetters;
	
	private Map<String, Object> config = new HashMap<String, Object>();
	
	//flags
	boolean sendLocal = false;
	boolean sendZip = true;
	
	// counters
	private int englishCount = 0;
	private int spanishCount = 0;
	private int profileCount = 0;
	private int invalidProfileCount = 0;
	
	/**
	 * constructor
	 */
	public KitRequestProcessor() {
		this(new Date(GregorianCalendar.getInstance().getTimeInMillis()));
	}
	
	/**
	 * constructor
	 * @param startDate
	 */
	public KitRequestProcessor(Date startDate) {
		this(startDate,startDate);
	}
	
	/**
	 * constructor
	 * @param startDate
	 * @param endDate
	 */
	public KitRequestProcessor(Date startDate, Date endDate) {
		PropertyConfigurator.configure("scripts/ans_log4j.properties");
		statusMsg = new StringBuilder();
		// load the config properties file or exit
		try {
			this.loadConfig();
		} catch (Exception e) {
			msg = "Could not load configuration file.  " + e.getClass();
			try {
				this.sendAdminEmail(false,msg);
			} catch (MailException me) {
				log.debug("Error sending admin email, ", me);
			}
			System.exit(-1);
		}
		// initialize member vars with config property values
		this.initializeProperties();
		// Load the letter XML template files ('wrapper' and 'body') or exit
		try {
			xmlWrapper = this.loadTemplate("xmlWrapper.xml");
			xmlLabelWrapper = this.loadTemplate("xmlLabelWrapper.xml");
			xmlEnglishBody = this.loadTemplate("xmlEnglishBody.xml");
			xmlSpanishBody = this.loadTemplate("xmlSpanishBody.xml");
			firstPageImageHeader = this.loadTemplate("xmlFirstPageImageHeader.xml");
			subPageImageHeader = this.loadTemplate("xmlSubPageImageHeader.xml");
			log.debug("Successfully loaded all XML templates.");
		} catch (Exception e) {
			msg = "Could not load one or more XML template files.  " + e.getClass();
			try {
				this.sendAdminEmail(false,msg);
			} catch (MailException me) {
				log.debug("Error sending admin email, ", me);
			}
			System.exit(-1);
		}
		
		// instantiate/initialize label/letter containers
		eLabels = new ExcelFileBuilder(new StringBuilder(xmlLabelWrapper));
		eLetters = new StringBuilder();
		sLabels = new ExcelFileBuilder(new StringBuilder(xmlLabelWrapper));
		sLetters = new StringBuilder();
		// get a database connection or exit
		try {
			this.getDBConnection();
			log.debug("Obtained a db connection.");
		} catch (Exception e) {
			msg = "Could not establish a database connection. " + e.getClass();
			try {
				this.sendAdminEmail(false,msg);
			} catch (MailException me) {
				log.debug("Error sending admin email, ", me);
			}
			System.exit(-1);
		}
		// set other required config paramters
		config.put(Constants.ENCRYPT_KEY, props.getProperty("encryptKey"));
		config.put(Constants.GEOCODE_URL, props.getProperty("geoUrl"));
	}
	
	/**
	 * Utility constructor for allowing logging when command-line dates fail with
	 * IllegalArgumentException
	 * @param msg
	 */
	public KitRequestProcessor(String msg) {
		PropertyConfigurator.configure("scripts/ans_log4j.properties");
		log.error(msg);
	}
	
	/**
	 * Main method.
	 * @param args
	 */
	public static void main(String[] args) {
		Date startDate = null;
		Date endDate = null;
		Date runDate = null;
		Calendar cal = GregorianCalendar.getInstance();
		runDate = new Date(cal.getTimeInMillis());
		// retrieve dates from command line args
		if (args != null && args.length > 0) {
			try {
				startDate = Date.valueOf(args[0]);
				if (args.length == 1) {
					endDate = (Date)startDate.clone();
				} else {
					endDate = Date.valueOf(args[1]);
					// make sure 'start' is earlier than 'end'
					if (endDate.before(startDate)) {
						Date tmp = (Date)endDate.clone();
						endDate = (Date)startDate.clone();
						startDate = (Date)tmp.clone();
						tmp = null;
					}
				}
			} catch (IllegalArgumentException iae) {
				String msg = "Error converting 'start' or 'end' date values, ";
				// instantiate to log the exception
				new KitRequestProcessor(msg + iae);
				// kill the krp instance and exit
				//krp = null;
				System.exit(-1);
			}
		} else {
			// If no date passed in , use yesterday's date as the processing date
			startDate = new Date(cal.getTimeInMillis() - (1000*60*60*24));
			endDate = (Date)startDate.clone();
		}
		
		// instantiate the main kit request processor
		KitRequestProcessor krp = new KitRequestProcessor(startDate,endDate);
		// Instantiate the profile retriever object
		KitRequestRetriever krr = new KitRequestRetriever(startDate, endDate);
		// retrieve contact profiles
		log.debug("retrieving requests");
		krr.setDBConnection(krp.conn);
		krr.loadKitRequestorProfiles();
		// process the profiles and build letters/labels
		krp.buildLetters(krr, runDate);
		
		// merge the letters with the appropriate wrappers
		krp.mergeLetterData(runDate);
		
		// retrieve the aggregate letters
		Map<String, StringBuilder> completeLetters = krp.retrieveCompleteLetters(runDate);
		
		// retrieve zipped customer letters file
		Map<String, byte[]> customerFile = krp.createZippedCustomerFile(runDate, completeLetters);
		
		//send letters and/or create local files
		krp.sendEmailNotification(customerFile);
		
		try {
			krp.conn.close();
			log.info("Successfully closed db connection.");
		} catch (Exception e) {
			log.error("Error attempting to close db connection.", e);
		}
		log.info("Exiting KitRequestProcessor.");
	}
	
	/**
	 * Processes profiles, builds letter and label files.
	 * @param pq
	 * @param fileBuilder
	 * @param runDate
	 */
	private void buildLetters(KitRequestRetriever krr, Date runDate) {
		// load the list and maps
		List<String> profileList = krr.getAllProfiles();
		Map<String,String> userMap = krr.getUserMap();
		Map<String,String> userCountry = krr.getCountryMap();
		
		log.debug("All Profiles size: " + profileList.size());
		log.debug("userMap size: " + userMap.size());
		log.debug("userCountry Map size: " + userCountry.size());
		
		//Initialize counters
		setProfileCount(0);
		setInvalidProfileCount(0);
		
		// If we have a map of profileId's/formats then process data.  Otherwise don't.
		if(! profileList.isEmpty()) {
			// Get profiles as UserDataVO objects.
			List<UserDataVO> listProfileVO = retrieveProfiles(profileList);
			log.info("Processing " + listProfileVO.size() + " profiles.");
			
			// If we have profile VO's then process them.  Otherwise don't.
			if ((!listProfileVO.isEmpty()) && (listProfileVO.size() > 0)) {
							
				Double latitude = 0.00;
				Double longitude = 0.00;
				UserDataVO tmpProfileVO = null;
				List<SurgeonVO> tmpListSurgeonVO = new ArrayList<SurgeonVO>();
				Iterator<UserDataVO> iterProfileVO = listProfileVO.iterator();
				
				// instantiate/initialize the letter builder
				PatientLetterBuilder pl = new PatientLetterBuilder();
				pl.setEnglishLetter(this.xmlEnglishBody);
				pl.setSpanishLetter(this.xmlSpanishBody);
				pl.setFirstImageHeader(this.firstPageImageHeader);
				pl.setSecondImageHeader(this.subPageImageHeader);
				//pl.setSendDate(sendDate);

				// Iterate through the profile objects.
				while(iterProfileVO.hasNext()) {
					profileCount++;
					//useShim = false;
					tmpProfileVO = iterProfileVO.next();
					log.debug("Processing #: " + profileCount + " - profileId: " + tmpProfileVO.getProfileId());
					// If the user has a valid address, process the user profile.
					if(isValidRequest(tmpProfileVO, userCountry)) {
						latitude = tmpProfileVO.getLatitude();
						longitude = tmpProfileVO.getLongitude();
						// Get clinics
						tmpListSurgeonVO = querySurgeons(latitude,longitude,searchRadius,surgeonLimit);
						// only build a letter if there are surgeons for this user
						if (tmpListSurgeonVO != null && !tmpListSurgeonVO.isEmpty()) {
							String kitFormat = userMap.get(tmpProfileVO.getProfileId());
							log.debug("processing " + kitFormat);
							// Create the patient's letter.
							if (kitFormat.equalsIgnoreCase("English")) {
								englishCount++;
								eLetters.append(pl.buildPatientLetter(tmpProfileVO,tmpListSurgeonVO,surgeonLimit,eLabels,kitFormat,englishCount));
							} else if (kitFormat.equalsIgnoreCase("Spanish")) {
								spanishCount++;
								sLetters.append(pl.buildPatientLetter(tmpProfileVO,tmpListSurgeonVO,surgeonLimit,sLabels,kitFormat,spanishCount));
							}
						} else {
							// log case in which user is valid but there are no surgeons within range.
							log.debug("No surgeons found within range of this user: " + tmpProfileVO.getProfileId());
						}
					} else {
						log.debug("incrementing invalidProfileCount.");
						incrementInvalidProfileCount();
						//TODO add method to process basic data about the invalid 
						// profile so that SJM has info on the request.
					}
				}
				log.info("Processed " + profileCount + " profiles.");
				log.info("Valid | Invalid | Total: " + 	(profileCount - getInvalidProfileCount()) + 
						" | " + getInvalidProfileCount() +  " | " + profileCount);
				log.info("English letters: " + englishCount);
				log.info("Spanish letters: " + spanishCount);
			} else {
				log.error("No profiles VO's could be obtained for valid profile ID's.");
				msg = "No profile VOs could be obtained for valid profile IDs.";
			}
		} else {
			log.info("No profile ID's exist for the specified date range.");
		}
	}
	
	/**
	 * Retrieves profile information for a given list of profile IDs.
	 * @param tmpListProfileIds
	 * @return
	 */
	protected List<UserDataVO> retrieveProfiles(List<String> tmpListProfileIds) {
		List<UserDataVO> listUserDataVO = new ArrayList<UserDataVO>();
		//ProfileManager pm = ProfileManagerFactory.getInstance(encryptKey, ProfileManagerFactory.DEFAULT_PROFILE_MANAGER);
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
	 * Helper method that checks to see if request is valid
	 * @param vo
	 * @param userCountry
	 * @return
	 */
	protected boolean isValidRequest (UserDataVO vo, Map<String,String> userCountry) {
		// request must have complete address, valid zipcode, be from the US, and
		// have a non-zero latitude and longitude.
		if(! vo.getLocation().isCompleteAddress()) {
			log.info("Failed 'complete address' test.");
			return false;
		} else if (! isValidProfileZipCode(vo.getZipCode())) {
			//info logging occurs in method call
			return false;
		} else if (! isUnitedStates(vo.getProfileId(),userCountry)) {
			//info logging occurs in method call
			return false;
		} else if (vo.getLatitude() == 0 && vo.getLongitude() == 0) {
			log.info("Latitude and longitude are both zero.");
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Checks length of zipcode to filter out international addresses.
	 * @param zcode
	 * @return
	 */
	protected boolean isValidProfileZipCode(String zcode) {
		int zLength = 0;
		String orig;
		if (zcode != null && zcode.length() > 0) {
			orig = zcode.trim();
			zLength = orig.length();
			// Check for valid zipcode (nnnnn or nnnnn-nnnn)
			if (zLength == 5 || (zLength == 10 && zcode.indexOf("-") > -1)) {
				boolean isValid = true;
				// If 10 chars, strip out the dash.
				if (zLength == 10) orig = orig.replace("-","");
				// Check for non-numbers.  New length is 9 chars.
				for (int i = 0; i < orig.length(); i++) {
					int val = orig.charAt(i);
					if (val < 48 || val > 57) {
						isValid = false;
						log.info("Invalid US zipcode - alphanumeric: " + orig);
						break;
					}
				}
				return isValid;
			} else {
				log.info("Invalid US zipcode - fails length test: " + zcode);
				return false;
			}
		} else {
			log.info("Invalid US zipcode - blank or null value: " + zcode);
			return false;
		}
	}
	
	/**
	 * Query for the contact's specified country value to see if contact
	 * is from the United States.
	 * @param action
	 * @param profId
	 * @param field
	 * @return
	 */
	protected boolean isUnitedStates(String profId, Map<String,String> countryMap) {
		
		String country = StringUtil.checkVal(countryMap.get(profId));
		if (country.length() > 0) {
			// Remove leading/trailing spaces, spaces/periods between words/chars.
			country = country.toLowerCase().trim();
			country = StringUtil.replace(country, " ", "");
			country = StringUtil.replace(country, ".", "");
			if (country.equals("us") || country.equals("usa") || 
				country.equals("unitedstates") || country.equals("unitedstatesofamerica") || 
				country.equals("losestadosunidos") || country.equals("estadosunidos") || 
				country.equals("eeuu")) {
				return true;
			} else {
				log.info("International - skipping: " + country);
				return false;
			}
		} else {
			// Assume that blank country value means contact is from the USA.
			return true;
		}
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
		String customDbSchema = props.getProperty("sbANSSchema");
		StringBuilder sql = new StringBuilder();
		sql.append("select a.surgeon_id, title_nm, first_nm, middle_nm, last_nm, ");
		sql.append("suffix_nm, a.website_url, clinic_nm, address_txt, address2_txt, ");
		sql.append("city_nm, state_cd, zip_cd, latitude_no, longitude_no, b.clinic_id, c.phone_number_txt ");
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
	 * Merges letter data into the file wrappers to create the files that
	 * will be sent
	 * @param runDate
	 */
	protected void mergeLetterData(Date runDate) {
		// merge letters with wrappers...
		String englishWrapper = xmlWrapper.toString();
		String spanishWrapper = xmlWrapper.toString();
		if (englishCount > 0) {
			eLetters = new StringBuilder(englishWrapper.replace("#reports#",eLetters.toString()));			
		}
		if (spanishCount > 0) {
			sLetters = new StringBuilder(spanishWrapper.replace("#reports#",sLetters.toString()));			
		}
	}
	
	/**
	 * Retrieves a map of letters, key is filename, value is letter body text.
	 * @param runDate
	 * @return
	 */
	protected Map<String, StringBuilder> retrieveCompleteLetters(Date runDate) {
		Map<String, StringBuilder> fileList = new LinkedHashMap<String, StringBuilder>();
		if (englishCount > 0 || spanishCount > 0) {
			if (englishCount > 0) {
				//build the letter filenames
				String ePrefix = "English_";
				StringBuilder eFileName = new StringBuilder();
				StringBuilder eLabelFileName = new StringBuilder();
				// build the English letters
				eFileName.append(ePrefix);
				eFileName.append(letterFileName).append("_");
				eFileName.append(runDate.toString()).append(props.getProperty("fileExt"));
				eLabelFileName.append(ePrefix);
				eLabelFileName.append(labelFileName).append("_");
				eLabelFileName.append(runDate.toString()).append(props.getProperty("labelFileExt"));
				//add English files to the map
				fileList.put(eFileName.toString(), eLetters);
				fileList.put(eLabelFileName.toString(), eLabels.getFileData());
			}
			
			if (spanishCount > 0) {
				String sPrefix = "Spanish_";
				StringBuilder sFileName = new StringBuilder();
				StringBuilder sLabelFileName = new StringBuilder();
				// build the Spanish letters
				sFileName.append(sPrefix);
				sFileName.append(letterFileName).append("_");
				sFileName.append(runDate.toString()).append(props.getProperty("fileExt"));
				sLabelFileName.append(sPrefix);
				sLabelFileName.append(labelFileName).append("_");
				sLabelFileName.append(runDate.toString()).append(props.getProperty("labelFileExt"));
				//add Spanish files to the map
				fileList.put(sFileName.toString(), sLetters);
				fileList.put(sLabelFileName.toString(), sLabels.getFileData());
			}
		}
		return fileList;
	}
	
	/**
	 * Creates the zipped customer file from the file Map passed in.  Returns the zipped file in a Map.
	 * @param runDate
	 * @param fileList
	 * @return
	 */
	private Map<String, byte[]> createZippedCustomerFile(Date runDate, Map<String, StringBuilder> fileList) {
		Map<String, byte[]> zippedFile = null;
		if (fileList.size() > 0) {
			// build the zipped file's name...
			StringBuilder customerFileName = new StringBuilder();
			customerFileName.append(props.getProperty("ftpFileName"));
			customerFileName.append("_").append(runDate.toString()).append(props.getProperty("ftpExt"));
			// call remote exec to use WinZip CL util.
			WinZipCommandLineUtility wcl = new WinZipCommandLineUtility(props);
			wcl.setZipFileName(customerFileName.toString());
			try {
				wcl.writeRawFiles(fileList);
				wcl.writeZippedFiles();
			} catch(Exception e) {
				log.error("Error completing zipped file operation, ", e);
			}
			
			try {
				wcl.deleteRawFiles();
				statusMsg.append("Raw source files successfully deleted.<br/>");
			} catch (Exception e) {
				log.error("Error deleting raw source files after zipped file operation, ", e);
			}
			
			Map<String,String> status = wcl.getExecMap();
			
			if (status.get("returnVal") != null && status.get("returnVal").equals("0")) {
				successStatus = true;
				statusMsg.append("Zip file creation successful.<br/>");
				try {
					zippedFile = wcl.retrieveZippedFile();
					log.info("Zipped file retrieved.");
				} catch (Exception e) {
					successStatus = false;
					statusMsg.append("Unable to retrieve zipped file.<br/>");
				}
				
				try {
					wcl.deleteZippedFile();
					log.info("Source .ZIP file successfully deleted.");
					statusMsg.append("Source .ZIP file successfully deleted.<br/>");
				} catch (Exception e) {
					successStatus = false;
					statusMsg.append("Error deleting source .ZIP file.<br/>");
				}
			} else {
				successStatus = false;
				statusMsg.append("Zip file creation failed with returnVal: " + status.get("returnVal"));
			}
		}
		return zippedFile;
	}
	
	/**
	 * Sends email notifications
	 * @param customerFile
	 */
	private void sendEmailNotification(Map<String, byte[]> customerFile) {
		boolean filesToSend = (customerFile != null);
		// if there weren't files to send, determine success/failure and msg.
		if (! filesToSend) {
			if (msg == null) {
				//msg wasn't set so everything was successful
				successStatus = true;
				statusMsg.append("No files to process today.");
			} else {
				statusMsg.append(msg);
			}
		} else {	// otherwise, email the customer file
			try {
				this.sendCustomerEmail(customerFile);
			} catch(MailException me) {
				log.error("Error sending customer email, ", me);	
			}	
		}
		
		// email the admin
		try {
			sendAdminEmail(successStatus, statusMsg.toString());
		} catch(MailException me) {
			log.error("Error sending admin email, ", me);	
		}
	}
	
	/**
	 * Sends email to customer including zipped/encrypted/password-protected customer letter file.
	 * @param customerFile
	 * @throws MailException
	 */
	private void sendCustomerEmail(Map<String, byte[]> customerFile) throws MailException {
		log.info("Sending customer email...");
		String to = props.getProperty("customerEmailTo");
		String from = props.getProperty("customerEmailFrom");
		String subject = props.getProperty("customerEmailSubject");
		StringBuilder body = new StringBuilder(props.getProperty("customerEmailBody"));
		this.sendEmail(from, to, subject, body, customerFile);
	}
		
	/**
	 * Sends SMT admin email
	 * @param success
	 * @param msg
	 * @param zippedFile
	 */
	private void sendAdminEmail(boolean success, String msg) throws MailException {
		log.info("Sending admin email...");
		String status = "";
		StringBuilder body = new StringBuilder();
		body.append("Messages:<br/>").append(msg).append("<br/>");
		
		// set subject header and body
		if (success) {
			status = "SUCCESS ---> Total|Valid|Invalid|English|Spanish: " +
				profileCount + "|" + (profileCount - invalidProfileCount) + "|" + 
				invalidProfileCount + "|" + englishCount + "|" + spanishCount;
			body.append("Valid profiles: ").append(profileCount).append("<br/>");
			body.append("Invalid profiles: ").append(invalidProfileCount).append("<br/>");
			body.append("English profiles: ").append(englishCount).append("<br/>");
			body.append("Spanish profiles: ").append(spanishCount).append("<br/>");
		} else {
			status = "FAILURE: " + msg; 
		}
		
		String smtpFrom = props.getProperty("smtpFrom");
		String smtpTo = props.getProperty("smtpTo");
		String subject = "SJM Kit Request Report Version 3.0 Send: " + status;
		this.sendEmail(smtpFrom, smtpTo, subject, body, null);
	}
	
	/**
	 * Sends email
	 * @param from
	 * @param to
	 * @param subject
	 * @param body
	 * @param attachment
	 * @throws MailException
	 */
	private void sendEmail(String from, String to, String subject, StringBuilder body, Map<String,byte[]> attachment) 
			throws MailException {
		
		String[] sendTo = to.split(",");
		SMTMail mail = new SMTMail(smtpServer,smtpPort,smtpUser,smtpPwd);
		mail.setFrom(from);
		mail.setRecpt(sendTo);
		mail.setSubject(subject);
		mail.setReplyTo(to);
		mail.setHtmlBody(body.toString());
		if (attachment != null && attachment.size() > 0) {
			for (String key : attachment.keySet()) {
				mail.addAttachment(key, attachment.get(key));
			}
		}
		// send the message
		mail.postMail();
	}
	
	/**
	 * loads configuration file
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void loadConfig() throws FileNotFoundException, IOException {
		// Load the config file.  Taken from com.depuy.datafeed.DataFeedUtil
		InputStream inStream = null;
		try {
			inStream = new FileInputStream(configPath);
			props.load(inStream);
			log.info("Successfully loaded config file");
		} catch (FileNotFoundException fnfe){
			log.error("Unable to find configuration file.");
			throw new FileNotFoundException();
		} catch (IOException ioe) {
			log.error("Unable to read/load configuration file.");
			throw new IOException();
		}
		finally {
			if (inStream != null) {
				try {
					inStream.close();
				} catch (Exception e) {}
			}
		}
	}
	
	/**
	 * Initializes members with config property values.
	 */
	private void initializeProperties() {
		// Get the db connection properties.
		dbDriver = props.getProperty("dbDriver");
		dbUrl = props.getProperty("dbUrl");
		dbUser = props.getProperty("dbUser");
		dbPassword = props.getProperty("dbPassword");
		// Get surgeon search properties.
		searchRadius = Convert.formatInteger(props.getProperty("searchRadius"));
		surgeonLimit = Convert.formatInteger(props.getProperty("surgeonLimit"));
		// Get document template properties.
		strTemplatePath = props.getProperty("templatePath");
		letterFileName = props.getProperty("letterFileName");
		labelFileName = props.getProperty("labelFileName");
		// SMTP properties
		smtpServer = props.getProperty("smtpServer");
		smtpPort = Integer.parseInt(props.getProperty("smtpPort"));
		smtpUser = props.getProperty("smtpUser");
		smtpPwd = props.getProperty("smtpPassword");
	}
	
	/**
	 * Retrieves a template file from the file system.
	 * @param templateName
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private StringBuilder loadTemplate(String templateName)
	throws FileNotFoundException, IOException {
		StringBuilder template = new StringBuilder();
		// Load the XML wrapper template file
		FileReader fr = null;
		BufferedReader br = null;
		try {
			fr = new FileReader(strTemplatePath + templateName);
			br = new BufferedReader(fr);
			String strIn = "";
			while((strIn = br.readLine()) != null) {
				template.append(strIn).append("\n");
			}
			log.info("Successfully loaded template file: " + templateName);
		} catch (FileNotFoundException fe) {
			log.error("Cannot load template " + templateName + ", ", fe);
			throw new FileNotFoundException();
		} catch (IOException ioe) {
			log.error("Cannot access template " + templateName + ", ", ioe);
			throw new IOException();
		} finally {
			try {
				br.close();
				fr.close();
			} catch (Exception e) {}
		}
		return template;
	}
	
	/**
	 * Gets a db connection
	 * @throws Exception
	 */
	private void getDBConnection() throws Exception {
		DatabaseConnection dbc = new DatabaseConnection(dbDriver,dbUrl,dbUser,dbPassword);
		try {
			conn = dbc.getConnection();
			log.info("Successfully established a database connection.");
		} catch (Exception de) {
			log.error("Could not establish a database connection. ",de);
			throw new Exception(de);
		}
	}
	
	/**
	 * @return the validProfileCount
	 */
	public int getProfileCount() {
		return profileCount;
	}

	/**
	 * @param validProfileCount the validProfileCount to set
	 */
	public void setProfileCount(int profileCount) {
		this.profileCount = profileCount;
	}

	/**
	 * Increments invalid profile counter.
	 */
	protected void setInvalidProfileCount(int invalidProfileCount){
		this.invalidProfileCount = invalidProfileCount;
	}
	
	/**
	 * Returns invalid profile count.
	 * @return
	 */
	protected int getInvalidProfileCount(){
		return invalidProfileCount;
	}
	
	/**
	 * Increments the invalid profile count
	 */
	protected void incrementInvalidProfileCount() {
		invalidProfileCount++;
	}

	/**
	 * @return the englishCount
	 */
	public int getEnglishCount() {
		return englishCount;
	}

	/**
	 * @param englishCount the englishCount to set
	 */
	public void setEnglishCount(int englishCount) {
		this.englishCount = englishCount;
	}

	/**
	 * @return the spanishCount
	 */
	public int getSpanishCount() {
		return spanishCount;
	}

	/**
	 * @param spanishCount the spanishCount to set
	 */
	public void setSpanishCount(int spanishCount) {
		this.spanishCount = spanishCount;
	}
	
}
