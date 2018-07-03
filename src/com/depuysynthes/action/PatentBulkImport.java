package com.depuysynthes.action;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.exception.FileException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.FileManager;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.siliconmtn.util.parser.AnnotationParser;

import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <b>Title: </b>PatentBulkImport.java
 <b>Project: </b>
 <b>Description: </b>
 <b>Copyright: </b>(c) 2000 - 2018 SMT, All Rights Reserved
 <b>Company: Silicon Mountain Technologies</b>
 @author cobalt
 @version 1.0
 @since Jul 2, 2018
 <b>Changes:</b> 
 ***************************************************************************/
public class PatentBulkImport extends CommandLineUtil {

	private final String propertiesPath = "/data/scripts/DSPatentBulkImport.properties";
	private String customDb;
	
	/**
	* Constructor
	*/
	public PatentBulkImport(String[] args) {
		super(args);
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

			// load source file
			FilePartDataBean dataBean = loadSourceFile();

			// parse the source file into a List of beans
			ArrayList<Object> beans = parseSourceFile(dataBean);

			if (! beans.isEmpty()) {
				// process parsed beans.
				processPatentBeans(beans);
			}
			
		 /* 7. Send email */
			
		} catch (Exception e) {
			errMsg = e.getMessage();
			
		} finally {
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
		
		customDb = props.getProperty(Constants.CUSTOM_DB_SCHEMA);

		// load db conn
		loadDBConnection(props);		
	}


	/**
	 * 
	 * @return
	 * @throws FileException
	 */
	private FilePartDataBean loadSourceFile() throws FileException {
		FileManager fm = new FileManager();
		FilePartDataBean bean = new FilePartDataBean();
		bean.setFileData(fm.retrieveFile(props.getProperty("sourceFilePath")));
		return bean;
	}


	/**
	 * turn excel into list of beans using annotations.  Import the beanList into the database.
	 * @param fpdb
	 * @return
	 * @throws Exception
	 */
	private ArrayList<Object> parseSourceFile(FilePartDataBean fpdb) 
			throws Exception {
		log.debug("parseSourceFile...");
		AnnotationParser parser;
		Map<Class<?>, Collection<Object>> beans;
		try {
			parser = new AnnotationParser(PatentVO.class, fpdb.getExtension());

			// Parser then returns the list of populated beans
			beans = parser.parseFile(fpdb, true);

		} catch(InvalidDataException e) {
			throw new ActionException("Error parsing source data file, ", e);
		}

		return new ArrayList<>(beans.get(PatentVO.class));
	}
	
	
	/**
	 * 
	 * @param beanList
	 * @throws SQLException
	 * @throws ActionException
	 */
	private void processPatentBeans(ArrayList<Object> beanList) 
			throws SQLException, ActionException {
		//Disable the db autocommit for the insert batch
		dbConn.setAutoCommit(false);

		// capture history of existing 'live' records.
		String companyNm = null;

		// loop patent beans, update org, set company name
		for (Object o : beanList) {
			PatentVO vo = (PatentVO) o;

			// get company name from first patent record
			if (companyNm == null) 
				companyNm = vo.getCompany();

			// set org on each vo
			vo.setOrganizationId(props.getProperty("organizationId"));
		}
		
		// write history record for all current 'live' patent records for company.
		writeHistoryByCompany(companyNm);

		// delete all patents for the company
		deleteByCompany(companyNm);

		// import patents
		 importBeans(beanList);

		//commit only after the entire import succeeds
		dbConn.commit();
	}

	/**
	 * 
	 * @param companyNm
	 * @throws SQLException
	 */
	private void writeHistoryByCompany(String companyNm) throws SQLException {
		// build insert statement
		StringBuilder sql = new StringBuilder(350);
		sql.append("insert into ").append(customDb);
		sql.append(PatentHistoryManager.PATENT_HISTORY_TABLE).append(" ");
		sql.append("(patent_id,action_id,organization_id,company_nm,code_txt, ");
		sql.append("item_txt,desc_txt,patents_txt,redirect_nm,redirect_address_txt, ");
		sql.append("status_flg,profile_id,activity_dt) ");
		sql.append("select patent_id, action_id, organization_id, company_nm, code_txt, item_txt, ");
		sql.append("desc_txt, patents_txt, redirect_nm, redirect_address_txt, status_flg, ");
		sql.append("profile_id, coalesce(update_dt, create_dt) ");
		sql.append("from ");
		sql.append(customDb).append(PatentManagementAction.PATENT_TABLE).append(" ");
		sql.append("where company_nm = ?");
		
		log.debug("writeHistoryByCompany SQL: " + sql + " | " + companyNm);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, companyNm);
			ps.execute();
		}
	}

	/**
	 * deletes all existing records for the given companyNm
	 * @param actionId
	 * @param companyNm
	 */
	private void deleteByCompany(String companyNm) throws ActionException {
		StringBuilder sql = new StringBuilder(100);
		sql.append("delete from ").append(customDb).append("dpy_syn_patent ");
		sql.append("where company_nm=?");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, companyNm);
			ps.executeUpdate();

		} catch (SQLException sqle) {
			log.error("could not delete company records", sqle);
			 throw new ActionException(sqle);
		}
	}


	 /**
	  * Imports the beans as a set of new records to the event_entry table
	  * @param beanList
	  * @param actionId
	  * @throws ActionException
	  */
	 private void importBeans(ArrayList<Object> beanList) throws ActionException {
		 if (beanList == null || beanList.isEmpty()) return;
		StringBuilder sql = new StringBuilder(150);
		sql.append("insert into ").append(customDb);
		sql.append(PatentManagementAction.PATENT_TABLE).append(" ");
		sql.append("(action_id, organization_id, company_nm, code_txt, ");
		sql.append("item_txt, desc_txt, patents_txt, redirect_nm, redirect_address_txt, create_dt) ");
		sql.append("values (?,?,?,?,?,?,?,?,?,?)");
		log.debug(sql);

		 try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			 for ( Object obj : beanList ) {
				 //Casts the generic object to PatentVO
				 PatentVO vo = (PatentVO) obj;

				 //code is required, skip any without it
				 if (vo.getCode() == null || 
						 vo.getCode().length() == 0) continue;
// TODO verify table fields...
				 ps.setString(1, vo.getActionId());
				 ps.setString(2, vo.getOrganizationId());
				 ps.setString(3, vo.getCompany());
				 ps.setString(4, vo.getCode());
				 ps.setString(5, vo.getItem());
				 ps.setString(6, vo.getDesc());
				 ps.setString(7, StringUtil.replace(vo.getPatents(), "|","; ")); //clean up the tokenized data and store it the way we'll need it for display
				 ps.setString(8, vo.getRedirectName());
				 ps.setString(9, vo.getRedirectAddress());
				 ps.setTimestamp(10, Convert.getCurrentTimestamp());
				 ps.addBatch();
				 log.debug("added to batch: "+ vo.getCode());
			 }
			 ps.executeBatch();
		 } catch (SQLException e){
			 throw new ActionException("Error inserting records into dpy_syn_patent table.",e);
		 }
	 }
	
		
	
	
	/**
	 * 
	 * @param errMsg
	 */
	private void sendAdminEmail(String errMsg) { 
		try {
			EmailMessageVO evo = new EmailMessageVO();
			evo.setFrom(props.getProperty("emailFrom"));
			evo.addRecipient(props.getProperty("emailTo"));
			evo.setSubject((errMsg == null ? "Success" : "FAILED") + " - DS Bulk Patent Import");
			evo.setHtmlBody("");
			sendEmail(evo);
		} catch (Exception e) {
			log.error("Error sending admin email, ", e);
		}
	}
	
}
