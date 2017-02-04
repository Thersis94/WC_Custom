/**
 *
 */
package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.biomed.smarttrak.action.GapAnalysisAction;
import com.biomed.smarttrak.admin.vo.GapColumnAttributeVO;
import com.biomed.smarttrak.admin.vo.GapColumnVO;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
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
		String sectionId = req.getParameter("sectionId");
		String gaColumnId = req.getParameter("gaColumnId");

		if(!StringUtil.isEmpty(sectionId)) {
			List<GapColumnVO> cols = loadColumns(sectionId, gaColumnId);
			if(!StringUtil.isEmpty(gaColumnId)) {
				req.setAttribute("gapAttributes", getProdAttributes(gaColumnId));
			}
			this.putModuleData(cols, cols.size(), false);
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
			GapColumnVO gap = null;
			String gaColId = null;
			while(rs.next()) {
				if(!rs.getString("ga_column_id").equals(gaColId)) {
					if(gap != null) {
						columns.add(gap);
					}
					gap = new GapColumnVO(rs);
					gaColId = rs.getString("ga_column_id");
				}
				gap.addAttribute(new GapColumnAttributeVO(rs));
			}

			columns.add(gap);
		} catch (SQLException e) {
			log.error(e);
		}

		return columns;
	}

	public Node getProdAttributes(String gaColumnId) {
		Map<String, Node> nodes = new LinkedHashMap<>();
		Tree t = null;

		try(PreparedStatement ps = dbConn.prepareStatement(getProdAttributesSql())) {
			ps.setString(1, gaColumnId);

			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				GapColumnAttributeVO a = new GapColumnAttributeVO(rs);
				Node n = new Node(a.getAttributeId(), a.getParentId());
				n.setNodeName(a.getAttributeName());
				n.setUserObject(a);
				nodes.put(n.getNodeId(), n);
			}
			t = new Tree(new ArrayList<Node>(nodes.values()));

			return t.findNode("DETAILS_ROOT");
		} catch (SQLException e) {
			log.error(e);
		}
		return null;
	}

	public String getProdAttributesSql() {
		StringBuilder sql = new StringBuilder(400);
		String custom = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sql.append("select * from ").append(custom);
		sql.append("biomedgps_product_attribute a ");
		sql.append("left outer join ").append(custom);
		sql.append("biomedgps_ga_column_attribute_xr b ");
		sql.append("on a.attribute_id = b.attribute_id and ga_column_id = ? ");
		sql.append("where a.active_flg = 1 ");
		sql.append("order by a.parent_id desc, a.order_no, a.attribute_nm");

		return sql.toString();
	}
	/**
	 * @param empty
	 * @return
	 */
	private String getColumnSql(boolean hasColumnId) {
		StringBuilder sql = new StringBuilder(200);
		String custom = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sql.append("select * from ").append(custom);
		sql.append("biomedgps_ga_column a ");

		if(hasColumnId) {
			sql.append("inner join ").append(custom);
			sql.append("biomedgps_ga_column_attribute_xr b ");
			sql.append("on a.ga_column_id = b.ga_column_id ");
		}
		sql.append("where a.section_id = ? ");
		if(hasColumnId) {
			sql.append("and a.ga_column_id = ? ");
		}
		sql.append("order by a.order_no");
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
