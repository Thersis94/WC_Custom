package com.wsla.data.ticket;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: StatusCodeVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> manages the data in the ticket status table
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Oct 11, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_ticket_status")
public class StatusCodeVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8894672449150276295L;

	// Member Variables
	private String statusCode;
	private String statusName;
	private String roleId;
	private String roleName;
	private int activeFlag;
	private Date createDate;
	private Date updateDate;
	
	/**
	 * 
	 */
	public StatusCodeVO() {
		super();
	}

	/**
	 * @param req
	 */
	public StatusCodeVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public StatusCodeVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the statusCode
	 */
	@Column(name="status_cd", isPrimaryKey=true)
	public String getStatusCode() {
		return statusCode;
	}

	/**
	 * @return the statusName
	 */
	@Column(name="status_nm")
	public String getStatusName() {
		return statusName;
	}

	/**
	 * @return the roleId
	 */
	@Column(name="role_id")
	public String getRoleId() {
		return roleId;
	}

	/**
	 * @return the activeFlag
	 */
	@Column(name="active_flg")
	public int getActiveFlag() {
		return activeFlag;
	}

	/**
	 * @return the roleName
	 */
	@Column(name="role_nm", isReadOnly=true)
	public String getRoleName() {
		return roleName;
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
	 * @param statusCode the statusCode to set
	 */
	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * @param statusName the statusName to set
	 */
	public void setStatusName(String statusName) {
		this.statusName = statusName;
	}

	/**
	 * @param roleId the roleId to set
	 */
	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}

	/**
	 * @param activeFlag the activeFlag to set
	 */
	public void setActiveFlag(int activeFlag) {
		this.activeFlag = activeFlag;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	/**
	 * @param roleName the roleName to set
	 */
	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

}

