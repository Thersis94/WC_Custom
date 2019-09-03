package com.mts.subscriber.data;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs 3.5
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: UserExtendedVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Stores entity data associated to an MTS user
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jun 11, 2019
 * @updates:
 ****************************************************************************/
@Table(name="mts_user_info")
public class UserExtendedVO extends BeanDataVO {
	/**
	 * Enum for the user info type code
	 */
	public enum TypeCode {
		BOOKMARK("User Bookmark");
		
		private String typeName;
		private TypeCode(String typeName) { this.typeName = typeName; }
		public String getTypeName() { return typeName; }
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8266034395717953003L;
	
	// Members
	private String userInfoId;
	private String userId;
	private String value;
	private TypeCode infoTypeCode;
	private Date createDate;
	
	/**
	 * 
	 */
	public UserExtendedVO() {
		super();
	}

	/**
	 * @param req
	 */
	public UserExtendedVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public UserExtendedVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the userInfoId
	 */
	@Column(name="user_info_id", isPrimaryKey=true)
	public String getUserInfoId() {
		return userInfoId;
	}

	/**
	 * @return the userId
	 */
	@Column(name="user_id")
	public String getUserId() {
		return userId;
	}

	/**
	 * @return the value
	 */
	@Column(name="value_txt")
	public String getValue() {
		return value;
	}

	/**
	 * @return the infoTypeCode
	 */
	@Column(name="user_info_type_cd")
	public TypeCode getInfoTypeCode() {
		return infoTypeCode;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param userInfoId the userInfoId to set
	 */
	public void setUserInfoId(String userInfoId) {
		this.userInfoId = userInfoId;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * @param infoTypeCode the infoTypeCode to set
	 */
	public void setInfoTypeCode(TypeCode infoTypeCode) {
		this.infoTypeCode = infoTypeCode;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

}

