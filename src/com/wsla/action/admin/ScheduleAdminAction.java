package com.wsla.action.admin;

//JDK 1.8.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
//WSLA Libs
import com.wsla.data.ticket.CASTicketAssignmentVO;
import com.wsla.data.ticket.OwnerTicketAssignmentVO;
import com.wsla.data.ticket.TicketScheduleVO;


/****************************************************************************
 * <b>Title</b>: DefectAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the administration viewing of the scheduled events
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Ryan Riker
 * @version 3.0
 * @since Sep 19, 2018
 * @updates:
 ****************************************************************************/

public class ScheduleAdminAction extends SBActionAdapter {
	public static final String SCHEDULE_TYPE = "events";
	public static final String START_DATE = "startDate";
	public static final String END_DATE = "endDate";
	
	public enum ScheduleDirection {
	    BACK,
	    FORWARD,
	    CURRENT;
	}
	
	/**
	 * 
	 */
	public ScheduleAdminAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public ScheduleAdminAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Overloaded constructor used for calling between actions.  Note default access modifier
	 * @param attrs
	 * @param conn
	 */
	public ScheduleAdminAction(Map<String, Object> attrs, SMTDBConnection conn) {
		this();
		this.setAttributes(attrs);
		this.setDBConnection(conn);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("Schedule action retrieve");
		
		Date startDate = req.getDateParameter(START_DATE);
		Date endDate = req.getDateParameter(END_DATE);
		String direction = req.getParameter("direction");
		
		Map<String, Date> dates =  processDateChange(startDate,endDate,direction);

		String locationId = req.getParameter("locationId");
		GridDataVO<TicketScheduleVO> resData = getSchedules( locationId, dates.get(START_DATE), dates.get(END_DATE) );
		Map<String, Object> resMap = new HashMap<>();
		
		resMap.put("attributes", dates);
		resMap.put("isSuccess", Boolean.TRUE);
		resMap.put("jsonActionError", "");
		resMap.put("actionData", resData.getRowData());
		resMap.put("count", resData.getTotal());
		putModuleData(resMap);
	}


	/**
	 * @param startDate
	 * @param endDate
	 * @param direction
	 */
	private Map<String, Date> processDateChange(Date startDate, Date endDate, String direction) {
		Map<String, Date> dates = new HashMap <>();
		if(StringUtil.isEmpty(direction))return dates;
		
		if(ScheduleDirection.BACK.name().equalsIgnoreCase(direction)) {
			endDate = startDate;
			startDate = Convert.formatStartDate(Convert.formatDate(endDate, Calendar.DAY_OF_YEAR, -7));
		} else if (ScheduleDirection.FORWARD.name().equalsIgnoreCase(direction)) {
			startDate = endDate;
			endDate = Convert.formatEndDate(Convert.formatDate(startDate, Calendar.DAY_OF_YEAR, 6));
		}else {
			int week = Convert.getWeekFromDate(new Date());
			startDate = Convert.formatStartDate(Convert.getDateFromWeek(week, Convert.getCurrentYear(), 1));
			endDate = Convert.formatEndDate(Convert.formatDate(startDate, Calendar.DAY_OF_YEAR, 6));
		}

		dates.put(START_DATE, startDate);
		dates.put(END_DATE, endDate);
		
		return dates;
	}

	/**
	 * Gets a list of providers.  Since this list should be small (< 100)
	 * assuming client side pagination and filtering 
	 * @param providerType
	 * @param providerId
	 * @return
	 */
	public GridDataVO<TicketScheduleVO> getSchedules( String locationId,  Date startDate, Date endDate ) {
		
		StringBuilder sql = new StringBuilder(780);
		sql.append("select t.ticket_no,  ts.*, ");
		sql.append("ov.ticket_assg_id as own_ticket_assg_id, ov.location_id as own_location_id, ov.location_id as own_user_id, ts.ticket_id as own_ticket_id, 1 as own_owner_flg, ov.assg_type_cd as own_assg_type_cd,  ");
		sql.append("ov.address_txt as own_address_txt, ov.address2_txt as own_address2_txt, ov.state_cd as own_state_cd, ov.city_nm as own_city_nm, ov.location_nm as own_location_nm, ov.zip_cd as own_zip_cd, ");
		sql.append("cta.ticket_assg_id as cas_ticket_assg_id, cta.location_id as cas_location_id, cta.location_id as cas_user_id, ts.ticket_id as cas_ticket_id, 0 as cas_owner_flg, cta.assg_type_cd as cas_assg_type_cd, ");
		sql.append("cl.address_txt as cas_address_txt, cl.address2_txt as cas_address2_txt, cl.state_cd as cas_state_cd, cl.city_nm as cas_city_nm, cl.location_nm as cas_location_nm, cl.zip_cd as cas_zip_cd, ");
		sql.append("ov.latitude_no as own_latitude_no, ov.longitude_no as own_longitude_no, cl.latitude_no as cas_latitude_no, cl.longitude_no as cas_longitude_no ");
		sql.append("from ").append(getCustomSchema()).append("wsla_ticket_schedule ts ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_ticket t on ts.ticket_id = t.ticket_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_ticket_assignment cta on cta.ticket_assg_id = ts.cas_location_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_provider_location cl on cta.location_id = cl.location_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_assignment_location_view ov on ov.ticket_assg_id = ts.owner_location_id ");
		sql.append(DBUtil.WHERE_1_CLAUSE);
		
		if (!StringUtil.isEmpty(locationId)) {
			sql.append(" and cta.location_id = ? ");
		}
		
		if(startDate != null && endDate != null) {
			log.debug("dates between " + startDate +" and "+ endDate);
			sql.append(" and schedule_dt between ? and ? ");
		}
		
		int count = 1;
		GridDataVO<TicketScheduleVO> data  = new GridDataVO<>();
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {

			if (!StringUtil.isEmpty(locationId)) {
				ps.setString(count, locationId);
				count++;
			}
			
			if(startDate != null && endDate != null) {
				ps.setDate(count, new java.sql.Date(startDate.getTime()));
				count++;
				ps.setDate(count, new java.sql.Date(endDate.getTime()));
				count++;
			}
			
			
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					
					TicketScheduleVO tso = new TicketScheduleVO(rs);
					tso.setStartDate(startDate);
					tso.setEndDate(endDate);
					OwnerTicketAssignmentVO ota = new OwnerTicketAssignmentVO(rs);
					CASTicketAssignmentVO cta = new CASTicketAssignmentVO(rs);
					tso.setCasLocation(cta);
					tso.setOwnerLocation(ota);
						
					data.getRowData().add(tso);
				}
			}
		} catch (Exception e) {
			log.error("could not get schedules or decrypt address ",e);
		}

		return data;
	}
}