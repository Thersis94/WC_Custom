package com.wsla.util.migration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.wsla.data.product.LocationItemMasterVO;
import com.wsla.util.migration.vo.InventoryFileVO;

/****************************************************************************
 * <p><b>Title:</b> LocationInventory.java</p>
 * <p><b>Description:</b> Location_item_master table.  From DM-Inventory & Ledger Excel file.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Feb 08, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class LocationInventory extends AbsImporter {

	private List<InventoryFileVO> data;

	private Map<String, String> locationIds = new HashMap<>(5000);

	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbstractImporter#run()
	 */
	@Override
	void run() throws Exception {
		data = readFile(props.getProperty("inventoryFile"), InventoryFileVO.class, SHEET_1);

		String sql = StringUtil.join("delete from ", schema, "wsla_location_item_master");
		delete(sql);

		//retrieve the locationIds
		locationIds.putAll(getProviderLocations());
		locationIds.putAll(WSLAInventoryLocation.staticMappings);
		//add a few manual anomalies
		locationIds.put("GD1", "GDL100");
		locationIds.put("GD2", "GDL200");
		locationIds.put("GD3", "GDL300");
		locationIds.put("GD4", "GDL400");
		locationIds.put("RE2", "RED200");
		
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
		Map<String, LocationItemMasterVO> inventory = new HashMap<>(data.size());
		for (InventoryFileVO dataVo : data) {
			setLocationId(dataVo);
			if (!locationIds.containsKey(dataVo.getLocationId())) {
				log.warn("inventory location does not exist: " + dataVo.getLocationId());
				continue;
			}
			String unqKey = dataVo.getProductId() + dataVo.getLocationId();
			LocationItemMasterVO vo;
			if (inventory.containsKey(unqKey)) {
				vo = inventory.get(unqKey);
				vo.setQuantityOnHand(vo.getQuantityOnHand() + Convert.formatInteger(dataVo.getQntyOnHand(),0));
			} else {
				vo = transposeInventoryData(dataVo, new LocationItemMasterVO());
			}

			//no point in saving empty records - drop any with zero inventory
			if (vo.getQuantityOnHand() > 0)
				inventory.put(unqKey, vo);
		}
		log.debug(String.format("loaded %d inventory records", inventory.size()));

		if (!inventory.isEmpty())
			writeToDB(new ArrayList<>(inventory.values()));

		// Copy inventory count into the "original_" column for archival.
		// Easier to run a second query than modify code in a confusing way (no UI alignment).
		String sql = StringUtil.join("update ", schema, "wsla_location_item_master ",
				"set original_qnty_no=actual_qnty_no where original_qnty_no is null or original_qnty_no=0");
		new DBProcessor(dbConn, schema).executeSQLCommand(sql);
	}


	/**
	 * Attempt to put an actual locationId to the string found in the CSV file.
	 * @param dataVo
	 */
	private void setLocationId(InventoryFileVO dataVo) {
		//first confirm the correct value isn't already set - this is almost never the case
		//taking the value here so our manual entries (above) also align
		if (locationIds.containsKey(dataVo.getLocationId())) {
			dataVo.setLocationId(locationIds.get(dataVo.getLocationId()));
			return;
		}

		//iterate the keys to see if one starts with our value.  Match if so
		for (String id : locationIds.keySet()) {
			if (id.startsWith(dataVo.getLocationId())) {
				log.debug(String.format("matched %s to %s", id, dataVo.getLocationId()));
				dataVo.setLocationId(id);
				return;
			}
		}
	}


	private Map<String, String> getProviderLocations() {
		Map<String, String> locns = new HashMap<>();
		String sql = StringUtil.join("select location_id from ", schema, "wsla_provider_location");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				locns.put(rs.getString(1), rs.getString(1));

		} catch (SQLException sqle) {
			log.error("could not load location list", sqle);
		}
		return locns;
	}


	/**
	 * Transpose and enhance the data we get from the import file into what the new schema needs
	 * @param dataVo
	 * @param vo
	 * @return
	 */
	private LocationItemMasterVO transposeInventoryData(InventoryFileVO dataVo, LocationItemMasterVO vo) {
		vo.setProductId(dataVo.getProductId());
		vo.setLocationId(dataVo.getLocationId());
		vo.setQuantityOnHand(Convert.formatInteger(dataVo.getQntyOnHand(), 0));
		vo.setCreateDate(Convert.getCurrentTimestamp());
		return vo;
	}
}