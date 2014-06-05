package com.ram.action.event;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.ram.action.data.InventoryEventGroupVO;
import com.ram.datafeed.data.CustomerEventVO;
import com.ram.datafeed.data.InventoryEventReturnVO;
import com.ram.datafeed.data.InventoryEventVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.DateRecurrenceUtil;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: InventoryEventRecurranceAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jun 3, 2014
 ****************************************************************************/
public class InventoryEventRecurrenceAction extends SBActionAdapter {
	
	//constant for passing data between actions
	public static final String EVENT_GRP_OBJ = "eventGroupObj";

	public InventoryEventRecurrenceAction() {
	}

	/**
	 * @param actionInit
	 */
	public InventoryEventRecurrenceAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	public void update(SMTServletRequest req) throws ActionException {
		InventoryEventGroupVO eventGroup = (InventoryEventGroupVO) getAttribute(EVENT_GRP_OBJ);
		log.debug("eventGroup=" + StringUtil.getToString(eventGroup));
		
		List<InventoryEventVO> events = loadEvents(eventGroup.getInventoryEventGroupId(), Convert.formatDate(req.getParameter("eventDate")));
		
		//batch through the events to propagate the recurrences
		this.processRecurrences(eventGroup, events);
		
	}
	
	
	private void processRecurrences(InventoryEventGroupVO eventGroup, List<InventoryEventVO> events) 
			throws ActionException {
		//this is the event we'll re-index "X" times, as needed.  it's the only one that contains all the data we need.
		InventoryEventVO mainEvent = events.get(0);
		
		//reset the time on the main event to the recurring time that was passed
		String newTs = Convert.formatDate(mainEvent.getScheduleDate(), Convert.DATE_DASH_PATTERN);
		newTs += " " + eventGroup.getDefaultTime();
		log.debug("newTime=" + newTs);
		mainEvent.setScheduleDate(Convert.formatDate(Convert.DATE_TIME_DASH_PATTERN_12HR, newTs));
		log.debug("set newDateTime=" + mainEvent.getScheduleDate());
		
		//setup the recurrence util, which will keep track of the date incrementation
		Calendar endDt = Calendar.getInstance();
		endDt.setTime(mainEvent.getScheduleDate());
		endDt.add(Calendar.WEEK_OF_YEAR, eventGroup.getTotalWeek());
		DateRecurrenceUtil recurrence = new DateRecurrenceUtil(mainEvent.getScheduleDate(), endDt.getTime());
		recurrence.setSunday(eventGroup.getSundayFlag() == 1);
		recurrence.setMonday(eventGroup.getMondayFlag() == 1);
		recurrence.setTuesday(eventGroup.getTuesdayFlag() == 1);
		recurrence.setWednesday(eventGroup.getWednesdayFlag() == 1);
		recurrence.setThursday(eventGroup.getThursdayFlag() == 1);
		recurrence.setFriday(eventGroup.getFridayFlag() == 1);
		recurrence.setSaturday(eventGroup.getSaturdayFlag() == 1);
		
		//figure out how many times we need to iterate; based on the greater of eventsWeHave and eventsWeNeed
		int countNeeded = eventGroup.getRecurrenceCount();
		if (countNeeded == 0) countNeeded = 1; //we must preserve the mainEvent even though there are no recurrences
		int iterations = (events.size() > countNeeded) ? events.size() : countNeeded;
		
		//this will be our batch statement and official & sequenced list of recurring Events
		List<InventoryEventVO> recurringEvents = new ArrayList<InventoryEventVO>(iterations);
		
		for (int x=0; x < iterations; x++) {
			//'this' event will be one already in the system (we're updating), or a new one we need to add.
			InventoryEventVO thisEvent = (x < events.size()) ? events.get(x) : new InventoryEventVO();
			
			//copy the data off the master record
			populateEvent(thisEvent, mainEvent);
			
			//do we need it, or should it be inactivated (deleted)?
			thisEvent.setActiveFlag((x < countNeeded) ? 1 : 0);
			
			//if we need it, calculate the event date based on the previous & schedule
			if (thisEvent.isActive()) {
				thisEvent.setScheduleDate(recurrence.nextDate());
				if (thisEvent.getScheduleDate() == null) 
					//this only happens when 'no recurrence' is selected; the recurrenceUtil has none to give back 
					//we have to go back to the built timestamp because thisEvent.equals(mainEvent) == true (same object!)
					thisEvent.setScheduleDate(Convert.formatDate(Convert.DATE_TIME_DASH_PATTERN_12HR, newTs));
			} else {
				//if we don't need it set the schedule date to today, which reflects the transaciton time of the change
				thisEvent.setScheduleDate(null);
			}
			
			//add this event to the batch
			recurringEvents.add(thisEvent);
			log.debug("added " + thisEvent.getScheduleDate());
		}
		
		//update the event table, capture the pkId for use on _XR tables
		InventoryEventAction sai = new InventoryEventAction(actionInit);
		sai.setDBConnection(dbConn);
		sai.setAttributes(getAttributes());
		sai.update(recurringEvents);
			

		//copy auditors
		//copyAuditors(mainEvent, recurringEvents);
		
		//copy customers
		copyCustomers(mainEvent.getInventoryEventId(), recurringEvents);
		
		//copy returns
		copyReturns(mainEvent.getInventoryEventId(), recurringEvents);
			
	}
	
	/**
	 * performs 3 queries to 
	 * 1) retrieve the customers attached to the main event,
	 * 2) purge all customers tied to the events we've added/re-used
	 * 3) batch-insert the customers to all the new/active events in the recurrence
	 * 
	 * @param masterEventId
	 * @param recurringEvents
	 * @throws ActionException
	 */
	private void copyCustomers(Integer masterEventId, List<InventoryEventVO> recurringEvents) throws ActionException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		PreparedStatement ps = null;
		List<CustomerEventVO> customers = new ArrayList<CustomerEventVO>();
		
		//find the one master _xr record we're going to need to re-insert "X" times over.
		sql.append("select customer_id, active_flg from ").append(customDb);
		sql.append("RAM_CUSTOMER_EVENT_XR ").append("where inventory_event_id=?");
		log.debug(sql + "|" + masterEventId);
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, masterEventId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				CustomerEventVO vo = new CustomerEventVO();
				vo.setCustomerId(rs.getInt(1));
				vo.setActiveFlag(rs.getInt(2));
				customers.add(vo);
			}
		} catch (SQLException sqle) {
			throw new ActionException("could not select customer_xr items", sqle);
		} finally {
			DBUtil.close(ps);
		}
		log.debug("found " + customers.size() + " customers to propagate");
		
		//batch delete
		sql = new StringBuilder();
		sql.append("delete from ").append(customDb).append("RAM_CUSTOMER_EVENT_XR ");
		sql.append("where inventory_event_id in (?");
		for (@SuppressWarnings("unused") InventoryEventVO vo : recurringEvents) sql.append(",?");
		sql.append(")");
		log.debug(sql);

		int x = 1;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(x++, masterEventId);
			for (InventoryEventVO vo : recurringEvents) 
				ps.setInt(x++, vo.getInventoryEventId());
			x = ps.executeUpdate();
			log.debug("deleted " + x + " records tied to events we're overwriting");
		} catch (SQLException sqle) {
			throw new ActionException("could not purge old customer_event_xr items", sqle);
		} finally {
			DBUtil.close(ps);
		}
		
		//if there were none in the system, we've already cleaned-up the reused records...we're done
		if (customers.size() == 0) return;
		
		//batch insert
		sql = new StringBuilder();
		sql.append("insert into ").append(customDb).append("RAM_CUSTOMER_EVENT_XR ");
		sql.append("(inventory_event_id, customer_id, active_flg, create_dt) ");
		sql.append("values (?,?,?,?)");
		log.debug(sql);
		try {
			ps = dbConn.prepareStatement(sql.toString());
			for (InventoryEventVO vo : recurringEvents) {
				if (! vo.isActive()) continue; //don't bind to dead events.
				
				//copy all the customers from the master record into every recurring event.  (double-loop)
				for (CustomerEventVO ret : customers) {
					ps.setInt(1, vo.getInventoryEventId()); //from the event
					ps.setInt(2, ret.getCustomerId());
					ps.setInt(3, ret.getActiveFlag());
					ps.setTimestamp(4, Convert.getCurrentTimestamp());
					ps.addBatch();
				}
			}
			int[] cnt = ps.executeBatch();
			log.debug("inserted " + cnt.length + " customers records");
		} catch (SQLException sqle) {
			throw new ActionException("could not save customers_xr items", sqle);
		} finally {
			DBUtil.close(ps);
		}
	}
	
	
	/**
	 * performs 3 queries to 
	 * 1) retrieve the returns attached to the main event,
	 * 2) purge all returns tied to the events we've added/re-used
	 * 3) batch-insert the returns to all the new/active events in the recurrence
	 * 
	 * @param masterEventId
	 * @param recurringEvents
	 * @throws ActionException
	 */
	private void copyReturns(Integer masterEventId, List<InventoryEventVO> recurringEvents) throws ActionException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		PreparedStatement ps = null;
		List<InventoryEventReturnVO> returns = new ArrayList<InventoryEventReturnVO>();
		
		//find the one master _xr record we're going to need to re-insert "X" times over.
		sql.append("select product_id, quantity_no, lot_number_txt, active_flg from ");
		sql.append(customDb).append("RAM_EVENT_RETURN_XR ");
		sql.append("where inventory_event_id=?");
		log.debug(sql + "|" + masterEventId);
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, masterEventId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				InventoryEventReturnVO vo = new InventoryEventReturnVO();
				vo.setProductId(rs.getInt(1));
				vo.setQuantity(rs.getInt(2));
				vo.setLotNumber(rs.getString(3));
				vo.setActiveFlag(rs.getInt(4));
				returns.add(vo);
			}
		} catch (SQLException sqle) {
			throw new ActionException("could not select old return_xr items", sqle);
		} finally {
			DBUtil.close(ps);
		}
		log.debug("found " + returns.size() + " returns to propagate");
		
		//batch delete
		sql = new StringBuilder();
		sql.append("delete from ").append(customDb).append("RAM_EVENT_RETURN_XR ");
		sql.append("where inventory_event_id in (?");
		for (@SuppressWarnings("unused") InventoryEventVO vo : recurringEvents) sql.append(",?");
		sql.append(")");
		log.debug(sql);

		int x = 1;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(x++, masterEventId);
			for (InventoryEventVO vo : recurringEvents) 
				ps.setInt(x++, vo.getInventoryEventId());
			x = ps.executeUpdate();
			log.debug("deleted " + x + " records tied to events we're overwriting");
		} catch (SQLException sqle) {
			throw new ActionException("could not purge old return_xr items", sqle);
		} finally {
			DBUtil.close(ps);
		}
		
		//if there were none in the system, we've already cleaned-up the reused records...we're done
		if (returns.size() == 0) return;
		
		//batch insert
		sql = new StringBuilder();
		sql.append("insert into ").append(customDb).append("RAM_EVENT_RETURN_XR ");
		sql.append("(inventory_event_id, product_id, quantity_no, lot_number_txt, ");
		sql.append("active_flg, create_dt) values (?,?,?,?,?,?)");
		log.debug(sql);
		try {
			ps = dbConn.prepareStatement(sql.toString());
			for (InventoryEventVO vo : recurringEvents) {
				if (! vo.isActive()) continue; //don't bind to dead events.
				
				//copy all the returns from the master record into every recurring event.  (double-loop)
				for (InventoryEventReturnVO ret : returns) {
					ps.setInt(1, vo.getInventoryEventId()); //from the event
					ps.setInt(2, ret.getProductId());
					ps.setInt(3, ret.getQuantity());
					ps.setString(4, ret.getLotNumber());
					ps.setInt(5, ret.getActiveFlag());
					ps.setTimestamp(6, Convert.getCurrentTimestamp());
					ps.addBatch();
				}
			}
			int[] cnt = ps.executeBatch();
			log.debug("inserted " + cnt.length + " return records");
		} catch (SQLException sqle) {
			throw new ActionException("could not save return_xr items", sqle);
		} finally {
			DBUtil.close(ps);
		}
	}
	
	
	/**
	 * copy data from the master record into the recurrence being created
	 * @param event
	 * @param master
	 * @return
	 */
	private InventoryEventVO populateEvent(InventoryEventVO event, InventoryEventVO master) {
		// note we're not setting the pkId on these; we want to preserve what's already there so we can update the DB.
		event.setInventoryEventGroupId(master.getInventoryEventGroupId());
		event.setCustomerLocationId(master.getCustomerLocationId());
		event.setComment(master.getComment());
		event.setScheduleDate(master.getScheduleDate());
		event.setInventoryCompleteDate(master.getInventoryCompleteDate());
		event.setDataLoadCompleteDate(master.getDataLoadCompleteDate());
		event.setVendorEventId(master.getVendorEventId());
		event.setActiveFlag(master.getActiveFlag());
		event.setCancellationComment(master.getCancellationComment());
		event.setPartialInventoryFlag(master.getPartialInventoryFlag());
		return event;
	}
	
	
	/**
	 * loads the 4 individual 'parts' of the event.
	 * returns a map instead of a list, because all the data is the same across the pieces except in the primary _event table
	 * @param eventGroupId
	 * @return
	 * @throws ActionException
	 */
	private List<InventoryEventVO> loadEvents(String eventGroupId, Date eventDt) throws ActionException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		List<InventoryEventVO> data = new ArrayList<InventoryEventVO>();
		
		StringBuilder sql = new StringBuilder();
		sql.append("select * from ").append(customDb).append("RAM_INVENTORY_EVENT ");
		sql.append("where schedule_dt >= ?  and inventory_event_group_id=? ");
		sql.append("order by schedule_dt");
		log.debug(sql + "|" + Convert.formatSQLDate(eventDt) + "|" + eventGroupId);
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setDate(1, Convert.formatSQLDate(eventDt));
			ps.setString(2, eventGroupId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				InventoryEventVO event = new InventoryEventVO();
				event.setInventoryEventId(rs.getInt("inventory_event_id"));
				
				//we only need ALL of the fields for the first event, since any we need to create will be identical to it.
				//this could have been done via a clone() on the VO, if it implemented Cloneable.
				if (data.size() == 0)  {
					event.setInventoryEventGroupId(rs.getString("inventory_event_group_id"));
					event.setCustomerLocationId(rs.getInt("customer_location_id"));
					event.setComment(rs.getString("comment_txt"));
					event.setScheduleDate(rs.getTimestamp("schedule_dt"));
					event.setInventoryCompleteDate(rs.getDate("inventory_complete_dt"));
					event.setDataLoadCompleteDate(rs.getDate("data_load_complete_dt"));
					event.setVendorEventId(rs.getString("vendor_event_id"));
					event.setActiveFlag(rs.getInt("active_flg"));
					event.setCancellationComment(rs.getString("cancellation_cmmt"));
					event.setPartialInventoryFlag(rs.getInt("partial_inventory_flg"));
				}
				
				data.add(event);
			}
			
		} catch (SQLException sqle) {
			throw new ActionException(sqle);
		} finally {
			DBUtil.close(ps);
		}
		return data;
	}
	

}
