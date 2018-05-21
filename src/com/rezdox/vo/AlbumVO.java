package com.rezdox.vo;

//Java 8
import java.io.Serializable;
import java.util.Date;

// SMTBaseLibs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/*****************************************************************************
 <p><b>Title</b>: AlbumVO.java</p>
 <p><b>Description: </b>Value object that encapsulates a RezDox photo album.</p>
 <p> 
 <p>Copyright: (c) 2018 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author Tim Johnson
 @version 1.0
 @since Feb 22, 2018
 <b>Changes:</b> 
 ***************************************************************************/

@Table(name="REZDOX_ALBUM")
public class AlbumVO extends BeanDataVO implements Serializable {
	private static final long serialVersionUID = -2320409186167844402L;

	private String albumId;
	private String residenceId;
	private String businessId;
	private String albumName;
	private String imageUrl;
	private Date createDate;
	private Date updateDate;

	public AlbumVO() {
		super();
	}

	/**
	 * @param req
	 */
	public AlbumVO(ActionRequest req) {
		this();
		populateData(req);
	}
	
	/**
	 * @return the albumId
	 */
	@Column(name="album_id", isPrimaryKey=true)
	public String getAlbumId() {
		return albumId;
	}

	/**
	 * @param albumId the albumId to set
	 */
	public void setAlbumId(String albumId) {
		this.albumId = albumId;
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
	 * @return the businessId
	 */
	@Column(name="business_id")
	public String getBusinessId() {
		return businessId;
	}

	/**
	 * @param businessId the businessId to set
	 */
	public void setBusinessId(String businessId) {
		this.businessId = businessId;
	}

	/**
	 * @return the albumName
	 */
	@Column(name="album_nm")
	public String getAlbumName() {
		return albumName;
	}

	/**
	 * @param albumName the albumName to set
	 */
	public void setAlbumName(String albumName) {
		this.albumName = albumName;
	}

	/**
	 * @return the imageUrl
	 */
	@Column(name="image_url", isReadOnly=true)
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
