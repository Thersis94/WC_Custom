package com.depuysynthes.srt.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.depuysynthes.srt.SRTRosterAction;
import com.depuysynthes.srt.SRTRosterImportAction;
import com.depuysynthes.srt.util.SRTRosterImporter;
import com.depuysynthes.srt.util.SRTUtil;
import com.depuysynthes.srt.vo.SRTRosterVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.workflow.data.WorkflowModuleVO;
import com.siliconmtn.workflow.modules.AbstractWorkflowModule;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.action.user.SBProfileManager;
import com.smt.sitebuilder.admin.action.OrganizationAction;
import com.smt.sitebuilder.admin.action.SiteAction;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.WCConfigUtil;

/****************************************************************************
 * <b>Title:</b> SRTUserImportModule.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages Importing Users for SRT from Excel File.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Apr 23, 2018
 ****************************************************************************/
public class SRTUserImportModule extends AbstractWorkflowModule {

	public static final String SALES_ROSTER_ID = "8";
	public static final String TEMP_TABLE_NM = "tempTableNm";
	public static final String FILE_NM = "fileNm";

	/**
	 * @param mod
	 * @param conn
	 * @param customSchema
	 * @throws Exception
	 */
	public SRTUserImportModule(WorkflowModuleVO mod, Connection conn, String customSchema) throws Exception {
		super(mod, conn, customSchema);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.workflow.modules.AbstractWorkflowModule#run()
	 */
	@Override
	protected void run() throws Exception {
		String tempTableName = (String) mod.getModuleConfig(TEMP_TABLE_NM).getValue();
		String fileNm = (String) mod.getModuleConfig(FILE_NM).getValue();
		String opCoId = (String) mod.getModuleConfig(SRTUtil.OP_CO_ID).getValue();

		//Load Temp Data from tempUserData Table
		Map<String, SRTRosterVO> tempData = loadNewUserData(tempTableName, opCoId);
		log.debug("TempUsers count: "+ tempData.size());

		//Map of ProfileIds and Emails in the system.
		Map<String, String> existingUsers = loadExistingData(opCoId);
		log.debug("Existing Users count: "+ existingUsers.size());

		//Get profileIds of the deactivatedUsers
		List<String> deactivations = getDeactivations(tempData, existingUsers);
		log.debug("Deactivated Users count: "+ deactivations.size());
		log.debug("New/Update Users count: "+ tempData.size());
		log.debug("Finished loading Data");
		
		//Deactivate Current Sales Rosters not in tempDatad
		deactivateSalesRosters(deactivations, opCoId);

		//Activate New Users
		updateUsers(tempData.values(), opCoId);

		//DropTempTable
		dropTempTable(tempTableName);

		//Remove File
		removeFile(fileNm);
	}

	/**
	 * Drops the Temp table once we're done with it.
	 * @param tempTableName
	 * @throws SQLException 
	 */
	private void dropTempTable(String tempTableName) throws SQLException {
		StringBuilder sql = new StringBuilder(100);
		sql.append("drop table if exists ").append(SRTRosterImportAction.TEMP_SCHEMA).append(".");
		sql.append(SRTRosterImportAction.TEMP_TABLE_PREFIX).append(tempTableName);

		new DBProcessor(getConnection(), SRTRosterImportAction.TEMP_SCHEMA).executeSQLCommand(sql.toString());
	}

	/**
	 * Remove the File from the FileSystem.
	 * @param fileNm
	 * @throws IOException 
	 */
	private void removeFile(String fileNm) throws IOException {
		Files.deleteIfExists(Paths.get(fileNm));
		this.buildStepNote(StringUtil.join("Successfully Deleted File: ",fileNm));
	}

	/**
	 * Call the SRTUserImport System with the remaining SRTRosterVOs
	 * @param opCoId 
	 * @param tempData
	 * @throws Exception 
	 */
	private void updateUsers(Collection<SRTRosterVO> newRosters, String opCoId) throws Exception {

		//Fast Fail if no new Users.
		if(newRosters == null || newRosters.isEmpty()) {
			return;
		}

		//Clone Attributes Map
		Map<String, Object> config = new HashMap<>();
		config.putAll(attributes);

		//Load all SiteConfig from DB
		config.putAll(WCConfigUtil.getSiteConfig(getConnection(), SRTUtil.PUBLIC_SITE_ID));

		//Load Config from Constants.
		config.put(OrganizationAction.ORGANIZATION_ID, SRTUtil.SRT_ORG_ID);
		config.put(SiteAction.SITE_ID, SRTUtil.PUBLIC_SITE_ID);
		config.put("registerActionId", SRTUtil.REGISTRATION_GRP_ID);
		config.put(SRTUtil.OP_CO_ID, opCoId);

		//Create new Importer with updated attributes map.
		SRTRosterImporter importer = new SRTRosterImporter(getConnection(), config);

		//Perform Import with newRosters.
		importer.importUsers(new ArrayList<>(newRosters));
	}

	/**
	 * Deactivates all roster records
	 * @param deactivations
	 * @throws SQLException 
	 */
	private void deactivateSalesRosters(List<String> deactivations, String opCoId) throws SQLException {
		//Fast Fail if nothing to do.
		if(StringUtil.isEmpty(opCoId) || deactivations == null || deactivations.isEmpty()) {
			return;
		}

		try(PreparedStatement ps = getConnection().prepareStatement(getDeactivationSql(deactivations.size()))) {
			int i = 1;
			ps.setInt(i++, 0);
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			ps.setString(i++, opCoId);
			for(String s : deactivations) {
				ps.setString(i++, s);
			}

			int res = ps.executeUpdate();
			log.debug("Deactivated " + res + " users");
		}

		SRTRosterAction sra = new SRTRosterAction();
		sra.setDBConnection(this.getConnection());
		sra.setAttributes(attributes);
		try {
			List<SRTRosterVO> rosters = convertDeactivations(deactivations);
			for(SRTRosterVO roster: rosters) {
				sra.saveProfileRole(SRTUtil.PUBLIC_SITE_ID, roster, true);
			}
		} catch (ActionException | DatabaseException e) {
			log.error("Error Deactivating Users", e);
		}
	}

	/**
	 * Load Profiles for Deactivated Users.
	 * @param deactivations
	 * @return
	 * @throws SQLException 
	 * @throws DatabaseException 
	 */
	private List<SRTRosterVO> convertDeactivations(List<String> deactivations) throws DatabaseException, SQLException {
		SBProfileManager pm = (SBProfileManager) ProfileManagerFactory.getInstance(attributes);
		List<UserDataVO> users = pm.searchProfile(getConnection(), deactivations);
		List<SRTRosterVO> rosters = new ArrayList<>();
		for(UserDataVO u : users) {
			SRTRosterVO r = new SRTRosterVO();
			r.setData(u.getDataMap());
			rosters.add(r);
		}
		return rosters;
	}

	/**
	 * Builds sql for deactivating Sales Roster's linked to the given profileIds
	 * @param profileIdCount
	 * @return
	 */
	private String getDeactivationSql(int profileIdCount) {
		StringBuilder sql = new StringBuilder(200 + profileIdCount * 3);
		sql.append(DBUtil.UPDATE_CLAUSE).append(schema).append("DPY_SYN_SRT_ROSTER ");
		sql.append("set is_active = ?, deactivated_dt = ?, wwid = null where op_co_id = ? and profile_id in (");
		DBUtil.preparedStatmentQuestion(profileIdCount, sql);
		sql.append(")");
		return sql.toString();
	}

	/**
	 * Filters out list of profileIds for deactivation.
	 * @param tempData
	 * @param existingUsers
	 * @return
	 */
	private List<String> getDeactivations(Map<String, SRTRosterVO> tempData, Map<String, String> existingUsers) {
		List<String> deactivations = new ArrayList<>();
		for(Entry<String, String> e : existingUsers.entrySet()) {
			SRTRosterVO roster = tempData.get(e.getKey());
			if(roster == null) {
				deactivations.add(e.getValue());
			}
		}
		return deactivations;
	}

	/**
	 * Load Existing Data into a map of <profileId, emailAddressTxt>
	 * @return
	 * @throws SQLException 
	 * @throws EncryptionException 
	 */
	private Map<String, String> loadExistingData(String opCoId) throws SQLException, EncryptionException {
		Map<String, String> profiles = new HashMap<>();
		try(PreparedStatement ps = getConnection().prepareStatement(loadExistingDataSql())) {
			ps.setString(1, opCoId);
			ps.setString(2, SALES_ROSTER_ID);
			ps.setInt(3, 1);

			ResultSet rs = ps.executeQuery();
			StringEncrypter se = new StringEncrypter((String)attributes.get(Constants.ENCRYPT_KEY));
			while(rs.next()) {
				String emailAddress = se.decrypt(rs.getString("email_address_txt"));
				if(!StringUtil.isEmpty(emailAddress)) {
					profiles.put(emailAddress, rs.getString("profile_id"));
				}
			}
		}
		return profiles;
	}

	/**
	 * Builds statement used to loadExistingData from the current Roster
	 * Table.
	 * @return
	 */
	private String loadExistingDataSql() {
		StringBuilder sql = new StringBuilder(250);
		sql.append("select p.profile_id, p.email_address_txt from profile p ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("dpy_syn_srt_roster r ");
		sql.append("on p.profile_id = r.profile_id ");
		sql.append(DBUtil.WHERE_CLAUSE).append("r.op_co_id = ? and r.workgroup_id = ? ");
		sql.append("and r.is_active = ?");
		return sql.toString();
	}

	/**
	 * Loads data that was stored in the tempTableName;
	 * @param tempTableName
	 * @return
	 * @throws SQLException 
	 */
	private Map<String, SRTRosterVO> loadNewUserData(String tempTableNm, String opCoId) throws SQLException {
		Map<String, SRTRosterVO> tempData = new HashMap<>();
		try(PreparedStatement ps = getConnection().prepareStatement(loadNewUserDataSql(tempTableNm))) {
			ResultSet rs = ps.executeQuery();

			SRTRosterVO roster = null;
			while(rs.next()) {
				roster = new SRTRosterVO(rs);
				roster.setOpCoId(opCoId);
				tempData.put(roster.getEmailAddress(), roster);
			}
		}
		return tempData;
	}

	/**
	 * Builds statement used to loadNewUserData from the temp sql table.
	 * @return
	 */
	private String loadNewUserDataSql(String tempTableNm) {
		StringBuilder sql = new StringBuilder(100);
		sql.append(DBUtil.SELECT_FROM_STAR).append(SRTRosterImportAction.TEMP_SCHEMA).append(".");
		sql.append(SRTRosterImportAction.TEMP_TABLE_PREFIX).append(tempTableNm);
		return sql.toString();
	}
}