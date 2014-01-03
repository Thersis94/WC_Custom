 package com.ansmed.sb.report;

// JDK 1.5.0
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// SB Libs
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

// SB ANS Libs
import com.ansmed.sb.comparator.ActualDollarsComparator;
import com.ansmed.sb.comparator.PermsComparator;
import com.ansmed.sb.comparator.RankComparator;
import com.ansmed.sb.comparator.TrialsComparator;
import com.ansmed.sb.physician.ActualsAction;
import com.ansmed.sb.physician.ActualsVO;
import com.ansmed.sb.security.ANSRoleFilter;
import com.ansmed.sb.util.calendar.InvalidCalendarException;
import com.ansmed.sb.util.calendar.SJMBusinessCalendar;

/****************************************************************************
 * <b>Title</b>: RankReportAction.java<p/>
 * <b>Description: </b> Returns rank and business plan data for surgeon.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Apr. 27, 2009
 ****************************************************************************/
public class RankReportAction extends SBActionAdapter {
	
	/**
	 * 
	 */
	public RankReportAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public RankReportAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	/**
	 * 
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		String schema = (String) this.getAttribute("customDbSchema");
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		ANSRoleFilter filter = new ANSRoleFilter();
		
		String surgeonId = StringUtil.checkVal(req.getParameter("surgeonId"));
		int order = Convert.formatInteger(req.getParameter("orderBy")).intValue();
		
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
		
		StringBuffer sql = new StringBuffer();
		
		sql.append("select d.sales_rep_id, d.last_nm as rep_last_nm, ");
		sql.append("d.first_nm as rep_first_nm, a.surgeon_id, a.last_nm as phys_last_nm, ");
		sql.append("a.first_nm as phys_first_nm, a.title_nm, a.rank_no, ");
		sql.append("b.business_plan_id, b.value_txt from (");
		sql.append(schema).append("ans_sales_region y inner join ").append(schema);
		sql.append("ans_sales_rep d on y.region_id = d.region_id inner join ");
		sql.append(schema).append("ans_surgeon a on d.sales_rep_id = a.sales_rep_id ");
		sql.append("left outer join ").append(schema).append("ans_xr_surgeon_busplan b ");
		sql.append("on a.surgeon_id = b.surgeon_id ");
		sql.append("inner join ").append(schema).append("ans_business_plan c ");
		sql.append("on b.business_plan_id = c.business_plan_id) left outer join ");
		sql.append(schema).append("ans_bp_category e on c.category_id = e.category_id ");
		sql.append("where 1 = 1 ");
		
		if (surgeonId.length() > 0) sql.append("and a.surgeon_id = ? ");
		
		// Add the role filter
		Boolean edit = false;
		if (mod.getDisplayPage().indexOf("facade") > 1) {
			edit = Boolean.TRUE;
		}
		sql.append(filter.getSearchFilter(role, "d", edit));
		
		sql.append("and b.bp_quarter_no = ? "); 
		sql.append("and b.bp_year_no = ? ");
		sql.append("and e.category_type_id = 2 ");
		sql.append("order by rep_last_nm, rep_first_nm, phys_last_nm, phys_first_nm ");
		
		log.info("ANS Rank Report plan data SQL: " + sql);
		
		PreparedStatement ps = null;
		String currSurgeonId = "";
		String prevSurgeonId = "";
		RankActualVO rvo = null;
		List<RankActualVO> data = new ArrayList<RankActualVO>();
		//Map<String,RankActualVO> data = new HashMap<String,RankActualVO>();
		int counter = 0;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			if(surgeonId.length() > 0) ps.setString(++counter, surgeonId);
			ps.setInt(++counter, currQtr);
			ps.setInt(++counter, currYr);
			
			ResultSet rs = ps.executeQuery();
	
			while (rs.next()) {
				currSurgeonId = StringUtil.checkVal(rs.getString("surgeon_id"));
				if (! currSurgeonId.equalsIgnoreCase(prevSurgeonId)) {
					if (rvo != null) data.add(rvo);
					rvo = new RankActualVO(rs);
				} else {
					rvo.addPlanData(rs.getString("business_plan_id"), rs.getString("value_txt"));
				}
				prevSurgeonId = currSurgeonId;
			}
			if (rvo != null) data.add(rvo);
			
		} catch (SQLException sqle) {
			log.error("Error retrieving rank report plan data.", sqle);
		}
		
		log.debug("RankActualVO list size: " + data.size());
		
		SMTActionInterface aa = new ActualsAction(this.actionInit);
		aa.setAttributes(this.attributes);
		aa.setDBConnection(dbConn);
		aa.retrieve(req);
		
		// Add the surgeons actuals data to the RankActualVO's
		processActuals(data, req.getAttribute(ActualsAction.PHYSICIAN_ACTUALS));	
		
		// Sort the RankActualVO list
		switch(order) {
		case 1:
			Collections.sort(data, new RankComparator());
			log.debug("sorting by: rank.");
			break;
		case 2:
			Collections.sort(data, new ActualDollarsComparator());
			log.debug("sorting by: actual dollars.");
			break;
		case 3:
			Collections.sort(data, new TrialsComparator());
			log.debug("sorting by: trials.");
			break;
		case 4:
			Collections.sort(data, new PermsComparator());
			log.debug("sorting by: perms.");
			break;
		case 5:
			break;
		default:
			Collections.sort(data, new RankComparator());
			break;
		}
		
		mod.setDataSize(data.size());
		mod.setActionData(data);
		
		AbstractSBReportVO rpt = new RankReport();
		rpt.setData(mod.getActionData());
		rpt.setFileName("RankingIndividualReport." + req.getParameter("reportType", "html"));
		log.debug("Mime Type: " + rpt.getContentType());
		
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
		req.setAttribute(Constants.BINARY_DOCUMENT, rpt);

	}
	
	@SuppressWarnings("unchecked")
	public List<RankActualVO> processActuals(List<RankActualVO> data, Object o) {
		// Iterate the List, add the physician's ActualVO to the RankReportVO
		Map<String,ActualsVO> actuals = new HashMap<String,ActualsVO>();
		// Get the ActualsVO map.
		if (o instanceof Map<?,?>) {
			actuals = (Map<String,ActualsVO>)o;
		}
		if (actuals.size() == 0) return null;
		
		// Iterate the List of RankActualVOs
		String surgeonId = "";
		for (Iterator<RankActualVO> iter = data.iterator(); iter.hasNext();) {
			RankActualVO rav = iter.next();
			surgeonId = rav.getSurgeonId();
			// If ActualsVO exists for surgeon, add it to the surgeon's RankActualVO.
			if (actuals.containsKey(surgeonId)) {
				rav.setActualsData(actuals.get(surgeonId));
			}
		}
		return data;
	}
}
