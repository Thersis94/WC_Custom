package com.perfectstorm.data;

// JDK 1.8.x
import java.sql.ResultSet;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: VenueTourAttributeVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for an instance of a venue tour attribute 
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Mar 5, 2019
 * @updates:
 ****************************************************************************/
@Table(name="ps_venue_tour_attribute_xr")
public class VenueTourAttributeVO extends AttributeVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2435915949259983846L;

	// Members
	private String venueTourAttributeId;
	private String venueTourId;
	private int value;
	
	/**
	 * 
	 */
	public VenueTourAttributeVO() {
		super();
	}

	/**
	 * @param req
	 */
	public VenueTourAttributeVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public VenueTourAttributeVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the venueTourAttributeId
	 */
	@Column(name="venue_tour_attribute_id", isPrimaryKey=true)
	public String getVenueTourAttributeId() {
		return venueTourAttributeId;
	}

	/**
	 * @return the venueTourId
	 */
	@Column(name="venue_tour_id")
	public String getVenueTourId() {
		return venueTourId;
	}

	/**
	 * @return the value
	 */
	@Column(name="value_no")
	public int getValue() {
		return value;
	}

	/**
	 * @param venueTourAttributeId the venueTourAttributeId to set
	 */
	public void setVenueTourAttributeId(String venueTourAttributeId) {
		this.venueTourAttributeId = venueTourAttributeId;
	}

	/**
	 * @param venueTourId the venueTourId to set
	 */
	public void setVenueTourId(String venueTourId) {
		this.venueTourId = venueTourId;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(int value) {
		this.value = value;
	}

}

