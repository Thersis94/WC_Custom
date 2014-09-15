package com.fastsigns.action.franchise.vo;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.data.Node;
import com.siliconmtn.db.DBUtil;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.dealer.DealerLocationVO;
import com.smt.sitebuilder.action.gis.MapVO;

/****************************************************************************
 * <b>Title</b>: MetroContainerVO.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Dec 9, 2010<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class MetroContainerVO extends SBModuleVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// Member Variables
	private List<DealerLocationVO> results = new ArrayList<DealerLocationVO>();
	private String metroAreaId = null;
	private String areaName = null;
	private String areaAlias = null;
	private String areaDesc = null;
	private String imagePath = null;
	private String title = null;
	private String metaKeyword = null;
	private String metaDesc = null;
	private String state = null;
	private String country = null;
	private Integer lstFlag = Integer.valueOf(0);
	private Double latitude = Double.valueOf(0);
	private Double longitude = Double.valueOf(0);
	private MapVO mapData = new MapVO();
	private Map<String,MetroProductVO> productPages = new HashMap<String,MetroProductVO>();
	private String communities = null;
	private String locality = null;
	private Integer mapZoomNo = Integer.valueOf(0);
	private String unitKey = null;
	private List<Node> prodList =  new ArrayList<Node>();
	
	public MetroContainerVO() {
	}
	
	public MetroContainerVO(ResultSet rs) {
		this.assignVals(rs);
	}
	
	
	/**
	 * 
	 * @param rs
	 */
	public void assignVals(ResultSet rs) {
		DBUtil db = new DBUtil();
		metroAreaId = db.getStringVal("metro_area_id", rs);
		areaName = db.getStringVal("area_nm", rs);
		areaAlias = db.getStringVal("area_alias_nm", rs);
		areaDesc = db.getStringVal("area_desc", rs);
		imagePath = db.getStringVal("image_path_url", rs);
		title = db.getStringVal("title_txt", rs);
		metaKeyword = db.getStringVal("meta_keyword_txt", rs);
		metaDesc = db.getStringVal("meta_desc_txt", rs);
		state = db.getStringVal("state_cd", rs);
		country = db.getStringVal("country_cd", rs);
		lstFlag = db.getIntVal("area_lst_flg", rs);
		latitude = db.getDoubleVal("latitude_no", rs);
		longitude = db.getDoubleVal("longitude_no", rs);
		communities = db.getStringVal("communities_txt", rs);
		locality = db.getStringVal("locality_txt", rs);
		//from superclass...  used for Sitemap generation (lastUpdateDate)
		createDate = db.getDateVal("create_dt", rs);
		updateDate = db.getDateVal("update_dt", rs);
		setMapZoomNo(db.getIntegerVal("map_zoom_no", rs));
	}
	
	/**
	 * 
	 * @param vo
	 */
	public void addResult(DealerLocationVO vo) {
		results.add(vo);
	}
	
	/**
	 * @return the locations
	 */
	public List<DealerLocationVO> getResults() {
		return results;
	}

	/**
	 * @param locations the locations to set
	 */
	public void setResults(List<DealerLocationVO> locations) {
		this.results = locations;
	}

	/**
	 * @return the areaName
	 */
	public String getAreaName() {
		return areaName;
	}

	/**
	 * @param areaName the areaName to set
	 */
	public void setAreaName(String areaName) {
		this.areaName = areaName;
	}

	/**
	 * @return the areaAlias
	 */
	public String getAreaAlias() {
		return areaAlias;
	}

	/**
	 * @param areaAlias the areaAlias to set
	 */
	public void setAreaAlias(String areaAlias) {
		this.areaAlias = areaAlias;
	}

	/**
	 * @return the areaDesc
	 */
	public String getAreaDesc() {
		return areaDesc;
	}

	/**
	 * @param areaDesc the areaDesc to set
	 */
	public void setAreaDesc(String areaDesc) {
		this.areaDesc = areaDesc;
	}

	/**
	 * @return the imagePath
	 */
	public String getImagePath() {
		return imagePath;
	}

	/**
	 * @param imagePath the imagePath to set
	 */
	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return the metaKeyword
	 */
	public String getMetaKeyword() {
		return metaKeyword;
	}

	/**
	 * @param metaKeyword the metaKeyword to set
	 */
	public void setMetaKeyword(String metaKeyword) {
		this.metaKeyword = metaKeyword;
	}

	/**
	 * @return the metaDesc
	 */
	public String getMetaDesc() {
		return metaDesc;
	}

	/**
	 * @param metaDesc the metaDesc to set
	 */
	public void setMetaDesc(String metaDesc) {
		this.metaDesc = metaDesc;
	}

	/**
	 * @return the lstFlag
	 */
	public Integer getLstFlag() {
		return lstFlag;
	}

	/**
	 * @param lstFlag the lstFlag to set
	 */
	public void setLstFlag(Integer lstFlag) {
		this.lstFlag = lstFlag;
	}

	/**
	 * @return the latitude
	 */
	public Double getLatitude() {
		return latitude;
	}

	/**
	 * @param latitude the latitude to set
	 */
	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	/**
	 * @return the longitude
	 */
	public Double getLongitude() {
		return longitude;
	}

	/**
	 * @param longitude the longitude to set
	 */
	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	/**
	 * @return the mapData
	 */
	public MapVO getMapData() {
		return mapData;
	}

	/**
	 * @param mapData the mapData to set
	 */
	public void setMapData(MapVO mapData) {
		this.mapData = mapData;
	}

	/**
	 * @return the metroAreaId
	 */
	public String getMetroAreaId() {
		return metroAreaId;
	}

	/**
	 * @param metroAreaId the metroAreaId to set
	 */
	public void setMetroAreaId(String metroAreaId) {
		this.metroAreaId = metroAreaId;
	}

	/**
	 * @return the state
	 */
	public String getState() {
		return state;
	}

	/**
	 * @param state the state to set
	 */
	public void setState(String state) {
		this.state = state;
	}

	/**
	 * @return the country
	 */
	public String getCountry() {
		return country;
	}

	/**
	 * @param country the country to set
	 */
	public void setCountry(String country) {
		this.country = country;
	}

	public void setProductPages(Map<String,MetroProductVO> productPages) {
		this.productPages = productPages;
	}
	
	public void addProductPage(MetroProductVO prod) {
		productPages.put(prod.getAliasNm(), prod);
	}

	public Map<String,MetroProductVO> getProductPages() {
		return productPages;
	}

	public void setCommunities(String communities) {
		this.communities = communities;
	}

	public String getCommunities() {
		return communities;
	}

	public void setLocality(String locality) {
		this.locality = locality;
	}

	public String getLocality() {
		return locality;
	}

	/**
	 * @param mapZoomNo the mapZoomNo to set
	 */
	public void setMapZoomNo(Integer mapZoomNo) {
		this.mapZoomNo = mapZoomNo;
	}

	/**
	 * @return the mapZoomNo
	 */
	public Integer getMapZoomNo() {
		return mapZoomNo;
	}

	/**
	 * @param unitKey the unitKey to set
	 */
	public void setUnitKey(String unitKey) {
		this.unitKey = unitKey;
	}

	/**
	 * @return the unitKey
	 */
	public String getUnitKey() {
		return unitKey;
	}

	public List<Node> getProdList() {
		return prodList;
	}

	public void setProdList(List<Node> prodList) {
		this.prodList = prodList;
	}
	
	public void addProduct(Node prod) {
		this.prodList.add(prod);
	}

}
