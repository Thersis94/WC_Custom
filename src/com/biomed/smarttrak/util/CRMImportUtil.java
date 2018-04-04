package com.biomed.smarttrak.util;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.biomed.smarttrak.admin.vo.BiomedCRMCustomerVO;
import com.depuy.datafeed.tms.TransactionManager;
import com.depuy.datafeed.tms.db.ResponseDB;
import com.depuy.datafeed.tms.modules.DataSourceVO;
import com.depuy.datafeed.tms.modules.ErrorModule;
import com.depuy.datafeed.tms.modules.PhysicianVO;
import com.depuy.datafeed.tms.modules.ProfileVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.io.mail.SMTMailHandler;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: CRMImportUtil.java<p/>
 * <b>Description: Gets all reminders and the user that created them </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2007<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Mar 29, 2017
 ****************************************************************************/

public class CRMImportUtil  extends CommandLineUtil {
	private static final int BUFFER_SIZE = 2048;
	
	private String DELIMITER;
	
	// Used to store variables that are used in the MessageSender constructor
	private HashMap<String, Object> attributes;
	
	private String directory;
	private String fileName;
	
	
	// Stores any errors that we run into throughout the import
	private List<String> errors;
	
	public CRMImportUtil(String[] args) {
		super(args);
		loadProperties("scripts/biomed.properties");
		loadDBConnection(props);
		System.out.println(args+"|"+args.length);
		if (args.length >= 2) {
			directory = args[0];
			fileName = args[1];
		} else {
			fileName = props.getProperty("fileName");
			directory = props.getProperty("directory");
		}
		
		prepareValues();
		errors = new ArrayList<>();
		loadDBConnection(props);
	}
	
	
	/**
	 * Set up the class values and determine which file headers we are going
	 * to need to use in order get the product information from the supplied files
	 */
	private void prepareValues() {
		attributes = new HashMap<>();
		attributes.put("defaultMailHandler", new SMTMailHandler(props));
		attributes.put("instanceName", props.get("instanceName"));
		attributes.put("appName", props.get("appName"));
		attributes.put("adminEmail", props.get("adminEmail"));
		attributes.put(Constants.PROFILE_MANAGER_CLASS, props.get(Constants.PROFILE_MANAGER_CLASS));
		attributes.put(Constants.ENCRYPT_KEY, props.get(Constants.ENCRYPT_KEY));
		
		DELIMITER = props.getProperty("delimiter");
		
	}
    
    
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {        
		//Create an instance of the MedianBinImporter
		CRMImportUtil imp = new CRMImportUtil(args);
		imp.run();
	}
	
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		try{
			String transactionId = prepareTransaction();
			List<BiomedCRMCustomerVO> customers = getCustomersFromFile(transactionId);
			prepareProfiles(customers);
			
			DBProcessor db = new DBProcessor(dbConn, props.getProperty(Constants.DATA_FEED_SCHEMA));
			
			for (BiomedCRMCustomerVO customer : customers) {
				db.insert(customer);
				log.info(db.getGeneratedPKId());
				customer.setCustomerId(db.getGeneratedPKId());
				saveMapInfo(customer);
			}
		} catch(Exception e) {
			log.error(e);
		}
			
			
			
		log.info("Ended at " + Convert.getCurrentTimestamp());
	}
	
	private void saveMapInfo(BiomedCRMCustomerVO customer) {
		customer.buildResponseList();
		ResponseDB db = new ResponseDB(BiomedCRMCustomerVO.CustomerField.getQuestionMap());
		try {
			db.store(dbConn, customer, props.getProperty(Constants.DATA_FEED_SCHEMA), new ArrayList<ErrorModule>());
		} catch (DatabaseException e) {
			log.error("Failed to add map info for " + customer.getProfile().getFirstName() + customer.getProfile().getLastName());
		}
	}


	private String prepareTransaction() {
		try {
		TransactionManager manager = new TransactionManager();
		DataSourceVO data = new DataSourceVO();
		data.setSourceId(BiomedCRMCustomerVO.CALL_SOURCE_ID);
		data.setSourceName(BiomedCRMCustomerVO.SOURCE_NAME);
		return manager.createTransaction(dbConn, props.getProperty(Constants.DATA_FEED_SCHEMA), data, 1, new Date());
		}catch(Exception e){
			log.error(e);
		}
		return null;
	}


	private void prepareProfiles(List<BiomedCRMCustomerVO> customers) {
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		for (BiomedCRMCustomerVO customer : customers) {
			try {
				String profileId = pm.checkProfile(customer.getPhysician(), dbConn);
				if (StringUtil.isEmpty(profileId)) {
					pm.updateProfile(customer.getPhysician(), dbConn);
				} else {
					customer.setPhysicianId(profileId);
				}
				
				profileId = pm.checkProfile(customer.getProfile(), dbConn);
				if (StringUtil.isEmpty(profileId)) {
					pm.updateProfile(customer.getProfile(), dbConn);
				} else {
					customer.setPhysicianId(profileId);
				}
				
			} catch (Exception e) {
				log.error(e);
			}
		}
	}


	private List<BiomedCRMCustomerVO> getCustomersFromFile(String transactionId) throws IOException {
		List<BiomedCRMCustomerVO> customers = new ArrayList<>();
		try{
		String fileData = getDataFromFile();

		// Build the map of pertinent columns from the supplied files
		String[] rows;
		log.info(fileData);
		rows = fileData.split("\\r?\\n", -1);
		log.info(fileName +"|"+rows.length);
		
		String[] headers = rows[0].split(DELIMITER, -1);
		log.info("1");
		String[] cols;
		for (int i=1; i<rows.length; i++) {
			log.info(rows[i]);
			cols = rows[i].split(DELIMITER, -1);
			if (cols.length < headers.length) {
				errors.add("Invalid data at line " + i+1 +" in file " + fileName + ".  Recieved "+cols.length+" columns, expected "+headers.length + " columns.");
				continue;
			}
			log.info("Building Customer");
			customers.add(buildCustomer(cols, transactionId));
		}
		}catch(Exception e){
			log.error(e);
		}
		
		
		log.info("Built Customers");
		return customers;
	}


	private BiomedCRMCustomerVO buildCustomer(String[] cols, String transactionId) {
		int i = 0;
		
		BiomedCRMCustomerVO customer = new BiomedCRMCustomerVO();
		try{
		ProfileVO profile = new ProfileVO();
		// Set Customer Profile fields
		profile.setFirstName(cols[i++]);
		profile.setLastName(cols[i++]);
		profile.setEmailAddress(cols[i++]);
		PhoneVO p = new PhoneVO(cols[i++]);
		p.setPhoneType("HOME");
		profile.addPhone(p);
		customer.setProfile(profile);
		
		// Set Customer Response Fields
		customer.setCompanyName(cols[i++]);
		customer.setProspectType(cols[i++]);
		customer.setStatus(cols[i++]);
		customer.setSubscriptionLevel(cols[i++]);
		customer.setExpirationDate(cols[i++]);
		
		// Set onwer information so we can find profile id in the main table
		// or ensure that we will have enough to create a profile entry
		PhysicianVO owner = new PhysicianVO();
		owner.setFirstName(cols[i++]);
		owner.setLastName(cols[i++]);
		owner.setEmailAddress(cols[i++]);
		customer.setPhysician(owner);
		
		// Set any deal specific fields here
		customer.setRepComments(cols[i++]);
		log.info(cols[i]);
		customer.setAttemptDate(Convert.formatDate(cols[i++]));
		log.info(customer.getAttemptDate());
		customer.setAcquisitionValue(Convert.formatFloat(cols[i++]));
		if (StringUtil.isEmpty(cols[i])) {
			customer.setLeadTypeId(BiomedCRMCustomerVO.LEAD_TYPE_ID);
		} else {
			customer.setLeadTypeId(Convert.formatInteger(cols[i]));
		}
		
		customer.setTransactionId(transactionId);

		}catch(Exception e){
			log.error(e);
		}
		log.info("Built customer");
		return customer;
	}

	
	private String getDataFromFile() throws IOException {
		InputStream input = new FileInputStream(directory+fileName);
		// Get the file
		byte[] b = new byte[2048];
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int c = 0;
		while ((c = input.read(b, 0, BUFFER_SIZE)) != -1) {
			baos.write(b, 0, c);
		}
		
		input.close();
		return new String(baos.toByteArray());
	}
	
}
