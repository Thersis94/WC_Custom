/**
 *
 */
package com.biomed.smarttrak.action.gap;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: GapFacadeAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Facade Action for Processing GAP Analysis Requests.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author raptor
 * @version 1.0
 * @since Jan 13, 2017
 ****************************************************************************/
public class GapFacadeAction extends SBActionAdapter {

	public GapFacadeAction() {
		super();
	}

	public GapFacadeAction(ActionInitVO init) {
		super(init);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		if(req.hasParameter("selItems")) {
			String [] selNodes = StringUtil.checkVal(req.getParameter("selItems")).split(",");
			List<Node> gapNodes = getGapHierarchy(selNodes);

			super.putModuleData(gapNodes, gapNodes.size(), false);
		}
	}

	/**
	 * Helper method that returns the Gap Hierarchy Tree.
	 * @param rootNode
	 * @return
	 */
	private List<Node> getGapHierarchy(String [] selNodes) {
		List<Node> nodes = new ArrayList<>();
		Set<String> keys = new HashSet<>();
		try(PreparedStatement ps = dbConn.prepareStatement(getGapTreeSql(selNodes.length))) {
			for(int i = 0; i < selNodes.length; i++) {
				ps.setString(i + 1, selNodes[i]);
			}

			ResultSet rs = ps.executeQuery();

			/*
			 * Loop over Result Set.  Each Result can contain up to 6 Node points
			 * so attempt to add at each level.  If a node doesn't add/isn't already
			 * present then that means no node was needed and we move to next
			 * Result.
			 */
			while(rs.next()) {
				String parent = null;
				if(!addNode(keys, nodes, parent, StringUtil.checkVal(rs.getString("lvl1Id")), rs.getString("lvl1Nm"))) {
					parent = StringUtil.checkVal(rs.getString("lvl1Id"));
					if(!addNode(keys, nodes, parent, StringUtil.checkVal(rs.getString("lvl2Id")), rs.getString("lvl2Nm"))) {
						parent = StringUtil.checkVal(rs.getString("lvl2Id"));
						if(!addNode(keys, nodes, parent, StringUtil.checkVal(rs.getString("lvl3Id")), rs.getString("lvl3Nm"))) {
							parent = StringUtil.checkVal(rs.getString("lvl3Id"));
							if(!addNode(keys, nodes, parent, StringUtil.checkVal(rs.getString("lvl4Id")), rs.getString("lvl4Nm"))) {
								parent = StringUtil.checkVal(rs.getString("lvl4Id"));
								if(!addNode(keys, nodes, parent, StringUtil.checkVal(rs.getString("lvl5Id")), rs.getString("lvl5Nm"))) {
									parent = StringUtil.checkVal(rs.getString("lvl5Id"));
									addNode(keys, nodes, parent, StringUtil.checkVal(rs.getString("lvl6Id")), rs.getString("lvl6Nm"));
								}
							}
						}
					}
				}
			}
		} catch (SQLException e) {
			log.error(e);
		}

		return nodes;
	}

	/**
	 * Helper method that contains logic for building and adding a node.
	 * @param keys - Set containing all NodeIds we've created.
	 * @param nodes - List of all Nodes.
	 * @param parent - The parentId for the current Node. 
	 * @param id - The Id for the current Node.
	 * @param value - The Value for the current Node.
	 * @return true if Node already exists or was created.
	 */
	private boolean addNode(Set<String> keys, List<Node> nodes, String parent, String id, String value) {

		/*
		 * See if the Node has been added already by verifying if the Node id is
		 * in the keys Set.
		 */
		boolean nodeAdded = keys.contains(id);

		/*
		 * If the node hasn't been added and the id isn't empty, add a new node
		 * and set added to true.
		 */
		if(!StringUtil.isEmpty(id) && !nodeAdded) {
			Node n = new Node(id, parent);
			n.setNodeName(value);
			nodes.add(n);
			nodeAdded = true;
		}

		//Return added.
		return nodeAdded;
	}
	/**
	 * Return the GapAnalysis Retrieval Sql.
	 * @return
	 */
	private String getGapTreeSql(int length) {
		StringBuilder sql = new StringBuilder(1000);
		sql.append("select b.section_id as lvl1Id, b.section_nm as lvl1Nm, ");
		sql.append("c.section_id as lvl2Id, c.section_nm as lvl2Nm, ");
		sql.append("d.section_id as lvl3Id, d.section_nm as lvl3Nm, ");
		sql.append("e.section_id as lvl4Id, e.section_nm as lvl4Nm, ");
		sql.append("f.section_id as lvl5Id, f.section_nm as lvl5Nm, ");
		sql.append("g.section_id as lvl6Id, g.section_nm as lvl6Nm ");
		sql.append("from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_section a ");
		sql.append("inner join ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_section b on a.section_id = b.parent_id ");
		sql.append("left join ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_section c on b.section_id = c.parent_id ");
		sql.append("left join ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_section d on c.section_id = d.parent_id ");
		sql.append("left join ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_section e on d.section_id = e.parent_id ");
		sql.append("left join ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_section f on e.section_id = f.parent_id ");
		sql.append("left join ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_section g on f.section_id = g.parent_id ");
		sql.append("where a.section_id in ( ");
		for(int i = 0; i < length; i++) {
			if(i > 0) {
				sql.append(",");
			}
			sql.append("?");
		}
		sql.append(") order by a.order_no, b.order_no, c.order_no, d.order_no, ");
		sql.append("e.order_no, f.order_no, g.order_no ");
		log.debug(sql.toString());
		return sql.toString();
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 * TODO - COMPLETE METHOD BODY
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		//super.build(req);
	}

}