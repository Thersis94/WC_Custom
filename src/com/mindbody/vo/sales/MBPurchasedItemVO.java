package com.mindbody.vo.sales;

/****************************************************************************
 * <b>Title:</b> MBPurchasedItemVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manage MindBody Purchased Item data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 25, 2017
 ****************************************************************************/
public class MBPurchasedItemVO {

	private long id;
	private boolean isService;

	public MBPurchasedItemVO() {
		//Default constructor
	}

	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * @return the isService
	 */
	public boolean isService() {
		return isService;
	}

	/**
	 * @param id the id to set.
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * @param isService the isService to set.
	 */
	public void setService(boolean isService) {
		this.isService = isService;
	}
}