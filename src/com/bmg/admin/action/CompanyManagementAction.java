package com.bmg.admin.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.biomed.smarttrak.admin.ContentHierarchyAction;
import com.bmg.admin.vo.AllianceVO;
import com.bmg.admin.vo.CompanyAttributeTypeVO;
import com.bmg.admin.vo.CompanyAttributeVO;
import com.bmg.admin.vo.CompanyVO;
import com.bmg.admin.vo.LocationVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.PageVO;
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
	
	public static final String ACTION_TYPE = "actionType";
	
	private enum ActionType {
		COMPANY, LOCATION, ALLIANCE, COMPANYATTRIBUTE, ATTRIBUTE, SECTION
	}
	
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		ActionType type;
		if (req.hasParameter("type")) {
			type = ActionType.valueOf(req.getParameter("type"));
		} else {
			type = ActionType.COMPANY;
		}
		
		switch (type) {
			case ATTRIBUTE:
				attributeRetrieve(req);
				break;
			case COMPANYATTRIBUTE:
				companyAttributeRetrieve(req);
				break;
			case LOCATION:
				locationRetrieve(req);
				break;
			case SECTION:
				retrieveSections(req);
				break;
			case COMPANY:
				companyRetrieve(req);
				break;
			case ALLIANCE:
				allianceRetrieve(req);
				break;
		}
	}
	
	
	/**
	 * Determine how to retrieve company information and do so.
	 * @param req
	 * @throws ActionException
	 */
	private void companyRetrieve(SMTServletRequest req) throws ActionException {
		if (req.hasParameter("companyId") && ! req.hasParameter("add")) {
			retrieveCompany(req.getParameter("companyId"));
		} else if (!req.hasParameter("add")) {
			retrieveCompanies(req);
		}
	}
	
	
	/**
	 * Determine how to retrieve company information and do so.
	 * @param req
	 * @throws ActionException
	 */
	private void attributeRetrieve(SMTServletRequest req) throws ActionException {
		if (req.hasParameter("attributeId") && "attributeType".equals(req.getParameter("add"))) {
			retrieveCompanyAttribute(req.getParameter("attributeId"));
		} else if (req.hasParameter("attributeId")) {
			retrieveAttributes(req);	
		}
	}
	
	
	/**
	 * Determine how to retrieve company information and do so.
	 * @param req
	 * @throws ActionException
	 */
	private void companyAttributeRetrieve(SMTServletRequest req) throws ActionException {
		if (req.hasParameter("companyAttributeId"))
			retrieveAttribute(req);
	}
	
	
	/**
	 * Determine how to retrieve company information and do so.
	 * @param req
	 * @throws ActionException
	 */
	private void locationRetrieve(SMTServletRequest req) throws ActionException {
		if (req.hasParameter("locationId"))
			retrieveLocation(req.getParameter("locationId"));
	}
	
	
	/**
	 * Determine how to retrieve company information and do so.
	 * @param req
	 * @throws ActionException
	 */
	private void allianceRetrieve(SMTServletRequest req) throws ActionException {
		if (req.hasParameter("allianceId"))
			retrieveAlliance(req.getParameter("allianceId"));
	}
	
	
	/**
	 * Get all sections available to companies and mark the active sections
	 * @param req
	 * @throws ActionException
	 */
	private void retrieveSections(SMTServletRequest req) throws ActionException {
		ContentHierarchyAction c = new ContentHierarchyAction();
		c.setActionInit(actionInit);
		c.setAttributes(attributes);
		c.setDBConnection(dbConn);
		
		List<Node> hierarchy = new Tree(c.getHierarchy(null)).preorderList();
		List<String> activeNodes = getActiveSections(req.getParameter("companyId"));
		
		// Loop over all sections and set the leaf property to 
		// signify it being in use by the current company.
		for (Node n : hierarchy) {
			if (activeNodes.contains(n.getNodeId())) {
				n.setLeaf(true);
			} else {
				n.setLeaf(false);
			}
		}
		super.putModuleData(hierarchy);
	}


	/**
	 * Gets all sections that have been assigned to the supplied company
	 * @param companyId
	 * @return
	 * @throws ActionException
	 */
	private List<String> getActiveSections(String companyId) throws ActionException {
		StringBuilder sql = new StringBuilder(150);
		sql.append("SELECT SECTION_ID FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_COMPANY_SECTION_XR WHERE COMPANY_ID = ? ");
		
		List<String> activeSections = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, companyId);
			
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				activeSections.add(rs.getString("SECTION_ID"));
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}
		
		return activeSections;
		
	}


	/**
	 * Retrieve all information pertaining to a particular alliance
	 * @param allianceId
	 */
	private void retrieveAlliance(String allianceId) {
		StringBuilder sql = new StringBuilder(100);
		sql.append("SELECT * FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("BIOMEDGPS_COMPANY_ALLIANCE_XR ");
		sql.append("WHERE COMPANY_ALLIANCE_XR_ID = ? ");
		
		List<Object> params = new ArrayList<>();
		params.add(allianceId);
		DBProcessor db = new DBProcessor(dbConn);
		AllianceVO alliance = (AllianceVO) db.executeSelect(sql.toString(), params, new AllianceVO()).get(0);
		super.putModuleData(alliance);
	}


	/**
	 *Retrieve all attributes available to the company.
	 * @param req
	 */
	private void retrieveAttributes(SMTServletRequest req) {
		StringBuilder sql = new StringBuilder(100);
		List<Object> params = new ArrayList<>();
		sql.append("SELECT * FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("BIOMEDGPS_COMPANY_ATTRIBUTE ");
		if (req.hasParameter("searchData")) {
			sql.append("WHERE lower(ATTRIBUTE_NM) like ? ");
			params.add("%" + req.getParameter("searchData").toLowerCase() + "%");
		}
		if (req.hasParameter("attributeTypeName")) {
			sql.append("WHERE TYPE_NM = ? ");
			params.add(req.getParameter("attributeTypeName"));
		}
		
		sql.append("ORDER BY DISPLAY_ORDER_NO ");

		DBProcessor db = new DBProcessor(dbConn);
		List<Object> results = db.executeSelect(sql.toString(), params, new CompanyAttributeTypeVO());
		List<Node> orderedResults = new ArrayList<>();
		for (Object o : results) {
			CompanyAttributeTypeVO attr = (CompanyAttributeTypeVO)o;
			Node n = new Node(attr.getAttributeId(), attr.getParentId());
			n.setUserObject(attr);
			orderedResults.add(n);
		}

		int rpp = Convert.formatInteger(req.getParameter("rpp"), 10);
		int page = Convert.formatInteger(req.getParameter("page"), 0);
		int end = orderedResults.size() < rpp*(page+1)? orderedResults.size() : rpp*(page+1);
		
		// If all attributes of a type is being requested set it as a request attribute since it is
		// being used to supplement the attribute xr editing.
		// Search data should not be turned into a tree after a search as requisite nodes may be missing
		if (req.hasParameter("attributeTypeName")) {
			req.getSession().setAttribute("attributeList", new Tree(orderedResults).getPreorderList());
		} else if (req.hasParameter("searchData")) {
			super.putModuleData(orderedResults.subList(rpp*page, end), orderedResults.size(), false);
		} else {
			super.putModuleData(new Tree(orderedResults).getPreorderList().subList(rpp*page, end), orderedResults.size(), false);
		}
	}


	/**
	 * Get the details of the supplied attribute type
	 * @param attributeId
	 */
	private void retrieveCompanyAttribute(String attributeId) {
		StringBuilder sql = new StringBuilder(100);
		sql.append("SELECT * FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("BIOMEDGPS_COMPANY_ATTRIBUTE ");
		sql.append("WHERE ATTRIBUTE_ID = ? ");
		
		List<Object> params = new ArrayList<>();
		params.add(attributeId);
		DBProcessor db = new DBProcessor(dbConn);
		CompanyAttributeTypeVO attr = (CompanyAttributeTypeVO) db.executeSelect(sql.toString(), params, new CompanyAttributeTypeVO()).get(0);
		super.putModuleData(attr);
	}


	/**
	 * Get the details of the supplied company attribute
	 * @param attributeId
	 */
	private void retrieveAttribute(SMTServletRequest req) {
		StringBuilder sql = new StringBuilder(100);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT * FROM ").append(customDb).append("BIOMEDGPS_COMPANY_ATTRIBUTE_XR xr ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_COMPANY_ATTRIBUTE a ");
		sql.append("ON a.ATTRIBUTE_ID = xr.ATTRIBUTE_ID ");
		sql.append("WHERE COMPANY_ATTRIBUTE_ID = ? ");
		log.debug(sql);
		List<Object> params = new ArrayList<>();
		params.add(req.getParameter("companyAttributeId"));
		DBProcessor db = new DBProcessor(dbConn);
		CompanyAttributeVO attr = (CompanyAttributeVO) db.executeSelect(sql.toString(), params, new CompanyAttributeVO()).get(0);
		super.putModuleData(attr);
		req.setParameter("attributeTypeName", attr.getAttributeTypeName());
		retrieveAttributes(req);
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
		addAttributes(company);
		addSections(company);
		
		super.putModuleData(company);
	}
	
	
	/**
	 * Get all the sections that are associated with the supplied company
	 * @param company
	 * @throws ActionException
	 */
	private void addSections(CompanyVO company) throws ActionException {
		StringBuilder sql = new StringBuilder(275);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT SECTION_NM, xr.COMPANY_SECTION_XR_ID FROM ").append(customDb).append("BIOMEDGPS_COMPANY_SECTION_XR xr ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_SECTION s ");
		sql.append("ON s.SECTION_ID = xr.SECTION_ID ");
		sql.append("WHERE COMPANY_ID = ? ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, company.getCompanyId());
			
			ResultSet rs = ps.executeQuery();
			
			while(rs.next()) {
				company.addSection(new GenericVO(rs.getString("COMPANY_SECTION_XR_ID"), rs.getString("SECTION_NM")));
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Get all attributes associated with the supplied company.
	 * @param company
	 */
	private void addAttributes(CompanyVO company) {
		StringBuilder sql = new StringBuilder(150);
		sql.append("SELECT * FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("BIOMEDGPS_COMPANY_ATTRIBUTE_XR ");
		sql.append("WHERE COMPANY_ID = ? ");
		log.debug(sql+"|"+company.getCompanyId());
		List<Object> params = new ArrayList<>();
		params.add(company.getCompanyId());
		DBProcessor db = new DBProcessor(dbConn);
		
		// DBProcessor returns a list of objects that need to be individually cast to attributes
		List<Object> results = db.executeSelect(sql.toString(), params, new CompanyAttributeVO());
		for (Object o : results) {
			company.addAttribute((CompanyAttributeVO)o);
		}
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
		ActionType action = ActionType.valueOf(req.getParameter(ACTION_TYPE));
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
			case COMPANYATTRIBUTE:
				CompanyAttributeVO attr = new CompanyAttributeVO(req);
				saveAttribute(attr, db);
				break;
			case ATTRIBUTE:
				CompanyAttributeTypeVO t = new CompanyAttributeTypeVO(req);
				saveAttributeType(t, db, Convert.formatBoolean(req.getParameter("insert")));
				break;
			case SECTION:
				saveSections(req);
				break;
		}
	}


	/**
	 * Add the supplied sections to the company xr table
	 * @param req
	 * @throws ActionException
	 */
	private void saveSections(SMTServletRequest req) throws ActionException {
		// Delete all sections currently assigned to this company before adding
		// what is on the request object.
		deleteSection(true, req.getParameter("companyId"));
		
		StringBuilder sql = new StringBuilder(225);
		sql.append("INSERT INTO ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_COMPANY_SECTION_XR (COMPANY_SECTION_XR_ID, SECTION_ID, ");
		sql.append("COMPANY_ID, CREATE_DT) ");
		sql.append("VALUES(?,?,?,?) ");
		String companyId = req.getParameter("companyId");
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (String sectionId : req.getParameterValues("sectionId")) {
				ps.setString(1, new UUIDGenerator().getUUID());
				ps.setString(2, sectionId);
				ps.setString(3, companyId);
				ps.setTimestamp(4, Convert.getCurrentTimestamp());
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Check whether the supplied attribute type needs to be inserted or updated and do so.
	 * @param attr
	 * @param db
	 * @param boolean1 
	 * @throws ActionException
	 */
	private void saveAttributeType(CompanyAttributeTypeVO t, DBProcessor db, Boolean insert) throws ActionException {
		try {
			if (insert) {
				db.insert(t);
			} else {
				db.update(t);
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Check whether the supplied attribute needs to be inserted or updated and do so.
	 * @param attr
	 * @param db
	 * @throws ActionException
	 */
	private void saveAttribute(CompanyAttributeVO attr, DBProcessor db) throws ActionException {
		try {
			if (StringUtil.isEmpty(attr.getCompanyAttributeId())) {
				attr.setCompanyAttributeId(new UUIDGenerator().getUUID());
				db.insert(attr);
			} else {
				db.update(attr);
			}
		} catch (Exception e) {
			throw new ActionException(e);
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
		ActionType action = ActionType.valueOf(req.getParameter(ACTION_TYPE));
		DBProcessor db = new DBProcessor(dbConn, (String) attributes.get(Constants.CUSTOM_DB_SCHEMA));
		try {
			log.debug(action);
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
			case COMPANYATTRIBUTE:
				CompanyAttributeVO attr = new CompanyAttributeVO(req);
				db.delete(attr);
				break;
			case ATTRIBUTE:
				CompanyAttributeTypeVO t = new CompanyAttributeTypeVO(req);
				db.delete(t);
				break;
			case SECTION:
				deleteSection(false, req.getParameter("sectionId"));
				break;
		}
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}
	
	
	/**
	 * Delete section xrs for a company. Deletes come in single xr deletion and
	 * full wipes used when new xrs are being saved.
	 * @param full
	 * @param id
	 * @throws ActionException
	 */
	private void deleteSection(boolean full, String id) throws ActionException {
		StringBuilder sql = new StringBuilder(150);
		sql.append("DELETE FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_COMPANY_SECTION_XR WHERE ");
		if (full) {
			sql.append("COMPANY_ID = ? ");
		} else {
			sql.append("COMPANY_SECTION_XR_ID = ? ");
		}
		log.debug(sql+"|"+id);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, id);
			
			ps.executeUpdate();
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
			msg = StringUtil.capitalizePhrase(buildAction) + " failed to complete successfully. Please contact an administrator for assistance";
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
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		// Redirect the user to the appropriate page
		StringBuilder url = new StringBuilder(128);
		url.append(page.getFullPath()).append("?").append("msg=").append(msg);
		
		// Only add a tab parameter if one was provided.
		if (req.hasParameter("tab")) {
			url.append("&tab=").append(req.getParameter("tab"));
		}
		
		//if a company is being deleted do not redirect the user to a company page
		if (!"delete".equals(buildAction) || 
				ActionType.valueOf(req.getParameter(ACTION_TYPE)) != ActionType.COMPANY) {
			url.append("&companyId=").append(req.getParameter("companyId"));
		}
		
		if (req.hasParameter("edit")) {
			url.append("&edit=").append(req.getParameter("edit"));
		}
		
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
	}

}
