package com.biomed.smarttrak.vo;

import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: ProductAllianceVO.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> DBProcessor enabled VO that stores information regarding
 * the alliances that can be assigned to a product.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Feb 3, 2017<p/>
 * <b>Changes: </b>
 ****************************************************************************/

@Table(name="BIOMEDGPS_PRODUCT_ALLIANCE_XR")
public class ProductAllianceVO {
	private String allianceId;
	private String productId;
	private String allianceTypeId;
	private String allianceTypeName;
	private String allyId;
	private String allyName;
	private String referenceText;
	private int orderNo;
	private int gaFlag;
	
	public ProductAllianceVO(){
		// Default constructor created to allow creation of this
		// vo without needing a servlet request.
	}
	
	public ProductAllianceVO(ActionRequest req) {
		setData(req);
	}
	
	private void setData(ActionRequest req) {
		allianceId = req.getParameter("allianceId");
		productId = req.getParameter("productId");
		allianceTypeId = req.getParameter("allianceTypeId");
		allianceTypeName = req.getParameter("allianceTypeName");
		allyId = req.getParameter("allyId");
		allyName = req.getParameter("allyName");
		referenceText = req.getParameter("referenceText");
		orderNo = Convert.formatInteger(req.getParameter("orderNo"));
	}

	@Column(name="product_alliance_xr_id", isPrimaryKey=true)
	public String getAllianceId() {
		return allianceId;
	}
	public void setAllianceId(String allianceId) {
		this.allianceId = allianceId;
	}
	@Column(name="product_id")
	public String getProductId() {
		return productId;
	}
	public void setProductId(String productId) {
		this.productId = productId;
	}
	@Column(name="alliance_type_id")
	public String getAllianceTypeId() {
		return allianceTypeId;
	}
	public void setAllianceTypeId(String allianceTypeId) {
		this.allianceTypeId = allianceTypeId;
	}
	@Column(name="type_nm", isReadOnly=true)
	public String getAllianceTypeName() {
		return allianceTypeName;
	}
	public void setAllianceTypeName(String allianceTypeName) {
		this.allianceTypeName = allianceTypeName;
	}
	@Column(name="company_id")
	public String getAllyId() {
		return allyId;
	}
	public void setAllyId(String allyId) {
		this.allyId = allyId;
	}
	@Column(name="company_nm", isReadOnly=true)
	public String getAllyName() {
		return allyName;
	}
	public void setAllyName(String allyName) {
		this.allyName = allyName;
	}
	@Column(name="reference_txt")
	public String getReferenceText() {
		return referenceText;
	}
	public void setReferenceText(String referenceText) {
		this.referenceText = referenceText;
	}
	@Column(name="order_no")
	public int getOrderNo() {
		return orderNo;
	}
	public void setOrderNo(int orderNo) {
		this.orderNo = orderNo;
	}
	@Column(name="ga_display_flg")
	public int getGaFlag() {
		return gaFlag;
	}

	public void setGaFlag(int gaFlag) {
		this.gaFlag = gaFlag;
	}

	// These functions exists only to give the DBProcessor a hook to autogenerate dates on
	@Column(name="UPDATE_DT", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDate() {return null;}
	@Column(name="CREATE_DT", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {return null;}

}
