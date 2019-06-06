package com.wsla.data.ticket;

import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: FailureReportVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Holds the data for one row of the 
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since May 3, 2019
 * @updates:
 ****************************************************************************/
public class FailureReportVO extends BeanDataVO {

	private static final long serialVersionUID = -5850656130989716480L;
	
	private String custProductId;
	private String productName;
	private int repairNumber;
	private int refundNumber;
	private int casConfigNumber;
	private int replaceNumber;
	private int totalUnits;
	private int periodFailure;
	private int allFailures;
	private float failureRate;
	private String providerName;


	/**
	 * 
	 */
	public FailureReportVO() {
		super();
	}


	/**
	 * @return the custProductId
	 */
	public String getCustProductId() {
		return custProductId;
	}


	/**
	 * @param custProductId the custProductId to set
	 */
	public void setCustProductId(String custProductId) {
		this.custProductId = custProductId;
	}


	/**
	 * @return the productName
	 */
	public String getProductName() {
		return productName;
	}


	/**
	 * @param productName the productName to set
	 */
	public void setProductName(String productName) {
		this.productName = productName;
	}


	/**
	 * @return the repairNumber
	 */
	public int getRepairNumber() {
		return repairNumber;
	}


	/**
	 * @param repairNumber the repairNumber to set
	 */
	public void setRepairNumber(int repairNumber) {
		this.repairNumber = repairNumber;
	}


	/**
	 * @return the refundNumber
	 */
	public int getRefundNumber() {
		return refundNumber;
	}


	/**
	 * @param refundNumber the refundNumber to set
	 */
	public void setRefundNumber(int refundNumber) {
		this.refundNumber = refundNumber;
	}


	/**
	 * @return the replaceNumber
	 */
	public int getReplaceNumber() {
		return replaceNumber;
	}


	/**
	 * @param replaceNumber the replaceNumber to set
	 */
	public void setReplaceNumber(int replaceNumber) {
		this.replaceNumber = replaceNumber;
	}


	/**
	 * @return the casConfigNumber
	 */
	public int getCasConfigNumber() {
		return casConfigNumber;
	}


	/**
	 * @param casConfigNumber the casConfigNumber to set
	 */
	public void setCasConfigNumber(int casConfigNumber) {
		this.casConfigNumber = casConfigNumber;
	}


	/**
	 * @return the totalUnits
	 */
	public int getTotalUnits() {
		return totalUnits;
	}


	/**
	 * @param totalUnits the totalUnits to set
	 */
	public void setTotalUnits(int totalUnits) {
		this.totalUnits = totalUnits;
	}


	/**
	 * @return the periodFailure
	 */
	public int getPeriodFailure() {
		return periodFailure;
	}


	/**
	 * @param periodfailure the periodFailure to set
	 */
	public void setPeriodFailure(int periodFailure) {
		this.periodFailure = periodFailure;
	}


	/**
	 * @return the allFailures
	 */
	public int getAllFailures() {
		return allFailures;
	}


	/**
	 * @param allFailures the allFailures to set
	 */
	public void setAllFailures(int allFailures) {
		this.allFailures = allFailures;
	}


	/**
	 * @return the failureRate
	 */
	public float getFailureRate() {
		return failureRate;
	}


	/**
	 * @param failureRate the failureRate to set
	 */
	public void setFailureRate(float failureRate) {
		this.failureRate = failureRate;
	}


	/**
	 * calculates its own failure rate
	 */
	public void calculateRate() {
		if(getTotalUnits() >0 && getAllFailures() >0) {
			setFailureRate((float)getAllFailures()/(float)getTotalUnits());
		}
	}


	/**
	 * @return the providerName
	 */
	public String getProviderName() {
		return providerName;
	}


	/**
	 * @param providerName the providerName to set
	 */
	public void setProviderName(String providerName) {
		this.providerName = providerName;
	}




	
	

}
