/**
 * 
 */
package com.depuysynthesinst.events;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title</b>: CourseCalendar.java<p/>
 * <b>Description: 
 * Admin side: imports data from an Excel file into the Event's portlet defined by the administrator.
 * Public side: loads all the events tied to the porlet and filters them by Anatomy type.
 * </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Aug 25, 2014
 ****************************************************************************/
public class CourseCalendar extends SimpleActionAdapter {

	public CourseCalendar() {
	}

	/**
	 * @param arg0
	 */
	public CourseCalendar(ActionInitVO arg0) {
		super(arg0);
	}
	
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}

	
	public void update(SMTServletRequest req) throws ActionException {
		super.update(req);
		
		//Wingo to load data from Excel file here...
		
	}
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		//mckain to add front-end code here
	}
}
