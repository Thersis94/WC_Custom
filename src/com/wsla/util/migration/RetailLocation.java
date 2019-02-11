package com.wsla.util.migration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.gis.MatchCode;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.wsla.data.provider.ProviderLocationVO;
import com.wsla.data.provider.ProviderLocationVO.Status;
import com.wsla.util.migration.vo.RetailerLocationVO;

/****************************************************************************
 * <p><b>Title:</b> RetailLocation.java</p>
 * <p><b>Description:</b> </p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Feb 11, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class RetailLocation extends CASLocation {

	private boolean reGeocode = true; //trigger to fix prior inserts that failed for SMT/Proxy reasons
	private  String providerId = "1_WALMART";
	private List<RetailerLocationVO> data;
	private Map<String, String> states = new HashMap<>(100);


	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbstractImporter#run()
	 */
	@Override
	void run() throws Exception {
		loadStates("MX");

		if (reGeocode) {
			reprocessFailures();
		} else {
			data = readFile(props.getProperty("retailLocFile"), RetailerLocationVO.class, SHEET_1);
			String sql = StringUtil.join("delete from ", schema, "wsla_provider_location ",
					"where provider_id='", providerId,"'");
			delete(sql);
			save();
		}
	}


	/**
	 * load failed records from DB, re-geocode, re-save to DB
	 */
	private void reprocessFailures() {
		String sql = StringUtil.join("select * from ", schema, "wsla_provider_location where provider_id=? and latitude_no=0");
		log.debug(sql);
		DBProcessor dp = new DBProcessor(dbConn, schema);
		List<ProviderLocationVO> records = dp.executeSelect(sql, Arrays.asList(providerId), new ProviderLocationVO());
		if (records.isEmpty()) return;

		for (ProviderLocationVO vo : records)
			geocode(vo);

		try {
			dp.executeBatch(records);
		} catch (DatabaseException de) {
			log.error("could not update geocoded locations", de);
		}
	}


	/**
	 * @param string
	 */
	private void loadStates(String countryCd) {
		String sql = "select state_cd, lower(state_nm) from state where country_cd=?";
		log.debug(sql);
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, countryCd);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				states.put(rs.getString(2), rs.getString(1));

		} catch (SQLException sqle) {
			log.error("could not load states for country=" + countryCd, sqle);
		}
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
		for (RetailerLocationVO dataVo : data) {
			ProviderLocationVO vo = transposeProviderData(dataVo, new ProviderLocationVO());
			locations.put(vo.getLocationId(), vo);
			//save every hundred to minimize data risk
			if (locations.size() == 100) {
				writeToDB(new ArrayList<>(locations.values()));
				locations.clear();
			}
		}

		writeToDB(new ArrayList<>(locations.values()));
	}


	/**
	 * Transpose and enhance the data we get from the import file into what the new schema needs
	 * @param dataVo
	 * @param vo
	 * @return
	 */
	private ProviderLocationVO transposeProviderData(RetailerLocationVO dataVo, ProviderLocationVO vo) {
		vo.setActiveFlag(1);
		vo.setStatus(Status.AUTHORIZED);
		vo.setLocationId("WALMART_MX_" + dataVo.getStore());
		vo.setStoreNumber(dataVo.getStore());
		vo.setProviderId(providerId);
		vo.setLocationName(StringUtil.truncate(dataVo.getName().replaceAll("\\s", " "), 100));
		vo.setAddress(dataVo.getAddress().replaceAll("\\s", " "));
		vo.setCity(dataVo.getCity());
		vo.setState(StringUtil.checkVal(states.get(dataVo.getState().toLowerCase()), dataVo.getState()));
		vo.setCountry("MX");
		geocode(vo);

		//put state back if it wasn't returned from Google
		if (StringUtil.isEmpty(vo.getCity()))
			vo.setCity(dataVo.getCity());
		if (StringUtil.isEmpty(vo.getState()))
			vo.setState(StringUtil.checkVal(states.get(dataVo.getState().toLowerCase()), dataVo.getState()));
		if (vo.getAddress().length() > 99)
			vo.setAddress(StringUtil.truncate(vo.getAddress(), 100));
		if (StringUtil.isEmpty(vo.getState()))
			vo.setState(vo.getCity()); //these are often the same, use to backfill Google's blanks
		if (vo.getState().length() > 4)
			vo.setState(StringUtil.truncate(StringUtil.checkVal(states.get(vo.getState().toLowerCase()), vo.getState()), 5));

		vo.setCreateDate(Convert.getCurrentTimestamp());
		//flag for review if address did not geocode fully
		vo.setReviewFlag(MatchCode.address == vo.getMatchCode() ? 0 : 1);

		return vo;
	}
}