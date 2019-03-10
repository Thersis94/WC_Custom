package com.wsla.util.migration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.wsla.data.product.ProductSerialNumberVO;
import com.wsla.data.product.ProductVO;
import com.wsla.data.product.ProductWarrantyVO;
import com.wsla.util.migration.vo.ServiceOrderVO;

/****************************************************************************
 * <p><b>Title:</b> ProductSetNSerial.java</p>
 * <p><b>Description:</b> Uses the ServiceOrders excel file to ingest product sets (actual complete TVs), 
 * and their serial #s.  From here we can extrapolate warranties (didn't exist in old system, but required here).</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Jan 22, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class ProductSerial extends AbsImporter {

	private List<ServiceOrderVO> data;
	private Map<String, String> warranties = new HashMap<>(5000);

	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbstractImporter#run()
	 */
	@Override
	void run() throws Exception {
		//not sure why this is sheet 3, but that's what Excel says too.  (It looks like Sheet 2 to human eyes!)
		data = readFile(props.getProperty("soFile"), ServiceOrderVO.class, SHEET_3);

		//no deletes here, but the OEMProvider and Product importer must be run first!

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
		for (ServiceOrderVO dataVo : data) {
			if (products.containsKey(dataVo.getProductId())) continue;

			ProductVO vo = transposeData(dataVo, new ProductVO());
			if (!StringUtil.isEmpty(vo.getProductId()) && !StringUtil.isEmpty(vo.getProviderId())) {
				products.put(vo.getProductId(), vo);
			} else {
				log.debug("missing required data in record: " + dataVo);
			}
		}
		log.debug("found " + products.size() + " product sets");

		//query the DB, any that already exist we want to remove from our list, but also update to be sets.
		products = reconsileExistingSets(products);
		log.debug(products.size() + " product sets left to add");

		//save the products
		writeToDB(new ArrayList<>(products.values()));

		//save the serial#s
		Collection<ProductSerialNumberVO> serials = saveSerialNumbers();

		//save a default warranty to each serial#
		loadProductWarranties();
		saveProductWarranties(serials);
	}


	/**
	 * Batch update existing products to mark them as sets.  Remove any that were updated from the list.
	 * @param products
	 * @return a list of product sets to insert
	 */
	private Map<String, ProductVO> reconsileExistingSets(Map<String, ProductVO> products) {
		Map<String, ProductVO> newProducts = new HashMap<>(products);
		Set<String> productIds = products.keySet();
		String sql = StringUtil.join("update ", schema, "wsla_product_master set set_flg=1, update_dt=CURRENT_TIMESTAMP where product_id=?");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			for (String pkId : productIds) {
				ps.setString(1, pkId);
				log.debug(pkId);
				ps.addBatch();
			}
			int[] results = ps.executeBatch();
			log.debug(String.format("attempted %d, reported %d", productIds.size(), results.length));

			// Iterate the keys again
			// The int[] indexes will align and we can determine if the updates were successful, or if such a record doesn't exist.
			int x = 0;
			for (String pkId : productIds) {
				if (results.length > x && results[x] == 1)
					newProducts.remove(pkId);
				++x;
			}

		} catch (SQLException sqle) {
			log.error("could not update existing sets", sqle);
		}
		return newProducts;
	}


	/**
	 * Creates a unique set of serial#s and saves them to the DB
	 */
	private Collection<ProductSerialNumberVO> saveSerialNumbers() throws Exception {
		//turn the list of data into a unique list of providers we write to the database
		Map<String, ProductSerialNumberVO> serials = new HashMap<>(data.size());
		for (ServiceOrderVO dataVo : data) {
			//discard any empty or "all zeros" serial#s
			String serial = dataVo.getSerialNo();
			if (serial.matches("^0+$")) serial = null; //treat any zero or all-zero entries as N/A
			if (StringUtil.isEmpty(serial) || serials.containsKey(serial)) continue;

			ProductSerialNumberVO vo = transposeSerialData(dataVo, new ProductSerialNumberVO());
			if (!StringUtil.isEmpty(vo.getProductId()) && !StringUtil.isEmpty(vo.getSerialNumber())) {
				serials.put(vo.getProductSerialId(), vo);
			}
		}
		writeToDB(new ArrayList<>(serials.values()));
		return serials.values();
	}


	/**
	 * Populate the list of warranties from the DB
	 */
	private void loadProductWarranties() {
		String sql = StringUtil.join("select distinct provider_id, warranty_id from ", schema, "wsla_warranty");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				warranties.put(rs.getString(1), rs.getString(2));

		} catch (SQLException sqle) {
			log.error("could not load warranties", sqle);
		}
	}


	/**
	 * Create a default warranty binding for each product serial#
	 */
	private void saveProductWarranties(Collection<ProductSerialNumberVO> serials) throws Exception {
		List<ProductWarrantyVO> warrs = new ArrayList<>(serials.size());
		for (ProductSerialNumberVO serialNo : serials) {
			ProductWarrantyVO vo = new ProductWarrantyVO();
			vo.setWarrantyId(warranties.get(serialNo.getProviderId())); //pkId in wsla_warranty
			vo.setProductSerialId(serialNo.getProductSerialId()); //pkId in wsla_product_serial
			vo.setCreateDate(Convert.getCurrentTimestamp());
			warrs.add(vo);
		}
		writeToDB(warrs);
	}


	/**
	 * Transpose and enhance the data we get from the import file into what the new schema needs
	 * @param dataVo ServiceOrderVO
	 * @param vo ProductVO
	 * @return
	 */
	private ProductVO transposeData(ServiceOrderVO dataVo, ProductVO vo) {
		vo.setProductId(dataVo.getProductId());
		vo.setProviderId(dataVo.getOemId());
		vo.setProductName(dataVo.getProductId());
		vo.setSetFlag(1);
		vo.setActiveFlag(1);
		vo.setValidatedFlag(1);
		vo.setCreateDate(Convert.getCurrentTimestamp());
		return vo;
	}


	/**
	 * Transpose and enhance the data we get from the import file into what the new schema needs
	 * @param dataVo ServiceOrderVO
	 * @param vo ProductSerialNumberVO
	 * @return
	 */
	private ProductSerialNumberVO transposeSerialData(ServiceOrderVO dataVo, ProductSerialNumberVO vo) {
		vo.setProviderId(dataVo.getOemId()); //used for warranty binding
		vo.setProductId(dataVo.getProductId());
		vo.setSerialNumber(dataVo.getSerialNo());
		vo.setProductSerialId(dataVo.getSerialNo()); //if we can rely on these being unique, it makes data imports to downstream tables 100x easier!
		vo.setValidatedFlag(1);
		vo.setCreateDate(Convert.getCurrentTimestamp());
		return vo;
	}
}