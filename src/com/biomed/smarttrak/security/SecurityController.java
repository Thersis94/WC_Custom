package com.biomed.smarttrak.security;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.biomed.smarttrak.vo.PermissionVO;
import com.biomed.smarttrak.vo.SectionVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.search.SearchDocumentHandler;

/****************************************************************************
 * <b>Title</b>: SecurityController.java<p/>
 * <b>Description: overload of Smarttrak permissions.  Makes the decisions about who gets to see what, 
 * based on the Roles of the user and the asset they're trying to view.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Feb 22, 2017
 ****************************************************************************/
public class SecurityController {

	protected static final Logger log = Logger.getLogger(SecurityController.class);
	
	/**
	 * how far down the hierarchy tree are permissions applied.  
	 * Put in a constant in-case Smarttrak ever changes their hierarchy structure.
	 */
	public static final int PERMISSION_DEPTH_LVL = 4;

	private SmarttrakRoleVO role;

	private SecurityController(SmarttrakRoleVO role) {
		super();
		this.role = role;
	}

	/**
	 * static factory method.  useful for inline applications: SecurityController.getInstance(roleData).getSolrACL();
	 * @param role
	 * @return
	 */
	public static SecurityController getInstance(SmarttrakRoleVO role) {
		return new SecurityController(role);
	}

	/**
	 * is the user authorized to see the Financial Dashboard tool (period)
	 * @return
	 */
	public boolean isFdAuthorized() {
		return role.isFdAuthorized();
	}

	/**
	 * is the user authorized to see the Gap Analysis tool (period)
	 * @return
	 */
	public boolean isGaAuthorized() {
		return role.isGaAuthorized();
	}

	/**
	 * is the user authorized to see the Market Reports (period)
	 * @return
	 */
	public boolean isMktAuthorized() {
		return role.isMktAuthorized();
	}

	/**
	 * returns a String we feed to Solr that gets applied to the Documents found in the search.
	 * Solr applies the permissions for us and removes documents the user is not authorized to see
	 * @return
	 */
	public String getSolrACL() {
		StringBuilder sb = new StringBuilder(500);
		//iterate the section permissions into a unix-like syntax used by Solr
		for (PermissionVO vo : role.getAccountRoles()) {
			sb.append(" +g:").append(vo.getHierarchyToken()); //Read: grant access to group <roleToken>
		}
		log.debug("Solr ACL: " + sb);
		return sb.toString().trim();
	}


	/**
	 * returns a list of groups, based on solrToken, to be stored in Solr for when we search for data
	 * @param sections
	 * @return
	 */
	public List<String> getSolrGroups(Tree sectionTree) {
		buildNodePaths(sectionTree.getRootNode());
		List<Node> nodes = sectionTree.preorderList();
		List<String> groups = new ArrayList<>();

		for (Node n : nodes) {
			SectionVO vo = (SectionVO) n.getUserObject();
			//we only care about level 4 nodes, which is where permissions are set.  Also toss any VOs that don't have permissions in them
			if (PERMISSION_DEPTH_LVL != n.getDepthLevel() || !vo.isSelected()) continue;

			groups.add(n.getFullPath());

		}

		return groups;
	}

	/**
	 * modeled after Tree.buildNodePaths() - only difference is we're using the solrToken from 
	 * the SectionVO and not the nodeName
	 * @param parentNode
	 * @param delimiter
	 */
	static void buildNodePaths(Node parentNode) {
		for (Node node : parentNode.getChildren()) {
			StringBuilder path = new StringBuilder(50);
			//take the path from the parent node as a starting point
			if (!StringUtil.isEmpty(parentNode.getFullPath()))
				path.append(parentNode.getFullPath());

			//get the PermissionVO - we need the solrToken from it
			PermissionVO vo = (PermissionVO) node.getUserObject();

			if (!StringUtil.isEmpty(vo.getSolrTokenTxt())) {
				//append a delimiter if the parent path is not empty
				if (path.length() > 0) path.append(SearchDocumentHandler.HIERARCHY_DELIMITER);

				path.append(vo.getSolrTokenTxt());
			}

			//if we built a path, assign it to this node.
			if (path.length() > 0) node.setFullPath(path.toString());

			buildNodePaths(node);
		}
	}
}