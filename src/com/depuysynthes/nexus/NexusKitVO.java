package com.depuysynthes.nexus;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.depuysynthes.nexus.NexusImporter.Source;
import com.siliconmtn.annotations.SolrField;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title</b>: NexusKitLayerVO.java<p/>
 * <b>Description: </b> Holds information pertaining to the kit in general.
 * <p/>
 * <b>Copyright:</b> (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Aug 9, 2015
 ****************************************************************************/


public class NexusKitVO extends SolrDocumentVO implements Serializable {
	
	public static final String KIT = "kit";
	public static final String OWNER = "owner";

	/**
	 * 
	 */
	private static final long serialVersionUID = 2414243653179753113L;
	
	private String kitGTIN;
	private String ownerId;
	private String orgName;
	private String branchCode;
	private Map<String, String> sharedWith;
	private List<NexusKitLayerVO> layers;
	private Source source;
	private boolean shared = false;
	
	NexusKitVO(String solrIndex) {
		super(solrIndex);
		layers = new ArrayList<>();
		sharedWith = new HashMap<>();
	}
	
	NexusKitVO(SMTServletRequest req, String solrIndex) {
		this(solrIndex);
		setData(req);
	}
	
	NexusKitVO(ResultSet rs, String solrIndex) {
		this(solrIndex);
		setData(rs);
	}
	
	/**
	 * Uses the string util to output a pipe delimited list of values
	 */
	public String toString() {
		return StringUtil.getToString(this, false, 1, "|");
	}
 	
	public void setData(SMTServletRequest req) {
		super.setDocumentId(req.getParameter("kitId"));
		super.setTitle(req.getParameter("kitSKU"));
		super.setSummary(req.getParameter("kitDesc"));
		branchCode = req.getParameter("branchCode");
		kitGTIN = req.getParameter("kitGTIN");
		orgName = req.getParameter("orgName");
		
	}
	
	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		super.setDocumentId(db.getStringVal("SET_INFO_ID", rs));
		super.setTitle(db.getStringVal("SET_SKU_TXT", rs));
		orgName = db.getStringVal("ORGANIZATION_ID", rs);
		kitGTIN = db.getStringVal("GTIN_TXT", rs);
		super.setSummary(db.getStringVal("DESCRIPTION_TXT", rs));
		branchCode = db.getStringVal("BRANCH_PLANT_CD", rs);
		ownerId = db.getStringVal("PROFILE_ID", rs);
	}

	public String getKitId() {
		return super.getDocumentId();
	}
	public void setKitId(String kitId) {
		super.setDocumentId(kitId);
	}

	public String getKitSKU() {
		return super.getTitle();
	}

	public void setKitSKU(String kitSKU) {
		super.setTitle(kitSKU);
	}

	public String getKitDesc() {
		return super.getSummary();
	}

	public void setKitDesc(String kitDesc) {
		super.setSummary(kitDesc);
	}
	
	@SolrField(name=NexusProductVO.DEVICE_ID)
	public String getKitGTIN() {
		return kitGTIN;
	}
	public void setKitGTIN(String kitGTIN) {
		this.kitGTIN = kitGTIN;
	}
	public String getOwnerId() {
		return ownerId;
	}
	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public Map<String, String> getSharedWith() {
		return sharedWith;
	}

	public void setSharedWith(Map<String, String> sharedWith) {
		this.sharedWith = sharedWith;
	}

	@SolrField(name=OWNER)
	public List<String> getPermissions() {
		List<String> s = new ArrayList<>(sharedWith.keySet());
		if (ownerId != null) s.add(ownerId);
		return s;
	}
	
	public void addPermision(String profileId, String name) {
		sharedWith.put(profileId, name);
	}
	
	public NexusKitLayerVO  findLayer(String layerId) {
		for (NexusKitLayerVO layer : layers) {
			if (layerId.equals(layer.getLayerId())) return layer;
			for (NexusKitLayerVO sublayer : layer.getSublayers()) {
				if (layerId.equals(sublayer.getLayerId())) return sublayer;
			}
		}
		return null;
	}
	

	public List<NexusKitLayerVO> getLayers() {
		return layers;
	}
	public void setLayers(List<NexusKitLayerVO> layers) {
		this.layers = layers;
	}
	public void addLayer(NexusKitLayerVO layer) {
		if (StringUtil.checkVal(layer.getParentId()).length() != 0) {
			NexusKitLayerVO parent = findLayer(layer.getParentId());
			if (parent != null) {
				parent.addLayer(layer);
			}
		} else {
			layers.add(layer);
		}
	}

	public List<String> getProducts() {
		List<String> productIds = new ArrayList<>();
		for (NexusKitLayerVO layer : layers) {
			for (NexusProductVO product : layer.getProducts()) {
				productIds.add(product.getProductId());
			}
			for (NexusKitLayerVO sublayer : layer.getSublayers()) {
				for (NexusProductVO product : sublayer.getProducts()) {
					productIds.add(product.getProductId());
				}
			}
		}
		return productIds;
	}
	
	public int getTotalProducts() {
		int count = 0;
		for (NexusKitLayerVO layer : layers) {
			count += layer.getProducts().size();
			
			for (NexusKitLayerVO sublayer : layer.getSublayers())
				count += sublayer.getProducts().size();
		}
		return count;
	}

	@SolrField(name=NexusProductVO.SOURCE)
	public Source getSource() {
		return source;
	}

	public void setSource(Source source) {
		this.source = source;
	}

	public String getOrgId() {
		if (super.getOrganization().isEmpty()) {
			return null;
		} else {
			return super.getOrganization().get(0);
		}
	}

	/**
	 * Kits will never have more than one organization and that
	 * rule is enforced here.
	 * @param orgId
	 */
	public void setOrgId(String orgId) {
		if (super.getOrganization().isEmpty()) {
			super.addOrganization(orgId);
		} else {
			super.getOrganization().set(0, orgId);
		}
	}

	@SolrField(name=NexusProductVO.ORGANIZATION_NM)
	public String getOrgName() {
		return orgName;
	}

	public void setOrgName(String orgName) {
		this.orgName = orgName;
	}

	public String getBranchCode() {
		return branchCode;
	}
	

	public void setBranchCode(String branchCode) {
		this.branchCode = branchCode;
	}


	/**
	 * Gathers all relevant information that could be used for autocompletion
	 * and condenses it into a single field
	 * @return
	 */
	@SolrField(name=SearchDocumentHandler.CONTENTS)
	@Override
	public String getContents() {
		StringBuilder auto = new StringBuilder();
		auto.append(super.getTitle()).append(" ");
		auto.append(kitGTIN).append(" ");
		auto.append(super.getSummary());
		return auto.toString();
	}
	

	//These functions exist solely to be called by the solr document creator
	//in order to ensure that certain fields get properly populated for use 
	//on the main site.
	
	@SolrField(name=KIT)
	public boolean isKit() {
		return true;
	}
	@SolrField(name=NexusProductVO.GTIN)
	public String getGTIN() {
		return kitGTIN;
	}
	@SolrField(name=NexusProductVO.SEARCHABLE_NM)
	public String getSearchableName() {
		if (super.getTitle() == null) return null;
		return super.getTitle().replaceAll("[\\-\\/\\.]", "");
	}

	public boolean isShared() {
		return shared;
	}

	public void setShared(boolean shared) {
		this.shared = shared;
	}
}
