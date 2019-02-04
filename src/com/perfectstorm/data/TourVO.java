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
	 * 
	 */
	private static final long serialVersionUID = 3604160386126666322L;
	
	// Members
	private String tourId;
	private String tourManagerId;
	private String name;
	private Date startDate;
	private Date endDate;
	private Date createDate;
	private Date updateDate;
	
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
	 * @return the memberId
	 */
	@Column(name="tour_manager_id")
	public String getTourManagerId() {
		return tourManagerId;
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
	 * @param tourId the tourId to set
	 */
	public void setTourId(String tourId) {
		this.tourId = tourId;
	}

	/**
	 * @param memberId the memberId to set
	 */
	public void setTourManagerId(String tourManagerId) {
		this.tourManagerId = tourManagerId;
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
}

