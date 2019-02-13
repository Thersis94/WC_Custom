package com.perfectstorm.action.admin;

// JDK 1.8.x
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// PS Libs
import com.perfectstorm.data.VenueVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
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

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;


/****************************************************************************
 * <b>Title</b>: VenueWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the creation of Venues in the system
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 5, 2019
 * @updates:
 ****************************************************************************/

public class VenueWidget extends SBActionAdapter {
	/**
	 * Key to access the widget through the controller
	 */
	public static final String AJAX_KEY = "venue";
	
	/**
	 * 
	 */
	public VenueWidget() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public VenueWidget(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/**
	 * @param actionInit
	 */
	public VenueWidget(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super();
		this.dbConn = dbConn;
		this.attributes = attributes;
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		
		Integer activeFlag = req.hasParameter("activeFlag") ? req.getIntegerParameter("activeFlag") : null;
		this.setModuleData(getVenues(activeFlag, new BSTableControlVO(req, VenueVO.class)));
	}
	
	/**
	 * 
	 * @param bst
	 * @param activeFlag
	 * @return
	 */
	public GridDataVO<VenueVO> getVenues(Integer activeFlag, BSTableControlVO bst) {
		// Add the params
		List<Object> vals = new ArrayList<>(); 
		StringBuilder sql = new StringBuilder(96);
		sql.append("select * from ").append(getCustomSchema()).append("ps_venue a ");
		sql.append("left outer join ( ");
		sql.append("select venue_id, count(*) as station_no ");
		sql.append("from custom.ps_venue_weather_station ");
		sql.append("group by venue_id ");
		sql.append(") as b on a.venue_id = b.venue_id ");
		sql.append("where 1=1 ");
		
		// Add the search filter
		if (bst.hasSearch()) {
			sql.append("and (lower(venue_nm) like ? or lower(venue_desc) like ?) ");
			vals.add(bst.getLikeSearch().toLowerCase());
			vals.add(bst.getLikeSearch().toLowerCase());
		}
		
		// Add the active flag filter
		if (activeFlag != null) {
			sql.append("and active_flg = ? ");
			vals.add(activeFlag);
		}
		
		sql.append(DBUtil.ORDER_BY).append(bst.getDBSortColumnName("venue_nm"));
		sql.append(" ").append(bst.getOrder("asc"));
		log.debug(sql.length() + "|" + sql + "|" + vals);
		
		// execute the sql
		DBProcessor db = new DBProcessor(dbConn);
		return db.executeSQLWithCount(sql.toString(), vals, new VenueVO(), bst);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {

		VenueVO venue = new VenueVO(req);
		try {
			if (req.getBooleanParameter("mapLocation")) {
				assignManualGeocode(venue);
			} else {
				saveVenue(venue, req.getBooleanParameter("overrideManualGeo"));
			}
			
			putModuleData(venue);
		} catch (Exception e) {
			log.error("Unable to add venue: " + venue, e);
			putModuleData(venue, 1, false, e.getLocalizedMessage(), true);
		}
	}
	
	/**
	 * Saves the venue information
	 * @param venue
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public void saveVenue(VenueVO venue, boolean override) 
	throws InvalidDataException, DatabaseException {
		boolean isNew = StringUtil.isEmpty(venue.getVenueId());
		
		// If not manually assigned or orverriden, geocode the location
		if (venue.getManualGeocodeFlag() != 1 || override) {
			AbstractGeocoder ag = GeocodeFactory.getInstance((String) attributes.get(Constants.GEOCODE_CLASS));
			ag.addAttribute(AbstractGeocoder.CONNECT_URL, attributes.get(Constants.GEOCODE_URL));
			ag.addAttribute(AbstractGeocoder.CASS_VALIDATE_FLG, Boolean.FALSE);
			GeocodeLocation gl = ag.geocodeLocation(venue).get(0);
			venue.setLatitude(gl.getLatitude());
			venue.setLongitude(gl.getLongitude());
			venue.setMatchCode(gl.getMatchCode());
			venue.setActiveFlag(1);
			venue.setManualGeocodeFlag(0);
			log.debug(venue);
		}
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.save(venue);
		
		// If the transaction is an insert, update the list of observation stations
		VenueStationWidget vsw = new VenueStationWidget(dbConn, attributes);
		vsw.updateObservationStation(venue, isNew);
	}
	
	/**
	 * 
	 * @param loc
	 * @throws DatabaseException 
	 */
	public void assignManualGeocode(VenueVO loc) throws SQLException {
		loc.setMatchCode(MatchCode.manual);

		StringBuilder sql = new StringBuilder(128);
		sql.append("update ").append(getCustomSchema()).append("ps_venue ");
		sql.append("set latitude_no=?,longitude_no=?,manual_geocode_flg=?,match_cd=? ");
		sql.append("where venue_id = ?");
		log.info(sql);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setDouble(1, loc.getLatitude());
			ps.setDouble(2, loc.getLongitude());
			ps.setInt(3, loc.getManualGeocodeFlag());
			ps.setString(4, loc.getMatchCode().toString());
			ps.setString(5, loc.getVenueId());

			ps.executeUpdate();
		}
	}
}

