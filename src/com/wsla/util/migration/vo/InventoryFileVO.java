package com.wsla.util.migration.vo;

import java.util.regex.Pattern;

import com.siliconmtn.annotations.Importable;

/****************************************************************************
 * <p><b>Title:</b> InventoryLedgerVO.java</p>
 * <p><b>Description:</b> models DM-Inventory & Ledger.xlsx provided by Steve</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Jan 7, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class InventoryFileVO {

	private static final Pattern HARVEST_PTRN = Pattern.compile("(.*)(:|;)?\\s?(H|HARV|HARVESTED)$", Pattern.CASE_INSENSITIVE);

	String providerId;
	String productId;
	String description1;
	String description2;
	String description3;
	String bin;
	String locationId;
	String categoryNm;
	String qntyOnHand; //qnty available is all we want from the file

	public String getProviderId() {
		return providerId;
	}

	public String getProductId() {
		//if harvested, remove the harvested flag
		if (isHarvested()) {
			String pkId = HARVEST_PTRN.matcher(productId).replaceAll("$1").trim();
			if (pkId.endsWith(":") ||  pkId.endsWith(";")) return pkId.substring(0, pkId.length()-1);
			else return pkId;
		}
		return productId;
	}

	public String getDescription1() {
		//if harvested, remove the harvested flag
		if (isHarvested()) {
			String desc = HARVEST_PTRN.matcher(description1).replaceAll("$1").trim();
			if (desc.endsWith(":") ||  desc.endsWith(";")) return desc.substring(0, desc.length()-1);
			else return desc;
		}
		return description1;
	}

	public String getDescription2() {
		return description2;
	}

	public String getDescription3() {
		return description3;
	}

	public String getBin() {
		return bin;
	}

	public String getLocationId() {
		return locationId;
	}

	public String getCategoryNm() {
		return categoryNm;
	}

	public String getQntyOnHand() {
		return qntyOnHand;
	}

	@Importable(name="CLIENTE but really VENDOR#")
	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

	@Importable(name="SW PART NUMBER")
	public void setProductId(String productId) {
		this.productId = productId;
	}

	@Importable(name="Descripcion 1")
	public void setDescription1(String description1) {
		this.description1 = description1;
	}

	@Importable(name="Descripcion 2")
	public void setDescription2(String description2) {
		this.description2 = description2;
	}

	@Importable(name="CLIENTE PARTE # (Description 3)")
	public void setDescription3(String description3) {
		this.description3 = description3;
	}

	@Importable(name="BIN")
	public void setBin(String bin) {
		this.bin = bin;
	}

	@Importable(name="LOCATION")
	public void setLocationId(String locationId) {
		this.locationId = locationId;
	}

	@Importable(name="CAT")
	public void setCategoryNm(String categoryNm) {
		this.categoryNm = categoryNm;
	}

	@Importable(name="Qty Available")
	public void setQntyOnHand(String qntyOnHand) {
		this.qntyOnHand = qntyOnHand;
	}

	/**
	 * Harvested parts can be determined by triggers on the part# or description
	 * @return
	 */
	public boolean isHarvested() {
		return HARVEST_PTRN.matcher(productId).matches() || HARVEST_PTRN.matcher(description1).matches();
	}
}