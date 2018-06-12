package com.depuysynthes.srt.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.depuysynthes.srt.SRTRequestAction;
import com.depuysynthes.srt.util.SRTUtil;
import com.depuysynthes.srt.vo.SRTFileVO;
import com.depuysynthes.srt.vo.SRTProjectVO;
import com.depuysynthes.srt.vo.SRTRequestAddressVO;
import com.depuysynthes.srt.vo.SRTRequestVO;
import com.depuysynthes.srt.vo.SRTRosterVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.FileWriterException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.EnumUtil;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.smt.sitebuilder.action.file.transfer.ProfileDocumentAction;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.AbstractDataProcessor;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.vo.FormFieldVO;
import com.smt.sitebuilder.data.vo.FormTransactionVO;

/****************************************************************************
 * <b>Title:</b> RequestDataTransactionHandler.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Data Transaction Handler for SRT Requests. 
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Feb 28, 2018
 ****************************************************************************/
public class RequestDataProcessor extends AbstractDataProcessor {

	public enum RequestField {
		REQUEST_ID(SRTRequestAction.SRT_REQUEST_ID), REQUESTOR_NM("requestorNm"),
		HOSPITAL_NM("hospitalName"), SURGEON_FIRST_NM("surgeonFirstName"),
		SURGEON_LAST_NM("surgeonLastName"), QTY("qtyNo"),
		REASON_FOR_REQUEST("reason"), CHARGE_TO("chargeTo"),
		DESCRIPTION("description"), ESTIMATE_ROI("estimatedRoi"),
		TERRITORY_ID("reqTerritoryId"), ADDRESS_1("address"),
		ADDRESS_2("address2"), CITY("city"), STATE("state"),
		ZIP("zipCode"), REQUEST_ADDRESS_ID("requestAddressId"), OP_CO_ID("opCoId"),
		ATTACHMENT_1("attachment1"), ATTACHMENT_2("attachment2"),
		ATTACHMENT_3("attachment3");

		private String reqParam;
		private RequestField(String reqParam) {
			this.reqParam = reqParam;
		}

		public String getReqParam() {return reqParam;}
	}

	private List<SRTFileVO> files;

	/**
	 * @param conn
	 * @param attributes
	 * @param req
	 */
	public RequestDataProcessor(SMTDBConnection conn, Map<String, Object> attributes, ActionRequest req) {
		super(conn, attributes, req);
		files = new ArrayList<>();
	}

	/**
	 * Process files on the form and Ensure we've written records to ProfileDocuments
	 * and SRT File.
	 * @param request
	 */
	private void writeSRTFileRecord() {
		SRTFileVO file = new SRTFileVO(req);
		file.setFileId(req.getParameter("profileDocumentId"));

		//Save SRTFile Record now that it's successfully saved to DocumentAction.
		files.add(file);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.data.AbstractFormTransaction#flushTransactions(com.smt.sitebuilder.data.DataContainer)
	 */
	@Override
	public void flushTransactions(DataContainer dc) {
		//Users can't delete a request.
		return;
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.data.AbstractDataTransaction#saveFormData(com.smt.sitebuilder.data.vo.FormTransactionVO)
	 */
	@Override
	protected void saveFormData(FormTransactionVO data) throws DatabaseException {
		log.debug("Saving SRT Request");

		translateToRequest(data);

		log.debug("Creating Request.");

		// Build the SRT Request and Address from the request.
		SRTRequestVO request = new SRTRequestVO(req);
		SRTRequestAddressVO address = new SRTRequestAddressVO(req);
		SRTProjectVO project = null;

		/*
		 * Store if this is an insert or not so we can decide if we create
		 * a Project Record laster.
		 */
		if(StringUtil.isEmpty(request.getRequestId())) {
			project = buildProjectRecord(SRTUtil.getRoster(req));
		}

		//Save the Request and Address info and optional generate a Project
		saveRequestData(request, address, project);

		data.setFormSubmittalId(request.getRequestId());
	}

	/**
	 * Helper method manages Translating Form Data for the RequestVO
	 * onto the ActionRequest
	 * @param data
	 */
	private void translateToRequest(FormTransactionVO data) {
		// Set the form fields that should not be saved as attributes, onto the request, with appropriate parameter names.
		Iterator<Map.Entry<String, FormFieldVO>> iter = data.getCustomData().entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, FormFieldVO> entry = iter.next();
			RequestField param = EnumUtil.safeValueOf(RequestField.class, entry.getValue().getSlugTxt());
			if (param != null) {
				req.setParameter(param.getReqParam(), StringUtil.checkVal(entry.getValue().getResponseText()).trim());
				log.debug(StringUtil.join(param.getReqParam(), " -- ", StringUtil.checkVal(entry.getValue().getResponseText()).trim()));
				iter.remove();
			}
		}
	}

	/**
	 * Manages saving the given Request, Address and Project Records.
	 * @param request - The SRTRequestVO to be saved.
	 * @param address - The SRTRequestAddressVO to be saved.
	 * @param project - The SRTProjectVO to be saved.
	 */
	public void saveRequestData(SRTRequestVO request, SRTRequestAddressVO address, SRTProjectVO project) {
		DBProcessor dbp = new DBProcessor(dbConn, (String)attributes.get(Constants.CUSTOM_DB_SCHEMA));

		try {

			//Save the SRTRequestVO
			dbp.save(request);
			req.setParameter(SRTRequestAction.SRT_REQUEST_ID, request.getRequestId());

			//Update the Address
			address.setRequestId(req.getParameter(SRTRequestAction.SRT_REQUEST_ID));

			//Save the AddressVO
			dbp.save(address);

			//Save the Request Address.
			request.setRequestAddress(address);

			//Generate Project Record if necessary.
			if(project != null) {
				project.setRequestId(request.getRequestId());
				new ProjectDataProcessor(dbConn, attributes, req).saveProjectRecord(project);
			}

		} catch(Exception e) {
			log.error("Could not save SRT Request", e);
		}
	}


	/**
	 * Build the Project Record for this request on inserts only.
	 * @param request
	 */
	public SRTProjectVO buildProjectRecord(SRTRosterVO roster) {
		SRTProjectVO p = new SRTProjectVO(req);
		p.setSrtContact(roster.getEngineeringContact());
		p.setProjectName(StringUtil.checkVal(req.getParameter(RequestField.DESCRIPTION.getReqParam())));
		if(p.getProjectName().length() > 100) {
			p.setProjectName(p.getProjectName().substring(0, 100) + "...");
		}
		p.setProjectType("NEW");
		p.setProjectStatus("UNASSIGNED");
		p.setCreateDt(Convert.getCurrentTimestamp());

		return p;
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.data.AbstractDataTransaction#saveFieldData(com.smt.sitebuilder.data.vo.FormTransactionVO)
	 */
	@Override
	protected void saveFieldData(FormTransactionVO data) throws DatabaseException {
		//Not Implemented
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.data.AbstractDataTransaction#loadTransactions(com.smt.sitebuilder.data.DataContainer)
	 */
	@Override
	protected DataContainer loadTransactions(DataContainer dc) throws DatabaseException {
		//Not Implemented
		return null;
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.data.AbstractDataTransaction#saveFile(com.smt.sitebuilder.action.file.transfer.ProfileDocumentAction, com.siliconmtn.util.databean.FilePartDataBean)
	 */
	@Override
	protected void saveFile(ProfileDocumentAction pda, FilePartDataBean fpdb) throws ActionException, FileWriterException, InvalidDataException {
		super.saveFile(pda, fpdb);
		writeSRTFileRecord();
	}

	@Override
	protected void saveFiles(FormTransactionVO data) {
		super.saveFiles(data);

		//Write SRT Files after processing all files.
		if(!files.isEmpty()) {
			try {
				new DBProcessor(dbConn, (String)attributes.get(Constants.CUSTOM_DB_SCHEMA)).executeBatch(files, true);
			} catch (com.siliconmtn.db.util.DatabaseException e) {
				log.error("Error Processing Code", e);
			}
		}
	}

	/**
	 * Used to update Request Fields on the Project Data Form.
	 * @param data
	 * @throws com.siliconmtn.db.util.DatabaseException
	 */
	public void updateRequestData(FormTransactionVO data) throws com.siliconmtn.db.util.DatabaseException {
		translateToRequest(data);

		String schema = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);

		//Prep Request Record Update Fields.
		List<String> fields = new ArrayList<>();
		fields.add("charge_to");
		fields.add("hospital_nm");
		fields.add("surgeon_first_nm");
		fields.add("surgeon_last_nm");
		fields.add("reason_for_request");
		fields.add("request_desc");
		fields.add("estimated_roi");
		fields.add("qty_no");

		//Build Request Update Query
		StringBuilder sql = new StringBuilder(300);
		int cnt = 0;

		sql.append(DBUtil.UPDATE_CLAUSE).append(schema).append("DPY_SYN_SRT_REQUEST set ");
		for (String field : fields) {
			sql.append(cnt++ > 0 ? ", " : "").append(field).append(" = ? ");
		}
		sql.append(DBUtil.WHERE_CLAUSE).append("request_id = ?");
		fields.add("request_id");

		//Save request
		DBProcessor dbp = new DBProcessor(dbConn, schema);
		dbp.executeSqlUpdate(sql.toString(), new SRTRequestVO(req), fields);

		//Prep Request Address Record Update Fields
		fields = new ArrayList<>();
		fields.add("address_txt");
		fields.add("address_2_txt");
		fields.add("city_nm");
		fields.add("state_cd");
		fields.add("zip_cd");

		//Build Request Address Update Query.
		sql = new StringBuilder(300);
		cnt = 0;

		sql.append(DBUtil.UPDATE_CLAUSE).append(schema).append("DPY_SYN_SRT_REQUEST_ADDRESS set ");
		for (String field : fields) {
			sql.append(cnt++ > 0 ? ", " : "").append(field).append(" = ? ");
		}
		sql.append(DBUtil.WHERE_CLAUSE).append("request_id = ?");
		fields.add("request_id");

		//Save Request Address.
		dbp.executeSqlUpdate(sql.toString(), new SRTRequestAddressVO(req), fields);
	}
}