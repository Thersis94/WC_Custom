package com.irricurb.action.data.job.vo;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: JobAttributeVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Attributes for a given job
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 27, 2018
 * @updates:
 ****************************************************************************/
@Table(name="ic_project_job_attribute_xr")
public class JobAttributeVO extends JobAttributeBaseVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2425427464608389878L;
	
	// Member Variables
	private String jobAttributeXRId;
	private String jobId;
	private String jobAttributeCode;
	private String value;
	private Date createDate;

	/**
	 * 
	 */
	public JobAttributeVO() {
		super();
	}

	/**
	 * @param req
	 */
	public JobAttributeVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public JobAttributeVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the jobAttributeXRId
	 */
	@Column(name="job_attribute_xr_id", isPrimaryKey=true)
	public String getJobAttributeXRId() {
		return jobAttributeXRId;
	}

	/**
	 * @return the jobId
	 */
	@Column(name="job_id")
	public String getJobId() {
		return jobId;
	}

	/**
	 * @return the jobAttributeCode
	 */
	@Column(name="job_attribute_cd")
	public String getJobAttributeCode() {
		return jobAttributeCode;
	}

	/**
	 * @return the value
	 */
	@Column(name="value_txt")
	public String getValue() {
		return value;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param jobAttributeXRId the jobAttributeXRId to set
	 */
	public void setJobAttributeXRId(String jobAttributeXRId) {
		this.jobAttributeXRId = jobAttributeXRId;
	}

	/**
	 * @param jobId the jobId to set
	 */
	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	/**
	 * @param jobAttributeCode the jobAttributeCode to set
	 */
	public void setJobAttributeCode(String jobAttributeCode) {
		this.jobAttributeCode = jobAttributeCode;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

}

