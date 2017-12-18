package com.irricurb.action.data.vo;

import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: IrriCurbProjectVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> value object used to store data for one irri curb project.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Dec 12, 2017
 * @updates:
 ****************************************************************************/
@Table(name="ic_project")
public class IrriCurbProjectVO extends BeanDataVO{

	private static final long serialVersionUID = -1785673422020888616L;
	
	private String projectId;
	private String customerId;
	private String projectName;
	private String projectDescption;
	private String statusCode;
	private Date createDate;
	private Date updateDate;
	
	public enum ProjectStatusCd {
		OPEN("Open"), 
		ACTIVE("Active"), 
		INPROGRESS("In Progress"),
		COMPLETE("Complete"),
		CLOSED("Closed");
		private String statusName;
		ProjectStatusCd(String statusName) {
			this.statusName = statusName;
		}
		public String getStatusName() {
			return statusName;
		}
	}
	
	public IrriCurbProjectVO(ResultSet rs) {
		super.populateData(rs);
	}

	public IrriCurbProjectVO(ActionRequest req) {
		super.populateData(req);
	}
	
	public IrriCurbProjectVO() {
		//empty constructor for db processor use
	}

	/**
	 * @return the projectId
	 */
	@Column(name="project_id", isPrimaryKey=true)
	public String getProjectId() {
		return projectId;
	}
	/**
	 * @return the customerId
	 */
	@Column(name="customer_id")
	public String getCustomerId() {
		return customerId;
	}
	
	/**
	 * @return the projectName
	 */
	@Column(name="project_nm")
	public String getProjectName() {
		return projectName;
	}
	
	/**
	 * @return the statusCode
	 */
	@Column(name="status_cd")
	public String getStatusCode() {
		return statusCode;
	}
	
	/**
	 * @return the projectDescption
	 */
	@Column(name="project_desc")
	public String getProjectDescption() {
		return projectDescption;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true)
	public Date getCreateDate() {
		return createDate;
	}
	
	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isUpdateOnly=true)
	public Date getUpdateDate() {
		return updateDate;
	}
	
	
	
	
	/**
	 * @param projectId the projectId to set
	 */
	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	/**
	 * @param customerId the customerId to set
	 */
	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	/**
	 * @param projectName the projectName to set
	 */
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	/**
	 * @param statusCode the statusCode to set
	 */
	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}
	
	/**
	 * @param statusCode the statusCode to set
	 */
	public void setStatus(ProjectStatusCd statusCode) {
		this.statusCode = statusCode.name();
	}

	/**
	 * @param projectDescption the projectDescption to set
	 */
	public void setProjectDescption(String projectDescption) {
		this.projectDescption = projectDescption;
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
	
	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.data.parser.BeanDataVO#toString()
	 */
	@Override
	public String toString(){
		return StringUtil.getToString(this);
	}

}
