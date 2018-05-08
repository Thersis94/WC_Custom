package com.rezdox.vo;

import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataMapper;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: RoomVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> This holds the information needed for one room 
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Mar 15, 2018
 * @updates:
 ****************************************************************************/
@Table(name="REZDOX_ROOM")
public class RoomVO {

	private String roomId;
	private String categoryName;
	private String roomName;
	private String residenceId;
	private String roomCategoryCode;
	private Date createDate;
	private Date updateDate;
	private int lengthFootNo;
	private int lengthInchNo;
	private int widthFootNo;
	private int widthInchNo;
	private int heightFootNo;
	private int heightInchNo;

	public RoomVO() {
		super();
	}


	/**
	 * Create a new VO using data auto-filled off the request.
	 * Request parameter names must match setter method names, sans the "set".
	 * e.g. setFirstName -> req.getParameter("firstName"); 
	 * @param req
	 * @return
	 */
	public static RoomVO instanceOf(ActionRequest req) {
		RoomVO vo = new RoomVO();
		BeanDataMapper.parseBean(vo, req.getParameterMap());
		return vo;
	}


	/**
	 * @return the roomId
	 */
	@Column(name="room_id", isPrimaryKey=true)
	public String getRoomId() {
		return roomId;
	}
	/**
	 * @param roomId the roomId to set
	 */
	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}

	/**
	 * @return the residenceId
	 */
	@Column(name="residence_id")
	public String getResidenceId() {
		return residenceId;
	}

	/**
	 * @param residenceId the residenceId to set
	 */
	public void setResidenceId(String residenceId) {
		this.residenceId = residenceId;
	}

	/**
	 * @return the roomCategoryName
	 */
	@Column(name="category_nm", isReadOnly=true)
	public String getCategoryName() {
		return categoryName;
	}

	/**
	 * @param roomCategoryName the roomCategoryName to set
	 */
	public void setCategoryName(String catName) {
		this.categoryName = catName;
	}

	/**
	 * @return the roomCategoryCode
	 */
	@Column(name="room_category_cd")
	public String getRoomCategoryCode() {
		return roomCategoryCode;
	}

	/**
	 * @param roomCategoryCode the roomCategoryCode to set
	 */
	public void setRoomCategoryCode(String roomCategoryCode) {
		this.roomCategoryCode = roomCategoryCode;
	}

	/**
	 * @return the lengthFootNo
	 */
	@Column(name="length_foot_no")
	public int getLengthFootNo() {
		return lengthFootNo;
	}

	/**
	 * @param lengthFootNo the lengthFootNo to set
	 */
	public void setLengthFootNo(int lengthFootNo) {
		this.lengthFootNo = lengthFootNo;
	}

	/**
	 * @return the widthFootNo
	 */
	@Column(name="width_foot_no")
	public int getWidthFootNo() {
		return widthFootNo;
	}

	/**
	 * @param widthFootNo the widthFootNo to set
	 */
	public void setWidthFootNo(int widthFootNo) {
		this.widthFootNo = widthFootNo;
	}

	/**
	 * @return the lengthInchNo
	 */
	@Column(name="length_inch_no")
	public int getLengthInchNo() {
		return lengthInchNo;
	}

	/**
	 * @param lengthInchNo the lengthInchNo to set
	 */
	public void setLengthInchNo(int lengthInchNo) {
		this.lengthInchNo = lengthInchNo;
	}

	/**
	 * @return the widthInchNo
	 */
	@Column(name="width_inch_no")
	public int getWidthInchNo() {
		return widthInchNo;
	}

	/**
	 * @param widthInchNo the widthInchNo to set
	 */
	public void setWidthInchNo(int widthInchNo) {
		this.widthInchNo = widthInchNo;
	}

	/**
	 * @return the heightFootNo
	 */
	@Column(name="height_foot_no")
	public int getHeightFootNo() {
		return heightFootNo;
	}

	/**
	 * @param heightFootNo the heightFootNo to set
	 */
	public void setHeightFootNo(int heightFootNo) {
		this.heightFootNo = heightFootNo;
	}

	/**
	 * @return the heightInchNo
	 */
	@Column(name="height_inch_no")
	public int getHeightInchNo() {
		return heightInchNo;
	}

	/**
	 * @param heightInchNo the heightInchNo to set
	 */
	public void setHeightInchNo(int heightInchNo) {
		this.heightInchNo = heightInchNo;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	/**
	 * @return the roomName
	 */
	@Column(name="room_nm")
	public String getRoomName() {
		return roomName;
	}

	/**
	 * @param roomName the roomName to set
	 */
	public void setRoomName(String roomName) {
		this.roomName = roomName;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return StringUtil.getToString(this);
	}
}