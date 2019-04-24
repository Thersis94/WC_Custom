package com.restpeer.data;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: MemberLocationVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Location for a given member
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * @author James Camire
 * @version 3.0
 * @since Feb 15, 2019
 * @updates:
 ****************************************************************************/
@Table(name="rp_member_location")
public class MemberLocationVO extends GeocodeLocation {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3944925442978292169L;

	// Members
	private String memberLocationId;
	private String memberId;
	private String locationName;
	private String locationDesc;
	private int manualGeocodeFlag;
	private int activeFlag;
	private Date createDate;
	
	// Sub-Beans
	private List<LocationAttributeVO> attributes = new ArrayList<>();
	private List<ProductVO> products = new ArrayList<>();
	private List<LocationUserVO> users = new ArrayList<>();
	
	// Helpers
	private String memberTypeCode;
	private String memberName;
	private double distance;
	
	/**
	 * 
	 */
	public MemberLocationVO() {
		super();
	}

	/**
	 * @param req
	 */
	public MemberLocationVO(ActionRequest req) {
		this.populateData(req);
	}

	/**
	 * @param rs
	 */
	public MemberLocationVO(ResultSet rs) {
		this.populateData(rs);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.gis.GeocodeLocation#toString()
	 */
	@Override
	public String toString() {
		return StringUtil.getToString(this);
	}

	/**
	 * @return the memberLocationId
	 */
	@Column(name="member_location_id", isPrimaryKey=true)
	public String getMemberLocationId() {
		return memberLocationId;
	}

	/**
	 * @return the memberId
	 */
	@Column(name="member_id")
	public String getMemberId() {
		return memberId;
	}

	/**
	 * @return the locationName
	 */
	@Column(name="location_nm")
	public String getLocationName() {
		return locationName;
	}

	/**
	 * @return the locationDesc
	 */
	@Column(name="location_desc")
	public String getLocationDesc() {
		return locationDesc;
	}

	/**
	 * @return the manualGeocodeFlag
	 */
	@Column(name="manual_geocode_flg")
	public int getManualGeocodeFlag() {
		return manualGeocodeFlag;
	}

	/**
	 * @return the activeFlag
	 */
	@Column(name="active_flg")
	public int getActiveFlag() {
		return activeFlag;
	}

	/**
	 * @return the memberTypeCode
	 */
	@Column(name="member_type_cd", isReadOnly=true)
	public String getMemberTypeCode() {
		return memberTypeCode;
	}

	/**
	 * @return the attributes
	 */
	public List<LocationAttributeVO> getAttributes() {
		return attributes;
	}

	/**
	 * @return the products
	 */
	public List<ProductVO> getProducts() {
		return products;
	}

	/**
	 * @return the users
	 */
	public List<LocationUserVO> getUsers() {
		return users;
	}
	
	/**
	 * 
	 * @return
	 */
	@Column(name="member_nm", isReadOnly=true)
	public String getMemberName() {
		return memberName;
	}

	/**
	 * @return the distance
	 */
	@Column(name="distance", isReadOnly=true)
	public double getDistance() {
		return distance;
	}

	/**
	 * @param memberLocationId the memberLocationId to set
	 */
	public void setMemberLocationId(String memberLocationId) {
		this.memberLocationId = memberLocationId;
	}

	/**
	 * @param memberId the memberId to set
	 */
	public void setMemberId(String memberId) {
		this.memberId = memberId;
	}

	/**
	 * @param locationName the locationName to set
	 */
	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}

	/**
	 * @param attributes the attributes to set
	 */
	public void setAttributes(List<LocationAttributeVO> attributes) {
		this.attributes = attributes;
	}

	/**
	 * 
	 * @param attribute
	 */
	@BeanSubElement
	public void addAttribute(LocationAttributeVO attribute) {
		this.attributes.add(attribute);
	}
	
	/**
	 * @param products the products to set
	 */
	public void setProducts(List<ProductVO> products) {
		this.products = products;
	}

	/**
	 * 
	 * @param product
	 */
	@BeanSubElement
	public void addProduct(ProductVO product) {
		this.products.add(product);
	}
	
	/**
	 * @param users the users to set
	 */
	public void setUsers(List<LocationUserVO> users) {
		this.users = users;
	}

	/**
	 * 
	 * @param user
	 */
	@BeanSubElement
	public void addUser(LocationUserVO user) {
		this.users.add(user);
	}
	
	/**
	 * @param locationDesc the locationDesc to set
	 */
	public void setLocationDesc(String locationDesc) {
		this.locationDesc = locationDesc;
	}

	/**
	 * @param manualGeocodeFlag the manualGeocodeFlag to set
	 */
	public void setManualGeocodeFlag(int manualGeocodeFlag) {
		this.manualGeocodeFlag = manualGeocodeFlag;
	}

	/**
	 * @param activeFlag the activeFlag to set
	 */
	public void setActiveFlag(int activeFlag) {
		this.activeFlag = activeFlag;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @param memberTypeCode the memberTypeCode to set
	 */
	public void setMemberTypeCode(String memberTypeCode) {
		this.memberTypeCode = memberTypeCode;
	}

	/**
	 * 
	 * @param memberName
	 */
	public void setMemberName(String memberName) {
		this.memberName = memberName;
	}

	/**
	 * @param distance the distance to set
	 */
	public void setDistance(double distance) {
		this.distance = Math.round(distance);
	}

}

