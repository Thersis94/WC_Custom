package com.ansmed.sb.util.calendar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SJMCalendar2011 implements SJMCalendar {
		
	public SJMCalendar2011() {
		
	}
	
	public Map<String,List<SJMDateBean>> getSJMCalendarYear() {
		
		SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		Map<String,List<SJMDateBean>> dateMap = new HashMap<String,List<SJMDateBean>>();
		List<SJMDateBean> months = new ArrayList<SJMDateBean>();
		List<SJMDateBean> quarters = new ArrayList<SJMDateBean>();
		List<SJMDateBean> year = new ArrayList<SJMDateBean>();
		
		try {
			// load month start/end values
			months.add(new SJMDateBean(s.parse("2011-01-02 00:00:00"),s.parse("2011-01-29 23:59:59")));
			months.add(new SJMDateBean(s.parse("2011-01-30 00:00:00"),s.parse("2011-03-05 23:59:59")));
			months.add(new SJMDateBean(s.parse("2011-03-06 00:00:00"),s.parse("2011-04-02 23:59:59")));
			months.add(new SJMDateBean(s.parse("2011-04-03 00:00:00"),s.parse("2011-04-30 23:59:59")));
			months.add(new SJMDateBean(s.parse("2011-05-01 00:00:00"),s.parse("2011-06-04 23:59:59")));
			months.add(new SJMDateBean(s.parse("2011-06-05 00:00:00"),s.parse("2011-07-02 23:59:59")));
			months.add(new SJMDateBean(s.parse("2011-07-03 00:00:00"),s.parse("2011-07-30 23:59:59")));
			months.add(new SJMDateBean(s.parse("2011-07-31 00:00:00"),s.parse("2011-09-03 23:59:59")));
			months.add(new SJMDateBean(s.parse("2011-09-04 00:00:00"),s.parse("2011-10-01 23:59:59")));
			months.add(new SJMDateBean(s.parse("2011-10-02 00:00:00"),s.parse("2011-10-29 23:59:59")));
			months.add(new SJMDateBean(s.parse("2011-10-30 00:00:00"),s.parse("2011-12-03 23:59:59")));
			months.add(new SJMDateBean(s.parse("2011-12-04 00:00:00"),s.parse("2011-12-31 23:59:59")));
						
			// load quarterly start/end values
			quarters.add(new SJMDateBean(s.parse("2011-01-02 00:00:00"),s.parse("2011-04-02 23:59:59")));
			quarters.add(new SJMDateBean(s.parse("2011-04-03 00:00:00"),s.parse("2011-07-02 23:59:59")));
			quarters.add(new SJMDateBean(s.parse("2011-07-03 00:00:00"),s.parse("2011-10-01 23:59:59")));
			quarters.add(new SJMDateBean(s.parse("2011-10-02 00:00:00"),s.parse("2011-12-31 23:59:59")));
			
			// load year start/end values
			year.add(new SJMDateBean(s.parse("2011-01-02 00:00:00"),s.parse("2011-12-31 23:59:59")));
			
		} catch (ParseException pe) {
			//log.error("Error parsing SJM business calendar date. ", pe); 
		}
		
		dateMap.put("months",months);
		dateMap.put("quarters",quarters);
		dateMap.put("year",year);
		
		return dateMap;
	}

}
