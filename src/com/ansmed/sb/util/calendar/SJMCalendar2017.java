package com.ansmed.sb.util.calendar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SJMCalendar2017 implements SJMCalendar {
		
	public SJMCalendar2017() {
		
	}
	
	public Map<String,List<SJMDateBean>> getSJMCalendarYear() {
		
		SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		Map<String,List<SJMDateBean>> dateMap = new HashMap<String,List<SJMDateBean>>();
		List<SJMDateBean> months = new ArrayList<SJMDateBean>();
		List<SJMDateBean> quarters = new ArrayList<SJMDateBean>();
		List<SJMDateBean> year = new ArrayList<SJMDateBean>();
		
		try {
			// load month start/end values
			months.add(new SJMDateBean(s.parse("2017-01-01 00:00:00"),s.parse("2017-01-28 23:59:59")));
			months.add(new SJMDateBean(s.parse("2017-01-29 00:00:00"),s.parse("2017-03-04 23:59:59")));
			months.add(new SJMDateBean(s.parse("2017-03-05 00:00:00"),s.parse("2017-04-01 23:59:59")));
			months.add(new SJMDateBean(s.parse("2017-04-02 00:00:00"),s.parse("2017-04-29 23:59:59")));
			months.add(new SJMDateBean(s.parse("2017-04-30 00:00:00"),s.parse("2017-06-03 23:59:59")));
			months.add(new SJMDateBean(s.parse("2017-06-04 00:00:00"),s.parse("2017-07-01 23:59:59")));
			months.add(new SJMDateBean(s.parse("2017-07-02 00:00:00"),s.parse("2017-07-29 23:59:59")));
			months.add(new SJMDateBean(s.parse("2017-07-30 00:00:00"),s.parse("2017-09-02 23:59:59")));
			months.add(new SJMDateBean(s.parse("2017-09-03 00:00:00"),s.parse("2017-09-30 23:59:59")));
			months.add(new SJMDateBean(s.parse("2017-10-01 00:00:00"),s.parse("2017-10-28 23:59:59")));
			months.add(new SJMDateBean(s.parse("2017-10-29 00:00:00"),s.parse("2017-12-02 23:59:59")));
			months.add(new SJMDateBean(s.parse("2017-12-03 00:00:00"),s.parse("2017-12-30 23:59:59")));
					
			// load quarterly start/end values
			quarters.add(new SJMDateBean(s.parse("2017-01-01 00:00:00"),s.parse("2017-04-01 23:59:59")));
			quarters.add(new SJMDateBean(s.parse("2017-04-02 00:00:00"),s.parse("2017-07-01 23:59:59")));
			quarters.add(new SJMDateBean(s.parse("2017-07-02 00:00:00"),s.parse("2017-09-30 23:59:59")));
			quarters.add(new SJMDateBean(s.parse("2017-10-01 00:00:00"),s.parse("2017-12-30 23:59:59")));
			
			// load year start/end values
			year.add(new SJMDateBean(s.parse("2017-01-01 00:00:00"),s.parse("2017-12-30 23:59:59")));
			
		} catch (ParseException pe) {
			//log.error("Error parsing SJM business calendar date. ", pe); 
		}
		
		dateMap.put("months",months);
		dateMap.put("quarters",quarters);
		dateMap.put("year",year);
		
		return dateMap;
	}

}
