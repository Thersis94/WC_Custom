package com.depuysynthes.ifu;

// Java 8
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
// WebCrescendo
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <b>Title: </b>IFULanguageAction.java
 <b>Project: </b>
 <b>Description: </b>
 <b>Copyright: </b>(c) 2000 - 2018 SMT, All Rights Reserved
 <b>Company: Silicon Mountain Technologies</b>
 @author cobalt
 @version 1.0
 @since Oct 8, 2018
 <b>Changes:</b> 
 ***************************************************************************/
public class IFULanguageAction extends SBActionAdapter {

	public static final String PARAM_LANGUAGE = "language";
	private final String fieldPrefix = "FIELD-";
	private final int maxFieldsNo = 13;
	
	/**
	* Constructor
	*/
	public IFULanguageAction() {
		super();
	}

	/**
	* Constructor
	*/
	public IFULanguageAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		// delete all XR records based on language type
		try (PreparedStatement ps = dbConn.prepareStatement(formatDeleteSql())) {
			ps.setString(1, req.getParameter(PARAM_LANGUAGE));
			ps.executeUpdate();
		} catch (SQLException sqle ) {
			log.error("Error deleting language mapping, ", sqle);
			throw new ActionException (sqle.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		// not implemented
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		List<IFULanguageVO> data = new ArrayList<>();

		// if this is an 'add new language' operation,  return.
		if (isAddLanguage(req))
			return;
		
		// retrieve a Map of field values for the given languate type
		String errMsg = null;
		boolean isError = false;
		try {
			data = retrieveData(req);
		} catch (Exception e) {
			data = new ArrayList<>();
			errMsg = e.getMessage();
			isError = true;
		}
		putModuleData(data, data == null ? 0 : data.size(), true, errMsg, isError);
	}

	/**
	 * Checks to see if the request is for the 'edit' page but no language has specified.  If so
	 * this is an 'add language' operation and we return an empty VO for use by the JSTL view.
	 * @param req
	 * @return
	 */
	private boolean isAddLanguage(ActionRequest req) {
		if ("edit".equals(req.getParameter("page")) &&
				! req.hasParameter(PARAM_LANGUAGE)) {
			putModuleData(new ArrayList<IFULanguageVO>());
			return true;
		}
		return false;
	}
		
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		List<IFULanguageVO> data = null;
		String errMsg = null;
		boolean isError = false;
		try {
			data = retrieveData(req);
		} catch (Exception e) {
			data = new ArrayList<>();
			errMsg = e.getMessage();
			isError = true;
		}
		putModuleData(data, data == null ? 0 : data.size(), false, errMsg, isError);
	}
	

	/**
	 * Retrieves language mapping data
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	public List<IFULanguageVO> retrieveData(ActionRequest req) 
			throws ActionException {
		// retrieve a Map of field values for the given languate type
		
		IFULanguageVO langVo = new IFULanguageVO();
		List<IFULanguageVO> languages = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(formatRetrieveSql(req.hasParameter(PARAM_LANGUAGE)))) {
			
			if (req.hasParameter(PARAM_LANGUAGE))
				ps.setString(1, req.getParameter(PARAM_LANGUAGE));
			
			ResultSet rs = ps.executeQuery();
			String prevLang = null;
			String currLang = null;
			
			while (rs.next()) {
				currLang = rs.getString("ifu_language_field_parent_cd");
				
				if (! currLang.equals(prevLang)) {
					// changed langs
					if (prevLang != null) {
						// if not first record, add the language VO to the languages Map
						languages.add(langVo);
					}
					// initialize the language bean
					langVo = new IFULanguageVO();
					langVo.setLanguage(currLang);
					langVo.setLanguageName(rs.getString("language_nm"));
				}
				
				langVo.addField(rs.getString("ifu_language_field_id"), rs.getString("value_txt"));
				prevLang = currLang;
			}
			
			// pick up the last map
			if (! langVo.getFieldMap().isEmpty()) {
				languages.add(langVo);
			}
			
		} catch (Exception e) {
			log.error("Error retrieving language map, ", e);
			throw new ActionException(e.getMessage());
		}
		
		return languages;
	}
	
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
		String errMsg = null;
		String lang = req.getParameter(PARAM_LANGUAGE);

		try (PreparedStatement ps = dbConn.prepareStatement(formatUpdateSql())) {
			dbConn.setAutoCommit(false);

			// delete existing mapping.
			this.delete(req);

			// write new mapping
			String fieldParam = null;
			int idx = 0;
			for (int i = 1; i <= maxFieldsNo; i++) {
				fieldParam = fieldPrefix + i;
				ps.setString(++idx, lang);
				ps.setString(++idx, fieldParam);
				ps.setString(++idx,req.getParameter(fieldParam));
				ps.setTimestamp(++idx, Convert.getCurrentTimestamp());
				ps.addBatch();
				idx = 0;
			}

			ps.executeBatch();
			dbConn.commit();

		} catch (Exception e) {
			errMsg = "Error updating language mapping.";
			log.error(errMsg, e);
			
			// rollback transaction
			try {
				dbConn.rollback();
			} catch (Exception e1) {
				log.error("Error rolling back IFU language update, ", e1);
			}

		} finally {
			// clean-up
			try {
				dbConn.setAutoCommit(true);
			} catch (Exception e2) {
				log.error("Error resetting db connection autocommit, ", e2);
			}
		}
	}
	
	/**
	 * Formats the list/retrieve SQL statement
	 * @param req
	 * @return
	 */
	private String formatRetrieveSql(boolean hasLanguage) {
		String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(300);
		sql.append("select lang.language_nm, ifx.ifu_language_field_parent_cd, ifx.ifu_language_field_id, ");
		sql.append("ifx.value_txt from ").append(customDb).append("depuy_ifu_language_field ifu ");
		sql.append("inner join ").append(customDb).append("depuy_ifu_language_field_xr ifx ");
		sql.append("on ifu.ifu_language_field_id = ifx.ifu_language_field_id ");
		sql.append("left outer join language lang on ifx.ifu_language_field_parent_cd = lang.language_cd ");
		
		if (hasLanguage) 
			sql.append("where lower(ifx.ifu_language_field_parent_cd) = lower(?) ");

		sql.append("order by ifx.ifu_language_field_parent_cd");
		log.debug("retrieve/list SQL: " + sql);
		return sql.toString();
	}
	
	/**
	 * Formats the build SQL statement
	 * @param req
	 * @return
	 */
	private String formatUpdateSql() {
		String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(200);
		sql.append("insert into ").append(customDb).append("depuy_ifu_language_field_xr ");
		sql.append("(ifu_language_field_parent_cd, ifu_language_field_id, value_txt, create_dt) values (?,?,?,?)");
		log.debug("update SQL: " + sql);
		return sql.toString();
	}
	
	/**
	 * Formats the 'delete' SQL statement
	 * @return
	 */
	private String formatDeleteSql() {
		String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA); 
		StringBuilder sql = new StringBuilder(100);
		sql.append("delete from ").append(customDb).append("depuy_ifu_language_field_xr ");
		sql.append("where ifu_language_field_parent_cd = ?");
		log.debug("Delete SQL: " + sql);
		return sql.toString();
	}

}
