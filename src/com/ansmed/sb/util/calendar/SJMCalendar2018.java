package com.ansmed.sb.util.calendar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SJMCalendar2018 implements SJMCalendar {
		
	public SJMCalendar2018() {
		
	}
	
	public Map<String,List<SJMDateBean>> getSJMCalendarYear() {
		
		SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		Map<String,List<SJMDateBean>> dateMap = new HashMap<String,List<SJMDateBean>>();
		List<SJMDateBean> months = new ArrayList<SJMDateBean>();
		List<SJMDateBean> quarters = new ArrayList<SJMDateBean>();
		List<SJMDateBean> year = new ArrayList<SJMDateBean>();
		
		try {
			// load month start/end values
			months.add(new SJMDateBean(s.parse("2017-12-31 00:00:00"),s.parse("2018-01-27 23:59:59")));
			months.add(new SJMDateBean(s.parse("2018-01-28 00:00:00"),s.parse("2018-03-03 23:59:59")));
			months.add(new SJMDateBean(s.parse("2018-03-04 00:00:00"),s.parse("2018-03-31 23:59:59")));
			months.add(new SJMDateBean(s.parse("2018-04-01 00:00:00"),s.parse("2018-04-28 23:59:59")));
			months.add(new SJMDateBean(s.parse("2018-04-29 00:00:00"),s.parse("2018-06-02 23:59:59")));
			months.add(new SJMDateBean(s.parse("2018-06-03 00:00:00"),s.parse("2018-06-30 23:59:59")));
			months.add(new SJMDateBean(s.parse("2018-07-01 00:00:00"),s.parse("2018-07-28 23:59:59")));
			months.add(new SJMDateBean(s.parse("2018-07-29 00:00:00"),s.parse("2018-09-01 23:59:59")));
			months.add(new SJMDateBean(s.parse("2018-09-02 00:00:00"),s.parse("2018-09-29 23:59:59")));
			months.add(new SJMDateBean(s.parse("2018-09-30 00:00:00"),s.parse("2018-10-27 23:59:59")));
			months.add(new SJMDateBean(s.parse("2018-10-28 00:00:00"),s.parse("2018-12-01 23:59:59")));
			months.add(new SJMDateBean(s.parse("2018-12-02 00:00:00"),s.parse("2018-12-29 23:59:59")));
					
			// load quarterly start/end values
			quarters.add(new SJMDateBean(s.parse("2017-12-31 00:00:00"),s.parse("2018-03-31 23:59:59")));
			quarters.add(new SJMDateBean(s.parse("2018-04-01 00:00:00"),s.parse("2018-06-30 23:59:59")));
			quarters.add(new SJMDateBean(s.parse("2018-07-01 00:00:00"),s.parse("2018-09-29 23:59:59")));
			quarters.add(new SJMDateBean(s.parse("2018-09-30 00:00:00"),s.parse("2018-12-29 23:59:59")));
			
			// load year start/end values
			year.add(new SJMDateBean(s.parse("2017-12-31 00:00:00"),s.parse("2018-12-29 23:59:59")));
			
		} catch (ParseException pe) {
			//log.error("Error parsing SJM business calendar date. ", pe); 
		}
		
		dateMap.put("months",months);
		dateMap.put("quarters",quarters);
		dateMap.put("year",year);
		
		return dateMap;
	}

}
