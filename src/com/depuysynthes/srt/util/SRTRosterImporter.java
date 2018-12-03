package com.depuysynthes.srt.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.depuysynthes.srt.vo.SRTRosterVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title:</b> SRTRosterImporter.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Roster Importer Implementation.  Used for importing
 * data from an external Data source.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Jun 18, 2018
 ****************************************************************************/
public class SRTRosterImporter extends SRTUserImport {

	/**
	 * @param args
	 */
	public SRTRosterImporter(String ... args) {
		super(args);
	}

	/**
	 * Constructor that takes Attributes map as config.
	 * @param smtdbConnection 
	 * @param attributes
	 */
	public SRTRosterImporter(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super(dbConn, attributes);
	}

	/* (non-Javadoc)
	 * @see com.depuysynthes.srt.util.SRTUserImport#buildMainQuery()
	 */
	@Override
	protected StringBuilder buildMainQuery() {
		return null;
	}

	/**
	 * Entry point for Excel Upload.  Performs insert of new users.  Existing
	 * users have been updated or deactivated by this point.
	 * @param rosters
	 * @throws Exception 
	 */
	public void importUsers(List<SRTRosterVO> rosters) throws Exception {
		startTimeInMillis = Calendar.getInstance().getTimeInMillis();

		throwErrors = true;

		populatePropVars();

		List<Map<String, Object>> records = convertRoster(rosters);

		insertRecords(records);
	}

	/**
	 * Converts a list of RosterVOs to List of MapData
	 * @param rosters
	 * @return
	 */
	private List<Map<String, Object>> convertRoster(List<SRTRosterVO> rosters) {
		List<Map<String, Object>> rosterData = new ArrayList<>();
		Map<String, Object> rData;
		for(SRTRosterVO roster : rosters) {

			//Ensure we don't add existing users.
			if(StringUtil.isEmpty(roster.getProfileId()) && !StringUtil.isEmpty(roster.getEmailAddress())) {
				rData = new HashMap<>();
				rData.put(ImportField.ACCOUNT_NO.name(), roster.getAccountNo());
				rData.put(ImportField.IS_ACTIVE.name(), Integer.toString(roster.getIsActive()));
				rData.put(ImportField.EMAIL_ADDRESS_TXT.name(), roster.getEmailAddress());
				rData.put(ImportField.ROSTER_EMAIL_ADDRESS_TXT.name(), roster.getEmailAddress());
				rData.put(ImportField.FIRST_NM.name(), roster.getFirstName());
				rData.put(ImportField.LAST_NM.name(), roster.getLastName());
				rData.put(ImportField.USER_NAME.name(), roster.getFullName());
				rData.put(ImportField.ADDRESS_TXT.name(), roster.getAddress());
				rData.put(ImportField.CITY_NM.name(), roster.getCity());
				rData.put(ImportField.STATE_CD.name(), roster.getState());
				rData.put(ImportField.ZIP_CD.name(), roster.getZipCode());
				rData.put(ImportField.PASSWORD_TXT.name(), roster.getPassword());
				rData.put(ImportField.ROLE_TXT.name(), roster.getWorkgroupId());
				rData.put(ImportField.WORKGROUP_ID.name(), roster.getWorkgroupId());
				rData.put(ImportField.ALLOW_COMM_FLG.name(), 1);
				rData.put(ImportField.TERRITORY_ID.name(), roster.getTerritoryId());
				rData.put(ImportField.REGION_ID.name(), roster.getRegion());
				rData.put(ImportField.AREA_ID.name(), roster.getArea());
				rData.put(ImportField.MOBILE_PHONE_TXT.name(), roster.getMobilePhone());
				rData.put(ImportField.OP_CO_ID.name(), roster.getOpCoId());
				rData.put(ImportField.IS_ADMIN.name(), Integer.toString(roster.getIsAdmin()));
				rData.put(ImportField.CO_ROSTER_ID.name(), roster.getCoRosterId());
				rData.put(ImportField.ENGINEERING_CONTACT.name(), roster.getEngineeringContact());
				rData.put(ImportField.WWID.name(), roster.getWwid());
				rosterData.add(rData);
			}
		}
		return rosterData;
	}
}
