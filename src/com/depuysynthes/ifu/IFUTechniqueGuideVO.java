package com.depuysynthes.ifu;

import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;

/****************************************************************************
 * <b>Title</b>: TechniqueGuideVO.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: Holds any information specific to the technique guides that
 * cannot be ascertained from the IFUDocumentInstanceVO and IFUContainer vos.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since March 10, 2015<p/>
 * <b>Changes: </b>
 ****************************************************************************/

public class IFUTechniqueGuideVO {
	
	private String tgId;
	private String tgName;
	private String urlText;
	private String dpySynMediaBinId;
	
	public IFUTechniqueGuideVO() {
			
	}

	public IFUTechniqueGuideVO(SMTServletRequest req) {
		this.setTgId(req.getParameter("tgId"));
		this.setTgName(req.getParameter("tgName"));
		this.setUrlText(req.getParameter("urlText"));
		this.setDpySynMediaBinId(req.getParameter("dpySynMediaBinId"));
	}
	
	public IFUTechniqueGuideVO(ResultSet rs) {
		DBUtil db = new DBUtil();
		this.setTgId(db.getStringVal("DEPUY_IFU_TG_ID", rs));
		this.setTgName(db.getStringVal("TG_NM", rs));
		this.setUrlText(db.getStringVal("tg_url", rs)); //synonym used in IFUDisplayAction
		this.setDpySynMediaBinId(db.getStringVal("tg_mediabin_id", rs)); //synonym used in IFUDisplayAction
		db = null;
	}

	public String getTgId() {
		return tgId;
	}

	public void setTgId(String tgId) {
		this.tgId = tgId;
	}

	public String getTgName() {
		return tgName;
	}

	public void setTgName(String tgName) {
		this.tgName = tgName;
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
	
	public String getPublicUrl() {
		if (this.dpySynMediaBinId != null) {
			return IFUFacadeAction.MEDIABIN_PATH + this.getDpySynMediaBinId();
		} else {
			return this.getUrlText();
		}
	}

}
