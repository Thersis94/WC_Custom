package com.depuysynthes.action;

// Java 8
import java.util.ArrayList;
import java.util.List;

// Apache Log4j
import org.apache.log4j.PropertyConfigurator;

// SMTBaseLibs
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.util.CommandLineUtil;

/*****************************************************************************
 <b>Title: </b>PatentBulkImport.java
 <b>Project: </b> WC_Custom
 <b>Description: </b>Bulk patent import script.  Reads and parses source file, writes
 history records for currently active patents, disables those same currently active patents
 and then imports the source records and makes them active.
 <b>Copyright: </b>(c) 2000 - 2018 SMT, All Rights Reserved
 <b>Company: Silicon Mountain Technologies</b>
 @author cobalt
 @version 1.0
 @since Jul 2, 2018
 <b>Changes:</b> 
 ***************************************************************************/
public class PatentBulkImport extends CommandLineUtil {

	private final String propertiesPath = "/data/git/WC_Custom/scripts/ds/patent-bulk-import.properties";
	List<String> messages;


	/**
	* Constructor
	*/
	public PatentBulkImport(String[] args) {
		super(args);
		PropertyConfigurator.configure("/data/git/WC_Custom/scripts/ds/patent-bulk-import-log4j.properties");
		messages = new ArrayList<>();
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PatentBulkImport pbi = new PatentBulkImport(args);
		pbi.run();
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		String errMsg = null;
		try {
			// load properties and dbConn.
			init(propertiesPath);

			PatentImportUtility util = new PatentImportUtility(props);
			util.setDbConn(dbConn);
			util.importPatents();

			messages.add(util.getResultMessage());

		} catch (NullPointerException npe) {
			errMsg = npe.toString();

		} catch (Exception e) {
			errMsg = e.getMessage();

		} finally {
			// clean-up
			closeDBConnection();
		}
		// send admin email
		sendAdminEmail(errMsg);
	}


	/**
	 * Method initializing properties file and DB connection.
	 * @throws Exception
	 */
	private void init(String propertiesPath) throws Exception {
		loadProperties(propertiesPath);
		if (props == null || props.isEmpty()) {
			throw new Exception("Error loading properties file, file is missing or empty.");
		}

		// load db conn
		loadDBConnection(props);		
	}


	/**
	 * 
	 * @param errMsg
	 */
	private void sendAdminEmail(String errMsg) { 
		try {
			EmailMessageVO evo = new EmailMessageVO();
			evo.setFrom(props.getProperty("fromAddress"));
			evo.addRecipient(props.getProperty("toAddress"));
			evo.setSubject((errMsg == null ? "Success - " : "FAILED - ") + props.getProperty("subject"));
			StringBuilder body = new StringBuilder(1000);
			for (String msg : messages) {
				body.append(msg).append("<br/>");
			}
			body.append("DS bulk patent import complete.");
			if (errMsg != null) {
				body.append("<br/><br/>Error message is: ").append(errMsg);
				body.append("<br/><br/>Check script log for more details.");
			}
			evo.setHtmlBody(body.toString());
			sendEmail(evo);
		} catch (Exception e) {
			log.error("Error sending admin email, ", e);
		}
	}
	
}
