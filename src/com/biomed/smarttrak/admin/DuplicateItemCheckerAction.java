package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.http.filter.fileupload.Constants;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: DuplicateItemCheckerAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Action for querying the database for existing items
 * with the same name as the supplied item
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * @author Eric Damschroder
 * @version 1.0
 * @since Sept 5, 2018
 ****************************************************************************/

public class DuplicateItemCheckerAction extends SBActionAdapter {
	
	enum CheckerType {
		PRODUCT("product", "product_nm", "short_nm", "product_id"),
		COMPANY("company", "company_nm", "short_nm_txt", "company_id");
		
		private String tableNm;
		private String nameField;
		private String shortNameField;
		private String idField;
		
		private CheckerType(String tableNm, String nameField, String shortNameField, String idField) {
			this.tableNm = tableNm;
			this.nameField = nameField;
			this.shortNameField = shortNameField;
			this.idField = idField;
		}
		
		public String getTableNm() {
			return tableNm;
		}
		
		public String getNameField() {
			return nameField;
		}
		
		public String getShortNameField() {
			return shortNameField;
		}
		
		public String getIdField() {
			return idField;
		}
	}
	
	public void retrieve(ActionRequest req) throws ActionException {
		CheckerType ct = CheckerType.valueOf(req.getParameter("type"));
		if (!req.hasParameter("name")) return;
		
		putModuleData(checkForDuplicates(ct, req.getParameter("name").toLowerCase(), req.getParameter("id")));
	}
	
	/**
	 * Check for items other than the supplied one with the same name or short name
	 * @param type
	 * @param name
	 * @param id
	 * @return
	 * @throws ActionException
	 */
	private List<GenericVO> checkForDuplicates(CheckerType type, String name, String id) throws ActionException {
		List<GenericVO> itemNames = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(buildSQL(type))) {
			ps.setString(1, name);
			ps.setString(2, name);
			ps.setString(3, id);
			ResultSet rs = ps.executeQuery();
			
			while (rs.next())
				itemNames.add(new GenericVO(rs.getString(1), rs.getString(2)));
			
		} catch (SQLException e) {
			throw new ActionException(e);
		}
		
		return itemNames;
	}
	
	
	/**
	 * Build the sql for the duplicate check
	 * @param type
	 * @return
	 */
	private String buildSQL(CheckerType type) {
		StringBuilder sql = new StringBuilder(150);
		sql.append("select ").append(type.getNameField()).append(", ").append(type.getIdField()).append(" from ");
		sql.append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("biomedgps_").append(type.getTableNm());
		sql.append(" where (lower(").append(type.getNameField()).append(") = ? or lower(").append(type.getShortNameField()).append(") = ?) ");
		sql.append("and ").append(type.getIdField()).append(" != ? ");
		sql.append("order by ").append(type.getNameField());
		
		return sql.toString();
	}

}
