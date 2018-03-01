package com.depuysynthes.srt.data;

import java.util.Map;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.smt.sitebuilder.data.FormDataTransaction;

/****************************************************************************
 * <b>Title:</b> RequestDataTransactionHandler.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Data Transaction Handler for SRT Projects. 
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Feb 28, 2018
 ****************************************************************************/
public class ProjectDataTransactionHandler extends FormDataTransaction {

	/**
	 * @param conn
	 * @param attributes
	 * @param req
	 */
	protected ProjectDataTransactionHandler(SMTDBConnection conn, Map<String, Object> attributes, ActionRequest req) {
		super(conn, attributes, req);
	}

}
