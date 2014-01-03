package com.depuy.sitebuilder.compliance;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.gis.AbstractGeocoder;
import com.siliconmtn.gis.GeocodeFactory;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.gis.Location;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.NavManager;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteBuilderUtil;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>:ComplianceFacadeAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2007<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Oct 24, 2007
 ****************************************************************************/
public class ComplianceFacadeAction extends SBActionAdapter {
	private SiteBuilderUtil util = null;
	
	public ComplianceFacadeAction() {
		util = new SiteBuilderUtil();
	}

	public ComplianceFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
		util = new SiteBuilderUtil();
	}

	@Override
	public void list(SMTServletRequest req) throws ActionException {
		// Get the search params
		String lastName = StringUtil.checkVal(req.getParameter("lastName"));
		String state = StringUtil.checkVal(req.getParameter("state"));
		String company = StringUtil.checkVal(req.getParameter("company"));
		log.debug("Comapny: " + company);
		
		// Set the paging information
		NavManager nav = new NavManager();
		nav.setRpp(Convert.formatInteger(req.getParameter("rpp"), 10));
		nav.setCurrentPage(Convert.formatInteger(req.getParameter("page"), 1));
		StringBuffer baseUrl = new StringBuffer("?facadeType=true&cPage=facade");
		baseUrl.append("&manMod=true&actionId=").append(req.getParameter("actionId"));;
		baseUrl.append("&lastName=").append(lastName);
		baseUrl.append("&state=").append(state);
		baseUrl.append("&company=").append(company);
		baseUrl.append("&actionName=").append(req.getParameter("actionName"));
		baseUrl.append("&sbActionId=").append(req.getParameter("sbActionId"));
		baseUrl.append("&organizationId=").append(req.getParameter("organizationId"));
		nav.setBaseUrl(baseUrl.toString());
		
		// Build the core sql statement
		StringBuffer sql = new StringBuffer();
		sql.append("select * from compliance_info where 1=1 ");
		
		// Build the filters
		if (lastName.length() > 0) sql.append("and last_nm like ? ");
		if (company.length() > 0) sql.append("and company_nm like ? ");
		if (state.length() > 0) sql.append("and state_cd = ? ");

		// Add the order clause
		sql.append("order by last_nm, first_nm");
		log.debug("Compliance list SQL: " + sql);
		
		PreparedStatement ps = null;
		List<ComplianceVO> data = new ArrayList<ComplianceVO>();
		try {
			int ctr = 1;
			ps = dbConn.prepareStatement(sql.toString());
			if (lastName.length() > 0) ps.setString(ctr++, "%" + lastName + "%");
			if (company.length() > 0) ps.setString(ctr++, "%" + company + "%");
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
			sqle.printStackTrace();
			log.error("Error Rtrv Compliance Data", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}

        // Store the retrieved data in the ModuleVO.actionData and replace into
        // the Map
        ModuleVO mod = (ModuleVO) attributes.get(AdminConstants.ADMIN_MODULE_DATA);
        mod.setActionData(data);
        mod.setDataSize(nav.getTotalElements());
        this.setAttribute(AdminConstants.ADMIN_MODULE_DATA, mod);
        req.setAttribute("navigationManager", nav);
	}

	@Override
	public void update(SMTServletRequest req) throws ActionException {
        Object msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		StringBuffer sql = new StringBuffer();
		String complianceId = StringUtil.checkVal(req.getParameter("complianceId"));
		if (complianceId.length() == 0) {
			complianceId = new UUIDGenerator().getUUID();
			sql.append("insert into compliance_info (company_nm, last_nm, first_nm, ");
			sql.append("city_nm, state_cd, longitude_no, latitude_no, match_cd, ");
			sql.append("consulting_no, grant_no, research_no, royalty_no, create_dt, ");
			sql.append("compliance_id) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		} else {
			sql.append("update compliance_info set company_nm = ?, last_nm = ?, ");
			sql.append("first_nm = ?, city_nm = ?, state_cd = ?, longitude_no = ?,");
			sql.append("latitude_no = ?, match_cd = ?, consulting_no = ?, ");
			sql.append("grant_no = ?, research_no = ?, royalty_no = ?, ");
			sql.append("update_dt = ? where compliance_id = ?");
		}
		
		// Get the data and geocode it
		ComplianceVO vo = new ComplianceVO(req);
		String geoClass = (String)getAttribute(GlobalConfig.GEOCODER_CLASS);
		log.debug("Geo Class: " + geoClass);
		AbstractGeocoder ag = GeocodeFactory.getInstance(geoClass);
		ag.addAttribute(AbstractGeocoder.CONNECT_URL, getAttribute(GlobalConfig.GEOCODER_URL));
		Location loc = new Location(null, vo.getCityName(), vo.getStateCode(), null);
		log.debug("OK");
		GeocodeLocation gl = ag.geocodeLocation(loc).get(0);
		PreparedStatement ps = null;
		log.debug("Preparing to update: ");
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, vo.getCompanyName());
			ps.setString(2, vo.getLastName());
			ps.setString(3, vo.getFirstName());
			ps.setString(4, vo.getCityName());
			ps.setString(5, vo.getStateCode());
			ps.setDouble(6, gl.getLongitude());
			ps.setDouble(7, gl.getLatitude());
			ps.setString(8, gl.getMatchCode().toString());
			ps.setInt(9, vo.getConsultingNumber());
			ps.setInt(10, vo.getGrantNumber());
			ps.setInt(11, vo.getResearchNumber());
			ps.setInt(12, vo.getRoyaltyNumber());
			ps.setTimestamp(13, Convert.getCurrentTimestamp());
			ps.setString(14, complianceId);
			
			if (ps.executeUpdate() < 1) {
				msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			}
		} catch(SQLException sqle) {
			log.error("Error updating compliance info", sqle);
			msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
        // Redirect after the update
        util.adminRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
        StringBuffer url = new StringBuffer((String)req.getAttribute(Constants.REDIRECT_URL));
        url.append("&page=").append(req.getParameter("page"));
        url.append("&lastName=").append(req.getParameter("lastName"));
        url.append("&state=").append(req.getParameter("state"));
        url.append("&company=").append(req.getParameter("company"));
        req.setAttribute(Constants.REDIRECT_URL, url.toString());
	}

}
