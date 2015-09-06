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
	private String checksum;
	private String eCopyTrackingNo;
	private String limeLightUrl;
	
	
	public enum State {
		Insert,Update,Delete,Ignore,Failed,ChecksumIssue;
	}

	public MediaBinDeltaVO() {
		super();
	}
	
	public MediaBinDeltaVO(ResultSet rs) {
		super(rs);
		DBUtil db = new DBUtil();
		setChecksum(db.getStringVal("file_checksum_txt", rs));
		db = null;
	}

	public State getRecordState() {
		return recordState;
	}

	public void setRecordState(State recordState) {
		this.recordState = recordState;
	}

	public String getErrorReason() {
		return errorReason;
	}

	public void setErrorReason(String errorReason) {
		this.errorReason = errorReason;
	}

	public String getChecksum() {
		return checksum;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
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
	
}
