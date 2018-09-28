package com.wsla.action.admin;

// SMT Base Libs
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.gis.AbstractGeocoder;
import com.siliconmtn.gis.GeocodeFactory;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.gis.MatchCode;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.action.ActionException;

import java.sql.PreparedStatement;
import java.sql.SQLException;
// JDK 1.8.x
import java.util.ArrayList;
import java.util.List;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.wsla.data.provider.ProviderLocationVO;

/****************************************************************************
 * <b>Title</b>: ProviderLocationAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages Provider Locations for a given provider
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 22, 2018
 * @updates:
 ****************************************************************************/

public class ProviderLocationAction extends SBActionAdapter {

	/**
	 * 
	 */
	public ProviderLocationAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public ProviderLocationAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		BSTableControlVO bst = new BSTableControlVO(req, ProviderLocationVO.class);
		setModuleData(getLocations(req.getParameter("providerId"), bst));
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		Object msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		boolean error = false;
		try {
			if (req.getBooleanParameter("mapLocation")) {
				ProviderLocationVO loc = new ProviderLocationVO(req);
				assignManualGeocode(loc);
			} else { 
				saveLocation(new ProviderLocationVO(req));
			}
		} catch (Exception e) {
			error = true;
			msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			log.error("unable to save provider location", e);
		}
		
		this.putModuleData("", 0, false, (String)msg, error);
	}
	
	/**
	 * 
	 * @param loc
	 * @throws DatabaseException 
	 */
	public void assignManualGeocode(ProviderLocationVO loc) throws SQLException {
		loc.setMatchCode(MatchCode.manual);
		
		StringBuilder sql = new StringBuilder(128);
		sql.append("update ").append(getCustomSchema()).append("wsla_provider_location ");
		sql.append("set latitude_no=?,longitude_no=?,manual_geocode_flg=?,match_cd=? ");
		sql.append("where location_id = ?");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setDouble(1, loc.getLatitude());
			ps.setDouble(2, loc.getLongitude());
			ps.setInt(3, loc.getManualGeocodeFlag());
			ps.setString(4, loc.getMatchCode().toString());
			ps.setString(5, loc.getLocationId());
			
			ps.executeUpdate();
		}
	}
	
	/**
	 * Saves a provider location.  Geocodes the address if manual geocode is 0
	 * @param loc
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public void saveLocation(ProviderLocationVO loc) throws InvalidDataException, DatabaseException {
		// If not manually assigned, geocode the location
		if (loc.getManualGeocodeFlag() != 1) {
			AbstractGeocoder ag = GeocodeFactory.getInstance((String) attributes.get(Constants.GEOCODE_CLASS));
			ag.addAttribute(AbstractGeocoder.CONNECT_URL, attributes.get(Constants.GEOCODE_URL));
			ag.addAttribute(AbstractGeocoder.CASS_VALIDATE_FLG, Boolean.FALSE);
			GeocodeLocation gl = ag.geocodeLocation(loc).get(0);
			loc.setLatitude(gl.getLatitude());
			loc.setLongitude(gl.getLongitude());
			loc.setMatchCode(gl.getMatchCode());
		}
		
		// Check if default location, remove default flag from all other locations for that provider
		if (loc.getDefaultFlag() == 1) resetDefaultLocation(loc.getProviderId());
		
		// Update the table data
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.save(loc);
	}
	
	/**
	 * When a location has been assigned as the default, update the other locations
	 * for that provider to not default
	 * @param providerId
	 */
	public void resetDefaultLocation(String providerId) {
		
		StringBuilder sql = new StringBuilder(64);
		sql.append("update ").append(getCustomSchema()).append("wsla_provider_location ");
		sql.append("set default_flg = 0 where provider_id = ");
		sql.append(StringUtil.checkVal(providerId, true));
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.executeSQLCommand(sql.toString());
	}
	
	/**
	 * Retrieves a list of locations for a given provider
	 * @param providerId
	 * @param bst
	 * @return
	 */
	public GridDataVO<ProviderLocationVO> getLocations(String providerId, BSTableControlVO bst) {
		List<Object> params = new ArrayList<>();
		params.add(providerId);
		
		StringBuilder sql = new StringBuilder(128);
		sql.append("select * from ").append(getCustomSchema()).append("wsla_provider_location ");
		sql.append("where provider_id = ? ");
		
		// Filter by search criteria
		if (bst.hasSearch()) {
			sql.append("and (location_nm like ? or store_no like ?) ");
			params.add(bst.getLikeSearch());
			params.add(bst.getLikeSearch());
		}
		
		sql.append(bst.getSQLOrderBy("location_nm",  "asc"));
		DBProcessor db = new DBProcessor(getDBConnection());
		
		return db.executeSQLWithCount(sql.toString(), params, new ProviderLocationVO(), bst.getLimit(), bst.getOffset());
	}
}

