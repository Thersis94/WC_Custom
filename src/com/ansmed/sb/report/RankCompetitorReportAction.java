package com.ansmed.sb.report;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.ansmed.sb.security.ANSRoleFilter;
import com.ansmed.sb.util.calendar.InvalidCalendarException;
import com.ansmed.sb.util.calendar.SJMBusinessCalendar;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title</b>: RankCompetitorReportAction.java<p/>
 * <b>Description: </b> Summary report of physicians by territory
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since May 07, 2009
 ****************************************************************************/
public class RankCompetitorReportAction extends SBActionAdapter {

	/**
	 * 
	 */
	public RankCompetitorReportAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public RankCompetitorReportAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String schema = (String)getAttribute("customDbSchema");
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		ANSRoleFilter filter = new ANSRoleFilter();
		StringBuffer sql = new StringBuffer();
		
		//retrieve the current SJM business calendar
		SJMBusinessCalendar bc = null;
		try {
			bc = new SJMBusinessCalendar();
		} catch (InvalidCalendarException ice) {
			log.error("Error retrieving SJM business calendar.", ice);
			throw new ActionException("Error retrieving business calendar.",ice);
		}
		
		int currQtr = bc.getCurrentQuarter();
		int currYr = bc.getCurrentYear();
		
		sql.append("select d.first_nm as 'rep_first_nm', d.last_nm as 'rep_last_nm', ");
		sql.append("a.first_nm as 'phys_first_nm', a.last_nm as 'phys_last_nm', ");
		sql.append("a.surgeon_id, a.title_nm, b.business_plan_id, b.value_txt, ");
		sql.append("b.bp_quarter_no, b.bp_year_no ");
		sql.append("from ").append(schema).append("ans_sales_region y inner join ");
		sql.append(schema).append("ans_sales_rep d on y.region_id = d.region_id ");
		sql.append("inner join ").append(schema).append("ans_surgeon a on ");
		sql.append("d.sales_rep_id = a.sales_rep_id	left outer join ");
		sql.append(schema).append("ans_xr_surgeon_busplan b on a.surgeon_id = b.surgeon_id ");
		sql.append("where 1 = 1 and (b.business_plan_id in ");
		sql.append("('bostonScientific', 'medtronic', 'sjmMedical', 'unknown', 'trialQ1', ");
		sql.append("'trialQ2', 'trialQ3', 'trialQ4', 'permQ1', 'permQ2', 'permQ3', ");
		sql.append("'permQ4') or b.business_plan_id is null) and (b.bp_quarter_no = ? or ");
		sql.append("b.bp_quarter_no is null) and (b.bp_year_no = ? or b.bp_year_no is null) "); 
		
		// Add the role filter
		Boolean edit = false;
		if (mod.getDisplayPage().indexOf("facade") > 1) {
			edit = Boolean.TRUE;
		}
			
		sql.append(filter.getSearchFilter(role, "d", edit));
		
		sql.append("order by rep_last_nm, rep_first_nm, phys_last_nm, phys_first_nm "); 
		
		log.info("Rank Competitor Report SQL: " + sql);
		
		PreparedStatement ps = null;
		List<ImplantVolumeVO> livo = new ArrayList<ImplantVolumeVO>();
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, currQtr);
			ps.setInt(2, currYr);
			ResultSet rs = ps.executeQuery();
			String prevId = "";
			String currId = "";
			ImplantVolumeVO ivvo = null;
			while (rs.next()) {
				currId = StringUtil.checkVal(rs.getString("surgeon_id"));
				//log.debug("prevId | currId: " + prevId + " | " + currId);
				if(currId.equals(prevId)) {
					// surgeon is same, add this implant data to vo.
					ivvo.addImplantData(rs.getString("business_plan_id"), rs.getString("value_txt")); 
				} else {
					if (ivvo != null) {
						// surgeon changed, do volume calc, save prev surgeon vo
						ivvo.setVolume();
						livo.add(ivvo);
					}
					ivvo = new ImplantVolumeVO(rs);
				}
				prevId = currId;
			}
			// Add the final record after doing volume calc.
			if (ivvo != null) {
				ivvo.setVolume();
				livo.add(ivvo);
			}
			
		} catch(Exception e) {
			throw new ActionException("Unable to retrieve rank competitor report data.", e);
		}
		
		mod.setActionData(livo);
		AbstractSBReportVO rpt = new RankCompetitorReportVO();
		rpt.setData(mod.getActionData());
		rpt.setFileName("RankingCompetitiveUse-SummaryReport." + req.getParameter("reportType", "xls"));
		log.debug("Mime Type: " + rpt.getContentType());
		
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
		req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
	}

}
