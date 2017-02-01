package com.ansmed.sb.physician;

import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.AbstractSiteBuilderVO;

/****************************************************************************
 * <b>Title</b>: DocumentVO.java<p/>
 * <b>Description: Data bean for storing physician documents</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2007<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since August 15, 2007
 ****************************************************************************/
public class DocumentVO extends AbstractSiteBuilderVO {
	private static final long serialVersionUID = 1l;
	
	private String documentTypeId = null;
	private String surgeonId = null;
	private String documentName = null;
	private String documentTypeName = null;
	
	public DocumentVO() {}
	
	
	public DocumentVO(ResultSet rs) {
		setData(rs);
	}
	
	public DocumentVO(ActionRequest req) {
		setData(req);
	}
	
	/**
	 * Assigns the elements of a row to the appropriate variable
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		actionId = db.getStringVal("document_id", rs);
		documentTypeId = db.getStringVal("document_type_id", rs);
		surgeonId = db.getStringVal("surgeon_id", rs);
		documentName = db.getStringVal("document_nm", rs);
		documentTypeName = db.getStringVal("type_nm", rs);
	}
	
	/**
	 * Assigns the elements of a request to the appropriate variable
	 * @param rs
	 */
	public void setData(ActionRequest req) {
		actionId = req.getParameter("documentId");
		documentTypeId = req.getParameter("documentTypeId");
		surgeonId = req.getParameter("surgeonId");
		documentName = req.getParameter("documentName");
	}
	
	public String getDocumentTypeId() {
		return documentTypeId;
	}

	public void setDocumentTypeId(String documentTypeId) {
		this.documentTypeId = documentTypeId;
	}

	public String getSurgeonId() {
		return surgeonId;
	}

	public void setSurgeonId(String surgeonId) {
		this.surgeonId = surgeonId;
	}

	public String getDocumentName() {
		return documentName;
	}

	public void setDocumentName(String documentName) {
		this.documentName = documentName;
	}


	public String getDocumentTypeName() {
		return documentTypeName;
	}


	public void setDocumentTypeName(String documentTypeName) {
		this.documentTypeName = documentTypeName;
	}

}
