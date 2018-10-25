package com.wsla.action.admin;

import java.util.ArrayList;
// JDK 1.8.x
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;
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
		log.debug("diagnostics action retrieve called ");
		BSTableControlVO bst = new BSTableControlVO(req, DiagnosticVO.class);
		int svcCtr = req.getIntegerParameter("serviceCenterFlag", 0);
		int casFlag = req.getIntegerParameter("casFlag", 0);
		String categoryCode = req.getParameter("categoryCode");
		boolean hasCasFlag = false;
		boolean hasCallCenterFlag = false;
		if (req.hasParameter("serviceCenterFlag")) {
			hasCallCenterFlag = true;
		}
		
		if ( req.hasParameter("casFlag")){
			hasCasFlag = true;
		}
		
		
		if(!req.hasParameter("diagnosticTable")) {
			bst.setLimit(1000);
			putModuleData(getDiagnosticsList(svcCtr, casFlag,hasCasFlag, hasCallCenterFlag, categoryCode, bst));
			return;
		}

		setModuleData(getDiagnostics(svcCtr, casFlag,hasCasFlag, hasCallCenterFlag, categoryCode, bst));
	}
	

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("diagnostic action build called");

		DiagnosticVO dvo = new DiagnosticVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());

		try {
			if(StringUtil.isEmpty(req.getParameter("origDiagnosticCode"))) {
				db.insert(dvo);
			}else {
				db.save(dvo);
			}
			
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Unable to save diagnostic", e);
			 putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}
	
	/**
	 * removes the bootstrap table layer for ticket generation
	 * @param svcCtr
	 * @param casFlag
	 * @param hasCallCenterFlag 
	 * @param hasCasFlag 
	 * @param categoryCode
	 * @param bst
	 * @return
	 */
	private List<DiagnosticVO> getDiagnosticsList(int svcCtr, int casFlag, boolean hasCasFlag, boolean hasCallCenterFlag, String categoryCode, BSTableControlVO bst) {
		GridDataVO<DiagnosticVO> data = getDiagnostics(svcCtr, casFlag,hasCasFlag, hasCallCenterFlag, categoryCode, bst );
		
		return data.getRowData();
	}

	/**
	 * Gets a list of diagnostics
	 * @param svcCtr
	 * @param cas
	 * @param hasCallCenterFlag 
	 * @param hasCasFlag 
	 * @param categoryCode 
	 * @param bst 
	 * @return
	 */
	public  GridDataVO<DiagnosticVO> getDiagnostics(int svcCtr, int cas, boolean hasCasFlag, boolean hasCallCenterFlag, String categoryCode, BSTableControlVO bst) {
		
		StringBuilder sql = new StringBuilder(128);
		sql.append("select b.category_cd, a.* from ").append(getCustomSchema()).append("wsla_diagnostic a ");
		sql.append("left outer join ").append(getCustomSchema()).append("wsla_product_category b on a.product_category_id = b.product_category_id ");
		sql.append("where 1 = 1 ");
		List<Object> params = new ArrayList<>();

		if (hasCallCenterFlag) {
			sql.append("and svc_ctr_flg = ? ");
			params.add(svcCtr);
		}
			
		if(hasCasFlag) {
			sql.append("and cas_flg = ? ");
			params.add(cas);
		}
	
		// Filter by Group code
		if (! StringUtil.isEmpty(categoryCode)) {
			sql.append("and a.product_category_id = ? ");
			params.add(categoryCode);
		}
		
		// Filter by search criteria
		if (bst.hasSearch()) {
			sql.append("and lower(diagnostic_cd) like ? or lower(desc_txt) like ? ");
			params.add(bst.getLikeSearch().toLowerCase());
			params.add(bst.getLikeSearch().toLowerCase());
		}
		
		sql.append(bst.getSQLOrderBy("diagnostic_cd",  "asc"));
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		GridDataVO<DiagnosticVO> data = db.executeSQLWithCount(sql.toString(), params, new DiagnosticVO(), bst.getLimit(), bst.getOffset());
		if (data == null)log.debug("##### data null ");
				
		return data;
	}

}

