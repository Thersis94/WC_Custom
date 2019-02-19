package com.wsla.data.ticket;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: NextStepVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Holds data regarding the next step for a service order.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Nov 8, 2018
 * @updates:
 ****************************************************************************/
public class NextStepVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7595095552093529941L;
	
	// Member Variables
	private String promptKeyCode;
	private String buttonUrl;
	private String buttonKeyCode;
	private String statusName;
	private String statusCode;
	private String groupStatusCode;
	private String roleName;
	private List<String> authorizedRoles;
	private boolean needsReloadFlag;
	
	/**
	 * 
	 */
	public NextStepVO() {
		super();
	}

	/**
	 * @param req
	 */
	public NextStepVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public NextStepVO(ResultSet rs) {
		super(rs);
	}
	
	/**
	 * @param status
	 * @param bundle
	 */
	public NextStepVO(StatusCode status) {
		setPromptKeyCode("status_code.next." + status.name());
		setButtonKeyCode("");
		setButtonUrl("");
		setStatusName("");
		setNeedsReloadFlag(false);
		setAuthorizedRoles(new ArrayList<>());
	}

	/**
	 * @return the promptKeyCode
	 */
	public String getPromptKeyCode() {
		return promptKeyCode;
	}

	/**
	 * @param promptKeyCode the promptKeyCode to set
	 */
	public void setPromptKeyCode(String promptKeyCode) {
		this.promptKeyCode = promptKeyCode;
	}

	/**
	 * @return the buttonUrl
	 */
	public String getButtonUrl() {
		return buttonUrl;
	}

	/**
	 * @param buttonUrl the buttonUrl to set
	 */
	public void setButtonUrl(String buttonUrl) {
		this.buttonUrl = buttonUrl;
	}

	/**
	 * @return the buttonKeyCode
	 */
	public String getButtonKeyCode() {
		return buttonKeyCode;
	}

	/**
	 * @param buttonName the buttonKeyCode to set
	 */
	public void setButtonKeyCode(String buttonKeyCode) {
		this.buttonKeyCode = buttonKeyCode;
	}

	/**
	 * @return the statusName
	 */
	public String getStatusName() {
		return statusName;
	}

	/**
	 * @param statusName the statusName to set
	 */
	public void setStatusName(String statusName) {
		this.statusName = statusName;
	}

	/**
	 * @return the statusCode
	 */
	public String getStatusCode() {
		return statusCode;
	}

	/**
	 * @param statusCode the statusCode to set
	 */
	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * @return the groupStatusCode
	 */
	public String getGroupStatusCode() {
		return groupStatusCode;
	}

	/**
	 * @param groupStatusCode the groupStatusCode to set
	 */
	public void setGroupStatusCode(String groupStatusCode) {
		this.groupStatusCode = groupStatusCode;
	}

	/**
	 * @return the roleName
	 */
	public String getRoleName() {
		return roleName;
	}

	/**
	 * @param roleName the roleName to set
	 */
	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	/**
	 * @return the authorizedRoles
	 */
	public List<String> getAuthorizedRoles() {
		return authorizedRoles;
	}

	/**
	 * @param authorizedRoles the authorizedRoles to set
	 */
	public void setAuthorizedRoles(List<String> authorizedRoles) {
		this.authorizedRoles = authorizedRoles;
	}

	/**
	 * @param authorizedRoles the authorizedRoles to add
	 */
	public void addAuthorizedRole(String authorizedRole) {
		this.authorizedRoles.add(authorizedRole);
	}

	/**
	 * @return the needsReloadFlag
	 */
	public boolean isNeedsReloadFlag() {
		return needsReloadFlag;
	}

	/**
	 * @param needsReloadFlag the needsReloadFlag to set
	 */
	public void setNeedsReloadFlag(boolean needsReloadFlag) {
		this.needsReloadFlag = needsReloadFlag;
	}
}

