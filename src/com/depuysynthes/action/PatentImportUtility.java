package com.depuysynthes.action;

//Java 8
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

// Apache Log4j
import org.apache.log4j.Logger;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.exception.FileException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.filter.fileupload.Constants;
import com.siliconmtn.io.FileManager;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.siliconmtn.util.parser.AnnotationParser;

// WebCrescendo libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.admin.action.OrganizationAction;

/*****************************************************************************
 <b>Title: </b>PatentImportUtility.java
 <b>Project: </b> WC_Custom
 <b>Description: </b>Patent import utiity.  Parses source file, writes
 history records for currently active patents, disables those same currently active patents
 and then imports the source records and makes them active.
 <b>Copyright: </b>(c) 2000 - 2018 SMT, All Rights Reserved
 <b>Company: Silicon Mountain Technologies</b>
 @author cobalt
 @version 1.0
 @since Jul 2, 2018
 <b>Changes:</b> 
 ***************************************************************************/
public class PatentImportUtility {

	public static final String PROP_IMPORT_PROFILE_ID = "importProfileId";
	public static final String PROP_SOURCE_FILE_PATH = "sourceFilePath";
	public static final String PROP_PRESERVE_PATENTS = "preservePatents";
	
	private Logger log = Logger.getLogger(getClass().getName());
	private FilePartDataBean filePartDataBean;
	private String customDbSchema;
	private String resultMsg;
	private Connection dbConn;
	private int patentCount;
	private String actionId;
	private String organizationId;
	private String importProfileId;
	private String companyName;
	/**
	 * preservePatents: If set to true, the utility disables the patent records 
	 * being replaced by the import.  If set to false, the utility deletes the patent 
	 * records being replaced by the import.
	 */
	private boolean preservePatents;
	private boolean isError;

	/**
	* Constructor
	*/
	public PatentImportUtility() {
		// empty by design
	}
	
	/**
	 * Convenience constructor for scripts
	* Constructor
	 * @throws FileException 
	 */
	public PatentImportUtility(Properties properties) throws FileException {
		this();
		initializeProperties(properties);
	}

	/**
	 * Imports patents
	 */
	public void importPatents() {
		try {
			// parse the source file into a List of beans
			List<Object> beans = parseSourceFile(filePartDataBean);

			if (! beans.isEmpty()) {
				// process parsed beans.
				processPatentBeans(beans);
			} else {
				resultMsg = "No patent records found in source file.";
			}
			
		} catch (Exception e) {
			log.error("Error processing patents file, ", e);
			isError = true;
			resultMsg = e.getMessage();
		}

	}


	/**
	 * turn excel into list of beans using annotations.  Import the beanList into the database.
	 * @param fpdb
	 * @return
	 * @throws Exception
	 */
	private List<Object> parseSourceFile(FilePartDataBean fpdb) 
			throws Exception {
		log.debug("parseSourceFile...extension is: " + fpdb.getExtension());
		AnnotationParser parser;
		Map<Class<?>, Collection<Object>> beans;
		try {
			parser = new AnnotationParser(PatentVO.class, fpdb.getExtension());

			// Parser then returns the list of populated beans
			beans = parser.parseFile(fpdb, true);
			log.info("Found " + beans.size() + " patent records for import in the source file.");
			log.info("Preserve patents flag set to: " + preservePatents);
		} catch(InvalidDataException e) {
			log.error("Error parsing source data file, ", e);
			throw new ActionException("Error parsing source data file, ", e);
		}

		return new ArrayList<>(beans.get(PatentVO.class));
	}
	
	
	/**
	 * Processes the patent records parsed from the import
	 * source file.
	 * @param beanList
	 * @throws SQLException
	 * @throws ActionException
	 */
	private void processPatentBeans(List<Object> beanList) 
			throws SQLException, ActionException {

		// loop patent beans, update org, set company name
		for (Object o : beanList) {
			PatentVO vo = (PatentVO) o;

			// get company name from first patent record
			if (companyName == null) 
				companyName = vo.getCompany();

			// set org on each vo
			vo.setOrganizationId(organizationId);
			vo.setActionId(actionId);
			vo.setStatusFlag(PatentAction.STATUS_ACTIVE);
		}

		log.info("Processed patent import for company name: " + companyName);

		//Disable the db autocommit for the insert batch
		try {
			dbConn.setAutoCommit(false);

			// write history record for all current 'live' patent records for company.
			writeHistoryByCompany();

			if (preservePatents) {
				// disable all 'live' patents for the company
				disableByCompany();
			} else {
				// delete all patents for the company
				deleteByCompany();
			}

			// import patents
			importBeans(beanList);

			//commit only after the entire import succeeds
			dbConn.commit();

			resultMsg = formatResultsMessage();
		} finally {
			try {
				// restore autocommit state
				dbConn.setAutoCommit(true);
			} catch (SQLException e) {
				// empty by design
			}
		}

	}


	/**
	 * Formats result message String. 
	 * @param patentCount
	 * @return
	 */
	private String formatResultsMessage() {
		StringBuilder msg = new StringBuilder(100);
		msg.append("Successfully imported ");
		msg.append(patentCount);
		msg.append(" patent records from source file for company ");
		msg.append(companyName).append(".");
		return msg.toString();
	}


	/**
	 * Inserts history records into the patent history table for all
	 * currently 'active' patent records for the target operating
	 * compnay.  This is performed prior to replacing all patent records
	 * for the target operating company with the records being imported..
	 * @param companyName
	 * @throws SQLException
	 */
	private void writeHistoryByCompany() throws SQLException {
		// build insert statement
		StringBuilder sql = new StringBuilder(350);
		sql.append("insert into ").append(customDbSchema);
		sql.append(PatentHistoryManager.PATENT_HISTORY_TABLE).append(" ");
		sql.append("(patent_id,action_id,organization_id,company_nm,code_txt, ");
		sql.append("item_txt,desc_txt,patents_txt,redirect_nm,redirect_address_txt, ");
		sql.append("status_flg,profile_id,activity_dt) ");
		sql.append("select patent_id, action_id, organization_id, company_nm, code_txt, item_txt, ");
		sql.append("desc_txt, patents_txt, redirect_nm, redirect_address_txt, status_flg, ");
		sql.append("profile_id, coalesce(update_dt, create_dt) ");
		sql.append("from ");
		sql.append(customDbSchema).append(PatentManagementAction.PATENT_TABLE).append(" ");
		sql.append("where company_nm = ? and status_flg = ?");

		log.debug("writeHistoryByCompany SQL: " + sql + " | " + companyName);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, companyName);
			ps.setInt(2, PatentAction.STATUS_ACTIVE);
			int count = 0;
			if (! ps.execute()) {
				count = ps.getUpdateCount();
			}

			log.info("Archived patent history for " + count + " 'live' patents.");
		}
	}


	/**
	 * deletes all existing records for the given companyName
	 * @param actionId
	 * @param companyName
	 */
	private void disableByCompany() throws ActionException {
		StringBuilder sql = new StringBuilder(100);
		sql.append("update ").append(customDbSchema).append("dpy_syn_patent ");
		sql.append("set status_flg = ? where company_nm=?");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1, PatentAction.STATUS_DISABLED);
			ps.setString(2, companyName);
			ps.executeUpdate();

		} catch (SQLException sqle) {
			log.error("could not delete company records", sqle);
			 throw new ActionException(sqle);
		}
		
		log.info("Disabled all 'live' patents for " + companyName);
	}


	/**
	 * Deletes all existing records for the given companyName
	 * @param actionId
	 * @param companyName
	 */
	private void deleteByCompany() throws ActionException {
		StringBuilder sql = new StringBuilder(100);
		sql.append("delete from ").append(customDbSchema).append("dpy_syn_patent ");
		sql.append("where company_nm=?");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, companyName);
			ps.executeUpdate();

		} catch (SQLException sqle) {
			log.error("could not delete company records", sqle);
			 throw new ActionException(sqle);
		}
		
		log.info("Deleted all archived patents for " + companyName);
	}


	 /**
	  * Imports the beans as a set of new records to the event_entry table
	  * @param beanList
	  * @param actionId
	  * @throws ActionException
	  */
	 private void importBeans(List<Object> beanList) throws ActionException {
		 if (beanList == null || beanList.isEmpty()) return;
		StringBuilder sql = new StringBuilder(150);
		sql.append("insert into ").append(customDbSchema);
		sql.append(PatentManagementAction.PATENT_TABLE).append(" ");
		sql.append("(action_id, organization_id, company_nm, code_txt, ");
		sql.append("item_txt, desc_txt, patents_txt, redirect_nm, redirect_address_txt, ");
		sql.append("status_flg, profile_id, create_dt) ");
		sql.append("values (?,?,?,?,?,?,?,?,?,?,?,?)");
		log.debug("patent record insert sql: " + sql);

		 try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			 for ( Object obj : beanList ) {
				 //Casts the generic object to PatentVO
				 PatentVO vo = (PatentVO) obj;

				 //code is required, skip any without it
				 if (vo.getCode() == null || 
						 vo.getCode().length() == 0) continue;
				 ps.setString(1, vo.getActionId());
				 ps.setString(2, vo.getOrganizationId());
				 ps.setString(3, vo.getCompany());
				 ps.setString(4, vo.getCode());
				 ps.setString(5, vo.getItem());
				 ps.setString(6, vo.getDesc());
				 ps.setString(7, StringUtil.replace(vo.getPatents(), "|","; ")); //clean up the tokenized data and store it the way we'll need it for display
				 ps.setString(8, vo.getRedirectName());
				 ps.setString(9, vo.getRedirectAddress());
				 ps.setInt(10, vo.getStatusFlag());
				 ps.setString(11, importProfileId);
				 ps.setTimestamp(12, Convert.getCurrentTimestamp());
				 ps.addBatch();

				 patentCount++;
			 }
			 ps.executeBatch();
		 } catch (SQLException e){
			 throw new ActionException("Error inserting records into custom patent table.",e);
		 }
		 
		 log.info("Inserted " + patentCount + " patent records for " + companyName);
	 }


	/**
	 * Sets FilePartDataBean using the sourceFilePath argument passed to the method.  This
	 * is a convenience method for scripts.
	 * @param sourceFilePath
	 * @throws FileException
	 */
	private void setFilePartDataBean(String sourceFilePath) throws FileException {
		log.info("loading source file from: " + sourceFilePath);
		FileManager fm = new FileManager();
		FilePartDataBean bean = new FilePartDataBean();
		bean.setFileName(sourceFilePath);
		bean.setFileData(fm.retrieveFile(sourceFilePath));
		bean.setExtensionByName(bean.getFileName());
		log.info("Loaded source file with size: " + bean.getFileSize());
		log.info("Filename | extension: " + bean.getFileName() + "|" + bean.getExtension());
		setFilePartDataBean(bean);
	}


	/**
	 * @param filePartDataBean the filePartDataBean to set
	 */
	public void setFilePartDataBean(FilePartDataBean filePartDataBean) {
		this.filePartDataBean = filePartDataBean;
	}


	/**
	 * @param customDbSchema the customDbSchema to set
	 */
	public void setCustomDbSchema(String customDbSchema) {
		this.customDbSchema = customDbSchema;
	}


	/**
	 * @param dbConn the db connection to set
	 */
	public void setDbConn(Connection dbConn) {
		this.dbConn = dbConn;
	}

	/**
	 * @param actionId the actionId to set
	 */
	public void setActionId(String actionId) {
		this.actionId = actionId;
	}

	/**
	 * @param organizationId the organizationId to set
	 */
	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
	}

	/**
	 * @param importProfileId the importProfileId to set
	 */
	public void setImportProfileId(String importProfileId) {
		this.importProfileId = importProfileId;
	}

	/**
	 * @param preservePatents the preservePatents to set
	 */
	public void setPreservePatents(boolean preservePatents) {
		this.preservePatents = preservePatents;
	}

	/**
	 * Initializes fields by parsing the properties file passed
	 * in as the method argument.
	 * @param properties
	 * @throws FileException 
	 */
	public void initializeProperties(Properties properties) throws FileException {
		organizationId = properties.getProperty(OrganizationAction.ORGANIZATION_ID);
		actionId = properties.getProperty(SBActionAdapter.ACTION_ID);
		importProfileId = properties.getProperty(PROP_IMPORT_PROFILE_ID);
		customDbSchema = properties.getProperty(Constants.CUSTOM_DB_SCHEMA);
		preservePatents = Convert.formatBoolean(properties.getProperty(PROP_PRESERVE_PATENTS));
		setFilePartDataBean(properties.getProperty(PROP_SOURCE_FILE_PATH));
	}


	/**
	 * @return the patentCount
	 */
	public int getPatentCount() {
		return patentCount;
	}

	/**
	 * @return the companyName
	 */
	public String getCompanyName() {
		return companyName;
	}

	/**
	 * @return the messages
	 */
	public String getResultMessage() {
		return resultMsg;
	}

	/**
	 * @return the isError
	 */
	public boolean isError() {
		return isError;
	}
	
}
