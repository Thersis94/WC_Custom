package com.rezdox.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rezdox.action.RezDoxUtils.EmailSlug;
import com.rezdox.vo.BusinessVO;
import com.rezdox.vo.MemberVO;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.io.mail.EmailRecipientVO;
import com.siliconmtn.sb.email.util.EmailCampaignBuilderUtil;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <p><b>Title</b>: ProjectShareAction.java</p>
 * <p><b>Description:</b> Fires the emails related to sharing a project (between business and homeowner).</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2018, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Jun 1, 2018
 * <b>Changes:</b>
 ****************************************************************************/
public class ProjectShareAction extends SimpleActionAdapter {

	public ProjectShareAction() {
		super();
	}

	public ProjectShareAction(ActionInitVO arg0) {
		super(arg0);
	}

	/**
	 * @param dbConnection
	 * @param attributes
	 */
	public ProjectShareAction(SMTDBConnection dbConnection, Map<String, Object> attributes) {
		this();
		setDBConnection(dbConnection);
		setAttributes(attributes);
	}


	/**
	 * Sends the given email out.  Internally performs data lookups and workflows as needed to 
	 * generate the email.
	 * @param slugTxt
	 * @param req
	 */
	public void sendEmail(EmailSlug slugTxt, ActionRequest req) {
		String projectId = req.getParameter(ProjectAction.REQ_PROJECT_ID);
		MemberVO o = loadHomeOwner(projectId); //o=owner
		BusinessVO b = loadBusiness(projectId); //b=business

		//test for unlikely scenarios that would cause email failure
		if (StringUtil.isEmpty(o.getProfileId()) || StringUtil.isEmpty(b.getBusinessId())) {
			log.error("could not send project sharing emails, data is missing:");
			log.error("owner= " + o);
			log.error("business= " + b);
			return;
		}

		if (EmailSlug.PROJ_ACCPT_BUSINESS == slugTxt) {
			sendEmail(slugTxt, b.getBusinessId(), b.getEmailAddressText(), o, b.getBusinessName(), b.getCategoryName());

		} else if (EmailSlug.PROJ_ACCPT_HOMEOWNER == slugTxt) {
			sendEmail(slugTxt, o.getProfileId(), o.getEmailAddress(), o, b.getBusinessName(), b.getCategoryName());

		} else if (EmailSlug.PROJ_SHARE_BUSINESS == slugTxt) { //to owner from business
			sendEmail(slugTxt, o.getProfileId(), o.getEmailAddress(), o, b.getBusinessName(), b.getCategoryName());

		} else if (EmailSlug.PROJ_SHARE_HOMEOWNER == slugTxt) { //to business from owner
			sendEmail(slugTxt, b.getBusinessId(), b.getEmailAddressText(), o, b.getBusinessName(), b.getCategoryName());
		}
	}


	/**
	 * performs the actual email delivery.  Reused by all supported scenarios
	 * @param slugTxt
	 * @param rcptProfileId
	 * @param rcptEmail
	 * @param owner
	 * @param businessNm
	 * @param residenceNm
	 */
	private void sendEmail(EmailSlug slugTxt, String rcptProfileId, String rcptEmail, 
			MemberVO owner, String businessNm, String projectNm) {
		Map<String, Object> dataMap = new HashMap<>();
		dataMap.put("firstName", owner.getFirstName());
		dataMap.put("lastName", owner.getLastName());
		dataMap.put("residenceName", owner.getProfilePicPath());
		dataMap.put("projectName", projectNm);
		dataMap.put("businessName", businessNm);

		List<EmailRecipientVO> rcpts = new ArrayList<>();
		rcpts.add(new EmailRecipientVO(rcptProfileId, rcptEmail, EmailRecipientVO.TO));

		EmailCampaignBuilderUtil util = new EmailCampaignBuilderUtil(getDBConnection(), getAttributes());
		util.sendMessage(dataMap, rcpts, slugTxt.name());
	}



	/**
	 * loads the business' profile - tied to the given project
	 * @param projectId
	 * @return
	 */
	private BusinessVO loadBusiness(String projectId) {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select b.business_nm, coalesce(b.email_address_txt, m.email_address_txt) as email_address_txt, ");
		//stuff the biz owner's contact info into BusinessVO fields to keep things easy.
		sql.append("m.profile_id as business_id, p.project_nm as category_nm "); //used in email_campaign_log
		sql.append("from ").append(schema).append("rezdox_project p ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("rezdox_business b on p.business_id=b.business_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("rezdox_business_member_xr bm on b.business_id=bm.business_id and bm.status_flg=1 ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("rezdox_member m on bm.member_id=m.member_id ");
		sql.append("where p.project_id=?");
		log.debug(sql);

		DBProcessor dbp = new DBProcessor(getDBConnection(), schema);
		List<BusinessVO> data = dbp.executeSelect(sql.toString(), Arrays.asList(projectId), new BusinessVO());
		return !data.isEmpty() ? data.get(0) : new BusinessVO();
	}


	/**
	 * loads the homeowner's profile - tied to the given project
	 * @param projectId
	 * @return
	 */
	private MemberVO loadHomeOwner(String projectId) {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select m.profile_id, m.first_nm, m.last_nm, m.email_address_txt, ");
		sql.append("r.residence_nm as profile_pic_pth "); //stuffing makes things cleaner
		sql.append("from ").append(schema).append("rezdox_project p ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("rezdox_residence r on p.residence_id=r.residence_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("rezdox_residence_member_xr rm on r.residence_id=rm.residence_id and rm.status_flg=1 ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("rezdox_member m on rm.member_id=m.member_id ");
		sql.append("where p.project_id=?");
		log.debug(sql);

		DBProcessor dbp = new DBProcessor(getDBConnection(), schema);
		List<MemberVO> data = dbp.executeSelect(sql.toString(), Arrays.asList(projectId), new MemberVO());
		return !data.isEmpty() ? data.get(0) : new MemberVO();
	}
}
