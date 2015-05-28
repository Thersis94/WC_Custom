package com.fastsigns.action.franchise;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.fastsigns.action.franchise.vo.FranchiseVO;
import com.fastsigns.action.franchise.vo.MetroCategoryVO;
import com.fastsigns.action.franchise.vo.MetroContainerVO;
import com.fastsigns.action.franchise.vo.MetroProductVO;
import com.fastsigns.action.wizard.FastsignsMetroWizard;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.exception.GeocodeException;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.dealer.DealerLocationVO;
import com.smt.sitebuilder.action.dealer.DealerLocatorAction;
import com.smt.sitebuilder.action.gis.MapLocationVO;
import com.smt.sitebuilder.action.gis.MapVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;


/****************************************************************************
 * <b>Title</b>: MetroAction.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Gather the information for a particular metro area
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Dec 9, 2010<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class MetroAction extends SBActionAdapter {
	
	public MetroAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public MetroAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.http.SMTServletRequest)
	 */
	public void delete(SMTServletRequest req) throws ActionException {
		//test for an admintool call to this method (directly)
		if (req.hasParameter("sbActionId")) {
			super.delete(req);
			super.moduleRedirect(req, getAttribute(AdminConstants.KEY_MESSAGE), 
					(String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
			return;
		}
		
		/* TODO Delete the try catch block when Fastsigns is fully converted over
		 * the page and module metro workflow.  Until then metro areas may be deleted that
		 * do not yet have pages or modules.
		 */
		try {
			// Make sure to delete the associated page and module for this metro area
			FastsignsMetroWizard fmw = new FastsignsMetroWizard(actionInit, dbConn);
			fmw.setAttributes(attributes);
			fmw.delete(req);
		} catch (Exception e) {
			log.warn("There were issues deleting the module and page associated with this metro area. ", e);
		}
		
		String cDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String s = "delete from " + cDb + "fts_metro_area where metro_area_id = ?";
		log.debug("********* Deleting Metro Area: " + req.getParameter("metroAreaId"));
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s);
			ps.setString(1, req.getParameter("metroAreaId"));
			ps.executeUpdate();
		} catch (Exception e) {
			throw new ActionException("Unable to delete metro location", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
	}
	
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		String orgId = ((SiteVO)req.getAttribute("siteData")).getOrganizationId();
		String franchiseTxt = (!orgId.contains("AU")) ? "Fastsigns" : "Signwave";
		MetroProductAction mpa = new MetroProductAction();
		mpa.setActionInit(actionInit);
		mpa.setAttributes(attributes);
		mpa.setDBConnection(dbConn);
		
		int lastIndex = 3;
		boolean connectCat = Convert.formatBoolean(((ModuleVO)attributes.get(Constants.MODULE_DATA)).getAttribute(ModuleVO.ATTRIBUTE_2));
		String metroAlias = StringUtil.checkVal(req.getParameter(SMTServletRequest.PARAMETER_KEY + "1"));
		if (metroAlias.length() == 0) {
			metroAlias = getMetro();
		}
		String productAlias = StringUtil.checkVal(req.getParameter(SMTServletRequest.PARAMETER_KEY + "2"));
		if (productAlias.length() == 0) {
			productAlias = StringUtil.checkVal(req.getParameter("product"));
		}
		Boolean isLST = Boolean.FALSE;
		if (metroAlias.equalsIgnoreCase("LST")) {
			metroAlias = StringUtil.checkVal(req.getParameter(SMTServletRequest.PARAMETER_KEY + "2"));
			productAlias = StringUtil.checkVal(req.getParameter(SMTServletRequest.PARAMETER_KEY + "3"));
			isLST = Boolean.TRUE;
			lastIndex = 4;
		}
		
		log.debug("Retrieving Metro Area: " + metroAlias + ", product=" + productAlias);
		
		//define some consistency across metros and LSTs
		req.setParameter("metroAlias", metroAlias);
		req.setParameter("productAlias", productAlias);
		req.setParameter("lstBase", (isLST) ? "LST/" : "");  //gets prepended to URLs to give us the /qs/LST/ base
		
		if (metroAlias.length() == 0) {
			this.listData(req);
			
			//Verify/set roles so that File Manager works properly
			KeystoneCenterPageAction cpa = new KeystoneCenterPageAction(actionInit);
			cpa.polymorphRoles(req);
			
		} else if (connectCat) {			
			// Get the data
			MetroContainerVO mcvo = this.getLocations(req, metroAlias, isLST);
			mcvo.setMapData(this.getMapData(mcvo));
			req.setAttribute("mapAltData", null);
			mcvo.setProdList(mpa.getProductCategories(mcvo.getMetroAreaId()));
			getFranchiseResults(mcvo,req);
			
			//If we're on a product page, update the title with the products title.
			log.debug("productAlias = " + productAlias);
			if(productAlias != null && productAlias.length() > 0) {
				PageVO p = (PageVO) req.getAttribute(Constants.PAGE_DATA);
				MetroCategoryVO vo;
				for (Node n : mcvo.getProdList()) {
					vo = (MetroCategoryVO)n.getUserObject();
					if (productAlias.equals(vo.getMetroCategoryAlias())) {
						p.setTitleName(vo.getTitleTxt());
						p.setMetaDesc(vo.getMetaDesc());
						p.setMetaKeyword(vo.getMetaKywd());
					}
						
				}
			}
			
			this.putModuleData(mcvo, mcvo.getResults().size(), false);
			// If the metro area isn't found, redirect to the locator page
			if (mcvo.getResults().size() == 0)
				super.sendRedirect("/My_" + franchiseTxt, null, req);
		} else {			
			// Get the data
			MetroContainerVO mcvo = this.getLocations(req, metroAlias, isLST);
			mcvo.setMapData(this.getMapData(mcvo));
			req.setAttribute("mapAltData", null);
			mcvo.setProductPages(mpa.getProductPages(mcvo.getMetroAreaId(), false, null));
			getFranchiseResults(mcvo,req);
			
			//If we're on a product page, update the title with the products title.
			log.debug("productAlias = " + productAlias);
			if(productAlias != null && productAlias.length() > 0) {
				PageVO p = (PageVO) req.getAttribute(Constants.PAGE_DATA);
				MetroProductVO mpv = mcvo.getProductPages().get(productAlias);
				if (mpv != null) {
					p.setTitleName(mpv.getTitleTxt());
					p.setMetaDesc(mpv.getMetaDesc());
					//p.setMetaKeyword(mpv.getMetaKywd());
				} else {
					log.warn("No product for that alias.  Redirecting to metro page.");
					super.sendRedirect("/metro-" + mcvo.getAreaAlias(), null, req);
				}
			}
			
			this.putModuleData(mcvo, mcvo.getResults().size(), false);
			// If the metro area isn't found, redirect to the locator page
			if (mcvo.getResults().size() == 0)
				super.sendRedirect("/My_" + franchiseTxt, null, req);
		}
		
		/*
		 * Need to trim excess parameters off qs.
		 */
		if(StringUtil.checkVal(req.getParameter(SMTServletRequest.PARAMETER_KEY + lastIndex)).length() > 0){
			StringBuilder sb = new StringBuilder();
			log.debug(req.getRequestURI());
			for(int i = 1; i < lastIndex; i++){
				sb.append("/");
				if(lastIndex == 3 && i == 1)
					sb.append("metro-");
				else if(lastIndex == 4 && i == 1)
					sb.append("metro/" + attributes.get(Constants.QS_PATH));
				
				sb.append(StringUtil.checkVal(req.getParameter(SMTServletRequest.PARAMETER_KEY + i)));
				}
			super.sendRedirect(sb.toString(), null, req);
		}
		
		if(metroAlias.length() > 0) {
				setCanonicals(req, productAlias, metroAlias);
		}
	}
	
	/**
	 * Adds franchise data to vo list
	 * @param mcvo
	 * @param req
	 * @throws ActionException
	 */
	private void getFranchiseResults(MetroContainerVO mcvo, SMTServletRequest req) 
	throws ActionException {
		FranchiseLocatorAction sai = new FranchiseLocatorAction();
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		List<DealerLocationVO> lst = null;
		try {
			lst = sai.getCustomFranchiseData(mcvo.getResults());
		} catch (SQLException e) {
			log.error(e);
		}
		if (lst != null)
			mcvo.setResults(lst); 
	}

	/**
	 * Set the canonical and relative urls for the page based on the product and metro aliases we are given
	 * @param req
	 * @param url
	 */
	private void setCanonicals(SMTServletRequest req, String productAlias, String metroAlias) {
		log.debug("Setting the canonical page urls");
		PageVO p = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		SiteVO s = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		
		StringBuilder url = new StringBuilder();
		
		url.append("/");
		
		if(req.getRequestURI().contains("metro"))
			url.append("metro-");
		
		url.append(metroAlias);
		
		if(StringUtil.checkVal(productAlias).length() > 0 && !req.getRequestURI().contains("metro")) {
			url.append("/products-" + productAlias);
		} else if (StringUtil.checkVal(productAlias).length() > 0){
			url.append("/" + productAlias);
		}
		
		if (!Convert.formatBoolean(s.getMobileFlag())) {
			p.setCanonicalMobileUrl(s.getMobileSiteUrl() + "/metro-" + metroAlias);
		}
		
		p.setCanonicalPageUrl(url.toString());
		
	}

	/**
	 * Gets the metro area based on the action id 
	 */
	private String getMetro() {
		String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT fma.AREA_ALIAS_NM FROM SB_ACTION sb ");
		sql.append("JOIN ").append(customDb).append("FTS_METRO_AREA fma ");
		sql.append("on fma.METRO_AREA_ID = sb.ATTRIB1_TXT ");
		sql.append("WHERE ACTION_ID = ?");
		
		try {
			PreparedStatement ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1,actionInit.getActionId());
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				return rs.getString(1);
		} catch (SQLException e) {
			log.error("Failed to get Metro area based on the action id. ", e);
		}
		return "";
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	private void listData(SMTServletRequest req) throws ActionException {
		log.debug("Listing Metro Areas");
		String id = StringUtil.checkVal(req.getParameter("metroAreaId"));
		String type = StringUtil.checkVal(req.getParameter("type"));
		String orgId = ((SiteVO)req.getAttribute("siteData")).getOrganizationId();
		boolean connectCat = Convert.formatBoolean(((ModuleVO)attributes.get(Constants.MODULE_DATA)).getAttribute(ModuleVO.ATTRIBUTE_2));
		MetroProductAction mpa = new MetroProductAction();
		mpa.setActionInit(actionInit);
		mpa.setAttributes(attributes);
		mpa.setDBConnection(dbConn);
		
		
		if (type.equals("locs") && id.length() > 0) {
			String lat = req.getParameter("latitude");
			String lng = req.getParameter("longitude");
			MetroContainerVO mcvo = getMetroAssoc(id, lat, lng, req.getParameter("country")); 
			this.putModuleData(mcvo, mcvo.getResults().size(), false);

		} else if (connectCat && type.equals("products") && id.length() > 0) {
				MetroContainerVO mcvo = getMetroInfo(id);
				mcvo.setProdList(mpa.getProductList(id, orgId));
				this.putModuleData(mcvo, mcvo.getProdList().size(), false);
				
		}  else if (type.equals("products") && id.length() > 0) {
			MetroContainerVO mcvo = getMetroInfo(id);
			mcvo.setProductPages(mpa.getProductPages(id, true, req.getParameter("metroProductId")));
			this.putModuleData(mcvo, mcvo.getProductPages().size(), false);
			
		} else if (id.length() > 0) {
			MetroContainerVO mcvo = getMetroInfo(id);
			if (StringUtil.checkVal(mcvo.getAreaName()).length() == 0) {
				mcvo.getMapData().setBestFitFlag(false);
				mcvo.getMapData().setZoomLevel(mcvo.getMapZoomNo());
				mcvo.getMapData().setMapZoomFlag(1);
			}
			this.putModuleData(mcvo, 0, false);
		} else {
			String countryCd = ((SiteVO)req.getAttribute("siteData")).getCountryCode();
			List<MetroContainerVO> maCon = this.getMetroList(false, countryCd);
			this.putModuleData(maCon, maCon.size(), false);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("building Metro Area: " + req.getParameter("metroAreaId"));
		
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		String msg = "msg.updateSuccess";
		String reqType = StringUtil.checkVal(req.getParameter("type"));
		StringBuilder url = new StringBuilder(page.getRequestURI());
		boolean connectCat = Convert.formatBoolean(((ModuleVO)attributes.get(Constants.MODULE_DATA)).getAttribute(ModuleVO.ATTRIBUTE_2));
		MetroProductAction mpa = new MetroProductAction();
		mpa.setActionInit(actionInit);
		mpa.setAttributes(attributes);
		mpa.setDBConnection(dbConn);
		
		try {
			if (Convert.formatBoolean(req.getParameter("areaSubmitted"))) {
				this.updateMetroInfo(req);
				
			} else if (Convert.formatBoolean(req.getParameter("assocSubmitted"))) {
				this.updateMetroAssoc(req);
				
			} else if ("delete".equals(reqType)) {
				this.delete(req);
				/*
				 * Update add Product Piece to use checkboxes and allow batch editing
				 * If the batch process isn't run, attempt to add off just the request.
				 */
			} else if (connectCat && "addProduct".equals(reqType)) {
				req.setValidateInput(Boolean.FALSE);
				url.append("?metroAreaId=").append(req.getParameter("metroAreaId"));
				url.append("&type=products&metroName=").append(req.getParameter("metroName"));
				url.append("&webEdit=true&metroLocation=").append(req.getParameter("metroLocation"));
				
				mpa.deleteMetroProducts(req.getParameter("metroAreaId"));
				mpa.addMetroProducts(req);
			} else if ("addProduct".equals(reqType)) {
				req.setValidateInput(Boolean.FALSE);
				url.append("?metroAreaId=").append(req.getParameter("metroAreaId"));
				url.append("&type=products&metroName=").append(req.getParameter("metroName"));
				url.append("&webEdit=true&metroLocation=").append(req.getParameter("metroLocation"));
				String [] ids = req.getParameterValues("productActionId");
				for(String s : ids){
					req.setParameter("productActionId", s);
					String productId = mpa.addProduct(req);
					
					if (Convert.formatBoolean(req.getParameter("showEdit")))
						url.append("&metroProductId=" + productId);
				}
			} else if ("editProduct".equals(reqType)) {
				req.setValidateInput(Boolean.FALSE);
				url.append("?metroAreaId=").append(req.getParameter("metroAreaId"));
				url.append("&type=products&metroName=").append(req.getParameter("metroName"));
				url.append("&webEdit=true&metroLocation=").append(req.getParameter("metroLocation"));
				
				mpa.editProduct(req);
				
			} else if ("delProduct".equals(reqType)) {
				url.append("?metroAreaId=").append(req.getParameter("metroAreaId"));
				url.append("&type=products&metroName=").append(req.getParameter("metroName"));
				url.append("&webEdit=true&metroLocation=").append(req.getParameter("metroLocation"));

				mpa.delProduct(req);
			}
			
		} catch(Exception e) {
			msg = "msg.cannotUpdate";
			log.error(msg, e);
		}
		
		// Redirect the browser
		super.sendRedirect(url.toString(), msg, req);
	}
	
	
	
	/**
	 * 
	 * @param req
	 */
	private void updateMetroInfo(SMTServletRequest req) throws SQLException {
		log.debug("Updating Metro Info");
		String metroAreaId = StringUtil.checkVal(req.getParameter("metroAreaId")); 
		StringBuilder s = new StringBuilder();
		String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		log.debug("Location ID: " + metroAreaId + "|" + req.getParameter("latitude"));
		boolean newMetro = metroAreaId.equalsIgnoreCase("NEW");
		
		if (newMetro) {
			metroAreaId = new UUIDGenerator().getUUID();
			s.append("insert into ").append(customDb).append("fts_metro_area (");
			s.append("area_nm, image_path_url, area_desc, area_lst_flg, ");
			s.append("latitude_no, longitude_no, title_txt, meta_keyword_txt, ");
			s.append("meta_desc_txt, area_alias_nm, create_dt, communities_txt, locality_txt, ");
			s.append("country_cd, map_zoom_no, metro_area_id) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		} else {
			s.append("update ").append(customDb).append("fts_metro_area set area_nm = ?, ");
			s.append("image_path_url = ?, area_desc = ?, area_lst_flg = ?, ");
			s.append("latitude_no = ?, longitude_no = ?, title_txt = ?, ");
			s.append("meta_keyword_txt = ?, meta_desc_txt = ?, area_alias_nm = ?, ");
			s.append("update_dt = ?, communities_txt = ?, locality_txt = ?, ");
			s.append("country_cd=?, map_zoom_no = ? where metro_area_id = ? ");
		}
		log.debug("Metro Area Update SQL: " + s + "|" + req.getParameter("imagePath"));
		PreparedStatement ps = null;
		try {
			req.setValidateInput(false);
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(1, req.getParameter("areaName"));
			ps.setString(2, req.getParameter("imagePath"));
			ps.setString(3, req.getParameter("areaDesc"));
			ps.setInt(4, Convert.formatYesNo(req.getParameter("isLst")));
			ps.setDouble(5, Convert.formatDouble(req.getParameter("latitude")));
			ps.setDouble(6, Convert.formatDouble(req.getParameter("longitude")));
			ps.setString(7, req.getParameter("title"));
			ps.setString(8, req.getParameter("metaKeyword"));
			ps.setString(9, req.getParameter("metaDesc"));
			ps.setString(10, req.getParameter("areaAlias"));
			ps.setTimestamp(11, Convert.getCurrentTimestamp());
			ps.setString(12, req.getParameter("communities"));
			ps.setString(13, req.getParameter("locality"));
			ps.setString(14, req.getParameter("country"));
			ps.setInt(15, Convert.formatInteger(req.getParameter("mapZoomNo")));
			ps.setString(16, metroAreaId);
			
			ps.executeUpdate();
			
			/* TODO Delete the try catch block when Fastsigns is fully converted over
			 * the page and module metro workflow.  Until then metro areas may be updated that
			 * do not yet have pages or modules.
			 */
			try {
				// Now that the metro area has been created we need to create the page and module for it.
				FastsignsMetroWizard fmw = new FastsignsMetroWizard(actionInit, dbConn);
				fmw.updateMetroArea(req, metroAreaId, newMetro);
			} catch (Exception e) {
				log.warn("There were issues creating or updating the module and page associated with this metro area. ", e);
			}
			
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
	}

	/**
	 * Add a Franchise to a Metro Group.
	 * @param req
	 */
	private void updateMetroAssoc(SMTServletRequest req) throws SQLException {
		log.debug("****** Updating Metro Associations");
		String cDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String dSql = "delete from " + cDb + "fts_franchise_metro_xr where metro_area_id = ? ";
		String iSql = "insert into " + cDb + "fts_franchise_metro_xr (metro_area_id, ";
		iSql += "franchise_id, create_dt) values (?,?,?) ";
		
		// Delete the existing
		PreparedStatement psDel = dbConn.prepareStatement(dSql);
		psDel.setString(1, req.getParameter("metroAreaId"));
		psDel.executeUpdate();
		
		// insert the new
		PreparedStatement ps = dbConn.prepareStatement(iSql);
		String[] vals = req.getParameterValues("franchiseId");
		for(int i=0; i < vals.length; i++) {
			ps.setString(1, req.getParameter("metroAreaId"));
			ps.setString(2, vals[i]);
			ps.setTimestamp(3, Convert.getCurrentTimestamp());
			ps.addBatch();
		}
		ps.executeBatch();
	}
	
	/**
	 * Returns Locations associated with a metro.
	 * @param id
	 * @param lat
	 * @param lng
	 * @return
	 */
	private MetroContainerVO getMetroAssoc(String id, String lat, String lng, String countryCd) {
		StringBuilder s = new StringBuilder();
		String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String distance = "round((sqrt(power(" + lat + "- geo_lat_no,2) + power(";
		distance += lng + "- geo_long_no,2)) /3.14159265)*180,1) ";
		countryCd = StringUtil.checkVal(countryCd);
		
		s.append("select c.metro_area_id as id, *, ").append(distance).append("as distance from DEALER_LOCATION a ");
		s.append("inner join ").append(customDb).append("FTS_FRANCHISE b ");
		s.append("on a.DEALER_LOCATION_ID = b.FRANCHISE_ID ");
		s.append("left outer join ").append(customDb).append("FTS_FRANCHISE_METRO_XR c ");
		s.append("	on b.FRANCHISE_ID = c.FRANCHISE_ID ");
		s.append("and c.METRO_AREA_ID = ? ");
		s.append("left outer join ").append(customDb).append("FTS_METRO_AREA d ");
		s.append("on c.METRO_AREA_ID = d.METRO_AREA_ID ");
		s.append("where ");
		if (!"US".equalsIgnoreCase(countryCd) && countryCd.length() > 0) {
			s.append("a.country_cd=? ");
		} else {
			s.append(distance).append(" < 100 ");
		}
		s.append("order by distance ");
		log.debug("Metro Assoc SQL: " + s + "|" + id);
		
		// set the map info
		MapVO map = new MapVO();
		map.setBestFitFlag(true);
		map.setMapHeight(450);
		map.setMapWidth(450);
		map.setMapTypeFlag(false);
		map.setMapZoomFlag(true);
		//map.setZoomLevel(mcvo.getMapZoomNo());
		int activeLocs = 0;
		PreparedStatement ps = null;
		MetroContainerVO mcvo = new MetroContainerVO();
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(1, id);
			if (!"US".equalsIgnoreCase(countryCd) && countryCd.length() > 0)
				ps.setString(2, countryCd);
			
			ResultSet rs = ps.executeQuery();
			for (int i=0; rs.next(); i++) {
				if (i++ == 0) mcvo.assignVals(rs);
				
				// Add the location data
				DealerLocationVO dlr = new FranchiseVO(rs);
				dlr.setActionId(rs.getString("id"));
				mcvo.addResult(dlr);
				
				// Only add the current selections
				if (StringUtil.checkVal(dlr.getActionId()).length() > 0) {		
					MapLocationVO loc = new MapLocationVO();
					loc.setLocationDesc(dlr.getLocationName() + "[Store # " + dlr.getDealerLocationId() + "]");
					loc.setLatitude(dlr.getLatitude());
					loc.setLongitude(dlr.getLongitude());
					loc.setAddress(dlr.getAddress());
					loc.setCity(dlr.getCity());
					loc.setState(dlr.getState());
					loc.setZipCode(dlr.getZipCode());
					map.addLocation(loc);
					activeLocs++;
				}
			}
			if(activeLocs < 2){
				map.setBestFitFlag(true);
			}
			// Add the map data
			mcvo.setMapData(map);
		} catch (Exception e) {
			log.error("Unable to retrieve metro locations", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		return mcvo;
	}
	
	/**
	 * Gets data for the selected area
	 * @param id
	 * @return
	 */
	private MetroContainerVO getMetroInfo(String id) {
		String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String s = "select * from " + customDb + "fts_metro_area where metro_area_id = ?";
		log.debug("metro Info SQL: " + s + "|" + id);
		MetroContainerVO mcvo = new MetroContainerVO();
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s);
			ps.setString(1, id);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				mcvo = new MetroContainerVO(rs);
				
				// Set the map data
				MapVO map = new MapVO();
				map.setBestFitFlag(true);
				map.setMapHeight(310);
				map.setMapWidth(340);
				map.setMapTypeFlag(true);
				map.setMapZoomFlag(true);
				map.setZoomLevel(mcvo.getMapZoomNo());
				if(mcvo.getMapZoomNo() > 0)
					map.setBestFitFlag(false);
				
				MapLocationVO loc = new MapLocationVO();
				loc.setLatitude(mcvo.getLatitude());
				loc.setLongitude(mcvo.getLongitude());
				map.addLocation(loc);
				mcvo.setMapData(map);
			}
			
		} catch (Exception e) {
			log.error("Unable to retrieve metro locations", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		return mcvo;
	}
	
	/**
	 * Gets all of the metros and their products in a single query
	 * @return
	 */
	public List<MetroContainerVO> getMetroSitemap(String countryCd) {
		if ("US".equals(countryCd)) {
			return getMetroSitemapPages(countryCd);
		}
		
		String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder s = new StringBuilder();
		s.append("select * from ").append(customDb).append("fts_metro_area a ");
		s.append("left outer join ").append(customDb).append("FTS_METRO_CATEGORY b ");
		s.append("on a.METRO_AREA_ID = b.METRO_AREA_ID ");
		s.append("where AREA_LST_FLG = 0 ");
		s.append("and a.country_cd = ? ");
		s.append("order by area_lst_flg, area_nm, order_no ");
		log.debug("Metro Sitemap SQL: " + s +"|"+ countryCd);
		
		List<MetroContainerVO> data = new ArrayList<MetroContainerVO>();
		String id = null, currId = null;
		MetroContainerVO vo = null;
		MetroCategoryVO cat = null;
		PreparedStatement ps = null;
		Node n = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(1, countryCd);
			ResultSet rs = ps.executeQuery();
			
			for (int i=0; rs.next(); i++) {
				id = rs.getString("metro_area_id");				
				if (! id.equals(currId)) {
					if (i > 0) {
						data.add(vo);
					}
					vo = new MetroContainerVO(rs);
					cat = new MetroCategoryVO(rs);
					n = new Node();
					n.setUserObject(cat);
					vo.addProduct(n);
				} else {
					cat = new MetroCategoryVO(rs);
					n = new Node();
					n.setUserObject(cat);
					vo.addProduct(n);
				}
				
				currId = id;
			}
			
			// Add the last entry to the list
			data.add(vo);
		} catch (Exception e) {
			log.error("Unable to retrieve metro locations", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}

		return data;
	}
	
	/**
	 * Gets all of the metros and their products in a single query
	 * @return
	 */
	public List<MetroContainerVO> getMetroSitemapPages(String countryCd) {
		String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder s = new StringBuilder();
		s.append("select * from ").append(customDb).append("fts_metro_area a ");
		s.append("left outer join ").append(customDb).append("FTS_METRO_AREA_PRODUCT b ");
		s.append("on a.METRO_AREA_ID = b.METRO_AREA_ID ");
		s.append("and visible_flg = 1 where AREA_LST_FLG = 0 ");
		if(!countryCd.equals("US"))
			s.append("and a.country_cd = ? ");
		s.append("order by area_lst_flg, area_nm, order_no ");
		log.debug("Metro Sitemap SQL: " + s +"|"+ countryCd);
		
		List<MetroContainerVO> data = new ArrayList<MetroContainerVO>();
		String id = null, currId = null;
		MetroContainerVO vo = null;
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			if(!countryCd.equals("US"))
				ps.setString(1, countryCd);
			ResultSet rs = ps.executeQuery();
			
			for (int i=0; rs.next(); i++) {
				id = rs.getString("metro_area_id");				
				if (! id.equals(currId)) {
					if (i > 0) {
						data.add(vo);
					}
					vo = new MetroContainerVO(rs);
					vo.addProductPage(new MetroProductVO(rs));
				} else {
					vo.addProductPage(new MetroProductVO(rs));
				}
				
				currId = id;
			}
			
			// Add the last entry to the list
			data.add(vo);
		} catch (Exception e) {
			log.error("Unable to retrieve metro locations", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}

		return data;
	}
	
	/**
	 * Returns the List of all Metros 
	 * @return
	 */
	public List<MetroContainerVO> getMetroList(Boolean loadProducts, String countryCd) {
		String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		boolean connectCat = Convert.formatBoolean(((ModuleVO)attributes.get(Constants.MODULE_DATA)).getAttribute(ModuleVO.ATTRIBUTE_2));
		MetroProductAction mpa = new MetroProductAction();
		mpa.setActionInit(actionInit);
		mpa.setAttributes(attributes);
		mpa.setDBConnection(dbConn);
		
		StringBuilder s = new StringBuilder();
		s.append("select * from ").append(customDb).append("fts_metro_area ");
		if(!countryCd.equals("US"))
			s.append("where country_cd = ? ");
		s.append("order by area_lst_flg, area_nm ");
		log.debug("sql: " + s.toString());
		List<MetroContainerVO> data = new ArrayList<MetroContainerVO>();
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			if(!countryCd.equals("US"))
				ps.setString(1, countryCd);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				MetroContainerVO vo = new MetroContainerVO(rs);

				//this was added to be called specifically from the SiteMapServlet
				if (connectCat && loadProducts) {
					vo.setProdList(((mpa.getProductCategories(vo.getMetroAreaId()))));
				} else if (loadProducts) {
					vo.setProductPages((mpa.getProductPages(vo.getMetroAreaId(), false, null)));
				}
				
				data.add(vo);
			}
			
		} catch (Exception e) {
			log.error("Unable to retrieve metro locations", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		return data;
	}
	
	/**
	 * Adds the map object to the request
	 * @param req
	 * @param mcvo
	 */
	private MapVO getMapData(MetroContainerVO mcvo) {
		MapVO map = new MapVO();
		map.setBestFitFlag(true);
		map.setMapHeight(310);
		map.setMapWidth(340);
		map.setMapZoomFlag(true);
		map.setZoomLevel(mcvo.getMapZoomNo());
		if(mcvo.getMapZoomNo() > 0)
			map.setBestFitFlag(false);
		// Add the locations
		for (int i=0; i < mcvo.getResults().size(); i++) {
			DealerLocationVO dl = mcvo.getResults().get(i);
			MapLocationVO ml = dl.getMapLocation();
			ml.setLatitude(dl.getLatitude());
			ml.setLongitude(dl.getLongitude());
			ml.setDefaultLocationFlag(0);
			ml.setMarkerClickType(MapLocationVO.JAVASCRIPT_CALL);
			ml.setLocationUrl("toggleLocatorDataMain(" + i + ")");
			map.addLocation(ml);
		}
		
		return map;
	}
	
	/**
	 *  Returns the Metro Locations found for this local.
	 * @param alias
	 * @param isLst
	 * @return
	 */
	private MetroContainerVO getLocations(SMTServletRequest req, String alias, boolean isLST) {
		Boolean useAttrib1Txt = Convert.formatBoolean((req.getParameter("useAttrib1Txt")));
		log.debug("Filtering results: " + useAttrib1Txt);
		String query = null;
		String country = ((SiteVO)req.getAttribute("siteData")).getCountryCode();
		int ratio = (country.equals("US") || country.equals("GB")) ? 0 : 1;
		
		if(req.hasParameter("zip"))
			query = getSearchQuery(req, useAttrib1Txt, ratio, country);
		else
			query = getReqularQuery(req, useAttrib1Txt, alias, ratio, country);
		
		PreparedStatement ps = null;
		MetroContainerVO mcvo = new MetroContainerVO();
		mcvo.setUnitKey((country.equals("US") || country.equals("GB")) ? "gis.mi" : "gis.km");
		try {
			ps = dbConn.prepareStatement(query);
			ps.setString(1, StringUtil.checkVal(alias).trim());
			ps.setInt(2, (isLST) ? 1 : 0);
			if (!"US".equals(country))
				ps.setString(3, country);
			ResultSet rs = ps.executeQuery();
			int i=0;
			while (rs.next()) {
				if (i++ == 0) {
					mcvo.assignVals(rs);
					
					// set the page data
					PageVO page = (PageVO)req.getAttribute(Constants.PAGE_DATA);
					page.setTitleName(StringUtil.checkVal(rs.getString("title_txt"), page.getTitleName()));
					page.setMetaDesc(StringUtil.checkVal(rs.getString("meta_desc_txt"), page.getMetaDesc()));
					page.setMetaKeyword(StringUtil.checkVal(rs.getString("meta_keyword_txt"), page.getMetaKeyword()));
				}
				
				// Add the location data
				DealerLocationVO dlvo = new FranchiseVO(rs);
				mcvo.addResult(dlvo);
			}
			
		} catch (Exception e) {
			log.error("Unable to retrieve metro locations", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		return mcvo;
		
	}
	
	/**
	 * Returns SQL lookup Query retrieving in order of distance from provided zipcode.
	 */
	private String getSearchQuery(SMTServletRequest req, boolean useAttrib1Txt, int ratio, String country){
		req.setParameter("country", ((SiteVO)req.getAttribute("siteData")).getCountryCode());
		String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sb = new StringBuilder();
		DealerLocatorAction dla = new FranchiseLocatorAction(this.actionInit);
		dla.setAttributes(attributes);
		dla.setDBConnection(dbConn);
		GeocodeLocation l = new GeocodeLocation();
		try {
			l = dla.getGeocode(req);
		} catch (GeocodeException e) {
			log.debug("Could not geocode location " + req.getParameter("zip"), e);
		}
		
		sb.append("select round((sqrt(power(").append(l.getLatitude());
		sb.append("- geo_lat_no,2) + power(").append(l.getLongitude());
		sb.append("- geo_long_no,2)) /3.14159265)*180*").append(getDistanceRatio(ratio));
		sb.append(",1) as distance, * ");
		sb.append("from dealer_location a ");
		sb.append("inner join ").append(customDb).append("fts_franchise b ");
		sb.append("on a.DEALER_LOCATION_ID = b.franchise_id ");
		sb.append("inner join ").append(customDb).append("fts_franchise_metro_xr c ");
		sb.append("on b.franchise_id = c.franchise_id ");
		sb.append("inner join ").append(customDb).append("fts_metro_area d ");
		sb.append("on c.metro_area_id = d.metro_area_id ");
		sb.append("where d.area_alias_nm = ? and d.area_lst_flg=? ");
		if (!"US".equals(country))
			sb.append("and a.country_cd = ? ");
		if (useAttrib1Txt) sb.append("and ATTRIB1_TXT is not null and ATTRIB1_TXT != '' ");
		sb.append("order by distance");
		
		log.debug("Metro Area SQL: " + sb + "|" + req.getParameter("zip") + "|");

		return sb.toString();
	}
	
	/**
	 * The following method returns the conversion ratio to go from miles(default) to whichever unit of measure is desired.
	 * @param unit
	 * @return
	 */
	private double getDistanceRatio(int unit){
		switch(unit){
		//miles
		case 0:{
			return 1;
		}
		//kilometers
		case 1:{
			return 1.609;
		}
		default:
			return 1;
		
		}
	}
	
	/**
	 * Returns SQL lookup Query retrieving in order of distance from metro center.
	 */
	private String getReqularQuery(SMTServletRequest req, boolean useAttrib1Txt, String alias, int ratio, String country) {
		String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder s = new StringBuilder();

		s.append("select *, ");
		s.append("round((sqrt(power(d.latitude_no - a.geo_lat_no,2) + power(d.longitude_no - a.geo_long_no,2)) /3.14159265)*180*").append(getDistanceRatio(ratio));
		s.append(",1) as distance ");
		s.append("from dealer_location a ");
		s.append("inner join ").append(customDb).append("fts_franchise b ");
		s.append("on a.DEALER_LOCATION_ID = b.franchise_id ");
		s.append("inner join ").append(customDb).append("fts_franchise_metro_xr c ");
		s.append("on b.franchise_id = c.franchise_id ");
		s.append("inner join ").append(customDb).append("fts_metro_area d ");
		s.append("on c.metro_area_id = d.metro_area_id ");
		s.append("where d.area_alias_nm = ? and d.area_lst_flg=? ");
		if (!"US".equals(country))
			s.append("and a.country_cd = ? ");
		if (useAttrib1Txt) s.append("and ATTRIB1_TXT is not null and ATTRIB1_TXT != '' ");
		s.append("order by distance");
		log.debug("Metro Area SQL: " + s + "|" + alias + "|");
		
		return s.toString();
	}
	
}
