package com.depuysynthes.huddle;

/****************************************************************************
 * <b>Title</b>: HTMLUtilsBean.java<p/>
 * <b>Description: contains shareThisPage and addToCalendar html rendering functions.
 * These features are reused site-wide, so they were put in a bean for abstraction.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 1, 2016
 ****************************************************************************/
public class HTMLUtilsBean {
	
	/**
	 * renders the 'share on' html dropdown menu
	 * @param pageUrl
	 * @return
	 */
	public static String getShareOn(String pageUrl) {
		StringBuilder sb = new StringBuilder(250);
		sb.append("<li><a href=\"javascript:;\" onclick=\"emailAFriend.shareEmail('").append(pageUrl).append("');\"><i class=\"fa fa-envelope\"></i> Share</a></li>");
		return sb.toString();
	}
	
	
	/**
	 * renders the html for Event-to-calendar adding 
	 * @param pageUrl
	 * @return
	 */
	public static String getAddToCalendar() {
		StringBuilder sb = new StringBuilder(500);
		sb.append("<li class=\"dropdown\">");
		sb.append("<a href=\"javascript:;\" data-toggle=\"dropdown\"><i class=\"fa fa-calendar-plus-o\"></i> Add to Calendar</a>");
		sb.append("<ul class=\"dropdown-menu dropdown-menu-share icon-dropdown-menu\">");
		sb.append("<li><a href=\"javascript:;\" onclick=\"sendCal('ICAL');\"><i class=\"fa fa-envelope-o\"></i> Outlook</a></li>");
		sb.append("<li><a href=\"javascript:;\" onclick=\"sendCal('ICAL');\"><i class=\"fa fa-calendar-o\"></i> iCal</a></li>");
		sb.append("<li><a href=\"javascript:;\" onclick=\"sendCal('GOOGLE');\"><i class=\"fa fa-google\"></i> Google</a></li>");
		sb.append("<li><a href=\"javascript:;\" onclick=\"sendCal('YAHOO');\"><i class=\"fa fa-yahoo\"></i> Yahoo</a></li>");
		sb.append("<li><a href=\"javascript:;\" onclick=\"sendCal('MSN');\"><i class=\"fa fa-windows\"></i> Windows Live</a></li>");
		sb.append("</ul></li>\n");
		return sb.toString();
	}
}