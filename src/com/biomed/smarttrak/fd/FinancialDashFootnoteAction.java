package com.biomed.smarttrak.fd;

import java.util.ArrayList;
import java.util.List;

import com.biomed.smarttrak.fd.FinancialDashVO.TableType;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: FinancialDashFootnoteAction.java<p/>
 * <b>Description: Manages footnotes on the financial dashboard.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Mar 22, 2017
 ****************************************************************************/

public class FinancialDashFootnoteAction extends SBActionAdapter {

	public FinancialDashFootnoteAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public FinancialDashFootnoteAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		FinancialDashFootnoteVO fvo = new FinancialDashFootnoteVO(req);
		TableType tableType = TableType.valueOf(req.getParameter("tableType"));
		
		putModuleData(retrieveFootnotes(fvo, tableType));
	}
	
	/**
	 * Retrieves footnotes for a given set of dashboard selections
	 * 
	 * @param fvo
	 * @param tableType
	 * @return
	 */
	protected List<Object> retrieveFootnotes(FinancialDashFootnoteVO fvo, TableType tableType) {
		String custom = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		
		String sql = getFootnoteSql(fvo, tableType, custom);
		List<Object> params = new ArrayList<>();
		params.add(fvo.getRegionCd());
		
		if (!StringUtil.isEmpty(fvo.getCompanyId())) {
			params.add(fvo.getCompanyId()); // Company View
		} else {
			params.add(fvo.getSectionId()); // List Views
		}
		
		DBProcessor dbp = new DBProcessor(dbConn, custom);
		return dbp.executeSelect(sql, params, new FinancialDashFootnoteVO()); 
	}
	
	/**
	 * Generates the sql required for retrieving footnotes for the current dashboard selections
	 * 
	 * @param fvo
	 * @param tableType
	 * @param custom
	 * @return
	 */
	protected String getFootnoteSql(FinancialDashFootnoteVO fvo, TableType tableType, String custom) {
		StringBuilder sql = new StringBuilder(300);
		sql.append("select footnote_id, region_cd, footnote_txt, expiration_dt, section_id, company_id ");
		sql.append("from ").append(custom).append("biomedgps_fd_revenue_footnote rf ");
		sql.append("where region_cd = ? and (expiration_dt > getdate() or expiration_dt is null) ");
		
		// Company View - select all for given company_id
		if (!StringUtil.isEmpty(fvo.getCompanyId())) {
			sql.append("and company_id = ? ");

		// Company List View - select all for given section_id w/company_id not null
		} else if (TableType.COMPANY == tableType) {
			sql.append("and section_id = ? and company_id is not null ");
		
		// Market List View - select all children of given section_id w/null company_id
		} else {
			sql.append("and company_id is null and section_id in ");
			sql.append("(select section_id from ").append(custom).append("biomedgps_section s ");
			sql.append("where parent_id = ?) ");
		}
		
		log.debug("Financial dashboard footnote sql: " + sql.toString());
		
		return sql.toString();
	}
}
