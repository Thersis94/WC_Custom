package com.depuysynthes.srt.data;

import java.util.Map;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.smt.sitebuilder.data.FormDataTransaction;

/****************************************************************************
 * <b>Title:</b> RequestDataTransactionHandler.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Data Transaction Handler for SRT Master Records. 
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Feb 28, 2018
 ****************************************************************************/
public class MasterRecordDataTransactionHandler extends FormDataTransaction {

	public enum MasterRecordField {
		PART_NO("partNo"), PROD_DESC("productDesc"), QUALITY_SYS("qualitySystem"), PROD_TYPE("productType"),
		COMPLEXITY("complexity"), LABEL_STATUS("labelStatus"), CATEGORY("productCategory"), MAKE_FROM_PART_NO("makeFromPartNos"),
		FUNCTIONAL_CHECK_PART_NO("functionalCheckPartNos"), PRODUCT_FAMILY("productFamily");
		private String reqParam;
		private MasterRecordField(String reqParam) {
			this.reqParam = reqParam;
		}

		public String getReqParam() {return reqParam;}
	}
	/**
	 * @param conn
	 * @param attributes
	 * @param req
	 */
	protected MasterRecordDataTransactionHandler(SMTDBConnection conn, Map<String, Object> attributes, ActionRequest req) {
		super(conn, attributes, req);
	}

}
