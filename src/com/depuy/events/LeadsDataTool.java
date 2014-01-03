package com.depuy.events;

//JDK 1.5.0
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// SMT BaseLibs
import com.depuy.events.vo.DePuyEventLeadSourceVO;
import com.depuy.events.vo.LeadCityVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// SiteBuilder libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.constants.Constants;


/****************************************************************************
 * <b>Title</b>: LeadsDataTool.java<p/>
 * <b>Description: </b> encapsulates all transactions that deal with the 
 * patient data/tables.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2005<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since a long long time ago
 ****************************************************************************/
public class LeadsDataTool extends SBActionAdapter {

	public static final Integer RADIUS_LIMIT_USER = Integer.valueOf(350);
	//public static final Integer RADIUS_LIMIT_DIRECTOR = Integer.valueOf(600);
	public static final int MAILING_LIST_PULL = 1;
	public static final int EMAIL_ADDRESS_PULL = 2;
	public static final int POSTCARD_SUMMARY_PULL = 4;
	public static final int MAILING_LIST_BY_DATE_PULL = 5;
	public static final int LOCATOR_REPORT = 6; //not used here
	public static final int RSVP_SUMMARY_REPORT = 7; //not used here
	public static final int EVENT_ROLLUP_REPORT = 8; //not used here
	public static final int RSVP_BREAKDOWN_REPORT = 9; //not used here
	public static final int LEAD_AGING_REPORT = 10;
	
	/**
	 * 
	 */
	public LeadsDataTool() {
		super();
	}

	/**
	 * @param arg0
	 */
	public LeadsDataTool(ActionInitVO arg0) {
		super(arg0);
	}

	/**
	 * pullLeads retrieves the actual users to invite to this event
	 * (note difference from scopeLeads)
	 * pulls the leads data from the view (profile tables) for this postcard/mailing
	 * @param leads
	 * @throws ActionException
	 */
	private List<UserDataVO> pullLeads(List<DePuyEventLeadSourceVO> leads, String productNm, int type, String startDt) 
	throws ActionException {
		log.debug("starting LeadsDataTool::pullLeads()");
        List<UserDataVO> data = new ArrayList<UserDataVO>();
        StringBuilder sql = new StringBuilder();
        type = Convert.formatInteger(type); //ensure not null
        
        //set default SQL contracts
        String select = "''";
        StringBuilder where = new StringBuilder();
        where.append("valid_address_flg=1 ");
        
        //determine select columns
        if (type == MAILING_LIST_PULL || type == LEAD_AGING_REPORT) {
        	select = "*";
        	
        } else if (type == EMAIL_ADDRESS_PULL) {
        	select = "email_address_txt"; //email list only
        	where = new StringBuilder("email_address_txt is not null");
        	
        } else if (type == MAILING_LIST_BY_DATE_PULL) {
        	select = "*"; //get all data but limit leads by registration date
        	where.append(" and create_dt >= ?");
		}
        
        //setup base SQL statement
        sql.append("select ").append(select).append(" from ").append(productNm);
        sql.append("_postcard_events_view ").append("where (1=0 ");
        
        
        //iterate the lead sources and build the WHERE conditional
		log.debug("starting leadSourceWhereClauseBuilder");
		String state;
		List<LeadCityVO> cities = null;
		List<String> zips = null;
		for (DePuyEventLeadSourceVO src : leads) {
			state = StringUtil.checkVal(src.getStateCode());
			cities = src.getLeadCities();
			zips = src.getLeadZips();
	        
			if (state.length() < 2) continue;
			
			if (cities.size() > 0 && zips.size() > 0) {
				sql.append(" or ((state_cd=? and lower(city_nm) in (");
				for (int x=0; x < cities.size(); x++) {
    	        	sql.append("?,");
            	}
    	        sql.append("'|')) or zip_cd in (");
    	        
    	        for (int x=0; x < zips.size(); x++) {
    	        	sql.append("?,");
            	}
    	        sql.append("'|'))");
    	        
			} else if (cities.size() > 0) {
				sql.append(" or (state_cd=? and lower(city_nm) in (");
				for (int x=0; x < cities.size(); x++) {
    	        	sql.append("?,");
            	}
    	        sql.append("'|'))");
    	        
			} else if (zips.size() > 0) {
				sql.append(" or zip_cd in (");
				for (int x=0; x < zips.size(); x++) {
    	        	sql.append("?,");
            	}
    	        sql.append("'|')");
			}
			src = null;
		}
		
		//finalize the SQL statement
        sql.append(") and (").append(where.toString()).append(")");
        if (type == LEAD_AGING_REPORT) sql.append(" order by create_dt desc");
        log.info("pullLeads sql=" + sql);
        
        PreparedStatement stmt = null;
        DBUtil db = new DBUtil();
        ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
    	int i = 0;
        try {
        	stmt = dbConn.prepareStatement(sql.toString());
        	
        	//iterate the lead sources and insert the WHERE values into the PS
            log.debug("starting leadSourceWhereClauseBuilder");
    		for (DePuyEventLeadSourceVO src : leads) {
    			state = StringUtil.checkVal(src.getStateCode());
    			cities = src.getLeadCities();
    			zips = src.getLeadZips();if (state.length() < 2) continue;
    			
    			if (cities.size() > 0 && zips.size() > 0) {
    				stmt.setString(++i, state);
    				
        	        for (LeadCityVO vo : cities)
        	        	stmt.setString(++i, vo.getCityNm().toLowerCase());
                	
        	        for (String zip : zips)
        	        	stmt.setString(++i, zip);
    				
    			} else if (cities.size() > 0) {
    				stmt.setString(++i, state);
    				
        	        for (LeadCityVO vo : cities)
        	        	stmt.setString(++i, vo.getCityNm().toLowerCase());
    				
    			} else if (zips.size() > 0) {
    				
    				for (String zip : zips)
        	        	stmt.setString(++i, zip);
    				
    			}
    			src = null;
    		}
        	
        	if (type == MAILING_LIST_BY_DATE_PULL) {
            	stmt.setDate(++i, Convert.formatSQLDate(startDt));
    		}
        	ResultSet rs = stmt.executeQuery();
        	while (rs.next()) {
        		UserDataVO vo = new UserDataVO();
        		
        		//pull limited fields off the RS depending on query type.
        		//note lack of "break;" statements
        		switch (type) {
        			case (LEAD_AGING_REPORT):
        			case (MAILING_LIST_PULL):
        			case (MAILING_LIST_BY_DATE_PULL):
	        			vo.setFirstName(pm.getStringValue("FIRST_NM", db.getStringVal("first_nm", rs)));
		        		vo.setLastName(pm.getStringValue("LAST_NM", db.getStringVal("last_nm", rs)));
		        		vo.setSuffixName(pm.getStringValue("SUFFIX_NM", db.getStringVal("suffix_nm", rs)));
		        		vo.setAddress(pm.getStringValue("ADDRESS_TXT", db.getStringVal("address_txt", rs)));
		        		vo.setAddress2(pm.getStringValue("ADDRESS2_TXT", db.getStringVal("address2_txt", rs)));
		        		vo.setCity(pm.getStringValue("CITY_NM", db.getStringVal("city_nm", rs)));
		        		vo.setState(pm.getStringValue("STATE_CD", db.getStringVal("state_cd", rs)));
		        		vo.setZipCode(pm.getStringValue("ZIP_CD", db.getStringVal("zip_cd", rs)));
		        		vo.setBirthDate(db.getDateVal("create_dt", rs));
	        		
	        		case (EMAIL_ADDRESS_PULL):
	        			vo.setEmailAddress(pm.getStringValue("EMAIL_ADDRESS_TXT", db.getStringVal("email_address_txt", rs)));
	        		
	        		default:
	        			vo.setPrefixName(pm.getStringValue("PREFIX_NM", db.getStringVal("prefix_nm", rs)));
        		}

        		data.add(vo);
        		vo = null;
        	}        	
        } catch (SQLException sqle) {
        	log.error("pulling Leads data", sqle);
        } finally {
        	log.debug("data size=" + data.size());
        	try {
        		stmt.close();
        	} catch (Exception e) {}
        }
        
        return data;
	}

	
	/**
	 * pullLeads retrieves the actual users to invite to this event
	 * (note difference from scopeLeads)
	 * leads data comes from the view (profile tables) for this postcard/mailing
	 * @param leads
	 * @throws ActionException
	 */
	public List<UserDataVO> pullLeads(String postcardId, String productNm, int pullType, String startDt) 
	throws ActionException {
		PreparedStatement stmt = null;
        StringBuilder sql = new StringBuilder();
        List<DePuyEventLeadSourceVO> data = new ArrayList<DePuyEventLeadSourceVO>();
        
        //get a list of the selected lead cities/zips for this postcard
        sql.append("select state_cd, city_nm, zip_cd from ");
        sql.append((String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
        sql.append("depuy_event_leads_datasource where event_postcard_id=? ");
        sql.append("order by state_cd");
        try {
        	stmt = dbConn.prepareStatement(sql.toString());
        	stmt.setString(1, postcardId);
        	ResultSet rs = stmt.executeQuery();
        	String state = "";
        	DePuyEventLeadSourceVO elsVo = null;
        	List<String> zips = new ArrayList<String>();
        	List<LeadCityVO> cities = new ArrayList<LeadCityVO>();
            LeadCityVO vo = null;

        	while (rs.next()) {
        		//add the last state to the stack and start a new one
        		if (!state.equals(rs.getString(1))) {
        			if (state.length() > 0) {
        				elsVo.setLeadCities(cities);
        				elsVo.setLeadZips(zips);
        				data.add(elsVo);
        				log.debug("added state " + state);
        			}
        			
        			elsVo = new DePuyEventLeadSourceVO();
        			elsVo.setStateCode(rs.getString(1));
        			state = rs.getString(1);
        			cities = new ArrayList<LeadCityVO>();
        			zips = new ArrayList<String>();
        		}
        		
        		if (rs.getString(2) != null) {
        			vo = new LeadCityVO();
        			vo.setCityNm(rs.getString(2));
        			cities.add(vo);
        			vo = null;
        		} else if (rs.getString(3) != null) { 
        			zips.add(rs.getString(3));
        		}
        	}
        	
        	//add that final state to the stack
        	if (state.length() > 0 ) {
        		elsVo.setLeadCities(cities);
				elsVo.setLeadZips(zips);
				data.add(elsVo);
        	}
        } catch (SQLException sqle) {
        	log.error("establishing selLeads list", sqle);
        } finally {
        	if (stmt != null) {
        		try {
        			stmt.close();
        		} catch (Exception e) {}
        	}
        }
        
        return this.pullLeads(data, productNm, pullType, startDt);
	}

	
	/**
	 * pulls potential-leads info from the view (profile tables) for this postcard/mailing
	 * limits non-admins to leads within "x" miles of their location
	 * @param leads
	 * @throws ActionException
	 */
	public List<LeadCityVO> scopeLeads(String postcardId, String stateCd, String productNm, UserDataVO user, int roleId, boolean byCity) {
        log.debug("starting scopeLeads");
        List<LeadCityVO> data = new ArrayList<LeadCityVO>();
        StringBuilder sql = new StringBuilder();        
        if (byCity) sql.append("select count(*) as cnt, lower(city_nm) as city ");
        else sql.append("select count(*) as cnt, lower(county_nm) as county, lower(city_nm) as city ");
        sql.append("from ").append(productNm);
        sql.append("_postcard_events_view where state_cd=? and valid_address_flg=1 ");
        
        //add 350mi distance limitation for users
        if (roleId < 30) {
    		//if (roleId == 30) range = RADIUS_LIMIT_DIRECTOR;
        	sql.append("and round((sqrt(power(? - latitude_no,2) + power(?");
        	sql.append(" - longitude_no,2)) /3.14159265)*180,2) < ?");
        }

        if (byCity) {
        	sql.append(" group by lower(city_nm) order by city");
        } else {
        	sql.append(" group by lower(county_nm), lower(city_nm) order by county, city");
        }
        log.debug("sql=" + sql + " state=" + stateCd);
        
        List<String> selCities = new ArrayList<String>();
        if (postcardId != null)
        	selCities = this.getSelected(postcardId, productNm, stateCd, "city");

        String cityNm;
    	LeadCityVO vo = null;
        Map<String, Date> mailingDates = this.loadTargetHistory(stateCd, actionInit.getActionId());
        PreparedStatement ps = null;
    	DBUtil db = new DBUtil();
        try {
        	ps = dbConn.prepareStatement(sql.toString());
        	ps.setString(1, stateCd);
        	if (roleId < 30) {
        		ps.setDouble(2, user.getLatitude());
        		ps.setDouble(3, user.getLongitude());
        		ps.setInt(4, RADIUS_LIMIT_USER);
        	}
        	ResultSet rs = ps.executeQuery();
        	while (rs.next()) {
        		vo = new LeadCityVO();
        		vo.setStateCd(stateCd);
        		if (!byCity) vo.setCountyNm(db.getStringVal("county", rs));
        		vo.setCityNm(StringUtil.checkVal(db.getStringVal("city", rs)));
        		cityNm = vo.getCityNm().toUpperCase();
        		
        		if (selCities.contains(cityNm)) 
        			vo.setSelected("checked");
        		
        		if (mailingDates.containsKey(cityNm)) 
        			vo.setLastMailingDt(mailingDates.get(cityNm));
        		
        		vo.setLeadsCnt(db.getIntegerVal("cnt",rs));
        		
        		data.add(vo);
        		vo = null;
        	}        	
        } catch (SQLException sqle) {
        	log.error("pulling scope leads data", sqle);
        } finally {
        	try { ps.close(); } catch (Exception e) {}
        }
        
        log.debug("data size=" + data.size());
    	return data;
	}
	
	
	/**
	 * pulls potential-leads info from the view (profile tables) for this postcard/mailing
	 * limits non-admins to leads within 150mi of their location
	 * @param leads
	 * @throws ActionException
	 */
	public List<String> getSelected(String postcardId, String productNm, String stateCd, String column) {
        log.debug("starting getSelected for " + column);
        List<String> data = new ArrayList<String>();
        StringBuilder sql = new StringBuilder();
        column = (column.equals("city")) ? "city_nm" : "zip_cd";
       	sql.append("select ").append(column).append(" as val from ");
        sql.append((String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
        sql.append("DEPUY_EVENT_LEADS_DATASOURCE where event_postcard_id=? ");
        sql.append("and ").append(column).append(" is not null ");
        if (stateCd != null) sql.append("and state_cd=? ");
        
        log.debug("sql=" + sql + " postcardId=" + postcardId);
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
        	ps = dbConn.prepareStatement(sql.toString());
        	ps.setString(1, postcardId);
        	if (stateCd != null) ps.setString(2, stateCd);
        	rs = ps.executeQuery();
        	DBUtil db = new DBUtil();
        	while (rs.next())
        		data.add(db.getStringVal("val", rs).toUpperCase());

        } catch (SQLException sqle) {
        	log.error("pulling lead Zip data", sqle);
        } finally {
        	try {
        		ps.close();
        	} catch (Exception e) { }
        }
        
        log.debug(column + " size=" + data.size());
    	return data;
	}
	
	/**
	 * this method loads a list of most-recent event dates for each city in the targeted state.
	 * these dates are matched to the cities displayed on the scope leads form 
	 * and displayed as "most recent event in this area".
	 * @param stateCd
	 * @param actionId
	 * @return
	 */
	private Map<String, Date> loadTargetHistory(String stateCd, String actionId) {
		Map<String, Date> data = new HashMap<String, Date>();
		StringBuilder sql = new StringBuilder();
		sql.append("select distinct a.city_nm, max(d.start_dt) ");
		sql.append("from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("depuy_event_leads_datasource a ");
		sql.append("inner join event_postcard b on  ");
		sql.append("a.event_postcard_id=b.event_postcard_id and b.status_flg in (2,4) ");
		sql.append("inner join event_postcard_assoc c on  ");
		sql.append("b.event_postcard_id=c.event_postcard_id ");
		sql.append("inner join event_entry d on c.event_entry_id=d.event_entry_id  ");
		sql.append("and d.start_dt < getDate() and d.action_id=? and d.status_flg=1 ");
		sql.append("where a.state_cd=? and len(a.city_nm) > 0 group by a.city_nm");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, actionId);
			ps.setString(2, stateCd);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				data.put(rs.getString(1).toUpperCase(), rs.getDate(2));
			
		} catch (SQLException sqle) {
			log.error("pulling send history data", sqle);
        } finally {
        	try {
        		ps.close();
        	} catch (Exception e) { }
        }
        
        log.debug(stateCd + " size=" + data.size());
		return data;
	}
	
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
        	
        	if (roleId < 30) {
        		int range = RADIUS_LIMIT_USER;
        		//if (roleId == 30) range = RADIUS_LIMIT_DIRECTOR;
            	stmt.setDouble(++i, user.getLatitude()); 
            	stmt.setDouble(++i, user.getLongitude());
            	stmt.setInt(++i, range);
            }
            
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
