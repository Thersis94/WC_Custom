package com.mindbody.vo.classes;

import com.siliconmtn.security.UserDataVO;

/****************************************************************************
 * <b>Title:</b> MBStaffVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages Staff Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 20, 2017
 ****************************************************************************/
public class MBStaffVO extends UserDataVO {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private int sortOrder;
	private boolean appointmentTrn;
	private boolean reservationTrn;
	private boolean independentContractor;
	private boolean alwaysAllowDoubleBooking;
	private String bio;

	public MBStaffVO() {
		super();
	}

	/**
	 * @return the sortOrder
	 */
	public int getSortOrder() {
		return sortOrder;
	}

	/**
	 * @return the appointmentTrn
	 */
	public boolean isAppointmentTrn() {
		return appointmentTrn;
	}

	/**
	 * @return the reservationTrn
	 */
	public boolean isReservationTrn() {
		return reservationTrn;
	}

	/**
	 * @return the independentContractor
	 */
	public boolean isIndependentContractor() {
		return independentContractor;
	}

	/**
	 * @return the alwaysAllowDoubleBooking
	 */
	public boolean isAlwaysAllowDoubleBooking() {
		return alwaysAllowDoubleBooking;
	}

	/**
	 * @return the bio
	 */
	public String getBio() {
		return bio;
	}

	/**
	 * @param sortOrder the sortOrder to set.
	 */
	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	/**
	 * @param appointmentTrn the appointmentTrn to set.
	 */
	public void setAppointmentTrn(boolean appointmentTrn) {
		this.appointmentTrn = appointmentTrn;
	}

	/**
	 * @param reservationTrn the reservationTrn to set.
	 */
	public void setReservationTrn(boolean reservationTrn) {
		this.reservationTrn = reservationTrn;
	}

	/**
	 * @param independentContractor the independentContractor to set.
	 */
	public void setIndependentContractor(boolean independentContractor) {
		this.independentContractor = independentContractor;
	}

	/**
	 * @param alwaysAllowDoubleBooking the alwaysAllowDoubleBooking to set.
	 */
	public void setAlwaysAllowDoubleBooking(boolean alwaysAllowDoubleBooking) {
		this.alwaysAllowDoubleBooking = alwaysAllowDoubleBooking;
	}

	/**
	 * @param bio the bio to set.
	 */
	public void setBio(String bio) {
		this.bio = bio;
	}
}