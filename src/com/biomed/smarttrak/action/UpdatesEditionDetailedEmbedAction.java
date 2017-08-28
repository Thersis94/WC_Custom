package com.biomed.smarttrak.action;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.biomed.smarttrak.vo.UpdateVO;
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
public class UpdatesEditionIndividualEmbedAction extends UpdatesEditionAction {

	public UpdatesEditionIndividualEmbedAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public UpdatesEditionIndividualEmbedAction(ActionInitVO arg0) {
		super(arg0);
	}
	

	/*
	 * (non-Javadoc)
	 * @see com.biomed.smarttrak.action.UpdatesEditionAction#packageDataForDisplay(com.siliconmtn.data.Tree, java.util.List)
	 */
	@Override
	protected void packageDataForDisplay(Tree t, List<UpdateVO> updates) {
		Map<String, Map<String, List<UpdateVO>>> dataMap = new LinkedHashMap<>();

		for (Node n : t.getRootNode().getChildren()) {
			boolean hasChildren = false;
			Map<String, List<UpdateVO>> children = new LinkedHashMap<>();
			if(n.getTotalChildren() > 0) {
				for(Node c : n.getChildren()) {
					if(c.getTotalChildren() > 0) {
						children.put(c.getNodeName(), (List<UpdateVO>)c.getUserObject());
						hasChildren = true;
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