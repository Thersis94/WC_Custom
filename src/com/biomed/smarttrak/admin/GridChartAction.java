package com.biomed.smarttrak.admin;

// JDK 1.8
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// App Libs
import static com.biomed.smarttrak.action.GridDisplayAction.GRID_ID;

import com.biomed.smarttrak.admin.report.GridClipboardReport;
import com.biomed.smarttrak.admin.vo.GridDetailVO;
import com.biomed.smarttrak.admin.vo.GridVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.common.constants.ErrorCodes;

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
	// Maps the table field name to the db field name for sorting purposes
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
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		// Get the DB Schema
		String schema = getAttribute(Constants.CUSTOM_DB_SCHEMA) + "";
		String gridId = req.getParameter(GRID_ID);

		if (StringUtil.isEmpty(gridId)) {
			retrieveList(req, schema);
		} else if ("column".equalsIgnoreCase(req.getParameter("type"))) {
			getColumnList(schema, gridId);
		} else if (! "ADD".equalsIgnoreCase(gridId)){
			retrieveData(gridId, schema, false);
		}
	}

	/**
	 * Gets a list of columns and their associated ids and returns JSON 
	 * @param gridId
	 */
	public void getColumnList(String schema, String gridId) {
		// get number of columns.  Return empty list of 0
		int numCols = this.getNumberColumns(gridId, schema);
		if (numCols == 0) {
			this.putModuleData(new ArrayList<>());
			return;
		}

		// Grab the columns
		StringBuilder sql = new StringBuilder(128);
		sql.append("select * from ").append(schema).append("biomedgps_grid where grid_id = ? or slug_txt = ?");
		List<Object> params = Arrays.asList(new Object[]{gridId, gridId});
		GridVO grid = new GridVO();
		DBProcessor dbp = new DBProcessor(dbConn, schema);
		List<GenericVO> data = new ArrayList<>();

		try {
			List<Object> gridData = dbp.executeSelect(sql.toString(), params, new GridVO());
			if (! gridData.isEmpty()) grid = (GridVO) gridData.get(0);
			int i = 1;
			for (String val : grid.getSeries()) {
				val = StringUtil.checkVal(val, "* Column " + (i++));
				data.add(new GenericVO(i-1, val));
			}

			// Remove the empty columns and add the data to the bean for processing
			data = data.subList(0, numCols);
			this.putModuleData(data);

		} catch (Exception e) {
			String msg = "Unable to retrieve grid data for columns"; 
			log.error(msg, e);
			this.putModuleData(null, 0, false, msg, true);
		}

	}

	/**
	 * Calculates the number of columns that have data in at lease one row
	 * @param gridId
	 * @return
	 */
	private int getNumberColumns(String gridId, String schema) {
		int numCols = 0;

		StringBuilder sql = new StringBuilder(512);
		sql.append("select ");
		sql.append("case ");
		sql.append("when max(length(value_10_txt)) > 0 then 10 ");
		sql.append("when max(length(value_9_txt)) > 0 then 9 ");
		sql.append("when max(length(value_8_txt)) > 0 then 8 ");
		sql.append("when max(length(value_7_txt)) > 0 then 7 ");
		sql.append("when max(length(value_6_txt)) > 0 then 6 ");
		sql.append("when max(length(value_5_txt)) > 0 then 5 ");
		sql.append("when max(length(value_4_txt)) > 0 then 4 ");
		sql.append("when max(length(value_3_txt)) > 0 then 3 ");
		sql.append("when max(length(value_2_txt)) > 0 then 2 ");
		sql.append("when max(length(value_1_txt)) > 0 then 1 ");
		sql.append("else 0 ");
		sql.append("end as num_cols from ").append(schema).append("biomedgps_grid_detail a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("biomedgps_grid b on a.grid_id = b.grid_id ");
		sql.append("where b.grid_id = ? or slug_txt = ? ");
		log.debug("Count SQL: " + sql + "|" + gridId);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			ps.setString(1, gridId);
			ps.setString(2, gridId);
			ResultSet rs = ps.executeQuery();
			rs.next();
			numCols = rs.getInt("num_cols");
		} catch(Exception e) {
			log.error("Unable to retrieve the number of columns", e);
		}

		return numCols;
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		if (Convert.formatBoolean(req.getParameter("deleteLegacy"))) {
			deactivateLegacy(req.getParameter("slugText"));
		} else if(req.hasParameter("tableData")) {
			buildGridExcel(req);
		}else {
			saveGrid(req);
		}
	}


	/**
	 * Create an excel file from the supplied table data.
	 * @param req
	 */
	private void buildGridExcel(ActionRequest req) {
		GridClipboardReport rpt = new GridClipboardReport();
		rpt.setData(req.getParameter("tableData"));
		rpt.setFileName(req.getParameter("chartName")+".xls");
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, true);
		req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
	}

	/**
	 * Deactivate the legacy relation between charts.
	 * @param slug
	 * @throws ActionException
	 */
	private void deactivateLegacy(String slug) throws ActionException {
		if (StringUtil.isEmpty(slug)) return;

		StringBuilder sql = new StringBuilder(125);
		sql.append("delete from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_grid_table_map where grid_graphic_id = ? ");
		log.debug(sql+"|"+slug);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, slug);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new ActionException(e);
		}
		putModuleData("Success");
	}


	/**
	 * Save the grid on the request object.
	 * @param req
	 * @throws ActionException
	 */
	private void saveGrid(ActionRequest req) {
		GridVO grid = new GridVO(req);
		grid.setCreateDate(new Date());
		grid.setUpdateDate(new Date());

		log.debug(req.getParameter(GridVO.JSON_DATA_KEY));
		String msg = "You have successfuly saved the grid data";
		boolean error = false;
		Map<String, String> columnMatch = new HashMap<>(grid.getDetails().size());
		DBProcessor db = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));

		// For the financial dashboard, find existing grid by name
		if (StringUtil.isEmpty(grid.getGridId()) && "FINANCE_DASHBOARD".equalsIgnoreCase(grid.getGridType())) {
			grid.setGridId(findGridIdByName(grid));
		}

		try {
			// Make sure the new grid id is assigned when creating a new grid
			// otherwise use the existing
			db.save(grid);
			log.debug("Grid ID: " + grid.getGridId());

			// Delete any rows that aren't being updated
			this.deleteRows(grid);

			// Store the rows
			for(GridDetailVO detail : grid.getDetails()) {
				// If we are adding a new grid, the grid ID does not exist when parsed.  Add it
				detail.setGridId(grid.getGridId());

				// Any row that starts with BIO_ will be inserted.  We need to send the mapping back
				// so the table row ids can be updated
				String gridDetailId = StringUtil.checkVal(detail.getGridDetailId());
				if (gridDetailId.startsWith("BIO_")) detail.setGridDetailId(null);

				// Save the data.  If the data is an insert, add to the column xref
				db.save(detail);
				if (gridDetailId.startsWith("BIO_")) columnMatch.put(gridDetailId, detail.getGridDetailId());
			}
		} catch (Exception e) {
			log.error("unable to save the grid data", e);
			msg = e.getLocalizedMessage();
			error = true;
		}

		// Return the data
		Map<String, Object> response = new HashMap<>(8);
		response.put(GRID_ID, grid.getGridId());
		response.put(GlobalConfig.SUCCESS_KEY, !error);
		response.put(ErrorCodes.ERR_JSON_ACTION, msg);
		response.put(ErrorCodes.ERR_JSON_ACTION, msg);
		response.put(GlobalConfig.ACTION_DATA_KEY, columnMatch);
		response.put(GlobalConfig.ACTION_DATA_COUNT, columnMatch.size());
		putModuleData(response, 0, false, msg, error);

	}


	/**
	 * Deletes any rows not being updated
	 * @param grid
	 */
	public void deleteRows(GridVO grid) {
		// Build a collection of new ID's
		Set<String> ids = new HashSet<>();
		Set<String> delIds = new HashSet<>();
		for(GridDetailVO detail : grid.getDetails()) {
			ids.add(detail.getGridDetailId());
		}

		// Build the delete sql
		Object schema = getAttribute(Constants.CUSTOM_DB_SCHEMA);

		// Build the SQL to retrieve the list of ids
		StringBuilder lSql = new StringBuilder(128);
		lSql.append("select grid_detail_id from ").append(schema).append("biomedgps_grid_detail ");
		lSql.append("where grid_id = ? ");

		try (PreparedStatement ps = dbConn.prepareStatement(lSql.toString())) {
			ps.setString(1, grid.getGridId());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (! ids.contains(rs.getString(1))) {
					delIds.add(rs.getString(1));
				}
			}

		} catch (Exception e) {
			log.error("Unable to delete unused rows", e);
		}

		// Convert the set to a comma delimited string for delete
		if (delIds.isEmpty()) return;

		// Build the delete sql.  Since the ids are built within the 
		// Backend, no worries about SQL Injection, so params are just set
		String vals = StringUtil.getDelimitedList(delIds.toArray(new String[delIds.size()]), true, ",");
		StringBuilder sql = new StringBuilder(128);
		sql.append("delete from ").append(schema).append("biomedgps_grid_detail ");
		sql.append("where grid_detail_id in (").append(vals).append(") ");
		log.debug("Del SQL: " + sql);

		try (PreparedStatement ps1 = dbConn.prepareStatement(sql.toString())) {
			ps1.executeUpdate();
		} catch(Exception e) {
			log.error("");
		}
	}

	/**
	 * Used mainly by the financial dashboard, checks for an existing grid by name
	 * @param name
	 * @param type
	 * @return
	 */
	public String findGridIdByName(GridVO grid) {
		Object schema = getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(128);
		sql.append("select grid_id from ").append(schema).append("biomedgps_grid ");
		sql.append("where grid_type_cd = ? and title_nm = ? ");
		log.debug("Find By Name SQL: " + sql + grid.getGridType() + "|" + grid.getTitle());

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, grid.getGridType());
			ps.setString(2, grid.getTitle());
			ResultSet rs = ps.executeQuery();
			if (rs.next()) return rs.getString(1);

		} catch(SQLException sqle) {
			log.error("Ubale to retrieve grid by name", sqle);
		}

		return null;
	}

	/**
	 * Retrieves all of the data for charts on a page when multiple
	 * @param gridIds
	 * @param schema
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<GridVO> retrievePageChartData(List<Object> gridIds, String schema) {
		StringBuilder sql = new StringBuilder(164);
		sql.append("select * from ").append(schema).append("biomedgps_grid a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("biomedgps_grid_detail b ");
		sql.append("on a.grid_id = b.grid_id where a.grid_id in ( ");
		DBUtil.preparedStatmentQuestion(gridIds.size(), sql);			
		sql.append(") or slug_txt in ( ");
		DBUtil.preparedStatmentQuestion(gridIds.size(), sql);	
		sql.append(") order by a.grid_id, b.order_no ");
		log.debug(sql);

		DBProcessor db = new DBProcessor(dbConn);
		List<Object> params = new ArrayList<>(gridIds);
		params.addAll(gridIds);
		List<?> data = db.executeSelect(sql.toString(), params, new GridVO(), null);

		return (List<GridVO>)data;
	}

	/**
	 * Retrieves a single grid with its details
	 * @param req
	 * @param schema
	 */
	public void retrieveData(String gridId, String schema, boolean display) {
		StringBuilder sql = new StringBuilder(650);
		sql.append("select a.*, b.*, g2.grid_id as LEGACY_ID, g2.title_nm as LEGACY_NM from ").append(schema).append("biomedgps_grid a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("biomedgps_grid_detail b on a.grid_id = b.grid_id ");
		sql.append("left join ").append(schema).append("biomedgps_grid_table_map gtm on a.slug_txt = gtm.grid_graphic_id ");
		sql.append("left join ").append(schema).append("biomedgps_grid g2 on gtm.slug_txt = g2.slug_txt ");
		sql.append("where (a.grid_id = ? or a.slug_txt = ?) ");
		if (display) sql.append(" and (grid_detail_type_cd = 'DATA' or grid_detail_type_cd is null) ");
		sql.append("order by b.order_no");
		log.debug(sql + "|" + gridId + "|" + display);

		DBProcessor db = new DBProcessor(dbConn);
		List<Object> params = Arrays.asList(new Object[]{gridId, gridId});
		List<?> data = db.executeSelect(sql.toString(), params, new GridVO(), null);
		if (log.isDebugEnabled())
			log.debug("Data: " + data);

		// Add the vo only.  Add a blank bean if nothing found
		putModuleData(data.isEmpty() ? new GridVO() : data.get(0));
	}

	/**
	 * Retrieves the data for the list of elements on the list page
	 * @param req
	 */
	public void retrieveList(ActionRequest req, String schema) {
		List<GridVO> data = new ArrayList<>(); 
		int count = 0;
		String msg = ""; 
		boolean error = false; 

		try {
			data = getGridList(req, schema);
			log.debug("Data Size: " + data.size());

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
		int rpp = (Convert.formatInteger(req.getParameter("limit")) == 0) ? 10 : Convert.formatInteger(req.getParameter("limit"));
		String sort = StringUtil.checkVal(sortMapper.get(req.getParameter("sort")), "title_nm");
		String order = StringUtil.checkVal(req.getParameter("order"), "asc");
		String search = StringUtil.checkVal(req.getParameter("search")).toUpperCase();
		if ("update_dt".equalsIgnoreCase(sort)){
			sort = "coalesce(update_dt, create_dt)";
		}

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

