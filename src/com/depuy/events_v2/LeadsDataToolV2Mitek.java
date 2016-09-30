package com.depuy.events_v2;

//JDK 1.5.0
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

// SMT BaseLibs
import com.depuy.events_v2.ReportBuilder.ReportType;
import com.depuy.events_v2.vo.DePuyEventSeminarVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.gis.parser.GeoLocation;

// SiteBuilder libs
import com.smt.sitebuilder.action.event.vo.EventEntryVO;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: LeadsDataTool.java<p/>
 * <b>Description: </b> encapsulates all transactions that deal with the 
 * patient data/tables.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 19, 2014
 ****************************************************************************/
public class LeadsDataToolV2Mitek extends LeadsDataToolV2 {
	
	public LeadsDataToolV2Mitek() {
		super();
	}

	/**
	 * @param arg0
	 */
	public LeadsDataToolV2Mitek(ActionInitVO arg0) {
		super(arg0);
	}

	/**
	 * pullLeads retrieves the actual users to invite to this event
	 * (note difference from scopeLeads)
	 * pulls the leads data from the view (profile tables) for this postcard/mailing
	 * @param leads
	 * @throws ActionException
	 */
	@Override
	protected List<UserDataVO> pullLeads(DePuyEventSeminarVO sem, ReportType type,
			Date startDt) throws ActionException {
		log.debug("starting LeadsDataTool::pullLeads()");
		List<UserDataVO> data = new ArrayList<UserDataVO>();
		EventEntryVO event = sem.getEvents().get(0);
		GeoLocation points = GeoLocation.fromDegrees(event.getLatitude(), event.getLongitude());
		Map<String, GeoLocation> coords = points.boundingCoordinates(MAX_DISTANCE);
		log.debug("event=" + event.getLatitude() + " " +  event.getLongitude());
		StringBuilder sql = new StringBuilder(500);
		sql.append("select a.*, lds.max_age_no, lds.event_lead_source_id ");
		sql.append("from (select * from MITEK_SEMINARS_VIEW where (latitude_no between ? and ?) and (longitude_no between ? and ?) and product_cd in (");
		for (@SuppressWarnings("unused") String joint : sem.getJoints())
			sql.append("?,");
		sql.replace(sql.length() - 1, sql.length(), ")"); // replace trailing comma with closing paren
		if (ReportType.leads != type) sql.append(" and valid_address_flg=1"); //if we're pulling a mailing list, we only want valid addresses
		sql.append(") as a ");
		//when targetting leads we may not have data the in _DATASOURCE table; use the join to denote 'checked' radio buttons.
		sql.append((ReportType.leads == type) ? "left outer" : "inner").append(" join ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DEPUY_EVENT_LEADS_DATASOURCE lds on a.state_cd=lds.state_cd and (a.city_nm=lds.city_nm or a.zip_cd=lds.zip_cd) ");
		sql.append("and a.product_cd=lds.PRODUCT_CD and lds.event_postcard_id=? ");
		log.debug(sql);

		int x = 1;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setDouble(x++, coords.get(GeoLocation.MIN_BOUNDING_LOC).getLatitudeInDegrees());
			ps.setDouble(x++, coords.get(GeoLocation.MAX_BOUNDING_LOC).getLatitudeInDegrees());
			ps.setDouble(x++, coords.get(GeoLocation.MIN_BOUNDING_LOC).getLongitudeInDegrees());
			ps.setDouble(x++, coords.get(GeoLocation.MAX_BOUNDING_LOC).getLongitudeInDegrees());
			for (String joint : sem.getJoints()) {
				ps.setString(x++, sem.getJointName(joint));
			}
			ps.setString(x++, sem.getEventPostcardId());
			
			
			ResultSet rs = ps.executeQuery();
			DBUtil db = new DBUtil();
			ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
			while (rs.next()) {
//				String geo = rs.getString("geo_match_cd");
//				if ("noMatch".equals(geo)) continue;  //we don't want these, but they're too taxing to remove in the query
				
				//before we capture each record, determine if it falls within the date requirement. (<3mos, 3-6mos, <1yr, all)
				Date createDt = db.getDateVal("attempt_dt", rs);
				if (createDt == null) createDt = new Date();
				
				//no date filters on the targetLeads page.  This is only used in reports:
				if (ReportType.leads != type) {
					//if we've been asked for "newer than a certain date" leads, filter the old ones out first.
					if (startDt != null && startDt.after(createDt)) continue; 
							
					int maxAgeMos = rs.getInt("max_age_no");
					if (maxAgeMos == 0) maxAgeMos = LeadTierFour; //20yrs of historical data
					Calendar cal = Calendar.getInstance();
					cal.add(Calendar.MONTH, -maxAgeMos); //subtract month limit from today
					
					//if the lead is not newer than the defined limit, skip it.
					//bypass this for scoping leads, which needs a count for each date range.
					if (!cal.getTime().before(createDt)) continue;
				}
				
				UserDataVO vo = new UserDataVO();
				vo.setBirthDate(createDt);
				vo.setPrefixName(pm.getStringValue("PREFIX_NM", db.getStringVal("prefix_nm", rs)));
				vo.setFirstName(pm.getStringValue("FIRST_NM", db.getStringVal("first_nm", rs)));
				vo.setLastName(pm.getStringValue("LAST_NM", db.getStringVal("last_nm", rs)));
				vo.setEmailAddress(pm.getStringValue("EMAIL_ADDRESS_TXT", db.getStringVal("EMAIL_ADDRESS_TXT", rs)));
				vo.setSuffixName(pm.getStringValue("SUFFIX_NM", db.getStringVal("suffix_nm", rs)));
				vo.setAddress(pm.getStringValue("ADDRESS_TXT", db.getStringVal("address_txt", rs)));
				vo.setAddress2(pm.getStringValue("ADDRESS2_TXT", db.getStringVal("address2_txt", rs)));
				vo.setCity(pm.getStringValue("CITY_NM", db.getStringVal("city_nm", rs)));
				vo.setState(pm.getStringValue("STATE_CD", db.getStringVal("state_cd", rs)));
				vo.setZipCode(pm.getStringValue("ZIP_CD", db.getStringVal("zip_cd", rs)));
				vo.setGlobalAdminFlag(rs.getInt("valid_address_flg"));
				vo.setValidEmailFlag(rs.getInt("valid_email_flg"));
				
				//grab some extras we need for display cosmetics
				if (ReportType.leads == type) {
					//we don't want max-age here (as an upper-limit), we want the LEAD'S age
					vo.setBirthYear(bucketizeLeadAge(differenceInMonths(createDt)));
					vo.setAliasName(rs.getString("event_lead_source_id"));
					vo.setCounty(rs.getString("COUNTY_NM"));
					vo.setPassword(rs.getString("product_cd"));
					vo.addAttribute("savedMaxAge", rs.getInt("max_age_no"));
				} else {
					vo.setProfileId(rs.getString("profile_address_id"));
				}

				data.add(vo);
			}
		} catch (SQLException sqle) {
			log.error("pulling Leads data", sqle);
		}

		log.debug("data size=" + data.size());
		return deduplicateUsers(data);
	}
}