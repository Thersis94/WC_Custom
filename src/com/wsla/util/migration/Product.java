package com.wsla.util.migration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.wsla.data.product.ProductVO;
import com.wsla.util.migration.vo.InventoryFileVO;

/****************************************************************************
 * <p><b>Title:</b> ProductImporter.java</p>
 * <p><b>Description:</b> </p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Jan 9, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class Product extends AbsImporter {

	private List<InventoryFileVO> data;

	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbstractImporter#run()
	 */
	@Override
	void run() throws Exception {
		data = readFile(props.getProperty("inventoryFile"), InventoryFileVO.class, SHEET_1);

		//don't need to call delete here, because replacing the providers cascaded into this table

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
		Map<String, ProductVO> products = new HashMap<>(data.size());
		for (InventoryFileVO dataVo : data) {
			 if (products.containsKey(dataVo.getProductId())) continue;

			ProductVO vo = transposeData(dataVo, new ProductVO());
			products.put(vo.getProductId(), vo);
		}

		writeToDB(new ArrayList<>(products.values()));
	}


	/**
	 * Transpose and enhance the data we get from the import file into what the new schema needs
	 * @param dataVo
	 * @param providerVO
	 * @return
	 */
	private ProductVO transposeData(InventoryFileVO dataVo, ProductVO vo) {
		vo.setProductId(dataVo.getProductId());
		vo.setProviderId(dataVo.getProviderId());
		vo.setProductName(StringUtil.checkVal(dataVo.getDescription1(), dataVo.getProductId()));
		
		String desc = StringUtil.checkVal(dataVo.getDescription2());
		if (!StringUtil.isEmpty(desc) && !StringUtil.isEmpty(dataVo.getDescription3()))
			desc += ", ";
		desc += StringUtil.checkVal(dataVo.getDescription3());
		if (desc.isEmpty()) desc = null;
		vo.setDescription(desc);
		
		vo.setSetFlag(0);
		boolean isInactive = vo.getProductName().matches("(?i)(.*)(NO USAR|DO NOT USE)(.*)");
		if (isInactive) {
			log.debug("INACTIVE: " + vo.getProductName());
		}
		vo.setActiveFlag(isInactive ? 0 : 1);
		vo.setValidatedFlag(1);
		vo.setCreateDate(Convert.getCurrentTimestamp());
		return vo;
	}
}