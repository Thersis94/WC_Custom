package com.depuysynthes.ifu;

import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;

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
	private String dpySynAssetName;
	private String implId;
	private int orderNo;
	
	public IFUTechniqueGuideVO() {
			
	}

	public IFUTechniqueGuideVO(SMTServletRequest req) {
		this.setTgId(req.getParameter("tgId"));
		this.setTgName(req.getParameter("tgName"));
		this.setUrlText(req.getParameter("urlText"));
		this.setDpySynMediaBinId(req.getParameter("dpySynMediaBinId"));
		this.setImplId(req.getParameter("implId"));
		this.setOrderNo(Convert.formatInteger(req.getParameter("orderNo")));
	}
	
	public IFUTechniqueGuideVO(ResultSet rs) {
		DBUtil db = new DBUtil();
		this.setTgId(db.getStringVal("DEPUY_IFU_TG_ID", rs));
		this.setTgName(db.getStringVal("TG_NM", rs));
		this.setUrlText(db.getStringVal("tg_url", rs)); //synonym used in IFUDisplayAction
		this.setDpySynMediaBinId(db.getStringVal("tg_mediabin_id", rs)); //synonym used in IFUDisplayAction
		this.setDpySynAssetName(db.getStringVal("TITLE_TXT", rs));
		this.setOrderNo(db.getIntVal("ORDER_NO", rs));
		this.setImplId(db.getStringVal("DEPUY_IFU_IMPL_ID", rs));
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

	public String getDpySynAssetName() {
		return dpySynAssetName;
	}

	public void setDpySynAssetName(String dpySynAssetName) {
		this.dpySynAssetName = dpySynAssetName;
	}
	
	public String getPublicUrl() {
		if (this.dpySynMediaBinId != null) {
			return IFUFacadeAction.MEDIABIN_PATH + this.getDpySynMediaBinId();
		} else {
			return this.getUrlText();
		}
	}

	public String getImplId() {
		return implId;
	}

	public void setImplId(String implId) {
		this.implId = implId;
	}

	public int getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(int orderNo) {
		this.orderNo = orderNo;
	}

}
