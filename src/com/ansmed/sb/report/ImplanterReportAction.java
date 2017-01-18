 package com.ansmed.sb.report;

// JDK 1.5.0
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// SMT Base Libs
import com.ansmed.sb.physician.SurgeonVO;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.gis.AbstractGeocoder;
import com.siliconmtn.gis.GeocodeFactory;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.gis.GeocodeType;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.NumberFormat;
import com.siliconmtn.util.StringUtil;

// SB Libs
import com.smt.sitebuilder.action.gis.MapAction;
import com.smt.sitebuilder.action.gis.MapLocationVO;
import com.smt.sitebuilder.action.gis.MapVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: ImplanterReportAction.java<p/>
 * <b>Description: </b> Performs a spatial search against the physician database
 * and returns the physicians to be displayed for the appropriate page of
 * the result set. This class is based on com.ansmed.sb.locator.LocatorFacadeAction
 * with minor changes to the querySurgeons method to ensure the proper return of
 * clinic phone numbers.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2008<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Oct 23, 2008
 ****************************************************************************/
public class ImplanterReportAction extends SBActionAdapter {
	public static final String ANS_USER_LATITUDE = "ansUserLatitude";
	public static final String ANS_USER_LONGITUDE = "ansUserLongitude";
	public static final String ANS_USER_STATE = "ansUserState";
	public static final String ANS_RESULT_PAGES = "ansResultPages";
	public static final String ANS_DETAIL_MAP = "ansDetailMap";
	public static final int MAX_SEARCH_RADIUS = 350;
	public static final int DEFAULT_MIN_RETURNED = 5;
	public static final String DEFAULT_ORDER = "distance";
	public static final String RETRIEVE_MIN_NUMBER = "retrieveMinimumNumber";
	public static final String RESULT_ORDER_BY = "resultOrderBy";
	private int ctr = 0;
	private String orderBy = null;
	
	/**
	 * 
	 */
	public ImplanterReportAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public ImplanterReportAction(ActionInitVO arg0) {
		super(arg0);
	}

	
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("Starting in ImplanterReportAction retrieve...");
		
		String reportType = StringUtil.checkVal(req.getParameter("reportType"));
		Integer radius = Convert.formatInteger(req.getParameter("radius"));
		boolean locatorSearchSubmitted = Convert.formatBoolean(req.getParameter("locatorSearchSubmitted"));
		Integer minNum = Convert.formatInteger(req.getParameter("minReturned"), DEFAULT_MIN_RETURNED);
		Boolean findMinNum = Convert.formatBoolean(req.getParameter(RETRIEVE_MIN_NUMBER));
		orderBy = StringUtil.checkVal(req.getParameter(RESULT_ORDER_BY),DEFAULT_ORDER);
		
		log.info("Min Returned");
		// Get the search criteria
		String address = req.getParameter("address");
		String city = req.getParameter("city");
		String state = req.getParameter("state");
		String zipCode = req.getParameter("zipCode");
		String fullAddress = StringUtil.checkVal(req.getParameter("fullAddress"));
		GeocodeLocation gLoc = null;
		if (fullAddress.length() > 0) {
			gLoc = new GeocodeLocation(fullAddress);
			log.debug("Parsing Full Address: " + gLoc.getGeocodeType());
		} else {
			gLoc = new GeocodeLocation(address, city, state, zipCode);
		}
		
		// Set the paging information
		Integer page = Convert.formatInteger(req.getParameter("page"), 1);
		Integer rpp = Convert.formatInteger(req.getParameter("rpp"), 10);
		int start = (page * rpp) - rpp;
		log.debug("Page: " + page);
		gLoc.setLatitude(Convert.formatDouble(req.getParameter(ANS_USER_LATITUDE)));
		gLoc.setLongitude(Convert.formatDouble(req.getParameter(ANS_USER_LONGITUDE)));

		log.debug("Lat/Lng/State: " + gLoc.getLatitude() + "/" + gLoc.getLongitude() + "/" + gLoc.getState());
		log.debug("Is " + gLoc.getGeocodeType() + ": " + gLoc.isMinGeocodeLevel(GeocodeType.city));
		// Check and see if the lat long was already set
		if (locatorSearchSubmitted && gLoc.isMinGeocodeLevel(GeocodeType.city)) {
			//Location loc = new Location(address, city, state, zipCode);
			//log.debug("Search Loc Type:" + loc.getGeocodeType());
			
			// Geocode the location and add the lat/long to the request object
			String geoClass = (String)getAttribute(GlobalConfig.GEOCODER_CLASS);
			log.debug("getting geocode for surgeon search: " + geoClass);
			AbstractGeocoder ag = GeocodeFactory.getInstance(geoClass);
			ag.addAttribute(AbstractGeocoder.CONNECT_URL, getAttribute(GlobalConfig.GEOCODER_URL));
			gLoc = ag.geocodeLocation(gLoc).get(0);
			
			/* 
			 * 11-06-09 Dave B. Added this code from James to provide an 
			 * alternate zipcode lookup so that locator services don't break 
			 * in the event of a Google maps outage, a Google maps server 
			 * IP address change, or when we exceed our geocode quota/license 
			 * 11/27/2009 Removed by JC to support the new Geocoder */

			
			req.setAttribute(ANS_USER_LATITUDE, gLoc.getLatitude());
			req.setAttribute(ANS_USER_LONGITUDE, gLoc.getLongitude());
			req.setAttribute(ANS_USER_STATE, gLoc.getState());
		} else {
			req.setAttribute(ANS_USER_LATITUDE, req.getParameter(ANS_USER_LATITUDE));
			req.setAttribute(ANS_USER_LONGITUDE, req.getParameter(ANS_USER_LONGITUDE));
			req.setAttribute(ANS_USER_STATE, gLoc.getState());
		}
			
		// Get a matching set of surgeons.  If none are returned AND a state was
		// Entered, get all of the surgeons for that state
		List<SurgeonVO> data = new ArrayList<SurgeonVO>();
		if (gLoc.getLatitude() != 0 && gLoc.getLongitude() != 0) {
			data = querySurgeons(1, gLoc, radius, start, rpp, req);
			
			if (data.size() == 0 || (findMinNum && data.size() < minNum)) {
				log.info("Min: " + minNum + "|" + findMinNum);
				data = querySurgeons(1, gLoc, MAX_SEARCH_RADIUS, start, rpp, req);
				log.info("Data Size: " + data.size());
				if (findMinNum && data.size() > minNum) data = data.subList(0, minNum);
			}
		} else if (StringUtil.checkVal(gLoc.getState()).length() > 0) {
			log.debug("State Search");
			data = querySurgeons(2, gLoc, radius, start, rpp, req);
		} else { 
			req.setAttribute("", "");
		}
		
		// Add the count and the data to the container
		int partial = ctr % rpp;
		int numPages = (int) ctr / rpp;
		if (partial > 0) numPages++;
		
		// Determine start page
		int navStart = page - 10;
		if (navStart <= 0) navStart = 1;
		
		// Determine end page
		int navEnd = (navStart - 1) + 20; 
		navEnd = navEnd > numPages ? numPages : navEnd;
		
		// If at the end, reset the start page to be bigger
		if ((navEnd - navStart) <= 20 && numPages - navEnd < 20) {
			navStart -= (20 -(navEnd - navStart));
			if (navStart <= 0) navStart = 1;
		}
		
		// Return the data to the user	
		if (reportType.equalsIgnoreCase("xls")) {
			log.debug("...returning report in XLS format...");
			AbstractSBReportVO rpt = new ImplanterReportVO();
			rpt.setData(data);
			rpt.setFileName("ImplanterReport.xls");
			log.debug("Mime Type: " + rpt.getContentType());
			req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
			req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
		} else if (reportType.equalsIgnoreCase("pdf")) {
			log.debug("...returning report in PDF format...");
			AbstractSBReportVO rpt = new ImplanterReportVO();
			rpt.setData(data);
			rpt.setFileName("ImplanterReport.pdf");
			// add font path for use by PDF renderer.
			rpt.addAttributes("fontPath", attributes.get(Constants.PDF_TEMPLATE_DIR));
			log.debug("Mime Type: " + rpt.getContentType());
			req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
			req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
		} else {
			ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
			mod.setDataSize(ctr);
			mod.setActionData(data);
			log.debug("Pages: " + navStart + "|" + navEnd + "|" + ctr);
			req.setAttribute("ansResultsNav",new Integer[] {navStart, navEnd});
			req.setAttribute(ANS_RESULT_PAGES, numPages);
			mod.setDataSize(ctr);
			mod.setActionData(data);
			attributes.put(Constants.MODULE_DATA, mod);	
		}
	}

	
	/**
	 * Perform the surgeon queries
	 * @param type
	 * @param loc
	 * @param radius
	 * @param start
	 * @param rpp
	 * @param req
	 * @return
	 */
	protected List<SurgeonVO> querySurgeons(int type, GeocodeLocation loc, int radius, 
											int start, int rpp, ActionRequest req) {
		
		List<SurgeonVO> data = new ArrayList<SurgeonVO>();
		
		String customDbSchema = (String)this.getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuffer sql = new StringBuffer();
		sql.append("select a.surgeon_id, title_nm, first_nm, middle_nm, last_nm, ");
		sql.append("suffix_nm, a.website_url, clinic_nm, address_txt, address2_txt, ");
		sql.append("city_nm, state_cd, zip_cd, latitude_no, longitude_no, ");
		sql.append("phone_number_txt, spanish_flg, b.clinic_id, specialty_nm, phone_type_id ");
		
		// Add the distance param as part of the sql statement if the search
		// is spatial
		if (type == 1) {
	        sql.append(",round((sqrt(power(").append(loc.getLatitude()).append("-latitude_no,2) + power(");
	        sql.append(loc.getLongitude()).append(" - longitude_no,2)) /3.14159265)*180,1) as distance ");
		}
		
		sql.append("from ").append(customDbSchema).append("ans_surgeon a ");
		sql.append("inner join ").append(customDbSchema).append("ans_clinic b ");
		sql.append("on a.surgeon_id = b.surgeon_id ");
		sql.append("left outer join ").append(customDbSchema).append("ans_specialty s ");
		sql.append("on a.specialty_id = s.specialty_id ");
		sql.append("left outer join ").append(customDbSchema).append("ans_phone c ");
		sql.append("on b.clinic_id = c.clinic_id and c.phone_type_id in ('WORK_PHONE','FAX') ");
		
		// for a spatial search, create the bounding box for the search
		if (type == 1) {
			
			Double lat = loc.getLatitude();
			Double lng = loc.getLongitude();
			Integer distance = Convert.formatInteger(radius, 25);
			double radDegree = 2 * distance * .014;
	        sql.append("where round((sqrt(power(").append(loc.getLatitude());
	        sql.append("-latitude_no,2) + power(").append(loc.getLongitude());
	        sql.append(" - longitude_no,2)) /3.14159265)*180,1) <= ").append(distance);
			sql.append(" and Latitude_no > ").append(NumberFormat.roundGeocode(lat - radDegree)).append(" and ");
			sql.append("Latitude_no < ").append(NumberFormat.roundGeocode(lat + radDegree)).append(" and ");
			sql.append("Longitude_no > ").append(NumberFormat.roundGeocode(lng - radDegree)).append(" and ");
			sql.append("Longitude_no < ").append(NumberFormat.roundGeocode(lng + radDegree));
			sql.append(" and status_id = 1 and surgeon_type_id = 0 and locator_display_flg = 1 ");
			sql.append("order by ").append(orderBy).append(" ");
		} else if (type == 2) {
			sql.append("where upper(state_cd) = '").append(loc.getState()).append("' ");
			sql.append("and status_id = 1  and surgeon_type_id = 0 and locator_display_flg = 1 ");
			sql.append("order by last_nm, first_nm ");
		}
		
		log.info("Locator search sql: " + sql.toString());
		
		PreparedStatement ps = null;
		
		MapVO map = new MapVO();
		map.setMapZoomFlag(true);
		map.setBestFitFlag(true);
		map.setMapHeight(300);
		map.setMapWidth(450);
		List<String> clinics = new ArrayList<String>();
		try {
			SurgeonVO vo = new SurgeonVO();
			ps = dbConn.prepareStatement(sql.toString());
			ResultSet rs = ps.executeQuery();
			for(; rs.next(); ctr++) {
				if (ctr >= start && ctr < (start + rpp)) {
					// If there are multiple phones, add it to the existing 
					// Value Object
					if (clinics.contains(rs.getString("clinic_id"))) {
						PhoneVO fax = new PhoneVO();
						fax.setPhoneNumber(rs.getString("phone_number_txt"));
						
						// Check the phone type and set it appropriately.
						if (StringUtil.checkVal(rs.getString("phone_type_id")).equals("WORK_PHONE")) {
							fax.setPhoneType(PhoneVO.WORK_PHONE);
						} else if (StringUtil.checkVal(rs.getString("phone_type_id")).equals("FAX")) {
							fax.setPhoneType(PhoneVO.FAX_PHONE);
						}
						
						vo.getClinic().addPhone(fax);
						log.debug("---> Same SurgeonVO, size of phones is now: " + vo.getClinic().getPhones().size());
						ctr--;
					} else {
						vo = new SurgeonVO(rs);
						log.debug("Surgeon Info: " + vo);
						
						// The first phone record gets set as the "main" phone 
						// when the SurgeonVO is created. We need to see what type
						// it really is and set the type appropriately.
						if (StringUtil.checkVal(rs.getString("phone_type_id")).equals("WORK_PHONE")) {
							vo.getClinic().getPhones().get(0).setPhoneType(PhoneVO.WORK_PHONE);
						} else if (StringUtil.checkVal(rs.getString("phone_type_id")).equals("FAX")) {
							vo.getClinic().getPhones().get(0).setPhoneType(PhoneVO.FAX_PHONE);
						}
						data.add(vo);
						log.debug("---> New SurgeonVO, size of phones is: " + vo.getClinic().getPhones().size());

						MapLocationVO mLoc = new MapLocationVO();
						mLoc.setData(rs);
						mLoc.setLocationDesc(vo.getClinic().getClinicName());
						StringBuffer sb = new StringBuffer();
						sb.append("?radius=").append(radius).append("&rpp=").append(rpp);
						sb.append("&ansUserLatitude=").append(loc.getLatitude());
						sb.append("&ansUserLongitude=").append(loc.getLongitude());
						sb.append("&state=").append(loc.getState());
						sb.append("&pmid=").append(req.getParameter("pmid"));
						sb.append("&page=").append(req.getParameter("page"));
						sb.append("&surgeonId=").append(vo.getSurgeonId());
						sb.append("&clinicId=").append(vo.getClinic().getClinicId());
					
						mLoc.setLocationUrl(sb.toString());
						map.addLocation(mLoc);
						clinics.add(vo.getClinic().getClinicId());
					}
				}
			}
		} catch (SQLException e) {
			log.error("Error finding surgeons",e);
		}
		
		// add the map data to the page
		req.setAttribute(MapAction.MAP_ALT_DATA, map);
		
		// Return the locations
		return data;
	}
	
}
