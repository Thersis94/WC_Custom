package com.biomed.smarttrak.vo;

import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: MarketAttributeVO.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> DBProcessor enabled VO that stores information regarding
 * the types of attributes that can be assigned to a market.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Feb 2, 2017<p/>
 * <b>Changes: </b>
 ****************************************************************************/

@Table(name="BIOMEDGPS_MARKET_ATTRIBUTE_XR")
public class MarketAttributeVO {
	
	private String marketAttributeId;
	private String attributeId;
	private String marketId;
	private String valueText;
	private String value1Text;
	private String attributeTypeCd;
	private String attributeName;
	private String marketName;
	private String groupName;
	private int orderNo;
	
	public MarketAttributeVO() {
		// Empty default constructor
	}
	
	
	public MarketAttributeVO(ActionRequest req) {
		setData(req);
	}
	
	
	public void setData(ActionRequest req) {
		marketAttributeId = req.getParameter("marketAttributeId");
		marketId = req.getParameter("marketId");
		attributeId = req.getParameter("attributeId");
		valueText = req.getParameter("valueText");
		setValue1Text(req.getParameter("value1Text"));
		orderNo = Convert.formatInteger(req.getParameter("orderNo"));
	}


	@Column(name="market_attribute_id", isPrimaryKey=true)
	public String getMarketAttributeId() {
		return marketAttributeId;
	}
	public void setMarketAttributeId(String marketAttributeId) {
		this.marketAttributeId = marketAttributeId;
	}
	@Column(name="market_id")
	public String getMarketId() {
		return marketId;
	}
	public void setMarketId(String marketId) {
		this.marketId = marketId;
	}
	@Column(name="attribute_id")
	public String getAttributeId() {
		return attributeId;
	}
	public void setAttributeId(String attributeId) {
		this.attributeId = attributeId;
	}
	@Column(name="value_txt")
	public String getValueText() {
		return valueText;
	}
	
	public String getIcon() {
		switch (valueText) {
			case "PIE":
				return "fa-pie-chart";
			case "BAR":
				return "fa-bar-chart";
			case "COLUMN":
				return "fa-line-chart";
			default:
				return "";
		}
	}
	
	public void setValueText(String valueText) {
		this.valueText = valueText;
	}
	
	@Column(name="value_1_txt")
	public String getValue1Text() {
		return value1Text;
	}


	public void setValue1Text(String value1Text) {
		this.value1Text = value1Text;
	}


	@Column(name="type_cd", isReadOnly=true)
	public String getAttributeTypeCd() {
		return attributeTypeCd;
	}


	public void setAttributeTypeCd(String attributeTypeCd) {
		this.attributeTypeCd = attributeTypeCd;
	}


	@Column(name="attribute_nm", isReadOnly=true)
	public String getAttributeName() {
		return attributeName;
	}


	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	@Column(name="market_nm", isReadOnly=true)
	public String getMarketName() {
		return marketName;
	}


	public void setMarketName(String marketName) {
		this.marketName = marketName;
	}


	@Column(name="group_nm", isReadOnly=true)
	public String getGroupName() {
		return groupName;
	}


	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}


	@Column(name="order_no")
	public int getOrderNo() {
		return orderNo;
	}
	public void setOrderNo(int orderNo) {
		this.orderNo = orderNo;
	}
	

	// These functions exists only to give the DBProcessor a hook to autogenerate dates on
	@Column(name="UPDATE_DT", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDate() {return null;}
	@Column(name="CREATE_DT", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {return null;}


}
