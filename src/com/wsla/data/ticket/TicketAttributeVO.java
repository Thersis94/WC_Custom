package com.wsla.data.ticket;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs 3.5
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: TicketAttributeVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object holding a single attribute for a given ticket
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 14, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_ticket_attribute")
public class TicketAttributeVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -21594505312062484L;

	// Member Variable
	private String attributeCode;
	private String attributeGroupCode;
	private String name;
	private int activeFlag;
	private String attributeGroupName;
	private String scriptText;
	private String noteText;
	private Date createDate;
	private Date updateDate;
	
	/**
	 * 
	 */
	public TicketAttributeVO() {
		super();
	}
	
	/**
	 * @param req
	 */
	public TicketAttributeVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public TicketAttributeVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the attributeCode
	 */
	@Column(name="attribute_cd", isPrimaryKey=true)
	public String getAttributeCode() {
		return attributeCode;
	}

	/**
	 * @return the name
	 */
	@Column(name="attribute_nm")
	public String getName() {
		return name;
	}

	/**
	 * @return the activeFlag
	 */
	@Column(name="active_flg")
	public int getActiveFlag() {
		return activeFlag;
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
	 * @return the attributeGroupCode
	 */
	@Column(name="attribute_group_cd")
	public String getAttributeGroupCode() {
		return attributeGroupCode;
	}

	/**
	 * @param attributeCode the attributeCode to set
	 */
	public void setAttributeCode(String attributeCode) {
		this.attributeCode = attributeCode;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param activeFlag the activeFlag to set
	 */
	public void setActiveFlag(int activeFlag) {
		this.activeFlag = activeFlag;
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
	 * @param attributeGroupCode the attributeGroupCode to set
	 */
	public void setAttributeGroupCode(String attributeGroupCode) {
		this.attributeGroupCode = attributeGroupCode;
	}

	/**
	 * @return the attributeGroupName
	 */
	@Column(name="group_nm", isReadOnly=true)
	public String getAttributeGroupName() {
		return attributeGroupName;
	}

	/**
	 * @param attributeGroupName the attributeGroupName to set
	 */
	public void setAttributeGroupName(String attributeGroupName) {
		this.attributeGroupName = attributeGroupName;
	}

	/**
	 * @return the scriptText
	 */
	@Column(name="script_txt")
	public String getScriptText() {
		return scriptText;
	}

	/**
	 * @param scriptText the scriptText to set
	 */
	public void setScriptText(String scriptText) {
		this.scriptText = scriptText;
	}

	/**
	 * @return the noteText
	 */
	@Column(name="note_txt")
	public String getNoteText() {
		return noteText;
	}

	/**
	 * @param noteText the noteText to set
	 */
	public void setNoteText(String noteText) {
		this.noteText = noteText;
	}

}

