package com.bmg.admin.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.bmg.admin.vo.AllianceVO;
import com.bmg.admin.vo.CompanyVO;
import com.bmg.admin.vo.LocationVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: CompanyManagementAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Manages companies for the biomed gps site.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Jan 16, 2017<p/>
 * <b>Changes: </b>
 ****************************************************************************/

public class CompanyManagementAction extends SimpleActionAdapter {
	
	private final String ACTION_TYPE = "actionType";
	
	private enum actionType {
		COMPANY, LOCATION, ALLIANCE
	}
	
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		if (req.hasParameter("locationId")) {
			retrieveLocation(req.getParameter("locationId"));
		} else if (req.hasParameter("companyId") && ! req.hasParameter("add")) {
			retrieveCompany(req.getParameter("companyId"));
		} else if (!req.hasParameter("add")) {
			retrieveCompanies(req);
		}
	}
	
	
	/**
	 * Get the details of the supplied location
	 * @param locationId
	 */
	private void retrieveLocation(String locationId) {
		StringBuilder sql = new StringBuilder(100);
		sql.append("SELECT * FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("BIOMEDGPS_COMPANY_LOCATION ");
		sql.append("WHERE LOCATION_ID = ? ");
		
		List<Object> params = new ArrayList<>();
		params.add(locationId);
		DBProcessor db = new DBProcessor(dbConn);
		LocationVO loc = (LocationVO) db.executeSelect(sql.toString(), params, new LocationVO()).get(0);
		super.putModuleData(loc);
	}

	
	/**
	 * Retrieve all companies from the database as well as create a flag that 
	 * shows whether the company has been invested in by another company.
	 * @param req
	 * @throws ActionException
	 */
	private void retrieveCompanies(SMTServletRequest req) throws ActionException {
		List<Object> params = new ArrayList<>();
		String customDb = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(100);
		sql.append("select c.*, CASE WHEN (ci.investee_company_id  is null) THEN 0 ELSE 1 end as INVESTED_FLG ");
		sql.append("FROM ").append(customDb).append("BIOMEDGPS_COMPANY c ");
		sql.append("left join ").append(customDb).append("biomedgps_company_investor ci ");
		sql.append("on c.COMPANY_ID = ci.investee_company_id ");
		
		// If the request has search terms on it add them here
		if (req.hasParameter("searchData")) {
			sql.append("WHERE lower(COMPANY_NM) like ?");
			params.add("%" + req.getParameter("searchData").toLowerCase() + "%");
		}
		log.debug(sql);
		int rpp = Convert.formatInteger(req.getParameter("rpp"), 10);
		int page = Convert.formatInteger(req.getParameter("page"), 0);
		
		DBProcessor db = new DBProcessor(dbConn);
		List<Object> companies = db.executeSelect(sql.toString(), params, new CompanyVO());
		int end = companies.size() < rpp*(page+1)? companies.size() : rpp*(page+1);
		super.putModuleData(companies.subList(rpp*page, end), companies.size(), false);
	}

	
	/**
	 * Get all information related to the supplied company.
	 * @param companyId
	 * @throws ActionException
	 */
	private void retrieveCompany(String companyId) throws ActionException {
		CompanyVO company;
		StringBuilder sql = new StringBuilder(100);
		sql.append("SELECT * FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("BIOMEDGPS_COMPANY ");
		sql.append("WHERE COMPANY_ID = ? ");
		
		List<Object> params = new ArrayList<>();
		params.add(companyId);
		DBProcessor db = new DBProcessor(dbConn);
		company = (CompanyVO) db.executeSelect(sql.toString(), params, new CompanyVO()).get(0);

		// Get specifics on company details
		addInvestors(company);
		addLocations(company);
		addAlliances(company);
		
		super.putModuleData(company);
	}
	
	
	/**
	 * Get all companies that have invested in the supplied company and add
	 * them to the vo.
	 * @param company
	 */
	private void addInvestors(CompanyVO company) throws ActionException {
		StringBuilder sql = new StringBuilder(175);
		sql.append("SELECT INVESTOR_COMPANY_ID FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_COMPANY_INVESTOR WHERE INVESTEE_COMPANY_ID = ? ");
		log.debug(sql+"|"+company.getCompanyId());
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, company.getCompanyId());
			
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				company.addInvestor(rs.getString("INVESTOR_COMPANY_ID"));
			}
		} catch (SQLException e) {
			throw new ActionException(e);
		}
		
	}

	
	/**
	 * Get all locations supported by the supplied company and add them to the vo.
	 * @param company
	 */
	private void addLocations(CompanyVO company) {
		StringBuilder sql = new StringBuilder(150);
		sql.append("SELECT * FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("BIOMEDGPS_COMPANY_LOCATION ");
		sql.append("WHERE COMPANY_ID = ? ");
		log.debug(sql+"|"+company.getCompanyId());
		List<Object> params = new ArrayList<>();
		params.add(company.getCompanyId());
		DBProcessor db = new DBProcessor(dbConn);
		
		// DBProcessor returns a list of objects that need to be individually cast to locations
		List<Object> results = db.executeSelect(sql.toString(), params, new LocationVO());
		for (Object o : results) {
			company.addLocation((LocationVO)o);
			log.debug(o);
		}
	}
	
	
	/**
	 * Get all alliances the supplied company is in and add them to the vo
	 * @param company
	 */
	private void addAlliances(CompanyVO company) {
		StringBuilder sql = new StringBuilder(400);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT * FROM ").append(customDb).append("BIOMEDGPS_COMPANY_ALLIANCE_XR cax ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_ALLIANCE_TYPE at ");
		sql.append("ON cax.ALLIANCE_TYPE_ID = at.ALLIANCE_TYPE_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_COMPANY c ");
		sql.append("ON c.COMPANY_ID = cax.REL_COMPANY_ID ");
		sql.append("WHERE cax.COMPANY_ID = ? ");
		
		List<Object> params = new ArrayList<>();
		params.add(company.getCompanyId());
		DBProcessor db = new DBProcessor(dbConn);
		
		// DBProcessor returns a list of objects that need to be individually cast to alliances
		List<Object> results = db.executeSelect(sql.toString(), params, new AllianceVO());
		for (Object o : results) {
			company.addAlliance((AllianceVO)o);
		}
	}

	
	/**
	 * Update a company or related attribute of a company
	 * @param req
	 * @throws ActionException
	 */
	private void updateElement(SMTServletRequest req) throws ActionException {
		actionType action = actionType.valueOf(req.getParameter(ACTION_TYPE));
		DBProcessor db = new DBProcessor(dbConn, (String) attributes.get(Constants.CUSTOM_DB_SCHEMA));
		switch(action) {
			case COMPANY:
				CompanyVO c = new CompanyVO(req);
				saveCompany(c, db);
				break;
			case LOCATION:
				LocationVO l = new LocationVO(req);
				saveLocation(l, db);
				break;
			case ALLIANCE:
				AllianceVO a = new AllianceVO(req);
				saveAlliance(a, db);
				break;
		}
	}

	
	/**
	 * Check whether the supplied alliance needs to be updated or inserted and do so.
	 * @param a
	 * @param db
	 * @throws ActionException
	 */
	private void saveAlliance(AllianceVO a, DBProcessor db) throws ActionException {
		try {
			if (StringUtil.isEmpty(a.getAllianceId())) {
				a.setAllianceId(new UUIDGenerator().getUUID());
				db.insert(a);
			} else {
				db.update(a);
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}
		
	}

	
	/**
	 * Check whether the supplied location needs to be updated or inserted and do so.
	 * @param l
	 * @param db
	 * @throws ActionException
	 */
	private void saveLocation(LocationVO l, DBProcessor db) throws ActionException {
		try {
			if (StringUtil.isEmpty(l.getLocationId())) {
				l.setLocationId(new UUIDGenerator().getUUID());
				db.insert(l);
			} else {
				db.update(l);
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Check whether we need to insert or update the supplied vo and do so.
	 * Then update the investors for the company.
	 * @param c
	 * @param db
	 * @throws ActionException
	 */
	private void saveCompany(CompanyVO c, DBProcessor db) throws ActionException {
		try {
			if (StringUtil.isEmpty(c.getCompanyId())) {
				c.setCompanyId(new UUIDGenerator().getUUID());
					db.insert(c);
			} else {
				db.update(c);
			}
			updateInvestors(c);
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Update the investors of a company but deleting all current investors and
	 * adding all supplied investors as the new list.
	 * @param c
	 */
	private void updateInvestors(CompanyVO c) throws ActionException {
		deleteInvestors(c.getCompanyId());
		StringBuilder sql = new StringBuilder(225);
		sql.append("INSERT INTO ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_COMPANY_INVESTOR(INVESTOR_ID, INVESTOR_COMPANY_ID, ");
		sql.append("INVESTEE_COMPANY_ID, CREATE_DT) ");
		sql.append("VALUES(?,?,?,?)");
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (String s : c.getInvestors()) {
				int i = 1;
				ps.setString(i++, new UUIDGenerator().getUUID());
				ps.setString(i++, s);
				ps.setString(i++, c.getCompanyId());
				ps.setTimestamp(i, Convert.getCurrentTimestamp());
				
				ps.addBatch();
			}
			
			ps.executeBatch();
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}

	
	/**
	 * Delete all investors for the supplied company in order to clear the
	 * field for the complete list.
	 * @param companyId
	 */
	private void deleteInvestors(String companyId) throws ActionException {
		StringBuilder sql = new StringBuilder(150);
		sql.append("DELETE FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_COMPANY_INVESTOR WHERE INVESTEE_COMPANY_ID = ? ");
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, companyId);
			
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}

	
	/**
	 * Delete a supplied element
	 * @param req
	 * @throws ActionException
	 */
	private void deleteElement(SMTServletRequest req) throws ActionException {
		actionType action = actionType.valueOf(req.getParameter(ACTION_TYPE));
		DBProcessor db = new DBProcessor(dbConn, (String) attributes.get(Constants.CUSTOM_DB_SCHEMA));
		try {
		switch(action) {
			case COMPANY:
				CompanyVO c = new CompanyVO(req);
				db.delete(c);
				break;
			case LOCATION:
				LocationVO l = new LocationVO(req);
				db.delete(l);
				break;
			case ALLIANCE:
				AllianceVO a = new AllianceVO(req);
				db.delete(a);
				break;
		}
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}
	
	
	/**
	 * Take in front end requests and direct them to the proper delete or update method
	 */
	public void build(SMTServletRequest req) throws ActionException {
		String buildAction = req.getParameter("buildAction");
		String msg = StringUtil.capitalizePhrase(buildAction) + " completed successfully.";
		try {
			if ("update".equals(buildAction)) {
				updateElement(req);
			} else if("delete".equals(buildAction)) {
				deleteElement(req);
			}
		} catch (Exception e) {
			msg = StringUtil.capitalizePhrase(buildAction) + "failed to complete successfully. Please contact an administrator for assistance";
		}
		
		redirectRequest(msg, buildAction, req);
		
	}


	/**
	 * Build the redirect for build requests
	 * @param msg
	 * @param buildAction
	 * @param req
	 */
	private void redirectRequest(String msg, String buildAction, SMTServletRequest req) {
		// Redirect the user to the appropriate page
		StringBuilder url = new StringBuilder(128);
		url.append("/companies?").append("msg=").append(msg);
		
		// Only add a tab parameter if one was provided.
		if (req.hasParameter("tab")) {
			url.append("&tab=").append(req.getParameter("tab"));
		}
		
		//if a company is being deleted do not redirect the user to a company page
		if (!"delete".equals(buildAction) || 
				actionType.valueOf(req.getParameter(ACTION_TYPE)) != actionType.COMPANY) {
			url.append("&companyId=").append(req.getParameter("companyId"));
		}
		
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
	}

}
