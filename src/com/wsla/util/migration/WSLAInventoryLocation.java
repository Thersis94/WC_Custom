package com.wsla.util.migration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.wsla.data.provider.ProviderLocationVO;
import com.wsla.util.migration.vo.SWLocFileVO;

/****************************************************************************
 * <p><b>Title:</b> WSLAInventoryLocation.java</p>
 * <p><b>Description:</b> </p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Jan 22, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class WSLAInventoryLocation extends AbsImporter {

	private List<SWLocFileVO> data;

	/**
	 * These are locations that already exist but didn't align with out matching algorithm.
	 */
	protected static Map<String, String> staticMappings = new HashMap<>(20); 
	static {
		staticMappings.put("ALF", "ALF100");
		staticMappings.put("BEL", "BEL100");
		staticMappings.put("EAL", "EAL100");
		staticMappings.put("EAS", "EAS100");
		staticMappings.put("EGO", "EGO100");
		staticMappings.put("ELU", "ELU100");
		staticMappings.put("ETI", "ETI100");
		staticMappings.put("EVM", "EVM100");
		staticMappings.put("RAC", "RAC100");
		staticMappings.put("ROM", "ROM100");
		staticMappings.put("SED", "SED100");
		staticMappings.put("SLR", "SLR100");
		staticMappings.put("SVT", "SVT100");
		staticMappings.put("TDG", "TDG100");
		staticMappings.put("TEL", "TEL100");
		staticMappings.put("TSM", "TSM110");
		//these are manually created.  See notes file and run those first!
		staticMappings.put("020", "WSLA_020");
		staticMappings.put("030", "WSLA_030");
		staticMappings.put("031", "WSLA_031");
		staticMappings.put("SRK", "WSLA_SRK");
		staticMappings.put("UNI", "WSLA_UNI");
		staticMappings.put("D11", "WLSA_D11");
		staticMappings.put("D21", "WSLA_D21");
		staticMappings.put("D31", "WSLA_D31");
		staticMappings.put("D41", "WSLA_D41");
		staticMappings.put("D51", "WSLA_D51");
	}

	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbstractImporter#run()
	 */
	@Override
	void run() throws Exception {
		data = readFile(props.getProperty("swLocFile"), SWLocFileVO.class, SHEET_1);

		String sql = StringUtil.join("delete from ", schema, "wsla_provider_location ",
				"where provider_id='WSLA_PROVIDER'");
		delete(sql);
		
		// We may need to create the provider!
		DBProcessor dbp = new DBProcessor(dbConn, schema);
		sql = "INSERT INTO custom.wsla_provider (provider_id, provider_type_id, provider_nm, create_dt) VALUES('WSLA_PROVIDER', 'WSLA', 'WSLA', current_timestamp)";
		dbp.executeSQLCommand(sql);
		
		//Create the default/WSLA locations
		sql = "INSERT INTO custom.wsla_provider_location (location_id, provider_id, location_nm, active_flg, default_flg, create_dt) VALUES " + 
				"('WSLA_020', 'WSLA_PROVIDER', 'ALGEBASA - WS LATAM', 1, 0, current_timestamp),\n" + 
				"('WSLA_030', 'WSLA_PROVIDER', 'SERVICIOS CARYO SA de CV', 1, 1, current_timestamp),\n" + 
				"('WSLA_031', 'WSLA_PROVIDER', 'CARYO HARVEST - WSLA', 1, 0, current_timestamp),\n" + 
				"('WSLA_SRK', 'WSLA_PROVIDER', 'SIRAK / HITACHI', 1, 0, current_timestamp),\n" + 
				"('WSLA_UNI', 'WSLA_PROVIDER', 'UNION COMERCIAL DE GUATEMALA', 1, 0, current_timestamp),\n" + 
				"('WSLA_D11', 'WSLA_PROVIDER', 'CISER', 0, 0, current_timestamp),\n" + 
				"('WSLA_D21', 'WSLA_PROVIDER', 'SERVICENTER MONTERREY', 0, 0, current_timestamp),\n" + 
				"('WSLA_D31', 'WSLA_PROVIDER', 'IMSG SISTEMAS DEL GOLFO', 0, 0, current_timestamp),\n" + 
				"('WSLA_D41', 'WSLA_PROVIDER', 'REPARACIONES ELECTRONICA', 0, 0, current_timestamp),\n" + 
				"('WSLA_D51', 'WSLA_PROVIDER', 'SOLUTIONS ELECTRONICS', 0, 0, current_timestamp)";
		dbp.executeSQLCommand(sql);
		
		save();
	}


	/**
	 * Save the imported providers to the database.
	 * @param data
	 * @throws Exception 
	 */
	@Override
	protected void save() throws Exception {
		//turn the list of data into a unique list of providers we write to the database
		Map<String, ProviderLocationVO> locations = new HashMap<>(data.size());
		Map<String, String> casLocns = getProviderLocations();
		for (SWLocFileVO dataVo : data) {
			ProviderLocationVO vo = transposeProviderData(dataVo, new ProviderLocationVO());
			String namePrefix = vo.getLocationName();
			int wsIdx = namePrefix.indexOf(' ');
			if (wsIdx > -1) namePrefix = namePrefix.substring(0, wsIdx);
			String idPrefix = vo.getLocationId();
			//log.debug("namePrefix=" + namePrefix + ", id=" + idPrefix)
			if (casLocns.containsKey(idPrefix)) {
				log.debug(String.format("mapped %s to %s by id - not creating location", vo.getLocationId(), idPrefix));
				vo.setLocationId(idPrefix);
				vo.setLocationName(casLocns.get(idPrefix));
			} else if (casLocns.containsKey(namePrefix)) {
				log.debug(String.format("mapped %s to %s by name - not creating location", vo.getLocationId(), namePrefix));
				vo.setLocationId(namePrefix);
				vo.setLocationName(casLocns.get(namePrefix));
			} else if (staticMappings.containsKey(idPrefix)) {
				log.debug(String.format("mapped %s to %s manually - not creating location", vo.getLocationId(), staticMappings.get(idPrefix)));
				vo.setLocationId(staticMappings.get(idPrefix));
				vo.setLocationName(casLocns.get(vo.getLocationId())); //use the mapped ID to find the actual location name
			} else {
				log.debug(String.format("need location %s", vo.getLocationId()));
				locations.put(vo.getLocationId(), vo);
			}
			//System.err.println(idPrefix + "\t" + vo.getLocationName() + "\t" + casId)
		}

		if (!locations.isEmpty())
			writeToDB(new ArrayList<>(locations.values()));
	}


	private Map<String, String> getProviderLocations() {
		Map<String, String> locns = new HashMap<>();
		String sql = StringUtil.join("select location_id, location_nm from ", schema, "wsla_provider_location where ",
				"provider_id in (select provider_id from ", schema, "wsla_provider where provider_type_id='CAS')");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				locns.put(rs.getString(1), rs.getString(2));

		} catch (SQLException sqle) {
			log.error("could not load CAS location list", sqle);
		}
		return locns;
	}


	/**
	 * Transpose and enhance the data we get from the import file into what the new schema needs
	 * @param dataVo
	 * @param vo
	 * @return
	 */
	private ProviderLocationVO transposeProviderData(SWLocFileVO dataVo, ProviderLocationVO vo) {
		vo.setLocationId(dataVo.getLoc());
		vo.setProviderId("WSLA_PROVIDER");
		vo.setLocationName(StringUtil.checkVal(dataVo.getName(), dataVo.getLoc()));
		vo.setCity(dataVo.getCity());
		vo.setState(dataVo.getState());
		vo.setZipCode(dataVo.getZip());
		vo.setCountry("MX");
		vo.setActiveFlag(1);
		vo.setReviewFlag(1);
		vo.setCreateDate(Convert.getCurrentTimestamp());
		return vo;
	}
}