 package com.ansmed.sb.physician;

// JDK 1.5.0
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;

// SMT Base Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;

// SB Libs
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

// SB ANS Libs
import com.ansmed.sb.security.ANSRoleFilter;
import com.ansmed.sb.util.calendar.InvalidCalendarException;
import com.ansmed.sb.util.calendar.SJMBusinessCalendar;

/****************************************************************************
 * <b>Title</b>: ActualsAction.java<p/>
 * <b>Description: </b> Returns map of actuals data for surgeons.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Apr. 27, 2009
 ****************************************************************************/
public class ActualsAction extends SBActionAdapter {
	
	public static final String PHYSICIAN_ACTUALS = "physicianActuals";
	
	/**
	 * 
	 */
	public ActualsAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public ActualsAction(ActionInitVO arg0) {
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
		
		//retrieve the current SJM business calendar
		SJMBusinessCalendar bc = null;
		try {
			bc = new SJMBusinessCalendar();
		} catch (InvalidCalendarException ice) {
			log.error("Error retrieving SJM business calendar.", ice);
			throw new ActionException("Error retrieving business calendar.", ice);
		}
		
		int currYr = bc.getCurrentYear();
		
		StringBuffer sql = new StringBuffer();
		
		sql.append("select a.surgeon_id, b.quarter_no, b.trial_no, b.ipg_no + ");
		sql.append("b.rc_ipg_no as perms_no, b.eol_ipg_no + eol_ipg_a_no as ");
		sql.append("revisions_no, b.physician_dollar_no from ");
		sql.append(schema).append("ans_sales_region y inner join ").append(schema);
		sql.append("ans_sales_rep d on y.region_id = d.region_id inner join ");
		sql.append(schema).append("ans_surgeon a on d.sales_rep_id = a.sales_rep_id ");
		sql.append("inner join ").append(schema).append("ans_physician_actual b ");
		sql.append("on a.surgeon_id = b.surgeon_id ");
		sql.append("where 1 = 1 and b.year_no = ? ");
		
		if (surgeonId.length() > 0) sql.append("and a.surgeon_id = ? ");
		
		// Add the role filter
		Boolean edit = false;
		if (mod.getDisplayPage().indexOf("facade") > 1) {
			edit = Boolean.TRUE;
		}
		sql.append(filter.getSearchFilter(role, "d", edit));		
		
		sql.append("order by a.surgeon_id, b.quarter_no, b.month_no ");

		log.info("ANS Actuals data SQL: " + sql);
		//log.info("Current year - 1: " + (currYr - 1));
		
		int counter = 0;
		PreparedStatement ps = null;
		ActualsVO avo = null;
		//Map surgeonId, ActualsVO
		Map<String,ActualsVO> data = new HashMap<String,ActualsVO>();
		String prevId = "";
		String currId = "";
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(++counter, (currYr - 1));
			if (surgeonId.length() > 0) {
				//log.info("surgeonId: " + surgeonId);
				ps.setString(++counter, surgeonId);
			}
			
			ResultSet rs = ps.executeQuery();
	
			while (rs.next()) {
				currId = StringUtil.checkVal(rs.getString("surgeon_id"));
				//log.debug("currId/dollars: " + currId + "/" + rs.getString("physician_dollar_no"));
				if (currId.equalsIgnoreCase(prevId)) {
					avo.setData(rs);
				} else {
					if (avo != null) data.put(prevId, avo);
					avo = new ActualsVO(rs);
				}
				prevId = currId;
			}
			if (avo != null) data.put(currId, avo);
			
		} catch (SQLException sqle) {
			log.error("Error retrieving actuals data.", sqle);
		}
		
		log.debug("ActualsVO map size: " + data.size());
		
		req.setAttribute(PHYSICIAN_ACTUALS, data);
	}
	
}
