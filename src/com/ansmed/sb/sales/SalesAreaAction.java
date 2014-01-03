package com.ansmed.sb.sales;

// JDK 1.5.0
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;

// SMT Base Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: SalesAreaAction.java</p>
 <p>Description: <b/>Manages the sales areas</p>
 <p>Copyright: Copyright (c) 2000 - 2006 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 1.0
 @since Sep 5, 2007
 Last Updated:
 ***************************************************************************/

public class SalesAreaAction extends SBActionAdapter {
	public static final long serialVersionUID = 1l;
	
	/**
	 * 
	 */
	public SalesAreaAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public SalesAreaAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest arg0) throws ActionException {
		StringBuffer sql = new StringBuffer();
		String schema = (String)getAttribute("customDbSchema");
		sql.append("select a.*, b.* from ").append(schema).append("ans_sales_area a ");
		sql.append("left outer join ").append(schema).append("ans_sales_region b ");
		sql.append("on a.area_id = b.area_id order by area_nm");
		log.debug("ANS Saleas Area SQL: " + sql);
		
		// Retrieve the data and store into a Map
		Map<String, SalesAreaVO> data = new TreeMap<String, SalesAreaVO>();
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				SalesAreaVO vo = new SalesAreaVO(rs);
				// If the area exists, add the region otherwise, add the area
				if (data.containsKey(vo.getAreaName())) {
					String id = rs.getString("region_id");
					String name = rs.getString("region_nm");
					data.get(vo.getAreaName()).addRegion(id, name);
				} else {
					data.put(vo.getAreaName(), vo);
				}
			} 
		} catch(SQLException sqle) {
			log.error("Error retrieving ans sales areas", sqle);
		}
		
		// Store the retrieved data in the ModuleVO.actionData and replace into
		// the Map
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		mod.setActionData(data);
		attributes.put(Constants.MODULE_DATA, mod);
		log.debug("Size: " + data.size());
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {
		StringBuffer sql = new StringBuffer();
		String areaId = StringUtil.checkVal(req.getParameter("areaId"));
		String schema = (String)getAttribute("customDbSchema");
		String message = "You have successfully updated the area information";
		
		if (areaId.length() == 0) {
			sql.append("insert into ").append(schema).append("ans_sales_area ");
			sql.append("(area_nm, create_dt, area_id) values (?,?,?) ");
			areaId = new UUIDGenerator().getUUID();
		} else {
			sql.append("update ").append(schema).append("ans_sales_area ");
			sql.append("set area_nm = ?, update_dt = ? where area_id = ?");
		}
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, req.getParameter("areaName"));
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, areaId);
			
			int success = ps.executeUpdate();
			if (success == 0) message = "Error updting area information";
		} catch (SQLException sqle) {
			log.error("Error updating ans sales area", sqle);
			message = "Error updting area information";
		}
		
		req.setAttribute(SalesAreaFacadeAction.ANS_AREA_MESSAGE, message);
	}

}
