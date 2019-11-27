package com.wsla.action.admin;

//JDK 1.8.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.orm.SQLTotalVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.wsla.common.UserSqlFilter;
//WSLA Libs
import com.wsla.data.ticket.CASTicketAssignmentVO;
import com.wsla.data.ticket.OwnerTicketAssignmentVO;
import com.wsla.data.ticket.TicketScheduleVO;
import com.wsla.data.ticket.UserVO;


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
	    PAGE,
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
		
		UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();
		
		Date startDate = null;
		Date endDate = null;
		
		//the website formats the dates with day or month first depending on the locale of the user so the date string is split and reorder to match what our code is expecting down stream.  
		
		if("en_US".equalsIgnoreCase(user.getLocale())) {
			startDate = req.getDateParameter(START_DATE);
			endDate = req.getDateParameter(END_DATE);
		}else {
			String[] startDateParts = req.getStringParameter(START_DATE, "").split("/");
			String[] endDateParts = req.getStringParameter(END_DATE, "").split("/");
			
			if(startDateParts != null && startDateParts.length > 1 && endDateParts != null && endDateParts.length > 1 ) {
				startDate = Convert.parseDateUnknownPattern(startDateParts[1] +"/"+ startDateParts[0] + "/"+ startDateParts[2] );
				endDate = Convert.parseDateUnknownPattern(endDateParts[1] +"/"+ endDateParts[0] + "/"+ endDateParts[2]);
			}
			
		}
		
		String direction = req.getParameter("direction");
		
		Map<String, Date> dates =  processDateChange(startDate,endDate,direction);

	
		String roleId = ((SBUserRole)req.getSession().getAttribute(Constants.ROLE_DATA)).getRoleId();
		UserSqlFilter userFilter = new UserSqlFilter(user, roleId, getCustomSchema());
		
		String locationId = req.getParameter("locationId");
		String ft = req.getParameter("eventFilterType");
		BSTableControlVO bst = new BSTableControlVO(req, TicketScheduleVO.class);
		GridDataVO<TicketScheduleVO> resData = getSchedules( locationId, userFilter, dates.get(START_DATE), dates.get(END_DATE), bst, ft);
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
		}  else if (ScheduleDirection.PAGE.name().equalsIgnoreCase(direction)) {
			// do nothing, keep current values
		} else {
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
	public GridDataVO<TicketScheduleVO> getSchedules( String locationId, UserSqlFilter userFilter, Date startDate, Date endDate, BSTableControlVO bst, String ft ) {
		
		StringBuilder sql = new StringBuilder(780);
		sql.append("select t.ticket_no,  ts.ticket_schedule_id, complete_dt, schedule_dt, transfer_type_cd, record_type_cd, ");
		sql.append("ov.ticket_assg_id as own_ticket_assg_id, ov.location_id as own_location_id, ov.location_id as own_user_id, ts.ticket_id as own_ticket_id, 1 as own_owner_flg, ov.assg_type_cd as own_assg_type_cd,  ");
		sql.append("ov.address_txt as own_address_txt, ov.address2_txt as own_address2_txt, ov.state_cd as own_state_cd, ov.city_nm as own_city_nm, ov.location_nm as own_location_nm, ov.zip_cd as own_zip_cd, ");
		sql.append("cta.ticket_assg_id as cas_ticket_assg_id, cta.location_id as cas_location_id, cta.location_id as cas_user_id, ts.ticket_id as cas_ticket_id, 0 as cas_owner_flg, cta.assg_type_cd as cas_assg_type_cd, ");
		sql.append("cl.address_txt as cas_address_txt, cl.address2_txt as cas_address2_txt, cl.state_cd as cas_state_cd, cl.city_nm as cas_city_nm, cl.location_nm as cas_location_nm, cl.zip_cd as cas_zip_cd, ");
		sql.append("ov.latitude_no as own_latitude_no, ov.longitude_no as own_longitude_no, cl.latitude_no as cas_latitude_no, cl.longitude_no as cas_longitude_no ");
		
		StringBuilder base = new StringBuilder();
		base.append("from ").append(getCustomSchema()).append("wsla_ticket_schedule ts ");
		base.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_ticket t on ts.ticket_id = t.ticket_id ");
		base.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_ticket_assignment cta on cta.ticket_assg_id = ts.cas_location_id ");
		base.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_provider_location cl on cta.location_id = cl.location_id ");
		base.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_assignment_location_view ov on ov.ticket_assg_id = ts.owner_location_id ");
		
		// Reduce data down by what the user is allowed to see
		List<Object> params = new ArrayList<>();
		base.append(userFilter.getTicketFilter("t", params));
		base.append(DBUtil.WHERE_1_CLAUSE);
		
		// Add the filter for the pending vs complete
		if (StringUtil.isEmpty(ft) || "PENDING".equals(ft)) base.append(" and complete_dt is null ");
		else if ("COMPLETE".equals(ft)) base.append(" and complete_dt is not null ");
		
		// Add the filter for the 
		if (bst.hasSearch()) {
			base.append(" and (lower(ticket_no) like ?) ");
			params.add(bst.getLikeSearch().toLowerCase());
		}
		
		if (!StringUtil.isEmpty(locationId)) base.append(" and cta.location_id = ? ");
		if(startDate != null && endDate != null) base.append(" and schedule_dt between ? and ? ");
		
		StringBuilder nav = new StringBuilder(64);
		nav.append(bst.getSQLOrderBy("schedule_dt", "ASC"));
		nav.append(" limit ? offset ? ");
		
		int count = 1;
		GridDataVO<TicketScheduleVO> data  = new GridDataVO<>();
		sql.append(base);
		sql.append(nav);
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (Object param : params) {
				ps.setString(count++, param.toString());
			}
			
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
			
			ps.setInt(count++, bst.getLimit());
			ps.setInt(count++, bst.getOffset());
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
				
				SQLTotalVO total = new SQLTotalVO(getTotalRows(base, locationId, params, startDate, endDate));
				data.setSqlTotal(total);
			}
		} catch (Exception e) {
			log.error("could not get schedules or decrypt address ",e);
		}

		return data;
	}
	
	/**
	 * Gets the total number of records
	 * @param base
	 * @param locationId
	 * @param params
	 * @param startDate
	 * @param endDate
	 * @param bst
	 * @return
	 */
	private int getTotalRows(StringBuilder base, String locationId, List<Object> params, Date startDate, Date endDate) {
		int count = 1;
		StringBuilder sql = new StringBuilder(base.length() + 16);
		sql.append("select count(*) ").append(base);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (Object param : params) {
				ps.setString(count++, param.toString());
			}
			
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
				if (rs.next()) return rs.getInt(1);
			}
		} catch (Exception e) {
			log.error("Unable to get count", e);
		}
		
		return 0;
	}
}