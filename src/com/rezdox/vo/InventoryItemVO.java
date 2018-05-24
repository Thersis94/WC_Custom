package com.rezdox.vo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataMapper;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;

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
@Table(name="rezdox_treasure_item")
public class InventoryItemVO {

	private String treasureItemId;
	private String ownerMemberId;
	private String residenceId;
	private String roomId;
	private String treasureCategoryCd;
	private String treasureCategoryName;
	private String beneficiaryName;
	private String itemName;
	private double valuationNo;
	private int quantityNo;
	private List<PhotoVO> photos;
	private List<DocumentVO> documents;
	private List<InventoryAttributeVO> itemAttributes;

	//cosmetic values
	private String roomName;
	private String warrantyExp;

	public InventoryItemVO() {
		super();
		photos = new ArrayList<>();
	}

	/**
	 * @param req
	 * @return
	 */
	public static InventoryItemVO instanceOf(ActionRequest req) {
		InventoryItemVO vo = new InventoryItemVO();
		BeanDataMapper.parseBean(vo, req.getParameterMap());

		//if the owner was not specifically passed, presume the logged-in user:
		if (StringUtil.isEmpty(vo.getOwnerMemberId())) {
			MemberVO owner = (MemberVO) req.getSession().getAttribute(Constants.USER_DATA);
			vo.setOwnerMemberId(owner.getMemberId());
		}
		return vo;
	}

	@Column(name="treasure_item_id", isPrimaryKey=true)
	public String getTreasureItemId() {
		return treasureItemId;
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

	@Column(name="beneficiary_nm")
	public String getBeneficiaryName() {
		return beneficiaryName;
	}

	@Column(name="item_nm")
	public String getItemName() {
		return itemName;
	}

	@Column(name="valuation_no")
	public double getValuationNo() {
		return valuationNo;
	}

	@Column(name="quantity_no")
	public int getQuantityNo() {
		return quantityNo;
	}

	public List<PhotoVO> getPhotos() {
		return photos;
	}

	public List<DocumentVO> getDocuments() {
		return documents;
	}

	public List<InventoryAttributeVO> getItemAttributes() {
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

	@Column(name="room_nm", isReadOnly=true)
	public String getRoomName() {
		return roomName;
	}


	public void setTreasureItemId(String treasureItemId) {
		this.treasureItemId = treasureItemId;
	}

	public void setTreasureCategoryName(String treasureCategoryName) {
		this.treasureCategoryName = treasureCategoryName;
	}

	public void setOwnerMemberId(String ownerMemberId) {
		this.ownerMemberId = StringUtil.checkVal(ownerMemberId, null);
	}

	public void setResidenceId(String residenceId) {
		this.residenceId = StringUtil.checkVal(residenceId, null);
	}

	public void setRoomId(String roomId) {
		this.roomId = StringUtil.checkVal(roomId, null);
	}

	public void setTreasureCategoryCd(String treasureCategoryCd) {
		this.treasureCategoryCd = StringUtil.checkVal(treasureCategoryCd, null);
	}

	public void setBeneficiaryName(String beneficiaryName) {
		this.beneficiaryName = beneficiaryName;
	}

	public void setItemName(String itemName) {
		this.itemName = itemName;
	}

	public void setValuationNo(double valuationNo) {
		this.valuationNo = valuationNo;
	}

	public void setQuantityNo(int quantityNo) {
		this.quantityNo = quantityNo;
	}

	@BeanSubElement
	public void addPhoto(PhotoVO photo) {
		photos.add(photo);
	}

	public void setPhotos(List<PhotoVO> photos) {
		this.photos = photos;
	}

	public void setDocuments(List<DocumentVO> documents) {
		this.documents = documents;
	}

	public void setItemAttributes(List<InventoryAttributeVO> itemAttributes) {
		this.itemAttributes = itemAttributes;
	}

	public void setRoomName(String roomName) {
		this.roomName = roomName;
	}

	@Column(name="warranty_exp", isReadOnly=true)
	public String getWarrantyExp() {
		return warrantyExp;
	}

	public void setWarrantyExp(String warrantyExp) {
		this.warrantyExp = warrantyExp;
	}

	public Date getWarrantyExpDate() {
		return !StringUtil.isEmpty(warrantyExp) ? Convert.formatDate(Convert.DATE_DASH_PATTERN, warrantyExp) : null;
	}
}