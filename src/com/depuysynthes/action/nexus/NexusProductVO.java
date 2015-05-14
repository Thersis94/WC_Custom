package com.depuysynthes.action.nexus;

import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.annotations.SolrField;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

public class NexusProductVO extends  SolrDocumentVO {
	
	private final static String solrIndex = "DEPUY_NEXUS";
	private String orgName;
	private List<String> gtin;
	private String gtinLevel;
	private String primaryDeviceId;
	private String unitOfUse;
	private String dpmGTIN;
	private int quantity;
	private String packageLevel;
	private String uomLevel;
	private String region;
	private String status;

	
	public NexusProductVO() {
		super(solrIndex);
		gtin = new ArrayList<>();
	}
	
	public NexusProductVO(String name) {
		this();
		setProductName(name);
	}


	public String getProductName() {
		return getDocumentId();
	}


	public void setProductName(String productName) {
		setDocumentId(productName);
		setTitle(productName);
	}


	@SolrField(name="organizationName_t")
	public String getOrgName() {
		return orgName;
	}


	public void setOrgName(String orgName) {
		this.orgName = orgName;
	}


	@SolrField(name="gtin_txt")
	public List<String> getGtin() {
		return gtin;
	}


	public void setGtin(List<String> gtin) {
		this.gtin = gtin;
	}
	
	public void addGtin(String gtin) {
		this.gtin.add(gtin);
	}


	@SolrField(name="gtinLvl_t")
	public String getGtinLevel() {
		return gtinLevel;
	}


	public void setGtinLevel(String gtinLevel) {
		this.gtinLevel = gtinLevel;
	}


	@SolrField(name="device_t")
	public String getPrimaryDeviceId() {
		return primaryDeviceId;
	}


	public void setPrimaryDeviceId(String primaryDeviceId) {
		this.primaryDeviceId = primaryDeviceId;
	}


	@SolrField(name="unitOfUse_t")
	public String getUnitOfUse() {
		return unitOfUse;
	}


	public void setUnitOfUse(String unitOfUse) {
		this.unitOfUse = unitOfUse;
	}


	@SolrField(name="dpmGTIN_t")
	public String getDpmGTIN() {
		return dpmGTIN;
	}


	public void setDpmGTIN(String dpmGTIN) {
		this.dpmGTIN = dpmGTIN;
	}


	@SolrField(name="quantity_i")
	public int getQuantity() {
		return quantity;
	}


	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}


	@SolrField(name="packageLvl_t")
	public String getPackageLevel() {
		return packageLevel;
	}


	public void setPackageLevel(String packageLevel) {
		this.packageLevel = packageLevel;
	}


	@SolrField(name="uomLvl_t")
	public String getUomLevel() {
		return uomLevel;
	}


	public void setUomLevel(String uomLevel) {
		this.uomLevel = uomLevel;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}
