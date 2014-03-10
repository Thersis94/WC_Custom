package com.ansmed.datafeed;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.siliconmtn.util.StringUtil;

//Log4J 1.2.15
import org.apache.log4j.Logger;

/****************************************************************************
 * <b>Title</b>: ProfileQuery.java<p/>
 * <b>Description: </b> Retrieves map of profiles and video format values
 * for various Contact Us/Registration portlets. 
 * <p/>
 * <b>Copyright:</b> (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Mar. 19, 2009
 ****************************************************************************/
public class ProfileQuery {
	
	private static Logger log = Logger.getLogger(ProfileQuery.class);
		
	private String schema = "SiteBuilder_sb.dbo.";
	private String dateStart;
	private String dateEnd;
	private List<String> allProfiles = null;
	private Map<String,String> userMap = null;
	private Map<String,String> countryMap = null;
	private Connection conn = null;

	public ProfileQuery () {
		this(new Date(GregorianCalendar.getInstance().getTimeInMillis()));
	}
	
	public ProfileQuery(Date runDate) {
		this(runDate, runDate);
	}
	
	public ProfileQuery(Date start, Date end) {
		this.dateStart = start.toString() + " 00:00:00";
		this.dateEnd = end.toString() + " 23:59:59";
		allProfiles = new ArrayList<String>();
		userMap = new HashMap<String,String>();
		countryMap = new HashMap<String,String>();
	}
	
	
	/**
	 * Calls query method to retrieve/load kit profiles
	 */
	public void loadKitRequestorProfiles() {
		
		//GET profile IDs
		List<String> profiles = null;
		
		//BRC
		profiles = this.queryProfiles(conn,"c0a8021eed0b59d1b502e106b0e9956c","c0a8021eed159b04c5a0a2d130a4e09","brcCard");
		log.debug("BRC profiles list size: " + profiles.size());
		this.addFormat(conn,profiles,"c0a8021eed0b59d1b502e106b0e9956c","c0a8021e209f76c4f5bbddee1a2ad8e0");
		this.addCountry(conn,profiles,"c0a8021eed0b59d1b502e106b0e9956c","c0a8021eed0e1d86d104529c9e094d8c","brcCard");
		
		//InfoKit
		profiles = null;
		profiles = this.queryProfiles(conn,"c0a80228d7b30e0f59cf59d3fffdfc22","","infoKit");
		log.debug("InfoKit profiles list size: " + profiles.size());
		this.addFormat(conn,profiles,"c0a80228d7b30e0f59cf59d3fffdfc22","c0a80228d7bc7345474c59ff8c97b5e1");
		this.addCountry(conn,profiles,"c0a80228d7b30e0f59cf59d3fffdfc22","c0a802283493393656c8fd4a909568a9","infoKit");
		
		//Contact Us
		profiles = null;
		profiles = this.queryProfiles(conn,"c0a8022834e40d1b8e531bc0e370fc60","c0a8022834ed44aa128d3e38c4d6413f","contactUs");
		log.debug("ContactUs profiles list size: " + profiles.size());
		this.addFormat(conn,profiles,"c0a8022834e40d1b8e531bc0e370fc60","c0a80228d7bc7345474c59ff8c97b5e1");
		this.addCountry(conn,profiles,"c0a8022834e40d1b8e531bc0e370fc60","c0a802283493393656c8fd4a909568a9","contactUs");
		
		//Teleconference
		profiles = null;
		profiles = this.queryProfiles(conn,"c0a8021e1148680e375617a926fce14","c0a80228d652353ce812f13e2b783780","teleConf");
		log.debug("Teleconference profiles list size: " + profiles.size());
		this.addFormat(conn,profiles,"c0a8021e1148680e375617a926fce14","c0a80228d652353ce812f13e2b783780");
		this.addCountry(conn,profiles,"c0a8021e1148680e375617a926fce14","c0a802283493393656c8fd4a909568a9","teleConf");
		
		//POYP-Spanish site InfoKit
		//For this form I am passing in two field id values as a 
		//colon-delimited String for use in the query.  This is split when the 
		//query SQL is built.
		profiles = null;
		profiles = this.queryProfiles(conn, "c0a802281bda5ce6b55e6589832dbcf8","c0a80237fc01dfb6991002f4572d2c24:c0a802281bea952eb11e5b26771331ef","spanishInfoKit");
		log.debug("POYP-ES InfoKit profiles list size: " + profiles.size());
		this.addFormat(conn,profiles,"c0a802281bda5ce6b55e6589832dbcf8","c0a802281bea952eb11e5b26771331ef");
		this.addCountry(conn,profiles,"c0a802281bda5ce6b55e6589832dbcf8","c0a80228179bfc1bc032f7c5d61b2694","spanishInfoKit");
		
		// set the connection reference to null
		conn = null;
		
	}
	
	
	/**
	 * Returns List of profile Id's based on parameters passed in.
	 * @param conn
	 * @param actionId
	 * @param fieldId
	 * @param form
	 * @return
	 */
	public List<String> queryProfiles(Connection conn, String actionId, String fieldId, String form) {
		// type = 'contact' or 'register'
		
		List<String> profiles = new ArrayList<String>();
		
		StringBuffer baseSql = new StringBuffer();
		baseSql.append("select distinct a.profile_id from contact_submittal a ");
		baseSql.append("inner join contact_data b on a.contact_submittal_id = ");
		baseSql.append("b.contact_submittal_id where a.action_id = ? ");
		
		PreparedStatement ps = null;
		
		StringBuffer sql = new StringBuffer(baseSql);
		sql.append(this.addFieldQuery(form));
	
		log.debug(form + " profile SQL: " + sql.toString());
	
		int index = 1;
		try {
			ps = conn.prepareStatement(sql.toString());
			ps.setString(index++, actionId);
			if (form.equalsIgnoreCase("brcCard") || form.equalsIgnoreCase("contactUs")) {
				ps.setString(index++, fieldId);
				ps.setString(index++, "yes");
			} else if (form.equalsIgnoreCase("teleConf")) {
				ps.setString(index++, fieldId);
			} else if (form.equalsIgnoreCase("spanishInfoKit")) {
				String[] fields = fieldId.split(":");
				ps.setString(index++, actionId);
				ps.setString(index++, fields[0]);
				ps.setString(index++, fields[1]);
			} 
			
			ps.setString(index++, dateStart);
			ps.setString(index++, dateEnd);
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (StringUtil.checkVal(rs.getString("profile_id")).length() > 0) {
					profiles.add(rs.getString("profile_id"));
					allProfiles.add(rs.getString("profile_id"));
				}
			}
						
		} catch (SQLException sqle) {
			log.error("ERROR: Could not retrieve profiles for actionId/type: " + actionId + "/", sqle);
		}
		
		return profiles;
		
	}
	
	private StringBuffer addFieldQuery(String actionId) {
		
		StringBuffer fieldQuery = new StringBuffer();
		
		// additional query logic for certain contact forms
		if (actionId.equalsIgnoreCase("brcCard") || actionId.equalsIgnoreCase("contactUs")) {
			fieldQuery.append(" and (b.contact_field_id = ? and ");
			fieldQuery.append("CAST(b.value_txt AS nvarchar(max)) = ?) ");
		} else if (actionId.equalsIgnoreCase("teleConf")) {
			fieldQuery.append(" and (b.contact_field_id = ? and ");
			fieldQuery.append("(CAST(b.value_txt AS nvarchar(max)) in ('DVD','VHS','Spanish VHS','Spanish DVD'))) "); 
		} else if (actionId.equalsIgnoreCase("spanishInfoKit")) {
			fieldQuery.append("and a.accepted_privacy_flg = 1 ");
			fieldQuery.append("and a.profile_id not in "); 
			fieldQuery.append("(select c.profile_id from ").append(schema).append("contact_submittal c ");
			fieldQuery.append("inner join ").append(schema).append("contact_data d on ");
			fieldQuery.append("c.contact_submittal_id = d.contact_submittal_id "); 
			fieldQuery.append("where c.action_id = ? and d.contact_field_id = ?) ");
			fieldQuery.append("and (b.contact_field_id = ? and ");
			fieldQuery.append("(CAST(b.value_txt AS nvarchar(max)) in ('DVD','Spanish DVD'))) ");
		}
		
		// add date range
		fieldQuery.append("and (b.create_dt >= ? and b.create_dt <= ?)");
				
		return fieldQuery;
	}
	
	/**
	 * Returns Map of profile Id's and field value text based on parameters passed in.
	 * @param conn
	 * @param profiles
	 * @param actionId
	 * @param fieldId
	 * @return
	 */
	public void addFormat(Connection conn, List<String> profiles, 
			String actionId, String fieldId) {
		
		StringBuffer sql = new StringBuffer();
		
		sql.append("select a.profile_id, b.value_txt from ").append(schema);
		sql.append("contact_submittal a inner join ").append(schema);
		sql.append("contact_data b on a.contact_submittal_id = ");
		sql.append("b.contact_submittal_id where a.action_id = ? ");
		sql.append("and a.profile_id = ? and b.contact_field_id = ? ");
		sql.append("and (b.create_dt >= ? and b.create_dt <= ?)");

		log.debug("addFormat SQL: " + sql.toString());
		log.debug("actionId: " + actionId);
		log.debug("fieldId: " + fieldId);
		
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		for(int i = 0; i < profiles.size(); i++) {
			try {
				
				ps = conn.prepareStatement(sql.toString());
				ps.setString(1, actionId);
				ps.setString(2, profiles.get(i));
				ps.setString(3, fieldId);
				ps.setString(4, dateStart);
				ps.setString(5, dateEnd);
				rs = ps.executeQuery();
				if (rs.next()) {
					String format = StringUtil.checkVal(rs.getString("value_txt"));
					if (format.toLowerCase().contains("dvd") || format.toLowerCase().contains("vhs")) {
						userMap.put(rs.getString("profile_id"), format);
						log.debug(i + ": userMap.put: " + rs.getString("profile_id") + "***" + format);
					} else {
						userMap.put(rs.getString("profile_id"), "DVD");
						log.debug(i + ": userMap.put: " + rs.getString("profile_id") + "***" + "DVD");
					}
				} else {
					userMap.put(profiles.get(i), "DVD");
					log.debug(i + ": userMap.put: " + profiles.get(i) + " *** " + "DVD");
				}
				
			} catch (SQLException sqle) {
				log.error("ERROR: Could not retrieve profile/format value text data.",sqle);
				log.error("ERROR: profileId/fieldId: " + profiles.get(i) + "/" + fieldId);
			}
			ps = null;
			rs = null;
		}
		
		return;
	}
	
	/**
	 * Populates a Map of profileIds/Countries
	 * @param conn
	 * @param profiles
	 * @param actionId
	 * @param fieldId
	 * @param form
	 */
	public void addCountry(Connection conn, List<String> profiles, 
			String actionId, String fieldId, String form) {
		
		StringBuffer sql = new StringBuffer();
		
		sql.append("select a.profile_id, b.value_txt from ").append(schema);
		sql.append("contact_submittal a inner join ").append(schema);
		sql.append("contact_data b on a.contact_submittal_id = ");
		sql.append("b.contact_submittal_id where a.action_id = ? ");
		sql.append("and a.profile_id = ? and b.contact_field_id = ? ");
		sql.append("and (b.create_dt >= ? and b.create_dt <= ?)");

		log.debug("addCountry SQL: " + sql.toString());
		log.debug("actionId/fieldId: " + actionId + "/" + fieldId);
		
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		for(int i = 0; i < profiles.size(); i++) {
			try {
				
				ps = conn.prepareStatement(sql.toString());
				ps.setString(1, actionId);
				ps.setString(2, profiles.get(i));
				ps.setString(3, fieldId);
				ps.setString(4, dateStart);
				ps.setString(5, dateEnd);
				rs = ps.executeQuery();
				if (rs.next()) {
					String valueTxt = StringUtil.checkVal(rs.getString("value_txt"));
					if (valueTxt.length() > 0) {
						countryMap.put(rs.getString("profile_id"), valueTxt);
						log.debug(i + ": countryMap.put: " + rs.getString("profile_id") + " *** " + valueTxt);
					} else {
						countryMap.put(rs.getString("profile_id"), "");
						log.debug(i + ": countryMap.put: " + rs.getString("profile_id") + " *** " + valueTxt);
					}
				}
				
			} catch (SQLException sqle) {
				log.error("ERROR: Could not retrieve profile/country value text data.",sqle);
				log.error("ERROR: profileId/fieldId: " + profiles.get(i) + "/" + fieldId);
			}
			ps = null;
			rs = null;
		}
		
		return;
	}

	/**
	 * @return the userProfiles
	 */
	public List<String> getAllProfiles() {
		return allProfiles;
	}

	/**
	 * @param userProfiles the userProfiles to set
	 */
	public void setAllProfiles(List<String> allProfiles) {
		this.allProfiles = allProfiles;
	}

	/**
	 * @return the userMap
	 */
	public Map<String, String> getUserMap() {
		return userMap;
	}

	/**
	 * @param userMap the userMap to set
	 */
	public void setUserMap(Map<String, String> userMap) {
		this.userMap = userMap;
	}

	/**
	 * @return the countryMap
	 */
	public Map<String, String> getCountryMap() {
		return countryMap;
	}

	/**
	 * @param countryMap the countryMap to set
	 */
	public void setCountryMap(Map<String, String> countryMap) {
		this.countryMap = countryMap;
	}

	/**
	 * @param conn the conn to set
	 */
	public void setDBConnection(Connection conn) {
		this.conn = conn;
	}
	
}
