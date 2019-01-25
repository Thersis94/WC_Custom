package com.wsla.data.admin;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: InvoiceTypeVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Data bean for the Invoice Type data
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jan 24, 2019
 * @updates:
 ****************************************************************************/
@Table(name="wsla_invoice_type")
public class InvoiceTypeVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2723221298995728447L;

	// Members
	private String invoiceTypeCode;
	private String name;
	private String desc;
	private int activeFlag;
	private Date createDate;
	
	/**
	 * 
	 */
	public InvoiceTypeVO() {
		super();
	}

	/**
	 * @param req
	 */
	public InvoiceTypeVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public InvoiceTypeVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the invoiceTypeCode
	 */
	@Column(name="invoice_type_cd", isPrimaryKey=true, isAutoGen=false)
	public String getInvoiceTypeCode() {
		return invoiceTypeCode;
	}

	/**
	 * @return the name
	 */
	@Column(name="type_nm")
	public String getName() {
		return name;
	}

	/**
	 * @return the desc
	 */
	@Column(name="type_desc")
	public String getDesc() {
		return desc;
	}

	/**
	 * @return the activeFlag
	 */
	@Column(name="active_flg")
	public int getActiveFlag() {
		return activeFlag;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param invoiceTypeCode the invoiceTypeCode to set
	 */
	public void setInvoiceTypeCode(String invoiceTypeCode) {
		this.invoiceTypeCode = invoiceTypeCode;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param desc the desc to set
	 */
	public void setDesc(String desc) {
		this.desc = desc;
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

}

