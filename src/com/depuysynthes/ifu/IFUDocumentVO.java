package com.depuysynthes.ifu;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;

/****************************************************************************
 * <b>Title</b>: IFUDocumentInstanceVO.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: Contains instance specific information for an IFU document.
 * This includes items such as the language, document alias, and any technique
 * guides pertaining to this instance of the document</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since March 10, 2015<p/>
 * <b>Changes: </b>
 ****************************************************************************/

public class IFUDocumentVO extends IFUVO implements Serializable{
	  private static final long serialVersionUID = -8409489653118817155L;
	
	private String documentId;
	private String ifuId;
	private String url;
	private String mediaBinId;
	private String languageCd;
	private String atricleTxt;
	private String partNoTxt;
	private String defaultMsgTxt;
	private List<TechniqueGuideVO> tgList;
	
	public IFUDocumentVO() {
		setTgList(new ArrayList<TechniqueGuideVO>());
	}

	public IFUDocumentVO(SMTServletRequest req) {
		super(req);
		this.setDocumentId(req.getParameter("documentId"));
		this.setDocumentId(req.getParameter("ifuId"));
		this.setUrl(req.getParameter("url"));
		this.setMediaBinId(req.getParameter("mediaBinId"));
		this.setLanguageCd(req.getParameter("languageCd"));
		this.setAtricleTxt(req.getParameter("articleTxt"));
		this.setPartNoTxt(req.getParameter("partNo"));
		this.setDefaultMsgTxt(req.getParameter("defaultMsgTxt"));
		setTgList(new ArrayList<TechniqueGuideVO>());
	}

	public IFUDocumentVO(ResultSet rs) {
		super(rs);
		DBUtil db = new DBUtil();
		this.setDocumentId(db.getStringVal("DEPUY_IFU_IMPL_ID", rs));
		this.setIfuId(db.getStringVal("DEPUY_IFU_ID", rs));
		this.setUrl(db.getStringVal("URL_TXT", rs));
		this.setMediaBinId(db.getStringVal("DPY_SYN_MEDIABIN_ID", rs));
		this.setLanguageCd(db.getStringVal("LANGUAGE_CD", rs));
		this.setAtricleTxt(db.getStringVal("ARTICLE_TXT", rs));
		this.setPartNoTxt(db.getStringVal("PART_NO_TXT", rs));
		this.setDefaultMsgTxt(db.getStringVal("DEFAULT_MSG_TXT", rs));
		db = null;
		setTgList(new ArrayList<TechniqueGuideVO>());
	}

	public String getDocumentId() {
		return documentId;
	}

	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}

	public String getIfuId() {
		return ifuId;
	}

	public void setIfuId(String ifuId) {
		this.ifuId = ifuId;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getMediaBinId() {
		return mediaBinId;
	}

	public void setMediaBinId(String mediaBinId) {
		this.mediaBinId = mediaBinId;
	}

	public String getLanguageCd() {
		return languageCd;
	}

	public void setLanguageCd(String languageCd) {
		this.languageCd = languageCd;
	}

	public String getAtricleTxt() {
		return atricleTxt;
	}

	public void setAtricleTxt(String atricleTxt) {
		this.atricleTxt = atricleTxt;
	}

	public String getPartNoTxt() {
		return partNoTxt;
	}

	public void setPartNoTxt(String partNoTxt) {
		this.partNoTxt = partNoTxt;
	}

	public String getDefaultMsgTxt() {
		return defaultMsgTxt;
	}

	public void setDefaultMsgTxt(String defaultMsgTxt) {
		this.defaultMsgTxt = defaultMsgTxt;
	}

	public List<TechniqueGuideVO> getTgList() {
		return tgList;
	}

	public void setTgList(List<TechniqueGuideVO> tgList) {
		this.tgList = tgList;
	}
	
	public void addTg(TechniqueGuideVO vo) {
		tgList.add(vo);
	}

}
