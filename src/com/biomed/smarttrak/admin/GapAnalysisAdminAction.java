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
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
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

			//If requesting a specific Column, load Product Attributes.
			if(!StringUtil.isEmpty(gaColumnId) || Convert.formatBoolean(req.getParameter("isAdd"))) {
				req.setAttribute("gapAttributes", getProdAttributes(gaColumnId));
			}
			this.putModuleData(cols, cols.size(), false);
		}
	}

	/**
	 * Helper method loads all the Gap Column Data on the request.
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

				//Check if ga_column_id is new.
				if(!rs.getString("ga_column_id").equals(gaColId)) {

					//If gap isn't null add it to the list.
					if(gap != null) {
						columns.add(gap);
					}

					//Instantiate new GapColumnVO and update gaColId.
					gap = new GapColumnVO(rs);
					gaColId = rs.getString("ga_column_id");
				}

				//Add Column Attribute on each call.
				gap.addAttribute(new GapColumnAttributeVO(rs));
			}

			//Add Trailing GapColumnVO.
			columns.add(gap);
		} catch (SQLException e) {
			log.error(e);
		}

		return columns;
	}

	/**
	 * Helper method loads all Product Attributes.
	 * @param gaColumnId
	 * @return
	 */
	private Node getProdAttributes(String gaColumnId) {
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

	/**
	 * Helper method that retrieves all Product Attributes in the system.
	 * @return
	 */
	private String getProdAttributesSql() {
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
	 * Helper method retrieves list of Gap Columns or Gap Column with all
	 * attributes depending on if a column Id is passed.
	 * @param empty
	 * @return
	 */
	private String getColumnSql(boolean hasColumnId) {
		StringBuilder sql = new StringBuilder(200);
		String custom = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sql.append("select * from ").append(custom);
		sql.append("biomedgps_ga_column a ");

		if(hasColumnId) {
			sql.append("left outer join ").append(custom);
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
		String actionPerform = req.getParameter("actionPerform");

		if(!StringUtil.isEmpty(actionPerform) && "delete".equals(actionPerform)) {
			delete(req);
		} else if(req.hasParameter("gaColumnId") || Convert.formatBoolean(req.getParameter("isAdd"))) {
			updateColumn(new GapColumnVO(req));
		}
	}

	/**
	 * Helper method saves all the attributes for a Given GapColumnVO.
	 * @param req
	 */
	private void saveAttributes(GapColumnVO col) {
		deleteAttributes(col.getGaColumnId());

		try(PreparedStatement ps = dbConn.prepareStatement(getAttributeCreateSql())) {
			UUIDGenerator uuid = new UUIDGenerator();
			for(GapColumnAttributeVO attr : col.getAttributes().values()) {
				ps.setString(1, attr.getAttributeId());
				ps.setString(2, col.getGaColumnId());
				ps.setTimestamp(3, Convert.getCurrentTimestamp());
				ps.setString(4, uuid.getUUID());
				ps.addBatch();
			}

			int [] res = ps.executeBatch();

			log.info("Inserted " + res[0] + " attributes");
		} catch (SQLException e) {
			log.error("Error updating attributes", e);
			log.error("Next Exception", e.getNextException());
		}
	}

	/**
	 * Helper method builds the Attribute Creation Sql Statement.
	 * @return
	 */
	private String getAttributeCreateSql() {
		StringBuilder sql = new StringBuilder(200);
		sql.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_ga_column_attribute_xr (attribute_id, ");
		sql.append("ga_column_id, create_dt, column_attribute_xr_id) "); 
		sql.append("values (?,?,?,?)");
		return sql.toString();
	}

	/**
	 * Helper method deletes all Gap Column Attributes.
	 * @param gaColumnId
	 */
	private void deleteAttributes(String gaColumnId) {
		try(PreparedStatement ps = dbConn.prepareStatement(getDeleteAttributeSql())) {
			ps.setString(1, gaColumnId);

			ps.executeUpdate();
		} catch (SQLException e) {
			log.error("Error Deleting Attributes", e);
		}
	}

	/**
	 * Helper method builds Gap Column Attribute Delete Sql.
	 * @return
	 */
	private String getDeleteAttributeSql() {
		StringBuilder sql = new StringBuilder(150);
		sql.append("delete from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_ga_column_attribute_xr where ga_column_id = ?");

		return sql.toString();
	}

	/**
	 * Helper method saves Gap Column.
	 * @param col
	 */
	private void updateColumn(GapColumnVO col) {
		DBProcessor dbp = new DBProcessor(dbConn, (String) getAttribute(Constants.CUSTOM_DB_SCHEMA));

		try {
			dbp.save(col);
			if(StringUtil.isEmpty(col.getGaColumnId())) {
				col.setGaColumnId(dbp.getGeneratedPKId());
			}
			saveAttributes(col);
		} catch(Exception e) {
			log.error("Problem saving Column.", e);
		}
	}
}
