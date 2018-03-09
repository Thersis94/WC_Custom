package com.depuysynthes.srt.vo;

import java.sql.ResultSet;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.gis.Location;

/****************************************************************************
 * <b>Title:</b> SRTLocationVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Stores SRT Request Location Address Data.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Feb 15, 2018
 ****************************************************************************/
@Table(name="DPY_SYN_SRT_REQUEST_ADDRESS")
public class SRTLocationVO extends Location {

	private String requestId;
	private String requestAddressId;

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public SRTLocationVO() {
		super();
	}

	public SRTLocationVO(ActionRequest req) {
		super();
		this.populateData(req);
	}
	
	/**
	 * 
	 * @param rs
	 */
	public SRTLocationVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * Creates the location object in a single, delimited string of the address
	 * @param fullAddress
	 */
	public SRTLocationVO(String fullAddress) {
		super(fullAddress, false);
	}

	/**
	 * Creates the location object either as a single line address or 
	 * as a delimited set of the address parts.
	 * @param fullAddress
	 * @param useSingleLineAddress
	 */
	public SRTLocationVO(String fullAddress, boolean useSingleLineAddress) {
		super(fullAddress, useSingleLineAddress);
	}

	/**
	 * Creates and populates the Loc bean
	 * @param addr
	 * @param city
	 * @param state
	 * @param zip
	 */
	public SRTLocationVO(String addr, String city, String state, String zip) {
		super();
	}

	/**
	 * @return the requestId
	 */
	@Column(name="REQUEST_ID")
	public String getRequestId() {
		return requestId;
	}

	/**
	 * @return the requestAddressId
	 */
	@Column(name="REQUEST_ADDRESS_ID", isPrimaryKey=true)
	public String getRequestAddressId() {
		return requestAddressId;
	}

	/**
	 * @param requestId the requestId to set.
	 */
	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	/**
	 * @param requestAddressId the requestAddressId to set.
	 */
	public void setRequestAddressId(String requestAddressId) {
		this.requestAddressId = requestAddressId;
	}	
}