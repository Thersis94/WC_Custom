package com.depuysynthes.ifu;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.http.SMTServletRequest;

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

public class IFUContainer {
	
	private String ifuId;
	private String ifuGroupId;
	private String title;
	private String version;
	private int visibleFlg;
	private int orderNo;
	private Map<String, IFUDocumentVO> ifuDocuments;
	
	public IFUContainer() {
		ifuDocuments = new HashMap<String, IFUDocumentVO>();
	}
	
	public IFUContainer(SMTServletRequest req) {
		super();
		setData(req);
	}
	

	
	public IFUContainer(ResultSet rs) {
		super();
		setData(rs);
	}
	
	public void setData(SMTServletRequest req) {
		
	}
	
	public void setData(ResultSet rs) {
		
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

	public int getVisibleFlg() {
		return visibleFlg;
	}

	public void setVisibleFlg(int visibleFlg) {
		this.visibleFlg = visibleFlg;
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

}
