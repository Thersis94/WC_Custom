package com.ram.action.products;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ram.action.or.RAMCaseManager;
import com.ram.action.or.vo.RAMCaseItemVO;
import com.ram.action.or.vo.RAMCaseItemVO.RAMCaseType;
import com.ram.action.or.vo.RAMCaseVO;
import com.ram.action.report.vo.ProductCartReport;
import com.ram.datafeed.data.RAMProductVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.MessageSender;

/****************************************************************************
 * <b>Title</b>ProductCartAction.java<p/>
 * <b>Description: Handles cart functionality for the ram site.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Billy Larsen
 * @version 3.3.1
 * @since Aug. 25, 2017
 * <b>Changes: </b>
 * 		June 30, 2017 - Moved case search functionality to CaseSearchAction
 ****************************************************************************/

public class ProductCartAction extends SimpleActionAdapter {

	// Names for the request parameters related to this action
	public static final String HOSPITAL = "hospital";
	public static final String ROOM = "room";
	public static final String SURGEON = "surgeon";
	public static final String TIME = "time";
	public static final String CASE_ID = "caseId";
	public static final String CART = "cart";
	public static final String NOTES = "notes";
	public static final String HOSPITAL_REP = "hospitalRep";
	public static final String SALES_REP = "salesRep";
	public static final String SIGNATURES = "signatures";
	public static final String BASE_DOMAIN = "baseDomain";
	public static final String FORMAT = "format";
	public static final String SURG_DATE = "surgDate";
	public static final String SEARCH = "search";
	public static final String EMAIL_ARRAY = "emails[]";

	private enum SearchFields {
		productName("product_nm"),
		customerName("c.customer_nm"),
		customerProductId("cust_product_id"),
		gtinProductNumber("c.gtin_number_txt || cast(p.gtin_product_id as varchar(64))"),
		gtinProductId("c.gtin_number_txt || cast(p.gtin_product_id as varchar(64))");

		private String cloumnNm;

		SearchFields(String columnNm) {
			this.cloumnNm = columnNm;
		}

		public String getColumnName (){
			return cloumnNm;
		}
	}

	/**
	 * Build actions this widget can perform, sent by the request.
	 * 
	 */
	private enum WidgetBuildAction {
		saveCaseInfo, deleteCase, addProduct, deleteProduct, addSignature, finalize, sendEmails, saveNote, persistCase
	}

	/**
	 * Retrieve actions this widget can perform, sent by the request.
	 */
	private enum WidgetRetrieveAction {
		loadCase, loadReport, searchProducts, searchProductsOrder
	}


	public ProductCartAction() {
		super();
	}

	/**
	 * @param avo
	 */
	public ProductCartAction(ActionInitVO avo) {
		super(avo);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		RAMCaseManager rcm = new RAMCaseManager(attributes, dbConn, req);
		WidgetBuildAction wa = WidgetBuildAction.valueOf(req.getParameter("widgetAction"));

		try {
			switch (wa) {
				case saveCaseInfo:
					RAMCaseVO cvo = rcm.saveCase(req);
					putModuleData(cvo);
					break;
				case addProduct:
					RAMCaseItemVO civo = rcm.updateItem(req);
					putModuleData(civo);
					break;
				case deleteProduct:
					String caseItemId = rcm.removeCaseItem(req);
					putModuleData(caseItemId);
					break;
				case addSignature:
					rcm.addSignature(req);
					rcm.persistCasePerm(rcm.retrieveCase(req.getParameter(RAMCaseManager.RAM_CASE_ID)));
					break;
				case finalize:
					rcm.finalizeCaseInfo();
					req.setParameter(EMAIL_ARRAY, rcm.getEmailAddresses(), true);
					sendEmails(req);
					break;
				case sendEmails:
					sendEmails(req);
					break;
				case saveNote:
					rcm.saveNote(req);
					break;
				case persistCase:
					rcm.persistCasePerm(rcm.retrieveCase(req.getParameter(RAMCaseManager.RAM_CASE_ID)));
					break;
				case deleteCase:
					deleteCase(req);
					break;
			}
		} catch (Exception e) {
			log.error("Error managing case", e);
			throw new ActionException(e);
		}
	}
	
	/**
	 * Deletes a case form the database
	 * @param req
	 * @throws ActionException
	 */
	private void deleteCase(ActionRequest req) throws ActionException {
		RAMCaseVO cvo = new RAMCaseVO();
		cvo.setCaseId(req.getParameter(CASE_ID));
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			db.delete(cvo);
		} catch(Exception e) {
			log.error("unable to delete case", e);
			throw new ActionException("unable to delete case", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		RAMCaseVO cvo =null;
		RAMCaseManager rcm = new RAMCaseManager(attributes, dbConn, req);
		WidgetRetrieveAction wa = WidgetRetrieveAction.valueOf(req.getParameter("widgetAction"));
		String caseId = req.getParameter(CASE_ID);
		
		try {
			if (! req.hasParameter("skipCase")) cvo = rcm.retrieveCase(caseId);
			switch (wa) {
				case searchProductsOrder:
					searchProducts(req.getIntegerParameter("customerLocationId"), req);
					break;
				case loadCase:
					putModuleData(cvo);
					break;
				case loadReport:
					buildReport(cvo, req);
					break;
				case searchProducts:
					searchProducts(cvo, req);
					break;
			}
		} catch (Exception e) {
			log.error("Error retrieving case", e);
			throw new ActionException(e);
		}
	}

	/**
	 * Search for products that match the supplied search crteria
	 * @param req
	 * @throws ActionException
	 */
	private void searchProducts(RAMCaseVO cvo, ActionRequest req) {
		searchProducts(cvo.getCustomerLocationId(), req);
	}
	
	/**
	 * Retrieves products using the customer location id directly.  This allows the product
	 * search feature to be used without a caseId
	 * @param customerLocationId
	 * @param req
	 * @throws ActionException
	 */
	private void searchProducts(int customerLocationId, ActionRequest req) {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(256);
		StringBuilder cSql = new StringBuilder(256);
		List<Object> params = new ArrayList<>();
		
		// Build the select clause
		cSql.append("select count(*) as key ");
		getProductSearchSelect(sql);
		
		// Build the body and where clause
		getProductSearchFilter(cSql, req, true);
		getProductSearchFilter(sql, req, false);
		
		// Add the parameters to the queries if searching
		params.add(customerLocationId);
		if (req.hasParameter(SEARCH)) {
			String searchData = "%" + req.getParameter(SEARCH).toLowerCase() + "%";
			params.add(searchData);
			params.add(searchData);
			params.add(searchData);
		}
		
		// Get the count
		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		List<Object> prodCount = db.executeSelect(cSql.toString(), params, new GenericVO());
		int size = Convert.formatInteger(((GenericVO)prodCount.get(0)).getKey()+"");
		
		// Add the nav params
		params.add(Convert.formatInteger(req.getParameter("offset"), 0));
		params.add(Convert.formatInteger(req.getParameter("limit"), 10));
		
		// Get the product list
		List<Object> products = db.executeSelect(sql.toString(), params, new RAMProductVO());
		
		// Return the data
		super.putModuleData(products, size, false);
	}

	/**
	 * Builds the select clause for the product search
	 * @param sql
	 */
	private void getProductSearchSelect(StringBuilder sql) {
		sql.append("select p.product_id, p.cust_product_id, desc_txt, short_desc, c.customer_nm, l.kit_layer_id, ");
		sql.append("c.gtin_number_txt || cast(p.gtin_product_id as varchar(64)) as gtin_number_txt, product_nm, i.location_item_master_id ");
	}

	/**
	 * Build the sql for the product search
	 * @param req
	 * @param fields
	 * @param searchType
	 * @return
	 */
	private void getProductSearchFilter (StringBuilder sql, ActionRequest req, boolean count) {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("ram_product p ");
		sql.append("item".equalsIgnoreCase(req.getParameter("productType")) ? DBUtil.INNER_JOIN : DBUtil.LEFT_OUTER_JOIN);
		sql.append(schema).append("ram_location_item_master i on p.product_id = i.product_id and customer_location_id = ? ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("ram_customer c on c.customer_id = p.customer_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("ram_kit_layer l on l.product_id = p.product_id ");
		sql.append("where p.active_flg = 1 ");
		
		// Add the search params
		if (req.hasParameter(SEARCH)) {
			sql.append("and (lower(product_nm) like ? or lower(cust_product_id) like ? ");
			sql.append("or lower(c.gtin_number_txt || cast(p.gtin_product_id as varchar(64))) like ? ) ");
		}
		
		// Set the order
		if (! count) {
			sql.append("order by ");
			sql.append(SearchFields.valueOf(req.getParameter("sort", "productName")).getColumnName()).append(" ");
			sql.append(StringUtil.checkVal(req.getParameter("order"), "asc"));
			sql.append(" offset ? limit ? ");
		}

		log.debug(sql);
	}


	/**
	 * Build the requested report based off of the request servlet and the 
	 * shopping cart
	 * @param cart
	 * @param cvo
	 * @param req 
	 * @throws ActionException 
	 */
	private void buildReport(RAMCaseVO cvo, ActionRequest req) {
		//TODO clean this up so that the product case report takes a case object and not a map
		//     make a second method named build report that returns the abstract report rather then sets it on the 
		//     req object so it can be used in send emails.
		AbstractSBReportVO report;
		String filename;
		String caseId = StringUtil.checkVal(cvo.getCaseId());
		if (caseId.length() != 0) {
			filename = "case-" + cvo.getHospitalCaseId();
		} else {
			filename = "RAM-" + new SimpleDateFormat("YYYYMMdd").format(Convert.getCurrentTimestamp());
		}
		report = new ProductCartReport();
		report.setAttributes(attributes);
		report.setFileName(filename + ".pdf");

		Map<String, Object> data = new HashMap<>();

		data.put(CART, cvo.getItems().get(StringUtil.checkVal(req.getParameter("caseType"), RAMCaseType.OR.toString())).values());
		data.put(HOSPITAL,StringUtil.checkVal(cvo.getCustomerName()));
		data.put(ROOM, StringUtil.checkVal(cvo.getOrRoomName()));
		data.put(SURGEON, StringUtil.checkVal(cvo.getSurgeonName()));
		data.put(TIME, StringUtil.checkVal(cvo.getSurgeryDate()));
		data.put(CASE_ID, StringUtil.checkVal(cvo.getHospitalCaseId()));
		data.put(NOTES, StringUtil.checkVal(cvo.getCaseNotes()));
		data.put(HOSPITAL_REP, cvo.getHospitalRep());
		
		data.put(SURG_DATE, Convert.formatDate(cvo.getSurgeryDate(), Convert.DATE_TIME_SLASH_PATTERN_12HR)  );
		data.put(SALES_REP, cvo.getSalesRep());

		data.put(SIGNATURES, cvo.getAllSignatures());
		data.put(BASE_DOMAIN, req.getHostName());
		data.put(FORMAT, req.getParameter(FORMAT));

		report.setData(data);
		req.setAttribute(Constants.BINARY_DOCUMENT, report);
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, true);
	}

	/**
	 * Send emails to the representative and the hospital
	 * @param req
	 */
	private void sendEmails(ActionRequest req) throws ActionException {
		//TODO clean up this method set it up so that send emails does not require the req object and remove unneeded
		//     binary document hooks and set up and generated the report.
		UserDataVO user = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
		String fromEmail = StringUtil.isEmpty(user.getEmailAddress() ) ? "info@ramgrp.com" : user.getEmailAddress();

		try {
			// Get the case
			RAMCaseManager rcm = new RAMCaseManager(attributes, dbConn, req);
			RAMCaseVO cvo = rcm.retrieveCase(StringUtil.checkVal(req.getParameter(CASE_ID)));
			
			// Build the PDF
			buildReport(cvo, req);
			AbstractSBReportVO report = (AbstractSBReportVO) req.getAttribute(Constants.BINARY_DOCUMENT);
			req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, false);

			// Send the email
			EmailMessageVO mail = new EmailMessageVO();
			mail.setSubject("Surgical Case Summary for Surgery ID: " + cvo.getHospitalCaseId());
			
			mail.addRecipients(req.getParameterValues(EMAIL_ARRAY));
		
			mail.setFrom(fromEmail);
			mail.setReplyTo(fromEmail);
			mail.addAttachment(report.getFileName(), report.generateReport());
			mail.setHtmlBody(buildEmailBody(user, cvo));
			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(mail);
		} catch (Exception e) {
			log.debug("error ", e);
			throw new ActionException(e);
		}
	}
	
	/**
	 * builds the email body
	 * @param user
	 * @param cvo
	 * @return
	 */
	private String buildEmailBody(UserDataVO user, RAMCaseVO cvo) {
		StringBuilder s = new StringBuilder(512);
		s.append("<h3>Attached is a Surgical Case Report for your Records</h3>");
		s.append("<p>The attached case report <i><u>(").append(cvo.getHospitalCaseId()).append(")</u></i> ");
		s.append("that was performed at <i><u>").append(cvo.getCustomerName()).append("</u></i> and scheduled for <i><u>");
		s.append(Convert.formatDate(cvo.getSurgeryDate(), Convert.DATE_TIME_SLASH_PATTERN_12HR));
		s.append("</u></i> has been sent to you for your record by <i><u>").append(user.getFullName()).append(" </u></i></p>");
		s.append("<p>If you received this report in error, please contact ").append(user.getFullName());
		s.append(" so we may update our system.  Thank you.</p>");
		s.append("<img src='http://www.ramgrp.com/binary/themes/CUSTOM/RAMGRP/PORTAL/images/ramlogo-small.png' />");
		
		return s.toString();
	}
}
