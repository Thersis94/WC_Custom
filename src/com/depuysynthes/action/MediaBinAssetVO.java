package com.depuysynthes.action;

// JDK 7
import java.sql.ResultSet;
import java.util.Date;

// SMTBaseLibs 2.0
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WebCrescendo 2.0
import com.smt.sitebuilder.action.SBModuleVO;

/****************************************************************************
 * <b>Title</b>: MediaBinAssetVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since May 7, 2013
 * Changes:
 * 2014-07-01: DBargerhuff: added downloadTypeTxt, languageCode fields.
 * 2014-07-10: DBargerhuff: added duration field.
 ****************************************************************************/
public class MediaBinAssetVO extends SBModuleVO {
	private static final long serialVersionUID = 1L;
	
	private String dpySynMediaBinId = null;
	private String assetNm = null;
	private String assetDesc = null;
	private String assetType = null;
	private String bodyRegionTxt = null;
	private String businessUnitNm = null;
	private Integer businessUnitId = Integer.valueOf(0);
	private String literatureTypeTxt = null;
	private String fileNm = null;
	private Date modifiedDt = null;
	private Integer fileSizeNo = Integer.valueOf(0);
	private Integer widthNo = Integer.valueOf(0);
	private Integer heightNo = Integer.valueOf(0);
	private Double duration = Double.valueOf(0);
	private String prodFamilyNm = null;
	private String prodNm = null;
	private String revisionLvlTxt = null;
	private String opCoNm = null;
	private String titleTxt = null;
	private String trackingNoTxt = null;
	private int importFileCd = 0;
	private String downloadTypeTxt = null;
	private String languageCode = null;
	private boolean isVideo = false;
	private String description = null;
	private String anatomy = null;
	private String metaKeywords = null;
	
	public MediaBinAssetVO(ResultSet rs) {
		DBUtil db = new DBUtil();
		dpySynMediaBinId = db.getStringVal("dpy_syn_mediabin_id", rs);
		assetNm = db.getStringVal("asset_nm", rs);
		assetDesc = db.getStringVal("asset_desc", rs);
		assetType = db.getStringVal("asset_type", rs);
		bodyRegionTxt = db.getStringVal("body_region_txt", rs);
		businessUnitNm = db.getStringVal("business_unit_nm", rs);
		businessUnitId = db.getIntegerVal("business_unit_id", rs);
		literatureTypeTxt = db.getStringVal("literature_type_txt", rs);
		fileNm = db.getStringVal("file_nm", rs);
		duration = db.getDoubleVal("duration_length_no", rs);
		modifiedDt = db.getDateVal("modified_dt", rs);
		fileSizeNo = db.getIntegerVal("orig_file_size_no", rs);
		prodFamilyNm = db.getStringVal("prod_family", rs);
		prodNm = db.getStringVal("prod_nm", rs);
		revisionLvlTxt = db.getStringVal("revision_lvl_txt", rs);
		opCoNm = db.getStringVal("opco_nm", rs);
		titleTxt = db.getStringVal("title_txt", rs);
		trackingNoTxt = StringUtil.checkVal(db.getStringVal("tracking_no_txt", rs));
		setImportFileCd(db.getIntVal("import_file_cd", rs));
		downloadTypeTxt = db.getStringVal("download_type_txt", rs);
		languageCode = db.getStringVal("language_cd", rs);
		setDescription(db.getStringVal("desc_txt", rs));
		setAnatomy(db.getStringVal("anatomy_txt", rs));
		setMetaKeywords(db.getStringVal("meta_keywords_txt", rs));
		
		String dims = db.getStringVal("dimensions_txt", rs);
		if (dims != null && dims.indexOf("~") > 0) {
			int delim = dims.indexOf("~");
			setWidthNo(Convert.formatInteger(dims.substring(0, delim)));
			setHeightNo(Convert.formatInteger(dims.substring(delim+1)));
		}
		
		// Determine if the asset is a video
		if (StringUtil.checkVal(assetType).toLowerCase().startsWith("multimedia")) {
			isVideo = true;
		}
	}
	
	
	public String getDpySynMediaBinId() {
		return dpySynMediaBinId;
	}
	public void setDpySynMediaBinId(String dpySynMediaBinId) {
		this.dpySynMediaBinId = dpySynMediaBinId;
	}
	public String getAssetNm() {
		return assetNm;
	}
	public void setAssetNm(String assetNm) {
		this.assetNm = assetNm;
	}
	public String getAssetDesc() {
		return assetDesc;
	}
	public void setAssetDesc(String assetDesc) {
		this.assetDesc = assetDesc;
	}
	public String getAssetType() {
		return assetType;
	}
	public void setAssetType(String assetType) {
		this.assetType = assetType;
	}
	public String getBodyRegionTxt() {
		return bodyRegionTxt;
	}
	public void setBodyRegionTxt(String bodyRegionTxt) {
		this.bodyRegionTxt = bodyRegionTxt;
	}
	public String getBusinessUnitNm() {
		return businessUnitNm;
	}
	public void setBusinessUnitNm(String businessUnitNm) {
		this.businessUnitNm = businessUnitNm;
	}
	public Integer getBusinessUnitId() {
		return businessUnitId;
	}
	public void setBusinessUnitId(Integer businessUnitId) {
		this.businessUnitId = businessUnitId;
	}
	public String getLiteratureTypeTxt() {
		return literatureTypeTxt;
	}
	public void setLiteratureTypeTxt(String literatureTypeTxt) {
		this.literatureTypeTxt = literatureTypeTxt;
	}
	public String getFileNm() {
		return fileNm;
	}
	public void setFileNm(String fileNm) {
		this.fileNm = fileNm;
	}
	public Date getModifiedDt() {
		return modifiedDt;
	}
	public void setModifiedDt(Date modifiedDt) {
		this.modifiedDt = modifiedDt;
	}
	public Integer getFileSizeNo() {
		return fileSizeNo;
	}
	public void setFileSizeNo(Integer fileSizeNo) {
		this.fileSizeNo = fileSizeNo;
	}
	public String getProdFamilyNm() {
		return prodFamilyNm;
	}
	public void setProdFamilyNm(String prodFamilyNm) {
		this.prodFamilyNm = prodFamilyNm;
	}
	public String getProdNm() {
		return prodNm;
	}
	public void setProdNm(String prodNm) {
		this.prodNm = prodNm;
	}
	public String getRevisionLvlTxt() {
		return revisionLvlTxt;
	}
	public void setRevisionLvlTxt(String revisionLvlTxt) {
		this.revisionLvlTxt = revisionLvlTxt;
	}
	public String getOpCoNm() {
		return opCoNm;
	}
	public void setOpCoNm(String opCoNm) {
		this.opCoNm = opCoNm;
	}
	public String getTitleTxt() {
		return titleTxt;
	}
	public void setTitleTxt(String titleTxt) {
		this.titleTxt = titleTxt;
	}
	public String getTrackingNoTxt() {
		return trackingNoTxt;
	}
	public void setTrackingNoTxt(String trackingNoTxt) {
		this.trackingNoTxt = trackingNoTxt;
	}


	public Integer getHeightNo() {
		return heightNo;
	}


	public void setHeightNo(Integer heightNo) {
		this.heightNo = heightNo;
	}


	public Integer getWidthNo() {
		return widthNo;
	}


	public void setWidthNo(Integer widthNo) {
		this.widthNo = widthNo;
	}


	/**
	 * @return the duration
	 */
	public Double getDuration() {
		return duration;
	}


	/**
	 * @param duration the duration to set
	 */
	public void setDuration(Double duration) {
		this.duration = duration;
	}


	public int getImportFileCd() {
		return importFileCd;
	}


	public void setImportFileCd(int importFileCd) {
		this.importFileCd = importFileCd;
	}


	/**
	 * @return the downloadTypeTxt
	 */
	public String getDownloadTypeTxt() {
		return downloadTypeTxt;
	}


	/**
	 * @param downloadTypeTxt the downloadTypeTxt to set
	 */
	public void setDownloadTypeTxt(String downloadTypeTxt) {
		this.downloadTypeTxt = downloadTypeTxt;
	}


	/**
	 * @return the languageCode
	 */
	public String getLanguageCode() {
		return languageCode;
	}


	/**
	 * @param languageCode the languageCode to set
	 */
	public void setLanguageCode(String languageCode) {
		this.languageCode = languageCode;
	}


	/**
	 * @return the isVideo
	 */
	public boolean isVideo() {
		return isVideo;
	}


	/**
	 * @param isVideo the isVideo to set
	 */
	public void setVideo(boolean isVideo) {
		this.isVideo = isVideo;
	}


	public String getDescription() {
		return description;
	}


	public void setDescription(String description) {
		this.description = description;
	}


	public String getAnatomy() {
		return anatomy;
	}


	public void setAnatomy(String anatomy) {
		this.anatomy = anatomy;
	}


	public String getMetaKeywords() {
		return metaKeywords;
	}


	public void setMetaKeywords(String keywords) {
		this.metaKeywords = keywords;
	}
	
}
