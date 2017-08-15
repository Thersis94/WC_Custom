package com.ram.action.user;

import java.sql.PreparedStatement;
import java.sql.SQLException;
// JDK 1.8
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.ram.action.or.vo.RAMSurgeonCustomerVO;

// WC Libs
import com.ram.action.or.vo.RAMSurgeonVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/********************************************************************
 * <b>Title: </b>SurgeonWidget.java<br/>
 * <b>Description: </b>Manages surgeon info for ram or module<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author James Camire
 * @version 3.x
 * @since Aug 15, 2017
 * Last Updated: 
 *******************************************************************/
public class SurgeonWidget extends SimpleActionAdapter {

	// Constants for local stuff
	private static final String SEARCH_KEY = "search";
	private static final String SURGEON_ID_KEY = "surgeonId";
	
	/**
	 * 
	 */
	public SurgeonWidget() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public SurgeonWidget(ActionInitVO actionInit) {
		super(actionInit);

	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		// Retrieve a list of surgeons or an individual
		if (! req.hasParameter("edit") && req.hasParameter("pmid")) {
			GenericVO data = getSurgeonList(req);
			putModuleData(data.getValue(), (int) data.getKey(), false);
		} else if (req.hasParameter(SURGEON_ID_KEY) || req.hasParameter("addSurgeon") ){
			RAMSurgeonVO surgeon = getSurgeonData(req.getParameter(SURGEON_ID_KEY));
			putModuleData(surgeon);
			
			// Add the list of providers
			req.setAttribute("customer-list", this.getProviderList(req));
		}

	}
	
	/**
	 * Gets a particular surgeon
	 * @param surgeonId
	 * @return
	 */
	public RAMSurgeonVO getSurgeonData(String surgeonId) {
		List<Object> params = new ArrayList<>();
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		StringBuilder sql = new StringBuilder(256);
		
		// add the select
		sql.append("select * ");
		
		// Build body
		buildSQLBody(sql);
		
		// Add the where clause
		sql.append(DBUtil.WHERE_CLAUSE).append("a.surgeon_id = ?");
		params.add(surgeonId);
		log.debug(sql + "|" + params);
		
		List<Object> data = db.executeSelect(sql.toString(), params, new RAMSurgeonVO(), "surgeon_id");
		if (data.isEmpty()) return null;
		
		return (RAMSurgeonVO)data.get(0);
	}
	
	/**
	 * Retrieves a list of surgeons and the count of total surgeons.  Returned as a generic vo with the count
	 * stored in the key value and the results in the value 
	 * @param req
	 * @return
	 */
	public GenericVO getSurgeonList(ActionRequest req) {
		List<Object> params = new ArrayList<>();
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		StringBuilder sql = new StringBuilder(256);
		StringBuilder cSql = new StringBuilder(256);
		
		// add the selects
		sql.append("select * ");
		cSql.append("select count(distinct(a.surgeon_id)) as key ");
		
		// Build body
		buildSQLBody(sql);
		buildSQLBody(cSql);
		
		// Build the sql
		buildSQLWhere(sql, req, params);
		buildSQLWhere(cSql, req, new ArrayList<Object>());
		
		// get the counts
		List<Object> count = db.executeSelect(cSql.toString(), params, new GenericVO());
		int total = Convert.formatInteger(((GenericVO)count.get(0)).getKey()+"");
		
		// Add the order and limits for the table
		buildOrderBy(sql, params, req);
		log.debug(sql.length() + "|" + sql);
		
		// get the collection
		List<Object> data = db.executeSelect(sql.toString(), params, new RAMSurgeonVO(), "surgeon_id");
		
		return new GenericVO(total, data);
	}
	
	/**
	 * Builds the order by and limits
	 * @param sql
	 * @param params
	 * @param req
	 */
	public void buildOrderBy(StringBuilder sql, List<Object> params, ActionRequest req) {
		sql.append("order by  ");
		sql.append(StringUtil.checkVal(DBUtil.getColumn(req.getParameter("sort"), new RAMSurgeonVO(), "a"), "last_nm, first_nm"));
		sql.append(" ").append(StringUtil.checkVal(req.getParameter("order"), "asc"));		
		sql.append(" limit ? offset ? ");
		params.add(Convert.formatInteger(req.getParameter("limit"), 10));
		params.add(Convert.formatInteger(req.getParameter("offset"), 0));
	}
	
	/**
	 * Builds the sql where clause
	 * @param sql
	 * @param req
	 * @param params
	 */
	public void buildSQLWhere(StringBuilder sql, ActionRequest req, List<Object> params) {
		sql.append(DBUtil.WHERE_1_CLAUSE);
		
		// Add the gender filter
		if (! StringUtil.isEmpty(req.getParameter("gender"))) {
			sql.append("and gender_cd = ? ");
			params.add(req.getParameter("gender"));
		}
		
		// Add the provider filter
		if (! StringUtil.isEmpty(req.getParameter("provider"))) {
			sql.append("and b.customer_id = ? ");
			params.add(Convert.formatInteger(req.getParameter("provider")));
		}
		
		// Add the search filter
		if (! StringUtil.isEmpty(req.getParameter(SEARCH_KEY))) {
			sql.append("and (lower(first_nm) like ? or lower(last_nm) like ? or lower(unique_id) like ? ) ");
			params.add("%" + req.getParameter(SEARCH_KEY).toLowerCase() + "%");
			params.add("%" + req.getParameter(SEARCH_KEY).toLowerCase() + "%");
			params.add("%" + req.getParameter(SEARCH_KEY).toLowerCase() + "%");
		}
	}
	
	/**
	 * builds the body of the select sql
	 * @param sql
	 */
	public void buildSQLBody(StringBuilder sql) {
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("ram_surgeon a ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("ram_surgeon_customer_xr b ");
		sql.append("on a.surgeon_id = b.surgeon_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("ram_customer c on b.customer_id = c.customer_id ");
	}
	
	/**
	 * Gets a selection list of providers for the view
	 * @param role USer role
	 * @param schema db schema
	 * @return
	 */
	public List<Object> getProviderList(ActionRequest req) {
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder(128);
		sql.append("select customer_id as key, customer_nm as value from ").append("ram_customer "); 
		sql.append("where customer_type_id = 'PROVIDER' and active_flg = 1 ");
		if (! StringUtil.isEmpty(req.getParameter(SURGEON_ID_KEY))) {
			sql.append("and customer_id not in (  select customer_id from ").append(getCustomSchema());
			sql.append("ram_surgeon_customer_xr  where surgeon_id = ? ) ");
			
			params.add(req.getParameter(SURGEON_ID_KEY));
		}
		sql.append(" order by customer_nm ");
		log.debug(sql);

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(sql.toString(), params, new GenericVO());
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		RAMSurgeonVO surgeon = new RAMSurgeonVO(req);
		String[] providers = req.getParameterValues("customerIds");
		
		try {
			saveSurgeon(surgeon, providers);
			this.putModuleData(surgeon, 0, false, null, false);
		} catch(Exception e) {
			log.error("Unable to add surgeon", e);
			this.putModuleData(null, 0, false, "Unable to save surgeon info", true);
		}
	}
	
	/**
	 * Saves the surgeon data as well as the surgeon customer mappings
	 * @param surgeon
	 * @param providers
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 * @throws SQLException
	 */
	public void saveSurgeon(RAMSurgeonVO surgeon, String[] providers) throws InvalidDataException, DatabaseException, SQLException {
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		// Save the surgeon data
		if (StringUtil.isEmpty(surgeon.getSurgeonId())) surgeon.setSurgeonId(null);
		surgeon.setCreateDate(new Date());
		db.save(surgeon);
		
		if (StringUtil.isEmpty(surgeon.getSurgeonId())) surgeon.setSurgeonId(db.getGeneratedPKId());
		
		// Delete existing customer mappings
		StringBuilder sql = new StringBuilder(128);
		sql.append(DBUtil.DELETE_CLAUSE).append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("ram_surgeon_customer_xr ");
		sql.append("where surgeon_id = ?");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, surgeon.getSurgeonId());
			ps.executeUpdate();
		}
		
		// insert the new records
		for (String provider : providers) {
			RAMSurgeonCustomerVO vo = new RAMSurgeonCustomerVO();
			vo.setSurgeonCustomerId(new UUIDGenerator().getUUID());
			vo.setSurgeonId(surgeon.getSurgeonId());
			vo.setCustomerId(Convert.formatInteger(provider));
			vo.setCreateDate(new Date());
			
			db.insert(vo);
		}
	}
}
