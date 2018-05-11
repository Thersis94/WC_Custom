package com.irricurb.action.data.job.vo;

// JDK 1.8.x
import java.util.Date;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: JobVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for the Job Information
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 27, 2018
 * @updates:
 ****************************************************************************/
@Table(name="ic_job")
public class JobVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4781595878706408739L;

	// Member Variables
	private String jobId;
	private String jobName;
	private String className;
	private Date updateDate;
	private Date createDate;
	
	// Sub Beans
	private List<JobAttributeVO> attributes = new ArrayList<>();
	
	/**
	 * 
	 */
	public JobVO() {
		super();
	}

	/**
	 * @param req
	 */
	public JobVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public JobVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the jobId
	 */
	@Column(name="job_id", isPrimaryKey=true)
	public String getJobId() {
		return jobId;
	}

	/**
	 * @return the jobName
	 */
	@Column(name="job_nm")
	public String getJobName() {
		return jobName;
	}

	/**
	 * @return the className
	 */
	@Column(name="class_nm")
	public String getClassName() {
		return className;
	}

	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isUpdateOnly=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @return the attributes
	 */
	public List<JobAttributeVO> getAttributes() {
		return attributes;
	}
	
	/**
	 * @param jobId the jobId to set
	 */
	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	/**
	 * @param jobName the jobName to set
	 */
	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	/**
	 * @param className the className to set
	 */
	public void setClassName(String className) {
		this.className = className;
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
	 * @param attributes the attributes to set
	 */
	public void setAttributes(List<JobAttributeVO> attributes) {
		this.attributes = attributes;
	}

	/**
	 * Adds an attribute to the collection
	 * @param attribute
	 */
	@BeanSubElement
	public void addAttribute(JobAttributeVO attribute) {
		this.attributes.add(attribute);
	}
	
	/**
	 * Returns the value for the given key (attribute code)
	 * @param key
	 * @return
	 */
	public String getAttributeValue(String key) {
		if (key == null) return null;
		
		for (JobAttributeVO attribute : attributes) {
			if (key.equalsIgnoreCase(attribute.getJobAttributeCode())) return attribute.getValue();
		}
		
		return null;
	}
}

