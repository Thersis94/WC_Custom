package com.wsla.util.migration.vo;

import java.util.Date;

import com.siliconmtn.annotations.Importable;

/****************************************************************************
 * <p><b>Title:</b> ServiceOrderVO.java</p>
 * <p><b>Description:</b> models DM-ServOrd Closed.xlsx provided by Steve</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Jan 22, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class ServiceOrderVO {

	private String soNo;
	private String originalSoNo;
	private Date captureDate; //createDate for ticket record
	private String productId;
	private String serialNo;
	private String status;
	private String status1;
	private int status2;
	private Date startDate; //date work started on the ticket
	private int aging; //ENVEJECIMIENTO
	private String casId; //CAS - presumably an assigned-to casLocationId
	private String partsGuide; //manual #
	private String returnGuide; //manual #
	private Date partsShipDate; //FECHA DE ENVIO DE PARTE - date of sending part
	private String partsRcvdDate; //FECHA CAS RECIBIO PARTE - CAS Date Received part
	private Date workStartDate; //DATE ESTATUS FECHA/INICIO - status start date
	private Date agendaDate; //FECHA AGENDADA - talk to Steve about this
	private Date dispatchDate; //FECHA DISPATCHED
	private String wslaComments; //COMMENTARIOS DE WSLA - wsla comments (this looks like a boolean 'x', TBD)
	private String casComments; // COMMENTARIOS DE CAS - cas comments (this looks like a boolean 'x', TBD)
	private String oemId; //FABRICANTE - maker
	private String location; // UBICACION - location of set - sometimes a CAS, sometimes w/consumer, sometimes other
	private String serviceDisposition; //DESC DE SERVICIO CORTO - short service description
	private String retailerId; //TIENDA - store
	private String problemDescription; //PROBLEMA REPORTADO DE CLIENTE
	private String partsNeeded; //PARTE NECESARIO - N/A means none, otherwise these need to cross-ref to the set's part descriptions


	public String getSoNo() {
		return soNo;
	}
	public String getOriginalSoNo() {
		return originalSoNo;
	}
	public Date getCaptureDate() {
		return captureDate;
	}
	public String getProductId() {
		return productId;
	}
	public String getSerialNo() {
		return serialNo;
	}
	public String getStatus() {
		return status;
	}
	public String getStatus1() {
		return status1;
	}
	public int getStatus2() {
		return status2;
	}
	public Date getStartDate() {
		return startDate;
	}
	public int getAging() {
		return aging;
	}
	public String getCasId() {
		return casId;
	}
	public String getPartsGuide() {
		return partsGuide;
	}
	public String getReturnGuide() {
		return returnGuide;
	}
	public Date getPartsShipDate() {
		return partsShipDate;
	}
	public String getPartsRcvdDate() {
		return partsRcvdDate;
	}
	public Date getWorkStartDate() {
		return workStartDate;
	}
	public Date getAgendaDate() {
		return agendaDate;
	}
	public Date getDispatchDate() {
		return dispatchDate;
	}
	public String getWslaComments() {
		return wslaComments;
	}
	public String getCasComments() {
		return casComments;
	}
	public String getOemId() {
		return oemId;
	}
	public String getLocation() {
		return location;
	}
	public String getServiceDisposition() {
		return serviceDisposition;
	}
	public String getRetailerId() {
		return retailerId;
	}
	public String getProblemDescription() {
		return problemDescription;
	}
	public String getPartsNeeded() {
		return partsNeeded;
	}


	@Importable(name="ORDEN DE SERVICIO (O/S)")
	public void setSoNo(String soNo) {
		this.soNo = soNo;
	}
	@Importable(name="Original O/S")
	public void setOriginalSoNo(String originalSoNo) {
		this.originalSoNo = originalSoNo;
	}
	@Importable(name="FECHA  DE CAPTURA")
	public void setCaptureDate(Date captureDate) {
		this.captureDate = captureDate;
	}
	@Importable(name="MODELO")
	public void setProductId(String productId) {
		this.productId = productId;
	}
	@Importable(name="NO. SERIE")
	public void setSerialNo(String serialNo) {
		this.serialNo = serialNo;
	}
	@Importable(name="ESTATUS")
	public void setStatus(String status) {
		this.status = status;
	}
	@Importable(name="ESTATUS PRIMARIO")
	public void setStatus1(String status1) {
		this.status1 = status1;
	}
	@Importable(name="ESTATUS SECONDARIO")
	public void setStatus2(int status2) {
		this.status2 = status2;
	}
	@Importable(name="")
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	@Importable(name="ENVEJECIMIENTO")
	public void setAging(int aging) {
		this.aging = aging;
	}
	@Importable(name="CAS")
	public void setCasId(String casId) {
		this.casId = casId;
	}
	@Importable(name="GUIA SALIENTE")
	public void setPartsGuide(String partsGuide) {
		this.partsGuide = partsGuide;
	}
	@Importable(name="VOLVER GUIA")
	public void setReturnGuide(String returnGuide) {
		this.returnGuide = returnGuide;
	}
	@Importable(name="FECHA DE ENVIO DE PARTE")
	public void setPartsShipDate(Date partsShipDate) {
		this.partsShipDate = partsShipDate;
	}
	@Importable(name="FECHA CAS RECIBIO PARTE")
	public void setPartsRcvdDate(String partsRcvdDate) {
		this.partsRcvdDate = partsRcvdDate;
	}
	@Importable(name="DATE ESTATUS FECHA/INICIO")
	public void setWorkStartDate(Date workStartDate) {
		this.workStartDate = workStartDate;
	}
	@Importable(name="FECHA AGENDADA")
	public void setAgendaDate(Date agendaDate) {
		this.agendaDate = agendaDate;
	}
	@Importable(name="FECHA DISPATCHED")
	public void setDispatchDate(Date dispatchDate) {
		this.dispatchDate = dispatchDate;
	}
	@Importable(name="COMMENTARIOS DE WSLA")
	public void setWslaComments(String wslaComments) {
		this.wslaComments = wslaComments;
	}
	@Importable(name="COMMENTARIOS DE CAS")
	public void setCasComments(String casComments) {
		this.casComments = casComments;
	}
	@Importable(name="FABRICANTE")
	public void setOemId(String oemId) {
		this.oemId = oemId;
	}
	@Importable(name="UBICACION")
	public void setLocation(String location) {
		this.location = location;
	}
	@Importable(name="DESC DE SERVICIO CORTO")
	public void setServiceDisposition(String serviceDisposition) {
		this.serviceDisposition = serviceDisposition;
	}
	@Importable(name="TIENDA")
	public void setRetailerId(String retailerId) {
		this.retailerId = retailerId;
	}
	@Importable(name="PROBLEMA REPORTADO DE CLIENTE")
	public void setProblemDescription(String problemDescription) {
		this.problemDescription = problemDescription;
	}
	@Importable(name="PARTE NECESARIO")
	public void setPartsNeeded(String partsNeeded) {
		this.partsNeeded = partsNeeded;
	}
}