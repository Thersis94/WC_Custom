package com.fastsigns.action.franchise.vo;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBModuleVO;

/****************************************************************************
 * <b>Title</b>: FranchiseCategoryVO.java<p/>
 * <b>Description: Store the requisite information for a Metro category and
 * its products</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Sep 15, 2014
 ****************************************************************************/
public class MetroCategoryVO extends SBModuleVO {

	private static final long serialVersionUID = -8670969500986105123L;
	private String metroCategoryId = null;
	private String metroAreaId = null;
	private String metroCategoryNm = null;
	private String metroCategoryAlias = null;
	private String metroCategoryDesc = null;
	private String titleTxt = null;
	private String metaDesc = null;
	private String metaKywd = null;
	private Integer orderNo = Integer.valueOf(0);
	private List<ProductVO> products = null;
	
	public MetroCategoryVO() {
		products = new ArrayList<ProductVO>();
	}
	
	public MetroCategoryVO(SMTServletRequest req) {
		metroCategoryId = req.getParameter("metroCategoryId");
		metroAreaId = req.getParameter("metroAreaId");
		metroCategoryNm = req.getParameter("metroCategoryNm");
		metroCategoryAlias = req.getParameter("metroCategoryAlias");
		metroCategoryDesc = req.getParameter("metroCategoryDesc");
		metaDesc = req.getParameter("metaDesc");
		metaKywd = req.getParameter("metaKywdTxt");
		titleTxt = req.getParameter("titleTxt");
		orderNo = Convert.formatInteger(req.getParameter("orderNo"), 0);
		products = new ArrayList<ProductVO>();
	}
	
	public MetroCategoryVO(ResultSet rs) {
		DBUtil db = new DBUtil();
		metroCategoryId = db.getStringVal("metro_category_id", rs);
		metroAreaId = db.getStringVal("metro_area_id", rs);
		metroCategoryNm = db.getStringVal("category_nm", rs);
		metroCategoryAlias = db.getStringVal("category_alias", rs);
		metroCategoryDesc = db.getStringVal("category_desc", rs);
		titleTxt = db.getStringVal("title_txt", rs);
		metaDesc = db.getStringVal("meta_desc", rs);
		metaKywd = db.getStringVal("meta_kywd_txt", rs);
		orderNo = db.getIntegerVal("order_no", rs);
		//from superclass...  used for Sitemap generation (lastUpdateDate)
		createDate = db.getDateVal("create_dt", rs);
		updateDate = db.getDateVal("update_dt", rs);
		products = new ArrayList<ProductVO>();
		db = null;
	}
	
	public String getMetroCategoryId() {
		return metroCategoryId;
	}
	public void setMetroCategoryId(String metroCategoryId) {
		this.metroCategoryId = metroCategoryId;
	}
	public String getMetroAreaId() {
		return metroAreaId;
	}
	public void setMetroAreaId(String metroAreaId) {
		this.metroAreaId = metroAreaId;
	}
	public String getMetroCategoryAlias() {
		return metroCategoryAlias;
	}
	public void setMetroCategoryAlias(String metroCategoryAlias) {
		this.metroCategoryAlias = metroCategoryAlias;
	}
	public String getMetroCategoryNm() {
		return metroCategoryNm;
	}
	public void setMetroCategoryNm(String metroCategoryNm) {
		this.metroCategoryNm = metroCategoryNm;
	}
	public String getMetroCategoryDesc() {
		return metroCategoryDesc;
	}

	public void setMetroCategoryDesc(String metroCategoryDesc) {
		this.metroCategoryDesc = metroCategoryDesc;
	}

	public String getMetaDesc() {
		return metaDesc;
	}
	public void setMetaDesc(String metaDesc) {
		this.metaDesc = metaDesc;
	}
	public String getMetaKywd() {
		return metaKywd;
	}
	public void setMetaKywd(String metaKywd) {
		this.metaKywd = metaKywd;
	}
	public Integer getOrderNo() {
		return orderNo;
	}
	public void setOrderNo(Integer orderNo) {
		this.orderNo = orderNo;
	}

	public void setTitleTxt(String titleTxt) {
		this.titleTxt = titleTxt;
	}

	public String getTitleTxt() {
		return titleTxt;
	}

	public List<ProductVO> getProducts() {
		return products;
	}

	public void setProducts(List<ProductVO> products) {
		this.products = products;
	}
	
	public void addProduct(ProductVO product) {
		products.add(product);
	}
	
}
