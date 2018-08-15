package com.depuysynthes.srt.vo;

import java.sql.ResultSet;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title:</b> SRTMasterRecordVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Stores SRT Master Record Information.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Feb 5, 2018
 ****************************************************************************/
@Table(name="DPY_SYN_SRT_MASTER_RECORD")
public class SRTMasterRecordVO extends BeanDataVO {

	/**
	 *
	 */
	private static final long serialVersionUID = 8656525987783386949L;
	private String masterRecordId;
	private String opCoId;
	private String partNo;
	private String titleTxt;
	private String qualitySystemId;
	private String prodTypeId;
	private String complexityId;
	private String prodCatId;
	private String makeFromPartNos;
	private String prodFamilyId;
	private String xrProjectId;
	private int totalBuilt;
	private int partCount;
	private int obsoleteFlg;
	private String obsoleteReason;
	private Date createDt;
	private Date updateDt;
	private SRTFileVO prodImg;
	private Map<String, String> attributes;
	private String projectId;
	private String mrProjectXRId;

	public SRTMasterRecordVO() {
		attributes = new LinkedHashMap<>();
	}

	public SRTMasterRecordVO(ActionRequest req) {
		this();
		populateData(req);
	}

	public SRTMasterRecordVO(ResultSet rs) {
		this();
		if(rs != null)
			populateData(rs);
	}

	/*
	 * Overloaded constructor for generating MasterRecord placeholders on
	 * project record.
	 */
	public SRTMasterRecordVO(String masterRecordId) {
		this.masterRecordId = masterRecordId;
	}
	/**
	 * @return the masterRecordId
	 */
	@Column(name="MASTER_RECORD_ID", isPrimaryKey=true)
	public String getMasterRecordId() {
		return masterRecordId;
	}

	/**
	 * @return the mrProjectXRId
	 */
	@Column(name="MASTER_RECORD_PROJECT_XR_ID", isReadOnly=true)
	public String getMrProjectXRId() {
		return mrProjectXRId;
	}

	/**
	 * @return the opCoId
	 */
	@Column(name="OP_CO_ID")
	public String getOpCoId() {
		return opCoId;
	}

	/**
	 * @return the partNo
	 */
	@Column(name="PART_NO")
	public String getPartNo() {
		return partNo;
	}

	/**
	 * @return the titleTxt
	 */
	@Column(name="TITLE_TXT")
	public String getTitleTxt() {
		return titleTxt;
	}

	/**
	 * @return the qualitySystemId
	 */
	@Column(name="QUALITY_SYSTEM_ID")
	public String getQualitySystemId() {
		return qualitySystemId;
	}

	/**
	 * @return the prodTypeId
	 */
	@Column(name="PROD_TYPE_ID")
	public String getProdTypeId() {
		return prodTypeId;
	}

	/**
	 * @return the complexityId
	 */
	@Column(name="COMPLEXITY_ID")
	public String getComplexityId() {
		return complexityId;
	}

	/**
	 * @return the prodCatId
	 */
	@Column(name="PROD_CAT_ID")
	public String getProdCatId() {
		return prodCatId;
	}

	/**
	 * @return the makeFromPartNos
	 */
	@Column(name="MAKE_FROM_PART_NOS")
	public String getMakeFromPartNos() {
		return makeFromPartNos;
	}

	/**
	 * @return the prodFamilyId
	 */
	@Column(name="PROD_FAMILY_ID")
	public String getProdFamilyId() {
		return prodFamilyId;
	}

	/**
	 * @return the totalBuilt
	 */
	@Column(name="TOTAL_BUILT")
	public int getTotalBuilt() {
		return totalBuilt;
	}

	/**
	 * @return the partCount
	 */
	@Column(name="PART_COUNT", isReadOnly=true)
	public int getPartCount() {
		return partCount;
	}

	/**
	 * @return the projectId.
	 */
	@Column(name="PROJECT_ID", isReadOnly=true)
	public String getProjectId() {
		return projectId;
	}

	/**
	 * @return the isObsolete
	 */
	@Column(name="OBSOLETE_FLG")
	public int getObsoleteFlg() {
		return obsoleteFlg;
	}

	/**
	 * @return the isObsolete
	 */
	public boolean isObsolete() {
		return Convert.formatBoolean(obsoleteFlg);
	}

	/**
	 * @return the obsoleteReason
	 */
	@Column(name="OBSOLETE_REASON")
	public String getObsoleteReason() {
		return obsoleteReason;
	}

	/**
	 * @return the createDt
	 */
	@Column(name="CREATE_DT", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDt() {
		return createDt;
	}

	/**
	 * @return the updateDt
	 */
	@Column(name="UPDATE_DT", isUpdateOnly=true, isAutoGen=true)
	public Date getUpdateDt() {
		return updateDt;
	}

	/**
	 * @return the xrProjectId
	 */
	@Column(name="PROJECT_ID", isReadOnly=true)
	public String getXrProjectId() {
		return xrProjectId;
	}

	/**
	 * @return the fileUploads
	 */
	public SRTFileVO getProdImg() {
		return prodImg;
	}

	/**
	 * @return the attributes
	 */
	public Map<String, String> getAttributes() {
		return attributes;
	}

	/**
	 * @param masterRecordId the masterRecordId to set.
	 */
	public void setMasterRecordId(String masterRecordId) {
		this.masterRecordId = masterRecordId;
	}

	/**
	 * @param mrProjectXRId the mrProjectXRId to set.
	 */
	public void setMrProjectXRId(String mrProjectXRId) {
		this.mrProjectXRId = mrProjectXRId;
	}

	/**
	 * @param opCoId the opCoId to set.
	 */
	public void setOpCoId(String opCoId) {
		this.opCoId = opCoId;
	}

	/**
	 * @param partNo the partNo to set.
	 */
	public void setPartNo(String partNo) {
		this.partNo = partNo;
	}

	/**
	 * @param titleTxt the titleTxt to set.
	 */
	public void setTitleTxt(String titleTxt) {
		this.titleTxt = titleTxt;
	}

	/**
	 * @param qualitySystemId the qualitySystemId to set.
	 */
	public void setQualitySystemId(String qualitySystemId) {
		this.qualitySystemId = qualitySystemId;
	}

	/**
	 * @param prodTypeId the prodTypeId to set.
	 */
	public void setProdTypeId(String prodTypeId) {
		this.prodTypeId = prodTypeId;
	}

	/**
	 * @param complexityId the complexityId to set.
	 */
	public void setComplexityId(String complexityId) {
		this.complexityId = complexityId;
	}

	/**
	 * @param prodCatId the prodCatId to set.
	 */
	public void setProdCatId(String prodCatId) {
		this.prodCatId = prodCatId;
	}

	/**
	 * @param makeFromPartNos the makeFromPartNos to set.
	 */
	public void setMakeFromPartNos(String makeFromPartNos) {
		this.makeFromPartNos = makeFromPartNos;
	}

	/**
	 * @param prodFamilyId the prodFamilyId to set.
	 */
	public void setProdFamilyId(String prodFamilyId) {
		this.prodFamilyId = prodFamilyId;
	}

	/**
	 * @param totalBuilt the totalBuilt to set.
	 */
	public void setTotalBuilt(int totalBuilt) {
		this.totalBuilt = totalBuilt;
	}

	/**
	 * @param partCount the partCount to set.
	 */
	public void setPartCount(int partCount) {
		this.partCount = partCount;
	}

	/**
	 * @param projectId the projectId to set.
	 */
	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	/**
	 * @param isObsolete the isObsolete to set.
	 */
	public void setObsolete(boolean obsoleteFlg) {
		this.obsoleteFlg = Convert.formatInteger(obsoleteFlg);
	}

	public void setObsoleteFlg(int obsoleteFlg) {
		this.obsoleteFlg = obsoleteFlg;
	}
	/**
	 * @param obsoleteReason the obsoleteReason to set.
	 */
	public void setObsoleteReason(String obsoleteReason) {
		this.obsoleteReason = obsoleteReason;
	}

	/**
	 * @param createDt the createDt to set.
	 */
	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}

	/**
	 * @param updateDt the updateDt to set.
	 */
	public void setUpdateDt(Date updateDt) {
		this.updateDt = updateDt;
	}

	/**
	 * @param fileUploads the fileUploads to set.
	 */
	@BeanSubElement
	public void setProdImg(SRTFileVO prodImg) {
		this.prodImg = prodImg;
	}

	/**
	 * @param attributes the attributes to set.
	 */
	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}

	/**
	 * @param key the attribute key to set
	 * @param value the attribute value to set
	 */
	public void addAttribute(String key, String value) {
		if(!StringUtil.isEmpty(key)) {
			attributes.put(key, value);
		}
	}

	/**
	 * @param xrProjectId the xrProjectId to set.
	 */
	public void setXrProjectId(String xrProjectId) {
		this.xrProjectId = xrProjectId;
	}
}