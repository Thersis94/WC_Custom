package com.rezdox.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rezdox.vo.BusinessReviewVO;
import com.rezdox.vo.BusinessVO;
import com.rezdox.vo.MemberVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.sb.email.util.EmailCampaignBuilderUtil;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SimpleActionAdapter;

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
		List<BusinessReviewVO> reviewList = retrieveReviews(req);
		putModuleData(reviewList, reviewList.size(), false);
		
		if (req.hasParameter("businessId")) {
			BusinessAction ba = new BusinessAction(dbConn, attributes);
			req.setAttribute(BusinessAction.BUSINESS_DATA, ba.retrieveBusinesses(req));
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
		DBProcessor dbp = new DBProcessor(dbConn);
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
		StringBuilder sql = new StringBuilder(200);
		
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

	/* 
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		BusinessReviewVO review = new BusinessReviewVO(req);
		DBProcessor dbp = new DBProcessor(dbConn);
		
		// Ensure the member editing/deleting, is the member who left the review originally
		if (!(StringUtil.isEmpty(review.getBusinessReviewId())) && !req.hasParameter(BusinessAdminDataTool.REQ_ADMIN_MODERATE)) {
			List<BusinessReviewVO> existingReview = retrieveReviews(req);
			if (!RezDoxUtils.getMemberId(req).equals(existingReview.get(0).getMemberId())) {
				return;
			}
		}
		
		try {
			if (req.hasParameter("isDelete")) {
				dbp.delete(review);
			} else {
				saveReview(review, dbp);
				
				if(!req.hasParameter(BusinessAdminDataTool.REQ_ADMIN_MODERATE) && !StringUtil.isEmpty(review.getBusinessId())) {
					sendReviewEmail(req);
				}
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
	protected void saveReview(BusinessReviewVO review, DBProcessor dbp) throws InvalidDataException, DatabaseException {
		// The group id could be either the reviewId or the parentId,
		// therefore we need to know the reviewId ahead of time, if this is an insert.
		boolean newReview = false;
		if (StringUtil.isEmpty(review.getBusinessReviewId())) {
			newReview = true;
			review.setBusinessReviewId(new UUIDGenerator().getUUID());
		}
		review.setGroupId(StringUtil.isEmpty(review.getParentId()) ? review.getBusinessReviewId() : review.getParentId());
		
		if (newReview) dbp.insert(review);
		else dbp.update(review);
	}
	
	/**
	 * Sends notification to the business about a review that was left
	 * 
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
		Map<String, String> rcptMap = new HashMap<>();
		MemberVO recipient = business.getMembers().entrySet().iterator().next().getValue();
		rcptMap.put(recipient.getProfileId(), business.getEmailAddressText());
		
		// Send the email
		EmailCampaignBuilderUtil util = new EmailCampaignBuilderUtil(getDBConnection(), getAttributes());
		util.sendMessage(dataMap, rcptMap, RezDoxUtils.EmailSlug.REVIEW_BUSINESS.name());
	}
}