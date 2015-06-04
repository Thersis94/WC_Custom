package com.depuysynthes.nexus;

import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.annotations.SolrField;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title</b>: NexusProductVO.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Handles the solr side of the nexus product information
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since May 20, 2015<p/>
 * @updates:
 ****************************************************************************/

public class NexusProductVO extends  SolrDocumentVO {
	
	public final static String solrIndex = "DEPUY_NEXUS";
	private String orgId;
	private String orgName ;
	private List<String> gtin;
	private List<String> gtinLevel;
	private String primaryDeviceId;
	private String unitOfUse;
	private String dpmGTIN;
	private int quantity = 0;
	private List<String> packageLevel;
	private List<String> uomLevel;
	private String region;
	private String status;

	
	public NexusProductVO() {
		super(solrIndex);
		gtin = new ArrayList<>();
		packageLevel = new ArrayList<>();
		gtinLevel = new ArrayList<>();
		uomLevel = new ArrayList<>();
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

	
	public String getOrgId() {
		return orgId;
	}


	public void setOrgId(String orgId) {
		this.orgId = orgId;
	}


	@SolrField(name="organizationName")
	public String getOrgName() {
		return orgName;
	}


	public void setOrgName(String orgName) {
		this.orgName = orgName;
	}


	@SolrField(name="gtin")
	public List<String> getGtin() {
		return gtin;
	}


	public void setGtin(List<String> gtin) {
		this.gtin = gtin;
	}
	
	public void addGtin(String gtin) {
		this.gtin.add(gtin);
	}


	@SolrField(name="gtinLvl")
	public List<String> getGtinLevel() {
		return gtinLevel;
	}
	
	public void setGtinLevel(List<String> gtinLevel) {
		this.gtinLevel = gtinLevel;
	}


	public void addGtinLevel(String gtinLevel) {
		this.gtinLevel.add(gtinLevel);
	}

	@SolrField(name="deviceId")
	public String getPrimaryDeviceId() {
		return primaryDeviceId;
	}


	public void setPrimaryDeviceId(String primaryDeviceId) {
		this.primaryDeviceId = primaryDeviceId;
	}


	@SolrField(name="unitOfUse")
	public String getUnitOfUse() {
		return unitOfUse;
	}


	public void setUnitOfUse(String unitOfUse) {
		this.unitOfUse = unitOfUse;
	}


	@SolrField(name="dpmGTIN")
	public String getDpmGTIN() {
		return dpmGTIN;
	}


	public void setDpmGTIN(String dpmGTIN) {
		this.dpmGTIN = dpmGTIN;
	}


	@SolrField(name="quantity")
	public int getQuantity() {
		return quantity;
	}


	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}


	@SolrField(name="packageLvl")
	public List<String> getPackageLevel() {
		return packageLevel;
	}


	public void setPackageLevel(List<String> packageLevel) {
		this.packageLevel = packageLevel;
	}
	
	public void addPackageLevel(String packageLevel) {
		this.packageLevel.add(packageLevel);
	}


	@SolrField(name="uomLvl")
	public List<String> getUomLevel() {
		return uomLevel;
	}
	
	public void addUOMLevel(String uom) {
		this.uomLevel.add(uom);
	}


	public void setUomLevel(List<String> uomLevel) {
		this.uomLevel = uomLevel;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}
	
	@SolrField(name="status")
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Gathers all relevant information that could be used for autocompletion
	 * and condenses it into a single field
	 * @return
	 */
	@SolrField(name="contents")
	public String getAutocomplete() {
		StringBuilder auto = new StringBuilder();
		auto.append(getTitle()).append(" ");
		for (String s : gtin) auto.append(s).append(" ");
		auto.append(getSummary());
		return auto.toString();
	}

}
