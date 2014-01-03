package com.depuy.events;

// JDK 1.6.0
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// SMT Base Libs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.gis.AbstractGeocoder;
import com.siliconmtn.gis.GISCoordinateManager;
import com.siliconmtn.gis.GISCoordinateVO;
import com.siliconmtn.gis.GeocodeFactory;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.gis.Location;
import com.siliconmtn.gis.USStateCoordinate;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// SB Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: DataMapAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Aug 14, 2009
 ****************************************************************************/
public class DataMapAction extends SBActionAdapter {
	private double milesToDegrees = .0070;
	private int mqConversion = 1;
	
	/**
	 * 
	 */
	public DataMapAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public DataMapAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}
	
    /* (non-Javadoc)
     * @see com.siliconmtn.action.ActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
     */
	public void retrieve(SMTServletRequest req) throws ActionException {
		String state = StringUtil.checkVal(req.getParameter("state"));
		String zip = StringUtil.checkVal(req.getParameter("zip"));
		GISCoordinateVO coord = new GISCoordinateVO();
		
		Map<Object, Object> data = null;

		if (zip.length() > 0) {
			data = this.getZipCodes(req, coord);
		} else if (state.length() == 0) {
			data = this.getStates();
			GISCoordinateManager gcm = new USStateCoordinate();
			coord = gcm.getCoordinate("KS");
			coord.setDefaultZoomLevel(coord.getDefaultZoomLevel() - 3);
			coord.setName("States");
		} else {
			data = this.getCounties(state);
			GISCoordinateManager gcm = new USStateCoordinate();
			coord = gcm.getCoordinate(state);
			coord.setName(state + " Counties");
		}
		
		// Set the data into the module container
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
        mod.setActionData(data);
        attributes.put(Constants.MODULE_DATA, mod);
        req.setAttribute("coordinates", coord);
	}
	
	
	public Map<Object, Object> getZipCodes(SMTServletRequest req, GISCoordinateVO coord) 
	throws ActionException {
		log.debug("Getting zip codes near by");
		if (StringUtil.checkVal(req.getParameter("zip")).length() == 0) return null;
		
		Integer radius = Convert.formatInteger(req.getParameter("radius"));
		
		// Geocode the location
		Location loc = new Location(req.getParameter("zip"));
		String geoClass = (String)getAttribute(GlobalConfig.GEOCODER_CLASS);
		AbstractGeocoder ag = GeocodeFactory.getInstance(geoClass);
		ag.addAttribute(AbstractGeocoder.CONNECT_URL, getAttribute(GlobalConfig.GEOCODER_URL));
		List<GeocodeLocation> geos = ag.geocodeLocation(loc);
		GeocodeLocation gl = (geos != null && geos.size() > 0) ? geos.get(0) : new GeocodeLocation();
		coord.setLatitude(gl.getLatitude());
		coord.setLongitude(gl.getLongitude());
		coord.setDefaultZoomLevel(9);
		coord.setName(gl.getState() + " Zip Codes");
		
		// Build the sql statement
        StringBuffer distance = new StringBuffer("");
        distance.append("round((sqrt(power(" + gl.getLatitude());
        distance.append("-latitude_no,2) + power(" + gl.getLongitude());
        distance.append(" - longitude_no,2)) /3.14159265)*180,1) as distance ");
		
        String sql = "select zip_cd, state_cd, " + distance + " from zip_code where 1=1 ";
		sql += getWhereClause(gl.getLatitude(), gl.getLongitude(), radius);
		sql += " order by distance";
		log.debug("Zip Finder SQL: " + sql);
		
	    PreparedStatement ps = null;
	    Map<Object, Object> data = new LinkedHashMap<Object, Object>();
	    try {
	    	ps = dbConn.prepareStatement(sql);
	    	ResultSet rs = ps.executeQuery();
	    	for (int i=0; i < 100 && rs.next(); i++) {
	    		data.put(rs.getString(2).trim() + "/zip_codes/" + rs.getString(1), rs.getString(1) + " [" + rs.getDouble(3) + "]");
	    	}
	    } catch(Exception e) {
	    	throw new ActionException("Unable to get zip list for " + req.getParameter("zip"), e);
	    } finally {
	    	try {
	    		ps.close();
	    	} catch(Exception e){}
	    }
	    
	    return data;
	}
	
	/**
	 * Retrieves a list of counties for the provided state
	 * @param state
	 * @return
	 * @throws ActionException
	 */
	public Map<Object, Object> getCounties(String state) throws ActionException {
		StringBuilder sb = new StringBuilder();
	    sb.append("select county_fips_cd, place_nm from county ");
	    sb.append("where state_cd = ? ");
	    sb.append("order by place_nm ");
	    
	    PreparedStatement ps = null;
	    Map<Object, Object> data = new LinkedHashMap<Object, Object>();
	    try {
	    	ps = dbConn.prepareStatement(sb.toString());
	    	ps.setString(1, state);
	    	ResultSet rs = ps.executeQuery();
	    	while (rs.next()) {
	    		data.put(state + "/counties/" + rs.getString(1), rs.getString(2));
	    	}
	    } catch(Exception e) {
	    	throw new ActionException("Unable to get county list for " + state, e);
	    } finally {
	    	try {
	    		ps.close();
	    	} catch(Exception e){}
	    }
	    
		return data;
	}
	
	/**
	 * Retrieves a list of states for the provided state
	 * @param state
	 * @return
	 * @throws ActionException
	 */
	public Map<Object, Object> getStates() throws ActionException {
		StringBuilder sb = new StringBuilder();
	    sb.append("select state_cd, state_nm from state ");
	    sb.append("where state_fips_cd < 60 order by state_nm ");
	    
	    PreparedStatement ps = null;
	    Map<Object, Object> data = new LinkedHashMap<Object, Object>();
	    try {
	    	ps = dbConn.prepareStatement(sb.toString());
	    	ResultSet rs = ps.executeQuery();
	    	while (rs.next()) {
	    		data.put(rs.getString(1) + "/state", rs.getString(2));
	    	}
	    } catch(Exception e) {
	    	throw new ActionException("Unable to get state list", e);
	    } finally {
	    	try {
	    		ps.close();
	    	} catch(Exception e){}
	    }
	    
		return data;
	}

	/**
	 * Builds the spatial where clause bounding box
	 * @param latitude
	 * @param longitude
	 * @param radius
	 * @return
	 */
	public String getWhereClause(double latitude, double longitude, int radius) {
		// Create the bounding box for the spatial search
		double minLat = latitude - ((double) (mqConversion * milesToDegrees) * (radius * 2));
		double minLong = longitude - ((double) (mqConversion * milesToDegrees) * (radius * 2));
		double maxLat = latitude + ((double) (mqConversion * milesToDegrees) * (radius * 2));
		double maxLong = longitude + ((double) (mqConversion * milesToDegrees) * (radius * 2));

		StringBuffer spatial = new StringBuffer(" and latitude_no > " + minLat);
        spatial.append(" and latitude_no < " + maxLat);
        spatial.append(" and longitude_no > " + minLong + " and longitude_no < " + maxLong);
        
        return spatial.toString();
	}
	
	
	@Override
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
}
