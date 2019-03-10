package com.wsla.util.migration.vo;

import com.siliconmtn.annotations.Importable;
import com.siliconmtn.util.StringUtil;

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
public class CategoryVO {

	String catId;
	String locId;
	String description;
	boolean isSet;


	public String getCatId() {
		return catId;
	}
	public String getLocId() {
		return locId;
	}
	public String getDescription() {
		return description;
	}
	public boolean isSet() {
		return isSet;
	}

	@Importable(name="Cat")
	public void setCatId(String catId) {
		this.catId = catId;
	}

	@Importable(name="Loc")
	public void setLocId(String locId) {
		this.locId = locId;
	}

	@Importable(name="Description")
	public void setDescription(String description) {
		if (StringUtil.isEmpty(description)) return;
		this.description = description;
		determineSet();
	}


	/**
	 * determines whether products bound to this category are complete units ("TV SETS") 
	 */
	private void determineSet() {
		this.isSet = catId.matches("(?i)(.*)(FPT|GEN|GLF|LNB|PLA|)(.*)");
	}
}