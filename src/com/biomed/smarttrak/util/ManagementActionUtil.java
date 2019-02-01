package com.biomed.smarttrak.util;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import com.biomed.smarttrak.action.AdminControllerAction;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> ManagementActionUtil.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Centralized Management Utility for managing Product, Company
 * and Market Data.  Tools are becoming increasingly homogenized and duplicated code
 * should be generalized and moved here.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Oct 5, 2018
 ****************************************************************************/
public class ManagementActionUtil {
	private SMTDBConnection dbConn;
	private Map<String, Object> attributes;
	public ManagementActionUtil(SMTDBConnection dbConn, Map<String, Object> attributes) {
		this.dbConn = dbConn;
		this.attributes = attributes;
	}

	private static Logger log = Logger.getLogger(ManagementActionUtil.class);

	/**
	 * Handle Bulk update of link Status and AttributeId.
	 * @param dbConn - Database connection
	 * @param schema - Database Schema holding Biomed Tables.
	 * @param req - ActionRequest
	 * @throws ActionException 
	 */
	public void bulkUpdateAttributeLinks(ActionRequest req) throws ActionException {
		String[] attributeIds = req.getParameterValues("attributeIds");
		String statusNo = req.getParameter("statusNo");
		String attributeId = req.getParameter("attributeId");
		String actionType = req.getParameter(AdminControllerAction.ACTION_TYPE);
		String schema = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);

		boolean hasStatus = !StringUtil.isEmpty(statusNo);
		boolean hasAttributeId = !StringUtil.isEmpty(attributeId);

		if(dbConn == null || ArrayUtils.isEmpty(attributeIds) || (!hasStatus && !hasAttributeId)) {
			throw new ActionException("Missing Necessary Params to continue.");
		}

		String sql = bulkUpdateLinksSql(attributeIds.length, hasStatus, hasAttributeId, actionType, schema);
		try(PreparedStatement ps = dbConn.prepareStatement(sql)) {
			int i = 1;
			if(hasStatus) {
				ps.setString(i++, statusNo);
			}
			if(hasAttributeId) {
				ps.setString(i++, attributeId);
			}
			for(String caId : attributeIds) {
				ps.setString(i++, caId);
			}
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error("Error Processing Code", e);
		}
	}

	/**
	 * Build Bulk Update Query for Links.  Customizes based on presence of
	 * status, attribute, number of attributes and type of Record we're updating.
	 * 
	 * @param attrCount - Number of Attributes to update
	 * @param hasStatus - Request has a Status
	 * @param hasAttributeId - Request has an attributeId
	 * @param actionType - ActionType to process
	 * @param schema - DB Schema
	 * @return
	 * @throws ActionException 
	 */
	private String bulkUpdateLinksSql(int attrCount, boolean hasStatus, boolean hasAttributeId, String actionType, String schema) throws ActionException {
		String tableNm = null;
		String pkNm = null;
		switch(actionType) {
			case "companyAdmin":
				tableNm = "BIOMEDGPS_COMPANY_ATTRIBUTE_XR";
				pkNm = "company_attribute_id";
				break;
			case "marketAdmin":
				tableNm = "BIOMEDGPS_MARKET_ATTRIBUTE_XR";
				pkNm = "market_attribute_id";
				break;
			case "productAdmin":
				tableNm = "BIOMEDGPS_PRODUCT_ATTRIBUTE_XR";
				pkNm = "product_attribute_id";
				break;
			default:
				log.error(String.format("Type %s not recognized.", actionType));
				break;
		}
		if(StringUtil.isEmpty(tableNm)) {
			throw new ActionException("Bulk Link Update Type not Supported.");
		}

		StringBuilder sql = new StringBuilder(150);
		sql.append("update ").append(schema).append(tableNm);
		if(hasStatus && hasAttributeId) {
			sql.append(" set status_no = ?, attribute_id = ?");
		} else if(hasAttributeId) {
			sql.append(" set attribute_id = ? ");
		} else if(hasStatus) {
			sql.append(" set status_no = ? ");
		}
		sql.append(DBUtil.WHERE_CLAUSE).append(pkNm).append(" in (");
		DBUtil.preparedStatmentQuestion(attrCount, sql);
		sql.append(')');
		return sql.toString();
	}
}
