package com.mts.publication.data;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: DocumentCategoryVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object to manage the association between the document 
 * and the category
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 2, 2019
 * @updates:
 ****************************************************************************/
@Table(name="mts_category_document_xr")
public class DocumentCategoryVO extends CategoryVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4805561504210881251L;
	
	// Members
	private String categoryDocumentId;
	private String documentId;
	private int primaryFlag;
	private Date updateDate;

	/**
	 * 
	 */
	public DocumentCategoryVO() {
		super();
	}

	/**
	 * @param req
	 */
	public DocumentCategoryVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public DocumentCategoryVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the categoryDocumentId
	 */
	@Column(name="category_document_id", isPrimaryKey=true)
	public String getCategoryDocumentId() {
		return categoryDocumentId;
	}

	/**
	 * @return the documentId
	 */
	@Column(name="document_id")
	public String getDocumentId() {
		return documentId;
	}

	/**
	 * @return the primaryFlag
	 */
	@Column(name="primary_flg")
	public int getPrimaryFlag() {
		return primaryFlag;
	}

	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isUpdateOnly=true, isAutoGen=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @param categoryDocumentId the categoryDocumentId to set
	 */
	public void setCategoryDocumentId(String categoryDocumentId) {
		this.categoryDocumentId = categoryDocumentId;
	}

	/**
	 * @param documentId the documentId to set
	 */
	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}

	/**
	 * @param primaryFlag the primaryFlag to set
	 */
	public void setPrimaryFlag(int primaryFlag) {
		this.primaryFlag = primaryFlag;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

}

