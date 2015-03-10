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

public class TechniqueGuideVO {
	
	private String guideId;
	private String guideNm;
	private String guideUrl;
	private String mediaBinId;
	
	public TechniqueGuideVO() {
			
	}

	public TechniqueGuideVO(SMTServletRequest req) {
		this.setGuideId(req.getParameter("guideId"));
		this.setGuideNm(req.getParameter("guideNm"));
		this.setGuideUrl(req.getParameter("guideUrl"));
		this.setMediaBinId(req.getParameter("mediaBinId"));
	}
	
	public TechniqueGuideVO(ResultSet rs) {
		DBUtil db = new DBUtil();
		this.setGuideId(db.getStringVal("DEPUY_IFU_TG_ID", rs));
		this.setGuideNm(db.getStringVal("TG_NM", rs));
		this.setGuideUrl(db.getStringVal("URL_TXT", rs));
		this.setMediaBinId(db.getStringVal("DPY_SYN_MEDIABIN_ID", rs));
		db = null;
	}

	public String getGuideId() {
		return guideId;
	}

	public void setGuideId(String guideId) {
		this.guideId = guideId;
	}

	public String getGuideNm() {
		return guideNm;
	}

	public void setGuideNm(String guideNm) {
		this.guideNm = guideNm;
	}

	public String getGuideUrl() {
		return guideUrl;
	}

	public void setGuideUrl(String guideUrl) {
		this.guideUrl = guideUrl;
	}

	public String getMediaBinId() {
		return mediaBinId;
	}

	public void setMediaBinId(String mediaBinId) {
		this.mediaBinId = mediaBinId;
	}

}
