package com.biomed.smarttrak.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.biomed.smarttrak.security.SmarttrakRoleVO;
import com.biomed.smarttrak.vo.PermissionVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.http.session.SMTSession;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
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
		super.retrieve(req);

		// Get the data necessary to trim down the tree to only the nodes
		// applicable for the user's subscription/permissions. 
		Tree tree = getTree(req);
		List<String> sectionIds = getPermittedSectionIds(req);
		
		// Trim the tree down to only nodes with a direct relation to the permitted ids.
		List<Node> sections = cleanTree(tree, sectionIds);
		
		this.putModuleData(sections);
	}
	
	/**
	 * Gets the section hierarchy tree
	 * 
	 * @return
	 * @throws ActionException 
	 */
	@SuppressWarnings("unchecked")
	protected Tree getTree(ActionRequest req) throws ActionException {
		SectionHierarchyAction sha = new SectionHierarchyAction(this.actionInit);
		sha.setAttributes(this.attributes);
		sha.setDBConnection(dbConn);
		sha.retrieve(req);
		
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		List<Node> hierarchy = (List<Node>) mod.getActionData();
		
		Tree tree = new Tree(hierarchy, hierarchy.get(0));
		tree.calculateTotalChildren(tree.getRootNode());
		
		return tree;
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
	 * @return
	 */
	protected List<Node> cleanTree(Tree tree, List<String> sectionIds) {
		Set<String> parentNodeIds = new HashSet<>();
		tree.buildNodePaths();

		// Get the list of parent node ids to keep
		for (String sectionId : sectionIds) {
			Node n = tree.findNode(sectionId);
			if (n != null) {
				String fullPath = n.getFullPath();
				parentNodeIds.addAll(Arrays.asList(fullPath.split("/")));
			}
		}

		Node n = tree.getRootNode();
		List<Node> sections = new ArrayList<>();
		sections.add(n);
		
		cleanNode(sections, parentNodeIds, sectionIds);
		
		return sections;
	}
	
	/**
	 * Removes nodes without a direct relationship to the user's subscribed sections
	 * 
	 * TODO: For some reason this modifies the cached version of the tree coming from the Section Hierarchy, maybe the object reference is cached???
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