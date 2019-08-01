package com.rezdox.data;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import com.rezdox.action.BusinessAction;
import com.rezdox.action.PhotoAction;
import com.rezdox.vo.BusinessVO;
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
		BUSINESS_STATE("state"), BUSINESS_ZIP("zipCode"), BUSINESS_COUNTRY("country"), BUSINESS_PHONE_1("mainPhoneText"),
		BUSINESS_PHONE_2("altPhoneText"), BUSINESS_EMAIL_1("emailAddressText"), BUSINESS_WEBSITE("websiteUrl"),
		BUSINESS_PRIVACY_FLAG("privacyFlag"), BUSINESS_CATEGORY("categoryCd"), BUSINESS_SUB_CATEGORY("subCategoryCd"),

		BUSINESS_AD_IMAGE("adFileUrl", "ad_file_url"), BUSINESS_LOGO("photoUrl", "photo_url"),
		//These two are hidden fields  that get transposed at the browser (into the two above).  
		//We list them here so they won't be saved into the _attributes table as duplicate data
		BUSINESS_AD_FILE_URL("adFileUrl"),BUSINESS_PHOTO_URL("photoUrl");

		private String reqParam;
		private String dbColNm;

		private BusinessField(String reqParam) { this.reqParam = reqParam; }
		private BusinessField(String reqParam, String dbCol) { this.reqParam = reqParam; this.dbColNm = dbCol; }
		public String getReqParam() { return reqParam; }
		public String getDBColNm() { return dbColNm; }
		public boolean isFileUpload() { return !StringUtil.isEmpty(dbColNm); }
	}

	/**
	 * Special use keys for values from the attributes table in the attibutes map
	 * Used in BusinessVO
	 */
	public static final String SLUG_BUSINESS_SUMMARY = "BUSINESS_SUMMARY";

	/**
	 * Maps submitted form builder parameter names to those expected in the business table 
	 */
	private Map<String, GenericVO> fileMap;

	/**
	 * @param conn
	 * @param attributes
	 * @param req
	 */
	public BusinessFormProcessor(SMTDBConnection conn, Map<String, Object> attributes, ActionRequest req) {
		super(conn, attributes, req);
		fileMap = new HashMap<>();
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
			FormFieldVO field = entry.getValue();

			// Add parameters to the request to be saved to the business table
			BusinessField param = EnumUtil.safeValueOf(BusinessField.class, field.getSlugTxt());
			if (param != null) {
				if (param.isFileUpload()) {
					//save special files separately, so we can upload them and put the file name into the DB after saving.
					fileMap.put(field.getFieldNm(), new GenericVO(param.getReqParam(), param.getDBColNm()));
				} else {
					req.setParameter(param.getReqParam(), field.getResponseText());
				}
				iter.remove();
			}

			//earmark the BUSINESS_YOUTUBEAD form field, we need it downstream in BusinessAction to evaluate notifications
			if ("BUSINESS_YOUTUBEAD".equals(field.getSlugTxt())) {
				req.setAttribute("newYoutubeAdUrl", field.getResponseText());
			} else if (BusinessField.BUSINESS_AD_IMAGE.toString().equals(field.getSlugTxt())) {
				req.setAttribute("adFileUploaded", Boolean.toString(req.getFile(field.getFieldNm()) != null));
			}
		}

		// Save the Business Data
		try {
			new BusinessAction(dbConn, attributes).saveBusiness(req);
		} catch (Exception e) {
			throw new DatabaseException("Could not save business", e);
		}
	}


	/**
	 * Saves the files to secure binary
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.data.AbstractDataProcessor#saveFiles(com.smt.sitebuilder.data.vo.FormTransactionVO)
	 */
	@Override
	protected void saveFiles(FormTransactionVO data) {
		if (req.getFiles() == null || req.getFiles().isEmpty()) return;

		String secBinaryPath = (String) attributes.get(com.siliconmtn.http.filter.fileupload.Constants.SECURE_PATH_TO_BINARY);
		String orgId = ((SiteVO) req.getAttribute(Constants.SITE_DATA)).getOrganizationId();
		String orgRoot = secBinaryPath + (String) attributes.get("orgAlias") + orgId;
		String rootBusinessPath = "/business/" + req.getParameter(BusinessAction.REQ_BUSINESS_ID) + "/";
		String uploadPath = orgRoot + rootBusinessPath;

		// Store the files to the file system
		List<String> dbFields = new ArrayList<>();
		FileLoader fl = new FileLoader(attributes);
		try {
			ListIterator<FilePartDataBean> iter = req.getFiles().listIterator();
			while (iter.hasNext()) {
				FilePartDataBean fpdb = iter.next();
				GenericVO field = fileMap.get(fpdb.getKey());
				if (field == null) continue;

				fl.setData(fpdb.getFileData());
				fl.setFileName(fpdb.getFileName());
				fl.setPath(uploadPath);
				fl.writeFiles();
				fl.reorientFiles();
				req.setParameter((String) field.getKey(), rootBusinessPath + fpdb.getFileName());
				dbFields.add((String) field.getValue());
				log.debug("processed " + fpdb.getKey() + " into " + field);
				iter.remove();
			}
		} catch (Exception e) {
			log.error("Could not write RezDox business files", e);
		}

		// Store file data to the db
		saveFileInfo(dbFields);

		//Write any remaining files to the Photo System. (Ad Images)
		if (!req.getFiles().isEmpty()) {
			req.setParameter("descriptionText", BusinessVO.AD_FILE_KEY);
			req.setAttribute(PhotoAction.UPLOAD_PATH, uploadPath);
			req.setAttribute(PhotoAction.URL_ROOT, rootBusinessPath);
			new PhotoAction(dbConn, attributes).saveFiles(req);
		}
	}


	/**
	 * Save file data to the existing business record
	 * @param dbFields
	 */
	private void saveFileInfo(List<String> dbFields) {
		if (dbFields == null || dbFields.isEmpty()) return;
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.UPDATE_CLAUSE).append(schema).append("rezdox_business set ");

		for (int x=0; x < dbFields.size(); x++) {
			if (x > 0) sql.append(", ");
			sql.append(dbFields.get(x)).append("=?");
		}
		sql.append(" where business_id=?");
		dbFields.add("business_id");

		DBProcessor dbp = new DBProcessor(dbConn, schema);
		try {
			dbp.executeSqlUpdate(sql.toString(), new BusinessVO(req), dbFields);
		} catch (Exception e) {
			log.error("Couldn't save business file data", e);
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
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (FormFieldVO formField : oldFormFields) {
				ps.setString(1, req.getParameter(BusinessAction.REQ_BUSINESS_ID));
				ps.setString(2, formField.getSlugTxt());
				ps.addBatch();
			}
			int[] cnt = ps.executeBatch();
			log.debug("deleted " + cnt.length + " saved responses");

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
