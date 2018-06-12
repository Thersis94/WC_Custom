package com.irricurb.action.data.vo;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: NetworkTypeVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Defines the different network types for a device
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 30, 2018
 * @updates:
 ****************************************************************************/
@Table(name="ic_network_type")
public class NetworkTypeVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1023878833986836653L;

	// Member Variables
	private String networkType;
	private String name;
	private Date createDate;
	
	/**
	 * 
	 */
	public NetworkTypeVO() {
		super();
	}

	/**
	 * @param req
	 */
	public NetworkTypeVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public NetworkTypeVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the networkType
	 */
	@Column(name="network_type_cd", isPrimaryKey=true)
	public String getNetworkType() {
		return networkType;
	}

	/**
	 * @return the name
	 */
	@Column(name="type_nm")
	public String getName() {
		return name;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param networkType the networkType to set
	 */
	public void setNetworkType(String networkType) {
		this.networkType = networkType;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

}

