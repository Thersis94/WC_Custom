package com.restpeer.action.account;

// JDK 1.8.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// RP Libs
	import com.restpeer.data.LocationAttributeVO;
import com.siliconmtn.action.ActionException;

// SMT Base Libs
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: LocationAttributeWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the attribute data for a given location
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 15, 2019
 * @updates:
 ****************************************************************************/
public class LocationAttributeWidget extends SBActionAdapter {
	/**
	 * Json key to access this action
	 */
	public static final String AJAX_KEY = "locationAttribute";
	
	/**
	 * 
	 */
	public LocationAttributeWidget() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public LocationAttributeWidget(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String mlid = req.getParameter("memberLocationId");
		String gc = req.getParameter("groupCode");
		boolean all = req.getBooleanParameter("allData");
		try {
			setModuleData(getLocationAttributes(mlid, gc,all));
		} catch (SQLException e) {
			setModuleData(null, 0, e.getLocalizedMessage());
			log.error("Failed retrieving location attributes", e);
		}
	}
	
	/**
	 * Gets the list of attributes for a given location
	 * @param mlid
	 * @return
	 * @throws SQLException
	 */
	public Map<String,Object> getLocationAttributes(String mlid, String groupCode, boolean all) 
	throws SQLException {
		StringBuilder sql = new StringBuilder(320);
		sql.append("select a.attribute_cd, b.value_txt, group_cd, attribute_nm, ");
		sql.append("member_location_id, location_attribute_id from ");
		sql.append(getCustomSchema()).append("rp_attribute a");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("rp_location_attribute_xr b ");
		sql.append("on a.attribute_cd = b.attribute_cd and member_location_id = ? ");
		sql.append("where a.group_cd = ? and active_flg = 1 ");
		sql.append("order by attribute_nm ");
		log.info(sql.length() + "|" + sql + "|" + mlid + "|" + groupCode);
		
		// Loop the data and add to the map
		Map<String,Object> resData  = new LinkedHashMap<>(24);
		Map<String, String> data = new LinkedHashMap<>();
		List<LocationAttributeVO> allData = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, mlid);
			ps.setString(2, groupCode);
			try (ResultSet rs = ps.executeQuery()) {
				while(rs.next()) {
					log.info("Value: " + rs.getString(2));
					if (all) allData.add(new LocationAttributeVO(rs));
					else data.put(rs.getString(1), StringUtil.checkVal(rs.getString(2)));
				}
			}
		}
		
		resData.put("actionData", (all) ? allData : data);
		resData.put("isSuccess", Boolean.TRUE);
		resData.put("count", (all) ? allData.size() : data.size());
		resData.put("jsonActionError", "");	

		return resData;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {

		try {
			if (req.getBooleanParameter("toggle")) {
				toggleAttribute(req);
			} else {
				saveAttributes(req);
			}
		} catch(Exception e) {
			putModuleData(null, 0, false, e.getLocalizedMessage(), true);
			log.error("Unable to update location attributes", e);
		}
	}
	
	/**
	 * Toggles the value for the amenities
	 * @param req
	 * @throws SQLException
	 */
	public void toggleAttribute(ActionRequest req) throws SQLException {
		LocationAttributeVO lavo = new LocationAttributeVO(req);
		
		String mlid = req.getParameter("memberLocationId");
		String groupCode = req.getParameter("groupCode");
		deleteRows(mlid, groupCode, lavo.getAttributeCode());
		
		try {
			DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
			db.insert(lavo);
			log.info("Inserted: " + lavo);
		} catch (Exception e) {
			log.error("Failed", e);
			throw new SQLException("Unable to toggle active flag for amenties", e);
		}
	}
	
	/**
	 * Updates the attributes passed into the method. Assumes the data will be 
	 * updated by group code
	 * @param req
	 * @throws SQLException
	 * @throws DatabaseException
	 */
	public void saveAttributes(ActionRequest req) throws SQLException, DatabaseException {
		String mlid = req.getParameter("memberLocationId");
		String groupCode = req.getParameter("groupCode");
		deleteRows(mlid, groupCode, null);
		
		List<LocationAttributeVO> attrs = new ArrayList<>();
		for (String key : req.getParameterMap().keySet()) {
			if (! key.startsWith("attr_")) continue;
			
			LocationAttributeVO lavo = new LocationAttributeVO();
			lavo.setValue(req.getParameter(key));
			lavo.setAttributeCode(key);
			lavo.setMemberLocationId(mlid);
			attrs.add(lavo);
		}
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.executeBatch(attrs, true);
	}
	
	/**
	 * Removes the attributes for a given group
	 * @param mlid
	 * @param groupCode
	 * @throws SQLException
	 */
	private void deleteRows(String mlid, String groupCode, String attrCode ) throws SQLException {
		StringBuilder sql = new StringBuilder(196);
		sql.append("delete from ").append(getCustomSchema()).append("rp_location_attribute_xr ");
		sql.append("where member_location_id = ? and attribute_cd in ( ");
		sql.append("select attribute_cd from ").append(getCustomSchema()).append("rp_attribute ");
		sql.append("where group_cd = ? ");
		if (!StringUtil.isEmpty(attrCode)) sql.append("and attribute_cd = ? ");
		sql.append(" ) ");
		log.debug(sql.length() + "|" + sql + "|" + mlid + "|" + groupCode + "|" + attrCode);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, mlid);
			ps.setString(2, groupCode);
			if (!StringUtil.isEmpty(attrCode)) ps.setString(3, attrCode);
			ps.executeUpdate();
		}
	}
}
