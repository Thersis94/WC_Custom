package com.ram.action.event;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.ram.action.data.InventoryEventGroupVO;
import com.ram.datafeed.data.AuditorVO;
import com.ram.datafeed.data.CustomerEventVO;
import com.ram.datafeed.data.InventoryEventAuditorVO;
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
	
	//Attribute variable that instructs action to place newly created events on passed Events List.
	public static final String IS_UTIL_BATCH = "isUtilBatch";
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
	
	/**
	 * Take in eventGroup and list of events for the group and replicate the root event based on the
	 * parameters of the event Group.
	 * 
	 * @update Billy Larsen - If IS_UTIL_BATCH parameter is on attributes map then newly created events 
	 * are placed on the passed events list.  
	 * @param eventGroup
	 * @param events
	 * @throws ActionException
	 */
	public void processRecurrences(InventoryEventGroupVO eventGroup, List<InventoryEventVO> events) 
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

				//this only happens when 'no recurrence' is selected; the recurrenceUtil has no recurrences to give back to us 
				//we have to go back to the prebuilt timestamp because thisEvent.equals(mainEvent) == true (same object!)
				if (thisEvent.getScheduleDate() == null) 
					thisEvent.setScheduleDate(Convert.formatDate(Convert.DATE_TIME_DASH_PATTERN_12HR, newTs));
			} else {
				//if we don't need it set the schedule date to null, which will prevent it from appearing on the website.
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
			

		//create a list of events to process child records for that excludes the master event, which already has what it needs.
		List<InventoryEventVO> insertEvents = new ArrayList<InventoryEventVO>(recurringEvents.size());
		for (InventoryEventVO vo : recurringEvents) {
			if (vo.getInventoryEventId().equals(mainEvent.getInventoryEventId())) continue;
			insertEvents.add(vo);
		}
				
		//copy auditors
		copyAuditors(mainEvent.getInventoryEventId(), insertEvents);
		
		//copy customers
		copyCustomers(mainEvent.getInventoryEventId(), insertEvents);
		
		//copy returns
		copyReturns(mainEvent.getInventoryEventId(), insertEvents);
		
		//Added for use with EventDataGeneratorUtil for bulk creation of historical transactions.
		if(Convert.formatBoolean(attributes.get(IS_UTIL_BATCH)))
			events.addAll(insertEvents);
	}
	
	/**
	 * performs queries to 
	 * 1) retrieve the customers attached to the main event,
	 * 2) find existing _xr records we can overwrite
	 * 3) iterate all the recurrence events and bind a set of customers records to each one
	 * 4) batch insert/update the records we need to put in the database
	 * 5) inactivate any existing records we're not going to use (for overwrites).
	 * 
	 * @param masterEventId
	 * @param recurringEvents
	 * @throws ActionException
	 */
	private void copyCustomers(Integer masterEventId, List<InventoryEventVO> recurrenceEvents) throws ActionException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = null;
		List<CustomerEventVO> customers = new ArrayList<CustomerEventVO>();
		
		//find the masterEvent's  _xr records we're going to need to re-insert "X" times over.
		sql = new StringBuilder();
		sql.append("select customer_id, active_flg from ").append(customDb);
		sql.append("RAM_CUSTOMER_EVENT_XR ").append("where inventory_event_id=?");
		log.debug(sql + "|" + masterEventId);
		PreparedStatement ps = null;
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
		
		
		//find records already in there we can overwrite (instead of deleting all & inserting)
		sql = new StringBuilder();
		sql.append("select customer_event_id from ").append(customDb).append("RAM_CUSTOMER_EVENT_XR ");
		sql.append("where inventory_event_id in (0");
		for (@SuppressWarnings("unused") InventoryEventVO vo : recurrenceEvents) 	sql.append(",?");
		sql.append(")");
		List<Integer> reusableIds = loadReuseableIds(sql.toString(), recurrenceEvents);
		log.debug("found " + reusableIds.size() + " records we can reuse/replace");
		
		
		//Flatten all the write transactions into a single list.  
		//Then we'll run two batch transactions against this list; one for the updates and one for the inserts
		List<CustomerEventVO> records = new ArrayList<CustomerEventVO>();
		for (InventoryEventVO vo : recurrenceEvents) {
			//don't bind to dead events
			if (! vo.isActive()) continue;
			
			//copy all the customers from the master record into each recurrence event.  (double-loop)
			for (CustomerEventVO ret : customers) {
				CustomerEventVO newRec = new CustomerEventVO();
				if (! reusableIds.isEmpty()) {
					//reassign the first available pkID to this record, while removing it from the list
					newRec.setCustomerEventId(reusableIds.remove(0));
				}
				newRec.setInventoryEventId(vo.getInventoryEventId()); //from the event
				newRec.setCustomerId(ret.getCustomerId());
				newRec.setActiveFlag(ret.getActiveFlag());
				records.add(newRec);
			}
		}
		
		
		if (records.size() > 0) {
			//batch update of reused records
			sql = new StringBuilder();
			sql.append("update ").append(customDb).append("RAM_CUSTOMER_EVENT_XR ");
			sql.append("set inventory_event_id=?, customer_id=?, active_flg=?, update_dt=? ");
			sql.append("where customer_event_id=?");
			batchCustomers(sql.toString(), false, records);
		
			//batch insert of new records
			sql = new StringBuilder();
			sql.append("insert into ").append(customDb).append("RAM_CUSTOMER_EVENT_XR ");
			sql.append("(inventory_event_id, customer_id, active_flg, create_dt) ");
			sql.append("values (?,?,?,?)");
			batchCustomers(sql.toString(), true, records);
		}
		
		
		//if there are reusableIds we didn't use, leave them as-is and just mark inactive
		if (! reusableIds.isEmpty()) {
			sql = new StringBuilder();
			sql.append("update ").append(customDb).append("RAM_CUSTOMER_EVENT_XR ");
			sql.append("set active_flg=?, update_dt=? where customer_event_id in (0");
			for (@SuppressWarnings("unused") Integer i : reusableIds) sql.append(",?");
			sql.append(")");
			batchInactiveSurplus(sql.toString(), reusableIds);
		}
	}
	
	
	/**
	 * queries the _XR tables in a generic way for a list of pkIds we can overwrite
	 * @param sql
	 * @param recurrenceEvents
	 * @return
	 * @throws ActionException
	 */
	private List<Integer> loadReuseableIds(String sql, List<InventoryEventVO> recurrenceEvents) 
			throws ActionException {
		List<Integer> reusableIds = new ArrayList<Integer>();
		log.debug(sql);
		int x = 1;
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			for (InventoryEventVO vo : recurrenceEvents) 
				ps.setInt(x++, vo.getInventoryEventId());
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				reusableIds.add(Integer.valueOf(rs.getInt(1)));
			
		} catch (SQLException sqle) {
			throw new ActionException("could not load existing records", sqle);
		} finally {
			DBUtil.close(ps);
		}
		
		return reusableIds;
	}
	
	
	/**
	 * generic method to mark _xr records as inactive
	 * @param sql
	 * @param reusableIds
	 * @throws ActionException
	 */
	private void batchInactiveSurplus(String sql, List<Integer> reusableIds) throws ActionException {
		log.debug(sql);
		int x = 1;
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(x++, 0);
			ps.setTimestamp(x++, Convert.getCurrentTimestamp());
			for (Integer id : reusableIds) 
				ps.setInt(x++, id);
			x = ps.executeUpdate();
			log.debug("inactivated " + x + " surplus records");
		} catch (SQLException sqle) {
			throw new ActionException("could not inactivate surplus records", sqle);
		} finally {
			DBUtil.close(ps);
		}
	}
	
	
	/**
	 * generically processes _customer_xr records; used by both insert and update queries
	 * @param sql
	 * @param isInsert
	 * @param records
	 * @throws ActionException
	 */
	private void batchCustomers(String sql, boolean isInsert, List<CustomerEventVO> records) 
			throws ActionException {
		log.debug(sql);
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql);
			for (CustomerEventVO vo : records) {
				int pkId = Convert.formatInteger(vo.getCustomerEventId()).intValue();
				if (! isInsert && pkId < 1) continue; //skip inserts on updates run
				else if (isInsert && pkId > 0) continue; //skip updates on inserts run
				
				ps.setInt(1, vo.getInventoryEventId());
				ps.setInt(2, vo.getCustomerId());
				ps.setInt(3, vo.getActiveFlag());
				ps.setTimestamp(4, Convert.getCurrentTimestamp());
				if (! isInsert) ps.setInt(5, pkId);
				ps.addBatch();
			}
			int[] cnt = ps.executeBatch();
			log.debug((isInsert ? "inserted " : "updated ") + cnt.length + " customer_xr records");
		} catch (SQLException sqle) {
			throw new ActionException("could not save customer_xr items", sqle);
		} finally {
			DBUtil.close(ps);
		}
	}
	
	
	/**
	 * generically processes _return_xr records; used by both insert and update queries
	 * @param sql
	 * @param isInsert
	 * @param records
	 * @throws ActionException
	 */
	private void batchReturns(String sql, boolean isInsert, List<InventoryEventReturnVO> records) 
			throws ActionException {
		log.debug(sql);
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql);
			for (InventoryEventReturnVO vo : records) {
				int pkId = Convert.formatInteger(vo.getEventReturnId()).intValue();
				if (! isInsert && pkId < 1) continue; //skip inserts on update runs
				else if (isInsert && pkId > 0) continue; //skip updates on insert runs
				
				ps.setInt(1, vo.getInventoryEventId());
				ps.setInt(2, vo.getProductId());
				ps.setInt(3, vo.getQuantity());
				ps.setString(4, vo.getLotNumber());
				ps.setInt(5, vo.getActiveFlag());
				ps.setTimestamp(6, Convert.getCurrentTimestamp());
				if (! isInsert) ps.setInt(7, pkId);
				ps.addBatch();
			}
			int[] cnt = ps.executeBatch();
			log.debug((isInsert ? "inserted " : "updated ") + cnt.length + " return_xr records");
		} catch (SQLException sqle) {
			throw new ActionException("could not save return_xr items", sqle);
		} finally {
			DBUtil.close(ps);
		}
	}
	
	
	/**
	 * generically processes _auditor_xr records; used by both insert and update queries
	 * @param sql
	 * @param isInsert
	 * @param records
	 * @throws ActionException
	 */
	private void batchAuditors(String sql, boolean isInsert, List<InventoryEventAuditorVO> records) 
			throws ActionException {
		log.debug(sql);
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql);
			for (InventoryEventAuditorVO vo : records) {
				int pkId = Convert.formatInteger(vo.getInventoryEventAuditorId()).intValue();
				if (! isInsert && pkId < 1) continue; //skip inserts on update runs
				else if (isInsert && pkId > 0) continue; //skip updates on insert runs

				ps.setInt(1, vo.getInventoryEventId());
				ps.setInt(2, vo.getAuditor().getAuditorId());
				ps.setInt(3, vo.getEventLeaderFlag());
				ps.setInt(4, vo.getPerformInventoryFlag());
				ps.setDate(5, Convert.formatSQLDate(vo.getDataLoadDate()));
				ps.setInt(6, vo.getActiveFlag());
				ps.setTimestamp(7, Convert.getCurrentTimestamp());
				if (! isInsert) ps.setInt(8, pkId);
				ps.addBatch();
			}
			int[] cnt = ps.executeBatch();
			log.debug((isInsert ? "inserted " : "updated ") + cnt.length + " auditor_xr records");
		} catch (SQLException sqle) {
			throw new ActionException("could not save auditor_xr items", sqle);
		} finally {
			DBUtil.close(ps);
		}
	}
	
	
	/**
	 * performs queries to 
	 * 1) retrieve the returns attached to the main event,
	 * 2) find existing _xr records we can overwrite
	 * 3) iterate all the recurrence events and bind a set of returns to each one
	 * 4) batch insert/update the records we need to put in the database
	 * 5) inactivate any existing records we're not going to use (for overwrites).
	 * 
	 * @param masterEventId
	 * @param recurringEvents
	 * @throws ActionException
	 */
	private void copyReturns(Integer masterEventId, List<InventoryEventVO> recurrenceEvents) throws ActionException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = null;
		List<InventoryEventReturnVO> returns = new ArrayList<InventoryEventReturnVO>();
		
		//find the masterEvent's  _xr records we're going to need to re-insert "X" times over.
		sql = new StringBuilder();
		sql.append("select product_id, quantity_no, lot_number_txt, active_flg from ");
		sql.append(customDb).append("RAM_EVENT_RETURN_XR ");
		sql.append("where inventory_event_id=?");
		log.debug(sql + "|" + masterEventId);
		PreparedStatement ps = null;
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
		
		
		//find records already in there we can overwrite (instead of deleting all & inserting)
		sql = new StringBuilder();
		sql.append("select event_return_id from ").append(customDb).append("RAM_EVENT_RETURN_XR ");
		sql.append("where inventory_event_id in (0");
		for (@SuppressWarnings("unused") InventoryEventVO vo : recurrenceEvents) 	sql.append(",?");
		sql.append(")");
		List<Integer> reusableIds = loadReuseableIds(sql.toString(), recurrenceEvents);
		log.debug("found " + reusableIds.size() + " records we can reuse/replace");
		
		
		//Flatten all the write transactions into a single list.  
		//Then we'll run two batch transactions against this list; one for the updates and one for the inserts
		List<InventoryEventReturnVO> records = new ArrayList<InventoryEventReturnVO>();
		for (InventoryEventVO vo : recurrenceEvents) {
			//don't bind to dead events
			if (! vo.isActive()) continue;
			
			//copy all the returns from the master record into each recurrence event.  (double-loop)
			for (InventoryEventReturnVO ret : returns) {
				InventoryEventReturnVO newRec = new InventoryEventReturnVO();
				if (! reusableIds.isEmpty()) {
					//reassign the first available pkID to this record, while removing it from the list
					newRec.setEventReturnId(reusableIds.remove(0));
				}
				newRec.setInventoryEventId(vo.getInventoryEventId()); //from the event
				newRec.setProductId(ret.getProductId());
				newRec.setQuantity(ret.getQuantity());
				newRec.setLotNumber(ret.getLotNumber());
				newRec.setActiveFlag(ret.getActiveFlag());
				records.add(newRec);
			}
		}
		
		
		if (records.size() > 0) {
			//batch update of reused records
			sql = new StringBuilder();
			sql.append("update ").append(customDb).append("RAM_EVENT_RETURN_XR ");
			sql.append("set inventory_event_id=?, product_id=?, quantity_no=?, ");
			sql.append("lot_number_txt=?, active_flg=?, update_dt=? ");
			sql.append("where event_return_id=?");
			batchReturns(sql.toString(), false, records);
		
			//batch insert of new records
			sql = new StringBuilder();
			sql.append("insert into ").append(customDb).append("RAM_EVENT_RETURN_XR ");
			sql.append("(inventory_event_id, product_id, quantity_no, lot_number_txt, ");
			sql.append("active_flg, create_dt) values (?,?,?,?,?,?)");
			batchReturns(sql.toString(), true, records);
		}
		
		
		//if there are reusableIds we didn't use, leave them as-is and just mark inactive
		if (! reusableIds.isEmpty()) {
			sql = new StringBuilder();
			sql.append("update ").append(customDb).append("RAM_EVENT_RETURN_XR ");
			sql.append("set active_flg=?, update_dt=? where event_return_id in (0");
			for (@SuppressWarnings("unused") Integer i : reusableIds) sql.append(",?");
			sql.append(")");
			batchInactiveSurplus(sql.toString(), reusableIds);
		}
	}
	
	
	/**
	 * performs queries to 
	 * 1) retrieve the auditors attached to the main event,
	 * 2) find existing _xr records we can overwrite
	 * 3) iterate all the recurrence events and bind a set of auditors to each one
	 * 4) batch insert/update the records we need to put in the database
	 * 5) inactivate any existing records we're not going to use (for overwrites).
	 * 
	 * @param masterEventId
	 * @param recurringEvents
	 * @throws ActionException
	 */
	private void copyAuditors(Integer masterEventId, List<InventoryEventVO> recurrenceEvents) throws ActionException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = null;
		List<InventoryEventAuditorVO> auditors = new ArrayList<InventoryEventAuditorVO>();
		
		//find the masterEvent's  _xr records we're going to need to re-insert "X" times over.
		sql = new StringBuilder();
		sql.append("select auditor_id, event_leader_flg, perform_inventory_flg, data_load_dt, active_flg from ");
		sql.append(customDb).append("RAM_INVENTORY_EVENT_AUDITOR_XR ");
		sql.append("where inventory_event_id=?");
		log.debug(sql + "|" + masterEventId);
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, masterEventId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				InventoryEventAuditorVO vo = new InventoryEventAuditorVO();
				AuditorVO aud = new AuditorVO();
				aud.setAuditorId(rs.getInt(1));
				vo.setAuditor(aud);
				vo.setEventLeaderFlag(rs.getInt(2));
				vo.setPerformInventoryFlag(rs.getInt(3));
				vo.setDataLoadDate(rs.getDate(4));
				vo.setActiveFlag(rs.getInt(5));
				auditors.add(vo);
			}
		} catch (SQLException sqle) {
			throw new ActionException("could not select old auditor_xr items", sqle);
		} finally {
			DBUtil.close(ps);
		}
		log.debug("found " + auditors.size() + " auditors to propagate");
		
		
		//find records already in there we can overwrite (instead of deleting all & inserting)
		sql = new StringBuilder();
		sql.append("select inventory_event_auditor_xr_id from ").append(customDb);
		sql.append("RAM_INVENTORY_EVENT_AUDITOR_XR ");
		sql.append("where inventory_event_id in (0");
		for (@SuppressWarnings("unused") InventoryEventVO vo : recurrenceEvents) 	sql.append(",?");
		sql.append(")");
		List<Integer> reusableIds = loadReuseableIds(sql.toString(), recurrenceEvents);
		log.debug("found " + reusableIds.size() + " records we can reuse/replace");
		
		
		//Flatten all the write transactions into a single list.  
		//Then we'll run two batch transactions against this list; one for the updates and one for the inserts
		List<InventoryEventAuditorVO> records = new ArrayList<InventoryEventAuditorVO>();
		for (InventoryEventVO vo : recurrenceEvents) {
			//don't bind to dead events
			if (! vo.isActive()) continue;
			
			//copy all the returns from the master record into each recurrence event.  (double-loop)
			for (InventoryEventAuditorVO ret : auditors) {
				InventoryEventAuditorVO newRec = new InventoryEventAuditorVO();
				if (! reusableIds.isEmpty()) {
					//reassign the first available pkID to this record, while removing it from the list
					newRec.setInventoryEventAuditorId(reusableIds.remove(0));
				}
				newRec.setInventoryEventId(vo.getInventoryEventId()); //from the event
				newRec.setAuditor(ret.getAuditor());
				newRec.setEventLeaderFlag(ret.getEventLeaderFlag());
				newRec.setPerformInventoryFlag(ret.getPerformInventoryFlag());
				newRec.setDataLoadDate(ret.getDataLoadDate());
				newRec.setActiveFlag(ret.getActiveFlag());
				records.add(newRec);
			}
		}
		
		
		if (records.size() > 0) {
			//batch update of reused records
			sql = new StringBuilder();
			sql.append("update ").append(customDb).append("RAM_INVENTORY_EVENT_AUDITOR_XR ");
			sql.append("set inventory_event_id=?, auditor_id=?,  event_leader_flg=?, ");
			sql.append("perform_inventory_flg=?, data_load_dt=?, active_flg=?, update_dt=? ");
			sql.append("where inventory_event_auditor_xr_id=?");
			batchAuditors(sql.toString(), false, records);
		
			//batch insert of new records
			sql = new StringBuilder();
			sql.append("insert into ").append(customDb).append("RAM_INVENTORY_EVENT_AUDITOR_XR ");
			sql.append("(inventory_event_id, auditor_id, event_leader_flg, perform_inventory_flg, data_load_dt, ");
			sql.append("active_flg, create_dt) values (?,?,?,?,?,?,?)");
			batchAuditors(sql.toString(), true, records);
		}
		
		
		//if there are reusableIds we didn't use, leave them as-is and just mark inactive
		if (! reusableIds.isEmpty()) {
			sql = new StringBuilder();
			sql.append("update ").append(customDb).append("RAM_INVENTORY_EVENT_AUDITOR_XR ");
			sql.append("set active_flg=?, update_dt=? where inventory_event_auditor_xr_id in (0");
			for (@SuppressWarnings("unused") Integer i : reusableIds) sql.append(",?");
			sql.append(")");
			batchInactiveSurplus(sql.toString(), reusableIds);
		}
	}
	
	
	/**
	 * copy data from the master record into the event recurrence being created
	 * @param event
	 * @param master
	 * @return
	 */
	private InventoryEventVO populateEvent(InventoryEventVO event, InventoryEventVO master) {
		// note we're not setting the pkId on these; we want to preserve what's already there so we can update the DB.
		event.setInventoryEventGroupId(master.getInventoryEventGroupId());
		event.setCustomerLocationId(master.getCustomerLocationId());
		event.setComment(master.getComment());
		event.setInventoryCompleteDate(master.getInventoryCompleteDate());
		event.setDataLoadCompleteDate(master.getDataLoadCompleteDate());
		event.setVendorEventId(master.getVendorEventId());
		event.setCancellationComment(master.getCancellationComment());
		event.setPartialInventoryFlag(master.getPartialInventoryFlag());
		//event.setScheduleDate(master.getScheduleDate());
		//event.setActiveFlag(master.getActiveFlag());
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
