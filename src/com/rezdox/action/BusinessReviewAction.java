package com.rezdox.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.rezdox.vo.BusinessReviewVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.StringUtil;
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
	}

	/**
	 * Retrives the selected business reviews.
	 * 
	 * @param req
	 * @return
	 */
	protected List<BusinessReviewVO> retrieveReviews(ActionRequest req) {
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		BusinessReviewVO options = new BusinessReviewVO(req);

		StringBuilder sql = getBaseReviewSql();
		sql.append("where 1=0 ");

		if (!StringUtil.isEmpty(options.getBusinessReviewId())) {
			sql.append("or business_review_id = ? ");
			params.add(options.getBusinessReviewId());

		} else if (!StringUtil.isEmpty(options.getMemberId())) {
			sql.append("or member_id = ? ");
			params.add(options.getMemberId());
			
		} else if (!StringUtil.isEmpty(options.getBusinessId())) {
			sql.append("or business_id = ? ");
			params.add(options.getBusinessId());
		}

		DBProcessor dbp = new DBProcessor(dbConn, schema);
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
		
		sql.append("select business_review_id, member_id, business_id, rating_no, review_txt, create_dt, update_dt ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_member_business_review ");
		
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

		try {
			if (req.hasParameter("isDelete")) {
				dbp.delete(review);
			} else {
				dbp.save(review);
			}
		} catch(Exception e) {
			log.error("Could not save or delete business review", e);
		}

		putModuleData(review, 1, false);
	}
}