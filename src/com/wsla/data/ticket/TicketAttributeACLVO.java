package com.wsla.data.ticket;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs 3.x
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: TicketAttributeACLVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Holds a single role item for the given attribute.  Helps define 
 * the security to allow viewing / editing of data attributes on a ticket 
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 17, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_ticket_attribute_acl")
public class TicketAttributeACLVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3658165984038125404L;
	
	// Member Variables
	private String attributeACLCode;
	private String attributeCode;
	private String roleId;
	private String roleName;
	private int readFlag;
	private int writeFlag;
	private Date createDate;

	/**
	 * 
	 */
	public TicketAttributeACLVO() {
		super();
	}

	/**
	 * @param req
	 */
	public TicketAttributeACLVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public TicketAttributeACLVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the attributeACLCode
	 */
	@Column(name="ticket_attribute_acl_cd", isPrimaryKey=true)
	public String getAttributeACLCode() {
		return attributeACLCode;
	}

	/**
	 * @return the attributeCode
	 */
	@Column(name="attribute_cd")
	public String getAttributeCode() {
		return attributeCode;
	}

	/**
	 * @return the roleId
	 */
	@Column(name="role_id")
	public String getRoleId() {
		return roleId;
	}

	/**
	 * @return the readFlag
	 */
	@Column(name="read_flg")
	public int getReadFlag() {
		return readFlag;
	}

	/**
	 * @return the writeFlag
	 */
	@Column(name="write_flg")
	public int getWriteFlag() {
		return writeFlag;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param attributeACLCode the attributeACLCode to set
	 */
	public void setAttributeACLCode(String attributeACLCode) {
		this.attributeACLCode = attributeACLCode;
	}

	/**
	 * @param attributeCode the attributeCode to set
	 */
	public void setAttributeCode(String attributeCode) {
		this.attributeCode = attributeCode;
	}

	/**
	 * @param roleId the roleId to set
	 */
	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}

	/**
	 * @param readFlag the readFlag to set
	 */
	public void setReadFlag(int readFlag) {
		this.readFlag = readFlag;
	}

	/**
	 * @param writeFlag the writeFlag to set
	 */
	public void setWriteFlag(int writeFlag) {
		this.writeFlag = writeFlag;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @return the roleName
	 */
	@Column(name="role_nm", isReadOnly=true)
	public String getRoleName() {
		return roleName;
	}

	/**
	 * @param roleName the roleName to set
	 */
	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

}

