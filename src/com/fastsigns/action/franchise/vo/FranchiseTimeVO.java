package com.fastsigns.action.franchise.vo;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: FranchiseTimeVO.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Bean and Formatter for Center Times.  Handles taking in 
 * a raw map of Times and combines them into a formatted map of timeslot and the
 * days that mathch that slot.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Billy Larsen
 * @version 1.0
 * @since Nov 14, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class FranchiseTimeVO implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Enumeration of Time types that are used in this class.
	 */
	public enum DayType {
		M_1, M_2, T_1, T_2, W_1, W_2, Th_1, Th_2, F_1, F_2, Sat_1, Sat_2,Sun_1, Sun_2
	}

	/**
	 * rawTimes holds unformatted time slots as they are entered in the class.
	 * sortedTimes holds formatted time slots combined by time and day
	 * timeStr holds the generic formatted text that is retrieved by the jsp
	 * times is a static map holding the Options List of time fields.
	 */
	private Map<DayType, String> rawTimes;
	private Map<String, String> sortedTimes;
	private String timeStr = "";
	private List<String> times = null;

	/**
	 * This constructor redirects all legacy calls to the new method
	 * @param times
	 */
	public FranchiseTimeVO(Map<DayType, String> times) {
		this(times, false);
	}
	
	/**
	 * Constructor that handles taking in a Map of Times, combines start and end
	 * times, sorts them and combines like time slots together and builds the default
	 * string so the jsp has less work to do.
	 * @param times
	 */
	public FranchiseTimeVO(Map<DayType, String> times, boolean useMilitaryTime) {
		this.times = buildTimes(useMilitaryTime);
		rawTimes = times;
		combineTimes();
		sortTimes();
		timeStr = buildTimeStr();
	}
	
	public FranchiseTimeVO(){
		this.times = buildTimes(false);
	}

	/**
	 * Adds the times to the Sorted times list.  This combines start and end times
	 * in a generic manner and stores them with the Day Abbreviation as the key.
	 */
	private void combineTimes() {
		sortedTimes = new LinkedHashMap<String, String>();
		if(rawTimes.containsKey(DayType.M_1))
			sortedTimes.put("M", rawTimes.get(DayType.M_1) + "-" + rawTimes.get(DayType.M_2));
		if(rawTimes.containsKey(DayType.T_1))
			sortedTimes.put("T", rawTimes.get(DayType.T_1) + "-" + rawTimes.get(DayType.T_2));
		if(rawTimes.containsKey(DayType.W_1))
			sortedTimes.put("W", rawTimes.get(DayType.W_1) + "-" + rawTimes.get(DayType.W_2));
		if(rawTimes.containsKey(DayType.Th_1))
			sortedTimes.put("Th", rawTimes.get(DayType.Th_1) + "-" + rawTimes.get(DayType.Th_2));
		if(rawTimes.containsKey(DayType.F_1))
			sortedTimes.put("F", rawTimes.get(DayType.F_1) + "-" + rawTimes.get(DayType.F_2));
		if(rawTimes.containsKey(DayType.Sun_1))
			sortedTimes.put("Sun", rawTimes.get(DayType.Sun_1) + "-" + rawTimes.get(DayType.Sun_2));
		if(rawTimes.containsKey(DayType.Sat_1))
			sortedTimes.put("Sat", rawTimes.get(DayType.Sat_1) + "-" + rawTimes.get(DayType.Sat_2));
	}

	/**
	 * Sorts the raw times into clusters of <Time, Day>
	 */
	private void sortTimes() {
		Map<String, String> combinedTimes = sortedTimes;
		sortedTimes = new LinkedHashMap<String, String>();
		boolean newSet = false;
		boolean continuous = false;
		
		for (String s : combinedTimes.keySet()) {
			String key = combinedTimes.get(s);
			if (sortedTimes.containsKey(key)) {
				String day = sortedTimes.get(key);				
				// Check see if we are starting a new series of days.
				// This ensures that each set of days will remain separated by commas
				if ((!continuous || day.substring(day.lastIndexOf(",")+1, day.length()).contains("-")) && newSet) {
					sortedTimes.put(key, day + "," + s);
					newSet = true;
				} else if (newSet){
					// If we are working with a new set of days we need to add this to the key
					// with a dash to indicate the continuity of the days
					sortedTimes.put(key, day + "-" + s);
					newSet = false;
				} else {
					// Since we are not dealing with a new set of days we need to take all but
					// the last day of the current set and replace the old last day with the one
					// we are working with right now.
					sortedTimes.put(key, day.substring(0,day.lastIndexOf('-')+1) + s);
					newSet = false;
				}
				continuous = true;
			} else {
				sortedTimes.put(key, s);
				
				continuous = !newSet;
				newSet = true;
			}
		}
	}

	/**
	 * Handles generic formatting of the times.
	 * @return
	 */
	private String buildTimeStr() {
		StringBuilder time = new StringBuilder();
		if(sortedTimes.size() > 0)
		for (String s : sortedTimes.keySet()) {
			if(StringUtil.checkVal(s).length() > 0 && StringUtil.checkVal(sortedTimes.get(s)).length() > 0){
			time.append("<span class=\"days\">").append(sortedTimes.get(s)).append("</span> : <span class=\"times\">");
			time.append(s).append("</span><br/>");
			}
		}
		return time.toString();
	}

	/**
	 * Returns the formatted time after it has been pre-processed on the server.
	 * @return
	 */
	public String getFormattedTime() {
		return timeStr;
	}
	
	/**
	 * Returns the map of sorted times so they can be formatted as needed in the
	 * jsp.
	 * @return
	 */
	public Map<String, String> getSortedTimes(){
		return sortedTimes;
	}

	/**
	 * Localised method for adding times to the correct map.
	 * @param key
	 * @param value
	 */
	private void addTime(DayType key, String value) {
		rawTimes.put(key, value);
	}

	/**
	 * This method builds the option lists for webedit.  It takes in a DayType and
	 * checks to see if that day has been set.  If so we set that option to be
	 * selected.
	 * @param key
	 * @return
	 */
	private String getList(DayType key) {
		StringBuilder sb = new StringBuilder();
		for (String s : times) {
			sb.append("<option value=\"").append(s).append("\"");
			if (s.equals(rawTimes.get(key)))
				sb.append("selected=\"selected\"");
			sb.append(">").append(s).append("</option>");
		}
		return sb.toString();
	}

	/**
	 * The following methods are all easy accessors for webedit location info form
	 * They handle building the Options lists and setting the default selected.
	 * @return
	 */
	public String getMonStart() {
		return getList(DayType.M_1);
	}

	public String getMonEnd() {
		return getList(DayType.M_2);
	}

	public String getTuesStart() {
		return getList(DayType.T_1);
	}

	public String getTuesEnd() {
		return getList(DayType.T_2);
	}

	public String getWedStart() {
		return getList(DayType.W_1);
	}

	public String getWedEnd() {
		return getList(DayType.W_2);
	}

	public String getThursStart() {
		return getList(DayType.Th_1);
	}

	public String getThursEnd() {
		return getList(DayType.Th_2);
	}

	public String getFriStart() {
		return getList(DayType.F_1);
	}

	public String getFriEnd() {
		return getList(DayType.F_2);
	}

	/**
	 * List of times used for generating webedit dropdowns.
	 * @return
	 */
	private static List<String> buildTimes(boolean useMilitaryTime) {
		
		List<String> t = new ArrayList<String>(55);
		
		Locale locale = new Locale("en", "US");
		GregorianCalendar cal = new GregorianCalendar(locale);
		SimpleDateFormat sdf = null;
		
		//uses simple date formatter to set up how a 24hr day and a 12hr day will
		//be displayed 
		if (useMilitaryTime) {
			sdf = new SimpleDateFormat("HHmm");
		}else{
			sdf = new SimpleDateFormat("h:mm a");
		}
		
		//sets the calendars time to midnight
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		
		//adds the existing empty row
		t.add("");
		//loops every half hour adding a new option to the list
	       for(int i=1; i<49; i++){
	    	   t.add(sdf.format(cal.getTime()));
	    	   cal.add(Calendar.MINUTE,  30);
	         }

		return t;
	}
	
	/**
	 * This method allows for a jsp to specify a Map of Times and will handle
	 * the processing required.
	 * @param times
	 */
	public void setRawTimes(Map<DayType, String> times){
		rawTimes = times;
		combineTimes();
		sortTimes();
		timeStr = buildTimeStr();
	}
	
	/**
	 * This method allows for a jsp to specify a Map of Times and will handle
	 * the processing required.
	 * @param t
	 */
	public void setTimes(Map<String, String> t) {
		rawTimes = new LinkedHashMap<DayType, String>();
		if(t == null)
			return;
		for(String s : t.keySet()){
			String k = StringUtil.replace(s, "lAttr_", "");
			if(k.length() > 0 && isDayType(k))
			this.addTime(DayType.valueOf(k), t.get(s));
		}
		combineTimes();
		sortTimes();
		timeStr = buildTimeStr();
	}
	
	/**
	 * Checks to make sure a given string is in the DayType Enumeration.
	 * @param s
	 * @return
	 */
	private static boolean isDayType(String s){
		try{
			DayType.valueOf(s);
		} catch(Exception e){
			return false;
		}
		return true;
	}

}
