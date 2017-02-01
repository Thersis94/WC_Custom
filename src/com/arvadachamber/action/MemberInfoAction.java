package com.arvadachamber.action;

// JDK 1.6.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.dealer.DealerLocatorVO;
import com.smt.sitebuilder.action.gis.MapAction;
import com.smt.sitebuilder.action.gis.MapLocationVO;
import com.smt.sitebuilder.action.gis.MapVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: MemberInfoAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Mar 27, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class MemberInfoAction extends SBActionAdapter {
	private int numberResults = 0;;
	
	/**
	 * 
	 */
	public MemberInfoAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public MemberInfoAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(ActionRequest req) throws ActionException {
		super.retrieve(req);
		if (! Convert.formatBoolean(req.getParameter("formSubmitted"))) return;
		int mId = Convert.formatInteger(req.getParameter("memberInfoId"));
		
		// Navigation info
		int rpp = Convert.formatInteger(req.getParameter("rpp"), 10);
		int page = Convert.formatInteger(req.getParameter("cp"));
		int start = page * rpp;
		int end = (page + 1) * rpp;
		
		// Get the data
		try {
			// get the data for the locator results or the individual member
			List<MemberInfoVO> data = new ArrayList<MemberInfoVO>();
			if (mId == 0) data = getResults(req, start, end);
			else data = getResults(mId);
			
			// Add the results to the collection
			this.putModuleData(data, numberResults, false);
			
			// Set the map data for the results
			this.setMapResults(req, data, numberResults == 1 ? false: true);
			
			// Add the nav info
			int numPages = numberResults / rpp;
			if (numberResults % rpp > 0) numPages++;
			req.setAttribute("avchPage", page);
			req.setAttribute("avchStart", numberResults == 0 ? 0 : start + 1);
			req.setAttribute("avchEnd", numberResults < end ? numberResults : end);
			req.setAttribute("avchNumPages", numPages);
			req.setParameter("rpp", rpp + "");
			
			DealerLocatorVO locator = new DealerLocatorVO(req.getQueryString());
			locator.setCurrentPage(Convert.formatInteger(req.getParameter("cp")));
			locator.setResultCount(numberResults);
			locator.setResultPerPage(rpp);
			req.setAttribute("avchLocator", locator);
		} catch (Exception e) {
			log.error("Unable to retrieve Arvada Chamber member info", e);
		} 
	}
	
	/**
	 * Grabs the member info and the associated hot deals
	 * @param req
	 * @param memberId
	 * @return
	 * @throws SQLException
	 */
	public List<MemberInfoVO> getResults(int memberId) throws SQLException {
		// Get the custom schema DB
		String dbs = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		// Build the sql statement
		StringBuilder s = new StringBuilder();
		s.append("select * from ").append(dbs).append("arvc_member a left outer join ");
		s.append(dbs).append("arvc_hot_deal b on a.member_id = b.member_id ");
		s.append("where a.member_id = ? ");
		
		PreparedStatement ps = dbConn.prepareStatement(s.toString());
		ps.setInt(1, memberId);
		ResultSet rs = ps.executeQuery();
		List<MemberInfoVO> data = new ArrayList<MemberInfoVO>();
		MemberInfoVO vo = null;
		for (int i = 0; rs.next(); i++) {
			if (i == 0) vo = new MemberInfoVO(rs);
			else vo.addHotDeal(new HotDealVO(rs));
		}
		// Add the data elements if a member was found
		if (vo != null) {
			data.add(vo);
			numberResults = 1;
		}
		
		ps.close();
		
		return data;
	}
	
	/**
	 * Searches the database for matching business entities based upon the search criteria.  
	 * @param req
	 * @param start
	 * @param end
	 * @return
	 * @throws SQLException
	 */
	public List<MemberInfoVO> getResults(ActionRequest req, int start, int end) 
	throws SQLException {
		// Get the custom schema DB
		String dbs = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		// Build the sql statement
		StringBuilder s = new StringBuilder();
		s.append("select a.member_id,member_nm,address_txt,address2_txt,city_nm,");
		s.append("state_cd,zip_cd,member_dt,website_url,hours_txt,primary_phone_txt, ");
		s.append("toll_free_txt,latitude_no,longitude_no, count(b.member_id) as num_deals, ");
		s.append("d.category_id from ").append(dbs).append("arvc_member a left outer join ");
		s.append(dbs).append("arvc_hot_deal b on a.member_id = b.member_id ");
		s.append("left outer join ").append(dbs).append("ARVC_XR_MEMBER_CATEGORY c ");
		s.append("on a.member_id = c.member_id and primary_flg = 1 ");
		s.append("left outer join ").append(dbs).append("ARVC_CATEGORY d ");
		s.append("on c.CATEGORY_ID = d.CATEGORY_ID ");
		s.append("where 1 = 1 ");
		s.append("and a.member_status_flg = 1 ");
		
		if ("category".equalsIgnoreCase(req.getParameter("searchType"))) {
			s.append("and (c.category_id = ? or parent_id = ?) ");
		} else if ("keyword".equalsIgnoreCase(req.getParameter("searchType"))) {
			s.append("and (member_nm like ? or keywords_txt like ? or category_nm like ?) ");
		} else if ("name".equalsIgnoreCase(req.getParameter("searchType"))) {
			s.append("and (member_nm like ?) ");
		}
		
		s.append("group by a.member_id,member_nm,address_txt,address2_txt,city_nm,");
		s.append("state_cd,zip_cd,member_dt,website_url,hours_txt,primary_phone_txt,");
		s.append("toll_free_txt,latitude_no,longitude_no, d.category_id ");
		s.append("order by member_nm");
		log.debug("Member Info SQL: " + s);
		
		String keywordId = this.decodeKeyword(req.getParameter("keywordId"));
		log.debug("keywordId/decoded: " + req.getParameter("keywordId") + "/" + keywordId);
		
		int ctr = 1;
		PreparedStatement ps = dbConn.prepareStatement(s.toString());
		if ("category".equalsIgnoreCase(req.getParameter("searchType"))) {
			ps.setString(ctr++, keywordId);
			ps.setString(ctr++, keywordId);
		} else if ("keyword".equalsIgnoreCase(req.getParameter("searchType"))) {
			ps.setString(ctr++, "%" + keywordId + "%");
			ps.setString(ctr++, "%" + keywordId + "%");
			ps.setString(ctr++, "%" + keywordId + "%");
		} else if ("name".equalsIgnoreCase(req.getParameter("searchType"))) {
			ps.setString(ctr++, "%" + keywordId + "%");
		}
		
		ResultSet rs = ps.executeQuery();
		int i=0;
		List<MemberInfoVO> data = new ArrayList<MemberInfoVO>();
		for (; rs.next(); i++) {
			if (StringUtil.checkVal(req.getParameter("memberInfoId")).length() > 0 || i >= start && i < end) {
				data.add(new MemberInfoVO(rs));
			}
		}
		
		// Close the statement
		ps.close();
		
		// Store the total number of results
		numberResults = i;
		
		return data;
	}
	
	/**
	 * Replaces entity codes in the keywordId with their character equivalent.  For example,
	 * an apostrophe entity code (&#39;) is replaced with an apostrophe character.
	 * @param keywordId
	 * @return
	 */
	protected String decodeKeyword(String keywordId) {
		StringEncoder se = new StringEncoder();
		return se.decodeValue(keywordId);
	}
	
	/**
	 * Adds a map object for the dealer results page.  Best fit is turned on.
	 * @param req
	 * @param dlr
	 */
	protected void setMapResults(ActionRequest req, List<MemberInfoVO> dlrs, boolean bestFit) {
		// Setup the map info
		MapVO map = new MapVO();
		map.setMapZoomFlag(true);
		map.setBestFitFlag(bestFit);
		map.setMapHeight(300);
		map.setMapWidth(975);
		
		String base = "?cp=" + req.getParameter("cp") + "&amp;categoryId=";
		base += StringUtil.checkVal(req.getParameter("categoryId")) + "&amp;keywordId=";
		base += StringUtil.checkVal(req.getParameter("keywordId"));
		base += "&amp;rpp=" + StringUtil.checkVal(req.getParameter("rpp"), "10");
		base += "&amp;formSubmitted=true&amp;memberInfoId=";
		for (int i = 0; i < dlrs.size(); i++) {
			MapLocationVO mapLoc = dlrs.get(i).getMapLocation();
			mapLoc.setLocationUrl(base + mapLoc.getMapActionId());
			map.addLocation(mapLoc);
		}
		
		// Add the data to the request object to be processed by the map display
		req.setAttribute(MapAction.MAP_ALT_DATA, map);
	}
}
