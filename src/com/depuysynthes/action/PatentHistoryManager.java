package com.depuysynthes.action;

// Java 8
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

// Apache Log4j
import org.apache.log4j.Logger;

//SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.http.filter.fileupload.Constants;

/*****************************************************************************
 <b>Title: </b>PatentHistoryAction.java
 <b>Project: </b> WebCrescendo
 <b>Description: </b> Saves a copy of a 'live' patent record before updates to the
 record are saved.
 <b>Copyright: </b>(c) 2000 - 2018 SMT, All Rights Reserved
 <b>Company: Silicon Mountain Technologies</b>
 @author David Bargerhuff
 @version 1.0
 @since Apr 2, 2018
 <b>Changes:</b>
 ***************************************************************************/
public class PatentHistoryManager {

	private static Logger log = Logger.getLogger(PatentHistoryManager.class);
	static final String PATENT_HISTORY_TABLE = "dpy_syn_patent_history";
	private SMTDBConnection dbConn;
	private Map<String,Object> attributes;

	/**
	* Constructor
	*/
	public PatentHistoryManager(Map<String,Object> attributes, SMTDBConnection dbConn) {
		this.attributes = attributes;
		this.dbConn = dbConn;
	}

	/**
	 * Logs an activity record for the given patent ID, activity type, and profile ID.
	 * @param patentId
	 * @param activityType
	 * @param profileId
	 * @throws ActionException
	 */
	public void writePatentHistory(PatentVO pvo) throws ActionException {
		String errMsg = "Error writing historical record for patent ID | profile ID: ";
		int idx = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(buildQuery())) {
			ps.setInt(++idx, pvo.getPatentId());
			int val = ps.executeUpdate();
			if (val == 0)
				throw new SQLException("No patent history record inserted.");
		} catch (SQLException sqle) {
			log.error(errMsg + pvo.getPatentId() + "|" + pvo.getUpdateById() + ", " + sqle.getMessage());
			throw new ActionException("Patent management failed.  Error saving patent history.");
		}
	}


	/**
	 * Builds the SQL insert query for logging patent history.  This query inserts a copy of the
	 * current 'live' patent record into the history table before the 'live' record is updated.
	 * @return
	 */
	private String buildQuery() {
		String schema = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);

		StringBuilder sql = new StringBuilder(200);
		sql.append("insert into ").append(schema).append(PATENT_HISTORY_TABLE).append(" ");
		sql.append("(patent_id,action_id,organization_id,company_nm,code_txt, ");
		sql.append("item_txt,desc_txt,patents_txt,redirect_nm,redirect_address_txt, ");
		sql.append("status_flg,profile_id,activity_dt) ");
		sql.append("select patent_id, action_id, organization_id, company_nm, code_txt, item_txt, ");
		sql.append("desc_txt, patents_txt, redirect_nm, redirect_address_txt, status_flg, ");
		sql.append("profile_id, coalesce(update_dt, create_dt) ");
		sql.append("from ");
		sql.append(schema).append(PatentManagementAction.PATENT_TABLE).append(" ");
		sql.append("where patent_id = ?");
		log.debug("writePatentHistory SQL: " + sql);
		return sql.toString();
	}

}
