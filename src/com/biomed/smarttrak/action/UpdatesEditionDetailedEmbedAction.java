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
import com.siliconmtn.html.tool.RegexParser;
import com.siliconmtn.html.tool.RegexParser.Patterns;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

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

	private String absoluteUrl = "https://app.smarttrak.com";

	public UpdatesEditionDetailedEmbedAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public UpdatesEditionDetailedEmbedAction(ActionInitVO arg0) {
		super(arg0);
	}


	/*
	 * (non-Javadoc)
	 * @see com.biomed.smarttrak.action.UpdatesEditionAction#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve (ActionRequest req) throws ActionException {
		req.setParameter("orderSort", "true");
		req.setParameter("redirectLinks", "true");
		super.retrieve(req);
	}

	/*
	 * (non-Javadoc)
	 * @see com.biomed.smarttrak.action.UpdatesEditionAction#packageDataForDisplay(com.siliconmtn.data.Tree, java.util.List)
	 */
	@Override
	protected void packageDataForDisplay(Tree t, List<UpdateVO> updates) {
		Map<String, Map<String, List<UpdateVO>>> dataMap = new LinkedHashMap<>();

		for (Node n : t.getRootNode().getChildren()) {
			Map<String, List<UpdateVO>> children = new LinkedHashMap<>();

			packageRootNode(n, children);

			//Only add Node if we actually have children.
			if (!children.isEmpty())
				dataMap.put(n.getNodeName(), children);
		}

		//change any relative links to be absolute - important for emailing
		for (Map.Entry<String,Map<String, List<UpdateVO>>> entry : dataMap.entrySet()) {
			for (Map.Entry<String, List<UpdateVO>> entry2 : entry.getValue().entrySet()) {
				externalizeLinks(entry2.getValue());
			}
		}

		putModuleData(dataMap, dataMap.size(), false);
	}


	/**
	 * abstraction to simplify complex method
	 * @param n
	 * @param children
	 */
	@SuppressWarnings("unchecked")
	private void packageRootNode(Node n, Map<String, List<UpdateVO>> children) {
		//Look for Updates tied to the Root Node.
		Object o = n.getUserObject();
		if(o != null && o instanceof List && !((List<?>)o).isEmpty()) {
			children.put("root", (List<UpdateVO>)o);
		}
		if (n.getTotalChildren() == 0) return;

		//Look for Updates on the Child Nodes.
		for(Node c : n.getChildren()) {
			if(c.getTotalChildren() > 0) {
				children.put(c.getNodeName(), (List<UpdateVO>)c.getUserObject());
			}
		}
	}


	/**
	 * iterates the list of updates and converts relative URLs to absolute - for embedding in the emails
	 * @param value
	 */
	private void externalizeLinks(List<UpdateVO> updates) {
		if (updates == null || updates.isEmpty()) return;
		for (UpdateVO vo : updates) {
			if (!StringUtil.isEmpty(vo.getMessageTxt()))
				vo.setMessageTxt(RegexParser.regexReplace(Patterns.ABS_HREF_PATHS, vo.getMessageTxt(), absoluteUrl));
		}	
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


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractBaseAction#setAttributes(java.util.Map)
	 */
	@Override
	public void setAttributes(Map<String, Object> attr) {
		super.setAttributes(attr);
		//see if an override was passed for the absoluteUrl - leave it as-is otherwise
		absoluteUrl = StringUtil.checkVal(attr.get("smarttrakAbsBaseUrl"), absoluteUrl);
	}
}