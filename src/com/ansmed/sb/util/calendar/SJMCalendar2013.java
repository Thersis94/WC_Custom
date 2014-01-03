package com.ansmed.sb.util.calendar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SJMCalendar2013 implements SJMCalendar {
		
	public SJMCalendar2013() {
		
	}
	
	public Map<String,List<SJMDateBean>> getSJMCalendarYear() {
		
		SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		Map<String,List<SJMDateBean>> dateMap = new HashMap<String,List<SJMDateBean>>();
		List<SJMDateBean> months = new ArrayList<SJMDateBean>();
		List<SJMDateBean> quarters = new ArrayList<SJMDateBean>();
		List<SJMDateBean> year = new ArrayList<SJMDateBean>();
		
		try {
			// load month start/end values
			months.add(new SJMDateBean(s.parse("2012-12-30 00:00:00"),s.parse("2013-01-26 23:59:59")));
			months.add(new SJMDateBean(s.parse("2013-01-27 00:00:00"),s.parse("2013-03-02 23:59:59")));
			months.add(new SJMDateBean(s.parse("2013-03-03 00:00:00"),s.parse("2013-03-30 23:59:59")));
			months.add(new SJMDateBean(s.parse("2013-03-31 00:00:00"),s.parse("2013-04-27 23:59:59")));
			months.add(new SJMDateBean(s.parse("2013-04-28 00:00:00"),s.parse("2013-06-01 23:59:59")));
			months.add(new SJMDateBean(s.parse("2013-06-02 00:00:00"),s.parse("2013-06-29 23:59:59")));
			months.add(new SJMDateBean(s.parse("2013-06-30 00:00:00"),s.parse("2013-07-27 23:59:59")));
			months.add(new SJMDateBean(s.parse("2013-07-28 00:00:00"),s.parse("2013-08-31 23:59:59")));
			months.add(new SJMDateBean(s.parse("2013-09-01 00:00:00"),s.parse("2013-09-28 23:59:59")));
			months.add(new SJMDateBean(s.parse("2013-09-29 00:00:00"),s.parse("2013-10-26 23:59:59")));
			months.add(new SJMDateBean(s.parse("2013-10-27 00:00:00"),s.parse("2013-11-30 23:59:59")));
			months.add(new SJMDateBean(s.parse("2013-12-01 00:00:00"),s.parse("2013-12-28 23:59:59")));
						
			// load quarterly start/end values
			quarters.add(new SJMDateBean(s.parse("2012-12-30 00:00:00"),s.parse("2013-03-30 23:59:59")));
			quarters.add(new SJMDateBean(s.parse("2013-03-31 00:00:00"),s.parse("2013-06-29 23:59:59")));
			quarters.add(new SJMDateBean(s.parse("2013-06-30 00:00:00"),s.parse("2013-09-28 23:59:59")));
			quarters.add(new SJMDateBean(s.parse("2013-09-29 00:00:00"),s.parse("2013-12-28 23:59:59")));
			
			// load year start/end values
			year.add(new SJMDateBean(s.parse("2012-12-30 00:00:00"),s.parse("2013-12-28 23:59:59")));
			
		} catch (ParseException pe) {
			//log.error("Error parsing SJM business calendar date. ", pe); 
		}
		
		dateMap.put("months",months);
		dateMap.put("quarters",quarters);
		dateMap.put("year",year);
		
		return dateMap;
	}

}
