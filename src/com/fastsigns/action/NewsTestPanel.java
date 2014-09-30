package com.fastsigns.action;

// JDK 1.1.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// SB Libs
import com.fastsigns.action.franchise.vo.CenterModuleOptionVO;
import com.fastsigns.action.franchise.vo.FranchiseTimeVO;
import com.fastsigns.action.franchise.vo.FranchiseTimeVO.DayType;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.dealer.DealerLocationVO;
import com.smt.sitebuilder.action.dealer.DealerLocatorAction;
import com.smt.sitebuilder.action.gis.MapLocationVO;
import com.smt.sitebuilder.action.gis.MapVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: NewsTestPanel.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Dec 30, 2010<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class NewsTestPanel extends SBActionAdapter {

	/**
	 * 
	 */
	public NewsTestPanel() {
		
	}

	/**
	 * @param actionInit
	 */
	public NewsTestPanel(ActionInitVO actionInit) {
		super(actionInit);
		
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		String sbActionId = StringUtil.checkVal(req.getParameter(SB_ACTION_ID));
		String fId = StringUtil.checkVal(req.getSession().getAttribute("FranchiseId"));
		if (sbActionId.length() > 0) {
			super.retrieve(req);
		} else if (fId.length() == 0) {
			List<CenterModuleOptionVO> data = retrieveEntries();
			this.putModuleData(data, data.size(), false);
		} else {
			DealerLocationVO dlr = this.getFranchiseData(req, fId);
			this.putModuleData(dlr);
		}
	}
	
	/**
	 * Retrieves the selected new panel data
	 * @return
	 */
	public List<CenterModuleOptionVO> retrieveEntries() {
		String cdb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder s = new StringBuilder();
		s.append("select * from ").append(cdb).append("fts_cp_module_option a ");
		s.append("inner join ").append(cdb).append("fts_module_news_xr b ");
		s.append("on a.cp_module_option_id = b.cp_module_option_id ");
		s.append("where b.action_id = ? ");
		s.append("order by fts_cp_module_type_id desc, a.create_dt desc ");
		log.debug("News Panel SQL: " + s + "|" + actionInit.getActionId());
		
		PreparedStatement ps = null;
		List<CenterModuleOptionVO> data = new ArrayList<CenterModuleOptionVO>();
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(1, actionInit.getActionId());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				data.add(new CenterModuleOptionVO(rs));
			}
		} catch(Exception e) {
			log.error("Unable to retrive news panel data", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		return data;
	}
	
	/**
	 * Deletes the users original selections
	 * @param id
	 * @throws SQLException
	 */
	public void deleteEntries(String id) throws SQLException {
		String cdb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String s = "delete from " + cdb + "fts_module_news_xr where action_id = ?";
		PreparedStatement ps = dbConn.prepareStatement(s);
		ps.setString(1, id);
		ps.executeUpdate();
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void update(SMTServletRequest req) throws ActionException {
		String sbActionId = StringUtil.checkVal(req.getParameter(SB_ACTION_ID));
		String msg = AdminConstants.KEY_SUCCESS_MESSAGE;
		try {
			this.deleteEntries(sbActionId);
			this.updateEntries(req, sbActionId);
		} catch(SQLException sqle) {
			log.error("Unable to update News Panel Entries", sqle);
			msg = AdminConstants.KEY_ERROR_MESSAGE;
		}

		this.moduleRedirect(req, getAttribute(msg), (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
	}
	
	/**
	 * 
	 * @param req
	 * @param id
	 * @throws SQLException
	 */
	public void updateEntries(SMTServletRequest req, String id) throws SQLException {
		String cdb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder s = new StringBuilder();
		s.append("insert into ").append(cdb).append("fts_module_news_xr ");
		s.append("(cp_module_option_id, action_id, create_dt) values (?,?,?) ");
		PreparedStatement ps = dbConn.prepareStatement(s.toString());
		List<String> elements =  new ArrayList<String>();
		elements.add(req.getParameter("testimonialId"));
		elements.addAll(Arrays.asList(req.getParameterValues("newsId")));
		
		//elements.add(req.getParameter("testimonialId"));
		for (int i=0; i < elements.size(); i++) {
			ps.setString(1, elements.get(i));
			ps.setString(2, id);
			ps.setTimestamp(3, Convert.getCurrentTimestamp());
			ps.addBatch();
		}
		
		ps.executeBatch();
	}
	
	
	/**
	 * 
	 * @param req
	 * @param fId
	 * @return
	 */
	protected DealerLocationVO getFranchiseData(SMTServletRequest req, String fId) {
		DealerLocationVO dlv = new DealerLocationVO();
		DealerLocatorAction dla = new DealerLocatorAction();
		dla.setDBConnection(dbConn);
		try {
			List<DealerLocationVO> dlrs = dla.getDealerInfo(req, new String[] { fId }, null);
			if (dlrs.size() > 0) dlv = dlrs.get(0);
			dlv.addAttribute("fullHours", getCenterHours(dlv));
			getFranchiseAttributes(dlv);
			
			MapLocationVO f = dlv.getMapLocation();
			StringBuilder s = new StringBuilder();
			s.append("/feature/map.jsp?lat=").append(f.getLatitude()).append("&lng=");
			s.append(f.getLongitude()).append("&zoom=13");
			s.append("&saddr=").append(f.getFormattedLocation());
			f.setMarkerClickType(MapLocationVO.LIGHT_WINDOW);
			f.setLocationUrl(s.toString());
			
			MapVO map = new MapVO();
			map.setBestFitFlag(false);
			map.setMapWidth(160);
			map.setMapHeight(140);
			map.addLocation(f);
			map.setMapZoomFlag(false);
			map.setZoomLevel(14);
			req.setAttribute("mapAltData", map);
			
		} catch(Exception de) {
			log.error("Unable to retrieve dealer data", de);
		}
		
		return dlv;
	}

	/**
	 * Gets the USE_RAQSAF flag from the franchise table.
	 */
	private void getFranchiseAttributes(DealerLocationVO dlv) {
		StringBuilder sql = new StringBuilder();
		String cdb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT USE_RAQSAF FROM ").append(cdb).append("FTS_FRANCHISE ");
		sql.append("WHERE FRANCHISE_ID = ?");
		
		// If, for some reason, the dealer does not have an id we return here.
		if (dlv.getDealerId() == null) return;
		String id = dlv.getDealerId().substring(3);
		
		log.debug(sql+"|"+id);
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, id);
			
			rs = ps.executeQuery();
			if (rs.next())
				dlv.addAttribute("useRAQSAF", rs.getInt("USE_RAQSAF"));
		} catch (SQLException e) {
			log.error("Unable to retrieve franchise attributes for dealer id " + id, e);
		} finally {
			DBUtil.close(ps);
		}
		
	}

	/**
	 * Get a franchise time vo in order to get the times set in webedit
	 * @param dlv
	 * @return
	 */
	private FranchiseTimeVO getCenterHours(DealerLocationVO dlv) {
		Map<DayType, String> times = new HashMap<DayType, String>();
		DayType day;
		for (String key : dlv.getAttributes().keySet()) {
			if (key == null) continue;
			day = DayType.valueOf(key);
			if (day != null) {
				times.put(day, StringUtil.checkVal(dlv.getAttributes().get(key)));
			}
		}
		return new FranchiseTimeVO(times);
	}
}
