package com.rezdox.action;

import java.util.ArrayList;
import java.util.List;

import com.rezdox.data.ResidenceFormProcessor;
import com.rezdox.vo.MemberVO;
import com.rezdox.vo.MembershipVO.Group;
import com.rezdox.vo.ResidenceVO;
import com.siliconmtn.action.ActionControllerFactoryImpl;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.form.FormAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.DataManagerUtil;
import com.smt.sitebuilder.data.vo.FormVO;
import com.smt.sitebuilder.data.vo.GenericQueryVO;
import com.smt.sitebuilder.data.vo.QueryParamVO;

/****************************************************************************
 * <b>Title</b>: ResidenceAction.java<p/>
 * <b>Description: Manages member interactions with a residence.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2018<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Feb 7, 2018
 ****************************************************************************/
public class ResidenceAction extends SBActionAdapter {
	
	public static final String RESIDENCE_DATA = "residenceData";
	public static final String RESIDENCE_ID = "residenceId";
	public static final String PRIMARY_RESIDENCE = " Primary Residence";
	public static final String UPGRADE_MSG = "You have reached your maximum residences. Please purchase a residence upgrade to continue.";
	
	public enum ResidenceColumnName {
		RESIDENCE_ID, CREATE_DT, UPDATE_DT
	}

	public ResidenceAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public ResidenceAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		List<ResidenceVO> residenceList = retrieveResidences(req);

		if (req.hasParameter("homeInfo")) {
			// When adding a new residence, check to make sure the member has not reached their limit
			if (residenceList.isEmpty() && !canAddNewResidence(residenceList, req)) {
				sendRedirect(RezDoxUtils.SUBSCRIPTION_UPGRADE_PATH, UPGRADE_MSG, req);
				return;
			} else {
				// Set the data to be returned
				req.setAttribute(RESIDENCE_DATA, residenceList);
				putModuleData(retrieveHomeInfoForm(req));
			}
		} else {
			putModuleData(residenceList, residenceList.size(), false);
		}
	}
	
	/**
	 * Validates whether a new residence can be added by the member.
	 * 
	 * @param residenceList
	 * @param member
	 * @return true if residence can be added, false if not
	 * @throws ActionException 
	 */
	private boolean canAddNewResidence(List<ResidenceVO> residenceList, ActionRequest req) throws ActionException {
		SMTSession session = req.getSession();
		MemberVO member = (MemberVO) session.getAttribute(Constants.USER_DATA);
			
		// Validate whether the member needs a residence upgrade
		SubscriptionAction sa = (SubscriptionAction) ActionControllerFactoryImpl.loadAction(SubscriptionAction.class.getName(), this);
		boolean needsUpgrade = sa.checkUpgrade(member.getMemberId(), Group.HO);
		
		// Set default residence name per requirements
		if (!needsUpgrade) {
			ResidenceVO defaultResidence = new ResidenceVO();
			defaultResidence.setResidenceName(member.getLastName() + PRIMARY_RESIDENCE);
			residenceList.add(defaultResidence);
		}
		
		// A residence can be added if they don't need an upgrade
		return !needsUpgrade;
	}
	
	/**
	 * Retrieves base residence data
	 * 
	 * @param req
	 * @return
	 */
	protected List<ResidenceVO> retrieveResidences(ActionRequest req) {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String residenceId = req.getParameter(RESIDENCE_ID);
		
		// Show only residences that the member has access to
		SMTSession session = req.getSession();
		MemberVO member = (MemberVO) session.getAttribute(Constants.USER_DATA);
		
		// Using pivot table on the attributes to get additional data for display
		StringBuilder sql = new StringBuilder(900);
		sql.append("select r.residence_id, residence_nm, address_txt, address2_txt, city_nm, state_cd, zip_cd, country_cd, profile_pic_pth, coalesce(r.update_dt, r.create_dt) as update_dt, ");
		sql.append("beds_no, baths_no, coalesce(f_sqft_no, 0) + coalesce(uf_sqft_no, 0) as sqft_no, purchase_price_no ");
		sql.append("from ").append(schema).append("rezdox_residence r inner join ");
		sql.append(schema).append("rezdox_residence_member_xr m on r.residence_id = m.residence_id ");
		sql.append("left join (SELECT * FROM crosstab('SELECT residence_id, slug_txt, value_txt FROM ").append(schema).append("rezdox_residence_attribute ORDER BY 1', ");
		sql.append("'SELECT DISTINCT slug_txt FROM ").append(schema).append("rezdox_residence_attribute WHERE slug_txt in (''RESIDENCE_BEDS'',''RESIDENCE_BATHS'',''RESIDENCE_F_SQFT'',''RESIDENCE_UF_SQFT'', ''RESIDENCE_PURCHASE_PRICE'') ORDER BY 1') ");
		sql.append("AS (residence_id text, baths_no float, beds_no int, f_sqft_no int, purchase_price_no float, uf_sqft_no int) ");
		sql.append(") ra on r.residence_id = ra.residence_id ");
		sql.append("where member_id = ? ");
		
		List<Object> params = new ArrayList<>();
		params.add(member.getMemberId());
		
		// Return only a specific residence if selected
		if (!StringUtil.isEmpty(residenceId)) {
			sql.append("and r.residence_id = ? ");
			params.add(residenceId);
		}
		
		DBProcessor dbp = new DBProcessor(dbConn);
		return dbp.executeSelect(sql.toString(), params, new ResidenceVO());
	}
	
	/**
	 * Retrieves the Residence Home Information form & saved form data
	 * 
	 * @param req
	 */
	protected FormVO retrieveHomeInfoForm(ActionRequest req) {
		String formId = getFormId();
		log.debug("Retrieving Residence Form: " + formId);
		
		// Set the requried params
		GenericQueryVO query = new GenericQueryVO(formId);
		QueryParamVO param = new QueryParamVO(ResidenceColumnName.RESIDENCE_ID.name(), false);
		param.setValues(req.getParameterValues(RESIDENCE_ID));
		query.addConditional(param);
		
		// Get the form and the saved data for re-display onto the form.
		DataContainer dc = new DataManagerUtil(attributes, dbConn).loadFormWithData(formId, req, query, ResidenceFormProcessor.class);
		req.setAttribute(FormAction.FORM_DATA, dc);
		
		return dc.getForm();
	}
	
	/**
	 * Get's the form id associated to the action
	 * 
	 * @return
	 */
	private String getFormId() {
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		return (String) mod.getAttribute(ModuleVO.ATTRIBUTE_1);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		String formId = getFormId();

		// Place ActionInit on the Attributes map for the Data Save Handler.
		attributes.put(Constants.ACTION_DATA, actionInit);

		// Call DataManagerUtil to save the form.
		new DataManagerUtil(attributes, dbConn).saveForm(formId, req, ResidenceFormProcessor.class);
	}
}