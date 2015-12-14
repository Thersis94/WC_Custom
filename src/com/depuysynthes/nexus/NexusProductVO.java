package com.depuysynthes.nexus;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.depuysynthes.nexus.NexusImporter.Source;
import com.siliconmtn.annotations.SolrField;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.search.SearchDocumentHandler;
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

public class NexusProductVO extends SolrDocumentVO {

	public final static String DEVICE_ID = "deviceId";
	public final static String SEARCHABLE_NM = "searchableName";
	public final static String ORGANIZATION_NM = "organizationName";
	public final static String GTIN = "gtin";
	public final static String GTIN_LVL = "gtinLvl";
	public final static String UNIT = "unitOfUse";
	public final static String DPM = "dpmGTIN";
	public final static String QUANTITY = "quantity";
	public final static String PKG_LVL = "packageLvl";
	public final static String UOM_LVL = "uomLvl";
	public final static String solrIndex = "DEPUY_NEXUS";
	public final static String STATUS = "status";
	public final static String SOURCE = "source";
	private String productId;
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
	private int orderNo;
	private boolean dateLot;
	private Date start;
	private Date end;
	private Source source;

	
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
	
	public NexusProductVO(ResultSet rs) {
		this();
		setData(rs);
	}
	
	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		productId = db.getStringVal("PRODUCT_SKU_TXT", rs);
		quantity = db.getIntVal("QUANTITY_NO", rs);
		if (!StringUtil.checkVal(db.getStringVal("ITEM_GTIN_TXT", rs)).equals(""))
			gtin.add(db.getStringVal("ITEM_GTIN_TXT", rs));
		primaryDeviceId = db.getStringVal("ITEM_GTIN_NO", rs);
		start = db.getDateVal("EFFECTIVE_START_DT", rs);
		end = db.getDateVal("EFFECTIVE_END_DT", rs);
		if (!StringUtil.checkVal(db.getStringVal("UNIT_MEASURE_CD", rs)).equals(""))
			uomLevel.add(db.getStringVal("UNIT_MEASURE_CD", rs));
		orderNo = db.getIntVal("ORDER_NO", rs);
	}
	
	public NexusProductVO clone() throws CloneNotSupportedException {
		NexusProductVO clone =  (NexusProductVO) super.clone();
		clone.setProductId(productId);
		clone.setOrgId(orgId);
		clone.setOrgName(orgName);
		clone.setGtin(new ArrayList<String>());
		for (String s : gtin) clone.addGtin(s);
		clone.setGtinLevel(new ArrayList<String>());
		for (String s : gtinLevel) clone.addGtinLevel(s);
		clone.setPrimaryDeviceId(primaryDeviceId);
		clone.setDpmGTIN(dpmGTIN);
		clone.setQuantity(quantity);
		clone.setPackageLevel(new ArrayList<String>());
		for (String s : packageLevel) clone.addPackageLevel(s);
		clone.setUomLevel(new ArrayList<String>());
		for (String s : uomLevel) clone.addUOMLevel(s);
		clone.setRegion(region);
		clone.setStatus(status);
		clone.setEnd(end);
		clone.setStart(start);
		return clone;
	}

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public String getProductName() {
		return getDocumentId();
	}


	public void setProductName(String productName) {
		setDocumentId(productName);
		setTitle(productName);
	}

	@SolrField(name=SEARCHABLE_NM)
	public String getSearchableName() {
		if (getDocumentId() == null) return null;
		return getDocumentId().replaceAll("[\\-\\/\\.]", "");
	}

	
	public String getOrgId() {
		return orgId;
	}


	public void setOrgId(String orgId) {
		this.orgId = orgId;
	}


	@SolrField(name=ORGANIZATION_NM)
	public String getOrgName() {
		return orgName;
	}


	public void setOrgName(String orgName) {
		this.orgName = orgName;
	}


	@SolrField(name=GTIN)
	public List<String> getGtin() {
		return gtin;
	}


	public void setGtin(List<String> gtin) {
		this.gtin = gtin;
	}
	
	public void addGtin(String gtin) {
		this.gtin.add(gtin);
	}


	@SolrField(name=GTIN_LVL)
	public List<String> getGtinLevel() {
		return gtinLevel;
	}
	
	public void setGtinLevel(List<String> gtinLevel) {
		this.gtinLevel = gtinLevel;
	}


	public void addGtinLevel(String gtinLevel) {
		this.gtinLevel.add(gtinLevel);
	}

	@SolrField(name=DEVICE_ID)
	public String getPrimaryDeviceId() {
		return primaryDeviceId;
	}


	public void setPrimaryDeviceId(String primaryDeviceId) {
		this.primaryDeviceId = primaryDeviceId;
	}


	@SolrField(name=UNIT)
	public String getUnitOfUse() {
		return unitOfUse;
	}


	public void setUnitOfUse(String unitOfUse) {
		this.unitOfUse = unitOfUse;
	}


	@SolrField(name=DPM)
	public String getDpmGTIN() {
		return dpmGTIN;
	}


	public void setDpmGTIN(String dpmGTIN) {
		this.dpmGTIN = dpmGTIN;
	}


	@SolrField(name=QUANTITY)
	public int getQuantity() {
		return quantity;
	}


	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}


	@SolrField(name=PKG_LVL)
	public List<String> getPackageLevel() {
		return packageLevel;
	}


	public void setPackageLevel(List<String> packageLevel) {
		this.packageLevel = packageLevel;
	}
	
	public void addPackageLevel(String packageLevel) {
		this.packageLevel.add(packageLevel);
	}


	@SolrField(name=UOM_LVL)
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
	
	@SolrField(name=STATUS)
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
	
	public int getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(int orderNo) {
		this.orderNo = orderNo;
	}

	/**
	 * Gathers all relevant information that could be used for autocompletion
	 * and condenses it into a single field
	 * @return
	 */
	@SolrField(name=SearchDocumentHandler.CONTENTS)
	public String getAutocomplete() {
		StringBuilder auto = new StringBuilder();
		auto.append(getTitle()).append(" ");
		for (String s : gtin) auto.append(s).append(" ");
		auto.append(getSummary());
		return auto.toString();
	}

	public Date getStart() {
		return start;
	}

	public void setStart(Date start) {
		this.start = start;
	}

	public Date getEnd() {
		return end;
	}

	public void setEnd(Date end) {
		this.end = end;
	}

	@SolrField(name=SOURCE)
	public Source getSource() {
		return source;
	}

	public void setSource(Source source) {
		this.source = source;
	}

}
