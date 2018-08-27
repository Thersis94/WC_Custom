package com.depuysynthes.action;

// Java 8
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Log4j
import org.apache.log4j.Logger;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WebCrescendo libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.admin.action.OrganizationAction;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <b>Title: </b>PatentManagementAction.java
 <b>Project: </b>
 <b>Description: </b>
 <b>Copyright: </b>(c) 2000 - 2018 SMT, All Rights Reserved
 <b>Company: Silicon Mountain Technologies</b>
 @author David Bargerhuff
 @version 1.0
 @since Apr 2, 2018
 <b>Changes:</b> 
 ***************************************************************************/
public class PatentManagementAction extends SBActionAdapter {

	private static Logger log = Logger.getLogger(PatentManagementAction.class.getName());
	static final String PARAM_IMPORT_FILE = "importFile";
	static final String PATENT_TABLE = "dpy_syn_patent";
	private static final String PARAM_SEARCH_BARCODE = "searchBarcode";
	private static final String PARAM_SEARCH_COMPANY = "searchCompany";
	private static final String PARAM_SEARCH_DESC = "searchDescription";
	private static final String PARAM_ACTIVITY_TYPE= "activityType";

	public enum ActivityType {
		ADD,
		LIST,
		UPDATE;
	}

	/**
	* Constructor
	*/
	public PatentManagementAction() {
		// empty
	}

	/**
	* Constructor
	*/
	public PatentManagementAction(ActionInitVO actionInit) {
		super(actionInit);
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		// not implemented
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		// not implemented
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		// delete unused
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		String patentId = StringUtil.checkVal(req.getParameter(PatentAction.PATENT_ID),null);

		// Retrieve patent data if there is a patentId or if this is a search.
		List<PatentVO> patents = new ArrayList<>();
		if (patentId != null || isSearch(req)) {
			patents = retrievePatentData(req);
		}

		putModuleData(patents, patents.size(),true);
	}


	/**
	 * Helper method for determining if this is a search request
	 * @param req
	 * @return
	 */
	private boolean isSearch(ActionRequest req) {
		return (req.hasParameter(PARAM_SEARCH_COMPANY) ||
				req.hasParameter(PARAM_SEARCH_DESC) ||
				req.hasParameter(PARAM_SEARCH_BARCODE));
	}


	/**
	 * Retrieves patent record data.
	 * @param req
	 * @return
	 */
	private List<PatentVO> retrievePatentData(ActionRequest req) {
		List<PatentVO> patents = new ArrayList<>();
		int idx = 0;
		String sql = buildQuery(req, ActivityType.LIST);
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {

			ps.setString(++idx, req.getParameter("organizationId"));
			if (req.hasParameter(PatentAction.PATENT_ID)) 
				ps.setInt(++idx, Convert.formatInteger(req.getParameter(PatentAction.PATENT_ID)));
			if (req.hasParameter(PARAM_SEARCH_BARCODE))
				ps.setString(++idx, "%"+req.getParameter(PARAM_SEARCH_BARCODE)+"%");
			if (req.hasParameter(PARAM_SEARCH_COMPANY)) 
				ps.setString(++idx, "%"+req.getParameter(PARAM_SEARCH_COMPANY).toUpperCase()+"%");
			if (req.hasParameter(PARAM_SEARCH_DESC)) 
				ps.setString(++idx, "%"+req.getParameter(PARAM_SEARCH_DESC).toUpperCase()+"%");

			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				patents.add(new PatentVO(rs));
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving patent record(s), ", sqle);
		}
		return patents;
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
		//save the Excel file if one was uploaded
		if (req.getFile(PARAM_IMPORT_FILE) != null) {
			processImportFile(req);
		} else {
			managePatents(req);
		}
	}

	/**
	 * Manage patent update
	 * @param req
	 */
	private void managePatents(ActionRequest req) {
		String errMsg = "Patent data updated successfully.";

		// populate bean data from request
		PatentVO pvo = new PatentVO();
		pvo.populateData(req);
		pvo.setUpdateById(req.getParameter("profileId"));

		// determine activity type
		ActivityType activityType = 
				parseActivityType(StringUtil.checkVal(req.getParameter(PARAM_ACTIVITY_TYPE)));

		try {
			dbConn.setAutoCommit(false);
			if (activityType.equals(ActivityType.ADD)) {
				// insert new patent record
				writePatentData(req,pvo,activityType);

			} else if (activityType.equals(ActivityType.UPDATE)) {
				// log history first to capture existing record
				logHistory(pvo);
				// update existing record
				writePatentData(req,pvo,activityType);

			}
			dbConn.commit();
		} catch (Exception e) {
			// already logged it, capture err msg for redirect.
			errMsg = e.getMessage();
			try {
				dbConn.rollback();
			} catch (Exception e1) {
				log.error("Error rolling back patent record transaction, ", e1);
			}
		} finally {
			try {
				dbConn.setAutoCommit(true);
			} catch (Exception e2) {
				log.error("Error resetting db connection autocommit, ", e2);
			}
		}

		// redirect
		sbUtil.adminRedirect(req, errMsg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
	}

	/**
	 * Writes the patent record data (either insert or update). 
	 * @param req
	 * @param pvo
	 * @param activityType
	 * @throws ActionException
	 */
	private void writePatentData(ActionRequest req, PatentVO pvo, 
			ActivityType activityType) throws ActionException {
		int idx = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(buildQuery(req, activityType))) {
			ps.setString(++idx, pvo.getActionId());
			ps.setString(++idx, pvo.getOrganizationId());
			ps.setString(++idx, pvo.getCompany());
			ps.setString(++idx, pvo.getCode());
			ps.setString(++idx, pvo.getItem());
			ps.setString(++idx, pvo.getDesc());
			ps.setString(++idx, pvo.getPatents());
			ps.setString(++idx, pvo.getRedirectName());
			ps.setString(++idx, pvo.getRedirectAddress());
			ps.setInt(++idx, pvo.getStatusFlag());
			ps.setString(++idx, pvo.getUpdateById());
			ps.setTimestamp(++idx, Convert.getCurrentTimestamp());

			ResultSet rs = null;
			if (activityType.equals(ActivityType.ADD)) {
				rs = ps.executeQuery();
				if (rs.next())	
					pvo.setPatentId(rs.getInt(1));
			} else {
				ps.setInt(++idx, pvo.getPatentId());
				ps.executeUpdate();
			}
		} catch (SQLException sqle) {
			String errMsg = "Error writing patent record.";
			log.error(errMsg, sqle);
			throw new ActionException(errMsg);
		}
	}


	/**
	 * Safely parses the ActivityType from the provided type argument.
	 * @param type
	 * @return
	 */
	private ActivityType parseActivityType(String type) {
		try {
			return ActivityType.valueOf(type);
		} catch (Exception e) {
			return ActivityType.LIST;
		}
	}


	/**
	 * Builds a SQL query based on the activity type argument.
	 * @param activityType
	 * @return
	 */
	private String buildQuery(ActionRequest req, ActivityType activityType) {
		StringBuilder table = new StringBuilder(30);
		table.append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		table.append(PATENT_TABLE).append(" ");

		StringBuilder sql = new StringBuilder(200);

		if (activityType.equals(ActivityType.ADD)) { 
			sql.append("insert into ").append(table);
			sql.append("(action_id, organization_id, company_nm, ");
			sql.append("code_txt, item_txt, desc_txt, patents_txt, redirect_nm, ");
			sql.append("redirect_address_txt, status_flg, profile_id, create_dt) ");
			sql.append("values (?,?,?,?,?,?,?,?,?,?,?,?) ");
			// add clause to return the sequence ID genererated for this insert.
			sql.append("returning patent_id");
		} else if (activityType.equals(ActivityType.UPDATE)) {
			sql.append("update ").append(table);
			sql.append("set action_id = ?, organization_id = ?, company_nm = ?, ");
			sql.append("code_txt = ?, item_txt = ?, desc_txt = ?, patents_txt = ?, ");
			sql.append("redirect_nm = ?, redirect_address_txt = ?, status_flg = ?, ");
			sql.append("profile_id = ?, update_dt = ? where patent_id = ?");
		} else {
			// default retrieve
			sql.append("select * from ").append(table);
			sql.append("where organization_id = ? ");
			if (req.hasParameter(PatentAction.PATENT_ID))
				sql.append("and patent_id = ? ");
			if (req.hasParameter(PARAM_SEARCH_BARCODE))
				sql.append("and code_txt like ? ");
			if (req.hasParameter(PARAM_SEARCH_COMPANY))
				sql.append("and upper(company_nm) like ? ");
			if (req.hasParameter(PARAM_SEARCH_DESC))
				sql.append("and upper(desc_txt) like ? ");
			sql.append("order by code_txt ");
		}

		log.debug("patent record SQL: " + sql);
		return sql.toString();
	}


	/**
	 * Logs an activity record for the patent management operation.
	 * @param pvo
	 * @throws ActionException
	 */
	private void logHistory(PatentVO pvo) throws ActionException {
		PatentHistoryManager paa = new PatentHistoryManager(attributes,dbConn);
		paa.writePatentHistory(pvo);
	}
	
	/**
	 * Processes patent import file submitted via data tool
	 * @param req
	 */
	private void processImportFile(ActionRequest req) {
		/* Delegate the import file processing to the import utility */
		PatentImportUtility util = new PatentImportUtility();
		util.setDbConn(dbConn);
		util.setCustomDbSchema((String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		util.setFilePartDataBean(req.getFile(PARAM_IMPORT_FILE));
		util.setActionId(req.getParameter(SBActionAdapter.ACTION_ID));
		util.setOrganizationId(req.getParameter(OrganizationAction.ORGANIZATION_ID));

		// get user profile
		UserDataVO admin = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
		util.setImportProfileId(admin.getProfileId());

		// import patents
		util.importPatents();
		
		//return some stats to the administrator
		super.adminRedirect(req, util.getResultMessage(), buildRedirect(util.getPatentCount()));

	}


	/**
	 * Append extra parameters to the redirect url so we can 
	 * display import count
	 * @param req
	 * @return
	 */
	private String buildRedirect(int importCnt) {
		StringBuilder redirect = new StringBuilder(150);
		redirect.append(getAttribute(AdminConstants.ADMIN_TOOL_PATH));
		redirect.append("?importCnt=").append(importCnt);
		return redirect.toString();
	}
	
}
