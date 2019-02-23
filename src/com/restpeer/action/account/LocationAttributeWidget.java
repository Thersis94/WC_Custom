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
import com.restpeer.data.AttributeVO.GroupCode;
import com.restpeer.data.LocationAttributeVO;
import com.siliconmtn.action.ActionException;

// SMT Base Libs
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;

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
		try {
			setModuleData(getLocationAttributes(req.getParameter("memberLocationId")));
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
	public Map<String,List<LocationAttributeVO>> getLocationAttributes(String mlid) throws SQLException {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select a.*, b.location_attribute_id, member_location_id, value_txt from ");
		sql.append(getCustomSchema()).append("rp_attribute a");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("rp_location_attribute_xr b ");
		sql.append("on a.attribute_cd = b.attribute_cd and member_location_id = ? ");
		sql.append("order by group_cd, attribute_nm ");
		log.info(sql.length() + "|" + sql + "|" + mlid);
		
		// Get the list of group codes to segment the data in the map
		Map<String,List<LocationAttributeVO>> data = new LinkedHashMap<>();
		for (GroupCode gc : GroupCode.values()) {
			data.put(gc.toString(), new ArrayList<LocationAttributeVO>());
		}
		
		// Loop the data and add to the map
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, mlid);
			try (ResultSet rs = ps.executeQuery()) {
				while(rs.next()) {
					data.get(rs.getString("group_cd")).add(new LocationAttributeVO(rs));
				}
			}
		}
		
		return data;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		try {
			saveAttributes(req);
		} catch(Exception e) {
			setModuleData(null, 0, e.getLocalizedMessage());
			log.error("Unable to update location attributes", e);
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
		deleteRows(mlid, groupCode);
		
		List<LocationAttributeVO> attrs = new ArrayList<>();
		for (String key : req.getParameterMap().keySet()) {
			LocationAttributeVO lavo = new LocationAttributeVO();
			lavo.setValue(req.getParameter(key));
			lavo.setAttributeCode(key);
			lavo.setMemberLocationId(mlid);
			attrs.add(lavo);
		}
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.executeBatch(attrs);
	}
	
	/**
	 * Removes the attributes for a given group
	 * @param mlid
	 * @param groupCode
	 * @throws SQLException
	 */
	private void deleteRows(String mlid, String groupCode ) throws SQLException {
		StringBuilder sql = new StringBuilder(64);
		sql.append("delete from ").append(getCustomSchema()).append("rp_location_attribute_xr ");
		sql.append("where member_location_id = ? and attribute_cd in ( ");
		sql.append("select attribute_cd from ").append(getCustomSchema()).append("rp_attribute ");
		sql.append("where group_cd = ? ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, mlid);
			ps.setString(2, groupCode);
			ps.executeUpdate();
		}
	}
}
