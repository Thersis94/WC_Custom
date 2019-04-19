package com.mts.util;

// JDK 1.8.x
import java.util.Date;

// MTS Libs
import com.mts.publication.data.MTSDocumentVO;

// SMT Base Libs
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: SBActionVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> ***Change Me
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 2, 2019
 * @updates:
 ****************************************************************************/
@Table(name="sb_action")
public class SBActionVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 900561179548288988L;
	
	// Members
	private String actionId;
	private String moduleTypeId; 
	private String actionName;
	private String actionDesc; 
	private String organizationId; 
	private String groupId; 
	private int pendingSyncFlag;
	private String createById;
	private Date createDate;
	
	/**
	 * 
	 */
	public SBActionVO(MTSDocumentVO doc) {
		super();
		actionId = doc.getActionId();
		actionName = doc.getActionName();
		actionDesc = doc.getActionDesc();
		moduleTypeId = doc.getModuleTypeId();
		organizationId = doc.getOrganizationId();
		groupId = doc.getActionGroupId();
		pendingSyncFlag = doc.getPendingSyncFlag();
		createById = doc.getCreateById();
	}

	/**
	 * @return the actionId
	 */
	@Column(name="action_id")
	public String getActionId() {
		return actionId;
	}

	/**
	 * @return the moduleTypeId
	 */
	@Column(name="module_type_id")
	public String getModuleTypeId() {
		return moduleTypeId;
	}

	/**
	 * @return the actionName
	 */
	@Column(name="action_nm")
	public String getActionName() {
		return actionName;
	}

	/**
	 * @return the actionDesc
	 */
	@Column(name="action_desc")
	public String getActionDesc() {
		return actionDesc;
	}

	/**
	 * @return the organizationId
	 */
	@Column(name="organization_id")
	public String getOrganizationId() {
		return organizationId;
	}

	/**
	 * @return the groupId
	 */
	@Column(name="action_group_id")
	public String getGroupId() {
		return groupId;
	}

	/**
	 * @return the pendingSyncFlag
	 */
	@Column(name="pending_sync_flg")
	public int getPendingSyncFlag() {
		return pendingSyncFlag;
	}

	/**
	 * @return the createById
	 */
	@Column(name="create_by_id")
	public String getCreateById() {
		return createById;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param actionId the actionId to set
	 */
	public void setActionId(String actionId) {
		this.actionId = actionId;
	}

	/**
	 * @param moduleTypeId the moduleTypeId to set
	 */
	public void setModuleTypeId(String moduleTypeId) {
		this.moduleTypeId = moduleTypeId;
	}

	/**
	 * @param actionName the actionName to set
	 */
	public void setActionName(String actionName) {
		this.actionName = actionName;
	}

	/**
	 * @param actionDesc the actionDesc to set
	 */
	public void setActionDesc(String actionDesc) {
		this.actionDesc = actionDesc;
	}

	/**
	 * @param organizationId the organizationId to set
	 */
	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
	}

	/**
	 * @param groupId the groupId to set
	 */
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	/**
	 * @param pendingSyncFlag the pendingSyncFlag to set
	 */
	public void setPendingSyncFlag(int pendingSyncFlag) {
		this.pendingSyncFlag = pendingSyncFlag;
	}

	/**
	 * @param createById the createById to set
	 */
	public void setCreateById(String createById) {
		this.createById = createById;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

}
