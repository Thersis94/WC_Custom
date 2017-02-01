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
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

// SB Libs
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/*****************************************************************************
<p><b>Title</b>: ResourceAction.java</p>
<p>Description: <b/></p>
<p>Copyright: Copyright (c) 2000 - 2009 SMT, All Rights Reserved</p>
<p>Company: Silicon Mountain Technologies</p>
@author David Bargerhuff
@version 1.0
@since Feb 18, 2009
Last Updated:
***************************************************************************/

public class ResourceAction extends SBActionAdapter {

	/**
	 * 
	 */
	public ResourceAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public ResourceAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("Building resources...");
		
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
	public void update(ActionRequest req) throws ActionException {
		String schema = (String)getAttribute("customDbSchema");
		String message = "You have successfully updated the resource information.";
		
		String surgeonId = StringUtil.checkVal(req.getParameter("surgeonId"));
		String resourceId = StringUtil.checkVal(req.getParameter("resourceId"));
		StringBuffer sql = new StringBuffer();
		
		if (resourceId.length() > 0) {
			log.debug("Updating resource...");
			sql.append("update ").append(schema).append("ans_resource set ");
			sql.append("resource_type_id = ?, resource_obj_txt = ?, ");
			sql.append("resource_result_txt = ?, used_qtr_no = ?, used_yr_no = ?, ");
			sql.append("completion_month_no = ?, completion_yr_no = ?, ");
			sql.append("update_dt = ? where surgeon_id = ? and resource_id = ?");
		} else {
			log.debug("Inserting resource...");
			sql.append("insert into ").append(schema).append("ans_resource ");
			sql.append("(resource_type_id, resource_obj_txt, resource_result_txt, ");
			sql.append("used_qtr_no, used_yr_no, completion_month_no, ");
			sql.append("completion_yr_no, create_dt, surgeon_id, ");
			sql.append("resource_id) values (?,?,?,?,?,?,?,?,?,?)");
		}
		
		log.debug("Resource SQL: " + sql.toString());
		
		PreparedStatement ps = null;
		try {
			dbConn.setAutoCommit(true);
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, Convert.formatInteger(req.getParameter("resourceTypeId")));
			ps.setString(2, req.getParameter("resourceObj"));
			ps.setString(3, req.getParameter("resourceResult"));
			ps.setInt(4, Convert.formatInteger(req.getParameter("usedQtr")));
			ps.setInt(5, Convert.formatInteger(req.getParameter("usedYear")));
			ps.setInt(6, Convert.formatInteger(req.getParameter("completionMonth")));
			ps.setInt(7, Convert.formatInteger(req.getParameter("completionYear")));
			ps.setTimestamp(8, Convert.getCurrentTimestamp());
			ps.setString(9, surgeonId);
			if (resourceId.length() > 0) {
				ps.setString(10, resourceId);
			} else {
				ps.setString(10, new UUIDGenerator().getUUID());
			}
			
			ps.execute();
			
		} catch(SQLException sqle) {
			sqle.printStackTrace();
			log.error("Error inserting/updating Physician's resource information.", sqle);
			message = "Error updating the Physician's resource information.";
		} finally {
		}
		try {
			ps.close();
		} catch(Exception e) {}
		
		// Add the message to the req object
		req.setAttribute(PhysicianDataFacadeAction.ANS_PHYSICIAN_MESSAGE, message);
		
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#delete(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		String schema = (String)getAttribute("customDbSchema");
		String message = "You have successfully deleted the resource information.";
		
		String resourceId = StringUtil.checkVal(req.getParameter("resourceId"));
		String surgeonId = StringUtil.checkVal(req.getParameter("surgeonId"));
		
		StringBuffer del = new StringBuffer();
		del.append("delete from ").append(schema).append("ans_resource ");
		del.append("where resource_id = ? and surgeon_id = ?");

		log.info("Resource delete SQL: " + del + req.getParameter("deleteId") + "|" + req.getParameter("surgeonId"));
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(del.toString());
			ps.setString(1, resourceId);
			ps.setString(2, surgeonId);
			
			// Execute the delete
			ps.executeUpdate();
		} catch(SQLException sqle) {
			log.error("Error deleting physician's resource data.", sqle);
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
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("Retrieving Resource data...");
		
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		ANSRoleFilter filter = new ANSRoleFilter();
		
		String surgeonId = StringUtil.checkVal(req.getParameter("surgeonId"));
		Integer orderBy = Convert.formatInteger(req.getParameter("orderBy"));
		String schema = (String)getAttribute("customDbSchema");

		StringBuffer sql = new StringBuffer();
		
		sql.append("select d.first_nm as 'rep_first_nm', d.last_nm as 'rep_last_nm', ");
		sql.append("a.first_nm as 'phys_first_nm', a.last_nm as 'phys_last_nm', ");
		sql.append("b.*, c.resource_nm from ").append(schema).append("ans_sales_region y ");
		sql.append("inner join ").append(schema).append("ans_sales_rep d ");
		sql.append("on y.region_id = d.region_id inner join ");
		sql.append(schema).append("ans_surgeon a on d.sales_rep_id = a.sales_rep_id ");
		sql.append("inner join ").append(schema).append("ans_resource b ");
		sql.append("on a.surgeon_id = b.surgeon_id inner join ").append(schema);
		sql.append("ans_resource_type c on b.resource_type_id = c.resource_type_id ");
		sql.append("where 1 = 1 ");
		
		if (surgeonId.length() > 0) {
			// surgeon ID exists if this is a resource tab request or an
			// individual resource utilization report request.
			sql.append("and a.surgeon_id = ? order by b.used_yr_no, b.used_qtr_no ");	
		} else {
			// this branch executed for resource summary report request
			// Add the role filter
			Boolean edit = false;
			if (mod.getDisplayPage().indexOf("facade") > 1) {
				edit = Boolean.TRUE;
			}
			sql.append(filter.getSearchFilter(role, "d", edit));
			sql.append("order by ");
			switch(orderBy) {
			case 1: // TM
				sql.append("rep_last_nm, rep_first_nm, phys_last_nm, phys_first_nm");
				break;
			case 2: // Physician
				sql.append("phys_last_nm, phys_first_nm");
				break;
			case 3: // Program
				sql.append("resource_type_id, rep_last_nm, rep_first_nm, phys_last_nm, phys_first_nm");
				break;
			case 4: // Projected Quarter
				sql.append("used_qtr_no, used_yr_no");
				break;
			case 5: // Actual Date
				sql.append("completion_month_no, completion_yr_no");
				break;
			default: // TM
				sql.append("rep_last_nm, rep_first_nm, phys_last_nm, phys_first_nm");
				break;
			}
		}
		
		log.debug("Resource SQL: " + sql.toString() + " | " + surgeonId);
		
		PreparedStatement ps = null;
		List<ResourceVO> rvo = new ArrayList<ResourceVO>();

		try {
			ps = dbConn.prepareStatement(sql.toString());
			
			if (surgeonId.length() > 0) ps.setString(1, surgeonId);
					
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				rvo.add(new ResourceVO(rs));
			}
		} catch(SQLException sqle) {
			log.error("Error retrieving physician resource data.", sqle);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		log.debug("List<ResourceVO> size: " + rvo.size());
		
		// Store the retrieved data in the ModuleVO.actionData and replace into
		// the Collection
		mod.setActionData(rvo);
		attributes.put(Constants.MODULE_DATA, mod);
	}
	
}
