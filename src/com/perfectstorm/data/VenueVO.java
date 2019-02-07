package com.perfectstorm.data;

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

/****************************************************************************
 * <b>Title</b>: VenueVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value Object for a Venue location
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 4, 2019
 * @updates:
 ****************************************************************************/
@Table(name="ps_venue")
public class VenueVO extends GeocodeLocation {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3863178709122752267L;
	
	// Members
	private String venueId;
	private String venueName;
	private String venueDescription;
	private int activeFlag;
	private int manualGeocodeFlag;
	private Date createDate;
	private Date updateDate;
	
	// Bean SubElements
	private List<VenueAttributeVO> attributes = new ArrayList<>();

	/**
	 * 
	 */
	public VenueVO() {
		super();
	}

	/**
	 * @param req
	 */
	public VenueVO(ActionRequest req) {
		this.populateData(req);
	}

	/**
	 * @param rs
	 */
	public VenueVO(ResultSet rs) {
		this.populateData(rs);
	}

	/**
	 * @return the venueId
	 */
	@Column(name="venue_id", isPrimaryKey=true)
	public String getVenueId() {
		return venueId;
	}

	/**
	 * @return the venueName
	 */
	@Column(name="venue_nm")
	public String getVenueName() {
		return venueName;
	}

	/**
	 * @return the venueDescription
	 */
	@Column(name="venue_desc")
	public String getVenueDescription() {
		return venueDescription;
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
	 * @return the updateDate
	 */
	@Column(name="update_dt", isUpdateOnly=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @return the manualGeocodeFlag
	 */
	@Column(name="manual_geocode_flg")
	public int getManualGeocodeFlag() {
		return manualGeocodeFlag;
	}

	/**
	 * @return the attributes
	 */
	public List<VenueAttributeVO> getAttributes() {
		return attributes;
	}

	/**
	 * @param venueId the venueId to set
	 */
	public void setVenueId(String venueId) {
		this.venueId = venueId;
	}

	/**
	 * @param venueName the venueName to set
	 */
	public void setVenueName(String venueName) {
		this.venueName = venueName;
	}

	/**
	 * @param venueDescription the venueDescription to set
	 */
	public void setVenueDescription(String venueDescription) {
		this.venueDescription = venueDescription;
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

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	/**
	 * @param attributes the attributes to set
	 */
	public void setAttributes(List<VenueAttributeVO> attributes) {
		this.attributes = attributes;
	}
	
	/**
	 * 
	 * @param attribute
	 */
	@BeanSubElement
	public void addAttribute(VenueAttributeVO attribute) {
		attributes.add(attribute);
	}

	/**
	 * @param manualGeocodeFlag the manualGeocodeFlag to set
	 */
	public void setManualGeocodeFlag(int manualGeocodeFlag) {
		this.manualGeocodeFlag = manualGeocodeFlag;
	}
}

