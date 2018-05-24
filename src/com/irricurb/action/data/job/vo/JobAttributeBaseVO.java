package com.irricurb.action.data.job.vo;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs 3.5
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: JobAttributeBean.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Base Attributes for jobs
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 27, 2018
 * @updates:
 ****************************************************************************/
@Table(name="ic_job_attribute")
public class JobAttributeBaseVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7024294899135603251L;

	// Member Variables
	private String jobAttributeCode;
	private String attributeName;
	private int adminFlag;
	private Date createDate;
	
	/**
	 * 
	 */
	public JobAttributeBaseVO() {
		super();
	}

	/**
	 * @param req
	 */
	public JobAttributeBaseVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public JobAttributeBaseVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the jobAttributeCode
	 */
	@Column(name="job_attribute_cd", isPrimaryKey=true)
	public String getJobAttributeCode() {
		return jobAttributeCode;
	}

	/**
	 * @return the attributeName
	 */
	@Column(name="attribute_nm")
	public String getAttributeName() {
		return attributeName;
	}

	/**
	 * @return the adminFlag
	 */
	@Column(name="admin_flg")
	public int getAdminFlag() {
		return adminFlag;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param jobAttributeCode the jobAttributeCode to set
	 */
	public void setJobAttributeCode(String jobAttributeCode) {
		this.jobAttributeCode = jobAttributeCode;
	}

	/**
	 * @param attributeName the attributeName to set
	 */
	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	/**
	 * @param adminFlag the adminFlag to set
	 */
	public void setAdminFlag(int adminFlag) {
		this.adminFlag = adminFlag;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

}

