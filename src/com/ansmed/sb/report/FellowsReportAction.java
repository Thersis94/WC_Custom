 package com.ansmed.sb.report;

// JDK 1.5.0
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// SMT Base Libs
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// SB Libs
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

// SB ANS Libs
import com.ansmed.sb.security.ANSRoleFilter;
import com.ansmed.sb.physician.FellowsSurgeonVO;
import com.ansmed.sb.physician.FellowsVO;

/****************************************************************************
 * <b>Title</b>: FellowsReportAction.java<p/>
 * <b>Description: </b> Returns fellows report data for surgeon.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since May 05, 2009
 ****************************************************************************/
public class FellowsReportAction extends SBActionAdapter {
	
	/**
	 * 
	 */
	public FellowsReportAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public FellowsReportAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	/**
	 * 
	 */
	public void retrieve(ActionRequest req) throws ActionException {
		
		String report = StringUtil.checkVal(req.getParameter("report"));
		String surgeonId = StringUtil.checkVal(req.getParameter("surgeonId"));
		log.debug("report: " + report);
		log.debug("surgeon_id: " + surgeonId);
		if (report.equalsIgnoreCase("detail")) {
			this.createDetailReport(req);
		} else {
			this.createSummaryReport(req);
		}
	}
	
	public void createDetailReport(ActionRequest req) {
		StringBuffer sql = new StringBuffer();
		String schema = (String)getAttribute("customDbSchema");
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		ANSRoleFilter filter = new ANSRoleFilter();
		
		sql.append("select d.first_nm as 'rep_first_nm', d.last_nm as 'rep_last_nm', ");
		sql.append("a.first_nm as 'phys_first_nm', a.last_nm as 'phys_last_nm', ");
		sql.append("a.surgeon_id, c.program_goal_txt, c.program_action_txt, ");
		sql.append("c.program_needs_txt, c.program_month_no, c.program_yr_no, e.fellows_nm ");  
		sql.append("from ").append(schema).append("ans_sales_region y inner join ");
		sql.append(schema).append("ans_sales_rep d on y.region_id = d.region_id ");
		sql.append("inner join ").append(schema).append("ans_surgeon a on ");
		sql.append("d.sales_rep_id = a.sales_rep_id	inner join ").append(schema);
		sql.append("ans_fellows b on a.surgeon_id = b.surgeon_id inner join ");
		sql.append(schema).append("ans_fellows_surgeon e on b.fellows_id = e.fellows_id ");
		sql.append("left join ").append(schema).append("ans_fellows_goal c on ");
		sql.append("b.fellows_id = c.fellows_id ");
		sql.append("where 1 = 1 ");
		
		/* If single surgeon */
		String surgeonId = StringUtil.checkVal(req.getParameter("surgeonId"));
		if (surgeonId.length() > 0) {
			sql.append("and a.surgeon_id = ? ");
		}
		
		/* Add the role filter */
		Boolean edit = false;
		if (mod.getDisplayPage().indexOf("facade") > 1) {
			edit = Boolean.TRUE;
		}
		sql.append(filter.getSearchFilter(role, "d", edit));
		
		Integer orderBy = Convert.formatInteger(req.getParameter("orderBy"));
		switch(orderBy) {
			default:
				sql.append("order by rep_last_nm, rep_first_nm, phys_last_nm, ");
				sql.append("phys_first_nm, c.create_dt ");
				break;
		}
		
		log.debug("Fellows Detail Report SQL: " + sql.toString() + "|" + surgeonId);
		
		List<FellowsVO> fvo = new ArrayList<FellowsVO>();
		
		PreparedStatement ps = null;
		int counter = 0;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			if (surgeonId.length() > 0) {
				ps.setString(++counter, surgeonId);
			}
			ResultSet rs = ps.executeQuery();
			FellowsVO vo = null;
			while (rs.next()) {
				vo = new FellowsVO(rs);
				vo.addFellowsSurgeon(new FellowsSurgeonVO(rs));
				fvo.add(vo);
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving Fellows Detail report data.",sqle);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		log.debug("FellowsVO list size: " + fvo.size());
			
		mod.setActionData(fvo);
		AbstractSBReportVO rpt = new FellowsDetailReportVO();
		rpt.setData(mod.getActionData());
		rpt.setFileName("FellowsIndividualDetailReport." + req.getParameter("reportType", "html"));
		log.debug("Mime Type: " + rpt.getContentType());
		
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
		req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
	}
	
	public void createSummaryReport(ActionRequest req) {
		StringBuffer sql = new StringBuffer();
		String schema = (String)getAttribute("customDbSchema");
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		ANSRoleFilter filter = new ANSRoleFilter();
		
		sql.append("select d.first_nm as 'rep_first_nm', d.last_nm as 'rep_last_nm', ");
		sql.append("a.first_nm as 'phys_first_nm', a.last_nm as 'phys_last_nm', ");
		sql.append("a.surgeon_id, b.program_nm, c.fellows_nm, c.fellows_email_addr_txt, ");
		sql.append("c.fellows_phone_no, c.fellows_end_month_no, c.fellows_end_yr_no, ");
		sql.append("c.fellows_plan_txt, e.specialty_nm "); 
		sql.append("from ").append(schema).append("ans_sales_region y inner join ");
		sql.append(schema).append("ans_sales_rep d on y.region_id = d.region_id ");
		sql.append("inner join ").append(schema).append("ans_surgeon a on ");
		sql.append("d.sales_rep_id = a.sales_rep_id	inner join ").append(schema);
		sql.append("ans_fellows b on a.surgeon_id = b.surgeon_id inner join ");
		sql.append(schema).append("ans_fellows_surgeon c on b.fellows_id = c.fellows_id ");
		sql.append("inner join ").append(schema).append("ans_specialty e on ");
		sql.append("c.fellows_specialty_id = e.specialty_id "); 
		sql.append("where 1 = 1 ");
		
		/* If single surgeon */
		String surgeonId = StringUtil.checkVal(req.getParameter("surgeonId"));
		if (surgeonId.length() > 0) {
			sql.append("and a.surgeon_id = ? ");
		}
		
		/* Add the role filter */
		Boolean edit = false;
		if (mod.getDisplayPage().indexOf("facade") > 1) {
			edit = Boolean.TRUE;
		}
		sql.append(filter.getSearchFilter(role, "d", edit));
		
		int orderBy = Convert.formatInteger(req.getParameter("orderBy")).intValue();
		log.debug("orderBy value: " + orderBy);
		switch(orderBy) {
			case 1:
				sql.append("order by b.coord_nm ");
				break;
			case 2:
				sql.append("order by b.program_nm ");
				break;
			case 3:
				sql.append("order by e.specialty_nm ");
				break;
			case 4:
				sql.append("order by c.fellows_end_month_no, c.fellows_end_yr_no ");
				break;
			case 5:
				sql.append("order by c.fellows_nm ");
				break;
			default:
				sql.append("order by phys_last_nm, phys_first_nm ");
				break;
		}
		
		log.debug("Fellows Summery Report SQL: " + sql.toString() + "|" + surgeonId);
		
		List<FellowsSummaryVO> fsvo = new ArrayList<FellowsSummaryVO>();
		
		PreparedStatement ps = null;
		int counter = 0;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			if (surgeonId.length() > 0) {
				ps.setString(++counter, surgeonId);
			}
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				fsvo.add(new FellowsSummaryVO(rs));
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving Fellows Detail report data.",sqle);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		log.debug("FellowsSummaryVO list size: " + fsvo.size());
			
		mod.setActionData(fsvo);
		AbstractSBReportVO rpt = new FellowsSummaryReportVO();
		rpt.setData(mod.getActionData());
		rpt.setFileName("FellowsSummaryReport." + req.getParameter("reportType", "html"));
		log.debug("Mime Type: " + rpt.getContentType());
		
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
		req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
	}
}
