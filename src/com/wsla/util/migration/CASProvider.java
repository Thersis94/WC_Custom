package com.wsla.util.migration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.util.StringUtil;
import com.wsla.data.provider.ProviderType;
import com.wsla.data.provider.ProviderVO;
import com.wsla.util.migration.vo.CASFileVO;

/****************************************************************************
 * <p><b>Title:</b> CASProvider.java</p>
 * <p><b>Description:</b> </p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Jan 9, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class CASProvider extends AbsImporter {

	private static final String[] WSLA_CAS_IDS = new String[]{ "000000","000001","000002" };
	private List<CASFileVO> data;

	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbstractImporter#run()
	 */
	@Override
	void run() throws Exception {
		data = readFile(props.getProperty("casFile"), CASFileVO.class, SHEET_1);

		String sql = StringUtil.join("delete from ", schema, "wsla_provider where provider_type_id='CAS'");
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
		Map<String, ProviderVO> providers = new HashMap<>(data.size());
		for (CASFileVO dataVo : data) {
			boolean isWSLA = StringUtil.stringContainsItem(dataVo.getCasId(), WSLA_CAS_IDS);
			if (isWSLA || providers.containsKey(dataVo.getCasId())) continue;

			ProviderVO vo = transposeProviderData(dataVo, new ProviderVO());
			providers.put(vo.getProviderId(), vo);
		}

		writeToDB(new ArrayList<>(providers.values()));
	}


	/**
	 * Transpose and enhance the data we get from the import file into what the new schema needs
	 * @param dataVo
	 * @param providerVO
	 * @return
	 */
	private ProviderVO transposeProviderData(CASFileVO dataVo, ProviderVO vo) {
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
}