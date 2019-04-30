package com.restpeer.action.admin;

// JDK 1.8.x
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// RP Libs
import com.restpeer.data.MemberLocationVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.gis.AbstractGeocoder;
import com.siliconmtn.gis.GeocodeFactory;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.gis.MatchCode;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: CategoryWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the categories of attributes for products
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 13, 2019
 * @updates:
 ****************************************************************************/

public class MemberLocationWidget extends SBActionAdapter {

	/**
	 * Json key to access this action
	 */
	public static final String AJAX_KEY = "memberLocation";
	
	/**
	 * 
	 */
	public MemberLocationWidget() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public MemberLocationWidget(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/**
	 * @param actionInit
	 */
	public MemberLocationWidget(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super();
		setDBConnection(dbConn);
		setAttributes(attributes);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		setModuleData(getLocations(req.getParameter("memberId")));
	}
	
	/**
	 * Get a list of categories
	 * @return
	 */
	public List<MemberLocationVO> getLocations(String memberId) {
		StringBuilder sql = new StringBuilder(96);
		sql.append("select * from ").append(getCustomSchema()).append("rp_member_location a ");
		sql.append("inner join ").append(getCustomSchema()).append("rp_member b ");
		sql.append("on a.member_id = b.member_id ");
		sql.append("where a.member_id = ? ");
		sql.append("order by location_nm");
		log.debug(sql.length() + "|" + sql);
		
		List<Object> vals = new ArrayList<>();
		vals.add(memberId);
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), vals, new MemberLocationVO());
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		MemberLocationVO loc = new MemberLocationVO(req);
		
		try {
			if (req.getBooleanParameter("mapLocation")) {
				assignManualGeocode(loc);
			} else {
				boolean override = req.getBooleanParameter("overrideManualGeo");
				saveLocation(loc, override);
			}
			
			setModuleData(loc);
		} catch (Exception e) {
			log.error("Unable to add member location:" + loc, e);
			putModuleData(loc, 0, false, e.getLocalizedMessage(), true);
		}
	}
	
	/**
	 * Inserts or updates the member location.  A geocode is performed unless
	 * the record is specified as manuall assigned
	 * @param loc
	 * @param override
	 * @throws DatabaseException
	 */
	public void saveLocation(MemberLocationVO loc, boolean override) 
	throws DatabaseException {
		
		// If not manually assigned or orverriden, geocode the location
		if (loc.getManualGeocodeFlag() != 1 || override) {
			AbstractGeocoder ag = GeocodeFactory.getInstance((String) attributes.get(Constants.GEOCODE_CLASS));
			ag.addAttribute(AbstractGeocoder.CONNECT_URL, attributes.get(Constants.GEOCODE_URL));
			ag.addAttribute(AbstractGeocoder.CASS_VALIDATE_FLG, Boolean.FALSE);
			GeocodeLocation gl = ag.geocodeLocation(loc).get(0);
			loc.setLatitude(gl.getLatitude());
			loc.setLongitude(gl.getLongitude());
			loc.setMatchCode(gl.getMatchCode());
			loc.setActiveFlag(1);
			loc.setManualGeocodeFlag(0);
		}
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			db.save(loc);
		} catch (Exception e) {
			throw new DatabaseException("Unable to save member location", e);
		}
	}
	
	/**
	 * 
	 * @param ml
	 * @throws DatabaseException 
	 */
	public void assignManualGeocode(MemberLocationVO ml) throws SQLException {
		ml.setMatchCode(MatchCode.manual);

		StringBuilder sql = new StringBuilder(128);
		sql.append("update ").append(getCustomSchema()).append("rp_member_location ");
		sql.append("set latitude_no=?,longitude_no=?,manual_geocode_flg=?,match_cd=? ");
		sql.append("where member_location_id = ?");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setDouble(1, ml.getLatitude());
			ps.setDouble(2, ml.getLongitude());
			ps.setInt(3, ml.getManualGeocodeFlag());
			ps.setString(4, ml.getMatchCode().toString());
			ps.setString(5, ml.getMemberLocationId());

			ps.executeUpdate();
		}
	}
}
