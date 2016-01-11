package com.depuysynthes.action;

// JDK 7
import java.beans.PropertyChangeEvent;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
	
	private static final List<String> videoTypes = Arrays.asList(MediaBinAdminAction.VIDEO_ASSETS);
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
	private String dimensionsTxt = null;
	private Double duration = Double.valueOf(0);
	private String prodFamilyNm = null;
	private String prodNm = null;
	private String revisionLvlTxt = null;
	private String eCopyRevisionLvl = null;
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
	private String videoChapters = null;
	private List<PropertyChangeEvent> deltas;
	
	public MediaBinAssetVO() {
	}
	
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
		setMetaKeywords(db.getStringVal("meta_kywds_txt", rs));
		setVideoChapters(db.getStringVal("META_CONTENT_TXT", rs));
		seteCopyRevisionLvl(db.getStringVal("ecopy_revision_lvl_txt", rs));
		
		String dims = db.getStringVal("dimensions_txt", rs);
		if (dims != null && dims.indexOf("~") > 0) {
			int delim = dims.indexOf("~");
			setWidthNo(Convert.formatInteger(dims.substring(0, delim)));
			setHeightNo(Convert.formatInteger(dims.substring(delim+1)));
		}
		this.setDimensionsTxt(dims);
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
		
		//the decision of whether or not this asset is a video is based on assetType,
		//so when we set assetType also set isVideo.  Based on the video types predefined by DS/SMT.
		if (assetType != null)
			isVideo = videoTypes.contains(assetType.toLowerCase());

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


	public String getVideoChapters() {
		return videoChapters;
	}


	public void setVideoChapters(String videoChapters) {
		this.videoChapters = videoChapters;
	}

	public String getDimensionsTxt() {
		return dimensionsTxt;
	}

	public void setDimensionsTxt(String dimensionsTxt) {
		this.dimensionsTxt = dimensionsTxt;
	}

	/**
	 * a rather complex "equals" method, but highly effective.
	 * This method is called specifically from the DSMediaBinImporterV2 class,
	 * which uses it to compare two versions of the same MB Asset for changes.
	 * It must look at every field in order to accurately determine if the records are identical.
	 */
	public boolean lexicographyEquals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;

		MediaBinAssetVO other = (MediaBinAssetVO) obj;
		if (!compareStr(anatomy, other.anatomy))
			this.addDelta(new PropertyChangeEvent(this,"anatomy",other.anatomy, anatomy));
		
		if (!compareStr(assetDesc, other.assetDesc))
			this.addDelta(new PropertyChangeEvent(this,"assetDesc",other.assetDesc, assetDesc));

		if (!compareStr(assetNm, other.assetNm))
			this.addDelta(new PropertyChangeEvent(this,"assetNm",other.assetNm, assetNm));

		if (!compareStr(assetType, other.assetType))
			this.addDelta(new PropertyChangeEvent(this,"assetType",other.assetType, assetType));

		if (!compareStr(bodyRegionTxt, other.bodyRegionTxt))
			this.addDelta(new PropertyChangeEvent(this,"bodyRegionTxt",other.bodyRegionTxt, bodyRegionTxt));

		if (!compareInt(businessUnitId, other.businessUnitId))
			this.addDelta(new PropertyChangeEvent(this,"businessUnitId",other.businessUnitId, businessUnitId));

		if (!compareStr(businessUnitNm, other.businessUnitNm))
			this.addDelta(new PropertyChangeEvent(this,"businessUnitNm",other.businessUnitNm, businessUnitNm));

		if (!compareStr(description, other.description))
			this.addDelta(new PropertyChangeEvent(this,"description",other.description, description));

		if (!compareStr(dimensionsTxt, other.dimensionsTxt))
			this.addDelta(new PropertyChangeEvent(this,"dimensionsTxt",other.dimensionsTxt, dimensionsTxt));

		if (!compareStr(downloadTypeTxt, other.downloadTypeTxt))
			this.addDelta(new PropertyChangeEvent(this,"downloadTypeTxt",other.downloadTypeTxt, downloadTypeTxt));

		if (!compareDbl(duration, other.duration))
			this.addDelta(new PropertyChangeEvent(this,"duration",Convert.formatDouble(other.duration), Convert.formatDouble(duration)));

		if (!compareStr(fileNm, other.fileNm))
			this.addDelta(new PropertyChangeEvent(this,"fileNm",other.fileNm, fileNm));

		if (!compareStr(languageCode, other.languageCode))
			this.addDelta(new PropertyChangeEvent(this,"languageCode",other.languageCode, languageCode));

		if (!compareStr(literatureTypeTxt, other.literatureTypeTxt))
			this.addDelta(new PropertyChangeEvent(this,"literatureTypeTxt",other.literatureTypeTxt, literatureTypeTxt));

		if (!compareStr(metaKeywords, other.metaKeywords))
			this.addDelta(new PropertyChangeEvent(this,"metaKeywords",other.metaKeywords, metaKeywords));

		if (!compareStr(opCoNm, other.opCoNm))
			this.addDelta(new PropertyChangeEvent(this,"opCoNm",other.opCoNm, opCoNm));

		if (!compareStr(prodFamilyNm, other.prodFamilyNm))
			this.addDelta(new PropertyChangeEvent(this,"prodFamilyNm",other.prodFamilyNm, prodFamilyNm));

		if (!compareStr(prodNm, other.prodNm))
			this.addDelta(new PropertyChangeEvent(this,"prodNm",other.prodNm, prodNm));

		if (!compareStr(titleTxt, other.titleTxt))
			this.addDelta(new PropertyChangeEvent(this,"titleTxt",other.titleTxt, titleTxt));

		if (!compareStr(trackingNoTxt, other.trackingNoTxt))
			this.addDelta(new PropertyChangeEvent(this,"trackingNoTxt",other.trackingNoTxt, trackingNoTxt));

		if (!compareStr(videoChapters, other.videoChapters))
			this.addDelta(new PropertyChangeEvent(this,"videoChapters",other.videoChapters, videoChapters));
		
		return getDeltas() == null;
	}
	
	/**
	 * helper to .equals method above; reusable String equality test to reduce code overhead.
	 * @param a
	 * @param b
	 * @return
	 */
	private boolean compareStr(String a, String b) {
		if (a == null && b == null) return true;
		if (a == null && b != null) return false;
		if (a != null && b == null) return false;
		return a.equals(b);
	}
	
	/**
	 * helper to .equals method above; reusable Integer equality test to reduce code overhead.
	 * @param a
	 * @param b
	 * @return
	 */
	private boolean compareInt(Integer a, Integer b) {
		if (a == null && b == null) return true;
		if (a == null && b != null) return false;
		if (a != null && b == null) return false;
		return a.equals(b);
	}
	
	/**
	 * helper to .equals method above; reusable Double equality test to reduce code overhead.
	 * @param a
	 * @param b
	 * @return
	 */
	private boolean compareDbl(Double a, Double b) {
		if (a == null && b == null) return true;
		if (a == null && b != null) return false;
		if (a != null && b == null) return false;
		return a.equals(b);
	}
	
	@Override
	public String toString() {
		return StringUtil.getToString(this, false, 1, "|");
	}
	

	/**
	 * the 3 methods below are for tracking changes across asset versioning.  
	 * We capture these from the .equals() method, and are able to report this information
	 * to an administrator
	 * @param evt
	 */

	public void addDelta(PropertyChangeEvent evt) {
		if (deltas == null) deltas = new ArrayList<>();
		deltas.add(evt);
	}

	public List<PropertyChangeEvent> getDeltas() {
		return deltas;
	}

	public void setDeltas(List<PropertyChangeEvent> deltas) {
		this.deltas = deltas;
	}

	public String geteCopyRevisionLvl() {
		return eCopyRevisionLvl;
	}

	public void seteCopyRevisionLvl(String lvl) {
		//do some data cleanup; a zero synonymizes null here
		if (lvl == null || lvl.length() == 0 || "0".equals(lvl)) lvl = null;
		this.eCopyRevisionLvl = lvl;
	}
}
