package com.rezdox.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rezdox.action.RewardsAction.Reward;
import com.rezdox.action.RezDoxNotifier.Message;
import com.rezdox.data.ProjectMyProviders;
import com.rezdox.vo.BusinessReviewVO;
import com.rezdox.vo.BusinessVO;
import com.rezdox.vo.MemberVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.http.CookieUtil;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.SQLTotalVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.mail.EmailRecipientVO;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.util.CampaignMessageSender;

/****************************************************************************
 * <b>Title</b>: BusinessReviewAction.java<p/>
 * <b>Description: Manages RezDox business reviews.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2018<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Mar 8, 2018
 ****************************************************************************/
public class BusinessReviewAction extends SimpleActionAdapter {

	public static final String COOKIE_REVIEW_COUNT = "rezdoxReviewCount";

	public BusinessReviewAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public BusinessReviewAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Overloaded constructor to simplify invocation.
	 * 
	 * @param dbConnection
	 * @param attributes
	 */
	public BusinessReviewAction(SMTDBConnection dbConnection, Map<String, Object> attributes) {
		this();
		setDBConnection(dbConnection);
		setAttributes(attributes);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (req.hasParameter("getCount")) {	
			CookieUtil.add(req, COOKIE_REVIEW_COUNT, String.valueOf(getReviewCount(RezDoxUtils.getMemberId(req))), "/", -1);
		} else {
			retrieveLists(req);

			List<BusinessReviewVO> reviewList = retrieveReviews(req);
			putModuleData(reviewList, reviewList.size(), false);

			// Business info for the business whose reviews are being shown
			if (req.hasParameter(BusinessAction.REQ_BUSINESS_ID)) {
				BusinessAction ba = new BusinessAction(dbConn, attributes);
				req.setAttribute(BusinessAction.BUSINESS_DATA, ba.retrieveBusinesses(req));
			}
		}
	}

	/**
	 * Retrives the selected business reviews.
	 * 
	 * @param req
	 * @return
	 * @throws ActionException 
	 */
	protected List<BusinessReviewVO> retrieveReviews(ActionRequest req) {
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		BusinessReviewVO options = new BusinessReviewVO(req);

		StringBuilder sql = getBaseReviewSql();
		sql.append("where 1=0 ");

		if (!StringUtil.isEmpty(options.getParentId())) {
			sql.append("or br.parent_id = ? ");
			params.add(options.getParentId());

		} else if (!StringUtil.isEmpty(options.getBusinessReviewId())) {
			sql.append("or br.business_review_id = ? ");
			params.add(options.getBusinessReviewId());

		} else if (!StringUtil.isEmpty(options.getBusinessId())) {
			sql.append("or (br.business_id = ? and br.parent_id is null) ");
			params.add(options.getBusinessId());

		} else {
			sql.append("or (br.member_id = ? and br.parent_id is null) ");
			params.add(RezDoxUtils.getMemberId(req));
		}

		sql.append("order by create_dt desc ");

		log.debug("Business Review SQL: " + sql);

		// Get the review data
		DBProcessor dbp = new DBProcessor(dbConn, schema);
		return dbp.executeSelect(sql.toString(), params, new BusinessReviewVO());
	}

	/**
	 * Returns a list of reviews that have not yet been moderated
	 * 
	 * @return
	 */
	protected List<BusinessReviewVO> retrieveUnmoderatedReviews() {
		// Use the base query
		StringBuilder sql = getBaseReviewSql();
		sql.append("where moderated_flg = ? ");

		// Get everything in the system that hasn't been moderated
		List<Object> params = new ArrayList<>();
		params.add(0);

		// Get/return the data
		DBProcessor dbp = new DBProcessor(dbConn, getCustomSchema());
		return dbp.executeSelect(sql.toString(), params, new BusinessReviewVO());
	}


	/**
	 * Returns the base sql needed for getting business reviews.
	 * May be appended to as necessary depending on usage.
	 * 
	 * @return
	 */
	private StringBuilder getBaseReviewSql() {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(500);
		sql.append("select br.business_review_id, br.parent_id, br.member_id, br.business_id, br.rating_no, br.review_txt, br.create_dt, br.update_dt, ");
		sql.append("br.moderated_flg, m.profile_id, m.privacy_flg, m.profile_pic_pth, b.business_nm, b.photo_url, m.first_nm, m.last_nm, coalesce(reply_count, 0) as reply_count ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_member_business_review br ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_member m on br.member_id = m.member_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_business b on br.business_id = b.business_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("(select count(*) as reply_count, parent_id ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_member_business_review where parent_id is not null ");
		sql.append("group by parent_id) rep on br.business_review_id = rep.parent_id ");
		return sql;
	}


	/**
	 * Gets lists for the member to switch between businesses
	 * 
	 * @param req
	 */
	private void retrieveLists(ActionRequest req) {
		SBUserRole role = ((SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA));

		// List of all businesses this member owns - My Businesses Reviews
		if (RezDoxUtils.isBusinessRole(role) && !req.hasParameter("getMyReviews")) {
			List<BusinessVO> businessList = new BusinessAction(dbConn, attributes).loadBusinessList(req);
			req.setAttribute("businessList", businessList);
			if (!req.hasParameter(BusinessAction.REQ_BUSINESS_ID) && !businessList.isEmpty()) {
				req.setParameter(BusinessAction.REQ_BUSINESS_ID, businessList.get(0).getBusinessId());
			}
		}

		// List of all businesses this user is connected to and can therefore leave a review for - My Reviews
		ProjectMyProviders pmp = new ProjectMyProviders(dbConn, attributes);
		List<GenericVO> providerList = pmp.retrieveMyProviders(RezDoxUtils.getMemberId(req));
		req.setAttribute("providerList", providerList);
	}


	/**
	 * Gets the number of reviews left for this member's businesses.
	 * 
	 * @param memberId
	 * @return
	 */
	public int getReviewCount(String memberId) {
		String schema = getCustomSchema();

		// Build the query
		StringBuilder sql = new StringBuilder(250);
		sql.append("select cast(count(*) as int) as total_rows_no from ").append(schema).append("rezdox_member_business_review mbr ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("rezdox_business_member_xr bmx on mbr.business_id = bmx.business_id and bmx.member_id = ? ");
		sql.append("where parent_id is null ");
		log.debug("Review Count Sql: " + sql.toString() + "|" + memberId);

		// Get & return the data
		DBProcessor dbp = new DBProcessor(dbConn, schema);
		List<SQLTotalVO> data = dbp.executeSelect(sql.toString(), Arrays.asList(memberId), new SQLTotalVO());
		return data == null ? 0 : data.get(0).getTotal();
	}

	/* 
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		boolean isModerated = req.hasParameter(BusinessAdminDataTool.REQ_ADMIN_MODERATE);
		BusinessReviewVO review = new BusinessReviewVO(req);
		DBProcessor dbp = new DBProcessor(dbConn, getCustomSchema());

		// Ensure the member editing/deleting, is the member who left the review originally
		if (!(StringUtil.isEmpty(review.getBusinessReviewId())) && !isModerated) {
			List<BusinessReviewVO> existingReview = retrieveReviews(req);
			if (!RezDoxUtils.getMemberId(req).equals(existingReview.get(0).getMemberId())) {
				return;
			}
		}

		try {
			if (req.hasParameter("isDelete")) {
				dbp.delete(review);

			} else {
				saveReview(review, dbp, req);

				if(!isModerated && !StringUtil.isEmpty(review.getBusinessId()))
					sendReviewEmail(req);
			}
		} catch(Exception e) {
			log.error("Could not save or delete business review", e);
		}

		putModuleData(review.getBusinessReviewId(), 1, false);
	}

	/**
	 * Saves a review while setting the group id as appropriate
	 * 
	 * @param review
	 * @param dbp
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	protected void saveReview(BusinessReviewVO review, DBProcessor dbp, ActionRequest req) 
			throws InvalidDataException, DatabaseException {
		// The group id could be either the reviewId or the parentId,
		// therefore we need to know the reviewId ahead of time, if this is an insert.
		boolean newReview = false;
		if (StringUtil.isEmpty(review.getBusinessReviewId())) {
			newReview = true;
			review.setBusinessReviewId(new UUIDGenerator().getUUID());
		}
		review.setGroupId(StringUtil.isEmpty(review.getParentId()) ? review.getBusinessReviewId() : review.getParentId());

		if (newReview) {
			dbp.insert(review);
			awardPoints(review.getMemberId(), req);
		} else {
			dbp.update(review);
		}
		//notify the business
		notifyBusinessOwner(review, req);
	}


	/**
	 * @param memberId
	 */
	private void awardPoints(String memberId, ActionRequest req) {
		RewardsAction ra = new RewardsAction(getDBConnection(), getAttributes());
		try {
			ra.applyReward(Reward.REVIEW_BUS.name(), memberId, req);
		} catch (ActionException e) {
			log.error("could not award reward points", e);
		}
	}


	/**
	 * Put a browser notification up for the business owner(s), so they see this good news
	 * @param bvo
	 */
	private void notifyBusinessOwner(BusinessReviewVO vo, ActionRequest req) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		RezDoxNotifier notifyUtil = new RezDoxNotifier(site, getDBConnection(), getCustomSchema());
		String[] profileIds = notifyUtil.getProfileIds(vo.getBusinessId(), false);

		//quit while we're ahead if there's nobody to inform
		if (profileIds == null || profileIds.length == 0) return;
		Map<String, Object> params = new HashMap<>();
		params.put("url", "/member/review");
		notifyUtil.send(Message.REVIEW_RCVD, params, null, profileIds);
	}

	/**
	 * Sends notification to the business about a review that was left
	 * @param req
	 */
	private void sendReviewEmail(ActionRequest req) {
		// Get the details for the business in question, using the businessId from the request
		BusinessAction ba = new BusinessAction(dbConn, attributes);
		BusinessVO business = ba.retrieveBusinesses(req).get(0);

		// Create the data map for the email
		Map<String, Object> dataMap = new HashMap<>();
		dataMap.put("reviewerName", RezDoxUtils.getMember(req).getFullName());
		dataMap.put("businessName", business.getBusinessName());

		// Set the recipient. Send to the business email address.
		List<EmailRecipientVO> rcpts = new ArrayList<>();
		MemberVO recipient = business.getMembers().entrySet().iterator().next().getValue();
		rcpts.add(new EmailRecipientVO(recipient.getProfileId(), business.getEmailAddressText(), EmailRecipientVO.TO));

		// Send the email
		CampaignMessageSender util = new CampaignMessageSender(getAttributes());
		util.sendMessage(dataMap, rcpts, RezDoxUtils.EmailSlug.REVIEW_BUSINESS.name());
	}
}