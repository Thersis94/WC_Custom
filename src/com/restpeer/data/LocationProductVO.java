package com.restpeer.data;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: LocationProductVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> value object for a given product in the location item master
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 22, 2019
 * @updates:
 ****************************************************************************/
@Table(name="rp_location_product_xr")
public class LocationProductVO extends ProductVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7043175199546786779L;
	
	// Members
	private String locationProductId;
	private String memberLocationId;
	private int qtyAvailable;
	private int qtyPurchased;
	
	// Sub-Beans
	private List<ProductScheduleVO> schedules = new ArrayList<>();

	/**
	 * 
	 */
	public LocationProductVO() {
		super();
	}

	/**
	 * @param req
	 */
	public LocationProductVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public LocationProductVO(ResultSet rs) {
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
	 * @return the locationProductId
	 */
	@Column(name="location_product_id", isPrimaryKey=true)
	public String getLocationProductId() {
		return locationProductId;
	}

	/**
	 * @return the memberLocationId
	 */
	@Column(name="member_location_id")
	public String getMemberLocationId() {
		return memberLocationId;
	}

	/**
	 * @return the qtyAvailable
	 */
	@Column(name="qty_available_no")
	public int getQtyAvailable() {
		return qtyAvailable;
	}

	/**
	 * @return the qtyPurchased
	 */
	@Column(name="qty_purchased_no")
	public int getQtyPurchased() {
		return qtyPurchased;
	}

	/**
	 * @param locationProductId the locationProductId to set
	 */
	public void setLocationProductId(String locationProductId) {
		this.locationProductId = locationProductId;
	}

	/**
	 * @param memberLocationId the memberLocationId to set
	 */
	public void setMemberLocationId(String memberLocationId) {
		this.memberLocationId = memberLocationId;
	}

	/**
	 * @param qtyAvailable the qtyAvailable to set
	 */
	public void setQtyAvailable(int qtyAvailable) {
		this.qtyAvailable = qtyAvailable;
	}

	/**
	 * @param qtyPurchased the qtyPurchased to set
	 */
	public void setQtyPurchased(int qtyPurchased) {
		this.qtyPurchased = qtyPurchased;
	}

	/**
	 * @return the schedules
	 */
	public List<ProductScheduleVO> getSchedules() {
		return schedules;
	}

	/**
	 * @param schedules the schedules to set
	 */
	public void setSchedules(List<ProductScheduleVO> schedules) {
		this.schedules = schedules;
	}
	
	/**
	 * @param schedules the schedules to set
	 */
	@BeanSubElement
	public void addSchedule(ProductScheduleVO schedule) {
		if (schedule != null && ! StringUtil.isEmpty(locationProductId))
			this.schedules.add(schedule);
	}

}

