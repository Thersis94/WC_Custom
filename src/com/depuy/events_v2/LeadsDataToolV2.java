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
import com.depuy.events.vo.LeadCityVO;
import com.depuy.events_v2.vo.DePuyEventSeminarVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.gis.parser.GeoLocation;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
// SiteBuilder libs
import com.smt.sitebuilder.action.SBActionAdapter;
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
public class LeadsDataToolV2 extends SBActionAdapter {
	
	private final double MAX_DISTANCE = 100.00; //miles a lead would drive to a Seminar
	
	public static final int POSTCARD_SUMMARY_PULL = 4;
	public static final int MAILING_LIST_BY_DATE_PULL = 5;
	//public static final int LOCATOR_REPORT = 6; //not used here
	public static final int RSVP_SUMMARY_REPORT = 7; //comes from WC core
	public static final int EVENT_ROLLUP_REPORT = 8; //not used here
	public static final int RSVP_BREAKDOWN_REPORT = 9; //not used here
	//public static final int LEAD_AGING_REPORT = 10;
	
	public LeadsDataToolV2() {
		super();
	}

	/**
	 * @param arg0
	 */
	public LeadsDataToolV2(ActionInitVO arg0) {
		super(arg0);
	}

	/**
	 * pullLeads retrieves the actual users to invite to this event
	 * (note difference from scopeLeads)
	 * pulls the leads data from the view (profile tables) for this postcard/mailing
	 * @param leads
	 * @throws ActionException
	 */
	protected List<UserDataVO> pullLeads(DePuyEventSeminarVO sem, int type,
			Date startDt) throws ActionException {
		log.debug("starting LeadsDataTool::pullLeads()");
		List<UserDataVO> data = new ArrayList<UserDataVO>();
		EventEntryVO event = sem.getEvents().get(0);
		GeoLocation points = GeoLocation.fromDegrees(event.getLatitude(), event.getLongitude());
		Map<String, GeoLocation> coords = points.boundingCoordinates(MAX_DISTANCE);
		log.debug("event=" + event.getLatitude() + " " +  event.getLongitude());
		StringBuilder sql = new StringBuilder();
		sql.append("select a.*, lds.max_age_no ");
		sql.append("from (select * from DEPUY_SEMINARS_VIEW where (latitude_no between ? and ?) and (longitude_no between ? and ?)) as a ");
		sql.append("inner join ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DEPUY_EVENT_LEADS_DATASOURCE lds on (a.city_nm=lds.city_nm and a.state_cd=lds.state_cd) or a.zip_cd=lds.zip_cd ");
		sql.append("where  a.product_cd in (");
		for (String joint : sem.getJoints())
			sql.append("?,");
		sql.replace(sql.length() - 1, sql.length(), ")"); // remove trailing comma
		sql.append(" and lds.event_postcard_id=?");
		log.debug(sql);

		int x = 1;
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
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
				//before we capture each record, determine if it falls within the date requirement. (<3mos, 3-6mos, <1yr, all)
				Date createDt = db.getDateVal("attempt_dt", rs);
				if (createDt == null) createDt = new Date();
				
				//if we've been asked for "newer than a certain date" leads, filter the old ones out first.
				if (startDt != null && startDt.after(createDt)) continue; 
						
				int maxAgeMos = rs.getInt("max_age_no");
				if (maxAgeMos == 0) maxAgeMos = 240; //20yrs of historical data
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.MONTH, -maxAgeMos); //subtract month limit from today
				
				//if the lead is not newer than the defined limit, skip it.
				if (!cal.getTime().before(createDt)) continue;
				
				UserDataVO vo = new UserDataVO();
				vo.setBirthDate(createDt);
				vo.setFirstName(pm.getStringValue("FIRST_NM", db.getStringVal("first_nm", rs)));
				vo.setLastName(pm.getStringValue("LAST_NM", db.getStringVal("last_nm", rs)));
				vo.setSuffixName(pm.getStringValue("SUFFIX_NM", db.getStringVal("suffix_nm", rs)));
				vo.setAddress(pm.getStringValue("ADDRESS_TXT", db.getStringVal("address_txt", rs)));
				vo.setAddress2(pm.getStringValue("ADDRESS2_TXT", db.getStringVal("address2_txt", rs)));
				vo.setCity(pm.getStringValue("CITY_NM", db.getStringVal("city_nm", rs)));
				vo.setState(pm.getStringValue("STATE_CD", db.getStringVal("state_cd", rs)));
				vo.setZipCode(pm.getStringValue("ZIP_CD", db.getStringVal("zip_cd", rs)));
				vo.setEmailAddress(pm.getStringValue("EMAIL_ADDRESS_TXT", db.getStringVal( "email_address_txt", rs)));
				vo.setPrefixName(pm.getStringValue("PREFIX_NM", db.getStringVal("prefix_nm", rs)));
				data.add(vo);
			}
		} catch (SQLException sqle) {
			log.error("pulling Leads data", sqle);
		} finally {
			try { ps.close(); } catch (Exception e) { }
		}

		log.debug("data size=" + data.size());
		return deduplicateUsers(data);
	}
	
	
	/**
	 * removes duplicate entries to ensure multiple postcards aren't sent to the same person/address.
	 * accomplished by fName +lName + address1
	 * @param users
	 * @return
	 */
	private List<UserDataVO> deduplicateUsers(List<UserDataVO> users) {
		List<UserDataVO> data = new ArrayList<UserDataVO>(users.size());
		List<String> dupls = new ArrayList<String>(users.size());
		String key = "";
		
		for (UserDataVO vo : users) {
			try {
				key = vo.getFirstName().concat(vo.getLastName()).concat(vo.getAddress());
			} catch (Exception e) {}
			
			if (dupls.contains(key)) {
				continue;
			} else {
				dupls.add(key);
				data.add(vo);
			}
		}
		log.debug("filtered size: " + data.size());
		return data;
	}

	
	
	/**
	 * pulls potential-leads info from the view (profile tables) for this postcard/mailing
	 * limits non-admins to leads within "x" miles of their location
	 * @param leads
	 * @throws ActionException
	 */
	public List<LeadCityVO> scopeLeads(DePuyEventSeminarVO sem) {
		log.debug("starting scopeLeads");
		List<LeadCityVO> data = new ArrayList<LeadCityVO>();
		EventEntryVO event = sem.getEvents().get(0);
		GeoLocation geoLoc = GeoLocation.fromDegrees(event.getLatitude(), event.getLongitude());
		Map<String, GeoLocation> coords = geoLoc.boundingCoordinates(MAX_DISTANCE);
		GeoLocation minCoords = coords.get(GeoLocation.MIN_BOUNDING_LOC);
		GeoLocation maxCoords = coords.get(GeoLocation.MAX_BOUNDING_LOC);

		StringBuilder sql = new StringBuilder();
		sql.append("select count(*), product_cd, count_nm, state_cd, city_nm, attempt_year_no, attempt_month_no ");
		sql.append("from depuy_seminars_view where  product_cd in (");
		for (String joint : sem.getJoints())
			sql.append("?,");
		sql.replace(sql.length() - 1, sql.length(), ")"); // remove trailing comma
		sql.append("and valid_address_flg=1 and latitude_no between ? and ? and longitude_no between ? and ? ");
		sql.append("group by product_cd, count_nm, state_cd, city_nm, attempt_year_no, attempt_month_no ");
		log.debug(sql);

		int x = 1;
		DBUtil db = new DBUtil();
		LeadCityVO vo;
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			for (String joint : sem.getJoints())
				ps.setString(x++, joint);
			ps.setDouble(x++, minCoords.getLatitudeInDegrees());
			ps.setDouble(x++, maxCoords.getLatitudeInDegrees());
			ps.setDouble(x++, minCoords.getLongitudeInDegrees());
			ps.setDouble(x++, maxCoords.getLongitudeInDegrees());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				vo = new LeadCityVO();
				vo.setStateCd(db.getStringVal("state_cd", rs));
				vo.setCountyNm(db.getStringVal("county_nm", rs));
				vo.setCityNm(StringUtil.checkVal(db.getStringVal("city_nm", rs)).toLowerCase());
				vo.setLeadsCnt(db.getIntegerVal("cnt", rs));
				data.add(vo);
			}
		} catch (SQLException sqle) {
			log.error("pulling scope leads data", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {
			}
		}

		log.debug("data size=" + data.size());
		return data;
	}
	
//	
//	/**
//	 * pulls potential-leads info from the view (profile tables) for this postcard/mailing
//	 * limits non-admins to leads within 150mi of their location
//	 * @param leads
//	 * @throws ActionException
//	 */
//	public List<String> getSelected(String postcardId, String productNm, String stateCd, String column) {
//        log.debug("starting getSelected for " + column);
//        List<String> data = new ArrayList<String>();
//        StringBuilder sql = new StringBuilder();
//        column = (column.equals("city")) ? "city_nm" : "zip_cd";
//       	sql.append("select ").append(column).append(" as val from ");
//        sql.append((String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
//        sql.append("DEPUY_EVENT_LEADS_DATASOURCE where event_postcard_id=? ");
//        sql.append("and ").append(column).append(" is not null ");
//        if (stateCd != null) sql.append("and state_cd=? ");
//        
//        log.debug("sql=" + sql + " postcardId=" + postcardId);
//        PreparedStatement ps = null;
//        ResultSet rs = null;
//        try {
//        	ps = dbConn.prepareStatement(sql.toString());
//        	ps.setString(1, postcardId);
//        	if (stateCd != null) ps.setString(2, stateCd);
//        	rs = ps.executeQuery();
//        	DBUtil db = new DBUtil();
//        	while (rs.next())
//        		data.add(db.getStringVal("val", rs).toUpperCase());
//
//        } catch (SQLException sqle) {
//        	log.error("pulling lead Zip data", sqle);
//        } finally {
//        	try {
//        		ps.close();
//        	} catch (Exception e) { }
//        }
//        
//        log.debug(column + " size=" + data.size());
//    	return data;
//	}
//	
	
	
	/**
	 * pulls potential-leads info from the view (profile tables) for this postcard/mailing
	 * limits non-admins to leads within 150mi of their location
	 * @param leads
	 * @throws ActionException
	 */
	public Integer countLeadsByZip(String zip, String productNm, String stateCd, UserDataVO user, int roleId) {
        log.debug("starting countLeadsByZip");
        Integer count = 0;
        StringBuilder sql = new StringBuilder();
        sql.append("select count(*) from ").append(productNm);
        sql.append("_postcard_events_view where zip_cd=? and state_cd=? and valid_address_flg=1 ");
        
        //add 350mi distance limitation for users, 600 for directors
        if (roleId  < 30) {
        	sql.append("and round((sqrt(power(? - latitude_no,2) + "); 
        	sql.append("power(? - longitude_no,2)) /3.14159265)*180,2) < ? ");
        }
        
        log.info("sql=" + sql + " zip=" + zip);
        PreparedStatement stmt = null;
        try {
        	int i = 0;
        	stmt = dbConn.prepareStatement(sql.toString());
        	stmt.setString(++i, zip);
        	stmt.setString(++i, stateCd);
        	
        ResultSet rs = stmt.executeQuery();
        	if (rs.next()) {
        		count = rs.getInt(1);
        	}
        } catch (SQLException sqle) {
        	log.error("pulling leads zip total", sqle);
        } finally {
        	try {
        		stmt.close();
        	} catch (Exception e) {}
        }
        
        log.debug("zips total=" + count);
    	return Convert.formatInteger(count);
	}

}
