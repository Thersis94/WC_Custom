package com.wsla.action.admin;

// JDK 1.8.x
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

// WSLA Libs
import com.wsla.data.ticket.DiagnosticVO;

/****************************************************************************
 * <b>Title</b>: DiagnosticAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> WC Action to manage the diagnostic data
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Oct 2, 2018
 * @updates:
 ****************************************************************************/

public class DiagnosticAction extends SBActionAdapter {
	public static final String DIAGNOSTIC_TYPE = "diagnostics";

	/**
	 * 
	 */
	public DiagnosticAction() {
		super();
	}
	
	/**
	 * Helper constructor
	 * @param attributes
	 * @param dbConn
	 */
	public DiagnosticAction(Map<String, Object> attributes, SMTDBConnection dbConn) {
		super();
		this.attributes = attributes;
		this.dbConn = dbConn;
	}

	/**
	 * @param actionInit
	 */
	public DiagnosticAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		BSTableControlVO bst = null;
		
		if(req.hasParameter("DiagnosticTable")) {
			bst = new BSTableControlVO(req, DiagnosticVO.class);
		}

		int svcCtr = req.getIntegerParameter("serviceCenterFlag", 0);
		int casFlag = req.getIntegerParameter("casFlag", 0);
		putModuleData(getDiagnostics(svcCtr, casFlag, bst));
	}
	
	/**
	 * Gets a list of diagnostics
	 * @param svcCtr
	 * @param cas
	 * @param bst 
	 * @return
	 */
	public List<DiagnosticVO> getDiagnostics(int svcCtr, int cas, BSTableControlVO bst) {
		
		StringBuilder sql = new StringBuilder(128);
		sql.append("select * from ").append(getCustomSchema()).append("wsla_diagnostic ");
		sql.append("where 1 = 1");
		
		if (svcCtr == 1) sql.append("and svc_ctr_flg = 1 ");
		if (cas == 1) sql.append("and cas_flg = 1 ");
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(sql.toString(), null, new DiagnosticVO());
	}

}

