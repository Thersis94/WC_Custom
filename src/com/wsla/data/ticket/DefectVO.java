package com.wsla.data.ticket;

import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: DefectVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for defect information
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Oct 9, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_defect")
public class DefectVO extends BeanDataVO {

	private static final long serialVersionUID = -2028564476651021590L;
	
	/**
	 * Defines the types of defect codes.  The initial defect type pre-repair 
	 * and the post-repair repair code
	 */
	public enum Type {
		DEFECT_CODE("Defect Code"), REPAIR_CODE("Repair Code"), BOTH("Defect and Repair");
		
		protected final String typeCode;
		Type(String typeCode) { this.typeCode = typeCode; }
		public String getType() { return typeCode; }
	}
	
	// Member Variables
	private String defectCode;
	private String providerId;
	private String providerName;
	private String defectName;
	private Type defectType;
	private int activeFlag;
	private Date createDate;
	private Date updateDate;
	
	/**
	 * 
	 */
	public DefectVO() {
		super();
	}
	/**
	 * @param req
	 */
	public DefectVO(ResultSet rs) {
		super(rs);
	}
	
	/**
	 * @param req
	 */
	public DefectVO(ActionRequest req) {
		super(req);
	}


	/**
	 * @return the defectCode
	 */
	@Column(name="defect_cd", isPrimaryKey=true)
	public String getDefectCode() {
		return defectCode;
	}

	/**
	 * @return the providerCode
	 */
	@Column(name="provider_id")
	public String getProviderId() {
		return providerId;
	}

	/**
	 * @return the defectName
	 */
	@Column(name="defect_nm")
	public String getDefectName() {
		return defectName;
	}

	/**
	 * @return the activeFlag
	 */
	@Column(name="active_flg")
	public int getActiveFlag() {
		return activeFlag;
	}

	/**
	 * @return the defectType
	 */
	@Column(name="defect_type_cd")
	public Type getDefectType() {
		return defectType;
	}
	
	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isUpdateOnly=true, isAutoGen=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @return the providerName
	 */
	@Column(name="provider_nm", isReadOnly=true)
	public String getProviderName() {
		return providerName;
	}

	/**
	 * @param providerName the providerName to set
	 */
	public void setProviderName(String providerName) {
		this.providerName = providerName;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @param activeFlag the activeFlag to set
	 */
	public void setActiveFlag(Integer activeFlag) {
		this.activeFlag = activeFlag;
	}

	/**
	 * @param defectName the defectName to set
	 */
	public void setDefectName(String defectName) {
		this.defectName = defectName;
	}

	/**
	 * @param providerCode the providerCode to set
	 */
	public void setProviderId(String providerId) {
		this.providerId = StringUtil.isEmpty(providerId) ? null : providerId;
	}

	/**
	 * @param defectCode the defectCode to set
	 */
	public void setDefectCode(String defectCode) {
		this.defectCode = defectCode;
	}

	/**
	 * @param defectType the defectType to set
	 */
	public void setDefectType(Type defectType) {
		this.defectType = defectType;
	}

}
