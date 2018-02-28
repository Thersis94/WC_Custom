package com.rezdox.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.rezdox.vo.MemberVO;
import com.rezdox.vo.MembershipVO.Group;
import com.rezdox.vo.ResidenceVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.form.FormAction;
import com.smt.sitebuilder.action.form.FormActionVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.DataManagerFacade;
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
public class ResidenceAction extends FormAction {
	
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
		SubscriptionAction sa = new SubscriptionAction(getActionInit());
		sa.setAttributes(getAttributes());
		sa.setDBConnection(getDBConnection());
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
		sql.append("select r.residence_id, residence_nm, address_txt, address2_txt, city_nm, state_cd, zip_cd, profile_pic_pth, coalesce(r.update_dt, r.create_dt) as update_dt, ");
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
	protected FormActionVO retrieveHomeInfoForm(ActionRequest req) {
		String actionId = this.actionInit.getActionId();
		FormActionVO fa = loadFormActionVO(actionId);
		
		if (fa == null) {
			log.debug("Could not get RezDox Residence Form Action for actionId: " + actionId);
			return fa;
		}

		log.debug("Retrieving Form tied to actionId: " + actionId);

		DataManagerFacade dmf = new DataManagerFacade(attributes, dbConn, req);
		DataContainer dc = dmf.loadForm(fa.getFormId());
		if (dc.hasErrors()) {
			for (Entry<String, Throwable> e : dc.getErrors().entrySet()) {
				log.error("Error retrieving form: ", e.getValue());
			}
		}
		
		dc.getForm().setCurrentTemplateNo(1);
		fa.setDataContainer(dc);

		// Get saved data and return for re-display on the form.
		GenericQueryVO query = new GenericQueryVO(fa.getFormId());
		QueryParamVO param = new QueryParamVO(ResidenceColumnName.RESIDENCE_ID.name(), false);
		param.setValues(req.getParameterValues(RESIDENCE_ID));
		query.addConditional(param);
		dc.setQuery(query);
		dmf.loadTransactions(dc);
		req.setAttribute(FormAction.FORM_DATA, dc);
		
		return fa;
	}

	/**
	 * Gets a residence form's action record
	 * 
	 * @param actionId
	 * @return
	 * @throws ActionException
	 */
	@Override
	protected FormActionVO loadFormActionVO(String actionId) {
		FormActionVO vo = null;

		StringBuilder sql = new StringBuilder(60);
		sql.append("select action_id, organization_id, module_type_id, action_nm, action_desc, attrib1_txt as form_id, pending_sync_flg, ");
		sql.append("action_group_id, create_dt, update_dt, create_by_id, update_by_id from sb_action where action_id = ? ");

		log.info("Residence Form Action SQL: " + sql.toString() + " | " + actionId);

		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, actionId);
			ResultSet rs = ps.executeQuery();

			if(rs.next()) {
				vo = new FormActionVO(rs);
				vo.setAttribute(SBModuleVO.ATTRIBUTE_1, rs.getString("form_id"));
			}
		} catch (SQLException e) {
			log.error("Could not retrieve residence form action record. ", e);
		}

		return vo;
	}
	
	/**
	 * Manages updating the Residence Form Action record.
	 * 
	 * @param req
	 * @throws SQLException
	 */
	@Override
	protected void updateFormRecord(ActionRequest req, String sbActionId, ModuleVO mod) throws SQLException {
		// No form record to update, only using the sb_action record for the RezDox Residence Form Action
	}
	
	/**
	 * Stores images from the form
	 *
	 * @param req
	 * @return
	 */
	@Override
	protected void saveFiles(ActionRequest req) {
		// TODO: Handle file saves differently than super class
	}
}