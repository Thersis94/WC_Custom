package com.wsla.util.migration;

import java.util.List;

import com.wsla.util.migration.vo.SOExtendedFileVO;

/****************************************************************************
 * <p><b>Title:</b> SOLineItems.java</p>
 * <p><b>Description:</b> </p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Feb 1, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class SOExtendedData extends AbsImporter {

	private List<SOExtendedFileVO> data;

	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbstractImporter#run()
	 */
	@Override
	void run() throws Exception {
		data = readFile(props.getProperty("soExtendedDataFile"), SOExtendedFileVO.class, SHEET_1);

		//		String sql = StringUtil.join("delete from ", schema, "wsla_provider where provider_type_id='CAS'");
		//		delete(sql);
		log.debug(data.size());
		//		save();
	}


	/**
	 * Save the imported providers to the database.
	 * @param data
	 * @throws Exception 
	 */
	@Override
	protected void save() throws Exception {
		//turn the list of data into a unique list of providers we write to the database
		//		Map<String, ProviderVO> providers = new HashMap<>(data.size());
		//		for (SOLineItemFileVO dataVo : data) {
		//			boolean isWSLA = StringUtil.stringContainsItem(dataVo.getCasId(), WSLA_CAS_IDS);
		//			if (isWSLA || providers.containsKey(dataVo.getCasId())) continue;
		//
		//			ProviderVO vo = transposeProviderData(dataVo, new ProviderVO());
		//			providers.put(vo.getProviderId(), vo);
		//		}
		//
		//		writeToDB(new ArrayList<>(providers.values()));
	}


	/**
	 * Transpose and enhance the data we get from the import file into what the new schema needs
	 * @param dataVo
	 * @param providerVO
	 * @return
	private ProviderVO transposeProviderData(SOLineItemFileVO dataVo, ProviderVO vo) {
		vo.setProviderId(dataVo.getCasId());
		vo.setProviderName(StringUtil.checkVal(dataVo.getCasName(), vo.getProviderId()));
		vo.setProviderType(ProviderType.CAS);

		String rawPhone = StringUtil.removeNonNumeric(dataVo.getContactPhone());
		if (rawPhone == null || rawPhone.length() != 10) {
			//phone isn't desired length.  Leave it alone and flag for an admin to review it.
			vo.setPhoneNumber(dataVo.getContactPhone());
			vo.setReviewFlag(1);
		} else {
			//phone is perfect, capture it sans any formatting from the old system
			vo.setPhoneNumber(rawPhone);
		}
		vo.setCreateDate(dataVo.getUpdateDate());
		return vo;
	}
	 */
}
