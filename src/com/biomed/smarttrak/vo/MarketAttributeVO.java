package com.biomed.smarttrak.vo;

import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

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
	private String marketAttributeGroupId;
	private String attributeId;
	private String marketId; 
	private String titleText;
	private String valueText;
	private String value1Text;
	private String value2Text;
	private String value3Text;
	private String attributeTypeCd;
	private String attributeName;
	private String marketName;
	private String groupName;
	private int orderNo;
	private String statusNo;
	private String revisionNote;
	private Date createDt;
	private Date updateDt;
	private int hasArchives;
	private String authorNm;
	
	public MarketAttributeVO() {
		// Empty default constructor
	}
	
	
	public MarketAttributeVO(ActionRequest req) {
		setData(req);
	}
	
	
	public void setData(ActionRequest req) {
		marketAttributeId = req.getParameter("marketAttributeId");
		marketAttributeGroupId = req.getParameter("marketAttributeGroupId");
		marketId = req.getParameter("marketId");
		titleText = req.getParameter("titleText");
		attributeId = req.getParameter("attributeId");
		valueText = req.getParameter("valueText");
		statusNo = req.getParameter("statusNo");
		setValue1Text(req.getParameter("value1Text"));
		setValue2Text(StringUtil.getDelimitedList(req.getParameterValues("value2Text"), false, ","));
		setValue3Text(req.getParameter("value3Text"));
		orderNo = Convert.formatInteger(req.getParameter("orderNo"));
		revisionNote = req.getParameter("revisionNote");
		authorNm = req.getParameter("authorName");
	}


	@Column(name="market_attribute_id", isPrimaryKey=true)
	public String getMarketAttributeId() {
		return marketAttributeId;
	}
	public void setMarketAttributeId(String marketAttributeId) {
		this.marketAttributeId = marketAttributeId;
	}
	
	/**
	 * @return the marketAttributeGroupId
	 */
	@Column(name="market_attribute_group_id")
	public String getMarketAttributeGroupId() {
		return marketAttributeGroupId;
	}


	/**
	 * @param marketAttributeGroupId the marketAttributeGroupId to set
	 */
	public void setMarketAttributeGroupId(String marketAttributeGroupId) {
		this.marketAttributeGroupId = marketAttributeGroupId;
	}
	
	@Column(name="market_id")
	public String getMarketId() {
		return marketId;
	}
	public void setMarketId(String marketId) {
		this.marketId = marketId;
	}


	@Column(name="title_txt")
	public String getTitleText() {
		return titleText;
	}


	public void setTitleText(String titleText) {
		this.titleText = titleText;
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

	@Column(name="value_2_txt")
	public String getValue2Text() {
		return value2Text;
	}
	
	public void setValue1Text(String value1Text) {
		this.value1Text = value1Text;
	}

	public void setValue2Text(String value2Text) {
		this.value2Text = value2Text;
	}

	/**
	 * @return the value3Text
	 */
	@Column(name="value_3_txt")
	public String getValue3Text() {
		return value3Text;
	}


	/**
	 * @param value3Text the value3Text to set
	 */
	public void setValue3Text(String value3Text) {
		this.value3Text = value3Text;
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


	@Column(name="status_no")
	public String getStatusNo() {
		return statusNo;
	}


	public void setStatusNo(String statusNo) {
		this.statusNo = statusNo;
	}
	

	@Column(name="UPDATE_DT", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDate() {return updateDt;}
	public void setUpdateDate(Date updateDt) {this.updateDt = updateDt;}

	@Column(name="CREATE_DT", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {return createDt;}
	public void setCreateDate(Date createDt) {this.createDt = createDt;}


	/**
	 * @return the revisionNote
	 */
	@Column(name="REVISION_NOTE")
	public String getRevisionNote() {
		return revisionNote;
	}


	/**
	 * @param revisionNote the revisionNote to set
	 */
	public void setRevisionNote(String revisionNote) {
		this.revisionNote = revisionNote;
	}

	/**
	 * @return the hasArchives
	 */
	@Column(name="has_archives", isReadOnly=true)
	public int getHasArchives() {
		return hasArchives;
	}

	/**
	 * @param hasArchives the hasArchives to set.
	 */
	public void setHasArchives(int hasArchives) {
		this.hasArchives = hasArchives;
	}

	@Column(name="author_nm")
	public String getAuthorNm() {
		return authorNm;
	}

	public void setAuthorNm(String authorNm) {
		this.authorNm = authorNm;
	}


}
