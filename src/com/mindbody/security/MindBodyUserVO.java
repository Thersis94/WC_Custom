package com.mindbody.security;

import java.sql.ResultSet;
import java.util.List;

import com.mindbody.vo.classes.MBClassScheduleVO;
import com.mindbody.vo.clients.MBClientServiceVO;
import com.mindbody.vo.clients.MBVisitVO;
import com.mindbody.vo.sales.MBSaleItemVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.commerce.payment.PaymentVO;
import com.siliconmtn.gis.Location;
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

	private List<MBVisitVO> visits;
	private List<MBClassScheduleVO> schedule;
	private List<MBClientServiceVO> services;
	private List<MBSaleItemVO> purchases;
	private int perkvillePoints;
	private String clientId;

	private PaymentVO cardData;
	private Location billingAddress;

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
	public List<MBVisitVO> getVisits() {
		return visits;
	}
	/**
	 * @return the schedule
	 */
	public List<MBClassScheduleVO> getSchedule() {
		return schedule;
	}
	/**
	 * @return the services
	 */
	public List<MBClientServiceVO> getServices() {
		return services;
	}
	/**
	 * @return the purchases
	 */
	public List<MBSaleItemVO> getPurchases() {
		return purchases;
	}

	public int getPerkvillePoints() {
		return perkvillePoints;
	}

	public int getClassPoints() {
		if(services != null) {
			return services.get(0).getRemaining();
		} else {
			return 0;
		}
	}

	public PaymentVO getCardData() {
		return cardData;
	}

	public Location getBillingAddress() {
		return billingAddress;
	}

	public String getClientId() {
		return clientId;
	}
	/**
	 * @param visits the visits to set.
	 */
	public void setVisits(List<MBVisitVO> visits) {
		this.visits = visits;
	}
	/**
	 * @param schedule the schedule to set.
	 */
	public void setSchedule(List<MBClassScheduleVO> schedule) {
		this.schedule = schedule;
	}
	/**
	 * @param services the services to set.
	 */
	public void setServices(List<MBClientServiceVO> services) {
		this.services = services;
	}
	/**
	 * @param purchases the purchases to set.
	 */
	public void setPurchases(List<MBSaleItemVO> purchases) {
		this.purchases = purchases;
	}

	public void setPerkvillePoints(int perkvillePoints) {
		this.perkvillePoints = perkvillePoints;
	}
	/**
	 * @param convertClientCreditCart
	 */
	public void setClientCardData(PaymentVO cardData) {
		this.cardData = cardData;
	}
	/**
	 * @param convertBillingAddress
	 */
	public void setBillingAddress(Location billingAddress) {
		this.billingAddress = billingAddress;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
}