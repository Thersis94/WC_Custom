package com.venture.cs.action;

// JDK 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



// SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.data.report.GenericReport;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.siliconmtn.util.parser.AnnotationParser;
import com.smt.sitebuilder.action.AbstractSBReportVO;
// WebCrescendo 2.0
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.WebCrescendoReport;
import com.smt.sitebuilder.common.SiteBuilderUtil;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.venture.cs.action.vo.VehicleArchiveImportVO;

/****************************************************************************
 *<b>Title</b>: VehicleArchiveImportAction<p/>
 *Imports vehicles into the archive table <p/>
 *Copyright: Copyright (c) 2014<p/>
 *Company: SiliconMountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Mar 31, 2014
 * Changes:
 * Mar 31, 2014: DBargerhuff: created class
 ****************************************************************************/

public class VehicleArchiveImportAction  extends SBActionAdapter {
	
	/**
	 * 
	 */
	public VehicleArchiveImportAction() {
		super();
	}

	/**
	 * 
	 * @param arg0
	 */
	public VehicleArchiveImportAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(SMTServletRequest req) throws ActionException {
		log.debug("VehicleArchiveImportAction list...");
		if(Convert.formatBoolean(req.getParameter("retrieveForm"))){
			processImport(req, false);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {
		log.debug("VehicleArchiveImportAction update...");
		if (Convert.formatBoolean(req.getParameter("vehicleImport"))) {
			processImport(req, true);
		}
	}
	
	/**
	 * This method processes an import list in CSV form from the Dealer Import
	 * Tool via admintool.  
	 */
	private void processImport(SMTServletRequest req, boolean isImport) 
			throws ActionException {
		log.debug("starting processImport, isImport: " + isImport);
		
		AnnotationParser parser = null;
		FilePartDataBean fpdb = req.getFile("uploadFile");
		String format = (fpdb != null ? fpdb.getExtension() : req.getParameter("format"));

		//We prep some lists for the Annotation Parser to give it direction.
		try {
			parser = new AnnotationParser(VehicleArchiveImportVO.class, format);
		} catch(Exception e) {
			log.error("Could not create Parser", e);
		}
		/*
		 * If we are not importing, put the Annotation data on the request so
		 * that we can use it to generate our form later.
		 */
		if (! isImport) {
			log.debug("returning csv form...");
			AbstractSBReportVO rpt = new WebCrescendoReport(new GenericReport());
			rpt.setFileName("Venture-Vehicle-Import-Template." + format);
			rpt.setData(parser.getTemplate());
			req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
			req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
			
		} else {
			log.debug("importing vehicle file...");
			List<VehicleArchiveImportVO> errors = null;
			// attempt to read the file uploaded by the user.  
			try {
			
				/* Turn off autoCommit in case of failure */
				dbConn.setAutoCommit(false);
				// 1. Attempt to read import file and parse String values
				
				//Convert String Values to Objects
				Map<Class<?>, Collection<Object>> beans = parser.parseFile(fpdb, true);
				
				//Forward data for importing.
				errors = importData(req, beans, isImport);
				
				//Commit the changes and set autoCommit true to save the data.
				dbConn.commit();
				dbConn.setAutoCommit(true);
			} catch (SQLException | ActionException sqle){
				log.error("Error importing VIN archive data file, ", sqle);
				throw new ActionException(sqle.getMessage());
			} catch (InvalidDataException e) {
				throw new ActionException("Error Parsing Data from File", e);
			}
			
			// build the response msg and redirect
			StringBuilder msg = new StringBuilder();
			if (errors.size() > 0) {
				//msg = "Errors occurred during import"
				msg.append("The vehicle import completed with errors:<br/>");
				for (VehicleArchiveImportVO v : errors) {
					msg.append("VIN ").append(v.getVin()).append(": ").append(v.getActionName()).append("<br/>");
				}
			} else {
				msg.append(attributes.get(AdminConstants.KEY_SUCCESS_MESSAGE));
			}
			
			SiteBuilderUtil util = new SiteBuilderUtil();
			util.adminRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
			
		}
	}
	
	/**
	 * Processes the import of the uploaded data file.
	 * @param beans
	 * @param isImport
	 * @param dealerTypeId
	 */
	private List<VehicleArchiveImportVO> importData (SMTServletRequest req, Map<Class<?>, Collection<Object>> beans, boolean isImport) 
			throws ActionException {
		log.debug("starting importData...");
		
		// 1. retrieve existing VINS from the archive table.
		Map<String, String> vins = retrieveArchivedVINs();
		Map<String, String> dealers = retrieveDealers(req);
		
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA); 
		StringBuilder sb = new StringBuilder();
		sb.append("insert into ").append(customDb).append("VENTURE_VEHICLE_OWNER_ARCHIVE ");
		sb.append("(VIN,DEALER_ID,LOCATION_NM,MODEL,MAKE,OWNER_NM,ADDRESS_TXT,CITY_NM,");
		sb.append("STATE_CD,ZIP_CD,COUNTRY_CD,PURCHASE_DT,CREATE_DT) ");
		sb.append("values (?,?,?,?,?,?,?,?,?,?,?,?,?) ");
		
		// 2. loop import row data and insert vehicles
		List<VehicleArchiveImportVO> skipped = new ArrayList<>();
		Collection<Object> vos = beans.get(VehicleArchiveImportVO.class);

		boolean skip = false;
		for (Object obj : vos) {
			VehicleArchiveImportVO vo = (VehicleArchiveImportVO)obj;
			
			if (StringUtil.checkVal(vo.getVin()).length() == 0) continue;

			// if VIN already exists or if DEALER_ID is invalid, set message and flag
			if (vins.containsKey(vo.getVin())) {
				vo.setActionName("VIN already exists in archive table");
				skip = true;
			} else if (! dealers.containsKey(vo.getDealerId())) {
				vo.setActionName("Specified dealer ID was not found");
				skip = true;
			}
			
			if (skip) {
				// add to skipped list for return to view
				skipped.add(vo);
			} else {
				// attempt to insert the vehicle into the archive
				insertVehicle(vo, sb, skipped);
			}
			
			skip = false;
		}

		return skipped;
	}
	
	/**
	 * Inserts a vehicle into the vehicle owner archive table.  If the insert fails due to a SQLException,
	 * the vehicle is added to the 'skipped' vehicle collection with an informational message indicating the
	 * reason for the failure. 
	 * @param vehicles
	 * @param sql
	 */
	private void insertVehicle(VehicleArchiveImportVO vehicle, StringBuilder sql, List<VehicleArchiveImportVO> skipped) {
		PreparedStatement ps = null;
		int index = 1;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(index++, vehicle.getVin());
			ps.setString(index++, vehicle.getDealerId());
			ps.setString(index++, vehicle.getLocationName());
			ps.setString(index++, vehicle.getModel());
			ps.setString(index++, vehicle.getMake());
			ps.setString(index++, vehicle.getOwnerName());
			ps.setString(index++, vehicle.getAddress());
			ps.setString(index++, vehicle.getCity());
			ps.setString(index++, vehicle.getState());
			ps.setString(index++, vehicle.getZipCode());
			ps.setString(index++, vehicle.getCountryCode());
			ps.setString(index++, vehicle.getPurchaseDate());
			ps.setTimestamp(index++, Convert.getCurrentTimestamp());
			ps.execute();
		} catch (Exception e) {
			vehicle.setActionName("An import field value is too long");
			skipped.add(vehicle);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) { log.error("Error closing PreparedStatement, ", e); }
			}
		}
	}
	
	/**
	 * Get the vin of all vehicles in the existing archive
	 */
	private Map<String, String> retrieveArchivedVINs() throws ActionException {
		log.debug("retrieving archived VINS...");
		Map<String, String> vehicles  = new HashMap<>();
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT VIN FROM " + attributes.get(Constants.CUSTOM_DB_SCHEMA) + "VENTURE_VEHICLE_OWNER_ARCHIVE ");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ResultSet rs = ps.executeQuery();
			String vin;
			while(rs.next()) {
				vin = rs.getString("VIN");
				vehicles.put(vin,vin);
			}
		} catch (SQLException sqle) {
			throw new ActionException(sqle.getMessage());
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) { log.error("Error closing PreparedStatement, ", e); }
			}
		}
		return vehicles;
	}

	/**
	 * Get the current list of customer service dealers
	 */
	private Map<String, String> retrieveDealers(SMTServletRequest req) 
			throws ActionException {
		log.debug("retrieving dealer IDs...");
		Map<String, String> dealers  = new HashMap<>();
		StringBuilder sb = new StringBuilder();
		sb.append("select a.DEALER_ID,  b.LOCATION_NM from DEALER a ");
		sb.append("inner join DEALER_LOCATION b on a.DEALER_ID = b.DEALER_ID ");
		sb.append("where a.ORGANIZATION_ID = ? and a.DEALER_TYPE_ID = ? ");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, req.getParameter("organizationId"));
			ps.setInt(2, Convert.formatInteger(req.getParameter("dealerTypeId")));
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				dealers.put(rs.getString("DEALER_ID"), rs.getString("LOCATION_NM"));
			}
		} catch (SQLException sqle) {
			throw new ActionException(sqle.getMessage());
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) { log.error("Error closing PreparedStatement, ", e); }
			}
		}
		return dealers;
	}
}
