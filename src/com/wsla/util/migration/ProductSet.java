package com.wsla.util.migration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.wsla.data.product.ProductSetVO;
import com.wsla.util.migration.vo.InventoryFileVO;

/****************************************************************************
 * <p><b>Title:</b> ProductSet.java</p>
 * <p><b>Description:</b> wsla_product_set loader.  Data is extrapolated from previously imported information.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Jan 22, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class ProductSet extends AbsImporter {

	private List<InventoryFileVO> data;
	Map<String, List<ProductSetVO>> sets = new HashMap<>(5000);

	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbstractImporter#run()
	 */
	@Override
	void run() throws Exception {
		data = readFile(props.getProperty("inventoryFile"), InventoryFileVO.class, SHEET_1);

		//don't need to call delete here, because replacing the products cascaded into this table

		//create the product_set entries now that we have parts and sets (complete TVs) loaded.
		load();
		combine();
		save();
	}


	/**
	 * Marry the PARTs in this excel file ("data") to their parent (completeUnit) loaded from the DB
	 * @return
	 */
	private void combine() {
		//loop the excel data
		for (InventoryFileVO dataVo : data) {
			String partId = StringUtil.checkVal(dataVo.getProductId());
			String setId = StringUtil.checkVal(dataVo.getDescription1());
			if (partId.isEmpty() || setId.isEmpty()) continue; //not enough info to bind to a set.  Omit.

			if (setId.indexOf(':') > -1) setId = separate(setId,":");
			else if (setId.indexOf(';') > -1) setId = separate(setId,";");

			setId = setId.trim();
			partId = partId.trim();

			//if setId=partId, this is a set and not a part.  Don't add it to itself.
			if (setId.equalsIgnoreCase(partId)) continue;

			//get the completeSet matching setId.  if none, drop a warning and move on
			List<ProductSetVO> parts = sets.get(setId);
			if (parts == null) { //this is not a set if it isn't on our list from the DB!
				log.warn("part is not a set: " + setId);
				continue;
			}

			ProductSetVO part = null;
			//try and find this part already tied to the product.  We'll increment qnty if defined more than once
			for (ProductSetVO vo : parts) {
				if (vo.getProductId().equals(partId)) {
					part = vo;
					break;
				}
			}
			if (part != null) {
				// For now I don't think we want to increment, it creates TVs with 5 remotes!
				//part.setQuantity(part.getQuantity()+1)
			} else {
				part = new ProductSetVO();
				part.setSetId(setId);
				part.setProductId(partId);
				part.setQuantity(1);
				part.setCreateDate(Convert.getCurrentTimestamp());
				parts.add(part);
			}
			sets.put(setId, parts);
		}
	}


	/**
	 * load all the product sets (complete units)
	 * @return
	 */
	private void load() {
		String sql = StringUtil.join("select distinct product_id from ", schema,"wsla_product_master where set_flg=1");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				sets.put(rs.getString(1), new ArrayList<>());

		} catch (SQLException sqle) {
			log.error("could not load product sets", sqle);
		}
	}


	/**
	 * Save the imported product_set records to the database.
	 * @param data
	 * @throws Exception 
	 */
	@Override
	protected void save() throws Exception {
		//turn the list of data into a unique list of providers we write to the database
		List<ProductSetVO> setList = new ArrayList<>(sets.size()*10);
		for (List<ProductSetVO> mapValue : sets.values())
			setList.addAll(mapValue);

		writeToDB(setList);
	}


	/**
	 * separate the setId from the rest of the description/noise
	 * @param setId
	 * @param tokenizer
	 * @return
	 */
	private String separate(String setId, String tokenizer) {
		String[] arr = setId.split(tokenizer);
		return arr.length > 0 ? arr[0] : setId;
	}
}