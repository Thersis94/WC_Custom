/**
 * 
 */
package com.depuy.scripts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.siliconmtn.gis.Location;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.user.LocationManager;
import com.smt.sitebuilder.action.user.LocationManager.AddressSeparator;
import com.smt.sitebuilder.db.DatabaseException;
import com.smt.sitebuilder.db.ProfileImport;

/****************************************************************************
 * <b>Title</b>: HarmonyAddressUpdater.java<p/>
 * <b>Description: Ingests updated address files provided by Harmony to update
 * the DePuy PROFILE_ADDRESS table.  
 * The Data was originally exported using ProfileExport (WC core .db. pkg)</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 16, 2012
 ****************************************************************************/
public class HarmonyAddressUpdater extends ProfileImport {
	
	private static String FILE_PATH="/scratch/depuy address updates/valid.txt";
	private static boolean isInvalidRun = false;
	
	public HarmonyAddressUpdater() {
		super();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {        
        HarmonyAddressUpdater db = new HarmonyAddressUpdater();
		try {
			System.out.println("importFile=" + FILE_PATH);
			List<Map<String,String>> data = db.parseFile(FILE_PATH);
			db.insertRecords(data);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error Processing ... " + e.getMessage());
		}
		db = null;
	}

	
	
	protected void insertRecords(List<Map<String, String>> records) throws Exception {
		//if this is the "invalid addresses" file, mark them as invalid.  Otherwise, updated them.
		if (isInvalidRun) {
			//we need to run this query in batches < 2000 in size...
			int MAX_BATCH_SIZE = 2000;  //MSSQL&JDBC limit us to 2100 parameters on a single query
			Integer listSz = records.size();
			int start = 0;
			int end = (listSz > MAX_BATCH_SIZE) ? MAX_BATCH_SIZE : listSz;
			do {
				markInvalid(records, start, end);
				start = end;
				end = (listSz > end + MAX_BATCH_SIZE) ? end + MAX_BATCH_SIZE : listSz;
			} while (start < listSz);
			
		} else {
			updateAddresses(records);
		}
	}
	
	
	/**
	 * updates the PROFILE_ADDRESS table marking the passed addresses as invalid.
	 * The hard-coded dates are bound to when the data was exported for Harmony,
	 * if a user has updated their address since we exported the data, we're not going
	 * to override that.
	 * @param records
	 * @param start
	 * @param end
	 * @throws DatabaseException
	 */
	private void markInvalid(List<Map<String, String>> records, int start, int end) throws DatabaseException {
		StringBuilder sql = new StringBuilder();
		sql.append("update profile_address set valid_address_flg=0, update_dt=? where ");
		sql.append("create_dt < '2012-12-05 08:00:00' and (update_dt is null or update_dt < '2012-12-05 08:00:00') ");
		sql.append("and valid_address_flg=1 and profile_id in (''");
		
		int updateCnt=0;
		
		for (int i = start; i < end; i++) {
			sql.append(",?");
		}
		sql.append(")");
		
		//Open DB Connection
		Connection dbConn = getDBConnection(DESTINATION_AUTH[0], DESTINATION_AUTH[1], DESTINATION_DB_DRIVER, DESTINATION_DB_URL);
		PreparedStatement ps = null;
		int x = 0;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setTimestamp(++x, Convert.getCurrentTimestamp());
			for (int i = start; i < end; i++) {
				ps.setString(++x, records.get(i).get("PROFILE_ID"));
			}
			updateCnt = ps.executeUpdate();
		} catch (SQLException sqle) {
			throw new DatabaseException(sqle);
		} finally {
			try { ps.close(); dbConn.close(); } catch (Exception e) {}
		}
		
		log.debug("updated " + updateCnt + " of " + records.size() + " records");
	}
	
	
	private void updateAddresses(List<Map<String, String>> records) throws DatabaseException {
		StringBuilder sql = new StringBuilder();
		sql.append("update profile_address set address_txt=?, address2_txt=?, city_nm=?, ");
		sql.append("state_cd=?, zip_cd=?, zip_suffix_cd=?, valid_address_flg=1, ");
		sql.append("current_address_flg=1, cass_validate_flg=1, update_dt=? ");
		sql.append("where profile_id=? and create_dt < '2012-12-05 08:00:00' ");
		sql.append("and (update_dt is null or update_dt < '2012-12-05 08:00:00')");
		
		//Open DB Connection
		Connection dbConn = getDBConnection(DESTINATION_AUTH[0], DESTINATION_AUTH[1], DESTINATION_DB_DRIVER, DESTINATION_DB_URL);
		StringEncrypter se = null;
		try {
			se = new StringEncrypter(encKey);
		} catch (EncryptionException e1) {
			e1.printStackTrace();
		}
		LocationManager lm = new LocationManager(new Location());
		PreparedStatement ps = null;
		int updateCnt = 0;
		
		for (Map<String, String> data : records) {
			try {
				String addr = data.get("ADDRESS");
				String addr2 = null;
				String zip = data.get("ZIP");
				String zipSuffix = null;
				
				//split the address parts
				AddressSeparator as = lm.new AddressSeparator(addr);
				if (as.getAddress() != null && as.getAddress().length() > 0) {
					addr = as.getAddress();
					addr2 = as.getAddress2();
					log.debug("addr=" + addr + ", addr2=" + addr2);
				}
				as = null;
				
				//split zip code if necessary
				if (zip.length() > 5) {
					zipSuffix = zip.substring(5);
					if (zipSuffix.startsWith("-")) zipSuffix = zipSuffix.substring(1);
					zip = zip.substring(0, 5);
					log.debug("zip=" + zip + ", zipSuffix=" + zipSuffix);
				}
				
				ps = dbConn.prepareStatement(sql.toString());
				ps.setString(1, se.encrypt(addr));
				ps.setString(2, addr2);
				ps.setString(3, data.get("CITY"));
				ps.setString(4, data.get("STATE"));
				ps.setString(5, zip);
				ps.setString(6, zipSuffix);
				ps.setTimestamp(7,  Convert.getCurrentTimestamp());
				ps.setString(8, data.get("PROFILE_ID"));
				updateCnt += ps.executeUpdate();
				if (updateCnt % 50 == 0) log.info("updated " + updateCnt + " records");
				
			} catch (SQLException sqle) {
				log.error("sql exception ", sqle);
				log.error(data);
			} catch (EncryptionException e1) {
				log.error("encryption exception ", e1);
				log.error(data);
			} finally {
				try { ps.close(); ps = null; } catch (Exception e) {}
			}
		}
		
		try {
			dbConn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		log.debug("updated " + updateCnt + " of " + records.size() + " records");
	}
}
