package com.ansmed.sb.physician;

// JDK 1.5.0
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// SMT Base Libs 2.0
import com.ansmed.sb.security.ANSRoleFilter;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

// SB Libs
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/*****************************************************************************
<p><b>Title</b>: BusinessAssessmentAction.java</p>
<p>Description: <b/></p>
<p>Copyright: Copyright (c) 2000 - 2009 SMT, All Rights Reserved</p>
<p>Company: Silicon Mountain Technologies</p>
@author David Bargerhuff
@version 1.0
@since Feb 10, 2009
Last Updated:
***************************************************************************/

public class BusinessAssessmentAction extends SBActionAdapter {
	public static final String BUSINESS_ASSESSMENTS = "busAssessments";
	public static final String BUSINESS_GOALS = "busGoals";

	/**
	 * 
	 */
	public BusinessAssessmentAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public BusinessAssessmentAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("Building business assessments/goals...");
		
		String deleteEle = StringUtil.checkVal(req.getParameter("deleteEle"));
		if (deleteEle.equalsIgnoreCase("true")) {
			delete(req);
		} else {
			update(req);
		}		
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {
		String schema = (String)getAttribute("customDbSchema");
		String message = "You have successfully updated the business assessment information.";
		
		String surgeonId = StringUtil.checkVal(req.getParameter("surgeonId"));
		
		String type = StringUtil.checkVal(req.getParameter("processType"));
		String typeId = "";
		log.debug("processType: " + type);
		
		StringBuffer sql = new StringBuffer();
		if (type.equalsIgnoreCase("assess")) {
			typeId = StringUtil.checkVal(req.getParameter("busAssessId"));
			if (typeId.length() > 0) {
				//update assessment
				sql.append("update ").append(schema).append("ans_business_assessment ");
				sql.append("set bus_assess_type = ?, bus_assess_txt = ?, ");
				sql.append("update_dt = ? where surgeon_id = ? and bus_assess_id = ?");
			} else {
				//insert assessment
				sql.append("insert into ").append(schema);
				sql.append("ans_business_assessment (bus_assess_type, ");
				sql.append("bus_assess_txt, create_dt, surgeon_id, ");
				sql.append("bus_assess_id) values (?,?,?,?,?)");
			}
		} else if (type.equalsIgnoreCase("goals")) {
			typeId = StringUtil.checkVal(req.getParameter("busGoalId"));
			if (typeId.length() > 0) {
				//update goal
				sql.append("update ").append(schema).append("ans_busassess_goals set ");
				sql.append("goal_txt = ?, goal_action_txt = ?, update_dt = ? ");
				sql.append("where surgeon_id = ? and bus_assess_goal_id = ?");
			} else {
				//insert goal
				sql.append("insert into ").append(schema);
				sql.append("ans_busassess_goals (goal_txt, goal_action_txt, ");
				sql.append("create_dt, surgeon_id, bus_assess_goal_id) values (?,?,?,?,?)");
			}
		} else {
			message = "WARNING: Could not update the physician's Business Assessment information.";
			req.setAttribute(PhysicianDataFacadeAction.ANS_PHYSICIAN_MESSAGE, message);
			return;
		}
		
		log.debug("busAssess SQL: " + sql.toString());
		
		PreparedStatement ps = null;
		try {
			dbConn.setAutoCommit(true);
			ps = dbConn.prepareStatement(sql.toString());
			if (type.equalsIgnoreCase("assess")) {
				ps.setInt(1,Convert.formatInteger(req.getParameter("assessType")));
				ps.setString(2,req.getParameter("assessTxt"));
				ps.setTimestamp(3, Convert.getCurrentTimestamp());
				ps.setString(4, surgeonId);
				if (typeId.length() > 0) {
					ps.setString(5, typeId);
				} else {
					ps.setString(5, new UUIDGenerator().getUUID());
				}
			} else {
				ps.setString(1,req.getParameter("goalName"));
				ps.setString(2,req.getParameter("goalAction"));
				ps.setTimestamp(3, Convert.getCurrentTimestamp());
				ps.setString(4, surgeonId);
				if (typeId.length() > 0) {
					ps.setString(5, typeId);
				} else {
					ps.setString(5, new UUIDGenerator().getUUID());
				}
			}
			
			ps.execute();
			
		} catch(SQLException sqle) {
			sqle.printStackTrace();
			log.error("Error updating the business assessment information.", sqle);
			message = "Error updating the business assessment information.";
		} finally {
		}
		try {
			ps.close();
		} catch(Exception e) {}
		
		req.setAttribute(PhysicianDataFacadeAction.ANS_PHYSICIAN_MESSAGE, message);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#delete(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void delete(SMTServletRequest req) throws ActionException {
		String schema = (String)getAttribute("customDbSchema");
		String message = "You have successfully deleted the business assessment information.";
		String processType = StringUtil.checkVal(req.getParameter("processType"));
		String typeId = "";
		String surgeonId = StringUtil.checkVal(req.getParameter("surgeonId"));
		
		StringBuffer del = new StringBuffer();
		
		if (processType.equalsIgnoreCase("assess")) {
			typeId = StringUtil.checkVal(req.getParameter("busAssessId"));
			del.append("delete from ").append(schema).append("ans_business_assessment ");
			del.append("where bus_assess_id = ? and surgeon_id = ?");
		} else if (processType.equalsIgnoreCase("goals")) {
			typeId = StringUtil.checkVal(req.getParameter("busGoalId"));
			del.append("delete from ").append(schema).append("ans_busassess_goals ");
			del.append("where bus_assess_goal_id = ? and surgeon_id = ?");
		}

		log.info("Bus Assess delete SQL: " + del + "|" + typeId + "|" + surgeonId);
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(del.toString());
			ps.setString(1, typeId);
			ps.setString(2, surgeonId);
			
			// Execute the delete
			ps.executeUpdate();
		} catch(SQLException sqle) {
			log.error("Error deleting physician's Business Assessment data.", sqle);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		req.setAttribute(PhysicianDataFacadeAction.ANS_PHYSICIAN_MESSAGE, message);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("Retrieving Business Assessment data...");
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		ANSRoleFilter filter = new ANSRoleFilter();
		
		String surgeonId = StringUtil.checkVal(req.getParameter("surgeonId"));
		String schema = (String)getAttribute("customDbSchema");

		StringBuffer sql = new StringBuffer();
		
		sql.append("select d.first_nm as rep_first_nm, d.last_nm as rep_last_nm, ");
		sql.append("a.first_nm as phys_first_nm, a.last_nm as phys_last_nm, ");
		sql.append("b.* from ").append(schema).append("ans_sales_rep d inner join ");
		sql.append(schema).append("ans_surgeon a on d.sales_rep_id = a.sales_rep_id ");
		sql.append("inner join ").append(schema).append("ans_business_assessment b ");
		sql.append("on a.surgeon_id = b.surgeon_id ");
		sql.append("where 1 = 1 ");
		
		if (surgeonId.length() > 0) {
			sql.append("and a.surgeon_id = ? ");
		}
		
		/* Add the role filter */
		Boolean edit = false;
		if (mod.getDisplayPage().indexOf("facade") > 1) {
			edit = Boolean.TRUE;
		}
		sql.append(filter.getSearchFilter(role, "d", edit));
		
		if (surgeonId.length() > 0) {
			sql.append("order by bus_assess_type ");
		} else {
			sql.append("order by rep_last_nm, rep_first_nm, phys_last_nm, ");
			sql.append("phys_first_nm, bus_assess_type ");
		}
		
		log.debug("Business assessment SQL: " + sql.toString() + " | " + surgeonId);
		
		PreparedStatement ps = null;
		List<BusAssessVO> bavo = new ArrayList<BusAssessVO>();
		int counter = 0;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			if (surgeonId.length() > 0) {
				ps.setString(++counter, surgeonId);
			}
					
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				bavo.add(new BusAssessVO(rs));
			}
		} catch(SQLException sqle) {
			log.error("Error retrieving physician's business assessment data.", sqle);
		}
		
		log.debug("List<BusAssesVO> size: " + bavo.size());
		
		StringBuffer sql_goal = new StringBuffer();
		
		sql_goal.append("select d.first_nm as 'rep_first_nm', d.last_nm as 'rep_last_nm', ");
		sql_goal.append("a.first_nm as 'phys_first_nm', a.last_nm as 'phys_last_nm', ");
		sql_goal.append("b.* from ").append(schema).append("ans_sales_rep d inner join ");
		sql_goal.append(schema).append("ans_surgeon a on d.sales_rep_id = a.sales_rep_id ");
		sql_goal.append("inner join ").append(schema).append("ans_busassess_goals b ");
		sql_goal.append("on a.surgeon_id = b.surgeon_id ");
		sql_goal.append("where 1 = 1 ");
		
		if (surgeonId.length() > 0) {
			sql_goal.append("and a.surgeon_id = ? ");
		}
		
		/* Add the role filter */
		edit = false;
		if (mod.getDisplayPage().indexOf("facade") > 1) {
			edit = Boolean.TRUE;
		}
		sql_goal.append(filter.getSearchFilter(role, "d", edit));
		
		
		if (surgeonId.length() > 0) {
			sql_goal.append("order by b.create_dt");
		} else {
			sql.append("order by rep_last_nm, rep_first_nm, phys_last_nm, ");
			sql.append("phys_first_nm, b.create_dt ");
		}
		
		log.debug("Business goal SQL: " + sql_goal.toString() + " | " + surgeonId);
		
		ps = null;
		List<BusGoalVO> bgvo = new ArrayList<BusGoalVO>();
		counter = 0;
		try {
			ps = dbConn.prepareStatement(sql_goal.toString());
			if (surgeonId.length() > 0) {
				ps.setString(++counter, surgeonId);
			}
					
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				bgvo.add(new BusGoalVO(rs));
			}
		} catch (SQLException sqle) {
			
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		log.debug("List<BusGoalVO> size: " + bgvo.size());
		// Store the retrieved data in the ModuleVO.actionData and replace into
		// the Collection
		mod.setActionData(bavo);
		mod.setAttribute(BUSINESS_GOALS, bgvo);
		attributes.put(Constants.MODULE_DATA, mod);

	}
	
}
