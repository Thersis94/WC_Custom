package com.wsla.data.ticket;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.wsla.common.WSLALocales;

/****************************************************************************
 * <b>Title</b>: StatusNotificationVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object holding the data for the mapping of roles,
 * language and status to an email campaign instance for notifications
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Nov 1, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_ticket_status_notification")
public class StatusNotificationVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3220873373394703944L;

	// Member Variables
	private String statusNotificationId;
	private String campaignInstanceId;
	private StatusCode statusCode;
	private String locale;
	private String roleId;
	private Date createDate;
	
	// Helpers
	private String campaignInstanceName;
	private String statusCodeName;
	private String localeName;
	
	/**
	 * 
	 */
	public StatusNotificationVO() {
		super();
	}

	/**
	 * @param req
	 */
	public StatusNotificationVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public StatusNotificationVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the statusNotificationId
	 */
	@Column(name="status_notification_id", isPrimaryKey=true)
	public String getStatusNotificationId() {
		return statusNotificationId;
	}

	/**
	 * @return the campaignInstanceId
	 */
	@Column(name="campaign_instance_id")
	public String getCampaignInstanceId() {
		return campaignInstanceId;
	}

	/**
	 * @return the statusCode
	 */
	@Column(name="status_cd")
	public StatusCode getStatusCode() {
		return statusCode;
	}

	/**
	 * @return the locale
	 */
	@Column(name="user_locale_txt")
	public String getLocale() {
		return locale;
	}

	/**
	 * @return the campaignInstanceName
	 */
	@Column(name="instance_nm")
	public String getCampaignInstanceName() {
		return campaignInstanceName;
	}

	/**
	 * @return the statusCodeName
	 */
	@Column(name="status_nm")
	public String getStatusCodeName() {
		return statusCodeName;
	}

	/**
	 * @return the localeName
	 */
	public String getLocaleName() {
		return localeName;
	}

	/**
	 * @return the roleId
	 */
	@Column(name="role_id")
	public String getRoleId() {
		return roleId;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param statusNotificationId the statusNotificationId to set
	 */
	public void setStatusNotificationId(String statusNotificationId) {
		this.statusNotificationId = statusNotificationId;
	}

	/**
	 * @param campaignInstanceId the campaignInstanceId to set
	 */
	public void setCampaignInstanceId(String campaignInstanceId) {
		this.campaignInstanceId = campaignInstanceId;
	}

	/**
	 * @param statusCode the statusCode to set
	 */
	public void setStatusCode(StatusCode statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * @param locale the locale to set
	 */
	public void setLocale(String locale) {
		this.locale = locale;
		
		if (locale != null) {
			localeName = WSLALocales.valueOf(locale).getDesc();
		}
	}

	/**
	 * @param roleId the roleId to set
	 */
	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @param campaignInstanceName the campaignInstanceName to set
	 */
	public void setCampaignInstanceName(String campaignInstanceName) {
		this.campaignInstanceName = campaignInstanceName;
	}

	/**
	 * @param statusCodeName the statusCodeName to set
	 */
	public void setStatusCodeName(String statusCodeName) {
		this.statusCodeName = statusCodeName;
	}

	/**
	 * @param localeName the localeName to set
	 */
	public void setLocaleName(String localeName) {
		this.localeName = localeName;
	}

}

