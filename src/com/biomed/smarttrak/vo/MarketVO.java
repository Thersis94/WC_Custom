package com.biomed.smarttrak.vo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: MarketVO.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> DBProcessor enabled VO that stores all information about
 * a market.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Feb 6, 2017<p/>
 * <b>Changes: </b>
 ****************************************************************************/

@Table(name="BIOMEDGPS_MARKET")
public class MarketVO {
	
	private String marketId;
	private String parentId;
	private String marketName;
	private int orderNo;
	private String statusNo;
	private String shortName;
	private String aliasName;
	private List<MarketAttributeVO> attributes;
	private List<GenericVO> sections;
	
	public MarketVO () {
		attributes = new ArrayList<>();
		sections = new ArrayList<>();
	}
	
	
	public MarketVO(ActionRequest req) {
		super();
		setData(req);
	}
	

	private void setData(ActionRequest req) {
		marketId = req.getParameter("marketId");
		parentId = req.getParameter("parentId");
		marketName = req.getParameter("marketName");
		shortName = req.getParameter("shortName");
		aliasName = req.getParameter("aliasName");
		orderNo = Convert.formatInteger(req.getParameter("orderNo"));
		statusNo = req.getParameter("statusNo");
	}


	@Column(name="market_id", isPrimaryKey=true)
	public String getMarketId() {
		return marketId;
	}
	public void setMarketId(String marketId) {
		this.marketId = marketId;
	}
	@Column(name="parent_id")
	public String getParentId() {
		return parentId;
	}
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}
	@Column(name="market_nm")
	public String getMarketName() {
		return marketName;
	}
	public void setMarketName(String marketName) {
		this.marketName = marketName;
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
	@Column(name="short_nm")
	public String getShortName() {
		return shortName;
	}


	public void setShortName(String shortName) {
		this.shortName = shortName;
	}


	@Column(name="alias_nm")
	public String getAliasName() {
		return aliasName;
	}


	public void setAliasName(String aliasName) {
		this.aliasName = aliasName;
	}
	

	public List<MarketAttributeVO> getAttributes() {
		return attributes;
	}


	public void setAttributes(List<MarketAttributeVO> attributes) {
		this.attributes = attributes;
	}
	
	public void addAttribute(MarketAttributeVO attribute) {
		this.attributes.add(attribute);
	}

	public List<GenericVO> getSections() {
		return sections;
	}


	public void setSections(List<GenericVO> sections) {
		this.sections = sections;
	}
	
	public void addSection(GenericVO section) {
		this.sections.add(section);
	}


	// These functions exists only to give the DBProcessor a hook to autogenerate dates on
	@Column(name="UPDATE_DT", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDate() {return null;}
	@Column(name="CREATE_DT", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {return null;}

	
}
