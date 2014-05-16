package com.fastsigns.action.franchise.centerpage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import com.fastsigns.action.franchise.CenterPageAction;
import com.fastsigns.action.franchise.vo.FranchiseTimeVO;
import com.fastsigns.action.franchise.vo.FranchiseVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.dealer.DealerInfoAction;
import com.smt.sitebuilder.action.gis.MapLocationVO;
import com.smt.sitebuilder.action.gis.MapVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: FranchiseLocationInfoAction <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> This class handles all the location related calls for 
 * CenterPageAction.  
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Billy Larsen
 * @version 1.0
 * @since Feb. 11, 2013<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class FranchiseLocationInfoAction extends SBActionAdapter {
	public static final int LOCATION_UPDATE = 1;
	
	public FranchiseLocationInfoAction(ActionInitVO avo){
		super(avo);
	}
	
	@Override
	public void update(SMTServletRequest req) throws ActionException {
		log.debug("Saving Center Location Info");
		String msg = "msg.updateSuccess";
		
		log.debug(((SiteVO)req.getAttribute("siteData")).getCountryCode());
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		String redir = page.getFullPath() + "?";
		String siteId = ((SiteVO)req.getAttribute("siteData")).getOrganizationId() + "_" + CenterPageAction.getFranchiseId(req) + "_1";
		if (req.getParameter("apprFranchiseId") != null)
			siteId = ((SiteVO)req.getAttribute("siteData")).getOrganizationId() + "_" + req.getParameter("apprFranchiseId") + "_1";
		
		//turn off string encoding since this is an administrative (& secure) method call
		req.setValidateInput(Boolean.FALSE);
		
		// Determine which data is being updated
		try {
			updateLocation(req);
			super.clearCacheByGroup(siteId);			
		} catch(Exception e) {
			log.error("Error Updating Center Page", e);
			msg = "msg.cannotUpdate";
		}
		
		super.clearCacheByGroup(CenterPageAction.getFranchiseId(req));
		
		log.debug("Sending Redirect to: " + redir);
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, redir + "msg=" + msg);
	}

	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		
	}
	
	@Override
	public void build (SMTServletRequest req) throws ActionException {
		
	}
	
	/**
	 * This method forwards requests pertaining to Dealer Info to 
	 * DealerInfoAction for processing.
	 * @param re
	 */
	public void updateLocation(SMTServletRequest req) throws Exception {
		String name = req.getParameter("locationName");
		String desc = this.getLocationDesc(req.getParameter("dealerLocationId"), name);
		
		log.debug("Updating the location Desc: " + desc);
		req.setParameter("locationDesc", desc);
		
		DealerInfoAction dia = new DealerInfoAction(actionInit);
		dia.setAttributes(attributes);
		dia.setDBConnection(dbConn);
		dia.updateDealerLocation(req, false);
	}
	
	/**
	 * Gets the location desc to update the locator info desc
	 * @param id
	 * @return
	 */
	public String getLocationDesc(String id, String locationName) {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String s = "select desc_txt from " + customDb + "fts_franchise a ";
		s+= "inner join  " + customDb + "fts_location_desc_option b ";
		s += "on a.location_desc_option_id = b.location_desc_option_id ";
		s+= "where franchise_id = ? ";
		log.debug("Location Desc SQL: " + s + "|" + id);
		
		PreparedStatement ps = null;
		String locationDesc = "";
		try {
			ps = dbConn.prepareStatement(s);
			ps.setString(1, id);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				locationDesc = rs.getString(1);
			}
		} catch(Exception e) {
			log.error("Error retrieving locaiton desc", e);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		
		// Parse out the [location] in the desc
		StringBuilder desc = new StringBuilder(StringUtil.checkVal(locationDesc));
		int loc = desc.indexOf("[location]");
		if (loc > -1) desc.replace(loc, loc + 10, locationName);
		
		return desc.toString();
	}
	
	/**
	 * This method retrieves all the location information for display to the 
	 * view.
	 * @param id
	 * @return
	 */
	public FranchiseVO getLocationInfo(String id, boolean showPendingData) {
		log.debug("Getting Location Info for store number: " + id);
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		int i = 0;
		
		//Build specialized query across base and custom db.
		StringBuilder s = new StringBuilder();
		s.append("select * from dealer d "); 
		s.append("inner join dealer_location dl on d.dealer_id = dl.dealer_id ");
		s.append("inner join ").append(customDb).append("fts_franchise ff ");
		s.append("on dl.dealer_location_id = ff.franchise_id ");
		s.append("left outer join ").append(customDb).append("fts_right_image fri ");
		s.append("on ff.right_image_id = fri.right_image_id ");
		s.append("left outer join ").append(customDb).append("fts_reseller_button frb ");
		s.append("on ff.reseller_button_id = frb.reseller_button_id ");
		s.append("left outer join ");
		s.append("DEALER_LOCATION_ATTRIBUTE dla on ");
		s.append("dl.DEALER_LOCATION_ID = dla.DEALER_LOCATION_ID ");
		s.append("where dl.dealer_location_id = ? ");
		log.debug("Franchise Location SQL: " + s + "|" + id);
		
		FranchiseVO franchise = new FranchiseVO();
		
		//Map for storing the Franchise Times
		Map<FranchiseTimeVO.DayType, String> times = new HashMap<FranchiseTimeVO.DayType, String>();
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(1, id);
			ResultSet rs = ps.executeQuery();
			for(;rs.next();i++){
				
				//Store the Franchise Values.
				if(i == 0){
					franchise.assignData(rs);
					
					if (showPendingData && rs.getString("new_center_image_url") != null) {
						//key distinction here: 
						//NULL new_image means no changes pending, BLANK new_image means pending deletion 
						franchise.setCenterImage(rs.getString("new_center_image_url"));
						franchise.setPendingImgChange(true);
					}
					if (showPendingData && rs.getString("new_white_board_text") != null) {
						//key distinction here: 
						//NULL new_image means no changes pending, BLANK new_image means pending deletion 
						franchise.setWhiteBoardText(rs.getString("new_white_board_text"));
						franchise.setPendingWbChange(true);
					}
				}
				if(StringUtil.checkVal(rs.getString("ATTRIBUTE_NM")).length() > 0)
				//Add the times to the map
				times.put(FranchiseTimeVO.DayType.valueOf(rs.getString("ATTRIBUTE_NM")), (StringUtil.checkVal(rs.getString("ATTRIBUTE_NM")).length() > 0 ? rs.getString("ATTRIBUTE_TXT") : "Closed"));
			}
			
			//Place the times on the FranchiseVO Attributes Map
			franchise.addAttribute("times", times);
		} catch (Exception e) {
			log.error("Unable to get franchise info", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		return franchise;
	}
	
	/**
	 * This method sets the map data for a franchise so that the mini map on the 
	 * right displays the correct area.
	 * @param f
	 * @return
	 */
	public MapVO setMapData(FranchiseVO f) {
		StringBuilder s = new StringBuilder();
		s.append("/feature/map.jsp?lat=").append(f.getLatitude()).append("&lng=");
		s.append(f.getLongitude()).append("&zoom=13");
		s.append("&saddr=").append(f.getFormattedLocation());
		
		MapLocationVO mapLoc = new MapLocationVO();
		mapLoc.setLocationDesc(f.getLocationName());
		mapLoc.setAddress(f.getAddress());
		mapLoc.setCity(f.getCity());
		mapLoc.setState(f.getState());
		mapLoc.setZipCode(f.getZipCode());
		mapLoc.setLatitude(f.getLatitude());
		mapLoc.setLongitude(f.getLongitude());
		mapLoc.setDefaultLocationFlag(1);
		mapLoc.setLocationUrl(s.toString());
		mapLoc.setMarkerClickType(MapLocationVO.LIGHT_WINDOW);
		
		MapVO map = new MapVO();
		map.setBestFitFlag(false);
		map.setMapWidth(160);
		map.setMapHeight(140);
		map.addLocation(mapLoc);
		map.setMapZoomFlag(false);
		map.setZoomLevel(14);
		return map;
	}	
}
