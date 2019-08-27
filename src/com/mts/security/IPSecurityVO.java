package com.mts.security;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// MTS Libs
import com.mts.subscriber.data.MTSUserVO;

// SMT Base Libs 3.5.x
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: IPSecurityVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for the ip address range authentication
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jun 10, 2019
 * @updates:
 ****************************************************************************/
@Table(name="mts_ip_security")
public class IPSecurityVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3296823283791986772L;
	
	// Members
	private String ipSecurityId;
	private String ipBase;
	private int ipStart;
	private int ipEnd;
	private String userId;
	private String companyName;
	private Date createDate;
	private Date updateDate;
	
	// Sub-Bean Elements
	private MTSUserVO user = new MTSUserVO();
	
	/**
	 * 
	 */
	public IPSecurityVO() {
		super();
	}

	/**
	 * @param req
	 */
	public IPSecurityVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public IPSecurityVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * Determines if the provide IP address falls into the range of the ipStart and ipEnd
	 * @param ip
	 * @return
	 */
	public boolean insideIPRange(String ip) {
		String base = ip.substring(0, ip.lastIndexOf('.'));
		int host = Convert.formatInteger(ip.substring(ip.lastIndexOf('.') + 1));
		return (base.equalsIgnoreCase(ipBase) && host >= ipStart && host <= ipEnd);
	}

	/**
	 * @return the ipSecurityId
	 */
	@Column(name="ip_security_id", isPrimaryKey=true)
	public String getIpSecurityId() {
		return ipSecurityId;
	}

	/**
	 * @return the ipStart
	 */
	@Column(name="ip_start_no")
	public int getIpStart() {
		return ipStart;
	}

	/**
	 * @return the ipEnd
	 */
	@Column(name="ip_end_no")
	public int getIpEnd() {
		return ipEnd;
	}

	/**
	 * @return the userId
	 */
	@Column(name="user_id")
	public String getUserId() {
		return userId;
	}

	/**
	 * @return the companyName
	 */
	@Column(name="company_nm")
	public String getCompanyName() {
		return companyName;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isUpdateOnly=true, isAutoGen=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @return the user
	 */
	public MTSUserVO getUser() {
		return user;
	}

	/**
	 * @param ipSecurityId the ipSecurityId to set
	 */
	public void setIpSecurityId(String ipSecurityId) {
		this.ipSecurityId = ipSecurityId;
	}

	/**
	 * @param ipStart the ipStart to set
	 */
	public void setIpStart(int ipStart) {
		this.ipStart = ipStart;
	}

	/**
	 * @param ipEnd the ipEnd to set
	 */
	public void setIpEnd(int ipEnd) {
		this.ipEnd = ipEnd;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * @param companyName the companyName to set
	 */
	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	/**
	 * @param user the user to set
	 */
	@BeanSubElement
	public void setUser(MTSUserVO user) {
		this.user = user;
	}

	/**
	 * @return the ipBase
	 */
	@Column(name="ip_base_txt")
	public String getIpBase() {
		return ipBase;
	}

	/**
	 * @param ipBase the ipBase to set
	 */
	public void setIpBase(String ipBase) {
		this.ipBase = ipBase;
	}

}

