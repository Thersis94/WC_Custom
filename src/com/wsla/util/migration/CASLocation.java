package com.wsla.util.migration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.gis.MatchCode;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.user.LocationManager;
import com.wsla.data.provider.AuthorizedServiceProviderVO;
import com.wsla.data.provider.ProviderLocationVO;
import com.wsla.util.migration.vo.CASFileVO;

/****************************************************************************
 * <p><b>Title:</b> CASLocation.java</p>
 * <p><b>Description:</b> </p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Jan 9, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class CASLocation extends AbsImporter {

	private static final String[] WSLA_CAS_IDS = new String[]{ "000000","000001","000002" };
	private List<CASFileVO> data;
	private Map<String, Object> config = new HashMap<>(); //geocoder wants a Map, not a Properties object
	

	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbstractImporter#run()
	 */
	@Override
	void run() throws Exception {
		for (String name : props.stringPropertyNames())
			    config.put(name, props.getProperty(name));
		
		data = readFile(props.getProperty("casFile"), CASFileVO.class, SHEET_1);

		String sql = StringUtil.join("delete from ", schema, "wsla_provider_location ",
				"where provider_id in (select provider_id from custom.wsla_provider where provider_type_id='CAS')");
		delete(sql);
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
		for (CASFileVO dataVo : data) {
			boolean isWSLA = StringUtil.stringContainsItem(dataVo.getCasId(), WSLA_CAS_IDS);
			if (isWSLA || locations.containsKey(dataVo.getCasId())) continue;

			ProviderLocationVO vo = transposeProviderData(dataVo, new ProviderLocationVO());
			locations.put(vo.getProviderId(), vo);
		}

		writeToDB(new ArrayList<>(locations.values()));
		
		createAuthSvcCenters(locations.values());
	}


	/**
	 * Bind all legacy CAS to the Flat Panel TV category - we know they service TVs (in a legacy role).
	 * @param locations
	 * @throws Exception 
	 */
	private void createAuthSvcCenters(Collection<ProviderLocationVO> locations) throws Exception {
		List<AuthorizedServiceProviderVO> ctrs = new ArrayList<>(locations.size());
		for (ProviderLocationVO loc : locations) {
			AuthorizedServiceProviderVO vo = new AuthorizedServiceProviderVO();
			vo.setProductCategoryId("FTP"); //FlaT Panel tv
			vo.setCertifiedFlag(1);
			vo.setLocationId(loc.getLocationId());
			vo.setCreateDate(Convert.getCurrentTimestamp());
			ctrs.add(vo);
		}
		writeToDB(ctrs);
	}


	/**
	 * Transpose and enhance the data we get from the import file into what the new schema needs
	 * @param dataVo
	 * @param vo
	 * @return
	 */
	private ProviderLocationVO transposeProviderData(CASFileVO dataVo, ProviderLocationVO vo) {
		vo.setDefaultFlag(1);
		vo.setLocationId(dataVo.getCasId());
		vo.setProviderId(dataVo.getCasId());
		vo.setStatus(dataVo.getStatusEnum());
		vo.setLocationName(StringUtil.checkVal(dataVo.getCasName(), dataVo.getCasId()));
		vo.setAddress(dataVo.getAddress());
		vo.setAddress2(dataVo.getAddress2());
		vo.setCity(dataVo.getCity());
		vo.setState(dataVo.getState());
		//vo.setZipCode(dataVo.getZip());  //Google does better without zips
		
		if (StringUtil.checkVal(vo.getCity()).matches("(?i)(.*)COSTA RICA$")) {
			vo.setCountry("CR");
		} else {
			vo.setCountry("MX");
		}

		//active flg - based on status
		vo.setActiveFlag(dataVo.getActive());

		//review flg - set to 1 if status isn't rejected, authorized, or signed contract
		//TODO need to bring status over for CAS Locations
		if (!"5. WS-NOAUTOCAS".equalsIgnoreCase(dataVo.getStatus()) 
				&& !"6. WS-SI AUTOCAS".equalsIgnoreCase(dataVo.getStatus()) 
				&&  !"9. WS-CONTRATFIRM".equalsIgnoreCase(dataVo.getStatus()))
			vo.setReviewFlag(1);

		//perform geocode on active locations only
//		if (vo.getActiveFlag() == 1 && vo.getReviewFlag() == 0) {
			geocode(vo);
//		} else {
//			vo.setState(StringUtil.truncate(dataVo.getState(), 5));
//			vo.setZipCode(dataVo.getZip());
//		}

		vo.setCreateDate(dataVo.getUpdateDate());
		return vo;
	}


	/**
	 * Call the geocoder to process this address
	 * @param vo
	 */
	private void geocode(ProviderLocationVO vo) {
		LocationManager lm = new LocationManager(vo);
		GeocodeLocation gl = lm.geocode(config, Boolean.FALSE, Boolean.FALSE);
		//any geocode not matched to address level gets flagged for manual review
		if (!gl.isSuccessfulGeocode(MatchCode.address))
			vo.setReviewFlag(1);
		
		vo.setLatitude(gl.getLatitude());
		vo.setLongitude(gl.getLongitude());
		vo.setAddress(lm.getAddress());
		vo.setAddress2(lm.getAddress2());
		vo.setCity(lm.getCity());
		vo.setState(StringUtil.truncate(gl.getState(), 5)); //take directly from the geoLoc, LM does weird things for Intl states
		vo.setZipCode(lm.getZipCode());
		vo.setCountry(lm.getCountry());
		vo.setMatchCode(gl.getMatchCode());
	}
}