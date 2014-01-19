package com.depuy.events;

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
		GeocodeLocation gl = this.getGeocode(req.getParameter("city"), req.getParameter("state"));
		
		// Build the data object for the center and coordinates
		Map<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("center", new Double[]{gl.getLatitude(), gl.getLongitude()} );
		
		int period = Convert.formatInteger(req.getParameter("period"), 3);
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.MONTH, -period);
		
		try {
			// Get the point coordinates
			data.put("coordinates", getPointData(gl, req.getParameter("joint_cd"), cal.getTime()));
		} catch (SQLException sqle) {
			log.error("Unable to retrieve point data", sqle);
		}
		
		this.putModuleData(data);
	}
	
	/**
	 * Geocode the source location
	 * @param city
	 * @param state
	 * @return
	 */
	public GeocodeLocation getGeocode(String city, String state) {
		
		// Initialize the geocoder
		String geocodeClass = (String) this.getAttribute(GlobalConfig.GEOCODER_CLASS);
		AbstractGeocoder ag = GeocodeFactory.getInstance(geocodeClass);
		ag.addAttribute(AbstractGeocoder.CONNECT_URL, getAttribute(GlobalConfig.GEOCODER_URL));
		
		// Set the location
		Location l = new Location();
		l.setCity(city);
		l.setState(state);
		
		// Get the geocode
		GeocodeLocation gl = ag.geocodeLocation(l).get(0);
		return gl;
	}
	
	/**
	 * queries the database and retrieves a collection of lat/longs
	 * @return
	 */
	private List<Double[]> getPointData(GeocodeLocation gl, String joint, Date startDate) 
	throws SQLException {
		// Get the earths mean radius and search radius (in Kilometers)
		double radius = 16;

		// Get the necessary calculations for the coordinates
		GeoLocation points = GeoLocation.fromDegrees(gl.getLatitude(), gl.getLongitude());
		Map<String, GeoLocation> coords = points.boundingCoordinates(radius);
		boolean meridian180WithinDistance =
				coords.get(GeoLocation.MIN_BOUNDING_LOC).getLongitudeInRadians() >
				coords.get(GeoLocation.MAX_BOUNDING_LOC).getLongitudeInRadians();
		
		//Build the SQL Statement
		List<Double[]> pointData = new ArrayList<>();
		StringBuffer sql = new StringBuffer();
		sql.append("select latitude_no, longitude_no from profile p ");
		sql.append("inner join profile_address pa on p.profile_id = pa.profile_id ");
		sql.append("inner join data_feed.dbo.customer c on c.profile_id = p.profile_id ");
		sql.append("where product_cd = ? and attempt_dt > ? ");
		sql.append("(latitude_no >= ? AND latitude_no <= ?) AND (longitude_no >= ? ");
		sql.append((meridian180WithinDistance ? "OR" : "AND") + " longitude_no <= ?) AND ");
		sql.append("acos(sin(?) * sin(latitude_no) + cos(?) * cos(latitude_no) * cos(longitude_no - ?)) <= ?");
		log.debug("SQL: " + sql);
		
		// Add the parameters
		PreparedStatement ps = dbConn.prepareStatement(sql.toString());
		ps.setString(1, joint);
		ps.setDate(2, Convert.formatSQLDate(startDate));
		ps.setDouble(3, coords.get(GeoLocation.MIN_BOUNDING_LOC).getLatitudeInRadians());
		ps.setDouble(4, coords.get(GeoLocation.MAX_BOUNDING_LOC).getLatitudeInRadians());
		ps.setDouble(5, coords.get(GeoLocation.MIN_BOUNDING_LOC).getLongitudeInRadians());
		ps.setDouble(6, coords.get(GeoLocation.MAX_BOUNDING_LOC).getLongitudeInRadians());
		ps.setDouble(7, points.getLatitudeInRadians());
		ps.setDouble(8, points.getLatitudeInRadians());
		ps.setDouble(9, points.getLongitudeInRadians());
		ps.setDouble(10, radius / GeoLocation.EARTH_RADIUS);
		ResultSet rs = ps.executeQuery();
		
		// Loop the results and add the lat/long to the collection
		while (rs.next()) {
			pointData.add(new Double[] {rs.getDouble("latitude_no"), rs.getDouble("longitude_no")});
		}
		
		return pointData;
	}
}
