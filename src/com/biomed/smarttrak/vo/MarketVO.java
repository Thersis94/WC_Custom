package com.biomed.smarttrak.vo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.action.AdminControllerAction.Section;
import com.biomed.smarttrak.util.MarketIndexer;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.annotations.SolrField;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.search.SearchDocumentHandler;

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
public class MarketVO extends AuthorVO {
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
	private Date createDate;
	private String regionCode;
	private String regionName;
	private int publicFlag;
	private int indentNo;
	
	//used to specify the associated region for ordering within Solr
	public enum RegionOrder{
		WW("WorldWide", 1), US("United States", 2), EU("Europe", 3), DE("Germany", 4), 
		CN("China", 5), FR("France", 6), IT("Italy", 7), ES("Spain", 8), GB("United Kingdom", 9);
		
		private String name;
		private int orderVal;
		private RegionOrder(String name, int orderVal){
			this.name = name;
			this.orderVal =orderVal;
		}

		/**====Getters====**/
		public String getName() { return name; }
		public int getOrderVal() { return orderVal; }

	}

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

	@Override
	protected void setData(ActionRequest req) {
		super.setData(req); //set the creator_profile_id
		marketId = req.getParameter("marketId");
		parentId = StringUtil.checkVal(req.getParameter("parentId"), null);
		marketName = req.getParameter("marketName");
		shortName = req.getParameter("shortName");
		aliasName = req.getParameter("aliasName");
		orderNo = Convert.formatInteger(req.getParameter("orderNo"));
		statusNo = req.getParameter("statusNo");
		setRegionCode(req.getParameter("regionCode"));
		marketSection = new SectionVO(req);
		setPublicFlag(Convert.formatInteger(req.getParameter("publicFlag")));
		setIndentNo(Convert.formatInteger(req.getParameter("indentNo")));
	}


	@Column(name="market_id", isPrimaryKey=true)
	public String getMarketId() {
		return marketId;
	}
	public void setMarketId(String marketId) {
		this.marketId = marketId;

		if(getMarketId().length() < AdminControllerAction.DOC_ID_MIN_LEN){
			super.setDocumentId(Section.MARKET.name() + "_" +marketId);
		}else {
			super.setDocumentId(marketId);
		}
	}
	@SolrField(name="parentId_s")
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

	@SolrField(name="status_s")
	@Column(name="status_no")
	public String getStatusNo() {
		return statusNo;
	}
	public void setStatusNo(String statusNo) {
		this.statusNo = statusNo;
	}
	

	@Override
	@SolrField(name=SearchDocumentHandler.META_KEYWORDS)
	public String getMetaKeywords() {
		StringBuilder sb = new StringBuilder(100);
		sb.append(StringUtil.checkVal(getShortName()));
		if (sb.length() > 0) sb.append(", ");
		sb.append(StringUtil.checkVal(getAliasName()));
		
		return sb.toString();
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
	
	public void setSectionParentId (String parentId) {
		if (marketSection == null)
			marketSection = new SectionVO();
		marketSection.setParentId(parentId);
	}

	@Column(name="section_parent_id", isReadOnly=true)
	public String getSectionParentId() {
		return marketSection.getParentId();
	}

	public void setSectionId (String id) {
		if (marketSection == null)
			marketSection = new SectionVO();
		marketSection.setSectionId(id);
	}

	@SolrField(name="sectionid_ss")
	@Column(name="section_id", isReadOnly=true)
	public String getSectionId() {
		return marketSection.getSectionId();
	}

	public void setSectionName (String name) {
		if (marketSection == null)
			marketSection = new SectionVO();
		marketSection.setSectionNm(name);
	}
	
	@Column(name="section_nm", isReadOnly=true)
	public String getSectionName() {
		return marketSection.getSectionNm();
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
	@Column(name="update_dt", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDt() {
		return updateDate;
	}
	
	@Override
	public void setUpdateDt(Date updateDate) {
		this.updateDate = updateDate;
	}


	@Column(name="create_dt", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDt() {
		return createDate;
	}
	public void setCreateDt(Date createDate) {
		this.createDate = createDate;
	}


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

	@Column(name="country_nm", isReadOnly=true)
	public String getRegionName() {
		return regionName;
	}


	public void setRegionName(String regionName) {
		this.regionName = regionName;
	}
	
	/**
	 * @return the creatorProfileId
	 */
	@Override
	@Column(name="creator_profile_id")
	public String getCreatorProfileId() {
		return creatorProfileId;
	}

	@Column(name="public_flg")
	public int getPublicFlag() {
		return publicFlag;
	}


	public void setPublicFlag(int publicFlag) {
		this.publicFlag = publicFlag;
	}


	/**
	 * @return the indentNo
	 */
	@Column(name="indent_no")
	public int getIndentNo() {
		return indentNo;
	}


	/**
	 * @param indentNo the indentNo to set
	 */
	public void setIndentNo(int indentNo) {
		this.indentNo = indentNo;
	}
}