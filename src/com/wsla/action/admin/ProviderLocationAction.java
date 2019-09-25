package com.wsla.action.admin;

// SMT Base Libs
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.pool.SMTDBConnection;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.smt.sitebuilder.action.BatchImport;
// WC Libs
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.wsla.data.provider.ProviderLocationVO;
import com.wsla.data.provider.ProviderVO;

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

public class ProviderLocationAction extends BatchImport {

	/**
	 * Key for the Ajax Controller to utilize when calling this class
	 */
	public static final String AJAX_KEY = "providerLocation";

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

	/**
	 * Overloaded constructor used for calling between actions.  Note default access modifier
	 * @param attrs
	 * @param conn
	 */
	public ProviderLocationAction(Map<String, Object> attrs, SMTDBConnection conn) {
		this();
		setAttributes(attrs);
		setDBConnection(conn);
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
		if(req.hasParameter("fromProviderId") && req.getBooleanParameter("moveAllLocations")) {
			log.debug("move all locations called");
			String toProvider = StringUtil.checkVal(req.getParameter("providerId"));
			String fromProvider = StringUtil.checkVal(req.getParameter("fromProviderId"));
			transferProviderLocations(toProvider, fromProvider);
			return;
		}
		
		ProviderLocationVO loc = new ProviderLocationVO(req);
		try {
			if (req.getBooleanParameter("mapLocation")) {

				assignManualGeocode(loc);
			} else { 
				// If provider needed to be reviewed and it is approved, change the review flag to 0
				if (loc.getReviewFlag() == 1 && req.getBooleanParameter("reviewApproved")) {
					loc.setReviewFlag(0);
				}

				saveLocation(loc);
			}
		} catch (Exception e) {
			error = true;
			msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			log.error("unable to save provider location", e);
		}

		this.putModuleData("", 0, false, (String)msg, error);
	}

	/**
	 * this method takes two provider ids and moves all provider locations 
	 * @param toProvider
	 * @param fromProvider
	 */
	private void transferProviderLocations(String toProvider, String fromProvider) {
		Object msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		boolean error = false;
		
		try {
			//update the locations to the new to provider
			updateProviderLocations(toProvider, fromProvider);
		
			//delete the old from provider
			removeOutdatedProvider(fromProvider);
		
		} catch (Exception e) {
			error = true;
			msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			log.error("unable to move all provider locations", e);
			this.putModuleData("", 0, false, (String)msg, error);
		}
	}

	/**
	 * @param fromProvider
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	private void removeOutdatedProvider(String fromProvider) throws InvalidDataException, DatabaseException {

		ProviderVO pvo = new ProviderVO();
		pvo.setProviderId(fromProvider);
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.setGenerateExecutedSQL(log.isDebugEnabled());
		db.delete(pvo);
	}

	/**
	 * @param toProvider
	 * @param fromProvider
	 * @throws SQLException 
	 */
	private void updateProviderLocations(String toProvider, String fromProvider) throws SQLException {
		StringBuilder sql = new StringBuilder(128);
		sql.append("update ").append(getCustomSchema()).append("wsla_provider_location ");
		sql.append("set provider_id =? ");
		sql.append("where provider_id = ?");
		log.debug("sql " + sql.toString() + "|"+toProvider+ "|" +fromProvider);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, toProvider);
			ps.setString(2, fromProvider);
			ps.executeUpdate();
		}
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
		return getLocations(providerId,bst,false);
	}
	
	/**
	 * Retrieves a list of locations for a given provider
	 * @param providerId
	 * @param bst
	 * @return
	 */
	public GridDataVO<ProviderLocationVO> getLocations(String providerId, BSTableControlVO bst, boolean getProvider) {
		String schema = getCustomSchema();
		String orderBy = null;
		StringBuilder sql = new StringBuilder(128);
		List<Object> params = new ArrayList<>();

		//if we don't have a providerId, join the provider table so we can grab the OEM names
		// getProvider forces the join in the event we have the id but not the name
		log.debug("providerId " + providerId + " getProvider " + getProvider  );
		if (StringUtil.isEmpty(providerId) || getProvider) {
			sql.append("select lcn.*, p.provider_nm from ").append(schema).append("wsla_provider_location lcn ");
			sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_provider p on lcn.provider_id=p.provider_id ");
			orderBy = "p.provider_nm, lcn.location_nm";
		} else {
			sql.append("select lcn.* from ").append(schema).append("wsla_provider_location lcn ");
			orderBy = "lcn.location_nm";
		}
		sql.append("where 1=1 ");

		// Filter by search criteria
		if (bst.hasSearch()) {
			sql.append("and (lcn.location_nm like ? or lcn.store_no like ?) ");
			params.add(bst.getLikeSearch());
			params.add(bst.getLikeSearch());
		}
		//filter by OEM
		if (!StringUtil.isEmpty(providerId)) {
			sql.append("and lcn.provider_id=? ");
			params.add(providerId);
		}

		sql.append(bst.getSQLOrderBy(orderBy,  "asc"));
		log.debug(sql);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSQLWithCount(sql.toString(), params, new ProviderLocationVO(), bst.getLimit(), bst.getOffset());
	}

	/* (non-Javadoc)
	 * @see com.wsla.action.admin.BatchImport#getBatchImportableClass()
	 */
	@Override
	protected Class<?> getBatchImportableClass() {
		return ProviderLocationVO.class ;
	}

	/*
	 * set additional values into the VOs from request params (oem, category, etc.)
	 * (non-Javadoc)
	 * @see com.wsla.action.admin.BatchImport#transposeBatchImport(com.siliconmtn.action.ActionRequest, java.util.ArrayList)
	 */
	@Override
	protected void transposeBatchImport(ActionRequest req, ArrayList<? extends Object> entries) throws ActionException {
		//set the providerId for all beans to the one passed on the request
		String provicerId = req.getParameter("providerId");
		Date dt = Calendar.getInstance().getTime();
		List <ProviderLocationVO> ivalidObjects = new ArrayList<>();

		for (Object obj : entries) {
			ProviderLocationVO vo = (ProviderLocationVO) obj;
			//getting null data rows remove there here
			if(StringUtil.isEmpty(vo.getAddress()) && StringUtil.isEmpty(vo.getLocationName())&& StringUtil.isEmpty(vo.getCity())) {
				log.error("not a valid provider location object " + vo);
				ivalidObjects.add(vo);
				continue;
			}

			vo.setProviderId(provicerId);
			vo.setCreateDate(dt);
		}
		entries.removeAll(ivalidObjects);
	}
	
	/**
	 * Gets a provider location.
	 * 
	 * @param locationId
	 * @return
	 * @throws com.siliconmtn.db.util.DatabaseException
	 */
	protected ProviderLocationVO getProviderLocation(String locationId) throws com.siliconmtn.db.util.DatabaseException {
		if(StringUtil.isEmpty(locationId))return null;
		DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
		
		// Try getting the location
		ProviderLocationVO location = new ProviderLocationVO();
		location.setLocationId(locationId);
		try {
			dbp.getByPrimaryKey(location);
		} catch (InvalidDataException e) {
			throw new com.siliconmtn.db.util.DatabaseException(e);
		}
		
		return location;
	}
}
