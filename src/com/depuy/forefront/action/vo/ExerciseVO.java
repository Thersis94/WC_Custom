package com.depuy.forefront.action.vo;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.depuy.forefront.action.ProgramAction;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;

public class ExerciseVO implements Serializable {

	private static final long serialVersionUID = 1984324685121L;
	private String exerciseId = null;
	private String programId = null;
	private String hospitalId = null;
	private String hospitalName = null;
	private String exerciseName = null;
	private String thumbnailImage = null;
	private String detailImage = null;
	private String video = null;
	private String shortDescText = null;
	private String detailedDescText = null;
	private String postSurgDescText = null;
	private List<ExerciseIntensityVO> intensityLevels = new ArrayList<ExerciseIntensityVO>();
	
	public ExerciseVO() {
	}
	
	public ExerciseVO(ResultSet rs) {
		DBUtil db = new DBUtil();
		exerciseId = db.getStringVal("exercise_id", rs);
		programId = db.getStringVal("program_id", rs);
		hospitalId = db.getStringVal("hospital_id", rs);
		hospitalName = db.getStringVal("hospital_nm", rs);
	
		exerciseName = db.getStringVal("exercise_nm", rs);
		thumbnailImage = db.getStringVal("thumbnail_img", rs);
		detailImage = db.getStringVal("detail_img", rs);
		shortDescText = db.getStringVal("short_desc_txt", rs);
		detailedDescText = db.getStringVal("detailed_desc_txt", rs);
		postSurgDescText = db.getStringVal("postsurg_desc_txt", rs);
		video = db.getStringVal("video_url", rs);
		db = null;
	}
	
	public ExerciseVO(ActionRequest req){
		exerciseId = req.getParameter("exerciseId");
		programId = (String) req.getSession().getAttribute(ProgramAction.PROGRAM_ID);
		hospitalId = StringUtil.checkVal(req.getParameter("hospitalId"), null);
		
		exerciseName = req.getParameter("exerciseName");
		thumbnailImage = req.getParameter("thumbnailImage");
		detailImage = req.getParameter("detailImage");
		shortDescText = req.getParameter("shortDescText");
		detailedDescText = req.getParameter("detailedDescText");
		postSurgDescText = req.getParameter("postsurgDescText");
		video = req.getParameter("video");	
	}

	/**
	 * @return the exerciseId
	 */
	public String getExerciseId() {
		return exerciseId;
	}

	/**
	 * @param exerciseId the exerciseId to set
	 */
	public void setExerciseId(String exerciseId) {
		this.exerciseId = exerciseId;
	}

	/**
	 * @return the programId
	 */
	public String getProgramId() {
		return programId;
	}

	/**
	 * @param programId the programId to set
	 */
	public void setProgramId(String programId) {
		this.programId = programId;
	}

	/**
	 * @return the hospitalId
	 */
	public String getHospitalId() {
		return hospitalId;
	}

	/**
	 * @param hospitalId the hospitalId to set
	 */
	public void setHospitalId(String hospitalId) {
		this.hospitalId = hospitalId;
	}
	
	/**
	 * @return the exerciseName
	 */
	public String getExerciseName() {
		return exerciseName;
	}

	/**
	 * @param exerciseName the exerciseName to set
	 */
	public void setExerciseName(String exerciseName) {
		this.exerciseName = exerciseName;
	}

	/**
	 * @return the thumbnailImage
	 */
	public String getThumbnailImage() {
		return thumbnailImage;
	}

	/**
	 * @param thumbnailImage the thumbnailImage to set
	 */
	public void setThumbnailImage(String thumbnailImage) {
		this.thumbnailImage = thumbnailImage;
	}

	/**
	 * @return the detailImage
	 */
	public String getDetailImage() {
		return detailImage;
	}

	/**
	 * @param detailImage the detailImage to set
	 */
	public void setDetailImage(String detailImage) {
		this.detailImage = detailImage;
	}

	/**
	 * @return the shortDescText
	 */
	public String getShortDescText() {
		return shortDescText;
	}

	/**
	 * @param shortDescText the shortDescText to set
	 */
	public void setShortDescText(String shortDescText) {
		this.shortDescText = shortDescText;
	}

	/**
	 * @return the detailedDescText
	 */
	public String getDetailedDescText() {
		return detailedDescText;
	}

	/**
	 * @param detailedDescText the detailedDescText to set
	 */
	public void setDetailedDescText(String detailedDescText) {
		this.detailedDescText = detailedDescText;
	}

	/**
	 * @return the intensityLevels
	 */
	public List<ExerciseIntensityVO> getIntensityLevels() {
		return intensityLevels;
	}

	/**
	 * @param listAttributes the listAttributes to set
	 */
	public void setIntensityLevels(List<ExerciseIntensityVO> intensityLevels) {
		this.intensityLevels = intensityLevels;
	}
	
	/**
	 * 
	 * @param vo the vo to add
	 */
	public void addIntensityLevel(ExerciseIntensityVO vo){
		if(vo != null && StringUtil.checkVal(vo.getExerciseIntensityId()).length() > 0 && !intensityLevels.contains(vo))
			intensityLevels.add(vo);
	}
	
	/**
	 * 
	 * @param vo the vo to remove
	 */
	public void removeIntensityLevel(ExerciseIntensityVO vo){
		if(vo != null && intensityLevels.contains(vo))
			intensityLevels.remove(vo);
	}

	/**
	 * @return the video
	 */
	public String getVideo() {
		return video;
	}

	/**
	 * @param video the video to set
	 */
	public void setVideo(String video) {
		this.video = video;
	}

	public String getPostSurgDescText() {
		return postSurgDescText;
	}

	public void setPostSurgDescText(String postSurgDescText) {
		this.postSurgDescText = postSurgDescText;
	}

	public String getHospitalName() {
		return hospitalName;
	}

	public void setHospitalName(String hospitalName) {
		this.hospitalName = hospitalName;
	}
	
	
}
