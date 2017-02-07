/**
 *
 */
package com.biomed.smarttrak.action;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.lang.ArrayUtils;

import com.biomed.smarttrak.admin.ContentHierarchyAction;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Node;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: GapAnalysisAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Public facing action for Processing GAP Analysis
 * Requests.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Jan 13, 2017
 ****************************************************************************/
public class GapAnalysisAction extends ContentHierarchyAction {

	public GapAnalysisAction() {
		super();
	}

	public GapAnalysisAction(ActionInitVO init) {
		super(init);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.ActionRequest)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if(req.hasParameter("selNodes")) {
			super.retrieve(req);
			ModuleVO mod = (ModuleVO) this.getAttribute(Constants.MODULE_DATA);
			List<Node> nodes = (List<Node>) mod.getActionData();
			String [] selNodes = req.getParameterValues("selNodes");

			nodes = filterNodes(nodes, selNodes);

			super.putModuleData(nodes, nodes.size(), false);
		}
	}

	/**
	 * Helper method that filters the Selected Child Nodes out of the main tree.
	 * @param selNodes
	 * @return
	 */
	private List<Node> filterNodes(List<Node> nodes, String[] selNodes) {
		List<Node> filteredNodes = new ArrayList<>();
		for(Node g : nodes) {
			for(Node p : g.getChildren()) {
				for(Node c : p.getChildren()) {
					ListIterator<Node> nIter = c.getChildren().listIterator();
					while(nIter.hasNext()) {
						Node n = nIter.next();
						for(int i = 0; i < selNodes.length; i++) {
							if(n.getNodeId().equals(selNodes[i])) {
								filteredNodes.add(n);
								nIter.remove();
								selNodes = (String[]) ArrayUtils.remove(selNodes, i);
								break;
							}
						}
					}
				}
			}
		}
		return filteredNodes;
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		//Forward to proper Action to manage Attributes.
	}

}