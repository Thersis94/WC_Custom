package com.mindbody.security;

import java.sql.ResultSet;
import java.util.List;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.user.HumanNameIntfc;

/****************************************************************************
 * <b>Title:</b> MindBodyUser.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages Extended data for MindBody UserData.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 20, 2017
 ****************************************************************************/
public class MindBodyUserVO extends UserDataVO implements HumanNameIntfc {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private List<Object> visits;
	private List<Object> schedule;
	private List<Object> services;
	private List<Object> purchases;

	/**
	 * 
	 */
	public MindBodyUserVO() {
		super();
	}
	/**
	 * @param req
	 */
	public MindBodyUserVO(ActionRequest req) {
		super(req);
	}
	/**
	 * @param rs
	 */
	public MindBodyUserVO(ResultSet rs) {
		super(rs);
	}
	/**
	 * @param req
	 */
	public MindBodyUserVO(SMTServletRequest req) {
		super(req);
	}
	/**
	 * @return the visits
	 */
	public List<Object> getVisits() {
		return visits;
	}
	/**
	 * @return the schedule
	 */
	public List<Object> getSchedule() {
		return schedule;
	}
	/**
	 * @return the services
	 */
	public List<Object> getServices() {
		return services;
	}
	/**
	 * @return the purchases
	 */
	public List<Object> getPurchases() {
		return purchases;
	}
	/**
	 * @param visits the visits to set.
	 */
	public void setVisits(List<Object> visits) {
		this.visits = visits;
	}
	/**
	 * @param schedule the schedule to set.
	 */
	public void setSchedule(List<Object> schedule) {
		this.schedule = schedule;
	}
	/**
	 * @param services the services to set.
	 */
	public void setServices(List<Object> services) {
		this.services = services;
	}
	/**
	 * @param purchases the purchases to set.
	 */
	public void setPurchases(List<Object> purchases) {
		this.purchases = purchases;
	}
}