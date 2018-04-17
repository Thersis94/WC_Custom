package com.depuysynthes.action;

// Java 8
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

// Apache Log4j
import org.apache.log4j.Logger;

// SMTBaseLibs
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.http.filter.fileupload.Constants;
import com.siliconmtn.util.Convert;

/*****************************************************************************
 <b>Title: </b>PatentActivityAction.java
 <b>Project: </b> WebCrescendo
 <b>Description: </b> Logs patent activity.
 <b>Copyright: </b>(c) 2000 - 2018 SMT, All Rights Reserved
 <b>Company: Silicon Mountain Technologies</b>
 @author David Bargerhuff
 @version 1.0
 @since Apr 2, 2018
 <b>Changes:</b> 
 ***************************************************************************/
public class PatentActivityAction {

	private static Logger log = Logger.getLogger(PatentActivityAction.class);
	private SMTDBConnection dbConn;
	private Map<String,Object> attributes;
	
	public enum ActivityType {
		LIST(-1), // LIST is for retrieval only.
		ADD(2),
		UPDATE(3);
		
		private int typeId;
		private ActivityType(int typeId) {
			this.typeId = typeId;
		}
		
		public int getTypeId() {
			return typeId;
		}
	}

	/**
	* Constructor
	*/
	public PatentActivityAction(Map<String,Object> attributes, SMTDBConnection dbConn) {
		this.attributes = attributes;
		this.dbConn = dbConn;
	}
	
	/**
	 * Logs an activity record for the given patent ID, activity type, and profile ID.
	 * @param patentId
	 * @param activityType
	 * @param profileId
	 */
	public void logActivity(int patentId, ActivityType activityType, String profileId) {
		// log patent ID, activity type id
		String errMsg = "Error logging patent activity for patent ID | profile ID | activity type: ";
		int idx = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(buildInsertQuery())) {
			ps.setInt(++idx, patentId);
			ps.setInt(++idx, activityType.getTypeId());
			ps.setString(++idx, profileId);
			ps.setTimestamp(++idx, Convert.getCurrentTimestamp());
			int val = ps.executeUpdate();
			if (val == 0) 
				throw new SQLException("No records inserted.");
		} catch (Exception e) {
			log.error(errMsg + patentId + "|" + profileId + "|" + activityType.name() + ", " + e);
		}

	}
	/**
	 * Builds the SQL insert query for logging patent activity.
	 * @return
	 */
	private String buildInsertQuery() {
		StringBuilder sql = new StringBuilder(200);
		sql.append("insert into ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("dpy_syn_patent_activity (patent_id, activity_type_id, profile_id, activity_dt) ");
		sql.append("values (?,?,?,?)");
		log.debug("logActivity SQL: " + sql);
		return sql.toString();
	}

}
