package com.biomed.smarttrak.admin;

// JDK 1.8
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.biomed.smarttrak.admin.vo.GridVO;
import com.google.gson.Gson;
// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/********************************************************************
 * <b>Title: </b>GridChartAction.java<br/>
 * <b>Description: </b>manages the data for the biomed grids<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author james
 * @version 3.x
 * @since Feb 24, 2017
 * Last Updated:
 * 	
 *******************************************************************/
public class GridChartAction extends SBActionAdapter {
	
	private Map<String, String> sortMapper;

	/**
	 * 
	 */
	public GridChartAction() {
		super();
		sortMapper = new HashMap<>();
		sortMapper.put("title", "title_nm");
		sortMapper.put("subtitle", "subtitle_nm");
		sortMapper.put("approved", "approve_flg");
		sortMapper.put("lastUpdated", "update_dt");
	}

	/**
	 * @param actionInit
	 */
	public GridChartAction(ActionInitVO actionInit) {
		super(actionInit);
		log.info("here: ");
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		// Get the DB Schema
		String schema = getAttribute(Constants.CUSTOM_DB_SCHEMA) + "";
		String gridId = req.getParameter("gridId");
		if (StringUtil.isEmpty(gridId)) {
			retrieveList(req, schema);
		} else if (! "ADD".equalsIgnoreCase(gridId)){
			retrieveData(req, schema);
		}
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.info("Building Data From grid");
		log.info("Grid Data: " + req.getParameter("gridData"));
	}
	
	/**
	 * Retrieves a single grid with its details
	 * @param req
	 * @param schema
	 */
	public void retrieveData(ActionRequest req, String schema) {
		StringBuilder sql = new StringBuilder(164);
		sql.append("select * from ").append(schema).append("biomedgps_grid a ");
		sql.append("inner join ").append(schema).append("biomedgps_grid_detail b ");
		sql.append("on a.grid_id = b.grid_id where a.grid_id = ? ");
		sql.append("order by b.order_no");
		log.debug(sql);
		
		DBProcessor db = new DBProcessor(dbConn);
		List<Object> params = Arrays.asList(new Object[]{req.getParameter("gridId")});
		List<?> data = db.executeSelect(sql.toString(), params, new GridVO(), null);
		
		// Add the vo only.  Add a blank bean if nothing found
		putModuleData(data.size() > 0 ? data.get(0) : new GridVO());
	}
	
	/**
	 * Retrieves the data for the list of elements on the list page
	 * @param req
	 */
	public void retrieveList(ActionRequest req, String schema) {
		
		log.info("Retrieving ...");
		List<GridVO> data = new ArrayList<>(); int count = 0;
		String msg = ""; 
		boolean error = false; 
		
		try {
			data = getGridList(req, schema);
			log.info("Data Size: " + data.size());
			
			// Get the count
			count = getGridCount(req, schema);
			
		} catch(Exception e) {
			msg = e.getLocalizedMessage();
			log.error("Unable to retrieve grid data", e);
			error = true;
		}
		
		// Return the data
		putModuleData(data, count, false, msg, error);
	}
	
	/**
	 * Gets the number of records
	 * @param req
	 * @param schema
	 * @return
	 * @throws SQLException
	 */
	public int getGridCount(ActionRequest req, String schema) throws SQLException {
		int count = 0;
		String search = StringUtil.checkVal(req.getParameter("search")).toUpperCase();
		
		StringBuilder sql = new StringBuilder(100);
		sql.append("select count(*) from ").append(schema).append("biomedgps_grid ");
		if (search.length() > 0) sql.append("where upper(title_nm) like ? or upper(subtitle_nm) like ? ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			if (search.length() > 0) {
				ps.setString(1, "%" + search + "%");
				ps.setString(2, "%" + search + "%");
			}
			
			ResultSet rs = ps.executeQuery();
			rs.next();
			count = rs.getInt(1);
		}
		
		return count;
	}
	
	/**
	 * Gets the list of grids form the database.  Assumes server side pagination
	 * @param req
	 * @return
	 * @throws SQLException
	 */
	public List<GridVO> getGridList(ActionRequest req, String schema) throws SQLException {
		// Get the navigation info
		int start = Convert.formatInteger(req.getParameter("offset"),0);
		int rpp = Convert.formatInteger(req.getParameter("limit"),10);
		String sort = StringUtil.checkVal(sortMapper.get(req.getParameter("sort")), "title_nm");
		String order = StringUtil.checkVal(req.getParameter("order"), "asc");
		String search = StringUtil.checkVal(req.getParameter("search")).toUpperCase();
		
		// Build the SQL
		List<GridVO> data = new ArrayList<>();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select a.*, (select count(*) from ").append(schema).append("biomedgps_grid_detail b ");
		sql.append("where a.grid_id = b.grid_id) as total_rows ");
		sql.append("from ").append(schema).append("biomedgps_grid a ");
		if (search.length() > 0) sql.append("where upper(title_nm) like ? or upper(subtitle_nm) like ? ");
		sql.append("order by ").append(sort).append(" ").append(order);
		sql.append(" limit ? offset ? ");
		log.debug(sql.toString());
		
		// Loop the data and store
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int ctr = 1;
			if (search.length() > 0) {
				ps.setString(ctr++, "%" + search + "%");
				ps.setString(ctr++, "%" + search + "%");
			}
			
			ps.setInt(ctr++, rpp);
			ps.setInt(ctr++, start);
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				GridVO vo = new GridVO(rs);
				vo.setNumberRows(rs.getInt("total_rows"));
				data.add(vo);
			}
		}
		
		return data;
	}
}

