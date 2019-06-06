package com.rezdox.action;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rezdox.action.BusinessAction.BusinessStatus;
import com.rezdox.data.BusinessCategoryList;
import com.rezdox.vo.BusinessReviewVO;
import com.rezdox.vo.BusinessVO;
import com.rezdox.vo.MemberVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.io.mail.EmailRecipientVO;
import com.siliconmtn.sb.email.util.EmailCampaignBuilderUtil;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> BusinessAdminDataTool.java<br/>
 * <b>Description:</b> WC Data Tool for managing business approvals and business
 * review moderation.
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author Tim Johnson
 * @version 1.0
 * @since March 14, 2018
 ****************************************************************************/
public class BusinessAdminDataTool extends SimpleActionAdapter {
	public static final String REQ_BUSINESS_STATUS = "businessStatus";
	public static final String REQ_APPROVE_REVIEW = "approveReview";
	public static final String REQ_DELETE_REVIEW = "deleteReview";
	public static final String REQ_ADMIN_MODERATE = "adminModerate";

	public BusinessAdminDataTool() {
		super();
	}

	public BusinessAdminDataTool(ActionInitVO arg0) {
		super(arg0);
	}


	/*
	 * Return a list of all the businesses requiring approval or business reviews requiring moderation.
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SimpleActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		req.setParameter("loadAll", "1");
		BusinessCategoryList bcl = new BusinessCategoryList(dbConn, attributes);
		bcl.retrieve(req);
		req.setAttribute("categoryList", ((ModuleVO) bcl.getAttribute(Constants.MODULE_DATA)).getActionData());

		// Only load data for ajax calls (bootstrap tables)
		if (!req.hasParameter("loadData")) return;

		if (req.hasParameter("businessApproval")) {
			BusinessAction ba = new BusinessAction(getDBConnection(), getAttributes());
			List<BusinessVO> businesses = ba.retrievePendingBusinesses();
			putModuleData(businesses);

		} else if (req.hasParameter("reviewModeration")) {
			BusinessReviewAction revAction = new BusinessReviewAction(getDBConnection(), getAttributes());
			List<BusinessReviewVO> reviews = revAction.retrieveUnmoderatedReviews();
			putModuleData(reviews);
		}
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SimpleActionAdapter#update(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
		String msg = null;

		if (req.hasParameter(REQ_BUSINESS_STATUS)) {
			BusinessAction ba = new BusinessAction(dbConn, attributes);
			BusinessVO business = ba.retrieveBusinesses(req).get(0);

			// Set value required for approval or denial of business
			business.setStatusCode(Convert.formatInteger(req.getParameter(REQ_BUSINESS_STATUS)));
			msg = setBusinessStatus(business);

			// Send confirmation to the business member
			sendApprovalStatusEmail(business);

		} else if (req.hasParameter(REQ_APPROVE_REVIEW)) {
			// Set values required for moderation of review
			BusinessReviewVO businessReview = new BusinessReviewVO();
			businessReview.setBusinessReviewId(req.getParameter("businessReviewId"));
			businessReview.setModeratedFlag(Convert.formatInteger(req.getParameter(REQ_APPROVE_REVIEW)));
			msg = setReviewStatus(businessReview);

		}

		adminRedirect(req, msg, (String) getAttribute(AdminConstants.ADMIN_TOOL_PATH));
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SimpleActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		if (req.hasParameter(REQ_DELETE_REVIEW)) {
			req.setParameter(REQ_ADMIN_MODERATE, "1");
			req.setParameter("isDelete", "1");

			BusinessReviewAction revAction = new BusinessReviewAction(getDBConnection(), getAttributes());
			revAction.build(req);
		}
	}

	/**
	 * Approve or deny a business.
	 * @param mrv
	 */
	private String setBusinessStatus(BusinessVO business) {
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.UPDATE_CLAUSE).append(getCustomSchema()).append("rezdox_business_member_xr ");
		sql.append("set status_flg=? where business_id=? ");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1, business.getStatusCode());
			ps.setString(2, business.getBusinessId());
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("Could not approve or deny business ", sqle);
			return (String) getAttribute(AdminConstants.KEY_ERROR_MESSAGE); 
		}

		return (String) getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
	}


	/**
	 * Sends an email confirmation to the business member as to
	 * whether the business was approved or denied
	 * @param business
	 */
	private void sendApprovalStatusEmail(BusinessVO business) {
		Map<String, Object> dataMap = new HashMap<>();
		dataMap.put("businessName", business.getBusinessName());
		log.debug(business);

		// Set the recipient. Send to the business email address.
		List<EmailRecipientVO> rcpts = new ArrayList<>();
		MemberVO recipient = business.getMembers().entrySet().iterator().next().getValue();
		rcpts.add(new EmailRecipientVO(recipient.getProfileId(), business.getEmailAddressText(), EmailRecipientVO.TO));

		// Send the appropriate email based on the approval status
		String emailSlug = RezDoxUtils.EmailSlug.BUSINESS_APPROVED.name();
		if (business.getStatus() == BusinessStatus.INACTIVE)
			emailSlug = RezDoxUtils.EmailSlug.BUSINESS_DECLINED.name();

		EmailCampaignBuilderUtil util = new EmailCampaignBuilderUtil(getDBConnection(), getAttributes());
		util.sendMessage(dataMap, rcpts, emailSlug);
	}


	/**
	 * Approve a review
	 * @param mrv
	 */
	private String setReviewStatus(BusinessReviewVO businessReview) {
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.UPDATE_CLAUSE).append(getCustomSchema()).append("rezdox_member_business_review ");
		sql.append("set moderated_flg = ? ");
		sql.append("where business_review_id = ? ");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1, businessReview.getModeratedFlag());
			ps.setString(2, businessReview.getBusinessReviewId());
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("Could not moderate review ", sqle);
			return (String) getAttribute(AdminConstants.KEY_ERROR_MESSAGE); 
		}

		return (String) getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
	}
}