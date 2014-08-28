/**
 * 
 */
package com.depuysynthesinst.events;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import com.depuysynthesinst.events.vo.CourseCalendarVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.parser.AnnotationXlsParser;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.event.EventEntryAction;

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
		
		AnnotationXlsParser parser = new AnnotationXlsParser();
		//Create a list of vo classnames
		LinkedList<Class<?>> classList = new LinkedList<>();
		classList.add(CourseCalendarVO.class);
		
		try {
			//Gets the xls file from the request object, and passes it to the parser.
			//Parser then returns the list of populated beans
			Map< Class<?>, Collection<Object>> beans = parser.readFileData(
					req.getFile("xlsFile").getFileData(), classList);
			
			ArrayList<Object> beanList = null;
			EventEntryAction eventAction = new EventEntryAction();
			eventAction.setDBConnection(dbConn);
			
			//Disable the db autocommit for the insert batch
			dbConn.setAutoCommit(false);
			
			for ( Class<?> className : beans.keySet() ){
				//Change the generic collection to an arrayList for the import method
				beanList = new ArrayList<>(beans.get(className));
				
				eventAction.importBeans(beanList, StringUtil.checkVal(req.getParameter(
						"attribute_1_txt")) );
				
				//commit the current batch
				dbConn.commit();
			}
		} catch (InvalidDataException | SQLException e) {
			log.debug(e);
		} finally {
			try {
				//restore autocommit state
				dbConn.setAutoCommit(true);
			} catch (SQLException e) {}
		}
	}
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		//mckain to add front-end code here
	}
}
