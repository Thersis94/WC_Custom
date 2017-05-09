package com.depuy.events;

//JDK 1.5.0
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.depuy.events.ReportBuilder.ReportType;
import com.depuy.events.vo.DePuyEventSeminarVO;
// SMT BaseLibs
import com.depuy.events.vo.LeadCityVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.common.html.SMTStateListFactory;
import com.siliconmtn.common.html.StateList;
import com.siliconmtn.gis.Location;
import com.siliconmtn.gis.parser.GeoLocation;

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
	
	private Map<String, String> usStates = null;
	
	protected final double MAX_DISTANCE = 62.00; //radians a lead would drive to a Seminar  ~50mi
	
	//constants for lead ages (in months); used on the Leads page to render the 4 display columns.
	public final static int LeadTierOne = 6;
	public final static int LeadTierTwo = 12;
	public final static int LeadTierThree = 36;
	public final static int LeadTierFour = 240;
	
	public enum SortType { city, county, zip }
	private UUIDGenerator uuid = null;
	
	
	/**
	 * returns an instance of PostcardEmailer, giving us abstract support for Mitek events
	 * @param sem
	 * @param attrs
	 * @param conn
	 * @return
	 */
	public static LeadsDataToolV2 newInstance(DePuyEventSeminarVO sem, ActionInitVO actionInit) {
		//test for Mitek Seminar.  If so, return the Mitek emailer instead of 'this' class
		if (sem != null && sem.isMitekSeminar()) {
			return new LeadsDataToolV2Mitek(actionInit);
		} else {
			return new LeadsDataToolV2(actionInit);
		}
	}
	
	
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
	protected List<UserDataVO> pullLeads(DePuyEventSeminarVO sem, ReportType type,
			Date startDt) throws ActionException {
		log.debug("starting LeadsDataTool::pullLeads()");
		List<UserDataVO> data = new ArrayList<>();
		EventEntryVO event = sem.getEvents().get(0);
		GeoLocation points = GeoLocation.fromDegrees(event.getLatitude(), event.getLongitude());
		Map<String, GeoLocation> coords = points.boundingCoordinates(MAX_DISTANCE);
		log.debug("event=" + event.getLatitude() + " " +  event.getLongitude());
		StringBuilder sql = new StringBuilder(500);
		sql.append("select a.*, lds.max_age_no, lds.event_lead_source_id ");
		sql.append("from (select * from DEPUY_SEMINARS_VIEW where latitude_no between ? and ? and longitude_no between ? and ? and product_cd in (");
		for (@SuppressWarnings("unused") String joint : sem.getJoints())
			sql.append("?,");
		sql.replace(sql.length() - 1, sql.length(), ")"); // replace trailing comma with closing paren
		if (ReportType.leads != type) sql.append(" and valid_address_flg=1"); //if we're pulling a mailing list, we only want valid addresses
		sql.append(") as a ");
		//when targetting leads we may not have data the in _DATASOURCE table; use the join to denote 'checked' radio buttons.
		sql.append((ReportType.leads == type) ? "left outer" : "inner").append(" join ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DEPUY_EVENT_LEADS_DATASOURCE lds on a.state_cd=lds.state_cd and (a.city_nm=lds.city_nm or a.zip_cd=lds.zip_cd) ");
		sql.append("and a.product_cd=lds.PRODUCT_CD and lds.event_postcard_id=? ");
		log.debug(sql + coords.toString());

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
			ProfileManager pm = ProfileManagerFactory.getInstance(getAttributes());
			while (rs.next()) {
//				String geo = rs.getString("geo_match_cd");
//				if ("noMatch".equals(geo)) continue;  //we don't want these, but they're too taxing to remove in the query
				
				//before we capture each record, determine if it falls within the date requirement. (<3mos, 3-6mos, <1yr, all)
				Date createDt = rs.getDate("attempt_dt");
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
				vo.setPrefixName(pm.getStringValue("PREFIX_NM", rs.getString("prefix_nm")));
				vo.setFirstName(pm.getStringValue("FIRST_NM", rs.getString("first_nm")));
				vo.setLastName(pm.getStringValue("LAST_NM", rs.getString("last_nm")));
				vo.setEmailAddress(pm.getStringValue("EMAIL_ADDRESS_TXT", rs.getString("EMAIL_ADDRESS_TXT")));
				vo.setSuffixName(pm.getStringValue("SUFFIX_NM", rs.getString("suffix_nm")));
				vo.setAddress(pm.getStringValue("ADDRESS_TXT", rs.getString("address_txt")));
				vo.setAddress2(pm.getStringValue("ADDRESS2_TXT", rs.getString("address2_txt")));
				vo.setCity(pm.getStringValue("CITY_NM", rs.getString("city_nm")));
				vo.setState(pm.getStringValue("STATE_CD", rs.getString("state_cd")));
				vo.setZipCode(pm.getStringValue("ZIP_CD", rs.getString("zip_cd")));
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
	
	
	/**
	 * removes duplicate entries to ensure multiple postcards aren't sent to the same person/address.
	 * accomplished by fName +lName + address1
	 * @param users
	 * @return
	 */
	protected List<UserDataVO> deduplicateUsers(List<UserDataVO> users) {
		Map<String, UserDataVO> data = new HashMap<>(users.size());
		String key = "";
		
		for (UserDataVO vo : users) {
			try {
				key = StringUtil.checkVal(vo.getFirstName(), vo.getEmailAddress())
						.concat(StringUtil.checkVal(vo.getLastName()))
						.concat(StringUtil.checkVal(vo.getAddress(), vo.getZipCode()));
			} catch (Exception e) {}
			
			if (data.containsKey(key)) {
				//persist the email and print flags for this (existing) user
				UserDataVO newUser = data.get(key);
				if (1 == vo.getGlobalAdminFlag()) newUser.setGlobalAdminFlag(1);
				if (1 == vo.getValidEmailFlag()) newUser.setValidEmailFlag(1);
				data.put(key, newUser);
			} else {
				data.put(key, vo);
			}
		}

		log.debug("filtered size: " + data.size());
		return new ArrayList<UserDataVO>(data.values());
	}
	
	
	/**
	 * Loads the same list of leads we'd mail to, then we sort them differently for display purposes
	 * The Comparator handles sorting the list so we can iterate it into html tables in the View.
	 * @param leads
	 * @throws ActionException
	 */
	public void targetLeads(DePuyEventSeminarVO sem, String sort) throws ActionException {
		log.debug("starting targetLeads");
		SortType sortType;
		try {
			sortType = SortType.valueOf(sort);
		} catch (Exception e) {
			sortType = SortType.county;
		}
		
		//we'll need this later, intitialize it
		uuid = new UUIDGenerator();
		
		//load the user base
		List<UserDataVO> leads = pullLeads(sem, ReportType.leads, null);
		
		//define data containers
		Map<Location, LeadCityVO> locnData  = new TreeMap<>(new LocationComparator());
		
		//put each user into the city bucket they belong in.
		for (UserDataVO user : leads) {
			Location loc = this.polishAddressData(user, sortType);
			LeadCityVO vo = locnData.get(loc);
			if (vo == null) vo = new LeadCityVO();
			vo.addLead(user, 1, isSelected(user)); //Alias holds 'checked' state, from the _XR table);  //holds maxAgeNo (range)
			locnData.put(loc, vo);
		}
		
		loadSoloKiosks(sortType, locnData);
		
		log.debug("data size: " + locnData.size());
		sem.setTargetLeads(locnData);
	}
	
	
	/**
	 * does a lookup of Solo kiosk that match the city/zip records we're about to display to the user
	 * @param sortType
	 * @param state
	 * @param cityZips
	 */
	private void loadSoloKiosks(SortType sortType, Map<Location, LeadCityVO> locnData) {
		List<String> unqStates = new ArrayList<>();
		
		StringBuilder sql = new StringBuilder(250);
		sql.append("select city_nm, state_cd, zip_cd from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DEPUY_SOLO_KIOSK where ");
		if (SortType.zip == sortType) {
			sql.append(" zip_cd in ('~'");
			for (int x=0; x<locnData.size(); x++) sql.append(",?");
			sql.append(")");
		} else {
			//query all cities by name and all states by name, then worry about marrying the two server-side afterwards
			sql.append("city_nm in ('~'");
			for (Location loc : locnData.keySet()) {
				sql.append(",?");
				unqStates.add(loc.getState()); //we'll need these below.  90% of cities belong to the same state, so we don't need to repeat state names for each. e.g. IN,IN,IN,IN
			}
			sql.append(") and state_cd in ('~'");
			for (int x=0; x<unqStates.size(); x++) sql.append(",?");
			sql.append(")");
		}
		log.debug(sql);
		
		int cnt=0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			if (SortType.zip == sortType) {
				for (Location loc: locnData.keySet())
					ps.setString(++cnt, loc.getCity());
			} else {
				for (Location loc: locnData.keySet())
					ps.setString(++cnt, loc.getCity());
				for (String state : unqStates)
					ps.setString(++cnt, state);
			}
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				//loop the locnData, find the city/state/zip that matches this RS record, and flag it as a kiosk location
				if (SortType.zip == sortType) {
					String zip = StringUtil.checkVal(rs.getString("zip_cd"));
					for (Location loc : locnData.keySet()) {
						if (zip.equals(loc.getCity())) {
							loc.setBarCodeId("1"); //trigger used by a <c:if> in the UI
							continue;
						}
					}
				} else {
					String city = StringUtil.checkVal(rs.getString("city_nm"));
					String state = StringUtil.checkVal(rs.getString("state_cd"));
					for (Location loc : locnData.keySet()) {
						if (city.equalsIgnoreCase(loc.getCity()) && state.equalsIgnoreCase(loc.getState()) ) {
							loc.setBarCodeId("1"); //trigger used by a <c:if> in the UI
							continue;
						}
					}
				}
			}
			
		} catch (SQLException sqle) {
			log.error("could not load Solo Kiosk list", sqle);
		}
	}
	
	/**
	 * determines if the given user's record should be checked on the form.
	 * @param user
	 * @return
	 */
	protected boolean isSelected(UserDataVO user) {
		boolean haveXR = user.getAliasName() != null;
		Integer age = (Integer) user.getAttributes().get("savedMaxAge");
		boolean ageMeetsSelection = (user.getBirthYear().intValue() == age.intValue());
		
		return haveXR && ageMeetsSelection;
	}
	
	
	/**
	 * normalizes text-case for the various address parts, so they're comparable by the Map's Comparator
	 * @param user
	 * @return
	 */
	protected Location polishAddressData(UserDataVO user, SortType sort) {
		Location loc = new Location();
		loc.setAddress(StringUtil.capitalizePhrase(user.getPassword())); //product
		loc.setCity(StringUtil.capitalizePhrase(user.getCity()));
		loc.setState(user.getState()); 
		
		//need to preserve stateCode for the form submission
		loc.setCountryName(getStateNameFromCode(user.getState()));
		
		//tweak some data depending on the SortType requested
		if (SortType.county == sort) {
			loc.setCounty(StringUtil.capitalizePhrase(user.getCounty()));
			if (StringUtil.isEmpty(loc.getCounty())) loc.setCounty("Unknown");
		} else if (SortType.zip == sort) {
			//stuff the zip into the city so the natural ordering of the rows is "by zip"
			loc.setCity(user.getZipCode());
		}
		
		loc.setLocationId(uuid.getUUID());
		loc.setZipCode(user.getZipCode());
		
		return loc;
	}
	
	/**
	 * returns the capitalized full name of the state for the code passed.  IN=>Indiana 
	 * @param cd
	 * @return
	 */
	protected String getStateNameFromCode(String cd) {
		//reverse the state list, but only once
		if (usStates == null) {
			usStates = new HashMap<String, String>(55, 100);
			StateList sl = SMTStateListFactory.getStateList("US");
			Map<Object, Object> lst = sl.getStateList();
			for (Object nm : lst.keySet())
				usStates.put((String)lst.get(nm), (String)nm);

		}
		return usStates.get(cd);
	}
	
	
	/**
	 * takes the age of the lead in months and returns one of four bucketed 
	 * values we're using to render the data.
	 * @param monthsOld
	 * @return
	 */
	protected int bucketizeLeadAge(int monthsOld) {
		if (monthsOld <= LeadTierOne) return LeadTierOne;
		else if (monthsOld <= LeadTierTwo) return LeadTierTwo;
		else if (monthsOld <= LeadTierThree) return LeadTierThree;
		else return LeadTierFour; //20yrs, which is the catch-all bucket
	}
	
	
	/**
	 * steps the attempt date forward in time to today, counting the # of months along the way
	 * @param attemptDt
	 * @return
	 */
	protected int differenceInMonths(Date attemptDt) {
		Calendar today = Calendar.getInstance();
		Calendar c1 = Calendar.getInstance();
		c1.setTime(attemptDt);
		
		int diff = 0;
		while (today.after(c1)) {
			diff++;
			c1.add(Calendar.MONTH, 1);
		}
		//log.debug("att=" + attemptDt + ", dif=" + diff);
		return diff;
	}

	/**
	 * **************************************************************************
	 * <b>Title</b>: LeadsDataToolV2.LocationComparator.java<p/>
	 * <b>Description: compares Location objects using their complete address.
	 * NOTE: loc1 & loc2 are specifically built in this hierarchy to ensure colation and alphabetical listing on the website.</b> 
	 * <p/>
	 * <b>Copyright:</b> Copyright (c) 2014<p/>
	 * <b>Company:</b> Silicon Mountain Technologies<p/>
	 * @author James McKain
	 * @version 1.0
	 * @since Jan 21, 2014
	 ***************************************************************************
	 */
	public class LocationComparator implements Comparator<Location> {
		@Override
		public int compare(Location l1, Location l2) {
			String loc1 = l1.getState() + l1.getCounty() + l1.getCity() + l1.getAddress();
			String loc2 = l2.getState() + l2.getCounty() + l2.getCity() + l2.getAddress();
			return loc1.compareTo(loc2);
		}
	}
}