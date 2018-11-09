package com.wsla.action.admin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
// JDK 1.8.x
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.Convert;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.ticket.CASTicketAssignmentVO;
import com.wsla.data.ticket.OwnerTicketAssignmentVO;
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
		log.debug("Schedule action retrieve");
		
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

		StringBuilder sql = new StringBuilder(780);
		sql.append("select t.ticket_no,  ts.*, ");
		sql.append("ov.ticket_assg_id as own_ticket_assg_id, ov.location_id as own_location_id, ov.location_id as own_user_id, ts.ticket_id as own_ticket_id, 1 as own_owner_flg, ov.assg_type_cd as own_assg_type_cd,  ");
		sql.append("ov.address_txt as own_address_txt, ov.address2_txt as own_address2_txt, ov.state_cd as own_state_cd, ov.city_nm as own_city_nm, ov.location_nm as own_location_nm, ov.zip_cd as own_zip_cd, ");
		sql.append("cta.ticket_assg_id as cas_ticket_assg_id, cta.location_id as cas_location_id, cta.location_id as cas_user_id, ts.ticket_id as cas_ticket_id, 0 as cas_owner_flg, cta.assg_type_cd as cas_assg_type_cd, ");
		sql.append("cl.address_txt as cas_address_txt, cl.address2_txt as cas_address2_txt, cl.state_cd as cas_state_cd, cl.city_nm as cas_city_nm, cl.location_nm as cas_location_nm, cl.zip_cd as cas_zip_cd ");
		sql.append("from ").append(getCustomSchema()).append("wsla_ticket_schedule ts ");
		sql.append("inner join  ").append(getCustomSchema()).append("wsla_ticket t on ts.ticket_id = t.ticket_id ");
		sql.append("inner join  ").append(getCustomSchema()).append("wsla_ticket_assignment cta on cta.ticket_assg_id = ts.cas_location_id ");
		sql.append("inner join  ").append(getCustomSchema()).append("wsla_provider_location cl on cta.location_id = cl.location_id ");
		sql.append("inner join  ").append(getCustomSchema()).append("wsla_assignment_location_view ov on ov.ticket_assg_id = ts.owner_location_id ");

		GridDataVO<TicketScheduleVO> data  = new GridDataVO<>();
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					
					TicketScheduleVO tso = new TicketScheduleVO(rs);
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