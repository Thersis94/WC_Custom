package com.restpeer.data;

// JDK 1.8.x
import java.sql.ResultSet;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.dealer.DealerAttributeVO;

/****************************************************************************
 * <b>Title</b>: LocationAttributeVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the attribute data for a given location
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 15, 2019
 * @updates:
 ****************************************************************************/
@Table(name="rp_location_attribute_xr")
public class LocationAttributeVO extends DealerAttributeVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9136179220827598325L;

	// Members
	private String locationAttributeId;
	private String memberLocationId;
	private String value;
	private String metaValue;
	private String userId;
	
	// Sub-bean Members
	private RPUserVO user;
	
	/**
	 * 
	 */
	public LocationAttributeVO() {
		super();
	}

	/**
	 * @param req
	 */
	public LocationAttributeVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public LocationAttributeVO(ResultSet rs) {
		super(rs);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.data.parser.BeanDataVO#toString()
	 */
	@Override
	public String toString() {
		return StringUtil.getToString(this);
	}

	/**
	 * @return the locationAttributeId
	 */
	@Column(name="location_attribute_id", isPrimaryKey=true)
	public String getLocationAttributeId() {
		return locationAttributeId;
	}

	/**
	 * @return the memberLocationId
	 */
	@Column(name="member_location_id")
	public String getMemberLocationId() {
		return memberLocationId;
	}

	/**
	 * @return the value
	 */
	@Column(name="value_txt")
	public String getValue() {
		return value;
	}

	/**
	 * @return the metaValue
	 */
	@Column(name="meta_value_txt")
	public String getMetaValue() {
		return metaValue;
	}

	/**
	 * @return the userId
	 */
	@Column(name="user_id")
	public String getUserId() {
		return userId;
	}

	/**
	 * @return the user
	 */
	public RPUserVO getUser() {
		return user;
	}

	/**
	 * @param locationAttributeId the locationAttributeId to set
	 */
	public void setLocationAttributeId(String locationAttributeId) {
		this.locationAttributeId = locationAttributeId;
	}

	/**
	 * @param memberLocationId the memberLocationId to set
	 */
	public void setMemberLocationId(String memberLocationId) {
		this.memberLocationId = memberLocationId;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * @param metaValue the metaValue to set
	 */
	public void setMetaValue(String metaValue) {
		this.metaValue = metaValue;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * @param user the user to set
	 */
	@BeanSubElement
	public void setUser(RPUserVO user) {
		this.user = user;
	}
}

