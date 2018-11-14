package com.biomed.smarttrak.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> BiomedUserExpiryUtil.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Utility that runs nightly to move expired users to an
 * inactive state.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 6, 2018
 ****************************************************************************/
public class BiomedUserExpiryUtil extends CommandLineUtil {

	/**
	 * @param args
	 */
	public BiomedUserExpiryUtil(String[] args) {
		super(args);
		loadProperties("scripts/bmg_smarttrak/user-expiry.properties");
		loadDBConnection(props);
	}

	public static void main(String [] args) {
		BiomedUserExpiryUtil util = new BiomedUserExpiryUtil(args);
		util.run();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {

		log.info("Loading Users");
		List<String> userIds = loadActiveExpiredUsers();
		log.info(String.format("Loaded %d Users", userIds.size()));
		if(!userIds.isEmpty()) {
			updateExpiredUser(userIds);
		} else {
			log.info("No Users to Deactivate.  Terminating.");
		}
	}

	/**
	 * Load Expired User Ids.
	 * @return
	 */
	private List<String> loadActiveExpiredUsers() {
		List<String> userIds = new ArrayList<>();

		try(PreparedStatement ps = dbConn.prepareStatement(getUserSql())) {
			ps.setInt(1, 0);
			ps.setString(2, "T");
			ResultSet rs = ps.executeQuery();

			while(rs.next()) {
				userIds.add(rs.getString("user_id"));
			}
		} catch (SQLException e) {
			log.error("Error Processing Code", e);
		}
		return userIds;
	}

	/**
	 * Sql to load Expired Users user_ids
	 * @return
	 */
	private String getUserSql() {
		StringBuilder sql = new StringBuilder(200);
		sql.append(DBUtil.SELECT_CLAUSE).append("user_id ");
		sql.append(DBUtil.FROM_CLAUSE).append(props.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_user ").append(DBUtil.WHERE_CLAUSE);
		sql.append("expiration_dt < current_date and active_flg != ? ");
		sql.append("and status_cd = ?");

		log.debug(sql.toString());
		return sql.toString();
	}

	/**
	 * Update User record and set inactive where user Id in passed userIds.
	 * @param userIds 
	 * 
	 */
	private void updateExpiredUser(List<String> userIds) {
		try(PreparedStatement ps = dbConn.prepareStatement(getUpdateSql(userIds.size()))) {
			int i = 1;
			ps.setInt(i++, 0);
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			for(String userId: userIds) {
				ps.setString(i++, userId);
			}
			ps.executeUpdate();

			log.info(String.format("Deactivated %d Users", userIds.size()));
		} catch (SQLException e) {
			log.error("Error Processing Code", e);
		}
	}

	/**
	 * Sql to update Expired Users Active Flag.
	 * @return
	 */
	private String getUpdateSql(int userCnt) {
		StringBuilder sql = new StringBuilder(250);
		sql.append(DBUtil.UPDATE_CLAUSE).append(props.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_user set active_flg = ?, update_dt = ? ");
		sql.append(DBUtil.WHERE_CLAUSE).append("user_id in (");
		DBUtil.preparedStatmentQuestion(userCnt, sql);
		sql.append(")");

		log.debug(sql.toString());
		return sql.toString();
	}

}
