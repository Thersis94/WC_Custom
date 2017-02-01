package com.ansmed.sb.action;

// JDK 1.6
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// SB libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

// SMT base libs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;

//SB ANS Medical libs
import com.ansmed.sb.util.calendar.InvalidCalendarException;
import com.ansmed.sb.util.calendar.SJMBusinessCalendar;

/*****************************************************************************
<p><b>Title</b>: PhysicianQualDataAction.java</p>
<p>Description: <b/>Retrieves the physician's event qualification data.</p>
<p>Copyright: Copyright (c) 2000 - 2009 SMT, All Rights Reserved</p>
<p>Company: Silicon Mountain Technologies</p>
@author Dave Bargerhuff
@version 1.0
@since July 27, 2009
Last Updated:
09-01-2009: Replaced BusinessPlanUtil with SJMBusinessCalendar.
***************************************************************************/
public class PhysicianQualDataAction extends SimpleActionAdapter {

	public static final String PHYS_QUAL_DATA_VO = "physQualDataVo";
	
	/**
	 * 
	 */
	public PhysicianQualDataAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public PhysicianQualDataAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}
	
	   /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
	public void build(ActionRequest req) throws ActionException {
		
		final String schema = (String) this.getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
   		//update the qualData we have in sessionScope
		PhysQualDataVO vo = (PhysQualDataVO) req.getSession().getAttribute(PhysicianEventAction.PHYS_QUAL_DATA);
		vo.setData(req);

		StringBuffer sql = new StringBuffer();
		//determine what qualData we are updating and build the query
		if (req.getParameter("scsStartDate") != null || req.getParameter("specialtyId") != null) {
			sql.append("update ").append(schema).append("ans_surgeon");
			sql.append(" set ");
			if (req.getParameter("scsStartDate") != null)
				sql.append("scs_start_dt=?, ");
			if (req.getParameter("specialtyId") != null)
				sql.append("specialty_id=?, ");
			
			sql.append("update_dt=? where surgeon_id=?");
		}
		log.debug("sql=" + sql);
    	PreparedStatement ps = null;
    	int i = 0;
    	try {
    		ps = dbConn.prepareStatement(sql.toString());
    		if (req.getParameter("scsStartDate") != null)
				ps.setDate(++i, Convert.formatSQLDate(vo.getScsStartDate()));
			if (req.getParameter("specialtyId") != null)
				ps.setInt(++i, vo.getSpecialtyId());
			
			ps.setTimestamp(++i, Convert.getCurrentTimestamp());
			ps.setString(++i, req.getParameter("surgeonId"));
			ps.executeUpdate();
				
    	} catch(SQLException sqle) {
    		log.error("Unable to update ANS physQualData", sqle);
    	} finally {
    		try { ps.close(); } catch(Exception e) {}
    		
    		//update the sessionScope bean
    		req.getSession().setAttribute(PhysicianEventAction.PHYS_QUAL_DATA, vo);
    	}
	    	
	}
	
	
    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
    public void retrieve(ActionRequest req) throws ActionException {
    	//BusinessPlanUtil bpu = new BusinessPlanUtil();
		//retrieve the current SJM business calendar
		SJMBusinessCalendar bc = null;
		try {
			bc = new SJMBusinessCalendar();
		} catch (InvalidCalendarException ice) {
			log.error("Error retrieving SJM business calendar.", ice);
			throw new ActionException();
		}
		
    	PhysQualDataVO vo = new PhysQualDataVO();
    	
	    PreparedStatement ps = null;
		final String schema = (String) this.getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuffer sql = new StringBuffer();
		
		//retrieve the surgeon
		sql.append("select surgeon_id, profile_id, first_nm, last_nm, title_nm, ");
		sql.append("surgeon_type_id, specialty_id, scs_start_dt from ");
		sql.append(schema).append("ans_surgeon a where surgeon_id = ? ");
		
		log.debug("Surgeon sql: " + sql + " | " + req.getParameter("surgeonId"));
		
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, req.getParameter("surgeonId"));
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				vo.setData(rs); 
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving surgeon information for PhysQualData", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		
		// retrieve the 'actuals' data
		ps = null;
		sql = new StringBuffer();
		
		sql.append("select trial_no, ipg_no, rc_ipg_no from ");
		sql.append(schema).append("ans_physician_actual where surgeon_id = ? ");
			
		// Determine the 'actuals' quarters that we need.
		switch(bc.getCurrentQuarter()) {
			case 1:
				// If Q1, we need Q1 - Q4 of previous year's actuals
				sql.append("and (year_no = ").append(bc.getCurrentYear() - 1);
				sql.append(" or year_no is null)");
				break;
			case 2:
				// If Q2, we need Q2 - Q4 of prev year's actuals, Q1 of curr year.
				sql.append("and ((quarter_no in (2,3,4) and year_no = ");
				sql.append(bc.getCurrentYear() - 1).append(") or (");
				sql.append("quarter_no = 1 and year_no = ");
				sql.append(bc.getCurrentYear()).append(") or (quarter_no is null ");
				sql.append("and year_no is null))");
				break;
			case 3:
				// If Q3, we need Q3 - Q4 of prev year's actuals, Q1 - Q2 of curr year.
				sql.append("and ((quarter_no in (3,4) and year_no = ");
				sql.append(bc.getCurrentYear() - 1).append(") or (");
				sql.append("quarter_no in (1,2) and year_no = ");
				sql.append(bc.getCurrentYear()).append(") or (quarter_no is null ");
				sql.append("and year_no is null))");
				break;
			case 4:
				// If Q4, we need Q4 from prev year's actuals, Q1 - Q3 of curr year.
				sql.append("and ((quarter_no = 4 and year_no = ");
				sql.append(bc.getCurrentYear() - 1).append(") or (");
				sql.append("quarter_no in (1,2,3) and year_no = ");
				sql.append(bc.getCurrentYear()).append(") or (quarter_no is null ");
				sql.append("and year_no is null))");
				break;
		}
						
		log.debug("Implant 'actuals' sql=" + sql + " | " + req.getParameter("surgeonId"));
		
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, req.getParameter("surgeonId"));
			ResultSet rs = ps.executeQuery();
			int cnt = 0;
			while (rs.next()) {
				cnt += rs.getInt("trial_no") + rs.getInt("ipg_no") + rs.getInt("rc_ipg_no");
			}
			vo.setImplantCnt(cnt);
			log.debug("Implant count after 'actuals': " + vo.getImplantCnt());
		} catch (SQLException sqle) {
			log.error("Error retrieving 'actuals' data for PhysQualData", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		
		/* Dave Bargerhuff
		 * 7-27-09: As of now we only have 'actuals' data for year 2008.  That
		 * means that we have to also get 'forecast' data from the 'current'
		 * quarter record set for the current year.  In the future, when 
		 * up-to-date actuals data is provided on a regular basis, then we will 
		 * need to change this to pull only the quarter(s) for the 'current' 
		 * year for which we don't have 'actuals' data.
		 */
		sql = new StringBuffer();
		sql.append("select business_plan_id, value_txt, bp_quarter_no, bp_year_no from ");
		sql.append(schema).append("ans_xr_surgeon_busplan where 1 = 1 ");
		sql.append("and surgeon_id = ? and bp_year_no = ? ");
		
		switch(bc.getCurrentQuarter()) {
		case 1:
			/* We need to use the current quarter's record set to get the
			 * latest implant values for the previous quarters in this year.
			 * If it's Q1 we need to return a zero value because we are
			 * assuming that we have the data we need from Q1-Q4 of the
			 * 'actuals' data.  If there is no 'actuals' data for a physician,
			 * and it's Q1, then this is a new physician and per Ryan Calhoun
			 * we need to return a value of zero for now.
			*/
			sql.append("and bp_quarter_no = 99");
			break;
		case 2:
			// If Q2, we need Q1 of curr year.
			sql.append("and bp_quarter_no = 2 ");
			sql.append("and  business_plan_id in ('trialQ1','permQ1',");
			sql.append("'bostonScientific','medtronic','sjmMedical','unknown')");
			break;
		case 3:
			// If Q3, we need Q1 - Q2 of curr year.
			sql.append("and bp_quarter_no = 3 ");
			sql.append("and  business_plan_id in ('trialQ1','trialQ2',");
			sql.append("'permQ1','permQ2',");
			sql.append("'bostonScientific','medtronic','sjmMedical','unknown')");
			break;
		case 4:
			// If Q4, we need Q1 - Q3 of curr year.
			sql.append("and bp_quarter_no = 4 ");
			sql.append("and  business_plan_id in ");
			sql.append("('trialQ1','trialQ2','trialQ3',");
			sql.append("'permQ1','permQ2','permQ3',");
			sql.append("'bostonScientific','medtronic','sjmMedical','unknown')");
			break;
		}
			
		log.debug("Implant 'forecast' sql=" + sql + " yr=" + bc.getCurrentYear() + " qtr=" + bc.getCurrentQuarter());
		
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, req.getParameter("surgeonId"));
			ps.setInt(2, bc.getCurrentYear());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				vo.addImplantData(rs.getString("business_plan_id"), rs.getString("value_txt"));
			}
			
			// Calculate the overall total implant count using the market share data
			vo.setImplantCountUsingMarketShare();
			
			log.debug("Overall implant count: actualImplants = " + vo.getImplantCnt());
		} catch (SQLException sqle) {
			log.error("Error retrieving 'forecast' data for PhysQualData", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		
		log.debug("PhysQualData= " + vo.toString());
		req.setAttribute(PHYS_QUAL_DATA_VO, vo);
    
	}
	
}
