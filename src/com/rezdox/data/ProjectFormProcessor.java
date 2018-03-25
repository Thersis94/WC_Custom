package com.rezdox.data;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.rezdox.action.ProjectAction;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
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
 * <p><b>Title</b>: ProjectFormProcessor</p>
 * <p><b>Description:</b> Writes the project form data to the database - see data model.</p>
 * <p> 
 * <p>Copyright: (c) 2018 SMT, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Mar 23, 2018
 * <b>Changes:</b>
 ****************************************************************************/
public class ProjectFormProcessor extends FormDataProcessor {

	/**
	 * Define a mapping between the form-field slug (enum name) and the request parameter
	 * our VO would use to self-populate.
	 */
	protected enum CoreFormSlug {
		NAME("projectName"), DESCRIPTION("descriptionText"), COST("totalNo"),
		CATEGORY("projectCategoryCd"), TYPE("projectTypeCd"), LABOR("laborNo"), 
		RESIDENCE("residenceId"), ROOM("roomId"), END_DATE("endDate"),
		PROJECT_ID("projectId"), BUSINESS_ID("businessId");

		private String reqParam;
		private CoreFormSlug(String reqParam) { this.reqParam = reqParam; }
		public String getReqParam() { return reqParam; }
	}

	/**
	 * Slug names - used in ProjectVO but put here for alignment with form/others (above).
	 * These are the NON-CONNECTED Homeowner fields.
	 */
	public enum FormSlug {
		PROJECT_NAME, //residence name
		PROJECT_OWNER, //homeowner name
		PROJECT_EMAIL, //homeowner email 
		PROJECT_PHONE; //homeowner phone 
	}

	/**
	 * @param conn
	 * @param attributes
	 * @param req
	 */
	public ProjectFormProcessor(SMTDBConnection conn, Map<String, Object> attributes, ActionRequest req) {
		super(conn, attributes, req);
	}


	/* 
	 * This method saves the core Project fields in the PROJECT table.
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.data.FormDataProcessor#saveFormData(com.smt.sitebuilder.data.vo.FormTransactionVO)
	 */
	@Override
	protected void saveFormData(FormTransactionVO data) throws DatabaseException {
		log.info("saving Project form");

		// Set the form fields that should not be saved as attributes, onto the request, with appropriate parameter names.
		// Remove from the form field map so they aren't saved as attributes.
		Iterator<Map.Entry<String, FormFieldVO>> iter = data.getCustomData().entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, FormFieldVO> entry = iter.next();
			CoreFormSlug param = EnumUtil.safeValueOf(CoreFormSlug.class, entry.getValue().getSlugTxt());
			if (param != null) {
				req.setParameter(param.getReqParam(), entry.getValue().getResponseText());
				log.debug(String.format("%s=%s", param.getReqParam(), entry.getValue().getResponseText()));
				iter.remove();
			}
		}

		//transpose the primary key
		req.setParameter(CoreFormSlug.PROJECT_ID.getReqParam(), req.getParameter(REQ_SUBMITTAL_ID));

		// Save the Project
		ProjectAction ta = new ProjectAction(dbConn, attributes);
		try {
			ta.save(req);
		} catch (Exception e) {
			throw new DatabaseException("Could not save project", e);
		}
	}


	/*
	 *  This method saves the custom Project fields into the _attribute table.
	 *  Note: By the time execution has reached this point the core form fields have been removed from the FormTransactionVO.
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.data.FormDataTransaction#saveFieldData(com.smt.sitebuilder.data.vo.FormTransactionVO)
	 */
	@Override
	protected void saveFieldData(FormTransactionVO data) throws DatabaseException {
		log.debug("Saving RezDox Project Attributes");

		List<FormFieldVO> fields = new ArrayList<>(data.getCustomData().values().size());
		for (FormFieldVO vo : data.getCustomData().values()) {
			// Save valid responses
			if (vo.getResponses() != null && !vo.getResponses().isEmpty())
				fields.add(vo);
		}

		deleteSavedResponses();
		saveFieldData(fields);
	}


	/**
	 * Save the field/extended data (the project_attributes)
	 * @param vo
	 * @throws DatabaseException
	 */
	protected void saveFieldData(List<FormFieldVO> fields) throws DatabaseException {
		String projectId = req.getParameter(CoreFormSlug.PROJECT_ID.getReqParam());
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		UUIDGenerator uuid = new UUIDGenerator();
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.INSERT_CLAUSE).append(schema).append("rezdox_project_attribute ");
		sql.append("(attribute_id, project_id, slug_txt, value_txt, create_dt) values (?,?,?,?,?)");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (FormFieldVO formField : fields) {
				for (String val : formField.getResponses()) {
					ps.setString(1, uuid.getUUID());
					ps.setString(2, projectId);
					ps.setString(3, StringUtil.checkVal(formField.getSlugTxt(), formField.getFormFieldGroupId()));
					ps.setString(4, val);
					ps.setTimestamp(5, Convert.getCurrentTimestamp());
					ps.addBatch();
				}
			}
			ps.executeBatch();
		} catch (SQLException sqle) {
			throw new DatabaseException("could not save RezDox project attributes", sqle);
		}
	}


	/**
	 * delete saved project_attribute entries
	 */
	protected void deleteSavedResponses() {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		String sql = StringUtil.join("delete from ", schema, "rezdox_project_attribute where project_id=?");

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, req.getParameter(CoreFormSlug.PROJECT_ID.getReqParam()));
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("Could not delete RexDox project attributes", sqle);
		}
	}


	/* 
	 * Load records from the tresure_item_attribute table for the give filter query (which is projectId)
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.data.AbstractFormTransaction#loadTransactions(com.smt.sitebuilder.data.DataContainer)
	 */
	@Override
	protected DataContainer loadTransactions(DataContainer dc) throws DatabaseException {
		log.debug("loading RezDox project attributes");
		GenericQueryVO qry = dc.getQuery();
		loadTransaction(dc, qry);
		return dc;
	}


	/*
	 * Build the Load Transaction Sql Statement.
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.data.FormDataProcessor#getLoadSql(com.smt.sitebuilder.data.vo.GenericQueryVO)
	 */
	@Override
	protected String getLoadSql(GenericQueryVO qry) {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(250);
		sql.append("select attribute_id as form_data_id, form_field_id, form_field_group_id, pa.slug_txt, ");
		sql.append("project_id as form_submittal_id, value_txt, 0 as data_enc_flg, pa.create_dt, pa.update_dt ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_project_attribute pa ");
		sql.append(DBUtil.INNER_JOIN).append("form_field ff on (pa.slug_txt=ff.slug_txt or pa.slug_txt=ff.form_field_group_id) ");
		sql.append(DBUtil.INNER_JOIN).append("form_template ft on ff.form_template_id=ft.form_template_id ");
		sql.append("where ft.form_id=? and ").append(qry.getConditionals().get(0).getFieldId()).append("=? ");

		log.debug(sql + StringUtil.getToString(qry.getConditionals().get(0).getValues()));
		return sql.toString();
	}


	/*
	 * Populates the Prepared Statement with the required parameters for loading the attributes
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.data.FormDataProcessor#populateQueryParams(java.sql.PreparedStatement, com.smt.sitebuilder.data.vo.GenericQueryVO)
	 */
	@Override
	protected void populateQueryParams(PreparedStatement ps, GenericQueryVO qry) throws SQLException {
		ps.setString(1, qry.getFormId());
		ps.setString(2, qry.getConditionals().get(0).getValues()[0]);
	}


	/* 
	 * Delete the project, which will cascade into dependent tables.
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.data.AbstractFormTransaction#flushTransactions(com.smt.sitebuilder.data.DataContainer)
	 */
	@Override
	public void flushTransactions(DataContainer dc) {
		ProjectAction ta = new ProjectAction(dbConn, attributes);
		try {
			req.setParameter("isDelete", "1");
			ta.save(req);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
}