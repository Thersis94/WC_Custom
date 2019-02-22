package com.restpeer.data;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// Rest Peer Libs
import com.restpeer.common.RPConstants.DataType;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: ProductVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the hierarchy of products
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 13, 2019
 * @updates:
 ****************************************************************************/
@Table(name="rp_product")
public class ProductVO extends BeanDataVO {
	/**
	 * 
	 */
	public enum UnitMeasure {
		MONTHLY("Monthly"),
		NONE("N/A"),
		WEEKLY("Weekly");
		
		private String uomName;
		UnitMeasure(String uomName) { 
			this.uomName = uomName;
		}

		public String getUomName() {	return uomName; }
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -149669378203475527L;
	
	// Members
	private String productCode;
	private String parentCode;
	private String categoryCode;
	private String groupCode;
	private String name;
	private double price;
	private int activeFlag;
	private int scheduleFlag;
	private int orderNumber;
	private DataType dataType;
	private UnitMeasure uom;
	private Date createDate;
	private Date updateDate;
	
	// Member Helpers
	private String categoryName;
	private String parentName;

	/**
	 * 
	 */
	public ProductVO() {
		super();
	}

	/**
	 * @param req
	 */
	public ProductVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public ProductVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the productCode
	 */
	@Column(name="product_cd", isPrimaryKey=true)
	public String getProductCode() {
		return productCode;
	}

	/**
	 * @return the parentCode
	 */
	@Column(name="parent_cd")
	public String getParentCode() {
		return parentCode;
	}

	/**
	 * @return the uom
	 */
	@Column(name="unit_measure_cd")
	public UnitMeasure getUom() {
		return uom;
	}

	/**
	 * @return the categoryCode
	 */
	@Column(name="category_cd")
	public String getCategoryCode() {
		return categoryCode;
	}

	/**
	 * @return the name
	 */
	@Column(name="product_nm")
	public String getName() {
		return name;
	}

	/**
	 * @return the price
	 */
	@Column(name="price_no")
	public double getPrice() {
		return price;
	}

	/**
	 * @return the activeFlag
	 */
	@Column(name="active_flg")
	public int getActiveFlag() {
		return activeFlag;
	}

	/**
	 * @return the scheduleFlag
	 */
	@Column(name="schedule_flg")
	public int getScheduleFlag() {
		return scheduleFlag;
	}

	/**
	 * @return the orderNumber
	 */
	@Column(name="order_no")
	public int getOrderNumber() {
		return orderNumber;
	}

	/**
	 * @return the dataType
	 */
	@Column(name="data_type_cd")
	public DataType getDataType() {
		return dataType;
	}

	/**
	 * @return the categoryName
	 */
	@Column(name="category_nm", isReadOnly=true)
	public String getCategoryName() {
		return categoryName;
	}

	/**
	 * @return the groupCode
	 */
	@Column(name="group_cd")
	public String getGroupCode() {
		return groupCode;
	}

	/**
	 * @return the parentName
	 */
	@Column(name="parent_nm", isReadOnly=true)
	public String getParentName() {
		return parentName;
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
	 * @param productCode the productCode to set
	 */
	public void setProductCode(String productCode) {
		this.productCode = productCode;
	}

	/**
	 * @param parentCode the parentCode to set
	 */
	public void setParentCode(String parentCode) {
		this.parentCode = parentCode;
	}

	/**
	 * @param uom the uom to set
	 */
	public void setUom(UnitMeasure uom) {
		this.uom = uom;
	}

	/**
	 * @param categoryCode the categoryCode to set
	 */
	public void setCategoryCode(String categoryCode) {
		this.categoryCode = categoryCode;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param price the price to set
	 */
	public void setPrice(double price) {
		this.price = price;
	}

	/**
	 * @param activeFlag the activeFlag to set
	 */
	public void setActiveFlag(int activeFlag) {
		this.activeFlag = activeFlag;
	}

	/**
	 * @param orderNumber the orderNumber to set
	 */
	public void setOrderNumber(int orderNumber) {
		this.orderNumber = orderNumber;
	}

	/**
	 * @param dataType the dataType to set
	 */
	public void setDataType(DataType dataType) {
		this.dataType = dataType;
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
	 * @param scheduleFlag the scheduleFlag to set
	 */
	public void setScheduleFlag(int scheduleFlag) {
		this.scheduleFlag = scheduleFlag;
	}

	/**
	 * @param categoryName the categoryName to set
	 */
	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	/**
	 * @param groupCode the groupCode to set
	 */
	public void setGroupCode(String groupCode) {
		this.groupCode = groupCode;
	}

	/**
	 * @param parentName the parentName to set
	 */
	public void setParentName(String parentName) {
		this.parentName = parentName;
	}

}

