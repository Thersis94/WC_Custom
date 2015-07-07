package com.ram.action.customer;

// JDK 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// RAMDataFeed
import com.ram.datafeed.data.CustomerLocationVO;

// SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.gis.AbstractGeocoder;
import com.siliconmtn.gis.GeocodeFactory;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.gis.GeocodeType;
import com.siliconmtn.gis.Location;
import com.siliconmtn.gis.MatchCode;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
// WebCrescendo 2.0
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title: </b>CustomerLocationAction.java <p/>
 * <b>Project: </b>WC_Custom <p/>
 * <b>Description: </b>
 * </p>
 * <b>Copyright: </b>Copyright (c) 2014<p/>
 * <b>Company: </b>Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0<p/>
 * @since May 20, 2014<p/>
 *<b>Changes: </b>
 * May 20, 2014: David Bargerhuff: Created class.
 ****************************************************************************/
public class CustomerLocationAction extends SBActionAdapter {
	
	/**
	 * 
	 */
	public CustomerLocationAction() {
		super(new ActionInitVO());
	}

	/**
	 * @param actionInit
	 */
	public CustomerLocationAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("CustomerLocationAction retrieve...");
		int customerId = Convert.formatInteger(req.getParameter("customerId"), 0);
		int customerLocationId = Convert.formatInteger(req.getParameter("customerLocationId"), 0);
		String customerTypeId = StringUtil.checkVal(req.getParameter("customerTypeId"));
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(320);
		sql.append("select b.* from ").append(schema);
		sql.append("ram_customer_location b ");
		sql.append("inner join ").append(schema).append("ram_customer c on b.customer_id = c.customer_id ");
		sql.append("where 1=1 ");
		
		if (customerId > 0) sql.append("and b.customer_id = ? ");
		if (customerLocationId > 0) sql.append("and b.customer_location_id = ? ");
		if (customerTypeId.length() > 0) sql.append("and c.customer_type_id = ? ");
		sql.append("order by location_nm");
		log.debug("CustomerLocation retrieve SQL: " + sql.toString() + " | " + customerId + " | " + customerLocationId);
		
		PreparedStatement ps = null;
		List<CustomerLocationVO> data = new ArrayList<>();
		int index = 1;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			
			if (customerId > 0) ps.setInt(index++, customerId);
			if (customerLocationId > 0) ps.setInt(index++, customerLocationId);
			if (customerTypeId.length() > 0) ps.setString(index++, customerTypeId);
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				data.add(new CustomerLocationVO(rs, false));
			}
		} catch (SQLException e) {
			log.error("Error retrieving RAM customer data, ", e);
		} finally {
			if (ps != null) {
				try { 	ps.close(); }
				catch (Exception e) { log.error("Error closing PreparedStatement, ", e); }
			}
		}
		
		ModuleVO modVo = (ModuleVO) attributes.get(Constants.MODULE_DATA);
        modVo.setDataSize(data.size());
        modVo.setActionData(data);
        this.setAttribute(Constants.MODULE_DATA, modVo);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("CustomerLocationAction build...");
		// instantiate a vo using the values on the request.
		CustomerLocationVO vo = new CustomerLocationVO(req);
		// geocode the location
		getGeocode(vo);

		boolean isUpdate = (vo.getCustomerLocationId() > 0);
		String msgAction = null;
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(300);
		if (isUpdate) {
			sql.append("update ").append(schema).append("RAM_CUSTOMER_LOCATION ");
			sql.append("set REGION_ID=?, LOCATION_NM=?, ADDRESS_TXT=?, ADDRESS2_TXT=?, ");
			sql.append("CITY_NM=?, STATE_CD=?, ZIP_CD=?, COUNTRY_CD=?, LATITUDE_NO=?, LONGITUDE_NO=?, ");
			sql.append("MATCH_CD=?, STOCKING_LOCATION_TXT=?, ACTIVE_FLG=?, UPDATE_DT=?, ");
			sql.append("CUSTOMER_ID=? WHERE CUSTOMER_LOCATION_ID = ?");
			msgAction = "updated";
		} else {
			// is an insert; Note: CUSTOMER_LOCATION_ID is an auto-incrementing field.
			sql.append("insert into ").append(schema).append("RAM_CUSTOMER_LOCATION ");
			sql.append("(REGION_ID, LOCATION_NM, ADDRESS_TXT, ADDRESS2_TXT, CITY_NM, ");
			sql.append("STATE_CD, ZIP_CD, COUNTRY_CD, LATITUDE_NO, LONGITUDE_NO, MATCH_CD, ");
			sql.append("STOCKING_LOCATION_TXT, ACTIVE_FLG, CREATE_DT, CUSTOMER_ID) ");
			sql.append("VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			msgAction = "inserted";			
		}
		log.debug("CustomerLocation build SQL: " + sql.toString() + "|" + vo.getCustomerLocationId());
		Object msg = null;
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			int index = 1;
			// handles insert/update
			ps.setString(index++, vo.getRegionId());
			ps.setString(index++, vo.getLocationName());
			ps.setString(index++, vo.getAddress());
			ps.setString(index++, vo.getAddress2());
			ps.setString(index++, vo.getCity());
			ps.setString(index++, vo.getState());
			ps.setString(index++, vo.getZipCode());
			ps.setString(index++, vo.getCountry());
			ps.setDouble(index++, vo.getLatitude());
			ps.setDouble(index++, vo.getLongitude());
			ps.setString(index++, vo.getMatchCode().name());
			ps.setString(index++, vo.getStockingLocation());
			ps.setInt(index++, vo.getActiveFlag());
			ps.setTimestamp(index++, Convert.getCurrentTimestamp());
			ps.setInt(index++, vo.getCustomerId());
			if (isUpdate) {
				ps.setInt(index++, vo.getCustomerLocationId());
			}
			
			ps.executeUpdate();
			msg = "You have successfully " + msgAction + " the customer location record.";
		} catch (SQLException sqle) {
			log.error("Error managing RAM customer location record, ", sqle);
			msg = sqle.getMessage();
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {log.error("Error closing PrepraredStatement, ", e);}
			}
		}
		
		
		boolean isJson = Convert.formatBoolean(StringUtil.checkVal(req.getParameter("amid")).length() > 0);
		if (isJson) {
			Map<String, Object> res = new HashMap<>(); 
			res.put("success", true);
			putModuleData(res);
		} else {
	        // Build the redirect and messages
			// Setup the redirect.
			StringBuilder url = new StringBuilder(50);
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			url.append(page.getRequestURI());
			if (msg != null) url.append("?msg=").append(msg);
			
			log.debug("CustomerLocationAction redir: " + url);
			req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
			req.setAttribute(Constants.REDIRECT_URL, url.toString());
		}
	}
	
	/**
	 * Attempts to obtain a geocode for the referenced CustomerLocationVO passed in on the argument.  If the initial
	 * matchcode of the CustomerLocationVO is 'state' or 'country', no geocode attempt is perfomed.  Otherwise geocode
	 * retrieval is delegated to the SMTGeocoder service.  After geocoding has been performed, the latitude and longitude of the
	 * CustomerLocationVO is updated unless the returned matchcode is one of 'county, 'state', 'country', or 'noMatch'.
	 * 
	 * @param cLoc
	 */
	public void getGeocode(CustomerLocationVO cLoc) {
		log.debug("getting geocode...");
		AbstractGeocoder ag = GeocodeFactory.getInstance((String)attributes.get(Constants.GEOCODE_CLASS));
		ag.addAttribute(AbstractGeocoder.CONNECT_URL, attributes.get(Constants.GEOCODE_URL));
		ag.addAttribute(AbstractGeocoder.CASS_VALIDATE_FLG, Boolean.FALSE);
		
		Location loc = null;
		loc = new Location();
		loc.setAddress(cLoc.getAddress());
		loc.setCity(cLoc.getCity());
		loc.setState(cLoc.getState());
		loc.setZipCode(cLoc.getZipCode());
		loc.setCountry(cLoc.getCountry());
		
		log.debug("Location info before geocoding: " + loc);
		
		if (loc.getGeocodeType().equals(GeocodeType.country) || loc.getGeocodeType().equals(GeocodeType.state)) {
			//gl = new GeocodeLocation(loc.getFormattedLocation());
			cLoc.setMatchCode(MatchCode.country);
			if (loc.getGeocodeType().equals(GeocodeType.state)) cLoc.setMatchCode(MatchCode.state);
		} else {
			GeocodeLocation gl = ag.geocodeLocation(loc).get(0);
			cLoc.setMatchCode(gl.getMatchCode());
			log.debug("GeocodeLocation info after geocoding: " + gl);
			// Set latitude/longitude depending upon the match code
			switch (gl.getMatchCode()) {
				case noMatch:
				case country:
				case state:
				case county:
					break;
				default:
					cLoc.setLatitude(gl.getLatitude());
					cLoc.setLongitude(gl.getLongitude());
					break;
			}
		}
	}

	/**
	 * Helper method that manages inserting and updating a Customer Location XR
	 * Record in the Database.
	 * @param vo
	 * @throws SQLException
	 */
	public void modifyLocationXr(CustomerLocationVO vo) throws SQLException {
		StringBuilder sql = new StringBuilder(200);
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		boolean isInsert = false;

		//Build appropriate SQL Statement.
		if (vo.getCustomerWorkflowXrId().startsWith("ext")) {
			// is an insert
			sql.append("insert into ").append(schema);
			sql.append("RAM_CUSTOMER_WORKFLOW_XR ");
			sql.append("(CUSTOMER_LOCATION_ID, WORKFLOW_ID, CREATE_DT, ");
			sql.append("CUSTOMER_WORKFLOW_XR_ID) ");
			sql.append("values (?,?,?,?)");

			isInsert = true;
			vo.setCustomerWorkflowXrId(new UUIDGenerator().getUUID());
		} else {
			// is an update
			sql.append("update ").append(schema);
			sql.append("RAM_CUSTOMER_WORKFLOW_XR ");
			sql.append("set CUSTOMER_LOCATION_ID = ?, WORKFLOW_ID = ? ");
			sql.append("WHERE CUSTOMER_WORKFLOW_XR_ID = ?");
		}

		log.debug(sql);
		
		//Exequte Query
		int i = 1;
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			//set insert/update params.
			ps.setInt(i++, vo.getCustomerLocationId());
			ps.setString(i++, vo.getWorkflowId());
			if (isInsert) ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			ps.setString(i++, vo.getCustomerWorkflowXrId());

			ps.executeUpdate();
		} catch(Exception e) {
			log.error("Problem occured while inserting/updating a Customer Location XR Record", e);
		}
	}
}
