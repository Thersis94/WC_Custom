package com.wsla.util.migration.vo;

import java.util.Date;

/****************************************************************************
 * <p><b>Title:</b> SOExtendedFileVO.java</p>
 * <p><b>Description:</b> The SOXDD Excel file from Steve.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Feb 1, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class SOExtendedFileVO {

	private String soNumber;
	private Date createDate;
	private String operator; //call ctr rep
	private String custName; //Dueno=Owner
	private String retailer; //Tienda=Store
	private String storeNumber; //Tienda#
	private Date purchaseDate; //Fecha de Compra
	private String mountedTV; //Montado TV (yes/no)
	private String email; //Correo Electron=email
	private String approveSurvey; //Encuesta Aproba (yes/no)
	private double costInPesos;
	private double costInDollars;
	private String bankUFData; //Datos Banco UF
	private String atclCrememEnv; //ATCL CreMem ENV ??
	//private 
	
	/*
	 * S/O NUMBER	Date Created	Operador	Dueno	Tienda	Fecha de Compra	Montado TV ?	Correo Electron	Encuesta Aproba	Tienda #	
	 * Costo en pesos	Costo en usd	Datos Banco UF	ATCL CreMem ENV	FAB Fecha AUTH	Nota Cargo TDA	FAB DebMem TDA	FAB DepTDA Fech	
	 * ATCL DebMem ENV	FAB Datos DEP	FECH TDA REC DE	TDA REEM UF FEC	TDA APL NC TMP	POP Adjunto	Parte Utilizada	Parte Enviado	
	 * CAS Parte Recib	Ubicacion Prod	Clave Reparacio	GUIA	Parte Desc	Volver Parcial	Volver GUIA	UF Pend POP	Volv GUIA Fecha	Seg Producto	
	 * 1.Dest. de Prod	2.No. Fact CAS	3.Pago de CAS	Estatus Prim3 Desc	Producto Dispo  Desc
	 */

}
