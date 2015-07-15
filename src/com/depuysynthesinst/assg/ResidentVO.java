package com.depuysynthesinst.assg;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: ResidentVO.java<p/>
 * <b>Description: VO represents DPY_SYN_INST_RESIDENT table in DB.  
 * A student/pupil eligible for assignments or enrolled in an assignment.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 7, 2015
 ****************************************************************************/
public class ResidentVO implements Serializable {
	private static final long serialVersionUID = -1227800196334867170L;

	private String residentId;
	private int residentDirectorId;
	private String pgyId;
	private String profileId;
	private UserDataVO profile;
	private int orderNo;
	private Date consentDt;
	private boolean consentTimedOut;
	private Date consentReqDt; //the date the Director last requested this user join their roster.  If pending, requests can be re-sent after 10 days.
	private List<AssignmentVO> assignments;
	private String resAssgId;
	private int completeCnt;

	public ResidentVO() {
		assignments = new ArrayList<>();
	}

	public ResidentVO(ResultSet rs) {
		this();
		DBUtil util = new DBUtil();
		residentId = util.getStringVal("resident_id", rs);
		residentDirectorId = util.getIntVal("res_dir_id", rs);
		resAssgId = util.getStringVal("res_assg_id", rs);
		pgyId = util.getStringVal("pgy_id", rs);
		profileId = util.getStringVal("profile_id", rs);
		consentDt = util.getDateVal("consent_dt", rs);
		setConsentTimedOut(util.getDateVal("invite_sent_dt", rs));
	}
	
	public ResidentVO(SMTServletRequest req) {
		this();
		profileId = StringUtil.checkVal(req.getParameter("profileId"), null);
		residentId = StringUtil.checkVal(req.getParameter("residentId"), null);
		residentDirectorId = Convert.formatInteger("" + req.getSession().getAttribute(AssignmentsFacadeAction.RES_DIR_ID));
	}
	
	
	public String getResidentId() {
		return residentId;
	}

	public void setResidentId(String residentId) {
		this.residentId = residentId;
	}

	public String getPgyId() {
		return pgyId;
	}

	public void setPgyId(String pgyId) {
		this.pgyId = pgyId;
	}

	public String getProfileId() {
		return profileId;
	}

	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}

	public int getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(int orderNo) {
		this.orderNo = orderNo;
	}

	public Date getConsentDt() {
		return consentDt;
	}

	public void setConsentDt(Date consentDt) {
		this.consentDt = consentDt;
	}

	public Date getConsentReqDt() {
		return consentReqDt;
	}

	public void setConsentReqDt(Date consentReqDt) {
		this.consentReqDt = consentReqDt;
	}

	public List<AssignmentVO> getAssignments() {
		return assignments;
	}

	public void setAssignments(List<AssignmentVO> assignments) {
		this.assignments = assignments;
	}
	
	public boolean isConsentTimedOut() {
		return consentTimedOut;
	}

	private void setConsentTimedOut(Date updateDt) {
		if (consentDt != null) { //can't timeout if it's already been done!
			consentTimedOut = false; 
			return;
		} else if (updateDt == null) { //this is really just a null trap; updateDt can never be null according to the query.
			consentTimedOut = true;
			return;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(updateDt);
		cal.add(Calendar.DAY_OF_YEAR, MyResidentsAction.CONSENT_TIMEOUT);

		consentTimedOut = (cal.getTime().before(Convert.getCurrentTimestamp()));
	}

	public UserDataVO getProfile() {
		return profile;
	}

	public void setProfile(UserDataVO profile) {
		this.profile = profile;
	}

	public int getResidentDirectorId() {
		return residentDirectorId;
	}

	public void setResidentDirectorId(int residentDirectorId) {
		this.residentDirectorId = residentDirectorId;
	}
	
	
	public String getResAssgId() {
		return resAssgId;
	}

	public void setResAssgId(String resAssgId) {
		this.resAssgId = resAssgId;
	}


	public int getCompleteCnt() {
		return completeCnt;
	}

	public void setCompleteCnt(int completeCnt) {
		this.completeCnt = completeCnt;
	}


	/**
	 * **************************************************************************
	 * <b>Title</b>: ResidentGrouping.java<p/>
	 * <b>Description: a simple nested class used in the Views to cleaning print all the
	 * Residents into buckets based on their PGY year.</b> 
	 * <p/>
	 * <b>Copyright:</b> Copyright (c) 2015<p/>
	 * <b>Company:</b> Silicon Mountain Technologies<p/>
	 * @author James McKain
	 * @version 1.0
	 * @since Jul 10, 2015
	 ***************************************************************************
	 */
	public class ResidentGrouping {
		private Map<String, List<ResidentVO>> data = new LinkedHashMap<>();
		private int totalResidents;
		public ResidentGrouping(List<ResidentVO> resList) {
			data.put("PGY 1",new ArrayList<ResidentVO>());
			data.put("PGY 2",new ArrayList<ResidentVO>());
			data.put("PGY 3",new ArrayList<ResidentVO>());
			data.put("PGY 4",new ArrayList<ResidentVO>());
			data.put("PGY 5",new ArrayList<ResidentVO>());
			data.put("PGY 6",new ArrayList<ResidentVO>());
			data.put("PGY 7",new ArrayList<ResidentVO>());
			data.put("Unknown",new ArrayList<ResidentVO>());
			
			for (ResidentVO res : resList) {
				++totalResidents;
				if (res.getPgyId() == null || res.getPgyId().length() == 0) {
					data.get("Unknown").add(res);
				} else {
					data.get("PGY " + res.getPgyId()).add(res);
				}
			}
		}
		
		public Map<String, List<ResidentVO>> getData() { return data; }
		public int getTotalResidents() { return totalResidents; }
	}
}


