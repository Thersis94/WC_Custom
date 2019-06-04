package com.mts.publication.data;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: DocumentAssetVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for the assets associated to a publication,
 * issue. or article
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 2, 2019
 * @updates:
 ****************************************************************************/
@Table(name="mts_document_asset")
public class AssetVO extends BeanDataVO {
	
	/**
	 * Enum for the different asset types
	 */
	public enum AssetType {
		FEATURE_IMG("Feature Image (255 x 150)"),
		COVER_IMG("Cover Image"),
		TEASER_IMG("Teaser Image (540 x 360)"),
		PDF_DOC("PDF Document"),
		EXCEL_DOC("Excel Document"),
		GEN_IMAGE("General Image");
		
		private String assetName;
		private AssetType(String assetName)  { this.assetName = assetName; }
		public String getAssetName() { return assetName; }
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -260036909553669405L;
	
	// Members
	private String documentAssetId;
	private String objectKeyId;
	private String documentName;
	private String documentPath;
	private String thumbnailPath;
	private AssetType assetType;
	private Date createDate;
	
	// Helpers
	private String assetTypeName;

	/**
	 * 
	 */
	public AssetVO() {
		super();
	}

	/**
	 * @param req
	 */
	public AssetVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public AssetVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the documentAssetId
	 */
	@Column(name="document_asset_id", isPrimaryKey=true)
	public String getDocumentAssetId() {
		return documentAssetId;
	}

	/**
	 * @return the objectReferenceId
	 */
	@Column(name="object_key_id")
	public String getObjectKeyId() {
		return objectKeyId;
	}

	/**
	 * @return the documentName
	 */
	@Column(name="document_nm")
	public String getDocumentName() {
		return documentName;
	}

	/**
	 * @return the documentPath
	 */
	@Column(name="document_path")
	public String getDocumentPath() {
		return documentPath;
	}

	/**
	 * @return the thumbnailPath
	 */
	@Column(name="thumbnail_path")
	public String getThumbnailPath() {
		return thumbnailPath;
	}

	/**
	 * @return the assetType
	 */
	@Column(name="asset_type_cd")
	public AssetType getAssetType() {
		return assetType;
	}

	/**
	 * @return the assetTypeName
	 */
	@Column(name="type_nm", isReadOnly=true)
	public String getAssetTypeName() {
		return assetTypeName;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param documentAssetId the documentAssetId to set
	 */
	public void setDocumentAssetId(String documentAssetId) {
		this.documentAssetId = documentAssetId;
	}

	/**
	 * @param objectReferenceId the objectReferenceId to set
	 */
	public void setObjectKeyId(String objectKeyId) {
		this.objectKeyId = objectKeyId;
	}

	/**
	 * @param documentName the documentName to set
	 */
	public void setDocumentName(String documentName) {
		this.documentName = documentName;
	}

	/**
	 * @param documentPath the documentPath to set
	 */
	public void setDocumentPath(String documentPath) {
		this.documentPath = documentPath;
	}

	/**
	 * @param thumbnailPath the thumbnailPath to set
	 */
	public void setThumbnailPath(String thumbnailPath) {
		this.thumbnailPath = thumbnailPath;
	}

	/**
	 * @param assetType the assetType to set
	 */
	public void setAssetType(AssetType assetType) {
		this.assetType = assetType;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @param assetTypeName the assetTypeName to set
	 */
	public void setAssetTypeName(String assetTypeName) {
		this.assetTypeName = assetTypeName;
	}

}

