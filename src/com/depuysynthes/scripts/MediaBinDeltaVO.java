package com.depuysynthes.scripts;

import java.sql.ResultSet;

import com.depuysynthes.action.MediaBinAssetVO;
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
	
	//added for Showpad support; all the other fields are reuseable
	private String showpadId;
	
	
	public enum State {
		Insert,Update,Delete,Ignore,Failed;
	}

	public MediaBinDeltaVO() {
		super();
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
	
	
	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}
	
	@Override
	public int hashCode() {
		return super.hashCode();
	}
	
	@Override
	public String toString() {
		return super.toString();
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

	
}