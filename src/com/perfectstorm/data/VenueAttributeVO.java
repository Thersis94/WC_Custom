package com.perfectstorm.data;

// JDK 1.8.x
import java.sql.ResultSet;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: VenueAttributeVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for an instance of an attribute 
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 4, 2019
 * @updates:
 ****************************************************************************/
@Table(name="ps_venue_attribute_xr")
public class VenueAttributeVO extends AttributeVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6264797334856461222L;

	// Members
	private String venueAttributeId;
	private String venueId;
	private String value;
	
	/**
	 * 
	 */
	public VenueAttributeVO() {
		super();
	}

	/**
	 * @param req
	 */
	public VenueAttributeVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public VenueAttributeVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the venueAttributeId
	 */
	@Column(name="venue_attribute_id", isPrimaryKey=true)
	public String getVenueAttributeId() {
		return venueAttributeId;
	}

	/**
	 * @return the venueId
	 */
	@Column(name="venue_id")
	public String getVenueId() {
		return venueId;
	}

	/**
	 * @return the value
	 */
	@Column(name="value_txt")
	public String getValue() {
		return value;
	}

	/**
	 * @param venueAttributeId the venueAttributeId to set
	 */
	public void setVenueAttributeId(String venueAttributeId) {
		this.venueAttributeId = venueAttributeId;
	}

	/**
	 * @param venueId the venueId to set
	 */
	public void setVenueId(String venueId) {
		this.venueId = venueId;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}

}

