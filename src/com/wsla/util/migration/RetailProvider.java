package com.wsla.util.migration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.wsla.data.provider.ProviderType;
import com.wsla.data.provider.ProviderVO;
import com.wsla.util.migration.vo.RetailerFileVO;

/****************************************************************************
 * <p><b>Title:</b> RetailProvider.java</p>
 * <p><b>Description:</b> Import a static list of Retailers (not retail locations) 
 * emailed by Steve (01/17/19).</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Jan 23, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class RetailProvider extends AbsImporter {

	private List<RetailerFileVO> data;

	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbstractImporter#run()
	 */
	@Override
	void run() throws Exception {
		data = readFile(props.getProperty("retailerFile"), RetailerFileVO.class, SHEET_1);

		String sql = StringUtil.join("delete from ", schema, "wsla_provider where provider_type_id='RETAILER'");
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
		for (RetailerFileVO dataVo : data) {
			if (providers.containsKey(dataVo.getProviderId())) continue;

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
	private ProviderVO transposeProviderData(RetailerFileVO dataVo, ProviderVO providerVO) {
		//prefix the IDs with the codes we were given - so we have them if downstream references are needed.
		//also uppercase and replace spaces with underscores for consistency.
		providerVO.setProviderId(dataVo.getCd().trim() + "_" + StringUtil.checkVal(dataVo.getProviderId()).toUpperCase().trim().replaceAll("\\s", "_"));
		//the names were provided in uppercase, but smooth them out to be safe and remove any whitespace.
		providerVO.setProviderName(StringUtil.checkVal(dataVo.getProviderName()).toUpperCase().trim());
		providerVO.setProviderType(ProviderType.RETAILER);
		providerVO.setCreateDate(Convert.getCurrentTimestamp());
		return providerVO;
	}
}