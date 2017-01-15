package com.depuysynthes.action;

import java.util.Date;
import java.util.List;

import com.siliconmtn.commerce.catalog.ProductAttributeVO;
import com.siliconmtn.commerce.catalog.ProductVO;

/****************************************************************************
 * <b>Title</b>: EMEAProductCatalogReportVO.java<p/>
 * <b>Description: DataBean/VO for the Report.  List<> of these fed to the report
 * for iteration into Excel.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 14, 2017
 * @update
 ****************************************************************************/
public class EMEAProductCatalogReportVO {

	private String productId;
	private String productName;
	private String urlAlias;
	private String sousProductName;
	private String hierarchy;
	private Date updateDate;
	private List<MediaBinAssetVO> assets;
	private List<ProductAttributeVO> attrs;

	public EMEAProductCatalogReportVO(ProductVO prod, List<ProductAttributeVO> attrs) {
		super();
		setAttrs(attrs);
		setProductId(prod.getProductId());
		setProductName(prod.getProductName());
		setUrlAlias(prod.getUrlAlias());
		setSousProductName(prod.getFullProductName());
		setHierarchy(prod.getAttrib1Txt());
		setUpdateDate(prod.getLastUpdate());
	}

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public String getUrlAlias() {
		return urlAlias;
	}

	public void setUrlAlias(String urlAlias) {
		this.urlAlias = urlAlias;
	}

	public String getSousProductName() {
		return sousProductName;
	}

	public void setSousProductName(String sousProductName) {
		this.sousProductName = sousProductName;
	}

	public int getAssetCount() {
		return assets != null ? assets.size() : 0;
	}

	public String getHierarchy() {
		return hierarchy;
	}

	public void setHierarchy(String hierarchy) {
		this.hierarchy = hierarchy;
	}

	public Date getUpdateDate() {
		return updateDate;
	}

	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	public List<MediaBinAssetVO> getAssets() {
		return assets;
	}

	public void setAssets(List<MediaBinAssetVO> assets) {
		this.assets = assets;
	}

	public List<ProductAttributeVO> getAttrs() {
		return attrs;
	}

	public void setAttrs(List<ProductAttributeVO> attrs) {
		this.attrs = attrs;
	}
}