package com.wsla.util.migration.vo;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.EnumUtil;
import com.siliconmtn.util.MapUtil;
import com.siliconmtn.util.StringUtil;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.data.ticket.TicketVO;

/****************************************************************************
 * <p><b>Title:</b> TicketTimeline.java</p>
 * <p><b>Description:</b> </p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Jul 3, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class TicketTimeline {

	protected static Logger log = Logger.getLogger(TicketTimeline.class);
	private Connection dbConn;
	private Map<String,String> statusCodeMap = new HashMap<>(80);

	public TicketTimeline(Connection dbConn, String schema) {
		super();
		this.dbConn = dbConn;

		//load the status codes
		String sql = StringUtil.join("select lower(status_nm) as key, status_cd as value from ", schema, "wsla_ticket_status");
		log.debug(sql);
		DBProcessor db = new DBProcessor(dbConn, schema);
		MapUtil.asMap(statusCodeMap, db.executeSelect(sql, null, new GenericVO()));
	}


	/**
	 * populate the ticket with its timeline events using the status code
	 * @param ticket
	 */
	public void populateTicketTimeline(TicketVO ticket) {
		List<String[]> events = getTimelineEvents(ticket.getStatusName());
		if (events.isEmpty()) return;

		Calendar cal = Calendar.getInstance();
		if (ticket.getCreateDate() != null) cal.setTime(ticket.getCreateDate());
		int tickMins = 0;

		//TODO load existing events.  Use Creation as seed for timing.  Replace closed event with chrronologically-correct one.

		for (String[] event : events) {
			//event[] = Status Name	Assigned	Billable Code	Billable Amount	Comments
			TicketLedgerVO ledger = new TicketLedgerVO();
			ledger.setTicketId(ticket.getTicketId());
			ledger.setDispositionBy(ticket.getUserId());
			if (!StringUtil.isEmpty(event[0])) ledger.setStatusCode(getStatusCodeFromName(event[0]));
			//as it turns out, we don't care about events[1] - its a cosmetic value shown in the report (not stored in the ticket_ledger table)
			if (!StringUtil.isEmpty(event[2])) ledger.setBillableActivityCode(event[2]);
			if (!StringUtil.isEmpty(event[3])) ledger.setBillableAmtNo(Convert.formatDouble(event[3]));
			if (!StringUtil.isEmpty(event[4])) ledger.setSummary(event[4]);
			//TODO Do we care about UnitLocation?  We're not setting it.

			//TODO set incrementing/chronological createDate - possibly use LineNumber from LNI data?
			tickMins += 30;
			cal.add(Calendar.MINUTE, tickMins);
			ledger.setCreateDate(cal.getTime());

			ticket.addTimeline(ledger);
		}
	}


	/**
	 * return the status code from the value off the map
	 * @param string
	 * @return
	 */
	private StatusCode getStatusCodeFromName(String statusName) {
		String sts = statusCodeMap.get(statusName.toLowerCase());
		if (sts == null)
			log.error("missing status: " + statusName);

		return EnumUtil.safeValueOf(StatusCode.class, sts);
	}


	/**
	 * Get the list of timeline events using the status code from the ticket
	 * @param status
	 * @return
	 */
	private List<String[]> getTimelineEvents(String status) {
		if (StringUtil.isEmpty(status)) return Collections.emptyList();

		switch (status) {
			case "CALL_CTR_RES": return callCenterResolved();
			case "REP_NO_PARTS": return repairNoParts();
			case "REPAIR_WITH_PARTS": return repairWithParts();
			case "REP_W_HAR": return repairWithHarvest();
			case "REFUND_RET_REPAIR": return refundRetRepair();
			case "REFUND_WITH_HARVEST": return refundWithHarvest();
			case "NO_REP_WAR": return noRepairWarranty();
			case "MAN_NOT_SPT": return manNotSpt();
			default: return Collections.emptyList();
		}
	}

	/**
	 * REFUND_WITH_HARVEST
	 */
	private List<String[]> refundWithHarvest() {
		List<String[]> events = new ArrayList<>(40);
		//Status Name	Assigned	Billable Code	Billable Amount	Comments
		events.add(new String[] {"Ticket Opened", "Call Center", "OPENED", "0", "Llamada Recibida"});
		events.add(new String[] {"Call Data Incomplete", "Customer Service", "", "0", "Llamada Concluida Información Procesada"});
		events.add(new String[] {"", "", "", "0", "Usuario Agrega Evidencia"});
		//		events.add(new String[] {"", "", "", "0", "Usuario Agrega Evidencia"});
		events.add(new String[] {"Approval Pending POP", "Customer Service", "", "0", "Usuario Agrega Evidencia"});
		events.add(new String[] {"", "", "", "0", "Evidencias Aprobadas"});
		//		events.add(new String[] {"", "", "", "0", "Evidencias Aprobadas"});
		//		events.add(new String[] {"", "", "", "0", "Evidencias Aprobadas"});
		events.add(new String[] {"User Data Complete", "Customer Service", "", "0", "Todo Evidencias Aprobadas"});
		events.add(new String[] {"CAS Assigned", "Service Center", "", "0", "Se Asigna Centro de Servicio"});
		//		events.add(new String[] {"CAS Assigned", "Service Center", "", "0", "Se Asigna Centro de Servicio"});
		events.add(new String[] {"Pickup Pending", "Service Center", "", "0", "Servicio Programado"});
		events.add(new String[] {"Pickup Complete", "Service Center", "", "0", "Equipo Ingresa a Centro de Servicio"});
		events.add(new String[] {"CAS Diagnotics", "Service Center", "CAS_IN_DAIG", "0", "Equipo Pendiente de Diagnóstico"});
		events.add(new String[] {"Unrepairable", "Customer Service", "", "0", "Estatus de Tipo de Servicio Modificado : NONREPAIRABLE"});
		events.add(new String[] {"CAS Repair Complete", "Service Center", "", "0", ""});
		events.add(new String[] {"Replacement Requested", "Customer Service", "", "0", "Estatus de Reparación Modificado: Unit is damaged beyond repair.  Need to perform a refund/replace"});
		events.add(new String[] {"Refund Requested", "Customer Service", "", "0", ""});
		events.add(new String[] {"", "", "", "0", "Nota de Cargo Creada"});
		events.add(new String[] {"Defective Pending Shipment", "Service Center", "", "0", "Envío Programado"});
		events.add(new String[] {"Defective Shipped", "Service Center", "", "0", "Envío Programado"});
		events.add(new String[] {"Defective Received", "Warehouse", "", "0", "Envío Recibido"});
		events.add(new String[] {"Harvest Approved", "Warehouse", "", "0", "Equipo Listo para Canibalización"});
		events.add(new String[] {"Ticket Closed", "", "CLOSED", "2", ""});
		events.add(new String[] {"", "", "", "0", "Usuario Agrega Evidencia"});
		events.add(new String[] {"", "", "", "0", "Nota de Cargo Aprobada"});
		return events;
	}


	/**
	 * CALL_CTR_RES
	 */
	private List<String[]> callCenterResolved() {
		List<String[]> events = new ArrayList<>(40);
		//Status Name	Assigned	Billable Code	Billable Amount	Comments
		events.add(new String[] {"Ticket Opened", "Call Center", "OPENED", "0", "Llamada Recibida"});
		events.add(new String[] {"Problem Resolved", "Service Center", "", "0", ""});
		events.add(new String[] {"Ticket Closed", "", "CLOSED", "0", "Llamada Concluida Información Procesada"});
		events.add(new String[] {"Ticket Closed", "", "CLOSED", "0", "Problema Resuelto en Llamada"});
		return events;
	}


	/**
	 * MAN_NOT_SPT
	 */
	private List<String[]> manNotSpt() {
		List<String[]> events = new ArrayList<>(40);
		//Status Name	Assigned	Billable Code	Billable Amount	Comments
		events.add(new String[] {"Ticket Opened", "Call Center", "OPENED", "0", "Llamada Recibida"});
		events.add(new String[] {"Ticket Closed", "", "CLOSED", "0", "Llamada Recibida"});
		return events;
	}


	/**
	 * NO_REP_WAR
	 */
	private List<String[]> noRepairWarranty() {
		List<String[]> events = new ArrayList<>(40);
		//Status Name	Assigned	Billable Code	Billable Amount	Comments
		events.add(new String[] {"Ticket Opened", "Call Center", "OPENED", "0", "Llamada Recibida"});
		events.add(new String[] {"Call Data Incomplete", "Customer Service", "", "0", "Llamada Concluida Información Procesada"});
		events.add(new String[] {"", "", "", "0", "Usuario Agrega Evidencia"});
		//		events.add(new String[] {"", "", "", "0", "Usuario Agrega Evidencia"});
		events.add(new String[] {"Approval Pending POP", "Customer Service", "", "0", "Usuario Agrega Evidencia"});
		events.add(new String[] {"", "", "", "0", "Evidencias Aprobadas"});
		//		events.add(new String[] {"", "", "", "0", "Evidencias Aprobadas"});
		//		events.add(new String[] {"", "", "", "0", "Evidencias Aprobadas"});
		events.add(new String[] {"User Data Complete", "Customer Service", "", "0", "Todo Evidencias Aprobadas"});
		events.add(new String[] {"CAS Assigned", "Service Center", "", "0", "Se Asigna Centro de Servicio"});
		//		events.add(new String[] {"CAS Assigned", "Service Center", "", "0", "Se Asigna Centro de Servicio"});
		events.add(new String[] {"Pickup Pending", "Service Center", "", "0", "Servicio Programado"});
		events.add(new String[] {"Pickup Complete", "Service Center", "", "0", "Equipo Ingresa a Centro de Servicio"});
		events.add(new String[] {"CAS Diagnotics", "Service Center", "CAS_IN_DAIG", "0", "Equipo Pendiente de Diagnóstico"});
		events.add(new String[] {"Unrepairable", "Customer Service", "", "0", "Estatus de Tipo de Servicio Modificado : NONREPAIRABLE"});
		events.add(new String[] {"CAS Repair Complete", "Service Center", "", "0", ""});
		events.add(new String[] {"Pending Equipment Return", "Service Center", "", "0", "Estatus de Reparación Modificado: Unit had screen damage caused by user"});
		events.add(new String[] {"Delivery Scheduled", "Service Center", "", "0", "Servicio Programado"});
		events.add(new String[] {"", "", "OTHER", "12", "Se Agrega Seguimiento: Cost to ship unit to CAS"});
		events.add(new String[] {"", "", "OTHER", "14", "Se Agrega Seguimiento: Cost to ship unit back to user"});
		events.add(new String[] {"Delivery Complete", "End User", "", "0", "Equipo Ingresa a Centro de Servicio"});
		events.add(new String[] {"Ticket Closed", "", "CLOSED", "2", "Órden de Servicio Cerrada"});
		return events;
	}


	/**
	 * REP_W_HAR
	 * @return
	 */
	private List<String[]> repairWithHarvest() {
		List<String[]> events = new ArrayList<>(40);
		//Status Name	Assigned	Billable Code	Billable Amount	Comments
		events.add(new String[] {"Ticket Opened", "Call Center", "OPENED", "0", "Llamada Recibida"});
		events.add(new String[] {"Call Data Incomplete", "Customer Service", "", "0", "Llamada Concluida Información Procesada"});
		events.add(new String[] {"", "", "", "0", "Usuario Agrega Evidencia"});
		//		events.add(new String[] {"", "", "", "0", "Usuario Agrega Evidencia"});
		events.add(new String[] {"Approval Pending pop", "Customer Service", "", "0", "Usuario Agrega Evidencia"});
		events.add(new String[] {"", "", "", "0", "Evidencias Aprobadas"});
		//		events.add(new String[] {"", "", "", "0", "Evidencias Aprobadas"});
		//		events.add(new String[] {"", "", "", "0", "Evidencias Aprobadas"});
		events.add(new String[] {"User Data Complete", "Customer Service", "", "0", "Todo Evidencias Aprobadas"});
		events.add(new String[] {"CAS Assigned", "Service Center", "", "0", "Se Asigna Centro de Servicio"});
		//		events.add(new String[] {"CAS Assigned", "Service Center", "", "0", "Se Asigna Centro de Servicio"});
		events.add(new String[] {"Pickup Pending", "Service Center", "", "0", "Servicio Programado"});
		events.add(new String[] {"Pickup Complete", "Service Center", "", "0", "Equipo Ingresa a Centro de Servicio"});
		events.add(new String[] {"CAS Diagnotics", "Service Center", "CAS_IN_DAIG", "0", "Equipo Pendiente de Diagnóstico"});
		events.add(new String[] {"Unrepairable", "Customer Service", "", "0", "Estatus de Tipo de Servicio Modificado : NONREPAIRABLE"});
		events.add(new String[] {"CAS Repair Complete", "Service Center", "", "0", ""});
		events.add(new String[] {"Replacement or Refund", "Customer Service", "", "0", "Estatus de Reparación Modificado: Not repairable economically"});
		events.add(new String[] {"Harvest Approved", "Warehouse", "", "0", "Equipo Listo para Canibalización"});
		events.add(new String[] {"Replacement Confirmed", "Customer Service", "", "0", ""});
		events.add(new String[] {"Replacement Delivery Scheduled", "Customer Service", "", "0", "Envío Programado"});
		events.add(new String[] {"Replacement Delivery Received", "Customer Service", "", "0", "Envío Recibido"});
		events.add(new String[] {"Pending Equipment Return", "Service Center", "", "0", ""});
		events.add(new String[] {"Delivery Scheduled", "Service Center", "", "0", "Servicio Programado"});
		events.add(new String[] {"Delivery Complete", "End User", "", "0", "Equipo Ingresa a Centro de Servicio"});
		events.add(new String[] {"Ticket Closed", "", "CLOSED", "2", "Órden de Servicio Cerrada"});
		return events;
	}


	/**
	 * REP_NO_PARTS
	 * @return
	 */
	private List<String[]> repairNoParts() {
		List<String[]> events = new ArrayList<>(40);
		//Status Name	Assigned	Billable Code	Billable Amount	Comments
		events.add(new String[] {"Ticket Opened", "Call Center", "OPENED", "0", "Llamada Recibida"});
		events.add(new String[] {"Call Data Incomplete", "Customer Service", "", "0", "Llamada Concluida Información Procesada"});
		events.add(new String[] {"", "", "", "0", "Usuario Agrega Evidencia"});
		//		events.add(new String[] {"", "", "", "0", "Usuario Agrega Evidencia"});
		events.add(new String[] {"Approval Pending POP", "Customer Service", "", "0", "Usuario Agrega Evidencia"});
		events.add(new String[] {"", "", "", "0", "Evidencias Aprobadas"});
		//		events.add(new String[] {"", "", "", "0", "Evidencias Aprobadas"});
		//		events.add(new String[] {"", "", "", "0", "Evidencias Aprobadas"});
		events.add(new String[] {"User Data Complete", "Customer Service", "", "0", "Todo Evidencias Aprobadas"});
		events.add(new String[] {"CAS Assigned", "Service Center", "", "0", "Se Asigna Centro de Servicio"});
		//		events.add(new String[] {"CAS Assigned", "Service Center", "", "0", "Se Asigna Centro de Servicio"});
		events.add(new String[] {"Pickup Pending", "Service Center", "", "0", "Servicio Programado"});
		//		events.add(new String[] {"Pickup Pending", "Service Center", "", "0", "Servicio Programado"});
		events.add(new String[] {"Pickup Complete", "Service Center", "", "0", "Equipo Ingresa a Centro de Servicio"});
		events.add(new String[] {"CAS Diagnotics", "Service Center", "CAS_IN_DAIG", "0", "Equipo Pendiente de Diagnóstico"});
		events.add(new String[] {"", "", "RP01", "0", "Tipo de Reparación: RP01"});
		events.add(new String[] {"CAS Repair Complete", "Service Center", "", "0", "Estatus de Tipo de Servicio Modificado : REPAIRED"});
		events.add(new String[] {"Pending Equipment Return", "Service Center", "", "0", ""});
		events.add(new String[] {"Delivery Scheduled", "Service Center", "", "0", "Servicio Programado"});
		events.add(new String[] {"Delivery Complete", "End User", "", "0", "Equipo Ingresa a Centro de Servicio"});
		events.add(new String[] {"Ticket Closed", "", "CLOSED", "2", "Órden de Servicio Cerrada"});
		return events;
	}


	/**
	 * REFUND_RET_REPAIR
	 * @return
	 */
	private List<String[]> refundRetRepair() {
		List<String[]> events = new ArrayList<>(40);
		//Status Name	Assigned	Billable Code	Billable Amount	Comments
		events.add(new String[] {"Ticket Opened", "Call Center", "OPENED", "0", "Llamada Recibida"});
		events.add(new String[] {"Call Data Incomplete", "Customer Service", "", "0", "Llamada Concluida Información Procesada"});
		events.add(new String[] {"", "", "", "0", "Usuario Agrega Evidencia"});
		//		events.add(new String[] {"", "", "", "0", "Usuario Agrega Evidencia"});
		events.add(new String[] {"Approval Pending POP", "Customer Service", "", "0", "Usuario Agrega Evidencia"});
		events.add(new String[] {"", "", "", "0", "Evidencias Aprobadas"});
		//		events.add(new String[] {"", "", "", "0", "Evidencias Aprobadas"});
		//		events.add(new String[] {"", "", "", "0", "Evidencias Aprobadas"});
		events.add(new String[] {"User Data Complete", "Customer Service", "", "0", "Todo Evidencias Aprobadas"});
		events.add(new String[] {"CAS Assigned", "Service Center", "", "0", "Se Asigna Centro de Servicio"});
		//		events.add(new String[] {"CAS Assigned", "Service Center", "", "0", "Se Asigna Centro de Servicio"});
		//		events.add(new String[] {"CAS Assigned", "Service Center", "", "0", "Se Asigna Centro de Servicio"});
		events.add(new String[] {"Pickup Pending", "Service Center", "", "0", "Servicio Programado"});
		events.add(new String[] {"Pickup Complete", "Service Center", "", "0", "Equipo Ingresa a Centro de Servicio"});
		events.add(new String[] {"CAS Diagnotics", "Service Center", "CAS_IN_DAIG", "0", "Equipo Pendiente de Diagnóstico"});
		events.add(new String[] {"Unrepairable", "Customer Service", "", "0", "Estatus de Tipo de Servicio Modificado : NONREPAIRABLE"});
		events.add(new String[] {"CAS Repair Complete", "Service Center", "", "0", ""});
		events.add(new String[] {"Replacement Requested", "Customer Service", "", "0", "Estatus de Reparación Modificado: Damage to the pixels on the screen as well as the main board.  Not economic to repair"});
		events.add(new String[] {"Refund Requested", "Customer Service", "", "0", ""});
		events.add(new String[] {"", "", "", "0", "Nota de Cargo Creada"});
		events.add(new String[] {"Defective Pending Shipment", "Service Center", "", "0", "Envío Programado"});
		events.add(new String[] {"Defective Shipped", "Service Center", "", "0", "Envío Programado"});
		events.add(new String[] {"Defective Received", "Warehouse", "", "0", "Envío Recibido"});
		events.add(new String[] {"Ticket Closed", "", "CLOSED", "2", "Nueva Órden de Servicio para Reparación de Equipo"});
		events.add(new String[] {"", "", "", "0", "Usuario Agrega Evidencia"});
		events.add(new String[] {"", "", "", "0", "Nota de Cargo Aprobada"});
		return events;
	}


	/**
	 * REPAIR_WITH_PARTS
	 * @return
	 */
	private List<String[]> repairWithParts() {
		List<String[]> events = new ArrayList<>(40);
		//Status Name	Assigned	Billable Code	Billable Amount	Comments
		events.add(new String[] {"Ticket Opened", "Call Center", "OPENED", "0", "Llamada Recibida"});
		events.add(new String[] {"Call Data Incomplete", "Customer Service", "", "0", "Llamada Concluida Información Procesada"});
		events.add(new String[] {"", "", "", "0", "Usuario Agrega Evidencia"});
		//		events.add(new String[] {"", "", "", "0", "Usuario Agrega Evidencia"});
		events.add(new String[] {"Approval Pending POP", "Customer Service", "", "0", "Usuario Agrega Evidencia"});
		events.add(new String[] {"", "", "", "0", "Evidencias Aprobadas"});
		//		events.add(new String[] {"", "", "", "0", "Evidencias Aprobadas"});
		//		events.add(new String[] {"", "", "", "0", "Evidencias Aprobadas"});
		events.add(new String[] {"User Data Complete", "Customer Service", "", "0", "Todo Evidencias Aprobadas"});
		events.add(new String[] {"CAS Assigned", "Service Center", "", "0", "Se Asigna Centro de Servicio"});
		//		events.add(new String[] {"CAS Assigned", "Service Center", "", "0", "Se Asigna Centro de Servicio"});
		events.add(new String[] {"Pickup Pending", "Service Center", "", "0", "Servicio Programado"});
		events.add(new String[] {"Pickup Complete", "Service Center", "", "0", "Equipo Ingresa a Centro de Servicio"});
		events.add(new String[] {"CAS Diagnotics", "Service Center", "CAS_IN_DAIG", "0", "Equipo Pendiente de Diagnóstico"});
		events.add(new String[] {"Repairable", "Service Center", "", "0", "Estatus de Tipo de Servicio Modificado : REPAIRABLE"});
		events.add(new String[] {"CAS Parts Requested", "Warehouse", "", "0", "Refacción Solicitada por Centro de Servicio"});
		events.add(new String[] {"CAS Ordered PARTS", "Warehouse", "CAS_PARTS_ORDERED", "2", "Disponibilidad de Refacción por Parte de Proveedor : Seems reasonable.  Approved"});
		events.add(new String[] {"Parts Shipped to CAS", "Service Center", "", "0", "Envío Programado"});
		events.add(new String[] {"Parts Received by CAS", "Service Center", "", "0", "Envío Recibido"});
		events.add(new String[] {"In Repair by CAS", "Service Center", "", "0", "Estatus de Reparación Modificado"});
		events.add(new String[] {"", "", "RP01", "0", "Tipo de Reparación: RP01"});
		events.add(new String[] {"CAS Repair Complete", "Service Center", "", "0", "Estatus de Tipo de Servicio Modificado : REPAIRED"});
		events.add(new String[] {"Pending Equipment Return", "Service Center", "", "0", ""});
		events.add(new String[] {"Delivery Scheduled", "Service Center", "", "0", "Servicio Programado"});
		events.add(new String[] {"Delivery Complete", "End User", "", "0", "Equipo Ingresa a Centro de Servicio"});
		events.add(new String[] {"Ticket Closed", "", "CLOSED", "2", "Órden de Servicio Cerrada"});
		return events;
	}
}