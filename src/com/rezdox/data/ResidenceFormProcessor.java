package com.rezdox.data;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.rezdox.action.ResidenceAction;
import com.rezdox.vo.ResidenceAttributeVO;
import com.rezdox.vo.ResidenceVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.EnumUtil;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.smt.sitebuilder.action.FileLoader;
import com.smt.sitebuilder.common.SiteVO;
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
		RESIDENCE_STATE("state"), RESIDENCE_ZIP("zipCode"), RESIDENCE_COUNTRY("country"), RESIDENCE_PRIVACY_FLAG("privacyFlag"),
		RESIDENCE_PROFILE_PIC_PATH("profilePicPath"), RESIDENCE_FOR_SALE_DATE("forSaleDate");

		private String reqParam;

		private ResidenceField(String reqParam) {
			this.reqParam = reqParam;
		}

		public String getReqParam() { return reqParam; }
	}

	/**
	 * DB field names, request param names for form field slug_txt values that are file uploads
	 */
	public enum ResidenceFile {
		RESIDENCE_PROFILE_IMAGE("profilePicPath", "profile_pic_pth");

		private String reqParam;
		private String dbField;

		private ResidenceFile(String reqParam, String dbField) {
			this.reqParam = reqParam;
			this.dbField = dbField;
		}

		public String getReqParam() { return reqParam; }
		public String getDbField() { return dbField; }
	}

	/**
	 * Slugs that are not updatable by the user, values should not be managed in the form processor
	 */
	public enum SkipSlug {
		RESIDENCE_ZESTIMATE, RESIDENCE_IMPROVEMENTS_VALUE, RESIDENCE_POTENTIAL_VALUE, RESIDENCE_WALK_SCORE,
		RESIDENCE_SUN_NUMBER, RESIDENCE_TRANSIT_SCORE, HOMEDETAILS, RESIDENCE_TOTAL_SQFT
	}

	/**
	 * Maps submitted form builder parameter names to those expected in the business table 
	 */
	private Map<String, GenericVO> fileMap;

	/**
	 * @param conn
	 * @param attributes
	 * @param req
	 */
	public ResidenceFormProcessor(SMTDBConnection conn, Map<String, Object> attributes, ActionRequest req) {
		super(conn, attributes, req);
		fileMap = new HashMap<>();
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

			// Add parameters to the request to be saved to the residence table
			ResidenceField param = EnumUtil.safeValueOf(ResidenceField.class, entry.getValue().getSlugTxt());
			if (param != null) {
				req.setParameter(param.getReqParam(), entry.getValue().getResponseText());
				iter.remove();
			}

			// Get form builder parameter names for files and map them to their residence table conterparts
			ResidenceFile fileParam = EnumUtil.safeValueOf(ResidenceFile.class, entry.getValue().getSlugTxt());
			if (fileParam != null) {
				fileMap.put(entry.getValue().getFieldNm(), new GenericVO(fileParam.getReqParam(), fileParam.getDbField()));
				iter.remove();
			}
		}

		// Save the Residence Data
		ResidenceAction ra = new ResidenceAction(dbConn, attributes);
		try {
			ra.saveResidence(req);

			trimSavedFields(req, data);

		} catch (Exception e) {
			throw new DatabaseException("Could not save residence", e);
		}
	}


	/**
	 * Check for fields Zillow may have provided that we don't want to erase.
	 * @param req
	 * @param data
	 */
	@SuppressWarnings("unchecked")
	private void trimSavedFields(ActionRequest req, FormTransactionVO data) {
		List<ResidenceAttributeVO> attrs = (List<ResidenceAttributeVO>) req.getAttribute("savedSlugs");
		if (attrs == null || attrs.isEmpty()) return;

		Set<String> savedFields = new HashSet<>();
		for (ResidenceAttributeVO vo : attrs)
			savedFields.add(vo.getSlugText());

		Iterator<Map.Entry<String, FormFieldVO>> iter = data.getCustomData().entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, FormFieldVO> entry = iter.next();
			//remove any form fields where Zillow gave us data and the user did not.  If the user gave data, favor & preserve their values
			if (savedFields.contains(entry.getValue().getSlugTxt()) && StringUtil.isEmpty(entry.getValue().getResponseText())) {
				iter.remove();
				log.debug(String.format("removed %s from form submission in favor of the Zillow data already saved", entry.getValue().getSlugTxt()));
			}
		}
	}


	/* 
	 * Saves the files to secure binary
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.data.AbstractDataProcessor#saveFiles(com.smt.sitebuilder.data.vo.FormTransactionVO)
	 */
	@Override
	protected void saveFiles(FormTransactionVO data) {
		String secBinaryPath = (String) attributes.get(com.siliconmtn.http.filter.fileupload.Constants.SECURE_PATH_TO_BINARY);
		String orgId = ((SiteVO) req.getAttribute(Constants.SITE_DATA)).getOrganizationId();

		// Root for the organization (RezDox)
		String orgRoot = secBinaryPath + (String) attributes.get("orgAlias") + orgId;

		// Root for this residence's files
		String rootResidencePath = "/residence/" + req.getParameter(ResidenceAction.RESIDENCE_ID) + "/";

		// Root upload path
		String rootUploadPath = orgRoot + rootResidencePath;

		// Store the files to the file system
		List<String> dbFields = new ArrayList<>();
		try {
			for(FilePartDataBean fpdb : req.getFiles()) {
				FileLoader fl = new FileLoader(attributes);
				fl.setData(fpdb.getFileData());
				fl.setFileName(fpdb.getFileName());
				fl.setPath(rootUploadPath);
				fl.writeFiles();
				fl.reorientFiles();

				GenericVO field = fileMap.get(fpdb.getKey());
				if (field != null) {
					req.setParameter((String) field.getKey(), rootResidencePath + fpdb.getFileName());
					dbFields.add((String) field.getValue());
				}
			}
		} catch (Exception e) {
			log.error("Could not write RezDox residence file", e);
		}

		// Store file data to the db
		if (!dbFields.isEmpty()) {
			saveFileInfo(dbFields);
		}
	}

	/**
	 * Save file data to the existing residence record
	 * 
	 * @param dbFields
	 */
	private void saveFileInfo(List<String> dbFields) {
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);

		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.UPDATE_CLAUSE).append(schema).append("rezdox_residence set ");

		int cnt = 0;
		for (String field : dbFields) {
			sql.append(cnt++ > 0 ? ", " : "").append(field).append(" = ? ");
		}

		sql.append("where residence_id = ? ");
		dbFields.add("residence_id");

		DBProcessor dbp = new DBProcessor(dbConn);
		try {
			dbp.executeSqlUpdate(sql.toString(), new ResidenceVO(req), dbFields);
		} catch (Exception e) {
			log.error("Couldn't save residence file data", e);
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
			SkipSlug skipSlug = EnumUtil.safeValueOf(SkipSlug.class, vo.getSlugTxt().toUpperCase());
			if (skipSlug != null) continue;

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
		//Not necessary.  Deleting a Residence record cascades into the _attributes table.
	}
}