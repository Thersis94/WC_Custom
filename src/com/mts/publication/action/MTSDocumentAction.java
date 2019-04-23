package com.mts.publication.action;

// JDK 1.8.x
import java.util.Map;

import com.mts.publication.data.MTSDocumentVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.pool.SMTDBConnection;

// WC Libs
import com.smt.sitebuilder.action.content.DocumentAction;

/****************************************************************************
 * <b>Title</b>: MTSDocumentAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Extends the Document Management Widget to manage the 
 * extra data tracked by MTS
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 23, 2019
 * @updates:
 ****************************************************************************/

public class MTSDocumentAction extends DocumentAction {

	/**
	 * Ajax Controller key for this action
	 */
	public static final String AJAX_KEY = "article";
	/**
	 * 
	 */
	public MTSDocumentAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public MTSDocumentAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * @param dbConn
	 * @param attributes
	 */
	public MTSDocumentAction(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super(dbConn, attributes);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.info("saving document");
		
		MTSDocumentVO doc = new MTSDocumentVO(req);
		setModuleData(doc);
	}
}

