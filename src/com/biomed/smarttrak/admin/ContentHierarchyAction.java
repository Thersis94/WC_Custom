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
import com.siliconmtn.action.ActionException;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: ContentHierarchyAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Action that manages Content Hierarchies.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author raptor
 * @version 1.0
 * @since Jan 6, 2017
 ****************************************************************************/
public class ContentHierarchyAction extends SBActionAdapter {

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#copy(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void copy(SMTServletRequest req) throws ActionException {
		throw new ActionException("Method not supported.");
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void delete(SMTServletRequest req) throws ActionException {
		SectionVO s = new SectionVO(req);

		DBProcessor dbp = new DBProcessor(dbConn, (String)attributes.get(Constants.CUSTOM_DB_SCHEMA));

		try {
			dbp.delete(s);
		} catch (InvalidDataException | DatabaseException e) {
			log.error(e);
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO modVo = (ModuleVO) getAttribute(Constants.MODULE_DATA);

		String sectionId = req.getParameter("sectionId");

		//Build a Tree from the list.
		Tree tree = getHierarchy(sectionId);
		List<Node> nodeList = tree.preorderList();
		modVo.setActionData(nodeList);
		modVo.setDataSize(tree.getDepth());

		this.setAttribute(Constants.MODULE_DATA, modVo);
	}

	/**
	 * 
	 * @param sectionId
	 * @return
	 */
	public Tree getHierarchy(String sectionId) {
		boolean isEdit = !StringUtil.isEmpty(sectionId);
		Map<String, Node> data = new LinkedHashMap<>();
		try (PreparedStatement ps = dbConn.prepareStatement(getContentHierarchyListSql(isEdit))) {
			if(isEdit) {
				ps.setString(1, sectionId);
			}

			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				SectionVO segment = new SectionVO(rs);
				String nodeId = segment.getSectionId();

				Node n = new Node(nodeId, segment.getParentId());
				n.setNodeName(segment.getSectionNm());
				n.setUserObject(segment);
				data.put(nodeId, n);
			}
		} catch (SQLException sqle) {
			log.error("Unable to get content hierarchies", sqle);
		}

		//Sort the Nodes.
		List<Node> sections = new ArrayList<>(data.values());
		Collections.sort(sections, new SectionComparator());
		log.debug("cnt=" + sections.size());

		//Build a Tree from the list.
		Tree tree = new Tree(sections);
		List<Node> nodeList = tree.preorderList();

		return tree;
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		SectionVO s = new SectionVO(req);

		saveSectionVO(s);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(SMTServletRequest req) throws ActionException {
		if(!StringUtil.isEmpty(req.getParameter(SBActionAdapter.SB_ACTION_ID))) {
			super.retrieve(req);
		} else {
			super.list(req);
		}
	}

	/**
	 * Helper method that returns the Sql Query for retrieving Segments. 
	 * @return
	 */
	private String getContentHierarchyListSql(boolean isEdit) {
		StringBuilder sql = new StringBuilder(200);
		sql.append("select * from ");
		sql.append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("BIOMEDGPS_SECTION a ");

		if(isEdit) {
			sql.append("where a.SECTION_ID = ? ");
		}

		//sql databases treat ordering nulls in different ways, by coalescing on blank we guarantee nulls first
		sql.append("order by PARENT_ID, ORDER_NO, SECTION_NM");

		return sql.toString();
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {
		super.update(req);

		 // Redirect after the update
        sbUtil.adminRedirect(req, attributes.get(Constants.ACTION_SUCCESS_KEY), (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
	}

	/**
	 * Helper method that inserts/updates a SectionVO.
	 * @param s
	 */
	private void saveSectionVO(SectionVO s) {
		DBProcessor dbp = new DBProcessor(dbConn, (String)attributes.get(Constants.CUSTOM_DB_SCHEMA));

		try {
			dbp.save(s);
		} catch (InvalidDataException | DatabaseException e) {
			log.error(e);
		}
	}

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