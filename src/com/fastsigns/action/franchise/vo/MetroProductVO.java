/**
 * 
 */
package com.fastsigns.action.franchise.vo;

import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBModuleVO;

/****************************************************************************
 * <b>Title</b>: MetroProductVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Sep 21, 2011
 ****************************************************************************/
public class MetroProductVO extends SBModuleVO {

	private static final long serialVersionUID = -8670969500986105123L;
	private String metroProductId = null;
	private String metroAreaId = null;
	private String productNm = null;
	private String aliasNm = null;
	private String bodyTxt = null;
	private String titleTxt = null;
	private String metaDesc = null;
	private String metaKywd = null;
	private Boolean visibleFlg = Boolean.FALSE;
	private Integer orderNo = Integer.valueOf(0);
	
	public MetroProductVO() {
		
	}
	
	public MetroProductVO(SMTServletRequest req) {
		metroProductId = req.getParameter("metroProductId");
		metroAreaId = req.getParameter("metroAreaId");
		productNm = req.getParameter("productNm");
		aliasNm = req.getParameter("aliasNm");
		bodyTxt = req.getParameter("bodyTxt");
		metaDesc = req.getParameter("metaDesc");
		metaKywd = req.getParameter("metaKywdTxt");
		titleTxt = req.getParameter("titleTxt");
		visibleFlg = Convert.formatBoolean(req.getParameter("visibleFlg"), false);
		orderNo = Convert.formatInteger(req.getParameter("orderNo"), 0);
	}
	
	public MetroProductVO(ResultSet rs) {
		DBUtil db = new DBUtil();
		metroProductId = db.getStringVal("metro_product_id", rs);
		metroAreaId = db.getStringVal("metro_area_id", rs);
		productNm = db.getStringVal("product_nm", rs);
		aliasNm = db.getStringVal("alias_txt", rs);
		bodyTxt = db.getStringVal("body_txt", rs);
		titleTxt = db.getStringVal("title_txt", rs);
		metaDesc = db.getStringVal("meta_desc", rs);
		metaKywd = db.getStringVal("meta_kywd_txt", rs);
		visibleFlg = db.getBooleanVal("visible_flg", rs);
		orderNo = db.getIntegerVal("order_no", rs);
		//from superclass...  used for Sitemap generation (lastUpdateDate)
		createDate = db.getDateVal("create_dt", rs);
		updateDate = db.getDateVal("update_dt", rs);
		db = null;
	}
	
	public String getMetroProductId() {
		return metroProductId;
	}
	public void setMetroProductId(String metroProductId) {
		this.metroProductId = metroProductId;
	}
	public String getMetroAreaId() {
		return metroAreaId;
	}
	public void setMetroAreaId(String metroAreaId) {
		this.metroAreaId = metroAreaId;
	}
	public String getAliasNm() {
		return aliasNm;
	}
	public void setAliasNm(String aliasNm) {
		this.aliasNm = aliasNm;
	}
	public String getProductNm() {
		return productNm;
	}
	public void setProductNm(String productNm) {
		this.productNm = productNm;
	}
	public String getBodyTxt() {
		return bodyTxt;
	}
	public void setBodyTxt(String bodyTxt) {
		this.bodyTxt = bodyTxt;
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
	public Boolean getVisibleFlg() {
		return visibleFlg;
	}
	public void setVisibleFlg(Boolean visibleFlg) {
		this.visibleFlg = visibleFlg;
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
	
}
