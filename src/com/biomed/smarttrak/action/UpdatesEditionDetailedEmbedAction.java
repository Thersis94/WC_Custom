package com.biomed.smarttrak.action;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.biomed.smarttrak.vo.UpdateVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title:</b> UpdatesEditionEmbedAction.java<br/>
 * <b>Description:</b> Referenced as the 'embed' action for Email Campaigns - Daily & Weekly.
 * Contains code customizations that apply to the EMAIL specifically, rather than the WEBPAGE.
 * This Version packages the data in a format where all updates are included so
 * the can be written out individually.
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2017<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Aug 7, 2017
 ****************************************************************************/
public class UpdatesEditionDetailedEmbedAction extends UpdatesEditionAction {

	public UpdatesEditionDetailedEmbedAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public UpdatesEditionDetailedEmbedAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	
	public void retrieve (ActionRequest req) throws ActionException {
		req.setParameter("orderSort", "true");
		req.setParameter("redirectLinks", "true");
		super.retrieve(req);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.biomed.smarttrak.action.UpdatesEditionAction#packageDataForDisplay(com.siliconmtn.data.Tree, java.util.List)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void packageDataForDisplay(Tree t, List<UpdateVO> updates) {
		Map<String, Map<String, List<UpdateVO>>> dataMap = new LinkedHashMap<>();

		for (Node n : t.getRootNode().getChildren()) {
			boolean hasChildren = false;
			Map<String, List<UpdateVO>> children = new LinkedHashMap<>();

			//Look for Updates tied to the Root Node.
			Object o = n.getUserObject();
			if(o != null && o instanceof List && !((List<?>)o).isEmpty()) {
				hasChildren = addUpdates(children, "root", (List<UpdateVO>)o);
			}

			if(n.getTotalChildren() > 0) {
				for(Node c : n.getChildren()) {

					//Look for Updates on the Child Nodes.
					if(c.getTotalChildren() > 0) {
						hasChildren = addUpdates(children, c.getNodeName(), (List<UpdateVO>)c.getUserObject());
					}
				}
			}

			//Only add Node if we actually have children.
			if(hasChildren) {
				dataMap.put(n.getNodeName(), children);
			}
		}

		putModuleData(dataMap, dataMap.size(), false);
	}

	/**
	 * Moved Add elements to it's own method.
	 * @param children
	 * @param key
	 * @param updates
	 * @return
	 */
	public boolean addUpdates(Map<String, List<UpdateVO>> children, String key, List<UpdateVO> updates) {
		children.put(key, updates);
		return true;
	}

	/*
	 * return the value loaded out of the EC data source, unless a suppress flag was also provided.
	 * (non-Javadoc)
	 * @see com.biomed.smarttrak.action.UpdatesEditionAction#getProfileId(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	protected void setProfileId(ActionRequest req) {
		if(Convert.formatBoolean(req.getParameter("uniqueSendFlg"))) {
			req.setParameter(PROFILE_ID, null);
		}

		log.debug(req.getParameter(PROFILE_ID));
	}
}