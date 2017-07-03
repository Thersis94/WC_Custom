package com.ram.action.products;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.ram.action.data.ORKitVO;
import com.ram.action.report.vo.KitExcelReport;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>CaseSearchAction.java<p/>
 * <b>Description: Handles case search functionality for the ram site.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since September 6, 2016
 * <b>Changes: </b>
 * 		June 30, 2017 - Refactored, pulled out of ProductCartAction
 ****************************************************************************/

public class CaseSearchAction extends SimpleActionAdapter {

	// Names for the request parameters related to this action
	public static final String SEARCH = "search";
	public static final String START_DATE = "startDate";
	public static final String END_DATE = "endDate";
	public static final String FINALIZED = "finalized";
	
	private enum SearchFields {
		repId("REP_ID"),
		caseId("CASE_ID"),
		surgeryDate("SURGERY_DT"),
		numProducts("COUNT(k.RAM_CASE_INFO_ID)"),
		finalized("FINALIZED_FLG");
		
		private String cloumnNm;
		
		SearchFields(String columnNm) {
			this.cloumnNm = columnNm;
		}
		
		public String getColumnName (){
			return cloumnNm;
		}
	}

	public CaseSearchAction() {
		super();
	}

	/**
	 * @param avo
	 */
	public CaseSearchAction(ActionInitVO avo) {
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
		// not implemented
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (!req.hasParameter("json"))
			return;
		
		if (req.hasParameter("exportKits")) {
			buildKitSummaryReport(req);
		} else {
			loadKits(req);
		}
	}
	
	/**
	 * Load all kits, potentially filtering them down based on supplied search criteria
	 * @param req
	 * @param finalized
	 */
	protected void loadKits(ActionRequest req) throws ActionException {
		List<ORKitVO> kits = new ArrayList<>();
		String sql = buildKitSearchSQL(req);
		int count = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
			int i =1;
			ps.setString(i++, user.getProfileId());
			if (req.hasParameter(SEARCH)) {
				ps.setString(i++, "%" + req.getParameter(SEARCH).toLowerCase() + "%");
				ps.setString(i++, "%" + req.getParameter(SEARCH).toLowerCase() + "%");
			}
			if (req.hasParameter(START_DATE)) ps.setTimestamp(i++, Convert.getTimestamp(Convert.formatDate(Convert.DATE_TIME_SLASH_PATTERN, req.getParameter(START_DATE)), false));
			if (req.hasParameter(END_DATE)) ps.setTimestamp(i++, Convert.getTimestamp(Convert.formatDate(Convert.DATE_TIME_SLASH_PATTERN, req.getParameter(END_DATE)), false));
			if (req.hasParameter(FINALIZED)) ps.setInt(i++, Convert.formatInteger(req.getParameter("finalized")));
			
			ResultSet rs = ps.executeQuery();
			int page = Convert.formatInteger(req.getParameter("offset"), 0);
			int rpp = Convert.formatInteger(req.getParameter("limit"));
			rpp = rpp == 0 ? 10 : rpp;
			int start = page * rpp;
			int end = rpp * (page + 1);
			boolean loadAll = Convert.formatBoolean(req.getParameter("loadAll"));
			while(rs.next()) {
				count++;
				if ((count <= start || count > end) && !loadAll) continue; 
				kits.add(new ORKitVO(rs));
			}
		} catch (SQLException e) {
			throw new ActionException(e);
		}
		
		super.putModuleData(kits, count, false);
	}
	
	
	/**
	 * Build the kit search sql query
	 * @param req
	 * @return
	 */
	private String buildKitSearchSQL(ActionRequest req) {
		StringBuilder sql = new StringBuilder(300);
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		sql.append("SELECT HOSPITAL_NM, OPERATING_ROOM, SURGERY_DT, SURGEON_NM, REP_ID, OTHER_ID, CASE_ID, RESELLER_NM, k.ram_case_info_id, COUNT(k.ram_case_info_ID) as NUM_PRODUCTS, FINALIZED_FLG ");
		sql.append("FROM ").append(customDb).append("ram_case_info k ");
		sql.append("LEFT JOIN ").append(customDb).append("ram_case_product xr ");
		sql.append("on k.ram_case_info_ID = xr.ram_case_info_ID ");
		sql.append("WHERE k.PROFILE_ID = ? ");
		
		if (req.hasParameter(SEARCH)) {
			sql.append("AND (lower(REP_ID)").append(" like ? ");
			sql.append("OR lower(CASE_ID)").append(" like ?) ");
		}
		if (req.hasParameter(START_DATE)) sql.append("AND k.SURGERY_DT > ? ");
		if (req.hasParameter(END_DATE)) sql.append("AND k.SURGERY_DT < ? ");
		if (req.hasParameter(FINALIZED)) sql.append("AND k.FINALIZED_FLG = ? ");
		
		sql.append("GROUP BY HOSPITAL_NM, OPERATING_ROOM, SURGERY_DT, SURGEON_NM, CASE_ID, RESELLER_NM, k.ram_case_info_ID, FINALIZED_FLG, REP_ID, OTHER_ID ");
		sql.append("ORDER BY ");
		
		if (req.hasParameter("sort")) {
			sql.append(SearchFields.valueOf(req.getParameter("sort")).getColumnName());
			
			String order = StringUtil.checkVal(req.getParameter("order"));
			if ("desc".equalsIgnoreCase(order)) {
				sql.append(" DESC ");
			} else {
				sql.append(" ASC ");
			}
		} else {
			sql.append("FINALIZED_FLG, SURGERY_DT DESC");
		}
		
		return sql.toString();
	}
	
	/**
	 * Build an excel file with the all kits from the
	 * current search and add it to the request
	 * @param req
	 */
	private void buildKitSummaryReport(ActionRequest req) throws ActionException {
		loadKits(req);
		AbstractSBReportVO report = new KitExcelReport();
		report.setData(((ModuleVO)attributes.get(Constants.MODULE_DATA)).getActionData());
		report.setFileName("kit_summary_report.xls");
		req.setAttribute(Constants.BINARY_DOCUMENT, report);
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, true);
	}
}
