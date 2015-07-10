package com.depuysynthesinst.assg;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: AssignmentVO.java<p/>
 * <b>Description: VO for an Assignment; either from the Professor's or Student's standpoint.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 7, 2015
 ****************************************************************************/
public class AssignmentVO implements Serializable {

	private static final long serialVersionUID = -2305686074893760531L;

	private String assgId;
	private int resDirId;
	private String parentId;
	private String directorProfileId;
	private UserDataVO directorProfile;
	private List<ResidentVO> residents;
	private String assgName;
	private String assgDesc;
	private Date dueDt;
	private boolean sequentialFlg; //if the assets should be completed in a certain sequence
	private boolean skipAheadFlg; //if the user has acknowledged that they're out of sequence.
	private int orderNo;
	private boolean activeFlg;
	private Date createDt;
	private Date updateDt;
	private List<AssignmentAssetVO> assets;
	
	
	public AssignmentVO() {
		assets = new ArrayList<>();
		residents = new ArrayList<>();
	}
	
	public AssignmentVO(ResultSet rs) {
		this();
		DBUtil util = new DBUtil();
		assgId = util.getStringVal("assg_id", rs);
		parentId = util.getStringVal("parent_id", rs);
		assgName = util.getStringVal("assg_nm", rs);
		assgDesc = util.getStringVal("desc_txt", rs);
		directorProfileId = util.getStringVal("res_dir_profile_id", rs);
		dueDt = util.getDateVal("due_dt", rs);
		updateDt = util.getDateVal("update_dt", rs);
		setSequentialFlg(util.getIntegerVal("sequential_flg", rs));
		setSkipAheadFlg(util.getIntegerVal("skip_ahead_flg", rs));
		setActiveFlg(util.getIntegerVal("active_flg", rs));
		util = null;
	}
	
	public AssignmentVO(SMTServletRequest req) {
		this();
		assgId = StringUtil.checkVal(req.getParameter("assgId"), null);
		parentId = StringUtil.checkVal(req.getParameter("parentId"), null);
		resDirId = Convert.formatInteger("" + req.getSession().getAttribute(AssignmentsFacadeAction.RES_DIR_ID));
		assgName = StringUtil.checkVal(req.getParameter("assgName"), null);
		assgDesc = StringUtil.checkVal(req.getParameter("assgDesc"), null);
		dueDt = Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("dueDt"));
		setSequentialFlg(Convert.formatBoolean(req.getParameter("sequentialFlg")));
		setActiveFlg(Convert.formatBoolean(req.getParameter("activeFlg")));
	}


	public String getDirectorProfileId() {
		return directorProfileId;
	}


	public void setDirectorProfileId(String directorProfileId) {
		this.directorProfileId = directorProfileId;
	}


	public UserDataVO getDirectorProfile() {
		return directorProfile;
	}


	public void setDirectorProfile(UserDataVO directorProfile) {
		this.directorProfile = directorProfile;
	}



	public String getAssgName() {
		return assgName;
	}


	public void setAssgName(String assgName) {
		this.assgName = assgName;
	}


	public String getAssgDesc() {
		return assgDesc;
	}


	public void setAssgDesc(String assgDesc) {
		this.assgDesc = assgDesc;
	}


	public Date getDueDt() {
		return dueDt;
	}
	public String getDueDtStr() {
		return Convert.formatDate(dueDt, Convert.DATE_SLASH_PATTERN);
	}


	public void setDueDt(Date dueDate) {
		this.dueDt = dueDate;
	}


	public boolean isSequentialFlg() {
		return sequentialFlg;
	}


	public void setSequentialFlg(Integer sequentialFlg) {
		setSequentialFlg(Convert.formatBoolean(sequentialFlg));
	}
	public void setSequentialFlg(boolean sequentialFlg) {
		this.sequentialFlg = sequentialFlg;
	}


	public boolean isSkipAheadFlg() {
		return skipAheadFlg;
	}

	public void setSkipAheadFlg(Integer skipAheadFlg) {
		setSkipAheadFlg(Convert.formatBoolean(skipAheadFlg));
	}
	public void setSkipAheadFlg(boolean skipAheadFlg) {
		this.skipAheadFlg = skipAheadFlg;
	}


	public int getOrderNo() {
		return orderNo;
	}


	public void setOrderNo(int orderNo) {
		this.orderNo = orderNo;
	}


	public boolean isActiveFlg() {
		return activeFlg;
	}


	public void setActiveFlg(boolean activeFlg) {
		this.activeFlg = activeFlg;
	}
	public void setActiveFlg(int activeFlg) {
		setActiveFlg(Convert.formatBoolean(activeFlg));
	}


	public Date getCreateDt() {
		return createDt;
	}


	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}


	public Date getUpdateDt() {
		return updateDt;
	}


	public void setUpdateDt(Date updateDt) {
		this.updateDt = updateDt;
	}


	public List<AssignmentAssetVO> getAssets() {
		return assets;
	}


	public void setAssets(List<AssignmentAssetVO> assets) {
		this.assets = assets;
	}
	
	public void addAsset(AssignmentAssetVO asset) {
		if (asset == null || asset.getAssgAssetId() == null) return;
		this.assets.add(asset);
	}
	
	
	/**
	 * loop the asset types (via SolrDocument) to display totals on the page
	 * 7 PDFs, 6 Courses, etc.
	 * @return
	 */
	public Map<String, Integer> getFacets() {
		Map<String, Integer> facets = new HashMap<>();
		
		for (AssignmentAssetVO vo : assets) {
			SolrDocument sd = vo.getSolrDocument();
			if (sd == null) continue;
			
			String type = StringUtil.checkVal(sd.getFieldValue("assetType_s"));
			if (type.length() == 0) type = "Unknown";
			int cnt = 1;
			if (facets.containsKey(type)) {
				cnt += facets.get(type).intValue();
			}
			facets.put(type, Integer.valueOf(cnt));
		}
		
		return facets;
	}
	
	public Integer getPercentComplete() {
		int completeCnt = 0;
		for (AssignmentAssetVO vo : assets)
			if (vo.getCompleteDt() != null) ++completeCnt;
		
		return Math.round((completeCnt / assets.size())) * 100;
	}
	
	public boolean isComplete() {
		return (100 == getPercentComplete());
	}

	public List<ResidentVO> getResidents() {
		return residents;
	}

	public void setResidents(List<ResidentVO> residents) {
		this.residents = residents;
	}
	
	public void addResident(ResidentVO resident) {
		this.residents.add(resident);
	}
	
	public int getResidentsCompleted() {
		int cnt = 0;
		for (ResidentVO res : residents)
			if (res.isCompleted()) ++cnt;
		
		return cnt;
	}

	public String getAssgId() {
		return assgId;
	}

	public void setAssgId(String assgId) {
		this.assgId = assgId;
	}

	public int getResDirId() {
		return resDirId;
	}

	public void setResDirId(int resDirId) {
		this.resDirId = resDirId;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}
}