package com.biomed.smarttrak.security;

// Java 7
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

// Log4j
import org.apache.log4j.Logger;

// WC Custom libs
import com.biomed.smarttrak.vo.SmartTRAKUserVO;

// SMTBaseLibs
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.security.AuthenticationException;

// WebCrescendo libs
import com.smt.sitebuilder.common.constants.Constants;


/*****************************************************************************
 <p><b>Title</b>: SmartTRAKUserAction.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Feb 1, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class SmartTRAKUserAction {

	private Logger log = Logger.getLogger(SmartTRAKUserAction.class);
	private Connection dbConn;
	private Map<String,Object> attributes;
	
	/**
	* Constructor
	*/
	public SmartTRAKUserAction() {
	}
	
	/**
	 * Retrieves SmartTRAK user data from custom table(s).
	 * @param profileId
	 * @return
	 * @throws AuthenticationException
	 */
	public SmartTRAKUserVO retrieveUserData(String profileId) 
			throws AuthenticationException {
		StringBuilder sql = new StringBuilder(100);
		sql.append("select user_id, profile_id from ");
		sql.append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_user where profile_id = ?");
		log.debug("SmartTRAK user SQL: " + sql.toString());
		
		SmartTRAKUserVO user;
		StringBuilder errMsg = new StringBuilder(75);
		errMsg.append("Error retrieving SmartTRAK user Id: ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, profileId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				user = new SmartTRAKUserVO();
				user.setUserId(rs.getString("user_id"));
			} else {
				throw new InvalidDataException("SmartTRAK user does not exist.");
			}
		} catch (SQLException sqle) {
			errMsg.append(sqle.getMessage());
			throw new AuthenticationException(errMsg.toString());
			
		} catch (InvalidDataException ide) {
			errMsg.append(ide.getMessage());
			throw new AuthenticationException(errMsg.toString());
			
		}
		
		return user;
	}

	/**
	 * @param dbConn the dbConn to set
	 */
	public void setDBConnection(Connection dbConn) {
		this.dbConn = dbConn;
	}

	/**
	 * @param attributes the attributes to set
	 */
	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}
	
}
