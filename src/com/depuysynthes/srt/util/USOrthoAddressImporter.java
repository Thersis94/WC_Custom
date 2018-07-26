package com.depuysynthes.srt.util;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.PropertyConfigurator;

import com.depuysynthes.srt.vo.SRTRequestAddressVO;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.gis.AbstractGeocoder;
import com.siliconmtn.gis.GeocodeFactory;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.gis.Location;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title:</b> USOrthoAddressImporter.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Converts US Ortho Case Addresses to Request Addresses.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Jul 2, 2018
 ****************************************************************************/
public class USOrthoAddressImporter extends CommandLineUtil {

	private static final String SOURCE_FILE_CONFIG="scripts/srt/user_import_config.properties";
	private static final String SOURCE_FILE_LOG="scripts/srt/user_import_log4j.properties";

	protected String configFilePath;
	private Map<String, String> requestAddresses;
	/**
	 * @param args
	 */
	public USOrthoAddressImporter(String ... args) {
		super(args);
		if(Files.exists(Paths.get(SOURCE_FILE_LOG))) {
			PropertyConfigurator.configure(SOURCE_FILE_LOG);
		}
		if(Files.exists(Paths.get(SOURCE_FILE_CONFIG))) {
			configFilePath = SOURCE_FILE_CONFIG;
		}
		loadProperties(configFilePath);

		loadDBConnection(props);
	}

	public static void main(String ...args) {
		USOrthoAddressImporter geoCoder = new USOrthoAddressImporter(args);
		geoCoder.run();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		try {
			List<GenericVO> addresses = loadAddresses();
			log.info(StringUtil.join("Loaded ", Integer.toString(addresses.size()), " Legacy Addresses."));

			loadRequestIds(addresses);
			log.info("Request Ids loaded.");

			List<SRTRequestAddressVO> geoAddresses = geocodeAddress();
			log.info(StringUtil.join("Geocoded ", Integer.toString(geoAddresses.size()), " successfully."));

			saveAddresses(geoAddresses);
			log.info("Addresses Saved.");
		} catch(Exception e) {
			log.error("Problem Geocoding Addresses.", e);
		}
	}

	/**
	 * Loads Addresses into GenericVOs for processing of <CaseNumber, Address>
	 * @return
	 */
	private List<GenericVO> loadAddresses() {
		return new DBProcessor(dbConn, "dbo").executeSelect(buildAddressSql(), null, new GenericVO());
	}

	/**
	 * Builds SQL Lookup for Case Addresses in Legacy Data Table.
	 * @return
	 */
	public String buildAddressSql() {
		StringBuilder sql = new StringBuilder(200);
		sql.append(DBUtil.SELECT_CLAUSE).append("distinct case_number as key, ");
		sql.append("trim(shipping_address) as value ");
		sql.append(DBUtil.FROM_CLAUSE).append("dbo.us_ortho_2014_2015 ");
		sql.append(DBUtil.WHERE_CLAUSE).append("shipping_address is not null and trim(shipping_address) != ''");

		return sql.toString();
	}

	/**
	 * LookupRequestIds and convert given legacy Address Records to
	 * Map <SRTRequestId, Address> for later processing.
	 * @param addresses 
	 * @param geoAddresses
	 */
	private void loadRequestIds(List<GenericVO> addresses) {
		requestAddresses = new HashMap<>();
		Map<String, String> requestIds = new HashMap<>();
		int i = 1;

		//Lookup and store mapping of <CaseNumber, requestId>
		try(PreparedStatement ps = dbConn.prepareStatement(loadRequestIdsSql())) {
			ps.setString(i++, "US_ORTHO");
			ResultSet rs = ps.executeQuery();

			while(rs.next()) {
				requestIds.put(rs.getString("co_req_id"), rs.getString("request_id"));
			}
		} catch (SQLException e) {
			log.error("Error Processing Code", e);
		}

		/*
		 * Match Case Number Key of Address to Case Number Key of Request
		 * and store in requestAddresses for processing.
		 */
		for(GenericVO g : addresses) {
			String key = requestIds.get(g.getKey());
			if(!StringUtil.isEmpty(key)) {
				requestAddresses.put(key, g.getValue().toString());
			}
		}

		log.info(StringUtil.join("Generated ", Integer.toString(requestAddresses.size()), " addresses for GeoCoding"));
	}

	/**
	 * Builds Query to load CaseNumber and RequestId from migrated data.
	 * @param size
	 * @return
	 */
	private String loadRequestIdsSql() {
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.SELECT_CLAUSE).append("distinct co_req_id, request_id ");
		sql.append(DBUtil.FROM_CLAUSE).append("custom.dpy_syn_srt_request ");
		sql.append(DBUtil.WHERE_CLAUSE).append("op_co_id = ?");

		return sql.toString();
	}

	/**
	 * Attempt Geocode Addresses and if successful, store result on new
	 * RequestAddressVO.  If not successful, store old address as address_txt
	 * to prevent loss of data.
	 * @param addresses
	 * @return
	 * @throws InterruptedException 
	 */
	private List<SRTRequestAddressVO> geocodeAddress() throws InterruptedException {
		List<SRTRequestAddressVO> ravs = new ArrayList<>();
		// Initialize the geocoder
		String geocodeClass = (String) props.get(GlobalConfig.GEOCODER_CLASS);
		AbstractGeocoder ag = GeocodeFactory.getInstance(geocodeClass);
		ag.addAttribute(AbstractGeocoder.CONNECT_URL, props.get(GlobalConfig.GEOCODER_URL));

		for(Entry<String, String> entry : requestAddresses.entrySet()) {
			// Set the location
			Location l = new Location(entry.getValue()); 

			// Get the geocode
			GeocodeLocation gl = ag.geocodeLocation(l).get(0);
			SRTRequestAddressVO rav;

			/*
			 * If valid, use returned GeocodeLocation.  Else, use given
			 * street address to preserve historical. 
			 */
			if(gl.isValidAddress()) {
				rav = new SRTRequestAddressVO(gl);
			} else {
				rav = new SRTRequestAddressVO();
				rav.setAddress(entry.getValue());
				rav.setCity("");
				rav.setState("");
				rav.setZipCode("");
			}

			rav.setRequestId(entry.getKey());
			ravs.add(rav);
			log.info(rav);

			/*
			 * Was getting 500 errors  from our Geocoder when letting it
			 * run un-throttled.
			 */
			Thread.sleep(2000);
		}

		return ravs;
	}


	/**
	 * Save Addresses after Geo-Coding
	 * @param geoAddresses
	 * @throws DatabaseException 
	 */
	private void saveAddresses(List<SRTRequestAddressVO> geoAddresses) throws DatabaseException {
		new DBProcessor(dbConn, "custom.").executeBatch(geoAddresses);
	}
}