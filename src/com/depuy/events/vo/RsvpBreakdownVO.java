package com.depuy.events.vo;

import java.sql.ResultSet;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.security.UserDataVO;

/****************************************************************************
 * <b>Title</b>: RsvpBreakdownVO.java<p/>
 * <b>Description: holds line-items for the rsvpBreakdown report.  Simple sumation of rsvp 
 * referral sources and a percent-of-total rsvps for each one. </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 20, 2014
 ****************************************************************************/
public class RsvpBreakdownVO extends UserDataVO {
	private static final long serialVersionUID = -8767070559534694420L;
	private String rsvpCode;
	private Date seminarDate;
	private Map<String, Integer> referralStats;

	public RsvpBreakdownVO() {
		referralStats = new HashMap<>();
	}

	public RsvpBreakdownVO(ResultSet rs) {
		this();
		DBUtil db = new DBUtil();
		super.setProfileId(db.getStringVal("profile_id", rs));
		rsvpCode = db.getStringVal("rsvp_code_txt", rs);
		seminarDate = db.getDateVal("start_dt", rs);
		addReferralStat(db.getStringVal("referral_txt", rs), db.getIntVal("cnt", rs));
	}

	public String getRsvpCode() {
		return rsvpCode;
	}
	public void setRsvpCode(String rsvpCode) {
		this.rsvpCode = rsvpCode;
	}
	public Date getSeminarDate() {
		return seminarDate;
	}
	public void setSeminarDate(Date seminarDate) {
		this.seminarDate = seminarDate;
	}
	public Map<String, Integer> getReferralStats() {
		return referralStats;
	}
	public void setReferralStats(Map<String, Integer> referralStats) {
		this.referralStats = referralStats;
	}

	public void addReferralStat(String stat, int cnt) {
		if (referralStats.containsKey(stat))
			cnt += referralStats.get(stat);

		referralStats.put(stat, cnt);
	}
}
