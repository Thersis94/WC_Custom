/**
 * 
 */
package com.depuysynthesinst.events;

import java.sql.PreparedStatement;
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
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.util.parser.AnnotationXlsParser;
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
			
			//Disable the db autocommit for the insert batch
			dbConn.setAutoCommit(false);
			
			for ( Class<?> className : beans.keySet() ){
				//Change the generic collection to an arrayList for the import method
				beanList = new ArrayList<>(beans.get(className));
				
				importBeans(beanList, StringUtil.checkVal(req.getParameter(
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
	
	/**
	 * Imports the beans as a set of new records to the event_entry table
	 * @param beanList
	 * @param actionId
	 * @throws ActionException
	 */
	private void importBeans(ArrayList<Object> beanList, String actionId) throws ActionException{
		StringBuilder sql = new StringBuilder(580);
		sql.append("insert into EVENT_ENTRY (EVENT_ENTRY_ID, STATE_CD, ACTION_ID, ");
		sql.append("EVENT_NM, LOCATION_DESC, EVENT_DESC, SHORT_DESC, CONTACT_NM, ");
		sql.append("EMAIL_ADDRESS_TXT, PHONE_TXT, START_DT, END_DT, ORDER_NO, ADDRESS_TXT, ");
		sql.append("ADDRESS2_TXT, CITY_NM, ZIP_CD, CREATE_DT, OPCO_NM, LATITUDE_NO, ");
		sql.append("LONGITUDE_NO, RSVP_CODE_TXT, STATUS_FLG, CONTACT_FLG, MAX_USER_NO, ");
		sql.append("APPROVAL_REQUIRED_FLG, EVENT_TYPE_ID, EVENT_URL, SERVICE_TXT,FILE_PATH_TXT) ");
		sql.append("values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			for ( Object obj : beanList ){
				int i = 0;
				
				//Casts the generic object to CourseCalendarVO
				CourseCalendarVO vo = (CourseCalendarVO) obj;
				
				ps.setString(++i, new UUIDGenerator().getUUID());
				ps.setString(++i, vo.getStateCode());
				ps.setString(++i, actionId);
				ps.setString(++i, vo.getEventName());
				ps.setString(++i, vo.getLocationDesc());
				ps.setString(++i, buildEventDesc(vo));
				ps.setString(++i, vo.getShortDesc());
				ps.setString(++i, vo.getContactName());
				ps.setString(++i, vo.getEmailAddress());
				ps.setString(++i, vo.getPhoneText());
				ps.setTimestamp(++i, Convert.getTimestamp(vo.getStartDate(), true));
				ps.setTimestamp(++i, Convert.getTimestamp(vo.getEndDate(),true));
				ps.setInt(++i, Convert.formatInteger(vo.getOrderNumber() ));
				ps.setString(++i, vo.getAddressText());
				ps.setString(++i, vo.getAddress2Text());
				ps.setString(++i, vo.getCityName());
				ps.setString(++i, vo.getZipCode());
				ps.setTimestamp(++i, Convert.getCurrentTimestamp() );
				ps.setString(++i, vo.getOpcoName());
				ps.setDouble(++i, vo.getLatitude());
				ps.setDouble(++i, vo.getLongitude());
				ps.setString(++i, vo.getRSVPCode());
				ps.setInt(++i,  Convert.formatInteger(vo.getStatusFlg() ) );
				ps.setInt(++i, Convert.formatInteger(vo.getContactFlg() ) );
				ps.setInt(++i, Convert.formatInteger( vo.getMaxNumberUsers() ) );
				ps.setInt(++i, Convert.formatInteger( vo.getApprovableFlag() ) );
				ps.setString(++i, actionId);
				ps.setString(++i, vo.getEventUrl());
				ps.setString(++i, vo.getServiceText());
				ps.setString(++i, vo.getEventFilePath());
				
				//adds this record to the batch
				ps.addBatch();
				ps.clearParameters();
			}
			
			ps.executeBatch();
		} catch (SQLException e){
			throw new ActionException("Error inserting records into event_entry table.",e);
		}
	}

	/**
	 * Used to concatenate the four description fields from the imported excel 
	 * file into a single unordered list (since there is only 1 event_desc field
	 * in the table).
	 * @param vo
	 * @return
	 */
	private String buildEventDesc(CourseCalendarVO vo){
		StringBuilder sb = new StringBuilder();
		
		sb.append("<ul><li>");
		sb.append(StringUtil.checkVal(vo.getEventDesc()) );
		sb.append("</li><li>");
		sb.append(StringUtil.checkVal(vo.getEventDesc2() ));
		sb.append("</li><li>");
		sb.append(StringUtil.checkVal(vo.getEventDesc3() ));
		sb.append("</li><li>");
		sb.append(StringUtil.checkVal(vo.getEventDesc4()) );
		sb.append("</li></ul>");
		
		return sb.toString();
	}
}
