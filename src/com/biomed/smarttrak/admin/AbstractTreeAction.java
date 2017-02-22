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

	protected static final String MASTER_ROOT = "MASTER_ROOT"; //the root node of the master/primary hierarchy.

	public AbstractTreeAction() {
		super();
	}

	/**
	 * @param init
	 */
	public AbstractTreeAction(ActionInitVO init) {
		super(init);
	}


	/**
	 * Helper method that writes an object to cache with the given parameters.
	 * @param o
	 * @param orgId
	 * @param cacheGroups
	 */
	protected void writeToCache(Object o, String orgId, String... cacheGroups) {
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
		sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("BIOMEDGPS_SECTION a ");
		sql.append("order by PARENT_ID, ORDER_NO, SECTION_NM");
		return sql.toString();
	}


	/**
	 * Helper method that returns List of Nodes containing Sections.
	 * @param sectionId
	 * @return
	 */
	public List<Node> getHierarchy(String... params) {
		return loadHierarchy(new SectionVO().getClass(), params);
	}



	/**
	 * overloaded to support PreparedStatement args
	 * @param klass
	 * @param params
	 * @return
	 */
	protected List<Node> loadHierarchy(Class<? extends SectionVO> klass, String... params) {
		Map<String, Node> data = new LinkedHashMap<>();
		String sql =  getFullHierarchySql();

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			if (params != null && params.length > 0) {
				for (int x=0; x < params.length; x++)
					ps.setString(1+x, params[x]);
			}
			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				SectionVO segment = klass.newInstance();
				segment.setData(rs);

				Node n = new Node(segment.getSectionId(), segment.getParentId());
				n.setNodeName(segment.getSectionNm());
				n.setUserObject(segment);
				data.put(n.getNodeId(), n);
			}
		} catch (SQLException sqle) {
			log.error("Unable to get content hierarchies", sqle);
		} catch (IllegalAccessException | InstantiationException e) {
			log.error("Failed to load VO class, " + klass.getCanonicalName() + " does not extend SectionVO", e);
		}

		//Sort the Nodes.
		List<Node> sections = new ArrayList<>(data.values());
		Collections.sort(sections, new SectionComparator());
		log.debug("sections: " + sections.size());
		return sections;
	}

	/**
	 * Loads the List<SectionVO> and transposes the data into a Tree struture.
	 * @return
	 */
	public Tree loadDefaultTree(String... params) {
		return loadTree(MASTER_ROOT, params);
	}


	/**
	 * Loads the List<SectionVO> and transposes the data into a Tree struture.
	 * @return
	 */
	public Tree loadTree(String sectionId, String... params) {
		return loadTree(sectionId, new SectionVO().getClass(), params);
	}


	/**
	 * Loads the List<SectionVO> and transposes the data into a Tree struture.
	 * Overloaded method so subclasses can change the bean type
	 * @param sectionId
	 * @param klass
	 * @param params
	 * @return
	 */
	public Tree loadTree(String sectionId, Class<? extends SectionVO> klass, String... params) {
		List<Node> sections = loadHierarchy(klass, params);
		Tree t = new Tree(sections);

		//find the requested root node and prune the tree
		if (!StringUtil.isEmpty(sectionId)) {
			Node n = t.findNode(sectionId);
			if (n != null)
				t.setRootNode(n);
		}
		return t;
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
	protected class SectionComparator implements Comparator<Node> {
		public int compare(Node o1, Node o2) {
			SectionVO p1 = (SectionVO) o1.getUserObject();
			SectionVO p2 = (SectionVO) o2.getUserObject();
			if (p1 == null || p2 == null) return 0;

			return p1.getOrderNo().compareTo(p2.getOrderNo());
		}
	}
}