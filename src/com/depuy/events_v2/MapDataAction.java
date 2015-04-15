package com.depuy.events_v2;

// JDK 1.7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;






// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.gis.AbstractGeocoder;
import com.siliconmtn.gis.GeocodeFactory;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.gis.Location;
import com.siliconmtn.gis.parser.GeoLocation;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title</b>: MapDataAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b>
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Jan 16, 2014<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class MapDataAction extends SimpleActionAdapter {

	/**
	 * 
	 */
	public MapDataAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public MapDataAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}

	public void retrieve(SMTServletRequest req) throws ActionException {
		// Get the geocode for the city center
		GeocodeLocation gl = this.getGeocode(req.getParameter("city"), req.getParameter("state"),
				req.getParameter("address_text"), req.getParameter("zip") );
		
		// Build the data object for the center and coordinates
		Map<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("center", new Double[]{gl.getLatitude(), gl.getLongitude()} );
		
		Date start;
		int period = Convert.formatInteger(req.getParameter("period"), 0);
		if (period == 0) {
			start = null;
		} else {
			Calendar cal = Calendar.getInstance();
			cal.setTime(new Date());
			cal.add(Calendar.MONTH, -period);
			start = cal.getTime();
		}
		
		try {
			// Get the point coordinates
			data.put("coordinates", getPointData(gl, req.getParameter("joint_cd"), start,
					Convert.formatDouble( req.getParameter("distance")), req.hasParameter("source") ));
		} catch (SQLException sqle) {
			log.error("Unable to retrieve point data", sqle);
		}
		
		this.putModuleData(data);
	}
	
	/**
	 * Geocode the source location
	 * @param city
	 * @param state
	 * @param address
	 * @param zipCode
	 * @return
	 */
	public GeocodeLocation getGeocode(String city, String state, String address, String zipCode) {
		// Initialize the geocoder
		String geocodeClass = (String) this.getAttribute(GlobalConfig.GEOCODER_CLASS);
		AbstractGeocoder ag = GeocodeFactory.getInstance(geocodeClass);
		ag.addAttribute(AbstractGeocoder.CONNECT_URL, getAttribute(GlobalConfig.GEOCODER_URL));
		
		// Set the location
		Location l = new Location();
		l.setCity(city);
		l.setState(state);
		l.setZipCode(zipCode); 
		
		//if an address was passed, include it
		if ( ! StringUtil.checkVal(address).isEmpty() )
			l.setAddress(address);
		
		// Get the geocode
		GeocodeLocation gl = ag.geocodeLocation(l).get(0);
		return gl;
	}
	
	/**
	 * queries the database and retrieves a collection of lat/longs
	 * @return
	 */
	private List<Double[]> getPointData(GeocodeLocation gl, String joint, Date startDate, Double meterRadius, boolean isMitek) 
	throws SQLException {
		// Get the earths mean radius and search radius (in Kilometers)
		double radius = meterRadius/1000.0;

		// Get the necessary calculations for the coordinates
		GeoLocation points = GeoLocation.fromDegrees(gl.getLatitude(), gl.getLongitude());
		Map<String, GeoLocation> coords = points.boundingCoordinates(radius);

		//Build the SQL Statement
		List<Double[]> pointData = new ArrayList<Double[]>();
		StringBuffer sql = new StringBuffer();
		sql.append("select latitude_no, longitude_no from ");
		sql.append((isMitek) ? "MITEK_SEMINARS_VIEW" : "DEPUY_SEMINARS_VIEW");
		sql.append(" where acos(sin(?) * sin(latitude_no) + cos(?) * cos(latitude_no) * cos(longitude_no - ?)) <= ? ");
		if (startDate != null) sql.append("and attempt_dt > ? ");
		sql.append("and product_cd = ? ");
		sql.append("and (latitude_no  between ? and ?)  and (longitude_no between ? and ?)");
		log.debug(sql + "|" + startDate);
		
		// Add the parameters
		int ctr = 1;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setDouble(ctr++, points.getLatitudeInRadians());
			ps.setDouble(ctr++, points.getLatitudeInRadians());
			ps.setDouble(ctr++, points.getLongitudeInRadians());
			ps.setDouble(ctr++, GeoLocation.EARTH_RADIUS / radius);
			if (startDate != null) ps.setDate(ctr++, Convert.formatSQLDate(startDate));
			ps.setString(ctr++, joint);
			ps.setDouble(ctr++, coords.get(GeoLocation.MIN_BOUNDING_LOC).getLatitudeInDegrees());
			ps.setDouble(ctr++, coords.get(GeoLocation.MAX_BOUNDING_LOC).getLatitudeInDegrees());
			ps.setDouble(ctr++, coords.get(GeoLocation.MIN_BOUNDING_LOC).getLongitudeInDegrees());
			ps.setDouble(ctr++, coords.get(GeoLocation.MAX_BOUNDING_LOC).getLongitudeInDegrees());
			ResultSet rs = ps.executeQuery();
			
			// Loop the results and add the lat/long to the collection
			while (rs.next()) {
				pointData.add(new Double[] {rs.getDouble("latitude_no"), rs.getDouble("longitude_no")});
			}
		}
		
		log.debug("data size: " + pointData.size());
		return pointData;
	}
}
