package com.rezdox.data;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.rezdox.action.BusinessAction;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.EnumUtil;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.FormDataProcessor;
import com.smt.sitebuilder.data.vo.FormFieldVO;
import com.smt.sitebuilder.data.vo.FormTransactionVO;
import com.smt.sitebuilder.data.vo.GenericQueryVO;

/****************************************************************************
 * <p><b>Title</b>: BusinessFormProcessor</p>
 * <p><b>Description: </b>Writes the business form data to the
 * business_attributes table.</p>
 * <p> 
 * <p>Copyright: (c) 2018 SMT, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author Tim Johnson
 * @version 1.0
 * @since Mar 8, 2018
 * <b>Changes:</b>
 ****************************************************************************/
public class BusinessFormProcessor extends FormDataProcessor {
	
	/**
	 * Request parameter names for form field slug_txt values that go to the business table, not the attributes table
	 */
	public enum BusinessField {
		BUSINESS_NAME("businessName"), BUSINESS_ADDRESS("address"), BUSINESS_ADDRESS_2("address2"), BUSINESS_CITY("city"),
		BUSINESS_STATE("state"), BUSINESS_ZIP("zipCode"), BUSINESS_COUNTRY("country"), BUSINESS_MAIN_PHONE("mainPhoneText"),
		BUSINESS_ALT_PHONE("altPhoneText"), BUSINESS_EMAIL("emailAddressText"), BUSINESS_WEBSITE("websiteUrl"),
		BUSINESS_PRIVACY_FLAG("privacyFlag");

		private String reqParam;
		
		private BusinessField(String reqParam) {
			this.reqParam = reqParam;
		}
		
		public String getReqParam() { return reqParam; }
	}

	/**
	 * @param conn
	 * @param attributes
	 * @param req
	 */
	public BusinessFormProcessor(SMTDBConnection conn, Map<String, Object> attributes, ActionRequest req) {
		super(conn, attributes, req);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.data.FormDataProcessor#saveFormData(com.smt.sitebuilder.data.vo.FormTransactionVO)
	 */
	@Override
	protected void saveFormData(FormTransactionVO data) throws DatabaseException {
		log.debug("Saving RezDox Business Form Transaction Data");
		
		// Set the form fields that should not be saved as attributes, onto the request, with appropriate parameter names.
		// Remove from the form field map so they aren't saved as attributes.
		Iterator<Map.Entry<String, FormFieldVO>> iter = data.getCustomData().entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, FormFieldVO> entry = iter.next();
			BusinessField param = EnumUtil.safeValueOf(BusinessField.class, entry.getValue().getSlugTxt());
			if (param != null) {
				req.setParameter(param.getReqParam(), entry.getValue().getResponseText());
				iter.remove();
			}
		}
		
		// Save the Business Data
		BusinessAction ba = new BusinessAction(dbConn, attributes);
		try {
			ba.saveBusiness(req);
		} catch (Exception e) {
			throw new DatabaseException("Could not save business", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.data.FormDataTransaction#saveFieldData(com.smt.sitebuilder.data.vo.FormTransactionVO)
	 */
	@Override
	protected void saveFieldData(FormTransactionVO data) throws DatabaseException {
		log.debug("Saving RezDox Business Attributes");
		
		List<FormFieldVO> oldFormFields = new ArrayList<>();
		List<FormFieldVO> newFormFields = new ArrayList<>();
		
		for (FormFieldVO vo : data.getCustomData().values()) {
			oldFormFields.add(vo);

			// Save valid responses.
			if (vo.getResponses() != null && !vo.getResponses().isEmpty() && !StringUtil.isEmpty(vo.getSlugTxt())) {
				newFormFields.add(vo);
			}
		}
		
		deleteSavedResponses(oldFormFields);
		saveFieldData(newFormFields);
	}

	/**
	 * Save the field data
	 * 
	 * @param vo
	 * @throws DatabaseException
	 */
	protected void saveFieldData(List<FormFieldVO> newFormFields) throws DatabaseException {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringEncoder sen = new StringEncoder();
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.INSERT_CLAUSE).append(schema).append("rezdox_business_attribute (attribute_id, business_id, ");
		sql.append("slug_txt, value_txt, create_dt) ");
		sql.append("values (?,?,?,?,?)");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (FormFieldVO formField : newFormFields) {
				for (String val : formField.getResponses()) {
					ps.setString(1, new UUIDGenerator().getUUID());
					ps.setString(2, req.getParameter(BusinessAction.REQ_BUSINESS_ID));
					ps.setString(3, formField.getSlugTxt());
					ps.setString(4, sen.decodeValue(StringUtil.checkVal(val)));
					ps.setTimestamp(5, Convert.getCurrentTimestamp());
					ps.addBatch();
				}
			}
			ps.executeBatch();
		} catch (SQLException sqle) {
			log.error("Could not save RezDox Business form fields.", sqle);
			throw new DatabaseException(sqle);
		}
	}

	/**
	 * Delete old Business form responses
	 * 
	 * @param vo
	 */
	protected void deleteSavedResponses(List<FormFieldVO> oldFormFields) {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.DELETE_CLAUSE).append(" from ").append(schema).append("rezdox_business_attribute ");
		sql.append("where business_id = ? and slug_txt = ? ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (FormFieldVO formField : oldFormFields) {
				ps.setString(1, req.getParameter(BusinessAction.REQ_BUSINESS_ID));
				ps.setString(2, formField.getSlugTxt());
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException sqle) {
			log.error("Could not delete RexDox Business form field data.", sqle);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.data.AbstractFormTransaction#loadTransactions(com.smt.sitebuilder.data.DataContainer)
	 */
	@Override
	protected DataContainer loadTransactions(DataContainer dc) throws DatabaseException {
		log.debug("Loading RezDox Business Form Transaction Data");
		
		GenericQueryVO qry = dc.getQuery();
		loadTransaction(dc, qry);
		
		return dc;
	}

	/**
	 * Build the Load Transaction Sql Statement.
	 * 
	 * @param qry
	 * @return
	 */
	@Override
	protected String getLoadSql(GenericQueryVO qry) {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		
		StringBuilder sql = new StringBuilder();
		sql.append("select attribute_id as form_data_id, form_field_id, form_field_group_id, ba.slug_txt, ");
		sql.append("business_id as form_submittal_id, value_txt, 0 as data_enc_flg, ba.create_dt, ba.update_dt ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_business_attribute ba ");
		sql.append(DBUtil.INNER_JOIN).append("form_field ff on ba.slug_txt = ff.slug_txt ");
		sql.append(DBUtil.WHERE_CLAUSE).append("business_id = ? ");

		log.debug("Business Attribte SQL: " + sql.toString() + "|" + qry.getConditionals().get(0).getFieldId() + "=" + qry.getConditionals().get(0).getValues()[0]);

		return sql.toString();
	}

	/**
	 * Populates the Prepared Statement with the required parameters for loading the attributes
	 *
	 * @param ps
	 * @param qry
	 * @throws SQLException
	 */
	@Override
	protected void populateQueryParams(PreparedStatement ps, GenericQueryVO qry) throws SQLException {
		ps.setString(1, qry.getConditionals().get(0).getValues()[0]);
	}
}
