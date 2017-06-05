package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.biomed.smarttrak.security.SmarttrakRoleVO;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.vo.PermissionVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Node;
import com.siliconmtn.http.session.SMTSession;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: FinancialDashHierarchyAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Action that retrieves Financial Dash Section Hierarchies.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 1.0
 * @since Feb 23, 2017
 ****************************************************************************/
public class FinancialDashHierarchyAction extends SBActionAdapter {

	/**
	 * @param init
	 */
	public FinancialDashHierarchyAction() {
		super();
	}
	
	public FinancialDashHierarchyAction(ActionInitVO init) {
		super(init);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		// Get the data necessary to trim down the tree to only the nodes
		// applicable for the user's subscription/permissions. 
		SmarttrakTree tree = getTree(req);
		List<String> dataSectionIds = getDataSectionIds();
		List<String> permSectionIds = getPermittedSectionIds(req);
		String rootId = req.getParameter("sectionId");
		
		// Trim the tree down to only the nodes actually used in the FD data
		List<Node> dataSections = cleanTree(tree, dataSectionIds, rootId, true);
		
		// Reset for a second pass to trim based on the user's subscriptions
		Node node = new Node();
		node.addChild(dataSections.get(0));
		dataSections = new ArrayList<>();
		dataSections.add(node);
		tree = new SmarttrakTree(dataSections, dataSections.get(0));

		// Trim the tree down to only nodes with a direct relation to the permitted ids.
		List<Node> sections = cleanTree(tree, permSectionIds, rootId, false);
		
		this.putModuleData(sections);
	}
	
	/**
	 * Gets the section hierarchy tree
	 * 
	 * @return
	 * @throws ActionException 
	 */
	protected SmarttrakTree getTree(ActionRequest req) throws ActionException {
		SectionHierarchyAction sha = new SectionHierarchyAction(this.actionInit);
		sha.setAttributes(this.attributes);
		sha.setDBConnection(dbConn);
		
		SmarttrakTree tree = sha.loadTree(null);
		tree.calculateTotalChildren(tree.getRootNode());
		
		return tree;
	}
	
	/**
	 * Gets a list of sections with available data to prevent users
	 * from navigating to empty data sets.
	 * 
	 * @return
	 */
	protected List<String> getDataSectionIds() {
		List<String> sectionIds = new ArrayList<>();
		String custom = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		
		StringBuilder sql = new StringBuilder(100);
		sql.append("select section_id ");
		sql.append("from ").append(custom).append("biomedgps_fd_revenue ");
		sql.append("group by section_id");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				sectionIds.add(rs.getString("section_id"));
			}
		} catch (SQLException sqle) {
			log.error("Unable to get list of available fd sections", sqle);
		}
		
		return sectionIds;
	}
	
	/**
	 * Get the list of sections the user has permission for on the financial dashboard.
	 * 
	 * @param req
	 * @return
	 */
	protected List<String> getPermittedSectionIds(ActionRequest req) {
		SMTSession ses = req.getSession();
		SmarttrakRoleVO rvo = (SmarttrakRoleVO) ses.getAttribute(Constants.ROLE_DATA);
		List<PermissionVO> permissions = rvo.getAccountRoles();
		
		List<String> sectionIds = new ArrayList<>();
		for (PermissionVO perm : permissions) {
			if (perm.isFdAuth()) {
				sectionIds.add(perm.getSectionId());
			}
		}
		
		return sectionIds;
	}

	/**
	 * Trims the tree down so that it only has a node's direct parents, grandparents, children, grandchildren, etc.
	 * Nothing should exist in the hierarchy tree without a direct relation to the permitted nodes.
	 * 
	 * TODO: This should really belong in the Tree class
	 * 
	 * @param tree
	 * @param sectionIds
	 * @param rootId
	 * @return
	 */
	protected List<Node> cleanTree(SmarttrakTree tree, List<String> sectionIds, String rootId, boolean buildPaths) {
		Set<String> parentNodeIds = new HashSet<>();
		
		if (buildPaths) {
			tree.buildNodePaths(tree.getRootNode(), "/", false);
		}

		// Get the list of parent node ids to keep
		for (String sectionId : sectionIds) {
			Node n = tree.findNode(sectionId);
			if (n != null) {
				String fullPath = n.getFullPath();
				parentNodeIds.addAll(Arrays.asList(fullPath.split("/")));
			}
		}

		Node n = tree.findNode(rootId);
		List<Node> sections = new ArrayList<>();
		sections.add(n);
		
		cleanNode(sections, parentNodeIds, sectionIds);
		
		return sections;
	}
	
	/**
	 * Removes nodes without a direct relationship to the user's subscribed sections
	 * 
	 * @param searchNodes
	 * @param parentNodeIds
	 * @param sectionIds
	 */
	protected void cleanNode(List<Node> searchNodes, Set<String> parentNodeIds, List<String> sectionIds) {
		Iterator<Node> iter = searchNodes.iterator();
		
		while (iter.hasNext()) {
			Node node = iter.next();
			String nodeId = node.getNodeId();
			
			if (sectionIds.contains(nodeId)) {
				continue;
			} else if (parentNodeIds.contains(nodeId)) {
				cleanNode(node.getChildren(), parentNodeIds, sectionIds);
			} else {
				iter.remove();
			}
		}
	}
}