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
 * <b>Title</b>: DiagnosticVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Data object for the diagnostic information
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Oct 2, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_diagnostic")
public class DiagnosticVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1390238180460592469L;
	
	// Member Variables
	private String diagnosticId;
	private String productCategoryId;
	private String diagnosticName;
	private String description;
	private int serviceCenterFlag;
	private int casFlag;
	private Date createDate;
	private Date updateDate;
	
	/**
	 * 
	 */
	public DiagnosticVO() {
		super();
	}

	/**
	 * @param req
	 */
	public DiagnosticVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public DiagnosticVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the diagnosticId
	 */
	@Column(name="diagnostic_id", isPrimaryKey=true)
	public String getDiagnosticId() {
		return diagnosticId;
	}

	/**
	 * @return the productCategoryId
	 */
	@Column(name="product_category_id")
	public String getProductCategoryId() {
		return productCategoryId;
	}

	/**
	 * @return the diagnosticName
	 */
	@Column(name="diagnostic_nm")
	public String getDiagnosticName() {
		return diagnosticName;
	}

	/**
	 * @return the description
	 */
	@Column(name="desc_txt")
	public String getDescription() {
		return description;
	}

	/**
	 * @return the serviceCenterFlag
	 */
	@Column(name="svc_ctr_flg")
	public int getServiceCenterFlag() {
		return serviceCenterFlag;
	}

	/**
	 * @return the casFlag
	 */
	@Column(name="cas_flg")
	public int getCasFlag() {
		return casFlag;
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
	 * @param diagnosticId the diagnosticId to set
	 */
	public void setDiagnosticId(String diagnosticId) {
		this.diagnosticId = diagnosticId;
	}

	/**
	 * @param productCategoryId the productCategoryId to set
	 */
	public void setProductCategoryId(String productCategoryId) {
		this.productCategoryId = productCategoryId;
	}

	/**
	 * @param diagnosticName the diagnosticName to set
	 */
	public void setDiagnosticName(String diagnosticName) {
		this.diagnosticName = diagnosticName;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @param serviceCenterFlag the serviceCenterFlag to set
	 */
	public void setServiceCenterFlag(int serviceCenterFlag) {
		this.serviceCenterFlag = serviceCenterFlag;
	}

	/**
	 * @param casFlag the casFlag to set
	 */
	public void setCasFlag(int casFlag) {
		this.casFlag = casFlag;
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

}

