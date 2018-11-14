package com.depuysynthes.srt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import com.depuysynthes.srt.data.SRTUserImportModule;
import com.depuysynthes.srt.util.SRTUtil;
import com.depuysynthes.srt.vo.SRTRosterVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.http.filter.fileupload.Constants;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.siliconmtn.util.parser.AnnotationParser;
import com.siliconmtn.workflow.WorkflowLookupUtil;
import com.siliconmtn.workflow.data.WorkflowMessageVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.util.WorkflowSender;

/****************************************************************************
 * <b>Title:</b> SRTRosterImportAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages Storing, Initial Parsing and creation of SRT
 * Roster Import Workflow.  Accepts an Excel file on the request, does
 * preliminary validation/saving of data to temp table and then sends WF
 * request that manages updating, inserting or deactivating profiles.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Apr 24, 2018
 ****************************************************************************/
public class SRTRosterImportAction extends SimpleActionAdapter {

	public static final String TEMP_SCHEMA = "temp";
	public static final String TEMP_TABLE_PREFIX = "temp_roster_";
	private static final String USER_UPLOAD = "USER_UPLOAD";
	private static final String USER_IMPORT = "USER_IMPORT";

	public SRTRosterImportAction() {
		super();
	}

	public SRTRosterImportAction(ActionInitVO init) {
		super(init);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void build(ActionRequest req) throws ActionException {
		try {
			//Retrieve file from Request.
			FilePartDataBean userData = loadFileFromReq(req);

			//Parse Excel File and store results in SRTRosterVO Framework.
			AnnotationParser parser = new AnnotationParser(SRTRosterVO.class, userData.getExtension());
			Map<Class<?>, Collection<Object>> parsedData = parser.parseFile(userData, false);
			List<SRTRosterVO> tempUserData = (List)new ArrayList<>(parsedData.get(SRTRosterVO.class));

			//Generate Temp Table and store Data.
			String tempTableNm = persistData(tempUserData, userData.getFileName());

			//Generate Workflow Request
			buildWorkflowRequest(tempTableNm, userData.getCanonicalPath(), SRTUtil.getOpCO(req));
		} catch(Exception e) {
			throw new ActionException("There was a problem processing the uploaded User File.", e);
		}
	}

	/**
	 * Extracts Upload File from Request.
	 * @param req
	 * @return
	 * @throws IOException 
	 * @throws InvalidFormatException 
	 * @throws EncryptedDocumentException 
	 */
	private FilePartDataBean loadFileFromReq(ActionRequest req) throws IOException {

		//Build FilePath for Uploaded File.
		String filePath = req.getParameter("filePathText");
		String fullPath = StringUtil.checkVal(attributes.get(Constants.PATH_TO_BINARY));
		if (! fullPath.endsWith("/")) fullPath += "/";
		fullPath += "file_transfer" + filePath;
		log.debug(fullPath);
		Path path = Paths.get(fullPath);
		byte[] data = Files.readAllBytes(path);

		//Build FPDB
		FilePartDataBean fpdb = new FilePartDataBean();
		fpdb.setFileData(data);
		fpdb.parseNameFromPath(fullPath);
		fpdb.setCanonicalPath(fullPath);
		return fpdb;
	}

	/**
	 * Generate the Temp Table and save records inside it.
	 * @param tempUserData
	 * @param fileNm 
	 * @return
	 * @throws DatabaseException 
	 */
	private String persistData(List<SRTRosterVO> tempUserData, String fileNm) throws DatabaseException {
		String tempTableNm = buildTempTable(fileNm);

		populateTempTable(tempUserData, tempTableNm);
		return tempTableNm;
	}

	/**
	 * Creates the temp schema in case it doesn't exist as well as the temp table.
	 * @param fileNm
	 */
	private String buildTempTable(String fileNm) {
		StringBuilder sql = new StringBuilder(100).append("CREATE SCHEMA IF NOT EXISTS ").append(TEMP_SCHEMA).append(";");
		String tableNm = fileNm.substring(0, fileNm.lastIndexOf('.'));
		new DBProcessor(dbConn).executeSQLCommand(sql.toString());
		new DBProcessor(dbConn, TEMP_SCHEMA).executeSQLCommand(buildTempTableSql(tableNm));
		return tableNm;
	}

	/**
	 * Generates Temp table creation Statement
	 * @param fileNm
	 * @return
	 */
	private String buildTempTableSql(String tableNm) {
		StringBuilder sql = new StringBuilder(1000);
		sql.append("create table if not exists ").append(TEMP_SCHEMA).append(".");
		sql.append(TEMP_TABLE_PREFIX).append(tableNm);
		sql.append("(first_nm varchar(60), ");
		sql.append("last_nm varchar(80), ");
		sql.append("email_address_txt varchar(250), ");
		sql.append("phone_number_txt varchar(60), ");
		sql.append("mobile_number_txt varchar(60), ");
		sql.append("address varchar(160), ");
		sql.append("address_2 varchar(160), ");
		sql.append("city_nm varchar(80), ");
		sql.append("state_cd varchar(5), ");
		sql.append("zip_cd varchar(10), ");
		sql.append("country_cd varchar(2), ");
		sql.append("account_no varchar(32), ");
		sql.append("workgroup_id varchar(32), ");
		sql.append("wwid varchar(32), ");
		sql.append("territory_id varchar(32), ");
		sql.append("area varchar(50), ");
		sql.append("region varchar(50), ");
		sql.append("ENGINEERING_CONTACT varchar(32), ");
		sql.append("is_admin integer, ");
		sql.append("IS_ACTIVE integer, ");
		sql.append("create_dt timestamp);");
		return sql.toString();
	}

	/**
	 * Inserting data from tempUserData into the Temp Table.
	 * @param tempUserData
	 * @param tempTableNm
	 * @throws DatabaseException 
	 */
	private void populateTempTable(List<SRTRosterVO> tempUserData, String tempTableNm) throws DatabaseException {
		Map<String, List<Object>> psValues = new HashMap<>();
		for(SRTRosterVO r : tempUserData) {
			if(!StringUtil.isEmpty(r.getEmailAddress())) {
				List<Object> data = new ArrayList<>();
				data.add(r.getFirstName());
				data.add(r.getLastName());
				data.add(r.getEmailAddress());
				data.add(r.getMainPhone());
				data.add(r.getMobilePhone());
				data.add(r.getAddress());
				data.add(r.getAddress2());
				data.add(r.getCity());
				data.add(r.getState());
				data.add(r.getZipCode());
				data.add(r.getCountryCode());
				data.add(r.getAccountNo());
				//data.add(r.getWorkgroupId());
				data.add(SRTUserImportModule.SALES_ROSTER_ID);
				data.add(r.getWwid());
				data.add(r.getTerritoryId());
				data.add(r.getArea());
				data.add(r.getRegion());
				data.add(r.getEngineeringContact());
				data.add(0);
				data.add(1);
				data.add(Convert.getCurrentTimestamp());
				psValues.put(r.getEmailAddress(), data);
			}
		}
		new DBProcessor(dbConn, TEMP_SCHEMA).executeBatch(populateTempTableSql(tempTableNm), psValues);
	}

	/**
	 * Build the SQL for populating the Temp Table.
	 * @return
	 */
	private String populateTempTableSql(String tempTableNm) {
		StringBuilder sql = new StringBuilder(1000);
		sql.append(DBUtil.INSERT_CLAUSE).append(TEMP_SCHEMA).append(".");
		sql.append(TEMP_TABLE_PREFIX).append(tempTableNm);
		sql.append("(first_nm, last_nm, email_address_txt, ");
		sql.append("phone_number_txt, mobile_number_txt, ");
		sql.append("address, address_2, city_nm, state_cd, zip_cd, country_cd, ");
		sql.append("account_no, workgroup_id, wwid, territory_id, area, ");
		sql.append("region, ENGINEERING_CONTACT, is_admin, is_active, create_dt) ");
		sql.append("values (");
		DBUtil.preparedStatmentQuestion(21, sql);
		sql.append(")");

		return sql.toString();
	}

	/**
	 * Send Workflow Request for processing an SRT User Upload.
	 * @param tempTableNm
	 * @param string 
	 */
	private void buildWorkflowRequest(String tempTableNm, String fileNm, String opCoId) {
		Map<String, Object> params = new HashMap<>();
		params.put(SRTUserImportModule.TEMP_TABLE_NM, tempTableNm);
		params.put(SRTUserImportModule.FILE_NM, fileNm);
		params.put(SRTUtil.OP_CO_ID, opCoId);
		WorkflowMessageVO wmv = new WorkflowMessageVO(new WorkflowLookupUtil(dbConn).lookupWorkflowId(USER_UPLOAD, USER_IMPORT));
		wmv.setParameters(params);

		//Queue Workflow Message for SRT Report.
		WorkflowSender wfs = new WorkflowSender(attributes);
		wfs.sendWorkflow(wmv);
	}
}