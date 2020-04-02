package com.rezdox.action;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rezdox.vo.MemberVO;
import com.rezdox.vo.MembershipVO;
import com.rezdox.vo.SubscriptionVO;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.io.mail.EmailRecipientVO;
import com.siliconmtn.sb.email.util.EmailCampaignBuilderUtil;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title</b>: DataEntryBuyEmailAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> this class will get the data needed and trigger an email camp to send an email each time 
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Mar 19, 2020
 * @updates:
 ****************************************************************************/
public class DataEntryBuyEmailAction extends SimpleActionAdapter {
	public static final String REZDOX_DATA_ENTRY_BUY = "REZDOX_DATA_ENTRY_BUY";

	public DataEntryBuyEmailAction() {
		super();
	}

	public DataEntryBuyEmailAction(ActionInitVO arg0) {
		super(arg0);
	}
	/**
	 * @param dbConn
	 * @param attributes
	 */
	public DataEntryBuyEmailAction(Connection dbConn, Map<String, Object> attributes) {
		this();
		this.setAttributes(attributes);
		this.setDBConnection((SMTDBConnection) dbConn);
	}
	

	/**
	 * gets the memberships name by membership id
	 * @param membershipId
	 * @return
	 */
	private String getMembershipName(String membershipId) {
		
		String schema = getCustomSchema();
		List<Object> vals = new ArrayList<>();
		vals.add(membershipId);

		StringBuilder sql = new StringBuilder(70);
		sql.append(DBUtil.SELECT_FROM_STAR).append(schema).append("rezdox_membership where membership_id = ? ");
		
		log.debug("sql " + sql.toString()+ "|" +vals);
		 
		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		List<MembershipVO> data = db.executeSelect(sql.toString(), vals, new MembershipVO());
		if (data != null && ! data.isEmpty()) {
			return data.get(0).getMembershipName();
		}
		
		return null;
	}

	/**
	 * @param s
	 */
	public void sendEmail(SubscriptionVO s) {
		
		EmailCampaignBuilderUtil ecbu = new EmailCampaignBuilderUtil(dbConn, attributes);
		String membershipId = StringUtil.checkVal(s.getMemberId());
		String membershipName = StringUtil.checkVal(getMembershipName(membershipId));
		MemberVO user = getUserByMemberId(s.getMemberId());
		if(user == null) {
			log.debug("null user for memeber id: " + s.getMemberId() + " no email is sent: " );
			return;
		}
		
		Map<String, Object> emailParams = new HashMap<>();
		emailParams.put("firstName", user.getFirstName());
		emailParams.put("lastName", user.getLastName());
		emailParams.put("emailAddress", user.getEmailAddress());
		emailParams.put("membershipId", membershipId);
		emailParams.put("membershipName", membershipName);
		
		List<EmailRecipientVO> recipients = new ArrayList<>();
		recipients.add(new EmailRecipientVO(null, "dataentry@rezdox.com", EmailRecipientVO.TO));
	
		ecbu.sendMessage(emailParams, recipients, REZDOX_DATA_ENTRY_BUY);
		
	}

	/**
	 * @param memberId
	 * @return
	 */
	private MemberVO getUserByMemberId(String memberId) {
		String schema = getCustomSchema();
		List<Object> vals = new ArrayList<>();
		vals.add(memberId);

		StringBuilder sql = new StringBuilder(70);
		sql.append(DBUtil.SELECT_FROM_STAR).append(schema).append("rezdox_member where member_id = ? ");
		
		log.debug("sql " + sql.toString()+ "|" +vals);
		 
		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		List<MemberVO> data = db.executeSelect(sql.toString(), vals, new MemberVO());
		if (data != null && ! data.isEmpty()) {
			return data.get(0);
		}

		return null;
	}
	
}
