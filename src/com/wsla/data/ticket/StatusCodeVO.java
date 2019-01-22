package com.wsla.data.ticket;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.EnumUtil;
import com.wsla.common.WSLAConstants.WSLARole;

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

	public enum Group {
		PROCESSING, DELIVERY, DIAGNOSIS, REPAIR, PICKUP, COMPLETE
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8894672449150276295L;

	// Member Variables
	private String statusCode;
	private Group groupStatusCode;
	private String statusName;
	private String roleId;
	private String roleName;
	private String billableActivityCode;
	private int activeFlag;
	private String nextStepUrl;
	private String nextStepBtnKeyCode;
	private String authorizedRoleText;
	private Date createDate;
	private Date updateDate;
	
	// Helper Variables
	private int daysInStatus;
	
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
	 * @return the nextStepUrl
	 */
	@Column(name="next_step_url")
	public String getNextStepUrl() {
		return nextStepUrl;
	}

	/**
	 * @return the nextStepBtnKeyCode
	 */
	@Column(name="next_step_btn_key_cd")
	public String getNextStepBtnKeyCode() {
		return nextStepBtnKeyCode;
	}

	/**
	 * @return the authorizedRoleText
	 */
	@Column(name="authorized_role_txt")
	public String getAuthorizedRoleText() {
		return authorizedRoleText;
	}

	/**
	 * @return the authorizedRole
	 */
	public List<WSLARole> getAuthorizedRole() {
		List<String> authorizedRoleIds = StringUtil.parseList(authorizedRoleText);
		List<WSLARole> roles = new ArrayList<>();
		
		for (String authorizedRoleId : authorizedRoleIds) {
			WSLARole role = EnumUtil.safeValueOf(WSLARole.class, authorizedRoleId);
			if (role != null) roles.add(role);
		}
		
		return roles;
	}

	/**
	 * @return the daysInStatus
	 */
	@Column(name="days_in_status", isReadOnly=true)
	public int getDaysInStatus() {
		return daysInStatus;
	}

	/**
	 * @return the billableActivityCode
	 */
	@Column(name="billable_activity_cd")
	public String getBillableActivityCode() {
		return billableActivityCode;
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

	/**
	 * @param nextStepUrl the nextStepUrl to set
	 */
	public void setNextStepUrl(String nextStepUrl) {
		this.nextStepUrl = nextStepUrl;
	}

	/**
	 * @param nextStepBtnKeyCode the nextStepBtnKeyCode to set
	 */
	public void setNextStepBtnKeyCode(String nextStepBtnKeyCode) {
		this.nextStepBtnKeyCode = nextStepBtnKeyCode;
	}

	/**
	 * @param authorizedRoleText the authorizedRoleText to set
	 */
	public void setAuthorizedRoleText(String authorizedRoleText) {
		this.authorizedRoleText = authorizedRoleText;
	}

	/**
	 * @param authorizedRole the authorizedRole to set
	 */
	public void setAuthorizedRole(List<WSLARole> authorizedRole) {
		String[] roleIds = new String[authorizedRole.size()];
		
		for (int idx = 0; idx < authorizedRole.size(); idx++) {
			roleIds[idx] = authorizedRole.get(idx).name();
		}
		
		this.authorizedRoleText = StringUtil.getDelimitedList(roleIds, false, ",");
	}

	/**
	 * @param daysInStatus the daysInStatus to set
	 */
	public void setDaysInStatus(int daysInStatus) {
		this.daysInStatus = daysInStatus;
	}

	/**
	 * @return the groupStatusCode
	 */
	@Column(name="group_status_cd")
	public Group getGroupStatusCode() {
		return groupStatusCode;
	}

	/**
	 * @param groupStatusCode the groupStatusCode to set
	 */
	public void setGroupStatusCode(Group groupStatusCode) {
		this.groupStatusCode = groupStatusCode;
	}

	/**
	 * @param billableActivityCode the billableActivityCode to set
	 */
	public void setBillableActivityCode(String billableActivityCode) {
		this.billableActivityCode = billableActivityCode;
	}

}

