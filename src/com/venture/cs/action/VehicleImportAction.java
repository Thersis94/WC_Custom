package com.venture.cs.action;

// JDK 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

// WebCrescendo 2.0
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.user.SBProfileManager;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 *<b>Title</b>: VehicleImportAction<p/>
 *Takes in a csv file divided by pipes and loads it into the database <p/>
 *Copyright: Copyright (c) 2013<p/>
 *Company: SiliconMountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since July 23, 2013
 * Changes:
 * July 23, 2013: Eric Damschroder: created class
 * Mar 11, 2014: DBargerhuff: added additional comments
 ****************************************************************************/

public class VehicleImportAction  extends SBActionAdapter {
	
	private final String DELIMITER = "\\|";
	
	/**
	 * 
	 */
	public VehicleImportAction() {
		super();
	}

	/**
	 * 
	 * @param arg0
	 */
	public VehicleImportAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder addVehicleSQL = new StringBuilder();
		StringBuilder addOwnerSQL = new StringBuilder();
		StringBuilder updateVehicleSQL = new StringBuilder();
		
		addVehicleSQL.append("INSERT INTO ").append(customDb).append("VENTURE_VEHICLE ");
		addVehicleSQL.append("(DEALER_ID, MAKE, MODEL, YEAR, PURCHASE_DT, ");
		addVehicleSQL.append("FREEZE_FLG, CREATE_DT, OWNER_ID, VIN, VENTURE_VEHICLE_ID) ");
		addVehicleSQL.append("VALUES(?, ?, ?, ?, ?, 0, ?, ?, ?, ?) ");

		updateVehicleSQL.append("UPDATE ").append(customDb).append("VENTURE_VEHICLE ");
		updateVehicleSQL.append("SET DEALER_ID = ?, MAKE = ?, MODEL = ?, YEAR = ?, PURCHASE_DT = ?, ");
		updateVehicleSQL.append("FREEZE_FLG = 0, UPDATE_DT = ?, OWNER_ID = ?, VIN = ? ");
		updateVehicleSQL.append("WHERE VENTURE_VEHICLE_ID = ?");
		
		try {
			Map<String, String> dealers = getDealers();
			Map<String, String> vehicles = getExistingVehicles();
			List<Map<String,String>> data = loadData(req);
			PreparedStatement owner = dbConn.prepareStatement(addOwnerSQL.toString());
			PreparedStatement addVehicle = dbConn.prepareStatement(addVehicleSQL.toString());
			PreparedStatement updateVehicle = dbConn.prepareStatement(updateVehicleSQL.toString());
			String ownerId;
			for (Map<String, String> row : data) {
				SBProfileManager sb = new SBProfileManager(attributes);
		    	UserDataVO user = new UserDataVO(req);
		    	user.setMainPhone(StringUtil.checkVal(row.get("Phone")));
		    	user.setName(StringUtil.checkVal(row.get("Name")));
		    	user.setAddress(StringUtil.checkVal(row.get("Address")));
		    	user.setCity(StringUtil.checkVal(row.get("City")));
		    	user.setZipCode(StringUtil.checkVal(row.get("Postal Code")));
		    	user.setState(StringUtil.checkVal(row.get("State")));
		    	ownerId = sb.checkProfile(user, dbConn);
		    	
		    	sb.updateProfile(user, dbConn);
				
				if (vehicles.containsKey(row.get("VIN"))) {
					log.debug("Update");
					// This vin already exists in the database and we need to perform an update.
					updateVehicle.setString(1, StringUtil.checkVal(dealers.get(row.get("Dealer Name").toUpperCase())));
					updateVehicle.setString(2, StringUtil.checkVal(row.get("Make")));
					updateVehicle.setString(3, StringUtil.checkVal(row.get("Model Number")));
					updateVehicle.setString(4, StringUtil.checkVal(row.get("YEAR")));
					updateVehicle.setString(5, StringUtil.checkVal(row.get("Delivery Date")));
					updateVehicle.setTimestamp(6, Convert.getCurrentTimestamp());
					updateVehicle.setString(7, ownerId);
					updateVehicle.setString(8, StringUtil.checkVal(row.get("VIN")));
					updateVehicle.setString(9, StringUtil.checkVal(vehicles.get(row.get("VIN"))));
					
					updateVehicle.addBatch();	
				} else {
					log.debug("Create");
					addVehicle.setString(1, StringUtil.checkVal(dealers.get(row.get("Dealer Name").toUpperCase())));
					addVehicle.setString(2, StringUtil.checkVal(row.get("Make")));
					addVehicle.setString(3, StringUtil.checkVal(row.get("Model Number")));
					addVehicle.setString(4, StringUtil.checkVal(row.get("YEAR")));
					addVehicle.setString(5, StringUtil.checkVal(row.get("PURCHASE")));
					addVehicle.setTimestamp(6, Convert.getCurrentTimestamp());
					addVehicle.setString(7, ownerId);
					addVehicle.setString(8, StringUtil.checkVal(row.get("VIN")));
					addVehicle.setString(9, new UUIDGenerator().getUUID());
					
					addVehicle.addBatch();		
				}
				
				
			}
			// We set autocommit to false so that we don't end up with an issue two commands
			// in and have the database get filled up with incpomplete or erronous data
			dbConn.setAutoCommit(false);
			
			// Execute the queries, making sure to do owner first in order to provide 
			// the vehicles with thier foreign keys.
			owner.executeBatch();
			updateVehicle.executeBatch();
			addVehicle.executeBatch();
			
			dbConn.commit();
			
			dbConn.setAutoCommit(true);
			
		} catch (SQLException e) {
			log.error("Could not update the database ", e);
			try {
				dbConn.rollback();
			} catch (SQLException e1) {
				log.error("Could not rollback database after failed import", e);
			}
		} catch (DatabaseException e) {
			log.error("Unable to get user data from database ", e);
		}
	}
	
	/**
	 * Loads all the data from the | delimited file uploaded by the user
	 */
	private List<Map<String, String>> loadData(SMTServletRequest req) {
		String[] lines = new String(req.getFile("uploadFile").getFileData()).split("\\r?\\n");
		
		//first row contains column names; must match UserDataVO mappings
		String tokens[] = lines[0].split(DELIMITER, -1);
		String[] columns = new String[tokens.length];
		for(int i=0; i<tokens.length; i++){
			columns[i] = tokens[i];
		}
		
		Map<String,String> entry = null;
		List<Map<String,String>> data = new ArrayList<Map<String,String>>();
		
		//execution in this loop WILL throw NoSuchElementException.
		//This is not trapped so you can cleanup data issue prior to import
		for (int y = 1; y<lines.length; y++) {
			tokens = lines[y].split(DELIMITER, -1);
			
			//test quality of data
			if (tokens.length != columns.length) {
				String error="";
				for(String token : tokens)
					error+=token+"|";
				log.error("Not loading row# " + y + " " + error);
				continue;
			}
			
			entry = new HashMap<String,String>(20);
			for (int x=0; x < tokens.length; x++) {
				String value = StringUtil.checkVal(tokens[x].trim());
				
				//remove surrounding quotes if they exist
				if (value.startsWith("\"") && value.endsWith("\""))
					value = value.substring(1, value.length()-1);
				
				if (value.equals("null")) value = null;
				//if (y %30 == 0) log.debug(columns[x] + " = " + value);
				entry.put(columns[x], value);
			}
			data.add(entry);
			entry = null;
		}
		
		log.debug("file size is " + data.size() + " rows");
		
		return data;
	}
	
	/**
	 * Get a map of all the dealer names and ids for venture
	 * @return
	 * @throws SQLException
	 */
	private Map<String, String> getDealers() throws SQLException {
		Map<String, String> dealers  = new HashMap<String, String>();
		StringBuilder sb = new StringBuilder();
		DBUtil db = new DBUtil();
		
		sb.append("SELECT d.DEALER_ID, dl.LOCATION_NM FROM DEALER d ");
		sb.append("left join DEALER_LOCATION dl on d.DEALER_ID = dl.DEALER_ID ");
		sb.append("WHERE d.ORGANIZATION_ID='VENTURE_RV'");
		log.debug(sb.toString());
		PreparedStatement ps = dbConn.prepareStatement(sb.toString());
		ResultSet rs = ps.executeQuery();
		
		while(rs.next())
			dealers.put(StringUtil.checkVal(db.getStringVal("LOCATION_NM", rs)).toUpperCase(), StringUtil.checkVal(db.getStringVal("DEALER_ID", rs)));
		
		return dealers;
	}
	
	/**
	 * Get the vin and vehicle id of all existing vehicles
	 */
	private Map<String, String> getExistingVehicles() throws SQLException {
		Map<String, String> vehicles  = new HashMap<String, String>();
		StringBuilder sb = new StringBuilder();
		DBUtil db = new DBUtil();
		
		sb.append("SELECT VIN, VENTURE_VEHICLE_ID FROM " + attributes.get(Constants.CUSTOM_DB_SCHEMA) + "VENTURE_VEHICLE ");
		
		PreparedStatement ps = dbConn.prepareStatement(sb.toString());
		ResultSet rs = ps.executeQuery();
		
		while(rs.next())
			vehicles.put(db.getStringVal("VIN", rs),db.getStringVal("VENTURE_VEHICLE_ID", rs));
		return vehicles;
	}

}
