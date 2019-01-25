package com.wsla.util.migration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.wsla.data.product.WarrantyType;
import com.wsla.data.product.WarrantyVO;
import com.wsla.data.provider.ProviderType;
import com.wsla.data.provider.ProviderVO;
import com.wsla.util.migration.vo.InventoryFileVO;

/****************************************************************************
 * <p><b>Title:</b> ProviderImporter.java</p>
 * <p><b>Description:</b> </p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Jan 9, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class OEMProvider extends AbsImporter {

	private List<InventoryFileVO> data;

	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbstractImporter#run()
	 */
	@Override
	void run() throws Exception {
		data = readFile(props.getProperty("inventoryFile"), InventoryFileVO.class, SHEET_1);

		String sql = StringUtil.join("delete from ", schema, "wsla_provider where provider_type_id='OEM'");
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
		for (InventoryFileVO dataVo : data) {
			if (providers.containsKey(dataVo.getProviderId())) continue;

			ProviderVO vo = transposeProviderData(dataVo, new ProviderVO());
			providers.put(vo.getProviderId(), vo);
		}

		writeToDB(new ArrayList<>(providers.values()));

		createManufWarranties(providers.values());
	}


	/**
	 * Create a default manufacturer warranty for each OEM
	 * @param providers (OEMs)
	 * @throws Exception 
	 */
	private void createManufWarranties(Collection<ProviderVO> providers) throws Exception {
		List<WarrantyVO> warrs = new ArrayList<>(providers.size());
		for (ProviderVO prov : providers) {
			WarrantyVO vo = new WarrantyVO();
			vo.setProviderId(prov.getProviderId());
			vo.setWarrantyType(WarrantyType.MANUFACTURER);
			vo.setCreateDate(Convert.getCurrentTimestamp());
			warrs.add(vo);
		}
		writeToDB(warrs);
	}


	/**
	 * Transpose and enhance the data we get from the import file into what the new schema needs
	 * @param dataVo
	 * @param providerVO
	 * @return
	 */
	private ProviderVO transposeProviderData(InventoryFileVO dataVo, ProviderVO providerVO) {
		providerVO.setProviderId(dataVo.getProviderId());
		providerVO.setProviderName(dataVo.getProviderId());
		providerVO.setProviderType(ProviderType.OEM);
		providerVO.setCreateDate(Convert.getCurrentTimestamp());
		return providerVO;
	}
}