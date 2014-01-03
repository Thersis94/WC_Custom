package com.ansmed.sb.report;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.ansmed.sb.security.ANSRoleFilter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title</b>: TerritorySummaryReportAction.java<p/>
 * <b>Description: </b> Summary report of physicians by territory
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since May 07, 2009
 ****************************************************************************/
public class TerritorySummaryReportAction extends SBActionAdapter {

	/**
	 * 
	 */
	public TerritorySummaryReportAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public TerritorySummaryReportAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		String schema = (String)getAttribute("customDbSchema");
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		ANSRoleFilter filter = new ANSRoleFilter();
		StringBuffer sql = new StringBuffer();
		
		sql.append("select a.first_nm as phys_first_nm, a.last_nm as phys_last_nm, ");
		sql.append("a.title_nm,	a.email_address_txt, b.clinic_nm, b.address_txt, b.address2_txt, ");
		sql.append("b.address3_txt,	b.city_nm, b.state_cd, b.zip_cd, ");
		sql.append("h.phone_number_txt as phone_no, ");
		sql.append("g.first_nm as rep_first_nm, g.last_nm as rep_last_nm, e.region_nm ");
		sql.append("from ").append(schema).append("ans_surgeon a inner join ");
		sql.append(schema).append("ans_clinic b on a.surgeon_id = b.surgeon_id ");
		sql.append("left outer join ").append(schema).append("ans_phone h on ");
		sql.append("b.clinic_id = h.clinic_id inner join ").append(schema);
		sql.append("ans_sales_rep d on a.sales_rep_id = d.sales_rep_id ");
		sql.append("inner join ").append(schema).append("ans_sales_region e ");
		sql.append("on d.region_id = e.region_id inner join ").append(schema);
		sql.append("ans_surgeon_type f on a.surgeon_type_id = f.surgeon_type_id ");
		sql.append("inner join ").append(schema).append("ans_sales_rep g on ");
		sql.append("a.sales_rep_id = g.sales_rep_id	where 1 = 1	");
		sql.append("and location_type_id = 1 and phone_type_id = 'WORK_PHONE' ");
		
		// Add the role filter
		Boolean edit = false;
		if (mod.getDisplayPage().indexOf("facade") > 1) {
			edit = Boolean.TRUE;
		}
			
		sql.append(filter.getSearchFilter(role, "d", edit));
		
		sql.append("order by rep_last_nm, rep_first_nm, phys_last_nm, phys_first_nm, ");
		sql.append("b.location_type_id, a.surgeon_id, b.clinic_id "); 
		
		log.info("Territory Summary Report SQL: " + sql);
		
		PreparedStatement ps = null;
		List<TerritorySummaryVO> tsvo = new ArrayList<TerritorySummaryVO>();
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				tsvo.add(new TerritorySummaryVO(rs));
			}
		} catch(Exception e) {
			throw new ActionException("Unable to retrieve territory summary report data.", e);
		}
		
		mod.setActionData(tsvo);
		AbstractSBReportVO rpt = new TerritorySummaryReportVO();
		rpt.setData(mod.getActionData());
		rpt.setFileName("GroupSummaryReport." + req.getParameter("reportType", "xls"));
		log.debug("Mime Type: " + rpt.getContentType());
		
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
		req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
	}

}
