package com.ansmed.sb.util.calendar;

//JDK 1.6
import java.util.List;
import java.util.Map;

/*****************************************************************************
<p><b>Title</b>: SJMCalendar.java</p>
<p>SJM business calendar interface.</p>
<p>Copyright: Copyright (c) 2009 SMT, All Rights Reserved</p>
<p>Company: Silicon Mountain Technologies</p>
@author Dave Bargerhuff
@version 1.0
@since Aug 21, 2009
***************************************************************************/
public interface SJMCalendar {
	
	public Map<String,List<SJMDateBean>> getSJMCalendarYear();

}
