package com.ansmed.sb.util.calendar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SJMCalendar2014 implements SJMCalendar {
		
	public SJMCalendar2014() {
		
	}
	
	public Map<String,List<SJMDateBean>> getSJMCalendarYear() {
		
		SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		Map<String,List<SJMDateBean>> dateMap = new HashMap<String,List<SJMDateBean>>();
		List<SJMDateBean> months = new ArrayList<SJMDateBean>();
		List<SJMDateBean> quarters = new ArrayList<SJMDateBean>();
		List<SJMDateBean> year = new ArrayList<SJMDateBean>();
		
		try {
			// load month start/end values
			months.add(new SJMDateBean(s.parse("2013-12-29 00:00:00"),s.parse("2014-01-25 23:59:59")));
			months.add(new SJMDateBean(s.parse("2014-01-26 00:00:00"),s.parse("2014-03-01 23:59:59")));
			months.add(new SJMDateBean(s.parse("2014-03-02 00:00:00"),s.parse("2014-03-29 23:59:59")));
			months.add(new SJMDateBean(s.parse("2014-03-30 00:00:00"),s.parse("2014-04-26 23:59:59")));
			months.add(new SJMDateBean(s.parse("2014-04-27 00:00:00"),s.parse("2014-05-31 23:59:59")));
			months.add(new SJMDateBean(s.parse("2014-06-01 00:00:00"),s.parse("2014-06-28 23:59:59")));
			months.add(new SJMDateBean(s.parse("2014-06-29 00:00:00"),s.parse("2014-07-26 23:59:59")));
			months.add(new SJMDateBean(s.parse("2014-07-27 00:00:00"),s.parse("2014-08-30 23:59:59")));
			months.add(new SJMDateBean(s.parse("2014-08-31 00:00:00"),s.parse("2014-09-27 23:59:59")));
			months.add(new SJMDateBean(s.parse("2014-09-28 00:00:00"),s.parse("2014-10-25 23:59:59")));
			months.add(new SJMDateBean(s.parse("2014-10-26 00:00:00"),s.parse("2014-11-29 23:59:59")));
			months.add(new SJMDateBean(s.parse("2014-11-30 00:00:00"),s.parse("2015-01-03 23:59:59")));
					
			// load quarterly start/end values
			quarters.add(new SJMDateBean(s.parse("2013-12-29 00:00:00"),s.parse("2014-03-29 23:59:59")));
			quarters.add(new SJMDateBean(s.parse("2014-03-30 00:00:00"),s.parse("2014-06-28 23:59:59")));
			quarters.add(new SJMDateBean(s.parse("2014-06-29 00:00:00"),s.parse("2014-09-27 23:59:59")));
			quarters.add(new SJMDateBean(s.parse("2014-09-28 00:00:00"),s.parse("2015-01-03 23:59:59")));
			
			// load year start/end values
			year.add(new SJMDateBean(s.parse("2013-12-29 00:00:00"),s.parse("2015-01-03 23:59:59")));
			
		} catch (ParseException pe) {
			//log.error("Error parsing SJM business calendar date. ", pe); 
		}
		
		dateMap.put("months",months);
		dateMap.put("quarters",quarters);
		dateMap.put("year",year);
		
		return dateMap;
	}

}
