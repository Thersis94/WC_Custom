package com.rezdox.data;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;

import com.rezdox.action.ResidenceAction;
import com.rezdox.vo.MemberVO;
import com.rezdox.vo.ResidenceVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.EnumUtil;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.FormDataTransaction;
import com.smt.sitebuilder.data.vo.FormFieldVO;
import com.smt.sitebuilder.data.vo.FormTransactionVO;
import com.smt.sitebuilder.data.vo.GenericQueryVO;

/****************************************************************************
 * <p><b>Title</b>: ResidenceFormTransaction</p>
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
public class ResidenceFormTransaction extends FormDataTransaction {
	
	/**
	 * Request parameter names for form field slug_txt values
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
	public ResidenceFormTransaction(SMTDBConnection conn, Map<String, Object> attributes, ActionRequest req) {
		super(conn, attributes, req);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.data.AbstractFormTransaction#writeTransaction(com.smt.sitebuilder.data.vo.FormTransactionVO)
	 */
	@Override
	protected FormTransactionVO writeTransaction(FormTransactionVO data) throws DatabaseException {
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
		
		// Save the Residence record
		ResidenceVO residence = new ResidenceVO(req);
		boolean newResidence = StringUtil.isEmpty(residence.getResidenceId());
		DBProcessor dbp = new DBProcessor(dbConn);
		try {
			dbp.save(residence);
			req.setParameter(ResidenceAction.RESIDENCE_ID, residence.getResidenceId());
		} catch(Exception e) {
			log.error("Could not save RezDox Residence");
		}
		
		// Save the Residence/Member XR
		saveResidenceMemberXR(newResidence);
		
		// Save the Residence attributes
		saveFieldData(data);
		
		return null;
	}
	
	/**
	 * Save the XR record between the residence and member
	 * 
	 * @param newResidence
	 * @throws DatabaseException
	 */
	protected void saveResidenceMemberXR(boolean newResidence) throws DatabaseException {
		// Record already exists if this isn't a new residence, don't add another
		if (!newResidence) return;
		
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.INSERT_CLAUSE).append(schema).append("rezdox_residence_member_xr (residence_member_xr_id, ");
		sql.append("member_id, residence_id, create_dt) ");
		sql.append("values (?,?,?,?)");
		log.debug(sql);
		
		// Get the member adding this residence
		SMTSession session = req.getSession();
		MemberVO member = (MemberVO) session.getAttribute(Constants.USER_DATA);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, new UUIDGenerator().getUUID());
			ps.setString(2, member.getMemberId());
			ps.setString(3, req.getParameter(ResidenceAction.RESIDENCE_ID));
			ps.setTimestamp(4, Convert.getCurrentTimestamp());
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
		
		for (FormFieldVO vo : data.getCustomData().values()) {
			deleteSavedResponses(vo);

			// Save valid responses.
			if (vo.getResponses() != null && !vo.getResponses().isEmpty()) {
				saveFieldData(vo);
			}
		}
	}

	/**
	 * Save the field data
	 * 
	 * @param vo
	 * @throws DatabaseException
	 */
	protected void saveFieldData(FormFieldVO vo) throws DatabaseException {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringEncoder sen = new StringEncoder();
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.INSERT_CLAUSE).append(schema).append("rezdox_residence_attribute (attribute_id, residence_id, ");
		sql.append("slug_txt, value_txt, create_dt) ");
		sql.append("values (?,?,?,?,?)");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (String val : vo.getResponses()) {
				ps.setString(1, new UUIDGenerator().getUUID());
				ps.setString(2, req.getParameter(ResidenceAction.RESIDENCE_ID));
				ps.setString(3, vo.getSlugTxt());
				ps.setString(4, sen.decodeValue(StringUtil.checkVal(val)));
				ps.setTimestamp(5, Convert.getCurrentTimestamp());
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException sqle) {
			log.error("Could not save RezDox Residence Form Field "	+ vo.getSlugTxt(), sqle);
			throw new DatabaseException(sqle);
		}
	}

	/**
	 * Delete old Residence form responses
	 * 
	 * @param vo
	 */
	protected void deleteSavedResponses(FormFieldVO vo) {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.DELETE_CLAUSE).append(" from ").append(schema).append("rezdox_residence_attribute ");
		sql.append("where residence_id = ? and slug_txt = ? ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, req.getParameter(ResidenceAction.RESIDENCE_ID));
			ps.setString(2, vo.getSlugTxt());
			ps.execute();
		} catch (SQLException sqle) {
			log.error("could not delete saved field responses", sqle);
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
