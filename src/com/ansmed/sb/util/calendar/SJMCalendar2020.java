package com.ansmed.sb.util.calendar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SJMCalendar2020 implements SJMCalendar {
		
	public SJMCalendar2020() {
		
	}
	
	public Map<String,List<SJMDateBean>> getSJMCalendarYear() {
		
		SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		Map<String,List<SJMDateBean>> dateMap = new HashMap<String,List<SJMDateBean>>();
		List<SJMDateBean> months = new ArrayList<SJMDateBean>();
		List<SJMDateBean> quarters = new ArrayList<SJMDateBean>();
		List<SJMDateBean> year = new ArrayList<SJMDateBean>();
		
		try {
			// load month start/end values
			months.add(new SJMDateBean(s.parse("2019-12-29 00:00:00"),s.parse("2020-01-25 23:59:59")));
			months.add(new SJMDateBean(s.parse("2020-01-26 00:00:00"),s.parse("2020-02-29 23:59:59")));
			months.add(new SJMDateBean(s.parse("2020-03-01 00:00:00"),s.parse("2020-03-28 23:59:59")));
			months.add(new SJMDateBean(s.parse("2020-03-29 00:00:00"),s.parse("2020-04-25 23:59:59")));
			months.add(new SJMDateBean(s.parse("2020-04-26 00:00:00"),s.parse("2020-05-30 23:59:59")));
			months.add(new SJMDateBean(s.parse("2020-05-31 00:00:00"),s.parse("2020-06-27 23:59:59")));
			months.add(new SJMDateBean(s.parse("2020-06-28 00:00:00"),s.parse("2020-07-25 23:59:59")));
			months.add(new SJMDateBean(s.parse("2020-07-26 00:00:00"),s.parse("2020-08-29 23:59:59")));
			months.add(new SJMDateBean(s.parse("2020-08-30 00:00:00"),s.parse("2020-09-26 23:59:59")));
			months.add(new SJMDateBean(s.parse("2020-09-27 00:00:00"),s.parse("2020-10-24 23:59:59")));
			months.add(new SJMDateBean(s.parse("2020-10-25 00:00:00"),s.parse("2020-11-28 23:59:59")));
			months.add(new SJMDateBean(s.parse("2020-11-29 00:00:00"),s.parse("2021-01-02 23:59:59")));
					
			// load quarterly start/end values
			quarters.add(new SJMDateBean(s.parse("2019-12-29 00:00:00"),s.parse("2020-03-28 23:59:59")));
			quarters.add(new SJMDateBean(s.parse("2020-03-29 00:00:00"),s.parse("2020-06-27 23:59:59")));
			quarters.add(new SJMDateBean(s.parse("2020-06-28 00:00:00"),s.parse("2020-09-26 23:59:59")));
			quarters.add(new SJMDateBean(s.parse("2020-09-27 00:00:00"),s.parse("2021-01-02 23:59:59")));
			
			// load year start/end values
			year.add(new SJMDateBean(s.parse("2019-12-29 00:00:00"),s.parse("2021-01-02 23:59:59")));
			
		} catch (ParseException pe) {
			//log.error("Error parsing SJM business calendar date. ", pe); 
		}
		
		dateMap.put("months",months);
		dateMap.put("quarters",quarters);
		dateMap.put("year",year);
		
		return dateMap;
	}

}
