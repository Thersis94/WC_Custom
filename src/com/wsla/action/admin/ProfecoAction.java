package com.wsla.action.admin;

// JDK 1.8.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

// WSLA Libs
import com.wsla.data.admin.ProfecoVO;
import com.wsla.data.ticket.TicketDataVO;

/****************************************************************************
 * <b>Title</b>: ProfecoAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the saving of the profeco ticket data
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Oct 7, 2019
 * @updates:
 ****************************************************************************/
public class ProfecoAction extends SBActionAdapter {

	public static final String AJAX_KEY = "profeco";
	
	/**
	 * 
	 */
	public ProfecoAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public ProfecoAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		
		if (req.getBooleanParameter("hearing")) {
			setModuleData(getHearings(req.getParameter("ticketId")));
		} else {
			BSTableControlVO bst = new BSTableControlVO(req, ProfecoVO.class);
			setModuleData(getProfecoList(req.getParameter("profecoStatusFilter", "IN_PROFECO"), bst));
		}
	}
	
	/**
	 * gets the list of hearings for a given service order
	 * @param ticketId
	 * @return
	 */
	public List<TicketDataVO> getHearings(String ticketId) {
		List<Object> vals = new ArrayList<>();
		vals.add(ticketId);
		
		StringBuilder sql = new StringBuilder(128);
		sql.append("select * from ").append(getCustomSchema()).append("wsla_ticket_data ");
		sql.append("where attribute_cd = 'attr_profecoHearing' and ticket_id = ? ");
		sql.append("order by value_txt desc ");
		log.debug(sql.length() +  "|" + sql + vals);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), vals, new TicketDataVO());
	}
	
	/**
	 * Gets a list of profecos
	 * @param statusCode
	 * @return
	 */
	public GridDataVO<ProfecoVO> getProfecoList(String statusCode, BSTableControlVO bst) {
		List<Object> params = new ArrayList<>();
		
		
		StringBuilder sql = new StringBuilder(864);
		sql.append("select t.ticket_id, ticket_no, profeco_status_cd, ");
		sql.append("first_nm || ' ' || last_nm as user_nm, ");
		sql.append("a.value_txt as case_number_txt, b.value_txt as start_dt, ");
		sql.append("c.value_txt as resolution_dt ");
		sql.append("from custom.wsla_ticket t ");
		sql.append("inner join custom.wsla_ticket_assignment ta ");
		sql.append("on t.ticket_id = ta.ticket_id and ta.assg_type_cd = 'CALLER' ");
		sql.append("inner join custom.wsla_user u on ta.user_id = u.user_id ");
		sql.append("left outer join custom.wsla_ticket_data a ");
		sql.append("on t.ticket_id = a.ticket_id and a.attribute_cd = 'attr_profecoCase' ");
		sql.append("left outer join custom.wsla_ticket_data b ");
		sql.append("on t.ticket_id = b.ticket_id and b.attribute_cd = 'attr_profecoStartDate' ");
		sql.append("left outer join custom.wsla_ticket_data c ");
		sql.append("on t.ticket_id = c.ticket_id and c.attribute_cd = 'attr_profecoResolutionDate' ");
		sql.append("where 1 = 1 ");
		if ("ALL".equalsIgnoreCase(statusCode)) {
			sql.append("and profeco_status_cd in (?, ?) ");
			params.add("IN_PROFECO");
			params.add("PROFECO_COMPLETE");
		} else {
			sql.append("and profeco_status_cd = ? ");
			params.add(statusCode);
		}
		
		// Filter
		if (bst.hasSearch()) {
			sql.append("and (lower(ticket_no) like ? or lower(first_nm) like ? ");
			sql.append("or lower(last_nm) like ? or lower(a.value_txt) like ?) ");
			params.add(bst.getLikeSearch().toLowerCase());
			params.add(bst.getLikeSearch().toLowerCase());
			params.add(bst.getLikeSearch().toLowerCase());
			params.add(bst.getLikeSearch().toLowerCase());
		}
		log.debug(sql.length() + "|" + sql + params);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSQLWithCount(sql.toString(), params, new ProfecoVO(), bst);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {

		try {
			if (req.getBooleanParameter("hearing")) {
				TicketDataVO tdvo = new TicketDataVO(req);
				DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
				db.save(tdvo);
			} else {
				
				ProfecoVO pv = new ProfecoVO(req);
				saveProfeco(pv);
				setModuleData(null, 3);
			}
		} catch (Exception e) {
			log.error("Unable to save", e);
			setModuleData(null, 0, e.getLocalizedMessage());
		}
	}
	
	/**
	 * 
	 * @param pv
	 * @throws SQLException 
	 */
	public void saveProfeco(ProfecoVO pv) 
	throws SQLException, InvalidDataException, DatabaseException {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select attribute_cd, data_entry_id from ");
		sql.append(getCustomSchema()).append("wsla_ticket_data ");
		sql.append("where ticket_id = ? and attribute_cd in ");
		sql.append("('attr_profecoCase', 'attr_profecoStartDate', 'attr_profecoResolutionDate') ");
		log.debug(sql + "|" + pv.getTicketId());
		
		// Get the existing entries
		Map<String, String> map = new HashMap<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, pv.getTicketId());
			
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) map.put(rs.getString(1), rs.getString((2)));
			}
		}
		
		// Insert / Update the records
		updateTicketData(map.get("attr_profecoCase"), "attr_profecoCase", pv.getTicketId(), pv.getCaseNumber());
		updateTicketData(map.get("attr_profecoStartDate"), "attr_profecoStartDate", pv.getTicketId(), pv.getStartDate());
		updateTicketData(map.get("attr_profecoResolutionDate"), "attr_profecoResolutionDate", pv.getTicketId(), pv.getResolutionDate());
	}
	
	/**
	 * Updates a single item in the ticket data for the profeco data
	 * @param id
	 * @param attr
	 * @param tId
	 * @param value
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	private void updateTicketData(String id, String attr, String tId, String value) 
	throws InvalidDataException, DatabaseException {
		TicketDataVO tdvo = new TicketDataVO();
		tdvo.setDataEntryId(id);
		tdvo.setTicketId(tId);
		tdvo.setAttributeCode(attr);
		tdvo.setValue(value);
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.save(tdvo);
	}
	
}
