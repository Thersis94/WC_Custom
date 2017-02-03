/**
 *
 */
package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.biomed.smarttrak.action.GapAnalysisAction;
import com.biomed.smarttrak.admin.vo.GapColumnAttributeVO;
import com.biomed.smarttrak.admin.vo.GapColumnVO;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: GapMarketAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Public facing action for Managing GAP Analysis data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Jan 13, 2017
 ****************************************************************************/
public class GapAnalysisAdminAction extends GapAnalysisAction {
	public GapAnalysisAdminAction() {
		super();
	}

	public GapAnalysisAdminAction(ActionInitVO init) {
		super(init);
	}

	@Override
	public void delete(ActionRequest req) {
		GapColumnVO col = new GapColumnVO(req);

		DBProcessor dbp = new DBProcessor(dbConn, (String)attributes.get(Constants.CUSTOM_DB_SCHEMA));

		try {
			dbp.delete(col);
		} catch (InvalidDataException | DatabaseException e) {
			log.error(e);
		}
	}

	@Override
	public void retrieve(ActionRequest req) {
		String attributeType = req.getParameter("attributeType");
		String sectionId = req.getParameter("sectionId");
		String gaColumnId = req.getParameter("gaColumnId");
		if(StringUtil.isEmpty(attributeType)) {
			List<GapColumnVO> cols = loadColumns(sectionId, gaColumnId);
			this.putModuleData(cols, cols.size(), false);
		} else {
			loadAttributeXRs(gaColumnId, attributeType);
		}
	}

	/**
	 * @param sectionId
	 * @param gaColumnId
	 * @return 
	 */
	private List<GapColumnVO> loadColumns(String sectionId, String gaColumnId) {
		List<GapColumnVO> columns = new ArrayList<>();
		try(PreparedStatement ps = dbConn.prepareStatement(getColumnSql(!StringUtil.isEmpty(gaColumnId)))) {
			ps.setString(1, sectionId);
			if(!StringUtil.isEmpty(gaColumnId)) {
				ps.setString(2, gaColumnId);
			}

			ResultSet rs = ps.executeQuery();

			while(rs.next()) {
				columns.add(new GapColumnVO(rs));
			}
		} catch (SQLException e) {
			log.error(e);
		}

		return columns;
	}

	/**
	 * @param empty
	 * @return
	 */
	private String getColumnSql(boolean hasColumnId) {
		StringBuilder sql = new StringBuilder(200);
		sql.append("select * from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_ga_column where section_id = ? ");
		if(hasColumnId) {
			sql.append("and ga_column_id = ? ");
		}
		sql.append("order by order_no");
		return sql.toString();
	}

	private List<Object> loadAttributeXRs(String gaColumnId, String attributeType) {
		List<Object> columnAttributes;

		DBProcessor dbp = new DBProcessor(dbConn, (String) attributes.get(Constants.CUSTOM_DB_SCHEMA));

		try {
			List<Object> params = new ArrayList<>();
			params.add(gaColumnId);
			columnAttributes = dbp.executeSelect(getAttributesSql(), params, (Object)new GapColumnAttributeVO());
		} catch(Exception e) {
			log.error("Problem retrieving attributes", e);
			columnAttributes = null;
		}

		return columnAttributes;
	}

	private String getAttributesSql() {
		StringBuilder sql = new StringBuilder(150);
		sql.append("select * from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_ga_column_attribute_xr where ga_column_id = ?");

		return sql.toString();
	}

	@Override
	public void build(ActionRequest req) {
		if(req.hasParameter("gaColumnId")) {
			updateColumn(new GapColumnVO(req));
		}
	}

	public void updateColumn(GapColumnVO col) {
		DBProcessor dbp = new DBProcessor(dbConn, (String) getAttribute(Constants.CUSTOM_DB_SCHEMA));

		try {
			dbp.save(col);
		} catch(Exception e) {
			log.error("Problem saving Column.", e);
		}
	}
}
