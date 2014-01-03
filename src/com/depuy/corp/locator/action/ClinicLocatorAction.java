package com.depuy.corp.locator.action;

import java.util.ArrayList;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.smt.sitebuilder.db.DatabaseException;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.dealer.DealerInfoAction;
import com.smt.sitebuilder.action.dealer.DealerLocationVO;
import com.smt.sitebuilder.action.dealer.DealerLocatorAction;
import com.smt.sitebuilder.action.dealer.DealerLocatorVO;
import com.smt.sitebuilder.action.gis.MapAction;
import com.smt.sitebuilder.action.gis.MapLocationVO;
import com.smt.sitebuilder.action.gis.MapVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: ClinicLocatorAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 30, 2011
 ****************************************************************************/
public class ClinicLocatorAction extends SimpleActionAdapter {
	
	public ClinicLocatorAction() {
	}

	public ClinicLocatorAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	public void build(SMTServletRequest req) throws ActionException {
		req.setValidateInput(false);
		boolean insertAction = Convert.formatBoolean(req.getParameter("insertAction"));
		String msg = (String)getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		
		// Update the record
		try {
			DealerInfoAction dia = new DealerInfoAction();
			dia.setDBConnection(dbConn);
			dia.setAttributes(attributes);
			if (insertAction) {
				
				req.setParameter("dealerId", new UUIDGenerator().getUUID());
				req.setParameter("dealerLocationId", new UUIDGenerator().getUUID());
				req.setParameter("dealerName", req.getParameter("locationName"));
				dia.updateDealerInfo(req);
			}
			
			dia.updateDealerLocation(req, false);
			
			//save the extended data
			req.setParameter("isInsert", "false");
			ExtendedDataAction ext = new ExtendedDataAction(actionInit);
			ext.setDBConnection(dbConn);
			ext.setAttributes(attributes);
			ext.update(req);
			ext = null;
			
		} catch (Exception e) {
			msg = (String)getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			log.error("Unable to add/update hospital record", e);
		}
		//Grab the id for redirecting
		String dlId = req.getParameter("dealerLocationId");
		this.sendRedirect(((PageVO)req.getAttribute(Constants.PAGE_DATA)).getFullPath() + "?sType=1&dlrInfoSub=true&dealerLocationId=" + dlId, msg, req);
	}
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		int sType = Convert.formatInteger(req.getParameter("sType"));
		log.debug("starting retrieve for sType=" + sType);
		if (Convert.formatBoolean(req.getParameter("dlrInfoSub")))
			req.setParameter("cp", "0");

		switch (sType) {
			case ExtendedDataAction.PHYSICIAN_SEARCH:
			case ExtendedDataAction.PATHOLOGY_SEARCH:
				searchByCustomData(req);
				break;
			
			case ExtendedDataAction.HOSPITAL_SEARCH:
			case ExtendedDataAction.LOCATION_SEARCH:
				searchByLocation(req);
				break;
		}	
		getPathologyData(req);

	}
	
	
	/**
	 * searches spatially using the stock Locator for nearby locations, then calls
	 * to the custom data for additional data for those locations.
	 * Returns the merged data in a consistent format
	 * @param req
	 * @throws ActionException
	 */
	@SuppressWarnings("unchecked")
	private void searchByLocation(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		ActionInitVO ai = new ActionInitVO();
		ai.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));

		//call the stock Locator to spatially query nearby locations
		SBUserRole roles = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		if (roles != null && roles.getRoleLevel() == SecurityController.ADMIN_ROLE_LEVEL 
				&& Convert.formatBoolean(req.getParameter("showAll"))) {
			req.setAttribute("showInactive", true);
		}
		
		req.setAttribute("loadAllLocations", true);
		DealerLocatorAction dla = new DealerLocatorAction(ai);
		dla.setDBConnection(dbConn);
		dla.setAttributes(attributes);
		dla.retrieveData(req);
		
		//load all the dealerLocationIds found nearby
		DealerLocatorVO locator = (DealerLocatorVO) mod.getActionData();
		String[] dlrLocnIds = new String[mod.getDataSize()];
		int x = 0;
		for (DealerLocationVO loc : locator.getResults())
			dlrLocnIds[x++] = loc.getDealerLocationId();
		
		log.debug("locns=" + StringUtil.getToString(dlrLocnIds, false, true, ","));
		req.setParameter("dealerLocationId", dlrLocnIds, true);
		
		//get the extended data for these locations
		ExtendedDataAction eda = new ExtendedDataAction(actionInit);
		eda.setDBConnection(dbConn);
		eda.setAttributes(attributes);
		eda.retrieve(req);
		Map<String, DePuyCorpLocationVO> data1 = (Map<String, DePuyCorpLocationVO>) mod.getActionData();
		log.debug("found " + data1.size() + " custom records");
		
		//sort the records before proceeding.  This enables us to do paging properly (after sorting)
		Map<String, DePuyCorpLocationVO> retVals = new LinkedHashMap<String, DePuyCorpLocationVO>();
		List<DePuyCorpLocationVO> phys = new ArrayList<DePuyCorpLocationVO>(data1.values());
		Collections.sort(phys, new SurgeonNameComparator());
		
		
		/* Create start and end to pair down the results for the return data mimicking the Standard Dealer Locator Behavior.
		 * Secondary reason for doing this here instead of in ExtendedDataAction is this allows us to retain how many items
		 * were returned so view can show proper counts.
		 */
		int start = Convert.formatInteger(req.getParameter("cp")) * Convert.formatInteger(req.getParameter("rpp"), 5);
		int end =((Convert.formatInteger(req.getParameter("cp")) + 1) * Convert.formatInteger(req.getParameter("rpp"), 5)) -1;
		log.debug("start: "+start+" | end: "+end);
		
		//iterate the data within our paging limitations
		int i = 0;
		for (DePuyCorpLocationVO vo : phys) {
			//if this pass is within the acceptable range, add it to the return map.
			if (i >= start && i <= end) {
				retVals.put(vo.getDealerLocation().getDealerLocationId(), vo);
			} else if (i > end) {
				break;
			}
			i++;
		}
				
		//merge the records
		for (DealerLocationVO loc : locator.getResults()) {
			DePuyCorpLocationVO vo = retVals.get(loc.getDealerLocationId());
			if (vo != null) vo.setDealerLocation(loc);
		}
		
		req.setAttribute("locatorResults", new ArrayList<DePuyCorpLocationVO>(retVals.values()));
		mod.setDataSize(retVals.size());
		mod.setActionData(locator);
		log.debug("Counts: "+locator.getResultCount()+"|"+mod.getDataSize()+"|"+retVals.size());
		
		//set the map data
		this.setMapResults(req, retVals, locator);
	}

	
	/**
	 * searches 1st against the custom data, then passes a list of dealerLocationIds
	 * to the stock Locator for retrieval of the base data.
	 * Returns the merged data in a consistent format
	 * @param req
	 * @throws ActionException
	 */
	@SuppressWarnings("unchecked")
	private void searchByCustomData(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		
		//load a list of dealers using the passed criteria, from the custom data
		ExtendedDataAction eda = new ExtendedDataAction(actionInit);
		eda.setDBConnection(dbConn);
		eda.setAttributes(attributes);
		eda.retrieve(req);
		Map<String, DePuyCorpLocationVO> data1 = (Map<String, DePuyCorpLocationVO>) mod.getActionData();
		log.debug("found " + data1.size() + " custom records");
		
		//sort the records before proceeding.  This enables us to do paging properly (after sorting)
		Map<String, DePuyCorpLocationVO> retVals = new LinkedHashMap<String, DePuyCorpLocationVO>();
		List<DePuyCorpLocationVO> phys = new ArrayList<DePuyCorpLocationVO>(data1.values());
		Collections.sort(phys, new SurgeonNameComparator());
		
		/* Create start and end to pair down the results for the return data mimicking the Standard Dealer Locator Behavior.
		 * Secondary reason for doing this here instead of in ExtendedDataAction is this allows us to retain how many items
		 * were returned so view can show proper counts.
		 */
		int start = Convert.formatInteger(req.getParameter("cp")) * Convert.formatInteger(req.getParameter("rpp"), 5);
		int end =((Convert.formatInteger(req.getParameter("cp")) + 1) * Convert.formatInteger(req.getParameter("rpp"), 5)) -1;
		log.debug("start: "+start+" | end: "+end);
		
		//iterate the data within our paging limitations
		
		int i = 0;
		for (DePuyCorpLocationVO vo : phys) {
			//if this pass is within the acceptable range, add it to the return map.
			if (i >= start && i <= end) {
				retVals.put(vo.getDealerLocation().getDealerLocationId(), vo);
			} else if (i > end) {
				break;
			}
			i++;
		}
		
		
		//call the stock Locator to retrieve the locations (base data)
		ActionInitVO ai = new ActionInitVO();
		ai.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		DealerLocatorAction dla = new DealerLocatorAction(ai);
		dla.setDBConnection(dbConn);
		dla.setAttributes(attributes);
		log.debug("actionInitId=" + actionInit.getActionId());
				
				
		DealerLocatorVO locator = dla.getLocator(ai.getActionId(), req.getQueryString());
		locator.setPmid(mod.getPageModuleId());
		locator.setCurrentPage(Convert.formatInteger(req.getParameter("cp")));
		locator.setResultPerPage(Convert.formatInteger(req.getParameter("rpp"), 5));
		List<DealerLocationVO> dlrs = null;
		try {
			//use the return map here instead of full data set to only return the necessary values.
			dlrs = dla.getDealerInfo(req, retVals.keySet().toArray(new String[0]), locator);
			log.debug("dealers found: " + dlrs.size());
		} catch (DatabaseException de) {
			log.error("could not load dealers", de);
		}
		
		//merge the data
		for (DealerLocationVO loc : dlrs) {
			DePuyCorpLocationVO vo = retVals.get(loc.getDealerLocationId());
			if (vo != null) vo.setDealerLocation(loc);
		}
		
		//set the response data for the View
		req.setAttribute("locatorResults", new ArrayList<DePuyCorpLocationVO>(retVals.values()));
		locator.setResultCount(data1.size());
		mod.setDataSize(retVals.size());
		mod.setActionData(locator);
		
		//set the map data
		this.setMapResults(req, retVals, locator);
	}
	
	
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	
	private void setMapResults(SMTServletRequest req, Map<String, DePuyCorpLocationVO> dlrs, DealerLocatorVO loc) {
		if (loc == null) loc = new DealerLocatorVO();
		
		// Setup the map info
		MapVO map = new MapVO();
		map.setMapZoomFlag(true);
		map.setBestFitFlag(true);
		map.setMapHeight(loc.getMapHeight());
		map.setMapWidth(loc.getMapWidth());
		
		String base = loc.getMoreInfoUrl() + "&amp;dealerLocationId=";
		for (String k : dlrs.keySet()) {
			MapLocationVO mapLoc = dlrs.get(k).getDealerLocation().getMapLocation();
			mapLoc.setLocationUrl(base + mapLoc.getMapActionId());
			String s = mapLoc.getLocationDesc();
			mapLoc.setLocationDesc(dlrs.get(k).getPhysicianName() + "</b><br/>" + s + "<b>"); //html hack applies at JSP, google_main.jsp
			map.addLocation(mapLoc);
		}
		
		// Add the data to the request object to be processed by the map display
		req.setAttribute(MapAction.MAP_ALT_DATA, map);
	}
	
	private void getPathologyData(SMTServletRequest req) throws ActionException{
		SimpleActionAdapter saa = new PathologiesAction(this.actionInit);
		saa.setAttributes(attributes);
		saa.setDBConnection(dbConn);
		saa.retrieve(req);
	}
	
	
	
	/**
	 * **************************************************************************
	 * <b>Title</b>: SurgeonNameComparator.java<p/>
	 * <b>Description: Splits the physician name field into parts, and compares 
	 * 	using last name.  (which, typically, is the 2nd string token)</b> 
	 * <p/>
	 * <b>Copyright:</b> Copyright (c) 2012<p/>
	 * <b>Company:</b> Silicon Mountain Technologies<p/>
	 * @author James McKain
	 * @version 1.0
	 * @since Mar 7, 2012
	 ***************************************************************************
	 */
	public class SurgeonNameComparator implements Comparator<DePuyCorpLocationVO> {
	    public int compare(DePuyCorpLocationVO o1, DePuyCorpLocationVO o2) {
	    	
	    	String[] n1 = o1.getPhysicianName().split(" ");
	    	String[] n2 = o2.getPhysicianName().split(" ");
	    	int res = 0;
	    	try {
	    		res = n1[1].compareTo(n2[1]);
	    	} catch (Exception e) {
	    		//compare using the full value if we didn't have multiple parts
	    		res = o1.getPhysicianName().compareTo(o2.getPhysicianName());
	    	}
	        return res;
	        
	    	//return o1.getPhysicianName().compareTo(o2.getPhysicianName());
	    }
	}

}
