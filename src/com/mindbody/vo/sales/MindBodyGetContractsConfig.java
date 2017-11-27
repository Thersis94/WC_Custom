package com.mindbody.vo.sales;

import java.util.ArrayList;
import java.util.List;

import com.mindbody.MindBodySaleApi.SaleDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;

/****************************************************************************
 * <b>Title:</b> MindBodyGetContractsConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages Config for GetContract Endpoint
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 26, 2017
 ****************************************************************************/
public class MindBodyGetContractsConfig extends MindBodySalesConfig {

	private Integer locationId;
	private List<Integer> contractIds;
	private boolean soldOnline;

	/**
	 * @param type
	 * @param source
	 * @param user
	 */
	public MindBodyGetContractsConfig(MindBodyCredentialVO source) {
		super(SaleDocumentType.GET_CONTRACTS, source, null);
		contractIds = new ArrayList<>();
	}

	/**
	 * @return the locationId
	 */
	public Integer getLocationId() {
		return locationId;
	}

	/**
	 * @return the contractIds
	 */
	public List<Integer> getContractIds() {
		return contractIds;
	}

	/**
	 * @return the soldOnline
	 */
	public boolean isSoldOnline() {
		return soldOnline;
	}

	/**
	 * @param locationId the locationId to set.
	 */
	public void setLocationId(Integer locationId) {
		this.locationId = locationId;
	}

	/**
	 * @param contractIds the contractIds to set.
	 */
	public void setContractIds(List<Integer> contractIds) {
		this.contractIds = contractIds;
	}

	/**
	 * @param soldOnline the soldOnline to set.
	 */
	public void setSoldOnline(boolean soldOnline) {
		this.soldOnline = soldOnline;
	}

	public void addContractId(Integer i) {
		contractIds.add(i);
	}

	@Override
	public boolean isValid() {
		return super.isValid() && locationId != null;
	}
}