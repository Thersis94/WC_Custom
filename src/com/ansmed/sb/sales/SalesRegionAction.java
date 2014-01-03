package com.ansmed.sb.sales;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.smt.sitebuilder.action.SBActionAdapter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

/*****************************************************************************
 <p><b>Title</b>: SalesRegionAction.java</p>
 <p>Description: <b/></p>
 <p>Copyright: Copyright (c) 2000 - 2006 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 1.0
 @since Sep 5, 2007
 Last Updated:
 ***************************************************************************/

public class SalesRegionAction extends SBActionAdapter implements Serializable {
	private static final long serialVersionUID = 1l;
	
	/**
	 * 
	 */
	public SalesRegionAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public SalesRegionAction(ActionInitVO actionInit) {
		super(actionInit);
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#delete(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void delete(SMTServletRequest req) throws ActionException {
		String message = "You have successfully deleted the Region";
		
		String schema = (String)getAttribute("customDbSchema");
		StringBuffer sql = new StringBuffer();
		sql.append("delete from ").append(schema).append("ans_sales_region ");
		sql.append("where region_id = ?");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, req.getParameter("regionId"));
			int success = ps.executeUpdate();
			if (success == 0) log.debug("Error updating record");
		} catch (SQLException sqle) {
			log.error("Error deleting ans sales region", sqle);
			message = "Unable to delete region";
		}
		
		req.setAttribute(SalesAreaFacadeAction.ANS_AREA_MESSAGE, message);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {
		StringBuffer sql = new StringBuffer();
		String regionId = StringUtil.checkVal(req.getParameter("regionId"));
		String schema = (String)getAttribute("customDbSchema");
		String message = "You have successfully updated the Region";
		
		if (regionId.length() == 0) {
			sql.append("insert into ").append(schema).append("ans_sales_region ");
			sql.append("(area_id, region_nm, create_dt, region_id) values (?,?,?,?) ");
			regionId = new UUIDGenerator().getUUID();
		} else {
			sql.append("update ").append(schema).append("ans_sales_region ");
			sql.append("set area_id = ?, region_nm = ?, update_dt = ? where region_id = ?");
		}
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, req.getParameter("areaId"));
			ps.setString(2, req.getParameter("regionName"));
			ps.setTimestamp(3, Convert.getCurrentTimestamp());
			ps.setString(4, regionId);
			
			int success = ps.executeUpdate();
			if (success == 0) log.debug("Error updating record");
		} catch (SQLException sqle) {
			log.error("Error updating ans sales region", sqle);
			message = "Unable to update region";
		}
		
		req.setAttribute(SalesAreaFacadeAction.ANS_AREA_MESSAGE, message);
	}

}
