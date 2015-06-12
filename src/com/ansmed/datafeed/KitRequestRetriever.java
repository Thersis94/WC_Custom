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
 * <b>Title</b>: KitRequestRetriever.java<p/>
 * <b>Description: </b> Retrieves map of profiles and video format values
 * for kit requests. 
 * <p/>
 * <b>Copyright:</b> (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Nov. 15, 2010
 * Change Log: June 2, 2011 - Added Spanish site (PoderSobreSuDolor) contact/info kit form processing
 ****************************************************************************/
public class KitRequestRetriever {
	
	private static Logger log = Logger.getLogger(KitRequestRetriever.class);
	private static final String LANG_EN = "english";
	private static final String LANG_ES = "spanish";
		
	private String schema = "SiteBuilder_sb.dbo.";
	private String dateStart;
	private String dateEnd;
	private List<String> allProfiles = null;
	private Map<String,String> userMap = null;
	private Map<String,String> countryMap = null;
	private Connection conn = null;

	public KitRequestRetriever () {
		this(new Date(GregorianCalendar.getInstance().getTimeInMillis()));
	}
	
	public KitRequestRetriever(Date runDate) {
		this(runDate, runDate);
	}
	
	public KitRequestRetriever(Date start, Date end) {
		this.dateStart = start.toString() + " 00:00:00";
		this.dateEnd = end.toString() + " 23:59:59";
	}
	
	
	/**
	 * Calls query method to retrieve/load kit profiles
	 */
	public void loadKitRequestorProfiles() {
		log.info("Retrieving profiles for range: " + dateStart + " to " + dateEnd);
		
		// instantiate the list and maps
		allProfiles = new ArrayList<String>();
		userMap = new HashMap<String,String>();
		countryMap = new HashMap<String,String>();
		
		//GET profile IDs
		List<String> profiles = null;
		
		// English forms
		//BRC
		profiles = this.queryProfiles(conn,"c0a8021eed0b59d1b502e106b0e9956c","c0a8021eed159b04c5a0a2d130a4e09","brcCard");
		log.debug("BRC profiles list size: " + profiles.size());
		if (profiles.size() > 0) {
			this.addFormat(conn,profiles,"c0a8021eed0b59d1b502e106b0e9956c","c0a8021e209f76c4f5bbddee1a2ad8e0","brcCard");
			this.addCountry(conn,profiles,"c0a8021eed0b59d1b502e106b0e9956c","c0a8021eed0e1d86d104529c9e094d8c","brcCard");
		}
		
		//InfoKit
		profiles = null;
		profiles = this.queryProfiles(conn,"c0a80228d7b30e0f59cf59d3fffdfc22","c0a80228d7bc7345474c59ff8c97b5e1","infoKit");
		log.debug("InfoKit profiles list size: " + profiles.size());
		if (profiles.size() > 0) {
			this.addFormat(conn,profiles,"c0a80228d7b30e0f59cf59d3fffdfc22","c0a80228d7bc7345474c59ff8c97b5e1","infoKit");
			this.addCountry(conn,profiles,"c0a80228d7b30e0f59cf59d3fffdfc22","c0a802283493393656c8fd4a909568a9","infoKit");
		}
		
		//Contact Us
		profiles = null;
		profiles = this.queryProfiles(conn,"c0a8022834e40d1b8e531bc0e370fc60","c0a8022834ed44aa128d3e38c4d6413f","contactUs");
		log.debug("ContactUs profiles list size: " + profiles.size());
		if (profiles.size() > 0) {
			this.addFormat(conn,profiles,"c0a8022834e40d1b8e531bc0e370fc60","c0a80228d7bc7345474c59ff8c97b5e1","contactUs");
			this.addCountry(conn,profiles,"c0a8022834e40d1b8e531bc0e370fc60","c0a802283493393656c8fd4a909568a9","contactUs");
		}
		
		//Teleconference
		profiles = null;
		profiles = this.queryProfiles(conn,"c0a8021e1148680e375617a926fce14","c0a80228d652353ce812f13e2b783780","teleConf");
		log.debug("Teleconference profiles list size: " + profiles.size());
		if (profiles.size() > 0) {
			this.addFormat(conn,profiles,"c0a8021e1148680e375617a926fce14","c0a80228d652353ce812f13e2b783780","teleConf");
			this.addCountry(conn,profiles,"c0a8021e1148680e375617a926fce14","c0a802283493393656c8fd4a909568a9","teleConf");
		}
		
		// Spanish forms
		/* OLD POYP-Spanish site InfoKit
		 * Split 'fieldId' field in query is used to filter out individuals who are outside of the U.S. 
		 * as SJM doesn't ship kits outside of the U.S.
		profiles = null;
		profiles = this.queryProfiles(conn, "c0a802281bda5ce6b55e6589832dbcf8","c0a80237fc01dfb6991002f4572d2c24:c0a802281bea952eb11e5b26771331ef","spanishInfoKit");
		log.debug("POYP-ES InfoKit profiles list size: " + profiles.size());
		this.addFormat(conn,profiles,"c0a802281bda5ce6b55e6589832dbcf8","c0a802281bea952eb11e5b26771331ef","spanishInfoKit");
		this.addCountry(conn,profiles,"c0a802281bda5ce6b55e6589832dbcf8","c0a80228179bfc1bc032f7c5d61b2694","spanishInfoKit");
		*/
		
		// PSSD Contactenos
		profiles = null;
		profiles = this.queryProfiles(conn, "c0a80228178cb3c08c115684c9eafb39", "c0a8022817a316c5837e583df47b0675", "spanishContactUs");
		log.debug("PSSD Contactenos profiles list size: " + profiles.size());
		if (profiles.size() > 0) {
			this.addFormat(conn, profiles, "c0a80228178cb3c08c115684c9eafb39", "c0a80241124651b03548292a5eb8ae71", "spanishContactUs");
			this.addCountry(conn, profiles, "c0a80228178cb3c08c115684c9eafb39", "c0a80228179bfc1bc032f7c5d61b2694", "spanishContactUs");
		}
		
		// PSSD Infokit
		profiles = null;
		profiles = this.queryProfiles(conn, "c0a80237cad8198bf3a74c321b39949", "c0a80237fc01dfb6991002f4572d2c24:c0a80241124651b03548292a5eb8ae71", "spanishInfoKit");
		log.debug("PSSD Info Kit profiles list size: " + profiles.size());
		if (profiles.size() > 0) {
			this.addFormat(conn, profiles, "c0a80237cad8198bf3a74c321b39949", "c0a80241124651b03548292a5eb8ae71", "spanishInfoKit");
			this.addCountry(conn, profiles, "c0a80237cad8198bf3a74c321b39949", "c0a80228179bfc1bc032f7c5d61b2694", "spanishInfoKit");
		}
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
				
		List<String> profiles = new ArrayList<String>();
		StringBuilder baseSql = new StringBuilder(150);
		baseSql.append("select distinct a.profile_id from contact_submittal a ");
		baseSql.append("inner join contact_data b on a.contact_submittal_id = ");
		baseSql.append("b.contact_submittal_id where a.action_id = ? ");
		
		StringBuilder sql = new StringBuilder(baseSql);
		sql.append(this.addFieldQuery(form));
		log.debug(form + " profile SQL: " + sql.toString());
		log.debug("actionId: " + actionId);
		log.debug("fieldId: " + fieldId);
		PreparedStatement ps = null;
		int index = 1;
		try {
			ps = conn.prepareStatement(sql.toString());
			ps.setString(index++, actionId);
			if (form.equalsIgnoreCase("brcCard") || form.equalsIgnoreCase("contactUs")) {
				ps.setString(index++, fieldId);
				ps.setString(index++, "yes");
			} else if (form.equalsIgnoreCase("spanishContactUs")) {
				ps.setString(index++, fieldId);
				ps.setString(index++, "Si");
			} else if (form.equalsIgnoreCase("teleConf")) {
				ps.setString(index++, fieldId);
			} else if (form.equalsIgnoreCase("spanishInfoKit")) {
				String[] fields = fieldId.split(":");
				ps.setString(index++, actionId);
				ps.setString(index++, fields[0]);
				ps.setString(index++, fields[1]);
			} else if(form.equalsIgnoreCase("infoKit") ){
				ps.setString(index++, fieldId);
				ps.setString(index++, "download pdf");
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
			log.error("Error: Could not retrieve profiles for actionId/type: " + actionId + "/", sqle);
		}
		return profiles;
	}
	
	/**
	 * Adds field-specific queries to the base SQL query
	 * @param actionId
	 * @return
	 */
	private StringBuilder addFieldQuery(String actionId) {
		StringBuilder fieldQuery = new StringBuilder(100);
		// additional query logic for certain contact forms
		if (actionId.equalsIgnoreCase("brcCard") || 
				actionId.equalsIgnoreCase("contactUs") ||
				actionId.equalsIgnoreCase("spanishContactUs")) {
			fieldQuery.append(" and (b.contact_field_id = ? and ");
			fieldQuery.append("CAST(b.value_txt AS nvarchar(max)) = ?) ");
		} else if (actionId.equalsIgnoreCase("teleConf")) {
			fieldQuery.append(" and (b.contact_field_id = ? and ");
			fieldQuery.append("(CAST(b.value_txt AS nvarchar(max)) in ('DVD','Spanish DVD'))) "); 
		} else if (actionId.equalsIgnoreCase("spanishInfoKit")) {
			fieldQuery.append("and a.accepted_privacy_flg = 1 ");
			fieldQuery.append("and a.profile_id not in "); 
			fieldQuery.append("(select c.profile_id from ").append(schema).append("contact_submittal c ");
			fieldQuery.append("inner join ").append(schema).append("contact_data d on ");
			fieldQuery.append("c.contact_submittal_id = d.contact_submittal_id "); 
			fieldQuery.append("where c.action_id = ? and d.contact_field_id = ?) ");
			fieldQuery.append("and (b.contact_field_id = ? and ");
			fieldQuery.append("(CAST(b.value_txt AS nvarchar(max)) in ('ingl�s','espa�ol'))) ");
		}else if(actionId.equalsIgnoreCase("infoKit")){
			fieldQuery.append(" and (b.contact_field_id = ? and ");
			fieldQuery.append("CAST(b.value_txt AS nvarchar(max)) != ?) ");
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
			String actionId, String fieldId, String formType) {
		
		StringBuilder sql = new StringBuilder(250);
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
		String language = null;
		for(int i = 0; i < profiles.size(); i++) {
			// assume 'English' language format
			language = LANG_EN;
			try {
				ps = conn.prepareStatement(sql.toString());
				ps.setString(1, actionId);
				ps.setString(2, profiles.get(i));
				ps.setString(3, fieldId);
				ps.setString(4, dateStart);
				ps.setString(5, dateEnd);
				rs = ps.executeQuery();
				if (rs.next()) {
					// if there is a record check the requested format to determine language
					String format = StringUtil.checkVal(rs.getString("value_txt")).toLowerCase();
					if (format.contains("espa") || format.contains(LANG_ES)) language = LANG_ES;
				} else {
					// no record so if 'spanish' form, assume 'spanish' language
					if (formType.contains(LANG_ES)) 	language = LANG_ES;
				}
				// add the user/format values to the map
				userMap.put(rs.getString("profile_id"), language);
				log.debug(i + ": userMap.put: " + rs.getString("profile_id") + " *** '" + language + "'");
				
			} catch (SQLException sqle) {
				log.error("Error: Could not retrieve profile/format value text data.",sqle);
				log.error("Error: profileId/fieldId: " + profiles.get(i) + "/" + fieldId);
			} 
			ps = null;
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
		
		StringBuilder sql = new StringBuilder(250);
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
					countryMap.put(rs.getString("profile_id"), valueTxt);
					log.debug(i + ": countryMap.put: " + rs.getString("profile_id") + " *** " + valueTxt);
				}				
			} catch (SQLException sqle) {
				log.error("Error: Could not retrieve profile/country value text data.",sqle);
				log.error("Error: profileId/fieldId: " + profiles.get(i) + "/" + fieldId);
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
