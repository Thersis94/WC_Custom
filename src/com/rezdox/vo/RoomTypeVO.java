package com.rezdox.vo;

import java.io.Serializable;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: RoomTypeVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> holds the data for one room type
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Mar 16, 2018
 * @updates:
 ****************************************************************************/

@Table(name="REZDOX_ROOM")
public class RoomTypeVO extends BeanDataVO implements Serializable {

	private static final long serialVersionUID = -6304414841588523655L;

	private String roomTypeCode;
	private String roomCategoryCode;
	private String typeName;
	private Date createDate;
	
	public RoomTypeVO() {
		super();
	}
	/**
	 * @param req
	 */
	public RoomTypeVO(ActionRequest req) {
		this();
		populateData(req);
	}
	
	
	/**
	 * @return the roomTypeCode
	 */
	@Column(name="room_type_cd", isPrimaryKey=true)
	public String getRoomTypeCode() {
		return roomTypeCode;
	}
	/**
	 * @param roomTypeCode the roomTypeCode to set
	 */
	public void setRoomTypeCode(String roomTypeCode) {
		this.roomTypeCode = roomTypeCode;
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
	 * @return the typeName
	 */
	@Column(name="type_nm")
	public String getTypeName() {
		return typeName;
	}
	/**
	 * @param typeName the typeName to set
	 */
	public void setTypeName(String typeName) {
		this.typeName = typeName;
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
	
	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.data.parser.BeanDataVO#toString()
	 */
	@Override
	public String toString() {
		return StringUtil.getToString(this);
	}
	
}
