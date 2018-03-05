package com.rezdox.data;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.rezdox.action.ResidenceAction;
import com.rezdox.vo.MemberVO;
import com.rezdox.vo.ResidenceVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.EnumUtil;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.user.LocationManager;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.FormDataProcessor;
import com.smt.sitebuilder.data.vo.FormFieldVO;
import com.smt.sitebuilder.data.vo.FormTransactionVO;
import com.smt.sitebuilder.data.vo.GenericQueryVO;

/****************************************************************************
 * <p><b>Title</b>: ResidenceFormProcessor</p>
 * <p><b>Description: </b>Writes the residence form data to the
 * residence_attributes table.</p>
 * <p> 
 * <p>Copyright: (c) 2018 SMT, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author Tim Johnson
 * @version 1.0
 * @since Feb 9, 2018
 * <b>Changes:</b>
 ****************************************************************************/
public class ResidenceFormProcessor extends FormDataProcessor {
	
	/**
	 * Request parameter names for form field slug_txt values that go to the residence table, not the attributes table
	 */
	public enum ResidenceField {
		RESIDENCE_NAME("residenceName"), RESIDENCE_ADDRESS("address"), RESIDENCE_ADDRESS_2("address2"), RESIDENCE_CITY("city"),
		RESIDENCE_STATE("state"), RESIDENCE_ZIP("zipCode"), RESIDENCE_COUNTRY("country"), RESIDENCE_PRIVACY_FLAG("privacyFlag");

		private String reqParam;
		
		private ResidenceField(String reqParam) {
			this.reqParam = reqParam;
		}
		
		public String getReqParam() { return reqParam; }
	}

	/**
	 * @param conn
	 * @param attributes
	 * @param req
	 */
	public ResidenceFormProcessor(SMTDBConnection conn, Map<String, Object> attributes, ActionRequest req) {
		super(conn, attributes, req);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.data.FormDataProcessor#saveFormData(com.smt.sitebuilder.data.vo.FormTransactionVO)
	 */
	@Override
	protected void saveFormData(FormTransactionVO data) throws DatabaseException {
		log.debug("Saving RezDox Residence Form Transaction Data");
		
		// Set the form fields that should not be saved as attributes, onto the request, with appropriate parameter names.
		// Remove from the form field map so they aren't saved as attributes.
		Iterator<Map.Entry<String, FormFieldVO>> iter = data.getCustomData().entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, FormFieldVO> entry = iter.next();
			ResidenceField param = EnumUtil.safeValueOf(ResidenceField.class, entry.getValue().getSlugTxt());
			if (param != null) {
				req.setParameter(param.getReqParam(), entry.getValue().getResponseText());
				iter.remove();
			}
		}
		
		// Get the residence data
		ResidenceVO residence = new ResidenceVO(req);
		boolean newResidence = StringUtil.isEmpty(residence.getResidenceId());
		
		// Geocode the residence address
		LocationManager lm = new LocationManager(residence);
		GeocodeLocation gl = lm.geocode(attributes);
		residence.setLatitude(gl.getLatitude());
		residence.setLongitude(gl.getLongitude());
		
		// Save the residence record
		DBProcessor dbp = new DBProcessor(dbConn);
		try {
			dbp.save(residence);
			req.setParameter(ResidenceAction.RESIDENCE_ID, residence.getResidenceId());
		} catch(Exception e) {
			log.error("Could not save RezDox Residence");
		}
		
		// Save the Residence/Member XR
		saveResidenceMemberXR(newResidence);
	}
	
	/**
	 * Save the XR record between the residence and member
	 * 
	 * @param newResidence
	 * @throws DatabaseException
	 */
	protected void saveResidenceMemberXR(boolean newResidence) throws DatabaseException {
		// Record already exists if this isn't a new residence, don't need another here
		if (!newResidence) return;
		
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.INSERT_CLAUSE).append(schema).append("rezdox_residence_member_xr (residence_member_xr_id, ");
		sql.append("member_id, residence_id, status_flg, create_dt) ");
		sql.append("values (?,?,?,?,?)");
		log.debug(sql);
		
		// Get the member adding this residence
		SMTSession session = req.getSession();
		MemberVO member = (MemberVO) session.getAttribute(Constants.USER_DATA);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, new UUIDGenerator().getUUID());
			ps.setString(2, member.getMemberId());
			ps.setString(3, req.getParameter(ResidenceAction.RESIDENCE_ID));
			ps.setInt(4, 1); // Newly added residences are always active
			ps.setTimestamp(5, Convert.getCurrentTimestamp());
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("Could not save RezDox Member/Residence XR ", sqle);
			throw new DatabaseException(sqle);
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.data.FormDataTransaction#saveFieldData(com.smt.sitebuilder.data.vo.FormTransactionVO)
	 */
	@Override
	protected void saveFieldData(FormTransactionVO data) throws DatabaseException {
		log.debug("Saving RezDox Residence Attributes");
		
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
		sql.append(DBUtil.INSERT_CLAUSE).append(schema).append("rezdox_residence_attribute (attribute_id, residence_id, ");
		sql.append("slug_txt, value_txt, create_dt) ");
		sql.append("values (?,?,?,?,?)");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (FormFieldVO formField : newFormFields) {
				for (String val : formField.getResponses()) {
					ps.setString(1, new UUIDGenerator().getUUID());
					ps.setString(2, req.getParameter(ResidenceAction.RESIDENCE_ID));
					ps.setString(3, formField.getSlugTxt());
					ps.setString(4, sen.decodeValue(StringUtil.checkVal(val)));
					ps.setTimestamp(5, Convert.getCurrentTimestamp());
					ps.addBatch();
				}
			}
			ps.executeBatch();
		} catch (SQLException sqle) {
			log.error("Could not save RezDox Residence form fields.", sqle);
			throw new DatabaseException(sqle);
		}
	}

	/**
	 * Delete old Residence form responses
	 * 
	 * @param vo
	 */
	protected void deleteSavedResponses(List<FormFieldVO> oldFormFields) {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.DELETE_CLAUSE).append(" from ").append(schema).append("rezdox_residence_attribute ");
		sql.append("where residence_id = ? and slug_txt = ? ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (FormFieldVO formField : oldFormFields) {
				ps.setString(1, req.getParameter(ResidenceAction.RESIDENCE_ID));
				ps.setString(2, formField.getSlugTxt());
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException sqle) {
			log.error("Could not delete RexDox Residence form field data.", sqle);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.data.AbstractFormTransaction#loadTransactions(com.smt.sitebuilder.data.DataContainer)
	 */
	@Override
	protected DataContainer loadTransactions(DataContainer dc) throws DatabaseException {
		log.debug("Loading RezDox Residence Form Transaction Data");
		
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
		sql.append("select attribute_id as form_data_id, form_field_id, form_field_group_id, ra.slug_txt, ");
		sql.append("residence_id as form_submittal_id, value_txt, 0 as data_enc_flg, ra.create_dt, ra.update_dt ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_residence_attribute ra ");
		sql.append(DBUtil.INNER_JOIN).append("form_field ff on ra.slug_txt = ff.slug_txt ");
		sql.append(DBUtil.WHERE_CLAUSE).append("residence_id = ? ");

		log.debug("Residence Attribte SQL: " + sql.toString() + "|" + qry.getConditionals().get(0).getFieldId() + "=" + qry.getConditionals().get(0).getValues()[0]);

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
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.data.AbstractFormTransaction#flushTransactions(com.smt.sitebuilder.data.DataContainer)
	 */
	@Override
	protected void flushTransactions(DataContainer dc) {
		//TODO: Find out from Mike if users can delete a Residence
	}

}
