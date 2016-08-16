/**
 *
 */
package com.ram.action.report;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.ram.action.report.vo.LocationInventoryReport;
import com.ram.datafeed.data.CustomerLocationVO;
import com.ram.workflow.data.vo.LocationItemMasterVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.WebCrescendoReport;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title</b>: InventoryStatusReportAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Class that manages generating the Inventory Status Report
 * for a given Location and list of available locations for a given user to run
 * reports against.
 * <b>Copyright:</b> Copyright (c) 2016
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Jul 8, 2016
 ****************************************************************************/
public class LocationInventoryReportAction extends SBActionAdapter {

	public static final String REPORT_DATA = "REPORT_DATA";
	public static final String SELECT_DATA = "SELECT_DATA";
	/**
	 * 
	 */
	public LocationInventoryReportAction() {
	}


	/**
	 * @param actionInit
	 */
	public LocationInventoryReportAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	public void retrieve(SMTServletRequest req) throws ActionException {

		//Check for locationId
		String locationId = req.getParameter("locationId");
		String locationNm = req.getParameter("locationNm");

		//Get the SBUserRole
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);

		/*
		 * if locationId present, generate report
		 * else retrieve list of locations for the user
		 */
		if(StringUtil.checkVal(locationId).length() > 0) {

			//Get Par level checks.
			List<LocationItemMasterVO> data = getInventoryStatusReportList(locationId, role);

			if(req.hasParameter("generateExcel")) {
				LocationInventoryReport gr = new LocationInventoryReport(locationId, locationNm);
				gr.setData(data);

				AbstractSBReportVO rpt = new WebCrescendoReport(gr);
				rpt.setFileName(locationNm + " Inventory.xls");
				req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
				req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
				return;
			} else {
				req.setAttribute(REPORT_DATA, data);
			}
		}

		//Generate Customer List.
		List<CustomerLocationVO> cls = getCustomerLocations(role);
		req.setAttribute(SELECT_DATA, cls);
	}

	/**
	 * Helper method that returns that CustomerLocations available for a given
	 * Role.
	 * @return
	 */
	private List<CustomerLocationVO> getCustomerLocations(SBUserRole role) {
		String customerId = StringUtil.checkVal(role.getAttribute("roleAttributeKey_1"));
		List<CustomerLocationVO> cls = new ArrayList<CustomerLocationVO>();

		/*
		 * Determine which query we need to run to get locations.  These have
		 * been rate limited to only the locations with a LocationItemMaster
		 * Record to prevent excessive listing.
		 * OEM - Gathers Locations based on LocationItemMaster Records
		 * PROVIDER - Gathers Locations based on Customer Id
		 * Admin - Can see all Locations
		 */
		String sql = null;
		switch(role.getRoleLevel()) {
			case 20:
				sql = getOEMCustomerLocations();
				break;
			case 25:
				sql = getProviderCustomerLocations();
				break;
			case 100:
				sql = getCustomerLocationSql();
				break;
		}

		//Process Query if present.
		if(sql != null) {
			try(PreparedStatement ps = dbConn.prepareStatement(sql)) {
				if(role.getRoleLevel() != 100) {
					ps.setString(1, customerId);
				}
				
				ResultSet rs = ps.executeQuery();
	
				while(rs.next()) {
					cls.add(new CustomerLocationVO(rs, false));
				}
			} catch (SQLException e) {
				log.error(e);
			}
		}
		return cls;
	}


	/**
	 * Helper method that retrieves the list of LocationItemMaster Records.
	 * @param locationId
	 * @param role 
	 * @return
	 */
	private List<LocationItemMasterVO> getInventoryStatusReportList(String locationId, SBUserRole role) {

		//Get Control Variables Ready.
		String customerId = StringUtil.checkVal(role.getAttribute("roleAttributeKey_1"));
		boolean filterByOem = role.getRoleLevel() == 20;

		List<LocationItemMasterVO> items = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(getLocationItemMasterSql(filterByOem))) {
			ps.setString(1, locationId);

			//Oems should only see their products in the report.
			if(filterByOem) {
				ps.setString(2, customerId);
			}
			ResultSet rs = ps.executeQuery();

			while(rs.next()) {
				items.add(new LocationItemMasterVO(rs));
			}
		} catch (SQLException e) {
			log.error(e);
		}

		return items;
	}

	/**
	 * Build Sql query for retrieving all locations that are stocking a given OEMs
	 * Products.
	 * @return
	 */
	private String getOEMCustomerLocations() {
		StringBuilder sql = new StringBuilder(450);
		sql.append("select distinct c.* from ");
		sql.append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_LOCATION_ITEM_MASTER a ");
		sql.append("inner join ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_PRODUCT b on a.PRODUCT_ID = b.PRODUCT_ID ");
		sql.append("inner join ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_CUSTOMER_LOCATION c on a.CUSTOMER_LOCATION_ID = c.CUSTOMER_LOCATION_ID ");
		sql.append("where b.CUSTOMER_ID = ? order by LOCATION_NM");

		log.debug(sql.toString());
		return sql.toString();	
	}

	/**
	 * Build Sql query for retrieving all locations that a Provider has access to.
	 * @return
	 */
	private String getProviderCustomerLocations() {
		StringBuilder sql = new StringBuilder(450);
		sql.append("select distinct c.* from ");
		sql.append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_LOCATION_ITEM_MASTER a ");
		sql.append("inner join ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_PRODUCT b on a.PRODUCT_ID = b.PRODUCT_ID ");
		sql.append("inner join ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_CUSTOMER_LOCATION c on a.CUSTOMER_LOCATION_ID = c.CUSTOMER_LOCATION_ID ");
		sql.append("where c.CUSTOMER_ID = ? order by LOCATION_NM");
		log.debug(sql.toString());
		return sql.toString();
	}

	/**
	 * Helper method that returns all locations
	 * @return
	 */
	private String getCustomerLocationSql() {
		StringBuilder sql = new StringBuilder(450);
		sql.append("select distinct c.* from ");
		sql.append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_LOCATION_ITEM_MASTER a ");
		sql.append("inner join ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_PRODUCT b on a.PRODUCT_ID = b.PRODUCT_ID ");
		sql.append("inner join ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_CUSTOMER_LOCATION c on a.CUSTOMER_LOCATION_ID = c.CUSTOMER_LOCATION_ID ");

		log.debug(sql.toString());
		return sql.toString();
	}

	/**
	 * Helper method that returns locationItemMaster Sql Retrieval Script.
	 * @return
	 */
	private String getLocationItemMasterSql(boolean filterByOem) {
		StringBuilder sql = new StringBuilder(450);
		sql.append("select * from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_LOCATION_ITEM_MASTER a inner join ");
		sql.append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_PRODUCT b on a.PRODUCT_ID = b.PRODUCT_ID ");
		sql.append("inner join ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("RAM_CUSTOMER c on b.CUSTOMER_ID = c.CUSTOMER_ID ");
		sql.append("where a.CUSTOMER_LOCATION_ID = ? ");
		if(filterByOem) {
			sql.append(" and b.CUSTOMER_ID = ? ");
		}

		log.debug(sql.toString());
		return sql.toString();
	}
}
