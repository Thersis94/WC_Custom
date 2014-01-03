package com.ansmed.sb.util.calendar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SJMCalendar2012 implements SJMCalendar {
		
	public SJMCalendar2012() {
		
	}
	
	public Map<String,List<SJMDateBean>> getSJMCalendarYear() {
		
		SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		Map<String,List<SJMDateBean>> dateMap = new HashMap<String,List<SJMDateBean>>();
		List<SJMDateBean> months = new ArrayList<SJMDateBean>();
		List<SJMDateBean> quarters = new ArrayList<SJMDateBean>();
		List<SJMDateBean> year = new ArrayList<SJMDateBean>();
		
		try {
			// load month start/end values
			months.add(new SJMDateBean(s.parse("2012-01-01 00:00:00"),s.parse("2012-01-28 23:59:59")));
			months.add(new SJMDateBean(s.parse("2012-01-29 00:00:00"),s.parse("2012-03-03 23:59:59")));
			months.add(new SJMDateBean(s.parse("2012-03-04 00:00:00"),s.parse("2012-03-31 23:59:59")));
			months.add(new SJMDateBean(s.parse("2012-04-01 00:00:00"),s.parse("2012-04-28 23:59:59")));
			months.add(new SJMDateBean(s.parse("2012-04-29 00:00:00"),s.parse("2012-06-02 23:59:59")));
			months.add(new SJMDateBean(s.parse("2012-06-03 00:00:00"),s.parse("2012-06-30 23:59:59")));
			months.add(new SJMDateBean(s.parse("2012-07-01 00:00:00"),s.parse("2012-07-28 23:59:59")));
			months.add(new SJMDateBean(s.parse("2012-07-29 00:00:00"),s.parse("2012-09-01 23:59:59")));
			months.add(new SJMDateBean(s.parse("2012-09-02 00:00:00"),s.parse("2012-09-29 23:59:59")));
			months.add(new SJMDateBean(s.parse("2012-09-30 00:00:00"),s.parse("2012-10-27 23:59:59")));
			months.add(new SJMDateBean(s.parse("2012-10-28 00:00:00"),s.parse("2012-12-01 23:59:59")));
			months.add(new SJMDateBean(s.parse("2012-12-02 00:00:00"),s.parse("2012-12-29 23:59:59")));
						
			// load quarterly start/end values
			quarters.add(new SJMDateBean(s.parse("2012-01-01 00:00:00"),s.parse("2012-03-31 23:59:59")));
			quarters.add(new SJMDateBean(s.parse("2012-04-01 00:00:00"),s.parse("2012-06-30 23:59:59")));
			quarters.add(new SJMDateBean(s.parse("2012-07-01 00:00:00"),s.parse("2012-09-29 23:59:59")));
			quarters.add(new SJMDateBean(s.parse("2012-09-30 00:00:00"),s.parse("2012-12-29 23:59:59")));
			
			// load year start/end values
			year.add(new SJMDateBean(s.parse("2012-01-01 00:00:00"),s.parse("2012-12-29 23:59:59")));
			
		} catch (ParseException pe) {
			//log.error("Error parsing SJM business calendar date. ", pe); 
		}
		
		dateMap.put("months",months);
		dateMap.put("quarters",quarters);
		dateMap.put("year",year);
		
		return dateMap;
	}

}
