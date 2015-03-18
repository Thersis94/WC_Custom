package com.depuysynthes.ifu;

import java.sql.ResultSet;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.approval.ApprovalController.SyncStatus;

/****************************************************************************
 * <b>Title</b>: IFUContainer.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: Top level container vo for the IFU documents.  This is designed
 * to hold all instances of the the document as well as related metadata that
 * is shared amongst all documents.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since March 10, 2015<p/>
 * <b>Changes: </b>
 ****************************************************************************/

public class IFUVO {
	
	private String ifuId;
	private String ifuGroupId;
	private String titleText;
	private String versionText;
	private int archiveFlg = 0;
	private int orderNo;
	private Map<String, IFUDocumentVO> ifuDocuments;
	private Date createDate = null;
	private String businessUnitName;
	private SyncStatus status = SyncStatus.APPROVED;
	
	public IFUVO() {
		ifuDocuments = new HashMap<String, IFUDocumentVO>();
	}
	
	public IFUVO(SMTServletRequest req) {
		this();
		this.setIfuId(req.getParameter("ifuId"));
		this.setIfuGroupId(req.getParameter("ifuGroupId"));
		this.setTitleText(req.getParameter("actionName"));
		this.setVersionText(req.getParameter("versionText"));
		this.setOrderNo(Convert.formatInteger(req.getParameter("orderNo")));
		this.setArchiveFlg(Convert.formatInteger(req.getParameter("archiveFlg")));
		this.setBusinessUnitName(req.getParameter("businessUnitName"));
	}
	
	public IFUVO(ResultSet rs) {
		this();
		DBUtil db = new DBUtil();
		this.setIfuId(db.getStringVal("DEPUY_IFU_ID", rs));
		this.setIfuGroupId(db.getStringVal("DEPUY_IFU_GROUP_ID", rs));
		this.setTitleText(db.getStringVal("TITLE_TXT", rs));
		this.setVersionText(db.getStringVal("VERSION_TXT", rs));
		this.setOrderNo(db.getIntVal("ORDER_NO", rs));
		this.setArchiveFlg(db.getIntVal("ARCHIVE_FLG", rs));
		this.setCreateDate(db.getDateVal("CREATE_DT", rs));
		this.setBusinessUnitName(db.getStringVal("BUSINESS_UNIT_NM", rs));
		if (db.getStringVal("WC_SYNC_STATUS_CD", rs) != null)
			this.setStatus(SyncStatus.valueOf(db.getStringVal("WC_SYNC_STATUS_CD", rs)));
		db = null;
	}

	public String getIfuId() {
		return ifuId;
	}

	public void setIfuId(String ifuId) {
		this.ifuId = ifuId;
	}

	public String getIfuGroupId() {
		return ifuGroupId;
	}

	public void setIfuGroupId(String ifuGroupId) {
		this.ifuGroupId = ifuGroupId;
	}

	public String getTitleText() {
		return titleText;
	}

	public void setTitleText(String titleText) {
		this.titleText = titleText;
	}

	public String getVersionText() {
		return versionText;
	}

	public void setVersionText(String versionText) {
		this.versionText = versionText;
	}

	public int getArchiveFlg() {
		return archiveFlg;
	}

	public void setArchiveFlg(int archiveFlg) {
		this.archiveFlg = archiveFlg;
	}

	public int getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(int orderNo) {
		this.orderNo = orderNo;
	}

	public Map<String, IFUDocumentVO> getIfuDocuments() {
		return ifuDocuments;
	}

	public void setIfuDocuments(Map<String, IFUDocumentVO> ifuDocuments) {
		this.ifuDocuments = ifuDocuments;
	}
	
	public void addIfuDocument(String key, IFUDocumentVO doc) {
		ifuDocuments.put(key, doc);
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public String getBusinessUnitName() {
		return businessUnitName;
	}

	public void setBusinessUnitName(String businessUnitName) {
		this.businessUnitName = businessUnitName;
	}

	public SyncStatus getStatus() {
		return status;
	}

	public void setStatus(SyncStatus status) {
		this.status = status;
	}

}
