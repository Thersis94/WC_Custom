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
	private String ipStart;
	private String ipEnd;
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
	 * @return the ipSecurityId
	 */
	@Column(name="ip_security_id", isPrimaryKey=true)
	public String getIpSecurityId() {
		return ipSecurityId;
	}

	/**
	 * @return the ipStart
	 */
	@Column(name="ip_start_txt")
	public String getIpStart() {
		return ipStart;
	}

	/**
	 * @return the ipEnd
	 */
	@Column(name="ip_end_txt")
	public String getIpEnd() {
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
	@Column(name="udpate_dt", isUpdateOnly=true, isAutoGen=true)
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
	public void setIpStart(String ipStart) {
		this.ipStart = ipStart;
	}

	/**
	 * @param ipEnd the ipEnd to set
	 */
	public void setIpEnd(String ipEnd) {
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

}

