package com.depuysynthes.action;

// JDK 7
import java.sql.ResultSet;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((anatomy == null) ? 0 : anatomy.hashCode());
		result = prime * result
				+ ((assetDesc == null) ? 0 : assetDesc.hashCode());
		result = prime * result
				+ ((assetNm == null) ? 0 : assetNm.hashCode());
		result = prime * result
				+ ((assetType == null) ? 0 : assetType.hashCode());
		result = prime * result
				+ ((bodyRegionTxt == null) ? 0 : bodyRegionTxt.hashCode());
		result = prime
				* result
				+ ((businessUnitId == null) ? 0 : businessUnitId.hashCode());
		result = prime
				* result
				+ ((businessUnitNm == null) ? 0 : businessUnitNm.hashCode());
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result
				+ ((dimensionsTxt == null) ? 0 : dimensionsTxt.hashCode());
		result = prime
				* result
				+ ((downloadTypeTxt == null) ? 0 : downloadTypeTxt
						.hashCode());
		result = prime
				* result
				+ ((dpySynMediaBinId == null) ? 0 : dpySynMediaBinId
						.hashCode());
		result = prime * result
				+ ((duration == null) ? 0 : duration.hashCode());
		result = prime * result + ((fileNm == null) ? 0 : fileNm.hashCode());
//		result = prime * result + ((fileSizeNo == null) ? 0 : fileSizeNo.hashCode());
		result = prime * result
				+ ((heightNo == null) ? 0 : heightNo.hashCode());
		result = prime * result + importFileCd;
		result = prime * result + (isVideo ? 1231 : 1237);
		result = prime * result
				+ ((languageCode == null) ? 0 : languageCode.hashCode());
		result = prime
				* result
				+ ((literatureTypeTxt == null) ? 0 : literatureTypeTxt
						.hashCode());
		result = prime * result
				+ ((metaKeywords == null) ? 0 : metaKeywords.hashCode());
		result = prime * result
				+ ((modifiedDt == null) ? 0 : modifiedDt.hashCode());
		result = prime * result + ((opCoNm == null) ? 0 : opCoNm.hashCode());
		result = prime * result
				+ ((prodFamilyNm == null) ? 0 : prodFamilyNm.hashCode());
		result = prime * result + ((prodNm == null) ? 0 : prodNm.hashCode());
		result = prime
				* result
				+ ((revisionLvlTxt == null) ? 0 : revisionLvlTxt.hashCode());
		result = prime * result
				+ ((titleTxt == null) ? 0 : titleTxt.hashCode());
		result = prime * result
				+ ((trackingNoTxt == null) ? 0 : trackingNoTxt.hashCode());
		result = prime * result
				+ ((videoChapters == null) ? 0 : videoChapters.hashCode());
		result = prime * result
				+ ((widthNo == null) ? 0 : widthNo.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		MediaBinAssetVO other = (MediaBinAssetVO) obj;
		if (anatomy == null) {
			if (other.anatomy != null) {
//				System.err.println("anatomy1 is different");
				return false;
			}
		} else if (!anatomy.equals(other.anatomy)) {
//			System.err.println("anatomy2 is different");
			return false;
		}
		if (assetDesc == null) {
			if (other.assetDesc != null) {
//				System.err.println("assetDesc1 is different");
				return false;
			}
		} else if (!assetDesc.equals(other.assetDesc)) {
//			System.err.println("assetDesc2 is different");
			return false;
		}
		if (assetNm == null) {
			if (other.assetNm != null) {
//				System.err.println("assetNm1 is different");
				return false;
			}
		} else if (!assetNm.equals(other.assetNm)) {
//			System.err.println("assetNm2 is different");
			return false;
		}
		if (assetType == null) {
			if (other.assetType != null) {
//				System.err.println("assetType1 is different");
				return false;
			}
		} else if (!assetType.equals(other.assetType)) {
//			System.err.println("assetType2 is different");
			return false;
		}
		if (bodyRegionTxt == null) {
			if (other.bodyRegionTxt != null) {
//				System.err.println("bodyRegion1 is different");
				return false;
			}
		} else if (!bodyRegionTxt.equals(other.bodyRegionTxt)) {
//			System.err.println("bodyRegion2 is different");
			return false;
		}
		if (businessUnitId == null) {
			if (other.businessUnitId != null) {
//				System.err.println("businessunit1 is different");
				return false;
			}
		} else if (!businessUnitId.equals(other.businessUnitId)) {
//			System.err.println("businessunit2 is different");
			return false;
		}
		if (businessUnitNm == null) {
			if (other.businessUnitNm != null) {
//				System.err.println("businessunitnm1 is different");
				return false;
			}
		} else if (!businessUnitNm.equals(other.businessUnitNm)) {
//			System.err.println("businessunitnm2 is different");
			return false;
		}
		if (description == null) {
			if (other.description != null) {
//				System.err.println("desc is different");
				return false;
			}
		} else if (!description.equals(other.description)) {
//			System.err.println("desc2 is different");
			return false;
		}
		if (dimensionsTxt == null) {
			if (other.dimensionsTxt != null) {
//				System.err.println("dims is different");
				return false;
			}
		} else if (!dimensionsTxt.equals(other.dimensionsTxt)) {
//			System.err.println("dims2 is different");
			return false;
		}
		if (downloadTypeTxt == null) {
			if (other.downloadTypeTxt != null) {
//				System.err.println("downloadTypeTxt is different");
				return false;
			}
		} else if (!downloadTypeTxt.equals(other.downloadTypeTxt)) {
//			System.err.println("downloadTypeTxt2 is different");
			return false;
		}
		if (dpySynMediaBinId == null) {
			if (other.dpySynMediaBinId != null) {
//				System.err.println("dpySynMediaBinId is different");
				return false;
			}
		} else if (!dpySynMediaBinId.equals(other.dpySynMediaBinId)) {
//			System.err.println("dpySynMediaBinId2 is different");
			return false;
		}
		if (duration == null) {
			if (other.duration != null) {
//				System.err.println("duration is different");
				return false;
			}
		} else if (!duration.equals(other.duration)) {
//			System.err.println("duration2 is different");
			return false;
		}
		if (fileNm == null) {
			if (other.fileNm != null) {
//				System.err.println("fileNm is different");
				return false;
			}
		} else if (!fileNm.equals(other.fileNm)) {
//			System.err.println("fileNm2 is different");
			return false;
		}
//		if (fileSizeNo == null) {
//			if (other.fileSizeNo != null) {
//				return false;
//			}
//		} else if (!fileSizeNo.equals(other.fileSizeNo)) {
//			return false;
//		}
//		if (heightNo == null) {
//			if (other.heightNo != null) {
//				System.err.println("heightNo is different");
//				return false;
//			}
//		} else if (!heightNo.equals(other.heightNo)) {
//			System.err.println("heightNo2 is different");
//			return false;
//		}
		if (importFileCd != other.importFileCd) {
//			System.err.println("importFileCd is different");
			return false;
		}
//		if (isVideo != other.isVideo) {
//			System.err.println("isVideo is different");
//			return false;
//		}
		if (languageCode == null) {
			if (other.languageCode != null) {
//				System.err.println("languageCode is different");
				return false;
			}
		} else if (!languageCode.equals(other.languageCode)) {
//			System.err.println("languageCode2 is different");
			return false;
		}
		if (literatureTypeTxt == null) {
			if (other.literatureTypeTxt != null) {
//				System.err.println("literatureTypeTxt is different");
				return false;
			}
		} else if (!literatureTypeTxt.equals(other.literatureTypeTxt)) {
//			System.err.println("literatureTypeTxt2 is different");
			return false;
		}
		if (metaKeywords == null) {
			if (other.metaKeywords != null) {
//				System.err.println("metaKeywords is different");
				return false;
			}
		} else if (!metaKeywords.equals(other.metaKeywords)) {
//			System.err.println(metaKeywords + "|||" + other.metaKeywords);
			return false;
		}
//		if (modifiedDt == null) {
//			if (other.modifiedDt != null) {
//				System.err.println("modifiedDt is different");
//				return false;
//			}
//		} else if (!modifiedDt.equals(other.modifiedDt)) {
//			System.err.println("modifiedDt2 is different");
//			return false;
//		}
		if (opCoNm == null) {
			if (other.opCoNm != null) {
//				System.err.println("opCoNm is different");
				return false;
			}
		} else if (!opCoNm.equals(other.opCoNm)) {
//			System.err.println("opCoNm2 is different");
			return false;
		}
		if (prodFamilyNm == null) {
			if (other.prodFamilyNm != null) {
//				System.err.println("prodFamilyNm is different");
				return false;
			}
		} else if (!prodFamilyNm.equals(other.prodFamilyNm)) {
//			System.err.println("prodFamilyNm2 is different");
			return false;
		}
		if (prodNm == null) {
			if (other.prodNm != null) {
//				System.err.println("prodNm is different");
				return false;
			}
		} else if (!prodNm.equals(other.prodNm)) {
//			System.err.println("prodNm2 is different");
			return false;
		}
		if (revisionLvlTxt == null) {
			if (other.revisionLvlTxt != null) {
//				System.err.println("revisionLvlTxt is different");
				return false;
			}
		} else if (!revisionLvlTxt.equals(other.revisionLvlTxt)) {
//			System.err.println("revisionLvlTxt2 is different");
			return false;
		}
		if (titleTxt == null) {
			if (other.titleTxt != null) {
//				System.err.println("titleTxt is different");
				return false;
			}
		} else if (!titleTxt.equals(other.titleTxt)) {
//			System.err.println("titleTxt2 is different");
			return false;
		}
		if (trackingNoTxt == null) {
			if (other.trackingNoTxt != null) {
//				System.err.println("trackingNoTxt is different");
				return false;
			}
		} else if (!trackingNoTxt.equals(other.trackingNoTxt)) {
//			System.err.println("trackingNoTxt2 is different");
			return false;
		}
		if (videoChapters == null) {
			if (other.videoChapters != null) {
//				System.err.println("videoChapters is different");
				return false;
			}
		} else if (!videoChapters.equals(other.videoChapters)) {
//			System.err.println("videoChapters2 is different");
			return false;
		}
//		if (widthNo == null) {
//			if (other.widthNo != null) {
//				System.err.println("widthNo is different");
//				return false;
//			}
//		} else if (!widthNo.equals(other.widthNo)) {
//			System.err.println("widthNo2 is different");
//			return false;
//		}
		return true;
	}
	
	@Override
	public String toString() {
		return StringUtil.getToString(this, true, 1, "|");
	}
}
