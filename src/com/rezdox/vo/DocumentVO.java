package com.rezdox.vo;

import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataMapper;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title:</b> DocumentVO.java<br/>
 * <b>Description:</b> RezDox Document - binds to Treasure Items as well as Projects.  See data model.
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Feb 26, 2018
 ****************************************************************************/
@Table(name="REZDOX_DOCUMENT")
public class DocumentVO {

	private String documentId;
	private String treasureItemId;
	private String projectId;
	private String documentName;
	private String descriptionText;
	private String filePath;

	public DocumentVO() {
		super();
	}

	/**
	 * @param req
	 * @return
	 */
	public static DocumentVO instanceOf(ActionRequest req) {
		DocumentVO vo = new DocumentVO();
		BeanDataMapper.parseBean(vo, req.getParameterMap());
		return vo;
	}

	@Column(name="document_id", isPrimaryKey=true)
	public String getDocumentId() {
		return documentId;
	}

	@Column(name="treasure_item_id")
	public String getTreasureItemId() {
		return treasureItemId;
	}

	@Column(name="project_id")
	public String getProjectId() {
		return projectId;
	}

	@Column(name="document_nm")
	public String getDocumentName() {
		return documentName;
	}

	@Column(name="description_txt")
	public String getDescriptionText() {
		return descriptionText;
	}

	@Column(name="file_pth")
	public String getFilePath() {
		return filePath;
	}

	@Column(name="create_dt", isInsertOnly=true)
	public Date getCreateDate() {
		return Convert.getCurrentTimestamp();
	}

	@Column(name="update_dt", isUpdateOnly=true)
	public Date getUpdateDate() {
		return Convert.getCurrentTimestamp();
	}


	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}

	public void setTreasureItemId(String treasureItemId) {
		this.treasureItemId = treasureItemId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public void setDocumentName(String documentName) {
		this.documentName = documentName;
	}

	public void setDescriptionText(String descriptionText) {
		this.descriptionText = descriptionText;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
}