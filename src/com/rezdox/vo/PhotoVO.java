package com.rezdox.vo;

//Java 8
import java.io.Serializable;
import java.util.Date;

// SMTBaseLibs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;

/*****************************************************************************
 <p><b>Title</b>: PhotoVO.java</p>
 <p><b>Description: </b>Value object that encapsulates a RezDox photo.</p>
 <p> 
 <p>Copyright: (c) 2018 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author Tim Johnson
 @version 1.0
 @since Feb 20, 2018
 <b>Changes:</b> 
 ***************************************************************************/
@Table(name="REZDOX_PHOTO")
public class PhotoVO extends BeanDataVO implements Serializable {
	private static final long serialVersionUID = -2320409186167844402L;

	private String photoId;
	private String albumId;
	private String treasureItemId;
	private String projectId;
	private String photoName;
	private String descriptionText;
	private String imageUrl;
	private String thumbnailUrl;
	private int orderNo;
	private Date createDate;
	private Date updateDate;

	public PhotoVO() {
		super();
	}

	/**
	 * @param req
	 */
	public PhotoVO(ActionRequest req) {
		this();
		populateData(req);
	}

	/**
	 * @return the photoId
	 */
	@Column(name="photo_id", isPrimaryKey=true)
	public String getPhotoId() {
		return photoId;
	}

	/**
	 * @param photoId the photoId to set
	 */
	public void setPhotoId(String photoId) {
		this.photoId = photoId;
	}

	/**
	 * @return the albumId
	 */
	@Column(name="album_id")
	public String getAlbumId() {
		return albumId;
	}

	/**
	 * @param albumId the albumId to set
	 */
	public void setAlbumId(String albumId) {
		this.albumId = StringUtil.checkVal(albumId, null);
	}

	/**
	 * @return the treasureItemId
	 */
	@Column(name="treasure_item_id")
	public String getTreasureItemId() {
		return treasureItemId;
	}

	/**
	 * @param treasureItemId the treasureItemId to set
	 */
	public void setTreasureItemId(String treasureItemId) {
		this.treasureItemId = StringUtil.checkVal(treasureItemId, null);
	}

	/**
	 * @return the projectId
	 */
	@Column(name="project_id")
	public String getProjectId() {
		return projectId;
	}

	/**
	 * @param projectId the projectId to set
	 */
	public void setProjectId(String projectId) {
		this.projectId = StringUtil.checkVal(projectId, null);
	}

	/**
	 * @return the photoName
	 */
	@Column(name="photo_nm")
	public String getPhotoName() {
		return photoName;
	}

	/**
	 * @param photoName the photoName to set
	 */
	public void setPhotoName(String photoName) {
		this.photoName = photoName;
	}

	/**
	 * @return the descriptionText
	 */
	@Column(name="desc_txt")
	public String getDescriptionText() {
		return descriptionText;
	}

	/**
	 * @param descriptionText the descriptionText to set
	 */
	public void setDescriptionText(String descriptionText) {
		this.descriptionText = descriptionText;
	}

	/**
	 * @return the imageUrl
	 */
	@Column(name="image_url")
	public String getImageUrl() {
		return imageUrl;
	}

	/**
	 * @param imageUrl the imageUrl to set
	 */
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	/**
	 * @return the thumbnailUrl
	 */
	@Column(name="thumbnail_url")
	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	/**
	 * @param thumbnailUrl the thumbnailUrl to set
	 */
	public void setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}

	/**
	 * @return the orderNo
	 */
	@Column(name="order_no")
	public int getOrderNo() {
		return orderNo;
	}

	/**
	 * @param orderNo the orderNo to set
	 */
	public void setOrderNo(int orderNo) {
		this.orderNo = orderNo;
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
}
