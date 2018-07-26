package com.depuysynthes.srt.vo;

import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.gis.Location;
import com.siliconmtn.security.UserDataVO;

/****************************************************************************
 * <b>Title:</b> SRTRequestActionCO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Stores Request Address Data.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Feb 28, 2018
 ****************************************************************************/
@Table(name="DPY_SYN_SRT_REQUEST_ADDRESS")
public class SRTRequestAddressVO extends Location {

	/**
	 *
	 */
	private static final long serialVersionUID = 8702728650671238249L;
	private String requestAddressId;
	private String requestId;
	private Date createDt;

	public SRTRequestAddressVO() {
		super();
	}

	public SRTRequestAddressVO(ActionRequest req) {
		populateData(req);
	}

	public SRTRequestAddressVO(ResultSet rs) {
		populateData(rs);
	}

	public SRTRequestAddressVO(UserDataVO u) {
		this();
		setData(u);
	}

	/**
	 * Store Given Location Data on this Address Record.
	 * @param gl
	 */
	public SRTRequestAddressVO(GeocodeLocation gl) {
		super(gl.getAddress(), gl.getCity(), gl.getState(), gl.getZipCode());
		this.address2 = gl.getAddress2();
	}

	/**
	 * @return the requestAddressId
	 */
	@Column(name="REQUEST_ADDRESS_ID", isPrimaryKey=true)
	public String getRequestAddressId() {
		return requestAddressId;
	}

	/**
	 * @return the requestId
	 */
	@Column(name="REQUEST_ID", isInsertOnly=true)
	public String getRequestId() {
		return requestId;
	}

	/**
	 * @return the createDt
	 */
	@Column(name="CREATE_DT", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDt() {
		return createDt;
	}

	/**
	 * @param requestAddressId the requestAddressId to set.
	 */
	public void setRequestAddressId(String requestAddressId) {
		this.requestAddressId = requestAddressId;
	}

	/**
	 * @param requestId the requestId to set.
	 */
	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	/**
	 * @param createDt the createDt to set.
	 */
	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}
}