package com.ansmed.sb.sales;

// JDK 1.5.0
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// SMT Base Libs 2.0
import com.smt.sitebuilder.action.SBActionAdapter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;

// SB Libs
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: AssignRepAction.java</p>
 <p>Description: <b/>Manages the rep information and the assignment of surgeons
 to reps</p>
 <p>Copyright: Copyright (c) 2000 - 2006 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 1.0
 @since Sep 6, 2007
 Last Updated:
 ***************************************************************************/

public class AssignRepAction extends SBActionAdapter {

	/**
	 * 
	 */
	public AssignRepAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public AssignRepAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("Retrieving Sales rep for assignment");
		StringBuffer sql = new StringBuffer();
		String schema = (String)getAttribute("customDbSchema");
		sql.append("select * from ").append(schema).append("ans_sales_rep ");
		sql.append("where sales_rep_id <> ? ");
		sql.append("order by last_nm, first_nm ");
		log.debug("SQL: " + sql + "|" + req.getParameter("regionId") + "|" + req.getParameter("salesRepId"));
		
		// Retrieve the data and store into a Map
		List<SalesRepVO> data = new ArrayList<SalesRepVO>();
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			//ps.setString(1, req.getParameter("regionId"));
			ps.setString(1, req.getParameter("salesRepId"));
			ResultSet rs = ps.executeQuery();

			while(rs.next()) {
					data.add(new SalesRepVO(rs));
			} 
		} catch(SQLException sqle) {
			log.error("Error retrieving ans sales reps", sqle);
		}
		
		// Store the retrieved data in the ModuleVO.actionData and replace into
		// the Map
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		mod.setActionData(data);
		attributes.put(Constants.MODULE_DATA, mod);
		log.debug("Reps in area: " + data.size());
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
		log.debug("Ressigning reps");
		String schema = (String)getAttribute("customDbSchema");
		String message = "You have successfully reassigned the physicians";
		StringBuffer sql = new StringBuffer();

		sql.append("update ").append(schema).append("ans_surgeon ");
		sql.append("set sales_rep_id = ? where surgeon_id = ? ");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			String[] ids = req.getParameterValues("surgeonId");
			for (int i=0; i < ids.length; i++) {
				log.debug("Assign: " + ids[i] + "|" + req.getParameter("salesRepId"));
				ps.setString(1, req.getParameter("salesRepId"));
				ps.setString(2, ids[i]);
				ps.addBatch();
			}
			
			int[] count = ps.executeBatch();
			log.debug("Number updated: " + count.length);
		} catch (Exception sqle) {
			log.error("Error assigning reps to new rep", sqle);
			message = "Error reassigning the sales reps";
		}
		
		req.setAttribute(SalesAreaFacadeAction.ANS_AREA_MESSAGE, message);

	}

}
