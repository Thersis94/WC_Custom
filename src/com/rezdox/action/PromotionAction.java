package com.rezdox.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.rezdox.vo.PromotionVO;
import com.rezdox.vo.PromotionXRVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: PromotionAction.java<p/>
 * <b>Description: Handles RezDox membership promotions.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2018<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Jan 29, 2018
 ****************************************************************************/
public class PromotionAction extends SBActionAdapter {

	public static final String PROMOTION_ID = "promotionId";

	/**
	 * Default promotion code used when someone signs up for the service
	 */
	public static final String SIGNUP_PROMOTION_CD = "REZDOXFIRST";

	public PromotionAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public PromotionAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * @param dbConnection
	 * @param attributes
	 */
	public PromotionAction(SMTDBConnection dbConnection, Map<String, Object> attributes) {
		this();
		setDBConnection(dbConnection);
		setAttributes(attributes);
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		List<PromotionVO> promotions = retrievePromotions(req);

		// Get membership data for the select picker
		MembershipAction ma = new MembershipAction(this.actionInit);
		ma.setAttributes(this.attributes);
		ma.setDBConnection(dbConn);
		req.setAttribute("memberships", ma.retrieveMemberships(req));

		putModuleData(promotions, promotions.size(), true);
	}


	/**
	 * Retrieves list of promotions
	 * @param req
	 * @return
	 */
	public List<PromotionVO> retrievePromotions(ActionRequest req) {
		String pCode = req.getParameter("promotionCode");
		String promoId = req.getParameter(PROMOTION_ID);
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();

		// Get the promotion(s) along with all memberships they apply to
		StringBuilder sql = new StringBuilder(250);
		sql.append("select * from ").append(schema).append("rezdox_promotion p ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_membership_promotion mp on p.promotion_id=mp.promotion_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_membership m on mp.membership_id=m.membership_id ");

		if (!StringUtil.isEmpty(promoId)) {
			sql.append("where p.promotion_id=? ");
			params.add(promoId);

		} else if (!StringUtil.isEmpty(pCode)) {
			sql.append("where p.promotion_cd=?");
			params.add(pCode);
		}

		log.debug(sql);
		DBProcessor dbp = new DBProcessor(dbConn, schema);
		return dbp.executeSelect(sql.toString(), params, new PromotionVO());
	}


	/**
	 * Retrieves the promotion used for signing up.
	 * @return
	 */
	public PromotionVO retrieveFreePromotion() {
		ActionRequest promotionReq = new ActionRequest();
		promotionReq.setParameter("promotionCode", SIGNUP_PROMOTION_CD);
		List<PromotionVO> promotions = retrievePromotions(promotionReq);
		return promotions.get(0);
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
		String msg = "";

		// Change the status or a full insert/update with xr
		if (req.hasParameter("toggleStatus")) {
			setPromotionStatus(req.getParameter(PROMOTION_ID), req.getIntegerParameter("toggleStatus"));
			msg = "The promotion status has been updated.";
		} else {
			updatePromotion(req);
			msg = "The promotion has been updated.";
		}

		sbUtil.adminRedirect(req, msg, (String) getAttribute(AdminConstants.ADMIN_TOOL_PATH));

		StringBuilder url = new StringBuilder(200);
		url.append(req.getAttribute(Constants.REDIRECT_URL));
		sbUtil.manualRedirect(req, url.toString());
	}


	/**
	 * Inserts or updates a promotion
	 * 
	 * @param req
	 */
	protected void updatePromotion(ActionRequest req) {
		PromotionVO promotion = new PromotionVO(req);
		promotion.setDiscountPctNo(promotion.getDiscountPctNo() / 100); // entered as whole percent, stored as decimal value

		try {
			DBProcessor dbp = new DBProcessor(dbConn, getCustomSchema());
			dbp.save(promotion);
			updatePromotionXR(promotion, req);
		} catch (Exception e) {
			log.error("Unable to save RezDox promotion.", e);
		}
	}


	/**
	 * Saves the XR data for memberships that are applicable to a promotion
	 * 
	 * @param promotion
	 * @param req
	 */
	protected void updatePromotionXR(PromotionVO promotion, ActionRequest req) {
		String schema = getCustomSchema();
		String sql = StringUtil.join("delete from ", schema, "rezdox_membership_promotion where promotion_id = ? ");
		List<String> fields = Arrays.asList("promotion_id");

		try {
			DBProcessor dbp = new DBProcessor(dbConn, schema);

			// Delete original xr records
			dbp.executeSqlUpdate(sql, promotion, fields);

			// Insert new xr records
			List<PromotionXRVO> xrs = new ArrayList<>();
			for (String membershipId : req.getParameterValues("membershipId"))
				xrs.add(new PromotionXRVO(promotion.getPromotionId(), membershipId));

			dbp.executeBatch(xrs);
		} catch (Exception e) {
			log.error("Unable to update RezDox membership promotion data.", e);
		}
	}


	/**
	 * Updates the status (active/inactive) of a promotion
	 * @param promotionId
	 * @param statusFlag
	 */
	protected void setPromotionStatus(String promotionId, int statusFlag) {
		String schema = getCustomSchema();
		String sql = StringUtil.join("update ", schema, "rezdox_promotion set status_flg=? where promotion_id=?");

		List<String> fields = new ArrayList<>();
		fields.add("status_flg");
		fields.add("promotion_id");

		PromotionVO promotion = new PromotionVO();
		promotion.setStatusFlag(statusFlag);
		promotion.setPromotionId(promotionId);

		try {
			DBProcessor dbp = new DBProcessor(dbConn, schema);
			dbp.executeSqlUpdate(sql, promotion, fields);
		} catch (Exception e) {
			log.error("Unable to change status of RezDox promotion. ", e);
		}
	}
}