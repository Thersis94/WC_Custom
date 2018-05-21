package com.depuysynthes.srt.vo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.siliconmtn.annotations.SolrField;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.user.HumanNameIntfc;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title:</b> SRTProjectVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Stores Project Data
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Feb 5, 2018
 ****************************************************************************/
@Table(name="DPY_SYN_SRT_PROJECT")
public class SRTProjectSolrVO extends SolrDocumentVO implements HumanNameIntfc {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private String opCoId;
	private Date shipDt;
	private String category;
	private String projectStatus;
	private String engineerId;
	private String engineerNm;
	private String designerId;
	private String designerNm;
	private String qualityEngineerId;
	private String qualityEngineerNm;
	private String supplier;
	private String territoryId;
	private List<String> productFamilies;
	private List<String> productCategory;
	private List<String> partNumbers;
	private String makeFromPartNos;
	private String surgeonName;
	private String firstNm;
	private String lastNm;
	private String poNumber;

	public SRTProjectSolrVO() {
		super();
		productCategory = new ArrayList<>();
		productFamilies = new ArrayList<>();
		partNumbers = new ArrayList<>();
	}

	public SRTProjectSolrVO(ResultSet rs) {
		this();
		new DBProcessor(null).executePopulate(this, rs, null);
		try {
			this.setUpdateDt(rs.getDate("update_dt"));
			this.setDocumentId(rs.getString("document_id"));
			this.setTitle(rs.getString("title"));
		} catch (SQLException e) {
			log.error("Error Processing Code", e);
		}
	}

	/**
	 * @return the opCoId
	 */
	@Column(name="op_co_id")
	@SolrField(name="opCoId_s")
	public String getOpCoId() {
		return opCoId;
	}

	/**
	 * @return the shipDt
	 */
	@SolrField(name="shipDt_d")
	public Date getShipDt() {
		return shipDt;
	}

	/**
	 * @return the category
	 */
	@Column(name="op_co_id")
	@SolrField(name="category_s")
	public String getCategory() {
		return category;
	}

	/**
	 * @return the productFamilies
	 */
	@SolrField(name="productFamily_ss")
	public List<String> getProductFamilies() {
		return productFamilies;
	}

	/**
	 * @return the productCategory
	 */
	@SolrField(name="productCategory_ss")
	public List<String> getProductCategory() {
		return productCategory;
	}

	/**
	 * @return the projectStatus
	 */
	@Column(name="proj_stat_id")
	@SolrField(name="projectStatus_s")
	public String getProjectStatus() {
		return projectStatus;
	}

	/**
	 * @return the engineerId
	 */
	@Column(name="engineer_id")
	@SolrField(name="engineerId_s")
	public String getEngineerId() {
		return engineerId;
	}

	/**
	 * @return the engineerNm
	 */
	@Column(name="engineer_nm")
	@SolrField(name="engineerNm_s")
	public String getEngineerNm() {
		return engineerNm;
	}

	/**
	 * @return the designerId
	 */
	@Column(name="designer_id")
	@SolrField(name="designerId_s")
	public String getDesignerId() {
		return designerId;
	}

	/**
	 * @return the designerNm
	 */
	@Column(name="designer_nm")
	@SolrField(name="designerNm_s")
	public String getDesignerNm() {
		return designerNm;
	}

	/**
	 * @return the qualityEngineerId
	 */
	@Column(name="quality_engineer_id")
	@SolrField(name="qaId_s")
	public String getQualityEngineerId() {
		return qualityEngineerId;
	}

	/**
	 * @return the qualityEngineerNm
	 */
	@Column(name="quality_engineer_nm")
	@SolrField(name="qaNm_s")
	public String getQualityEngineerNm() {
		return qualityEngineerNm;
	}

	/**
	 * @return the supplier
	 */
	@Column(name="supplier_id")
	@SolrField(name="supplierId_s")
	public String getSupplier() {
		return supplier;
	}

	/**
	 * @return the territoryId
	 */
	@Column(name="request_territory_id")
	@SolrField(name="territoryId_s")
	public String getTerritoryId() {
		return territoryId;
	}

	/**
	 * @return the partNumbers
	 */
	@SolrField(name="partNumbers_ss")
	public List<String> getPartNumbers() {
		return partNumbers;
	}

	/**
	 * @return the makeFromPartNos
	 */
	@Column(name="make_from_order_no")
	@SolrField(name="makeFromPartNos_s")
	public String getMakeFromPartNos() {
		return makeFromPartNos;
	}

	/**
	 * @return the surgeonName
	 */
	@Column(name="surgeon_nm")
	@SolrField(name="surgeonName_s")
	public String getSurgeonName() {
		return surgeonName;
	}

	/**
	 * @return the salesRepName
	 */
	@SolrField(name="salesRep_s")
	public String getSalesRepName() {
		return StringUtil.join(firstNm, " ", lastNm);
	}

	/**
	 * @return the poNumber
	 */
	@Column(name="hospital_po")
	@SolrField(name="poNumber_s")
	public String getPoNumber() {
		return poNumber;
	}

	/**
	 * @param opCoId the opCoId to set.
	 */
	public void setOpCoId(String opCoId) {
		this.opCoId = opCoId;
	}

	/**
	 * @param shipDt the shipDt to set.
	 */
	public void setShipDt(Date shipDt) {
		this.shipDt = shipDt;
	}

	/**
	 * @param category the category to set.
	 */
	public void setCategory(String category) {
		this.category = category;
	}

	/**
	 * @param productFamilies the productFamilies to set.
	 */
	public void setProductFamilies(List<String> productFamilies) {
		this.productFamilies = productFamilies;
	}

	/**
	 * @param productCategory the productCategory to set.
	 */
	public void setProductCategory(List<String> productCategory) {
		this.productCategory = productCategory;
	}

	/**
	 * @param projectStatus the projectStatus to set.
	 */
	public void setProjectStatus(String projectStatus) {
		this.projectStatus = projectStatus;
	}

	/**
	 * @param engineerId the engineerId to set.
	 */
	public void setEngineerId(String engineerId) {
		this.engineerId = engineerId;
	}

	/**
	 * @param designerId the designerId to set.
	 */
	public void setDesignerId(String designerId) {
		this.designerId = designerId;
	}

	/**
	 * @param qualityEngineerId the qualityEngineerId to set.
	 */
	public void setQualityEngineerId(String qualityEngineerId) {
		this.qualityEngineerId = qualityEngineerId;
	}

	/**
	 * @param supplier the supplier to set.
	 */
	public void setSupplier(String supplier) {
		this.supplier = supplier;
	}

	/**
	 * @param territoryId the territoryId to set.
	 */
	public void setTerritoryId(String territoryId) {
		this.territoryId = territoryId;
	}

	/**
	 * @param partNumbers the partNumbers to set.
	 */
	public void setPartNumbers(List<String> partNumbers) {
		this.partNumbers = partNumbers;
	}

	/**
	 * @param makeFromPartNos the makeFromPartNos to set.
	 */
	public void setMakeFromPartNos(String makeFromPartNos) {
		this.makeFromPartNos = makeFromPartNos;
	}

	/**
	 * @param surgeonName the surgeonName to set.
	 */
	public void setSurgeonName(String surgeonName) {
		this.surgeonName = surgeonName;
	}

	/**
	 * @param poNumber the poNumber to set.
	 */
	public void setPoNumber(String poNumber) {
		this.poNumber = poNumber;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.util.user.HumanNameIntfc#getFirstName()
	 */
	@Override
	@Column(name="first_nm")
	public String getFirstName() {
		return firstNm;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.util.user.HumanNameIntfc#getLastName()
	 */
	@Override
	@Column(name="last_nm")
	public String getLastName() {
		return lastNm;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.util.user.HumanNameIntfc#setFirstName(java.lang.String)
	 */
	@Override
	public void setFirstName(String firstNm) {
		this.firstNm = firstNm;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.util.user.HumanNameIntfc#setLastName(java.lang.String)
	 */
	@Override
	public void setLastName(String lastNm) {
		this.lastNm = lastNm;
	}

	public void addProductFamily(String productFamily) {
		this.productFamilies.add(productFamily);
	}

	public void addProductCategory(String productCategory) {
		this.productCategory.add(productCategory);
	}

	public void addPartNumbers(String partNumbers) {
		this.partNumbers.add(partNumbers);
	}

	/**
	 * @param engineerNm the engineerNm to set
	 */
	public void setEngineerNm(String engineerNm) {
		this.engineerNm = engineerNm;
	}

	/**
	 * @param designerNm the designerNm to set
	 */
	public void setDesignerNm(String designerNm) {
		this.designerNm = designerNm;
	}

	/**
	 * @param qualityEngineerNm the qualityEngineerNm to set
	 */
	public void setQualityEngineerNm(String qualityEngineerNm) {
		this.qualityEngineerNm = qualityEngineerNm;
	}
}