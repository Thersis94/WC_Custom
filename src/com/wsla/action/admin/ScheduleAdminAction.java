package com.wsla.action.admin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
// JDK 1.8.x
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.wsla.data.provider.ProviderLocationVO;
import com.wsla.data.ticket.TicketAssignmentVO;
// WSLA Libs
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
		log.debug("$$$$$$$$$$$$$$$$ Schedule action retrieve");
		
		String providerId = req.getParameter("providerId");
		boolean hasActiveFlag = req.hasParameter("activeFlag");
		int activeFlag = Convert.formatInteger(req.getParameter("activeFlag"));
		
		setModuleData(getSchedules(providerId, activeFlag, hasActiveFlag, new BSTableControlVO(req, TicketScheduleVO.class)));
	}


	/**
	 * Gets a list of providers.  Since this list should be small (< 100)
	 * assuming client side pagination and filtering 
	 * @param providerType
	 * @param providerId
	 * @return
	 */
	public GridDataVO<TicketScheduleVO> getSchedules(String providerId,  int activeFlag, boolean hasActiveFlag, BSTableControlVO bst) {
		
		//TODO change from retailer to cas when one is available
		
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(600);
		sql.append("select * from  ").append(schema).append("wsla_ticket_schedule a inner join ").append(schema).append("wsla_assignment_location_view b on a.location_dest_id = b.ticket_assg_id ");
		sql.append("inner join ").append(schema).append("wsla_ticket t on a.ticket_id = t.ticket_id ");
		sql.append("where t.ticket_id in (select distinct(ticket_id) from ").append(schema).append("wsla_ticket_assignment where assg_type_cd = 'CAS') ");
		sql.append("union ");
		sql.append("select * from  ").append(schema).append("wsla_ticket_schedule a inner join ").append(schema).append("wsla_assignment_location_view b on a.location_src_id = b.ticket_assg_id ");
		sql.append("inner join ").append(schema).append("wsla_ticket dt on a.ticket_id = dt.ticket_id ");
		sql.append("where dt.ticket_id in (select distinct(ticket_id) from ").append(schema).append("wsla_ticket_assignment where assg_type_cd = 'CAS') ");
		
		GridDataVO<TicketScheduleVO> data  = new GridDataVO<>();
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			StringEncrypter se =  new StringEncrypter((String)getAttribute(Constants.ENCRYPT_KEY));

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					processResults(rs, data, se);
				}
			}
		} catch (Exception e) {
			log.error("could not get schedules or decrypt address ",e);
		}
		return data;
	}

	/**
	 * processes the ticket schedules and loads all related data
	 * @param rs 
	 * @param se 
	 * @param data 
	 * @throws SQLException 
	 * @throws EncryptionException 
	 * 
	 */
	private void processResults(ResultSet rs, GridDataVO<TicketScheduleVO> data, StringEncrypter se) throws Exception {
		boolean newTicket = true;
		String assgTypeCode = rs.getString("assg_type_cd");
		//build ticket schdule vo
		TicketScheduleVO tc = new TicketScheduleVO(rs);
		//loop the tickets and make sure its new or not
		for (TicketScheduleVO d : data.getRowData() ) {
			if (d.getTicketId().equals(tc.getTicketId())) {
				newTicket = false;
				tc = d;
			}
		}

		//get the assignment and set it to the source or desc accordingly
		TicketAssignmentVO ta = new TicketAssignmentVO(rs);
		ProviderLocationVO pv = new ProviderLocationVO(rs);
		
		if ("CALLER".equals(assgTypeCode) ) {
			pv.setAddress( se.decrypt(pv.getLocation().getAddress()));
			pv.setAddress2(se.decrypt(pv.getLocation().getAddress2()) );
		}
			
		ta.setLocation(pv);
		//place the location on the correct sub element
		if (tc.getLocationSourceId() != null && tc.getLocationSourceId().equals(StringUtil.checkVal(ta.getTicketAssignmentId()))) {
			tc.setLocationSource(ta);
		}else {
			tc.setLocationDestination(ta);
		}
		//if the ticket is still new add it to the list
		if(newTicket)data.getRowData().add(tc);
		
	}
}