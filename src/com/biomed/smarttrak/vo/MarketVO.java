package com.biomed.smarttrak.vo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.annotations.SolrField;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

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
public class MarketVO extends SolrDocumentVO {
	
	public static final String SOLR_INDEX = "BIOMEDGPS_MARKET";
	
	private String marketId;
	private String parentId;
	private String marketName;
	private int orderNo;
	private String statusNo;
	private String shortName;
	private String aliasName;
	private List<MarketAttributeVO> attributes;
	private List<GenericVO> sections;
	private String updateMsg;
	private Date updateDate;
	
	public MarketVO () {
		super(SOLR_INDEX);
		attributes = new ArrayList<>();
		sections = new ArrayList<>();
	}
	
	
	public MarketVO(ActionRequest req) {
		this();
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
		super.setDocumentId(marketId);
		this.marketId = marketId;
	}
	@Column(name="parent_id")
	public String getParentId() {
		return parentId;
	}
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}
	@SolrField(name=SearchDocumentHandler.TITLE)
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
	@SolrField(name=SearchDocumentHandler.CONTENT_TYPE)
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
	

	public List<MarketAttributeVO> getMarketAttributes() {
		return attributes;
	}


	public void setMarketAttributes(List<MarketAttributeVO> attributes) {
		this.attributes = attributes;
	}
	
	public void addMarketAttribute(MarketAttributeVO attribute) {
		this.attributes.add(attribute);
	}

	public List<GenericVO> getMarketSections() {
		return sections;
	}


	public void setSections(List<GenericVO> sections) {
		this.sections = sections;
	}
	
	public void addSection(GenericVO section) {
		this.sections.add(section);
	}
	
	/**
	 * Turn the list of generic vos
	 * @return
	 */
	@SolrField(name=SearchDocumentHandler.HIERARCHY)
	public List<String>getSectionNames() {
		List<String> nameList = new ArrayList<>();
		for (GenericVO vo : sections) {
			nameList.add((String)vo.getValue());
		}
		return nameList;
	}
	
	@Override
	@SolrField(name=SearchDocumentHandler.UPDATE_DATE)
	@Column(name="UPDATE_DT", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDt() {
		return updateDate;
	}
	
	@Column(name="UPDATE_DT", isAutoGen=true, isUpdateOnly=true)
	public void setUpdateDt(Date updateDate) {
		this.updateDate = updateDate;
	}


	// These functions exists only to give the DBProcessor a hook to autogenerate dates on
	@Column(name="CREATE_DT", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {return null;}


	public String getUpdateMsg() {
		return updateMsg;
	}


	public void setUpdateMsg(String updateMsg) {
		this.updateMsg = updateMsg;
	}

	
}