package com.depuysynthes.scripts;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.depuysynthes.action.MediaBinAssetVO;
import com.depuysynthes.scripts.showpad.ShowpadTagVO;
import com.siliconmtn.db.DBUtil;

/****************************************************************************
 * <b>Title</b>: MediaBinDeltaVO.java<p/>
 * <b>Description: extends a stock mediabin record with some additional fields that 
 * help us monitor state (insert|update|delete), error-reason, and ingest-time parameters
 * that we don't need to exist within WC or the database.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Aug 26, 2015
 ****************************************************************************/
public class MediaBinDeltaVO extends MediaBinAssetVO {
	private static final long serialVersionUID = 1134677899809090L;
	
	private State recordState;
	private String errorReason;
	private String eCopyTrackingNo;
	private String limeLightUrl;
	private String fileName;
	
	/*
	 * used by ShowpadProductDecorator to pass-along the update date of the product to affiliated assets.
	 */
	private Date productUpdateDt;
	
	private List<ShowpadTagVO> tags;
	
	//added for Showpad support; all the other fields are reuseable
	private String showpadId;
	
	
	public enum State {
		Insert,Update,Delete,Ignore,Failed;
	}

	public MediaBinDeltaVO() {
		super();
		tags = new ArrayList<>();
	}
	
	public MediaBinDeltaVO(ResultSet rs) {
		super(rs);
		DBUtil db = new DBUtil();
		setShowpadId(db.getStringVal("DPY_SYN_SHOWPAD_ID", rs));
		db = null;
	}

	public State getRecordState() {
		return recordState;
	}

	public void setRecordState(State recordState) {
		this.recordState = recordState;
	}
	
	public boolean isUsable() {
		return (State.Failed != recordState && State.Delete != recordState);
	}

	public String getErrorReason() {
		return errorReason;
	}

	public void setErrorReason(String errorReason) {
		this.errorReason = errorReason;
	}

	public String getEcopyTrackingNo() {
		return eCopyTrackingNo;
	}

	public void setEcopyTrackingNo(String eCopyTrackingNo) {
		this.eCopyTrackingNo = eCopyTrackingNo;
	}

	public String getLimeLightUrl() {
		return limeLightUrl;
	}

	public void setLimeLightUrl(String limeLightUrl) {
		this.limeLightUrl = limeLightUrl;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getShowpadId() {
		return showpadId;
	}

	public void setShowpadId(String showpadId) {
		this.showpadId = showpadId;
	}

	public List<ShowpadTagVO> getTags() {
		return tags;
	}

	public void setTags(List<ShowpadTagVO> tags) {
		this.tags = tags;
	}
	
	public void addTag(ShowpadTagVO tag) {
		tags.add(tag);
	}
	
	public void addTag(String id, String name, String division, String externalId) {
		tags.add(new ShowpadTagVO(id, name, division, externalId));
	}

	public Date getProductUpdateDt() {
		return productUpdateDt;
	}

	public void setProductUpdateDt(Date productUpdateDt) {
		this.productUpdateDt = productUpdateDt;
	}
}