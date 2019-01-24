package com.wsla.util.migration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.wsla.data.product.ProductCategoryAssociationVO;
import com.wsla.data.product.ProductCategoryVO;
import com.wsla.util.migration.vo.CategoryVO;
import com.wsla.util.migration.vo.InventoryFileVO;

/****************************************************************************
 * <p><b>Title:</b> CategoryImporter.java</p>
 * <p><b>Description:</b> </p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Jan 10, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class Category extends AbsImporter {

	private List<CategoryVO> data;
	private List<InventoryFileVO> products;

	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbstractImporter#run()
	 */
	@Override
	void run() throws Exception {
		products = readFile(props.getProperty("inventoryFile"), InventoryFileVO.class, SHEET_1);
		data = readFile(props.getProperty("inventoryFile"), CategoryVO.class, SHEET_3);

		save();
	}


	/**
	 * Save the imported data to the database.
	 * @param data
	 * @throws Exception 
	 */
	@Override
	protected void save() throws Exception {
		saveCategories();
		saveProductXR();
	}


	private void saveCategories() throws Exception {
		String sql = StringUtil.join("delete from ", schema, "wsla_product_category");
		delete(sql);

		//turn the list of data into a unique list of providers we write to the database
		Map<String, ProductCategoryVO> cats = new HashMap<>(data.size());
		for (CategoryVO dataVo : data) {
			if (cats.containsKey(dataVo.getCatId())) continue;

			ProductCategoryVO vo = transposeProviderData(dataVo, new ProductCategoryVO());
			cats.put(vo.getProductCategoryId(), vo);
		}

		writeToDB(new ArrayList<>(cats.values()));
	}


	private void saveProductXR() throws Exception {
		//these were automatically deleted by referential integrity

		//turn the list of data into a unique list of providers we write to the database
		Map<String, ProductCategoryAssociationVO> xrs = new HashMap<>(data.size());
		for (InventoryFileVO dataVo : products) {
			if (StringUtil.isEmpty(dataVo.getCategoryNm()) || StringUtil.isEmpty(dataVo.getProductId())) continue;

			//duplicates here will automatically disappear thanks to the Map
			ProductCategoryAssociationVO vo = transposeXRData(dataVo, new ProductCategoryAssociationVO());
			xrs.put(vo.getCategoryAssociationId(), vo);
		}

		writeToDB(new ArrayList<>(xrs.values()));
	}


	/**
	 * @param dataVo
	 * @param productCategoryAssociationVO
	 * @return
	 */
	private ProductCategoryAssociationVO transposeXRData( InventoryFileVO dataVo, ProductCategoryAssociationVO vo) {
		vo.setProductId(dataVo.getProductId());
		vo.setProductCategoryId(dataVo.getCategoryNm());
		vo.setCategoryAssociationId(vo.getProductId() + "_" + vo.getProductCategoryId());
		vo.setCreateDate(Convert.getCurrentTimestamp());
		return vo;
	}


	/**
	 * Transpose and enhance the data we get from the import file into what the new schema needs
	 * @param dataVo
	 * @param categoryVo
	 * @return
	 */
	private ProductCategoryVO transposeProviderData(CategoryVO dataVo, ProductCategoryVO vo) {
		vo.setActiveFlag(1);
		vo.setProductCategoryId(dataVo.getCatId());
		vo.setCategoryCode(dataVo.getDescription());
		vo.setCreateDate(Convert.getCurrentTimestamp());
		return vo;
	}

}