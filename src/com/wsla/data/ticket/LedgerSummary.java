package com.wsla.data.ticket;

/****************************************************************************
 * <b>Title</b>: LedgerSummary.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> List of standard summary elements to be added to the ledger.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 29, 2018
 * @updates:
 ****************************************************************************/

public enum LedgerSummary {
	
	CALL_RECVD ("Llamada Recibida"),
	CALL_FINISHED ("Llamada Concluida Información Procesada"),
	SCHEDULE_TRANSFER ("Servicio Programado"),
	SCHEDULE_TRANSFER_COMPLETE ("Equipo Ingresa a Centro de Servicio"),
	RAN_DIAGNOSTIC ("Equipo Pendiente de Diagnóstico"),
	DIAGNOSTIC_COMPLETED ("Diagnóstico Concluido"),
	CAS_REQUESTED_PARTS ("Refacción Solicitada por Centro de Servicio"),
	PARTS_REQUEST_REVIEWED ("Disponibilidad de Refacción por Parte de Proveedor"),
	PARTS_REQUEST_REJECTED ("Refacción no Disponible por Parte de Proveedor"),
	SHIPMENT_CREATED ("Envío Programado"),
	SHIPMENT_RECEIVED ("Envío Recibido"),
	SHIPMENT_COST ("Costo de Envío"),
	VALID_SERIAL_SAVED("El Número de Serie Capturado en la Órden de Servicio es Invalido"),
	SERIAL_UPDATED("El Número de Serie Capturado en la Órden de Servicio ha sido Actualizado"),
	INVALID_SERIAL_SAVED("El Número de Serie Capturado en la Órden de Servicio es Invalido"),
	SERIAL_APPROVED("Número de Serie Aprobado"),
	REPAIR_STATUS_CHANGED ("Estatus de Reparación Modificado"),
	DISPOSITION_CHANGED("Estatus de Tipo de Servicio Modificado"),
	ACTIVITY_ADDED ("Se Agrega Seguimiento"),
	CAS_ASSIGNED ("Se Asigna Centro de Servicio"),
	TICKET_CLONED ("Órden de Servicio Cerrada y Duplicada"),
	TICKET_CLOSED ("Órden de Servicio Cerrada"),
	REFUND_REJECTED ("Reembolso o Reemplazo Rechazado"),
	HARVEST_COMPETE ("Canibalización Completa / Piezas Disponibles: "),
	ASSET_LOADED ("Usuario Agrega Evidencia"),
	ASSET_REJECTED ("Evidencias Rechazadas"),
	ASSET_APPROVED ("Evidencias Aprobadas"),
	FINAL_ASSET_REJECTED ("Todo Evidencias Rechazadas"),
	FINAL_ASSET_APPROVED ("Todo Evidencias Aprobadas"),
	REPAIR_TYPE ("Tipo de Reparación"),
	HARVEST_AFTER_RECEIPT ("Equipo Listo para Canibalización"),
	REPAIR_AFTER_RECEIPT ("Nueva Órden de Servicio para Reparación de Equipo"), 
	RETAIL_OWNED_ASSET_NOT_REQUIRED ("Equipo de Piso de Venta no Requiere Comprobante de Compra"), 
	RESOLVED_DURING_CALL("Problema Resuelto en Llamada"),
	CREDIT_MEMO_CREATED("Nota de Cargo Creada"),
	CREDIT_MEMO_APPROVED("Nota de Cargo Aprobada"),
	ASSETS_BYPASSED("Sin Evidencias Requeridas"),
	REFUND_VERIFIED("Reembolso Verificado");
	
	public final String summary;
	LedgerSummary(String summary) { this.summary = summary; }
}

