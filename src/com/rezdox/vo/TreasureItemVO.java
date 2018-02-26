package com.rezdox.vo;

import java.util.Date;
import java.util.List;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataMapper;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title:</b> TreasureItemVO.java<br/>
 * <b>Description:</b> 
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Feb 26, 2018
 ****************************************************************************/
public class TreasureItemVO {

	private String treasureItemId;
	private String ownerMemberId;
	private String residenceId;
	private String roomId;
	private String treasureCategoryCd;
	private String treasureCategoryName;
	private String beneficiaryMemberId;
	private String itemName;
	private double valiationNo;
	private int quantityNo;
	private List<PhotoVO> photos;
	private List<DocumentVO> documents;
	private List<TreasureItemAttributeVO> itemAttributes;

	public TreasureItemVO() {
		super();
	}

	@Column(name="treasure_item_id", isPrimaryKey=true)
	public String getTreasureItemId() {
		return treasureItemId;
	}

	/**
	 * @param req
	 * @return
	 */
	public static TreasureItemVO instanceOf(ActionRequest req) {
		TreasureItemVO vo = new TreasureItemVO();
		BeanDataMapper.parseBean(vo, req.getParameterMap());
		return vo;
	}

	@Column(name="owner_member_id")
	public String getOwnerMemberId() {
		return ownerMemberId;
	}

	@Column(name="residence_id")
	public String getResidenceId() {
		return residenceId;
	}

	@Column(name="room_id")
	public String getRoomId() {
		return roomId;
	}

	@Column(name="treasure_category_cd")
	public String getTreasureCategoryCd() {
		return treasureCategoryCd;
	}

	@Column(name="category_nm", isReadOnly=true)
	public String getTreasureCategoryName() {
		return treasureCategoryName;
	}

	@Column(name="beneficiary_member_id")
	public String getBeneficiaryMemberId() {
		return beneficiaryMemberId;
	}

	@Column(name="item_nm")
	public String getItemName() {
		return itemName;
	}

	@Column(name="valuation_no")
	public double getValiationNo() {
		return valiationNo;
	}

	@Column(name="quantity_no")
	public int getQuantityNo() {
		return quantityNo;
	}

	//@BeanSubElement  - This method is NOT annotated because it's not part of the SQL query that populates this VO.
	public List<PhotoVO> getPhotos() {
		return photos;
	}

	//@BeanSubElement  - This method is NOT annotated because it's not part of the SQL query that populates this VO.
	public List<DocumentVO> getDocuments() {
		return documents;
	}

	@BeanSubElement
	public List<TreasureItemAttributeVO> getItemAttributes() {
		return itemAttributes;
	}

	@Column(name="create_dt", isInsertOnly=true)
	public Date getCreateDate() {
		return Convert.getCurrentTimestamp();
	}


	@Column(name="update_dt", isUpdateOnly=true)
	public Date getUpdateDate() {
		return Convert.getCurrentTimestamp();
	}


	public void setTreasureItemId(String treasureItemId) {
		this.treasureItemId = treasureItemId;
	}

	public void setTreasureCategoryName(String treasureCategoryName) {
		this.treasureCategoryName = treasureCategoryName;
	}

	public void setOwnerMemberId(String ownerMemberId) {
		this.ownerMemberId = ownerMemberId;
	}

	public void setResidenceId(String residenceId) {
		this.residenceId = residenceId;
	}

	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}

	public void setTreasureCategoryCd(String treasureCategoryCd) {
		this.treasureCategoryCd = treasureCategoryCd;
	}

	public void setBeneficiaryMemberId(String beneficiaryMemberId) {
		this.beneficiaryMemberId = beneficiaryMemberId;
	}

	public void setItemName(String itemName) {
		this.itemName = itemName;
	}

	public void setValiationNo(double valiationNo) {
		this.valiationNo = valiationNo;
	}

	public void setQuantityNo(int quantityNo) {
		this.quantityNo = quantityNo;
	}

	public void setPhotos(List<PhotoVO> photos) {
		this.photos = photos;
	}

	public void setDocuments(List<DocumentVO> documents) {
		this.documents = documents;
	}

	public void setItemAttributes(List<TreasureItemAttributeVO> itemAttributes) {
		this.itemAttributes = itemAttributes;
	}
}