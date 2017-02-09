/**
 *
 */
package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.biomed.smarttrak.vo.SectionVO;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: AbstractTreeAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Action containing Tree loading and caching code that can
 * be implemented as necessary by child actions.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Jan 23, 2017
 ****************************************************************************/
public abstract class AbstractTreeAction extends SBActionAdapter {

	/**
	 * @param init
	 */
	public AbstractTreeAction(ActionInitVO init) {super(init);}
	public AbstractTreeAction() {super();}

	/**
	 * Helper method that writes an object to cache with the given parameters.
	 * @param o
	 * @param orgId
	 * @param cacheGroups
	 */
	public void writeToCache(Object o, String orgId, String ...cacheGroups) {
		//Use a new ModuleVO so as to prevent issues with cache.
		ModuleVO mod = new ModuleVO();

		mod.setActionData(o);
		mod.setDataSize(0);
		mod.setCacheable(true);
		mod.setOrganizationId(orgId);

		//Common Cache Group for Content Hierarchy Data.
		mod.setCacheGroups(cacheGroups);
		mod.setPageModuleId(getCacheKey());

		//Write to Cache.
		super.writeToCache(mod);
	}
	
	/**
	 * Helper method that returns the Sql Query for retrieving Segments. 
	 * @return
	 */
	protected String getFullHierarchySql() {
		StringBuilder sql = new StringBuilder(200);
		sql.append("select * from ");
		sql.append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("BIOMEDGPS_SECTION a ");

		//sql databases treat ordering nulls in different ways, by coalescing on blank we guarantee nulls first
		sql.append("order by PARENT_ID, ORDER_NO, SECTION_NM");

		return sql.toString();
	}

	/**
	 * Helper method that returns a Sql statment for retrieving a partial
	 * Hierarchy Tree from a given sectionId down.
	 * @return
	 */
	protected String getPartialHierarchySql() {
		StringBuilder sql = new StringBuilder(1000);
		sql.append("select b.section_id, b.section_nm as lvl1nm, ");
		sql.append("c.section_id, c.section_nm as lvl2nm, ");
		sql.append("d.section_id, d.section_nm as lvl3nm, ");
		sql.append("e.section_id, e.section_nm as lvl4nm_column, ");
		sql.append("f.section_id, f.section_nm as lvl5nm_column, ");
		sql.append("g.section_id, g.section_nm as lvl6nm_column ");
		sql.append("from custom.biomedgps_section a ");
		sql.append("inner join custom.biomedgps_section b on a.section_id=b.parent_id ");
		sql.append("left join custom.biomedgps_section c on b.section_id=c.parent_id ");
		sql.append("left join custom.biomedgps_section d on c.section_id=d.parent_id ");
		sql.append("left join custom.biomedgps_section e on d.section_id=e.parent_id ");
		sql.append("left join custom.biomedgps_section f on e.section_id=f.parent_id ");
		sql.append("left join custom.biomedgps_section g on f.section_id=g.parent_id ");
		sql.append("where a.section_id = ? ");
		sql.append("order by a.order_no, b.order_no, c.order_no, d.order_no, e.order_no, f.order_no, g.order_no; ");

		return sql.toString();
	}

	/**
	 * Helper method that returns List of Nodes containing Sections.
	 * @param sectionId
	 * @return
	 */
	public List<Node> getHierarchy(String sectionId) {
		Map<String, Node> data = new LinkedHashMap<>();
		String sql;
		if(StringUtil.isEmpty(sectionId)) {
			sql = this.getFullHierarchySql();
		} else {
			sql = this.getPartialHierarchySql();
		}
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			if(!StringUtil.isEmpty(sectionId)) {
				ps.setString(1, sectionId);
			}

			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				SectionVO segment = new SectionVO(rs);

				Node n = new Node(segment.getSectionId(), segment.getParentId());
				n.setNodeName(segment.getSectionNm());
				n.setUserObject(segment);
				data.put(n.getNodeId(), n);
			}
		} catch (SQLException sqle) {
			log.error("Unable to get content hierarchies", sqle);
		}

		//Sort the Nodes.
		List<Node> sections = new ArrayList<>(data.values());
		Collections.sort(sections, new SectionComparator());
		log.debug("cnt=" + sections.size());

		return sections;
	}

	/**
	 * Helper method that manages creating a ModuleVO, loading the entire
	 * hierarchy tree and storing it in cache.
	 * @return
	 */
	public Tree loadTree(String sectionId) {

		List<Node> sections = getHierarchy(sectionId);

		//Build and return a Tree from the list.
		return new Tree(sections);
	}

	/**
	 * Abstract Method that should return a given actions cacheKey.
	 * @return
	 */
	public abstract String getCacheKey();

	/**
	 * **************************************************************************
	 * <b>Title: </b>SectionComparator<p/>
	 * <b>Description: </b> Reorders list of Content Sections.
	 * <p/>
	 * <b>Copyright:</b> Copyright (c) 2015<p/>
	 * <b>Company:</b> Silicon Mountain Technologies<p/>
	 * @author Billy Larsen
	 * @version 1.0
	 * @since Jan 6, 2017
	 ***************************************************************************
	 */
	private class SectionComparator implements Comparator<Node> {
		public int compare(Node o1, Node o2) {
			SectionVO p1 = (SectionVO) o1.getUserObject();
			SectionVO p2 = (SectionVO) o2.getUserObject();
			if (p1 == null || p2 == null) return 0;

			return p1.getOrderNo().compareTo(p2.getOrderNo());
		}
		
	}
}
