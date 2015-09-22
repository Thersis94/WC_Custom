package com.depuysynthesinst.assg;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;

import com.depuysynthesinst.assg.ResidentVO.ResidentGrouping;
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
	private Map<String, ResidentVO> residents;
	private String assgName;
	private String assgDesc;
	private Date dueDt;
	private Date publishDt;
	private boolean sequentialFlg; //if the assets should be completed in a certain sequence
	private boolean skipAheadFlg; //if the user has acknowledged that they're out of sequence.
	private int orderNo;
	private Date createDt;
	private Date updateDt;
	private List<AssignmentAssetVO> assets;
	
	private String resAssgId;
	
	
	public AssignmentVO() {
		assets = new ArrayList<>();
		residents = new HashMap<>();
	}
	
	public AssignmentVO(ResultSet rs) {
		this();
		DBUtil util = new DBUtil();
		assgId = util.getStringVal("assg_id", rs);
		resDirId = util.getIntVal("res_dir_id", rs);
		parentId = util.getStringVal("parent_id", rs);
		assgName = util.getStringVal("assg_nm", rs);
		assgDesc = util.getStringVal("desc_txt", rs);
		directorProfileId = util.getStringVal("res_dir_profile_id", rs);
		dueDt = util.getDateVal("due_dt", rs);
		setPublishDt(util.getDateVal("publish_dt", rs));
		setUpdateDt(util.getDateVal("update_dt", rs));
		setSequentialFlg(util.getIntegerVal("sequential_flg", rs));
		setSkipAheadFlg(util.getIntegerVal("skip_ahead_flg", rs));
		setResAssgId(util.getStringVal("res_assg_id", rs));
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


	public Date getCreateDt() {
		return createDt;
	}


	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}


	public Date getUpdateDt() {
		if (updateDt != null && publishDt != null && updateDt.after(publishDt))
			return updateDt;
		else return null;
	}


	public void setUpdateDt(Date updateDt) {
		this.updateDt = updateDt;
	}


	public List<AssignmentAssetVO> getAssets() {
		Collections.sort(assets);
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
			
			String type = StringUtil.checkVal(sd.getFieldValue("assetType_s")).toUpperCase();
			if (type.length() == 0) type = "Unknown";
			type = StringUtil.capitalizePhrase(type, 4);
			if ("Emodule".equals(type)) type = "eModule";
			int cnt = 1;
			if (facets.containsKey(type)) {
				cnt += facets.get(type).intValue();
			}
			facets.put(type, Integer.valueOf(cnt));
		}
		
		return facets;
	}
	
	public void addResidentStats(String residentId, int completeCnt) {
		if (this.residents.containsKey(residentId))
				this.residents.get(residentId).setCompleteCnt(completeCnt);
	}
	
	public Integer getPercentComplete() {
		if (assets.size() == 0) return 0;
		double completeCnt = 0;
		double size = assets.size();
		for (AssignmentAssetVO vo : assets)
			if (vo.getCompleteDt() != null) ++completeCnt;

		try {
			double d = (completeCnt / size);
			return Double.valueOf(d * 100).intValue();
		} catch (Exception e) {
			return 0;
		}
	}
	
	public boolean isComplete() {
		return (100 == getPercentComplete());
	}

	public Collection<ResidentVO> getResidents() {
		return residents.values();
	}

	public void addResident(ResidentVO resident) {
		if (! residents.containsKey(resident.getResidentId()))
				this.residents.put(resident.getResidentId(), resident);
	}
	
	public int getResidentsCompleted() {
		int cnt = 0;
		for (ResidentVO res : residents.values()) {
			if (res.getCompleteCnt() == this.assets.size()) ++cnt;
		}
		
		return cnt;
	}
	
	public void setResidentAssetCompleted(String assgAssetId, Date completeDt) {
		if (completeDt == null) return; //nothing needs to be done if the asset is not complete
		
		for (AssignmentAssetVO asset : assets) {
			if (asset.getAssgAssetId().equals(assgAssetId))
				asset.setCompleteDt(completeDt);
		}
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
	
	public boolean isExpired() {
		/**
		 * "expired" has 3 faces per requirement U_BUS_2010
		 * All elements of the assignment have been completed/checked
		 * Or Due Date has passed (if there is a Due Date)
		 * Or 90 days after creation if there is no Due Date 
		 */
		//if the due date has lapsed
		Calendar cal = Calendar.getInstance();
		if (dueDt != null && dueDt.before(cal.getTime())) return true;
		
		//if the assignment is 90 days old
		cal.add(Calendar.DAY_OF_YEAR, -90);
		Date d = updateDt != null ? updateDt : publishDt;
		if (d != null && d.before(cal.getTime())) return true;
		
		//if all the assets have been completed
		return isComplete();
	}
	
	public ResidentGrouping getResidentGrouping() {
		return new ResidentVO().new ResidentGrouping(new ArrayList<ResidentVO>(residents.values()));
	}

	public Date getPublishDt() {
		return publishDt;
	}

	public void setPublishDt(Date publishDt) {
		this.publishDt = publishDt;
	}

	public String getResAssgId() {
		return resAssgId;
	}

	public void setResAssgId(String resAssgId) {
		this.resAssgId = resAssgId;
	}
}