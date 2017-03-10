package com.biomed.smarttrak.vo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.biomed.smarttrak.util.MarketIndexer;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.annotations.SolrField;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.util.solr.SecureSolrDocumentVO;

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
public class MarketVO extends SecureSolrDocumentVO {
	private String marketId;
	private String parentId;
	private String marketName;
	private int orderNo;
	private String statusNo;
	private String shortName;
	private String aliasName;
	private List<MarketAttributeVO> marketAttributes;
	private List<MarketAttributeVO> graphs;
	private List<SectionVO> sections;
	private SectionVO marketSection;
	private String updateMsg;
	private Date updateDate;
	private String regionCode;

	public MarketVO () {
		super(MarketIndexer.INDEX_TYPE);
		marketAttributes = new ArrayList<>();
		sections = new ArrayList<>();
		graphs = new ArrayList<>();
	}


	public MarketVO(ActionRequest req) {
		this();
		setData(req);
	}


	private void setData(ActionRequest req) {
		marketId = req.getParameter("marketId");
		parentId = StringUtil.checkVal(req.getParameter("parentId"), null);
		marketName = req.getParameter("marketName");
		shortName = req.getParameter("shortName");
		aliasName = req.getParameter("aliasName");
		orderNo = Convert.formatInteger(req.getParameter("orderNo"));
		statusNo = req.getParameter("statusNo");
		setRegionCode(req.getParameter("regionCode"));
		marketSection = new SectionVO(req);
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

	@Column(name="status_no")
	public String getStatusNo() {
		return statusNo;
	}
	public void setStatusNo(String statusNo) {
		this.statusNo = statusNo;
	}
	

	@SolrField(name=SearchDocumentHandler.META_KEYWORDS)
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
		return marketAttributes;
	}


	public void setMarketAttributes(List<MarketAttributeVO> attributes) {
		this.marketAttributes = attributes;
	}

	public void addMarketAttribute(MarketAttributeVO attribute) {
		marketAttributes.add(attribute);
	}

	public List<MarketAttributeVO> getGraphs() {
		return graphs;
	}


	public void setGraphs(List<MarketAttributeVO> graphs) {
		this.graphs = graphs;
	}


	public void addGraph(MarketAttributeVO graph) {
		graphs.add(graph);
	}


	public List<SectionVO> getMarketSections() {
		return sections;
	}


	public void setSections(List<SectionVO> sections) {
		this.sections = sections;
	}

	public void addSection(SectionVO section) {
		sections.add(section);
	}

	/**
	 * Turn the list of generic vos
	 * @return
	 */
	@SolrField(name=SearchDocumentHandler.HIERARCHY)
	public List<String>getSectionNames() {
		List<String> nameList = new ArrayList<>();
		for (SectionVO vo : sections) {
			nameList.add(vo.getSectionNm());
		}
		return nameList;
	}

	public SectionVO getMarketSection() {
		return marketSection;
	}


	public void setMarketSection(SectionVO marketSection) {
		this.marketSection = marketSection;
	}


	@Override
	@SolrField(name=SearchDocumentHandler.UPDATE_DATE)
	@Column(name="UPDATE_DT", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDt() {
		return updateDate;
	}

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


	@Column(name="region_cd")
	public String getRegionCode() {
		return regionCode;
	}


	public void setRegionCode(String regionCode) {
		this.regionCode = regionCode;
	}
}