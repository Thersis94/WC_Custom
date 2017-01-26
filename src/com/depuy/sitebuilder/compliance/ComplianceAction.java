package com.depuy.sitebuilder.compliance;

// JDK 1.6.0
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.gis.AbstractGeocoder;
import com.siliconmtn.gis.GeocodeFactory;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.gis.Location;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.NavManager;
import com.siliconmtn.util.NumberFormat;
import com.siliconmtn.util.StringUtil;

// SB Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteBuilderUtil;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>:ComplianceAction.java<p/>
 * <b>Description: Manages the compliance information</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2007<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Oct 23, 2007
 ****************************************************************************/
public class ComplianceAction extends SBActionAdapter {
    SiteBuilderUtil util = null;
    
	/**
	 * 
	 */
	public ComplianceAction() {
		util = new SiteBuilderUtil();
	}

	/**
	 * @param actionInit
	 */
	public ComplianceAction(ActionInitVO actionInit) {
		super(actionInit);
		util = new SiteBuilderUtil();
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		log.debug("FT: " + req.getParameter("facadeType"));
		if (Convert.formatBoolean(req.getParameter("facadeType"))) {
			ActionInterface aac = new ComplianceFacadeAction(actionInit);
			aac.setAttributes(attributes);
			aac.setDBConnection(dbConn);
			aac.list(req);
		} else {
			super.retrieve(req);
		}
		log.debug("finished List");
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (! Convert.formatBoolean(req.getParameter("searchSubmitted")))
			return;
		
		// Get the search params
		String firstName = StringUtil.checkVal(req.getParameter("firstName"));
		String lastName = StringUtil.checkVal(req.getParameter("lastName"));
		String state = StringUtil.checkVal(req.getParameter("state"));
		String city = StringUtil.checkVal(req.getParameter("city"));
		String company = StringUtil.checkVal(req.getParameter("company"));
		log.debug("Company: " + company);
		
		// Make sure a state and last name are passed
		if (company.length() < 2 && lastName.length() == 0) {
			req.setAttribute("message", "A single letter for last name or 2 letters for company is required ");
			return;
		}
		
		// Build the core sql statement
		StringBuffer sql = new StringBuffer();
		sql.append("select * from compliance_info where 1=1 ");
		
		// Build the filters
		if (firstName.length() > 0) sql.append("and first_nm like ? ");
		if (lastName.length() > 0) sql.append("and last_nm like ? ");
		if (company.length() > 0) sql.append("and company_nm like ? ");
		if (state.length() > 0) sql.append("and state_cd = ? ");
		
		// Get the city geocode and get the spatial filter
		if (city.length() > 0) {
			String geoClass = (String) getAttribute(GlobalConfig.GEOCODER_CLASS);
			AbstractGeocoder ag = GeocodeFactory.getInstance(geoClass);
			ag.addAttribute(AbstractGeocoder.CONNECT_URL, getAttribute(GlobalConfig.GEOCODER_URL));
			sql.append(getSpatial(ag.geocodeLocation(new Location(null, city, state, null)).get(0)));
		}
		
		// Add the order clause
		sql.append("order by last_nm, first_nm");
		log.debug("Compliance Rtrv SQL: " + sql);
		
		// Set the paging information
		NavManager nav = new NavManager();
		nav.setRpp(Convert.formatInteger(req.getParameter("rpp"), 10));
		nav.setCurrentPage(Convert.formatInteger(req.getParameter("page"), 1));
		StringBuffer baseUrl = new StringBuffer("?searchSubmitted=true");
		baseUrl.append("&rpp=").append(nav.getRpp());
		baseUrl.append("&firstName=").append(firstName);
		baseUrl.append("&lastName=").append(lastName);
		baseUrl.append("&state=").append(state);
		baseUrl.append("&city=").append(city);
		baseUrl.append("&company=").append(company);
		nav.setBaseUrl(baseUrl.toString());
		
		PreparedStatement ps = null;
		List<ComplianceVO> data = new ArrayList<ComplianceVO>();
		try {
			int ctr = 1;
			ps = dbConn.prepareStatement(sql.toString());
			if (firstName.length() > 0) ps.setString(ctr++, "%" + firstName + "%");
			if (lastName.length() > 0) ps.setString(ctr++, lastName + "%");
			if (company.length() > 0) ps.setString(ctr++, company + "%");
			if (state.length() > 0) ps.setString(ctr++, state);
			ResultSet rs = ps.executeQuery();
			
			// Load the data
			int i = 1;
			for(;rs.next(); i++) {
				if (i >= nav.getStart() && i <= nav.getEnd()) {
					data.add(new ComplianceVO(rs));
				}
			}
			nav.setTotalElements(i - 1);
			
		} catch(SQLException sqle) {
			log.error("Error Rtrv Compliance Data", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		
        // Store the retrieved data in the ModuleVO.actionData and replace into
        // the Map
		log.debug("Number of responses: " + nav.getTotalElements());
		req.setAttribute("navigationManager", nav);
        ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
        mod.setDataSize(nav.getTotalElements());
        mod.setActionData(data);
        attributes.put(Constants.MODULE_DATA, mod);
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
		log.debug("FT: " + req.getParameter("facadeType"));
		Object msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		
		if (Convert.formatBoolean(req.getParameter("facadeType"))) {
			ActionInterface aac = new ComplianceFacadeAction(actionInit);
			aac.setAttributes(attributes);
			aac.setDBConnection(dbConn);
			aac.update(req);
		} else {
			super.update(req);
		}
		
    	util.moduleRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		log.debug("FT: " + req.getParameter("facadeType"));
		Object msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		
		if (Convert.formatBoolean(req.getParameter("facadeType"))) {
			ActionInterface aac = new ComplianceFacadeAction(actionInit);
			aac.setAttributes(attributes);
			aac.setDBConnection(dbConn);
			aac.delete(req);
		} else {
			super.delete(req);
		}
		
    	util.moduleRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
	}
	
	/**
	 * Creates the sql where clause for the spatial request
	 * @param loc
	 * @return
	 */
	public String getSpatial(GeocodeLocation loc) {
		Double lat = loc.getLatitude();
		Double lng = loc.getLongitude();
		double radDegree = 25 * .014;
		StringBuffer sql = new StringBuffer();
		sql.append("and Latitude_no > ").append(NumberFormat.roundGeocode(lat - radDegree)).append(" and ");
		sql.append("Latitude_no < ").append(NumberFormat.roundGeocode(lat + radDegree)).append(" and ");
		sql.append("Longitude_no > ").append(NumberFormat.roundGeocode(lng - radDegree)).append(" and ");
		sql.append("Longitude_no < ").append(NumberFormat.roundGeocode(lng + radDegree));
		
		return sql.toString();
	}
	
	
}
