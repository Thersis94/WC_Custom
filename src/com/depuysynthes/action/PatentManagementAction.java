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
import com.depuysynthes.action.PatentActivityAction.ActivityType;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WebCrescendo libs
import com.smt.sitebuilder.action.SBActionAdapter;
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
	private static final String PATENT_TABLE = "DPY_SYN_PATENT";
	private static final String PARAM_SEARCH_VAL = "searchVal";
	private static final String PARAM_ACTIVITY_TYPE= "activityType";
	
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
		log.debug("list...");
		String patentId = StringUtil.checkVal(req.getParameter(PatentAction.PATENT_ID),null);
		List<PatentVO> patents = new ArrayList<>();

		// Return empty data is no patent ID specified and this is not a search query.
		if (patentId == null && ! req.hasParameter(PARAM_SEARCH_VAL)) {
			putModuleData(patents);
			return;
		}

		patents = retrievePatentData(req);
		putModuleData(patents, patents.size(),true);
	}
	
	/**
	 * 
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
			if (req.hasParameter(PARAM_SEARCH_VAL))
				ps.setString(++idx, "%"+req.getParameter(PARAM_SEARCH_VAL)+"%");

			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				log.debug("patents text: " + rs.getString("patents_txt"));
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
		// update
		log.debug("update...");
		String errMsg = "Patent record added/updated successfully.";
		// populate bean data from request
		PatentVO pvo = new PatentVO();
		pvo.populateData(req);
		pvo.setUpdateById(req.getParameter("profileId"));

		// determine activity type
		ActivityType activityType = 
				parseActivityType(StringUtil.checkVal(req.getParameter(PARAM_ACTIVITY_TYPE)));

		log.debug("activityType: " + activityType);
		int idx = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(buildQuery(req, activityType))) {
			if (activityType.equals(ActivityType.ACTIVE) || 
					activityType.equals(ActivityType.INACTIVE)) {
				ps.setInt(++idx, pvo.getStatusFlag());
				ps.setTimestamp(++idx, Convert.getCurrentTimestamp());
			} else {
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
				ps.setTimestamp(++idx, Convert.getCurrentTimestamp());
			}

			ResultSet rs = null;
			if (activityType.equals(ActivityType.ADD)) {
				rs = ps.executeQuery();
				if (rs.next())	
					pvo.setPatentId(rs.getInt(1));
				log.debug("added patent ID: " + pvo.getPatentId());
			} else {
				ps.setInt(++idx, pvo.getPatentId());
				ps.executeUpdate();
			}

		} catch (SQLException sqle) {
			errMsg = "Error adding/updating patent.";
			log.error(errMsg, sqle);
		}

		// log the activity
		try {
			logActivity(pvo.getPatentId(), activityType, pvo.getUpdateById());
		} catch (Exception e) {
			// set errMsg, already logged in underlying action
			errMsg = e.getMessage();
		}

		// redirect
		sbUtil.adminRedirect(req, errMsg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
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
		StringBuilder sql = new StringBuilder(200);
		StringBuilder table = new StringBuilder(30);
		table.append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		table.append(PATENT_TABLE).append(" ");

		switch(activityType) {
			case ADD:
				sql.append("insert into ").append(table);
				sql.append("(action_id, organization_id, company_nm, ");
				sql.append("code_txt, item_txt, desc_txt, patents_txt, redirect_nm, ");
				sql.append("redirect_address_txt, status_flg, create_dt) ");
				sql.append("values (?,?,?,?,?,?,?,?,?,?,?) ");
				// add clause to return the sequence ID genererated for this insert.
				sql.append("returning patent_id");
				break;
			case UPDATE:
				sql.append("update ").append(table);
				sql.append("set action_id = ?, organization_id = ?, company_nm = ?, ");
				sql.append("code_txt = ?, item_txt = ?, desc_txt = ?, patents_txt = ?, ");
				sql.append("redirect_nm = ?, redirect_address_txt = ?, status_flg = ?, ");
				sql.append("update_dt = ? where patent_id = ?");
				break;
			case INACTIVE:
			case ACTIVE:
				sql.append("update ").append(table);
				sql.append("set status_flg = ?, update_dt = ? ");
				sql.append("where patent_id = ?");
				break;
			default: // LIST
				sql.append("select * from ").append(table);
				sql.append("where organization_id = ? ");
				if (req.hasParameter(PatentAction.PATENT_ID))
					sql.append("and patent_id = ? ");
				if (req.hasParameter(PARAM_SEARCH_VAL))
					sql.append("and code_txt like ? ");
				sql.append("order by code_txt ");
				break;
		}

		log.debug("patent SQL: " + sql);
		return sql.toString();
	}


	/**
	 * Logs an activity record for the patent management operation.
	 * @param patentId
	 * @param activityType
	 * @param profileId
	 */
	private void logActivity(int patentId, ActivityType activityType, String profileId) {
		PatentActivityAction paa = new PatentActivityAction(attributes,dbConn);
		paa.logActivity(patentId, activityType, profileId);
	}
	
}
