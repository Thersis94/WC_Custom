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
	private String tgNM;
	private String urlTxt;
	private String dpySynMediaBinId;
	
	public IFUTechniqueGuideVO() {
			
	}

	public IFUTechniqueGuideVO(SMTServletRequest req) {
		this.setTgId(req.getParameter("tgId"));
		this.setTgNM(req.getParameter("tgNM"));
		this.setUrlTxt(req.getParameter("urlTxt"));
		this.setDpySynMediaBinId(req.getParameter("dpySynMediaBinId"));
	}
	
	public IFUTechniqueGuideVO(ResultSet rs) {
		DBUtil db = new DBUtil();
		this.setTgId(db.getStringVal("DEPUY_IFU_TG_ID", rs));
		this.setTgNM(db.getStringVal("TG_NM", rs));
		this.setUrlTxt(db.getStringVal("URL_TXT", rs));
		this.setDpySynMediaBinId(db.getStringVal("DPY_SYN_MEDIABIN_ID", rs));
		db = null;
	}

	public String getTgId() {
		return tgId;
	}

	public void setTgId(String tgId) {
		this.tgId = tgId;
	}

	public String getTgNM() {
		return tgNM;
	}

	public void setTgNM(String tgNM) {
		this.tgNM = tgNM;
	}

	public String getUrlTxt() {
		return urlTxt;
	}

	public void setUrlTxt(String urlTxt) {
		this.urlTxt = urlTxt;
	}

	public String getDpySynMediaBinId() {
		return dpySynMediaBinId;
	}

	public void setDpySynMediaBinId(String dpySynMediaBinId) {
		this.dpySynMediaBinId = dpySynMediaBinId;
	}

}
