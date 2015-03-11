package com.depuysynthes.ifu;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;

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
	private String title;
	private String version;
	private int archiveFlg = 0;
	private int orderNo;
	private Map<String, IFUDocumentVO> ifuDocuments;
	
	public IFUVO() {
		ifuDocuments = new HashMap<String, IFUDocumentVO>();
	}
	
	public IFUVO(SMTServletRequest req) {
		this();
		this.setIfuId(req.getParameter("ifuId"));
		this.setIfuGroupId(req.getParameter("ifuGroupId"));
		this.setTitle(req.getParameter("title"));
		this.setVersion(req.getParameter("version"));
		this.setOrderNo(Convert.formatInteger(req.getParameter("orderNo")));
		this.setArchiveFlg(Convert.formatInteger(req.getParameter("archiveFlg")));
	}
	
	public IFUVO(ResultSet rs) {
		this();
		DBUtil db = new DBUtil();
		this.setIfuId(db.getStringVal("DEPUY_IFU_ID", rs));
		this.setIfuGroupId(db.getStringVal("DEPUY_IFU_GROUP_ID", rs));
		this.setTitle(db.getStringVal("TITLE_TXT", rs));
		this.setVersion(db.getStringVal("VERSION_TXT", rs));
		this.setOrderNo(db.getIntVal("ORDER_NO", rs));
		this.setArchiveFlg(db.getIntVal("ARCHIVE_FLG", rs));
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

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
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

}
