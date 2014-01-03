package com.ansmed.sb.util.calendar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SJMCalendar2009 implements SJMCalendar {
		
	public SJMCalendar2009() {
		
	}
	
	public Map<String,List<SJMDateBean>> getSJMCalendarYear() {
		
		SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		Map<String,List<SJMDateBean>> dateMap = new HashMap<String,List<SJMDateBean>>();
		List<SJMDateBean> months = new ArrayList<SJMDateBean>();
		List<SJMDateBean> quarters = new ArrayList<SJMDateBean>();
		List<SJMDateBean> year = new ArrayList<SJMDateBean>();
		
		try {
			// load month start/end values
			months.add(new SJMDateBean(s.parse("2009-01-04 00:00:00"),s.parse("2009-01-31 23:59:59")));
			months.add(new SJMDateBean(s.parse("2009-02-01 00:00:00"),s.parse("2009-03-07 23:59:59")));
			months.add(new SJMDateBean(s.parse("2009-03-08 00:00:00"),s.parse("2009-04-04 23:59:59")));
			months.add(new SJMDateBean(s.parse("2009-04-05 00:00:00"),s.parse("2009-05-02 23:59:59")));
			months.add(new SJMDateBean(s.parse("2009-05-03 00:00:00"),s.parse("2009-06-06 23:59:59")));
			months.add(new SJMDateBean(s.parse("2009-06-07 00:00:00"),s.parse("2009-07-04 23:59:59")));
			months.add(new SJMDateBean(s.parse("2009-07-05 00:00:00"),s.parse("2009-08-01 23:59:59")));
			months.add(new SJMDateBean(s.parse("2009-08-02 00:00:00"),s.parse("2009-09-05 23:59:59")));
			months.add(new SJMDateBean(s.parse("2009-09-06 00:00:00"),s.parse("2009-10-03 23:59:59")));
			months.add(new SJMDateBean(s.parse("2009-10-04 00:00:00"),s.parse("2009-10-31 23:59:59")));
			months.add(new SJMDateBean(s.parse("2009-11-01 00:00:00"),s.parse("2009-12-05 23:59:59")));
			months.add(new SJMDateBean(s.parse("2009-12-06 00:00:00"),s.parse("2010-01-02 23:59:59")));
			
			// load quarterly start/end values
			quarters.add(new SJMDateBean(s.parse("2009-01-04 00:00:00"),s.parse("2009-04-04 23:59:59")));
			quarters.add(new SJMDateBean(s.parse("2009-04-05 00:00:00"),s.parse("2009-07-04 23:59:59")));
			quarters.add(new SJMDateBean(s.parse("2009-07-05 00:00:00"),s.parse("2009-10-03 23:59:59")));
			quarters.add(new SJMDateBean(s.parse("2009-10-04 00:00:00"),s.parse("2010-01-02 23:59:59")));
			
			// load year start/end values
			year.add(new SJMDateBean(s.parse("2009-01-04 00:00:00"),s.parse("2010-01-02 23:59:59")));
			
		} catch (ParseException pe) {
			//log.error("Error parsing SJM business calendar date. ", pe); 
		}
		
		dateMap.put("months",months);
		dateMap.put("quarters",quarters);
		dateMap.put("year",year);
		
		return dateMap;
	}

}
