package com.depuysynthes.srt.data;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;

import com.depuysynthes.srt.SRTRequestAction;
import com.depuysynthes.srt.vo.SRTRequestAddressVO;
import com.depuysynthes.srt.vo.SRTRequestVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.util.EnumUtil;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.FormDataTransaction;
import com.smt.sitebuilder.data.vo.FormFieldVO;
import com.smt.sitebuilder.data.vo.FormTransactionVO;
import com.smt.sitebuilder.data.vo.GenericQueryVO;

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
public class RequestDataTransactionHandler extends FormDataTransaction {

	public enum RequestField {
		REQUEST_ID(SRTRequestAction.SRT_REQUEST_ID), REQUESTOR_NM("requestorNm"), 
		HOSPITAL_NM("hospitalName"), SURGEON_FIRST_NM("surgeonFirstName"), 
		SURGEON_LAST_NM("surgeonLastName"), QTY("qtyNo"), 
		REASON_FOR_REQUEST("reason"), CHARGE_TO("chargeTo"), 
		DESCRIPTION("description"), ESTIMATE_ROI("estimatedRoi"), 
		TERRITORY_ID("reqTerritoryId"), ADDRESS_1("address"), 
		ADDRESS_2("address2"), CITY("city"), STATE("state"), 
		ZIP("zipCode"), REQUEST_ADDRESS_ID("requestAddressId"), OP_CO_ID("opCoId");

		private String reqParam;
		private RequestField(String reqParam) {
			this.reqParam = reqParam;
		}

		public String getReqParam() {return reqParam;}
	}

	/**
	 * @param conn
	 * @param attributes
	 * @param req
	 */
	protected RequestDataTransactionHandler(SMTDBConnection conn, Map<String, Object> attributes, ActionRequest req) {
		super(conn, attributes, req);
	}

	@Override
	protected FormTransactionVO writeTransaction(FormTransactionVO data) throws DatabaseException {
		log.debug("Saving SRT Request");

		// Set the form fields that should not be saved as attributes, onto the request, with appropriate parameter names.
		// Remove from the form field map so they aren't saved as attributes.
		Iterator<Map.Entry<String, FormFieldVO>> iter = data.getCustomData().entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, FormFieldVO> entry = iter.next();
			RequestField param = EnumUtil.safeValueOf(RequestField.class, entry.getValue().getSlugTxt());
			if (param != null) {
				req.setParameter(param.getReqParam(), entry.getValue().getResponseText());
				iter.remove();
			}
		}

		// Get the residence data
		SRTRequestVO request = new SRTRequestVO(req);
		SRTRequestAddressVO addr = new SRTRequestAddressVO(req);
		
		// Save the residence record
		DBProcessor dbp = new DBProcessor(dbConn);
		try {
			dbp.save(request);
			req.setParameter(SRTRequestAction.SRT_REQUEST_ID, request.getRequestId());
			addr.setRequestId(request.getRequestId());
			
		} catch(Exception e) {
			log.error("Could not save RezDox Residence");
		}

		// Save the Residence attributes
		saveFieldData(data);
		return null;
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.data.AbstractFormTransaction#loadTransactions(com.smt.sitebuilder.data.DataContainer)
	 */
	@Override
	protected DataContainer loadTransactions(DataContainer dc) throws DatabaseException {
		log.debug("Loading SRT Request Form Transaction Data");
		
		GenericQueryVO qry = dc.getQuery();
		loadTransaction(dc, qry);
		
		return dc;
	}


	/**
	 * Populates the Prepared Statement with the required parameters for loading the attributes
	 *
	 * @param ps
	 * @param qry
	 * @throws SQLException
	 */
	@Override
	protected void populateQueryParams(PreparedStatement ps, GenericQueryVO qry) throws SQLException {
		ps.setString(1, qry.getConditionals().get(0).getValues()[0]);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.data.AbstractFormTransaction#flushTransactions(com.smt.sitebuilder.data.DataContainer)
	 */
	@Override
	protected void flushTransactions(DataContainer dc) {
		//Users can't delete a request.
		return;
	}
}
