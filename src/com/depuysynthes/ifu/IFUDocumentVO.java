package com.depuysynthes.ifu;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

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
	
	private String implId;
	private String urlText;
	private String dpySynMediaBinId;
	private String dpySynAssetName;
	private String languageCd;
	private String languageName;
	private String articleText;
	private String partNoText;
	private String defaultMsgText;
	private Map<String, IFUTechniqueGuideVO> nativeLangTgList;
	private Map<String, IFUTechniqueGuideVO> defaultLangTgList;
	private boolean keywordMatched;
	
	public IFUDocumentVO() {
		nativeLangTgList = new LinkedHashMap<>();
		defaultLangTgList = new LinkedHashMap<>();
	}

	public IFUDocumentVO(SMTServletRequest req) {
		super(req);
		nativeLangTgList = new LinkedHashMap<>();
		defaultLangTgList = new LinkedHashMap<>();
		this.setImplId(req.getParameter("implId"));
		this.setIfuId(req.getParameter("ifuId"));
		
		// Check if we're getting a new file that will replace the old one.
		if (req.getFile("instanceFile") != null) {
			this.setUrlText(req.getFile("instanceFile").getFileName());
		} else {
			this.setUrlText(req.getParameter("urlText"));
		}
		
		this.setTitleText(req.getParameter("titleText"));
		this.setDpySynMediaBinId(req.getParameter("dpySynMediaBinId"));
		this.setLanguageCd(req.getParameter("languageCd"));
		this.setArticleText(req.getParameter("articleText"));
		this.setPartNoText(req.getParameter("partNo"));
		this.setDefaultMsgText(req.getParameter("defaultMsgText"));
	}

	public IFUDocumentVO(ResultSet rs) {
		super(rs);
		nativeLangTgList = new LinkedHashMap<>();
		defaultLangTgList = new LinkedHashMap<>();
		DBUtil db = new DBUtil();
		this.setImplId(db.getStringVal("DEPUY_IFU_IMPL_ID", rs));
		this.setIfuId(db.getStringVal("DEPUY_IFU_ID", rs));
		this.setUrlText(db.getStringVal("URL_TXT", rs));
		this.setDpySynMediaBinId(db.getStringVal("DPY_SYN_MEDIABIN_ID", rs));
		this.setLanguageCd(db.getStringVal("LANGUAGE_CD", rs));
		this.setLanguageName(db.getStringVal("LANGUAGE_NM", rs));
		this.setArticleText(db.getStringVal("ARTICLE_TXT", rs));
		this.setPartNoText(db.getStringVal("PART_NO_TXT", rs));
		this.setDefaultMsgText(db.getStringVal("DEFAULT_MSG_TXT", rs));
		db = null;
	}

	public String getImplId() {
		return implId;
	}

	public void setImplId(String implId) {
		this.implId = implId;
	}

	public String getUrlText() {
		return urlText;
	}

	public void setUrlText(String urlText) {
		this.urlText = urlText;
	}

	public String getDpySynMediaBinId() {
		return dpySynMediaBinId;
	}

	public void setDpySynMediaBinId(String dpySynMediaBinId) {
		this.dpySynMediaBinId = dpySynMediaBinId;
	}

	public String getDpySynAssetName() {
		return dpySynAssetName;
	}

	public void setDpySynAssetName(String dpySynAssetNm) {
		this.dpySynAssetName = dpySynAssetNm;
	}

	public String getLanguageCd() {
		return languageCd;
	}

	public void setLanguageCd(String languageCd) {
		this.languageCd = languageCd;
	}

	public String getLanguageName() {
		return languageName;
	}

	public void setLanguageName(String languageName) {
		this.languageName = languageName;
	}

	public String getArticleText() {
		return articleText;
	}

	public void setArticleText(String atricleText) {
		this.articleText = atricleText;
	}

	public String getPartNoText() {
		return partNoText;
	}

	public void setPartNoText(String partNoText) {
		this.partNoText = partNoText;
	}

	public String getDefaultMsgText() {
		return defaultMsgText;
	}

	public void setDefaultMsgText(String defaultMsgText) {
		this.defaultMsgText = defaultMsgText;
	}

	/**
	 * see comment below on addTg method as to why we have two maps here.
	 * @return
	 */
	public Collection<IFUTechniqueGuideVO> getTgList() {
		if (nativeLangTgList.size() > 0) {
			return nativeLangTgList.values();
		} else {
			return defaultLangTgList.values();
		}
	}

	
	/**
	 * hold nativeLang and defaultLang TGs in separate Maps.  
	 * The business rules state if there are none in the native language then the
	 * default language's TGs should appear.  They should not appear intermixed.
	 * @param vo
	 * @param isNativeLang
	 */
	public void addTg(IFUTechniqueGuideVO vo, boolean isNativeLang) {
		if (vo.getTgName() == null || vo.getTgId() == null) return;
			
		if (isNativeLang) {
			nativeLangTgList.put(vo.getTgId(), vo);
		} else {
			defaultLangTgList.put(vo.getTgId(), vo);
		}
		return;
	}
	
	public String getPublicUrl() {
		if (this.dpySynMediaBinId != null && this.dpySynMediaBinId.length() > 0) {
			return IFUFacadeAction.MEDIABIN_PATH + this.getDpySynMediaBinId();
		} else if (this.urlText != null && this.urlText.length() > 0) {
			return IFUFacadeAction.BINARY_PATH + getBusinessUnitName() + "/" + this.getUrlText();
		} else {
			return "javascript:;";
		}
	}

	public boolean isKeywordMatched() {
		return keywordMatched;
	}

	public void setKeywordMatched(boolean keywordMatched) {
		this.keywordMatched = keywordMatched;
	}

}
