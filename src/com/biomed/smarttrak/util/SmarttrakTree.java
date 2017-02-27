package com.biomed.smarttrak.util;

import java.util.List;

import com.biomed.smarttrak.vo.SectionVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.search.SearchDocumentHandler;

/****************************************************************************
 * <b>Title</b>: SmarttrakTree.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Feb 24, 2017
 ****************************************************************************/
public class SmarttrakTree extends Tree {
	private static final long serialVersionUID = 6016665755141838505L;

	/**
	 * @param data
	 * @param root
	 */
	public SmarttrakTree(List<Node> data, Node root) {
		super(data, root);
	}

	/**
	 * @param data
	 */
	public SmarttrakTree(List<Node> data) {
		super(data);
	}


	/**
	 * Set the fullPath variable of all nodes in the tree
	 * using the tree's default delimiter
	 */
	@Override
	public void buildNodePaths() {
		buildNodePaths(getRootNode(), SearchDocumentHandler.HIERARCHY_DELIMITER, true);
	}


	/**
	 * overrides the default method so we can build a full-path for name as well as a full-path for permissions (solrToken)
	 */
	@Override
	public void buildNodePaths(Node parent, String delimiter, boolean useName) {
		for (Node child : parent.getChildren()) {
			setNamePath(parent, child, delimiter, useName);
			setSolrTokenPath(parent, child);
			buildNodePaths(child, delimiter, useName);
		}
	}


	/**
	 * Populates the name-based hierachy from parent to child
	 * @param parent
	 * @param child
	 * @param delimiter
	 * @param useName
	 * @return
	 */
	private String setNamePath(Node parent, Node child, String delimiter, boolean useName) {
		StringBuilder path = new StringBuilder(50);

		//take the path from the parent node as a starting point
		if (!StringUtil.isEmpty(parent.getFullPath()))
			path.append(parent.getFullPath());

		//set the name hierarchy
		if (useName && !StringUtil.isEmpty(child.getNodeName())) {
			//append a delimiter if the parent path is not empty
			if (path.length() > 0) path.append(delimiter);
			path.append(child.getNodeName());

		} else if (!StringUtil.isEmpty(child.getNodeId())) {  //TODO - do we need paths based on ID fields?  Where used?
			//append a delimiter if the parent path is not empty
			if (path.length() > 0) path.append(delimiter);
			path.append(child.getNodeId());
		}

		if (path.length() > 0) 
			child.setFullPath(path.toString());

		return path.toString();
	}


	/**
	 * Populates the solrToken hierachy from parent to child
	 * @param parent
	 * @param child
	 * @return
	 */
	private String setSolrTokenPath(Node parent, Node child) {
		StringBuilder solrToken = new StringBuilder(50);
		SectionVO vo = (SectionVO) child.getUserObject();
		SectionVO parVo = (SectionVO) parent.getUserObject();

		//take the solrToken from the parent node as a starting point
		if (parVo != null && !StringUtil.isEmpty(parVo.getSolrTokenTxt())) 
			solrToken.append(parVo.getSolrTokenTxt());

		//set the solr hierarchy
		if (!StringUtil.isEmpty(vo.getSolrTokenTxt())) {
			//append a delimiter if the parent token is not empty
			if (solrToken.length() > 0) solrToken.append(SearchDocumentHandler.HIERARCHY_DELIMITER);
			solrToken.append(vo.getSolrTokenTxt());
		}

		if (solrToken.length() > 0) 
			vo.setSolrTokenTxt(solrToken.toString());

		return solrToken.toString();
	}
}