package com.rezdox.vo;

import java.util.Date;

import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title:</b> ProjectMaterialAttributeVO.java<br/>
 * <b>Description:</b> RezDox Treasure Box Item attribute - see data model.
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Feb 26, 2018
 ****************************************************************************/
@Table(name="REZDOX_TREASURE_ITEM_ATTRIBUTE")
public class InventoryAttributeVO {

	private String attributeId;
	private String treasureItemId;
	private String slugTxt;
	private String valueTxt;

	public InventoryAttributeVO() {
		super();
	}


	@Column(name="attribute_id", isPrimaryKey=true)
	public String getAttributeId() {
		return attributeId;
	}

	@Column(name="treasure_item_id")
	public String getTreasureItemId() {
		return treasureItemId;
	}

	@Column(name="slug_txt")
	public String getSlugTxt() {
		return slugTxt;
	}

	@Column(name="value_txt")
	public String getValueTxt() {
		return valueTxt;
	}

	@Column(name="create_dt", isInsertOnly=true)
	public Date getCreateDate() {
		return Convert.getCurrentTimestamp();
	}

	@Column(name="update_dt", isUpdateOnly=true)
	public Date getUpdateDate() {
		return Convert.getCurrentTimestamp();
	}


	public void setAttributeId(String attributeId) {
		this.attributeId = attributeId;
	}

	public void setTrreasureItemId(String treasureItemId) {
		this.treasureItemId = treasureItemId;
	}

	public void setSlugTxt(String slugTxt) {
		this.slugTxt = slugTxt;
	}

	public void setValueTxt(String valueTxt) {
		this.valueTxt = valueTxt;
	}
}