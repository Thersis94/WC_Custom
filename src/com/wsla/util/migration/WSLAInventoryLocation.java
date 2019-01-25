package com.wsla.util.migration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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


	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbstractImporter#run()
	 */
	@Override
	void run() throws Exception {
		data = readFile(props.getProperty("swLocFile"), SWLocFileVO.class, SHEET_1);

		String sql = StringUtil.join("delete from ", schema, "wsla_provider_location ",
				"where provider_id='WSLA_PROVIDER'");
		delete(sql);
		// NOTE: We may need to create the provider!
		// INSERT INTO custom.wsla_provider (provider_id, provider_type_id, provider_nm, create_dt) 
		//	VALUES('WSLA_PROVIDER', 'WSLA', 'WSLA', current_timestamp)

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
			String casId;
//			log.debug("namePrefix=" + namePrefix + ", id=" + idPrefix);
			if (casLocns.containsKey(namePrefix)) {
//				log.debug(String.format("matched %s to CAS %s, %s", vo.getLocationId(), namePrefix, casLocns.get(namePrefix)));
				casId = namePrefix;
				vo.setLocationName(casLocns.get(namePrefix));
			} else if (casLocns.containsKey(idPrefix)) {
//				log.debug(String.format("matched %s to CAS %s, %s", vo.getLocationId(), idPrefix, casLocns.get(idPrefix)));
				casId = idPrefix;
				vo.setLocationName(casLocns.get(idPrefix));
			} else {
//				log.debug(String.format("WSLA Location? %s", vo.getLocationId()));
				casId = "";
			}
			System.err.println(idPrefix + "\t" + vo.getLocationName() + "\t" + casId);
			locations.put(vo.getLocationId(), vo);
		}

		//writeToDB(new ArrayList<>(locations.values()));
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