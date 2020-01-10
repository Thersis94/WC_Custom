package com.wsla.action.ticket;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.gis.GeocodeLocation;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.common.WSLAConstants;
import com.wsla.data.ticket.LocationDistanceVO;
import com.wsla.data.ticket.TicketAssignmentVO;

/****************************************************************************
 * <b>Title</b>: CASSelectionAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the selection of the Service Center on a
 * ticket
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Oct 31, 2018
 * @updates:
 ****************************************************************************/

public class CASSelectionAction extends SBActionAdapter {

	/**
	 * 
	 */
	public CASSelectionAction() {
		super();
	}

	public CASSelectionAction(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super();
		this.dbConn = dbConn;
		this.attributes = attributes;
	}
	
	/**
	 * @param actionInit
	 */
	public CASSelectionAction(ActionInitVO actionInit) {
		super(actionInit);
	}
		
	/**
	 * Finds the closest CAS to the units location
	 * @param ticketId
	 * @return
	 */
	public List<GenericVO> getUserSelectionList(String ticketId, String locale ) {
		List<Object> vals = new ArrayList<>();
		
		// Get the source location
		GeocodeLocation gLoc = getSourceLocation(ticketId);
		if (gLoc == null) return new ArrayList<>();
		
		vals.add(gLoc.getLatitude());
		vals.add(gLoc.getLongitude());
		vals.add(locale.contains("US") ? "MI" : "KM");
		
		log.debug(vals);
		StringBuilder sql = new StringBuilder(768);
		sql.append("select cast(geoCalcDistance(cast(? as numeric), cast(? as numeric), ");
		sql.append("latitude_no, longitude_no, ?) as int) as distance, ");
		sql.append("location_id as key, ");
		sql.append("location_nm as value ");
		sql.append("from ").append(getCustomSchema()).append("wsla_provider a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_provider_location b ");
		sql.append("on a.provider_id = b.provider_id ");
		sql.append("where provider_type_id = 'CAS' and latitude_no is not null ");
		sql.append("order by distance, city_nm ");
		sql.append("limit 20 ");
		
		log.debug(sql.toString()+"|"+vals);
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		List<LocationDistanceVO> items = db.executeSelect(sql.toString(), vals, new LocationDistanceVO());
		List<GenericVO> data = new ArrayList<>();
		for (LocationDistanceVO item : items) {
			String value = item.getValue() + " (" + item.getDistance() + " " + (locale.contains("US") ? "MI" : "KM") + ")";
			data.add(new GenericVO(item.getKey(), value));
		}
		
		// Add the WSLA CAS Locations
		StringBuilder s = new StringBuilder(88);
		s.append("select location_id as key, location_nm || ' (WSLA)' as value ");
		s.append("from ").append(getCustomSchema()).append("wsla_provider_location ");
		s.append("where provider_id = '").append(WSLAConstants.WSLA_CAS_ID).append("' ");
		s.append("order by location_nm");
		data.addAll(db.executeSelect(s.toString(), null, new GenericVO()));
		
		return data;
	}
	
	
	/**
	 * Based upon the ticket assignments and the location of the unit, a source 
	 * location will be returned for the user
	 * @param ticketId
	 * @param loc
	 * @return
	 */
	public GeocodeLocation getSourceLocation(String ticketId) {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select location_id, latitude_no, longitude_no from ");
		sql.append(getCustomSchema()).append("wsla_assignment_location_view a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_ticket b ");
		sql.append("on a.ticket_id = b.ticket_id ");
		sql.append("where b.ticket_id = ? and assg_type_cd = unit_location_cd ");
		
		List<Object> vals = new ArrayList<>();
		vals.add(ticketId);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		List<GeocodeLocation> locs = db.executeSelect(sql.toString(), vals, new GeocodeLocation());
		return locs.isEmpty() ? null : locs.get(0);
	}

	/**
	 * checks to see if there is an existing Cas Assignment and returns it.  if no cass assigntment if found the method returns null
	 * @param ticketId
	 * @return
	 */
	public TicketAssignmentVO getExsitingCasSelection(String ticketId) {
		StringBuilder sql = new StringBuilder(175);
		
		sql.append(DBUtil.SELECT_CLAUSE).append("wta.* ").append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_ticket_assignment wta ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_ticket t  on wta.ticket_id = t.ticket_id ");
		sql.append(DBUtil.WHERE_CLAUSE).append("assg_type_cd = 'CAS' and (ticket_no = ? or t.ticket_id = ? ) ");
		
		List<Object> vals = new ArrayList<>();
		vals.add(ticketId);
		vals.add(ticketId);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		
		List<TicketAssignmentVO> casList = db.executeSelect(sql.toString(), vals, new TicketAssignmentVO());
		return casList.isEmpty() ? null : casList.get(0);
	}
}
