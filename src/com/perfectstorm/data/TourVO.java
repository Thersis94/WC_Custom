package com.perfectstorm.data;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: TourVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for a tour of venues
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 4, 2019
 * @updates:
 ****************************************************************************/
@Table(name="ps_tour")
public class TourVO extends BeanDataVO {
	
	/** 
	 * Defines the type of tour
	 */
	public enum TourType {
		TOUR ("Multi-Location Tour"),
		ROUTE ("Route"),
		EVENT ("Single Event");
		
		private String tourName;
		private TourType(String tourName) {this.tourName = tourName; }
		public String getTourName() { return this.tourName; }
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3604160386126666322L;
	
	// Members
	private String tourId;
	private String customerId;
	private String name;
	private String desc;
	private int activeFlag;
	private long numberVenues;
	private Date startDate;
	private Date endDate;
	private Date createDate;
	private Date updateDate;
	private TourType tourTypeCode;
	
	// Bean SubElements
	private List<VenueTourVO> venues = new ArrayList<>();
	
	/**
	 * 
	 */
	public TourVO() {
		super();
	}

	/**
	 * @param req
	 */
	public TourVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public TourVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the tourId
	 */
	@Column(name="tour_id", isPrimaryKey=true)
	public String getTourId() {
		return tourId;
	}

	/**
	 * @return the name
	 */
	@Column(name="tour_nm")
	public String getName() {
		return name;
	}

	/**
	 * @return the startDate
	 */
	@Column(name="start_dt")
	public Date getStartDate() {
		return startDate;
	}

	/**
	 * @return the endDate
	 */
	@Column(name="end_dt")
	public Date getEndDate() {
		return endDate;
	}

	/**
	 * @return the activeFlag
	 */
	@Column(name="active_flg")
	public int getActiveFlag() {
		return activeFlag;
	}

	/**
	 * @return the desc
	 */
	@Column(name="tour_desc")
	public String getDesc() {
		return desc;
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
	 * @return the tourTypeCode
	 */
	@Column(name="tour_type_cd")
	public TourType getTourTypeCode() {
		return tourTypeCode;
	}

	/**
	 * @return the numberVenues
	 */
	@Column(name="venue_no", isReadOnly=true)
	public long getNumberVenues() {
		return numberVenues;
	}

	/**
	 * @return the customerId
	 */
	@Column(name="customer_id")
	public String getCustomerId() {
		return customerId;
	}

	/**
	 * @param tourId the tourId to set
	 */
	public void setTourId(String tourId) {
		this.tourId = tourId;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param startDate the startDate to set
	 */
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	/**
	 * @param endDate the endDate to set
	 */
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
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
	 * @return the venues
	 */
	public List<VenueTourVO> getVenues() {
		return venues;
	}

	/**
	 * @param venues the venues to set
	 */
	public void setVenues(List<VenueTourVO> venues) {
		this.venues = venues;
	}

	/**
	 * 
	 * @param venue
	 */
	@BeanSubElement
	public void addVenue(VenueTourVO venue) {
		venues.add(venue);
	}

	/**
	 * @param activeFlag the activeFlag to set
	 */
	public void setActiveFlag(int activeFlag) {
		this.activeFlag = activeFlag;
	}

	/**
	 * @param desc the desc to set
	 */
	public void setDesc(String desc) {
		this.desc = desc;
	}

	/**
	 * @param tourTypeCode the tourTypeCode to set
	 */
	public void setTourTypeCode(TourType tourTypeCode) {
		this.tourTypeCode = tourTypeCode;
	}

	/**
	 * @param numberVenues the numberVenues to set
	 */
	public void setNumberVenues(long numberVenues) {
		this.numberVenues = numberVenues;
	}

	/**
	 * @param customerId the customerId to set
	 */
	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}
}

