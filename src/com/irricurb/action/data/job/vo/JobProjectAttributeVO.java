package com.irricurb.action.data.job.vo;

// JDK 1.8.x
import java.sql.ResultSet;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: JobProjectAttributeVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Attributes that override the defaults for a job attribute on a project
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 27, 2018
 * @updates:
 ****************************************************************************/
@Table(name="ic_project_job_attribute_xr")
public class JobProjectAttributeVO extends JobAttributeVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -427901886388650581L;

	// Member Variable
	private String projectJobAttributeId;
	private String projectJobId;
	
	/**
	 * 
	 */
	public JobProjectAttributeVO() {
		super();
	}

	/**
	 * @param req
	 */
	public JobProjectAttributeVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public JobProjectAttributeVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the projectJobAttributeId
	 */
	@Column(name="project_job_attribute_xr_id", isPrimaryKey=true)
	public String getProjectJobAttributeId() {
		return projectJobAttributeId;
	}

	/**
	 * @return the projectJobId
	 */
	@Column(name="project_job_id")
	public String getProjectJobId() {
		return projectJobId;
	}

	/**
	 * @param projectJobAttributeId the projectJobAttributeId to set
	 */
	public void setProjectJobAttributeId(String projectJobAttributeId) {
		this.projectJobAttributeId = projectJobAttributeId;
	}

	/**
	 * @param projectJobId the projectJobId to set
	 */
	public void setProjectJobId(String projectJobId) {
		this.projectJobId = projectJobId;
	}

}

