package com.wsla.util.migration.vo;

import java.util.Date;

import com.siliconmtn.annotations.Importable;

/****************************************************************************
 * <p><b>Title:</b> ClosedSOFileVO.java</p>
 * <p><b>Description:</b> models JLAWKCLO.xlsx provided by Steve.  ~11k closed Service Orders.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Jan 23, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class ClosedSOFileVO {

	//ticket
	private String serviceOrderNo; //ORDEN DE SERVICIO
	private String typeOfService; //TIPO DE SERVICIO
	private String owner; //DUEÑO
	private String name; //NOMBRE - (customer/owner name?)
	private Date closeDate; //FECHA CONCLUIDO
	private int aging; //ANTIGUEDAD - aging
	private String finalStatus; //ESTATUS FINAL
	private Date autWmtDate; //FECHA AUT. WMT - (not sure what this means!)
	private String tdaNote; //NOTA DE CARGO TDA - (not sure what this means!)
	private String creditNote; //NOTA DE CREDITO FAB
	private Date depositDate; //FECHA DEPOSITO A WMT
	private String productLocation; //UBICACION DE PRODUCTO
	private String city; //CIUDAD
	private String initialContact; //CONTACTO INICIAL
	private String warrantyStatus; //CON / SIN GARANTIA
	private String status; //ESTATUS

	//unit
	private String oemId; //FABRICANTE
	private String model; //MODELO
	private String serialNumber; //NUMERO DE SERIE
	private String retailer; //TIENDA
	private Date registrationDate; //FECHA DE REGISTRO
	private Date purchaseDate; //FECHA DE COMPRA

	//diagnostics
	private String diagRep; //EX Rep
	private String diagCode; //Action Code 1
	private String diagShortDesc; //Action Short Desc
	private String diagLongDesc; //Action Description
	private String reportedFailureCode; //CODIGO DE FALLA REPORTADO
	private String reportedFailureDesc; //DESCRIPCION DE FALLA REPORTADO
	private String partNeeded; //PARTE UTILIZADA


	public String getServiceOrderNo() {
		return serviceOrderNo;
	}
	public String getTypeOfService() {
		return typeOfService;
	}
	public String getOwner() {
		return owner;
	}
	public String getName() {
		return name;
	}
	public Date getCloseDate() {
		return closeDate;
	}
	public int getAging() {
		return aging;
	}
	public String getFinalStatus() {
		return finalStatus;
	}
	public Date getAutWmtDate() {
		return autWmtDate;
	}
	public String getTdaNote() {
		return tdaNote;
	}
	public String getCreditNote() {
		return creditNote;
	}
	public Date getDepositDate() {
		return depositDate;
	}
	public String getProductLocation() {
		return productLocation;
	}
	public String getCity() {
		return city;
	}
	public String getInitialContact() {
		return initialContact;
	}
	public Date getPurchaseDate() {
		return purchaseDate;
	}
	public String getWarrantyStatus() {
		return warrantyStatus;
	}
	public String getStatus() {
		return status;
	}
	public String getOemId() {
		return oemId;
	}
	public String getModel() {
		return model;
	}
	public Date getRegistrationDate() {
		return registrationDate;
	}
	public String getSerialNumber() {
		return serialNumber;
	}
	public String getRetailer() {
		return retailer;
	}
	public String getDiagRep() {
		return diagRep;
	}
	public String getDiagCode() {
		return diagCode;
	}
	public String getDiagShortDesc() {
		return diagShortDesc;
	}
	public String getDiagLongDesc() {
		return diagLongDesc;
	}
	public String getReportedFailureCode() {
		return reportedFailureCode;
	}
	public String getReportedFailureDesc() {
		return reportedFailureDesc;
	}
	public String getPartNeeded() {
		return partNeeded;
	}


	@Importable(name="ORDEN DE SERVICIO")
	public void setServiceOrderNo(String serviceOrderNo) {
		this.serviceOrderNo = serviceOrderNo;
	}
	@Importable(name="TIPO DE SERVICIO")
	public void setTypeOfService(String typeOfService) {
		this.typeOfService = typeOfService;
	}
	@Importable(name="DUEÑO")
	public void setOwner(String owner) {
		this.owner = owner;
	}
	@Importable(name="NOMBRE")
	public void setName(String name) {
		this.name = name;
	}
	@Importable(name="FECHA CONCLUIDO")
	public void setCloseDate(Date closeDate) {
		this.closeDate = closeDate;
	}
	@Importable(name="ANTIGUEDAD")
	public void setAging(int antiquity) {
		this.aging = antiquity;
	}
	@Importable(name="ESTATUS FINAL")
	public void setFinalStatus(String finalStatus) {
		this.finalStatus = finalStatus;
	}
	@Importable(name="FECHA AUT. WMT")
	public void setAutWmtDate(Date autWmtDate) {
		this.autWmtDate = autWmtDate;
	}
	@Importable(name="NOTA DE CARGO TDA")
	public void setTdaNote(String tdaNote) {
		this.tdaNote = tdaNote;
	}
	@Importable(name="NOTA DE CREDITO FAB")
	public void setCreditNote(String creditNote) {
		this.creditNote = creditNote;
	}
	@Importable(name="FECHA DEPOSITO A WMT")
	public void setDepositDate(Date depositDate) {
		this.depositDate = depositDate;
	}
	@Importable(name="UBICACION DE PRODUCTO")
	public void setProductLocation(String productLocation) {
		this.productLocation = productLocation;
	}
	@Importable(name="CIUDAD")
	public void setCity(String city) {
		this.city = city;
	}
	@Importable(name="CONTACTO INICIAL")
	public void setInitialContact(String initialContact) {
		this.initialContact = initialContact;
	}
	@Importable(name="FECHA DE COMPRA")
	public void setPurchaseDate(Date purchaseDate) {
		this.purchaseDate = purchaseDate;
	}
	@Importable(name="CON / SIN GARANTIA")
	public void setWarrantyStatus(String warrantyStatus) {
		this.warrantyStatus = warrantyStatus;
	}
	@Importable(name="ESTATUS")
	public void setStatus(String status) {
		this.status = status;
	}
	@Importable(name="FABRICANTE")
	public void setOemId(String oemId) {
		this.oemId = oemId;
	}
	@Importable(name="MODELO")
	public void setModel(String model) {
		this.model = model;
	}
	@Importable(name="FECHA DE REGISTRO")
	public void setRegistrationDate(Date registrationDate) {
		this.registrationDate = registrationDate;
	}
	@Importable(name="NUMERO DE SERIE")
	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}
	@Importable(name="TIENDA")
	public void setRetailer(String retailer) {
		this.retailer = retailer;
	}
	@Importable(name="EX Rep")
	public void setDiagRep(String diagRep) {
		this.diagRep = diagRep;
	}
	@Importable(name="Action Code 1")
	public void setDiagCode(String diagCode) {
		this.diagCode = diagCode;
	}
	@Importable(name="Action Short Desc")
	public void setDiagShortDesc(String diagShortDesc) {
		this.diagShortDesc = diagShortDesc;
	}
	@Importable(name="Action Description")
	public void setDiagLongDesc(String diagLongDesc) {
		this.diagLongDesc = diagLongDesc;
	}
	@Importable(name="CODIGO DE FALLA REPORTADO")
	public void setReportedFailureCode(String reportedFailureCode) {
		this.reportedFailureCode = reportedFailureCode;
	}
	@Importable(name="DESCRIPCION DE FALLA REPORTADO")
	public void setReportedFailureDesc(String reportedFailureDesc) {
		this.reportedFailureDesc = reportedFailureDesc;
	}
	@Importable(name="PARTE UTILIZADA")
	public void setPartNeeded(String partNeeded) {
		this.partNeeded = partNeeded;
	}
}