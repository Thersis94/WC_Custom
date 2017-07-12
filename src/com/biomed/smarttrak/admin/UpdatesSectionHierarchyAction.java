package com.biomed.smarttrak.admin;

//jdk 1.8.x
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


//wc_custom libs
import com.biomed.smarttrak.action.UpdatesWeeklyReportAction;
import com.biomed.smarttrak.vo.UpdateVO;
import com.biomed.smarttrak.vo.UpdateXRVO;
import com.biomed.smarttrak.vo.UserVO;

//smt base libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.DateUtil;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * Title: UpdateSectionHierarchyAction.java <p/>
 * Project: WC_Custom <p/>
 * Description: Facade-like action responsible for combining the section hierarchy 
 * data along with the associated updates weekly reports data.<p/>
 * Copyright: Copyright (c) 2017<p/>
 * Company: Silicon Mountain Technologies<p/>
 * @author Devon Franklin
 * @version 1.0
 * @since Mar 9, 2017
 *   Updates - Included the root node in listing. Removed intended depth level value 
 *   to allow full hierarchy searching. 06/23/17
 ****************************************************************************/
public class UpdatesSectionHierarchyAction extends AbstractTreeAction {
	public static final String PROFILE_ID = "profileId";

	/**
	 * No arg-constructor for initialization
	 */
	public UpdatesSectionHierarchyAction() {
		super();
	}

	/**
	 * Initializes class with ActionInitVO
	 * @param init
	 */
	public UpdatesSectionHierarchyAction(ActionInitVO init) {
		super(init);
	}

	/*
	 * (non-Javadoc)
	 * @see com.biomed.smarttrak.admin.SectionHierarchyAction#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		//pass to superclass for portlet registration (WC admintool)
		super.retrieve(req);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void retrieve(ActionRequest req) throws ActionException{		
		log.debug("Retrieving updates section hierarchy listing...");

		//load the core section hierarchy
		Tree t = loadDefaultTree();

		//load the updates that should be displayed
		List<UpdateVO> updates = fetchUpdates(req);
		log.debug("Number of updates retrieved: " + updates.size());

		//loop the updates and create a Node on the hierarchy for each of their parent levels (Update Type)
		for (UpdateVO vo : updates) {
			List<UpdateXRVO> secs = vo.getUpdateSections();
			if (secs == null || secs.isEmpty()) continue;
			for (UpdateXRVO xrvo : secs) {
				log.debug("sec=" + vo.getTitle() + " " + xrvo.getSectionId());
				//find at deepest a section level 3 node.  We may be dealing with a level 4 here
				Node secNode = t.findNode(xrvo.getSectionId());
				while (secNode.getDepthLevel() > 3) {
					secNode = t.findNode(secNode.getParentId());
					log.debug("found  parent " + secNode);
				}

				log.debug("secNode =" + secNode.getNodeName() + " depth=" + secNode.getDepthLevel());
				//add a node to the stack for the Type - NodeID must be unique (contextualize it w/sectionId)
				Node typeNode = t.findNode(secNode.getNodeName()+vo.getTypeNm());
				if (typeNode == null) {
					typeNode = new Node(secNode.getNodeName()+vo.getTypeNm(), secNode.getNodeId());
					typeNode.setNodeName(vo.getTypeNm());
					typeNode.setOrderNo(vo.getTypeCd());
					log.debug("adding node for type: " + typeNode);
					t.findNode(typeNode.getParentId()).addChild(typeNode);
				}
			}
		}

		//iterate the hierarchy and set an easy-to-reference full path we can use to merge the data.
		//while there, add a 4th level that equates to the Update Types.
		//We'll them attach the updates themselves, as the lowest level.
		t = marryUpdatesToNodes(t, updates);
		
		//sortNodes(t.getRootNode());
		
		if (log.isDebugEnabled()) {
			for (Node n : t.preorderList())
				log.debug(n);
		}

		//set the appropriate time range onto request for view
		setDateRange(req);

		putModuleData(t, updates.size(), false);
		
		//if this is for the email, format the data as a Map<String, Integer>() containing the root levels
		if (req.getAttribute("isWebpage") == null) {
			formatDataMap(t);
		}
	}

	/**
	 * @param t
	 */
	private void formatDataMap(Tree t) {
		Map<String, Integer> counts = new HashMap<>();
		for (Node n : t.getRootNode().getChildren()) {
			if (n.getTotalChildren() > 0)
				counts.put(n.getNodeName(), n.getTotalChildren());
			log.debug(n.getNodeName() + " =" +  n.getTotalChildren());
		}
		putModuleData(counts, counts.size(), false);
	}

	/**
	 * recursively traverses the levels of the Tree, and at each one sorts the items by Update Type.
	 * @param t
	 */
	private void sortNodes(Node r) {
		if(!r.isLeaf()) {
			Collections.sort(r.getChildren());
			for(Node n : r.getChildren()) {
				sortNodes(n);
			}
		}
	}

	/**
	 * @param t
	 * @param updates
	 */
	private Tree marryUpdatesToNodes(Tree t, List<UpdateVO> updates) {
		//iterate the node tree.  At each level look for updates that belong there.  
		//Compile a list and attach it to the given Node.  Then, preclude that Update from being re-displayed in the same top-level section
		for (Node n : t.getRootNode().getChildren()) {
			//maintain a list of updates already tied to this root section - they cannot appear here twice.
			Set<String> exclusions = new HashSet<>();
			iterateUpdatesForNode(n, t, updates, exclusions);
			n.setTotalChildren(exclusions.size());
			log.debug("root " + n.getNodeName() + " has " + n.getTotalChildren());
		}
		return t;
	}


	/**
	 * Note this method is recursive - run spirals down each of the top-level hierarchy sections.
	 * @param n
	 * @param exclusions
	 * @param secUpds
	 */
	@SuppressWarnings("unchecked")
	private void iterateUpdatesForNode(Node n, Tree t, List<UpdateVO> updates, Set<String> exclusions) {
		log.debug("depth= " + n.getDepthLevel() + " name=" + n.getNodeName());
		List<UpdateVO> secUpds = new ArrayList<>();
		for (UpdateVO vo : updates) {
			List<UpdateXRVO> secs = vo.getUpdateSections();
			if (exclusions.contains(vo.getUpdateId()) || secs == null || secs.isEmpty()) continue;
			for (UpdateXRVO xrvo : secs) {
				if (n.getNodeId().equals(xrvo.getSectionId())) {
					secUpds.add(vo);
					log.debug(vo.getUpdateId() + " is comitted to " + n.getNodeName() + " &par=" + n.getParentId());
					exclusions.add(vo.getUpdateId());
				}
			}
		}
		//if depth is 4 then give these to our parent, level 3
		if (4 == n.getDepthLevel()) {
			Node par = t.findNode(n.getParentId());
			List<UpdateVO> data = (List<UpdateVO>) par.getUserObject();
			if (data == null) data = new ArrayList<>();
			data.addAll(secUpds);
			par.setUserObject(data);
			par.setTotalChildren(data.size());
		} else {
			n.setUserObject(secUpds);
			n.setTotalChildren(secUpds.size());
		}
		log.debug("saved " + n.getNodeName() + " has " + n.getTotalChildren());

		//dive deeper into this node's children
		for (Node child : n.getChildren())
			this.iterateUpdatesForNode(child, t, updates, exclusions);

	}


	/**
	 * Retrieves the correct updates, either scheduled or general list of updates
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	@SuppressWarnings("unchecked")
	protected List<UpdateVO> fetchUpdates(ActionRequest req) throws ActionException{
		ActionInterface actInf;

		//grab the profile id from the request or session
		String profileId = getProfileId(req);

		//if profile id is present, return the list of scheduled updates

		if (profileId != null) {
			actInf =  new UpdatesScheduledAction();
		} else { //retrieve the list of daily/weekly updates
			req.setParameter(UpdatesWeeklyReportAction.EMAIL_UPDATES, "true");
			actInf = new UpdatesWeeklyReportAction();
		}
		actInf.setAttributes(attributes);
		actInf.setDBConnection(dbConn);
		actInf.retrieve(req);
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		return (List<UpdateVO>) mod.getActionData();
	}

	/**
	 * Grabs the profileId from either the request or session if available
	 * @param req
	 * @return
	 */
	protected String getProfileId(ActionRequest req){
		String profileId = null;

		//This means that a unique send(no account was used) has occurred, simply return
		if(Convert.formatBoolean(req.getParameter("uniqueSendFlg"))){
			return profileId;
		}

		if(req.hasParameter(PROFILE_ID)){
			profileId = req.getParameter(PROFILE_ID);
		}else{
			SMTSession ses = req.getSession();
			UserVO user = (UserVO) ses.getAttribute(Constants.USER_DATA);
			if(user != null){
				profileId = user.getProfileId();
				req.setParameter(PROFILE_ID, profileId); //place on request for downstream
				//set their status code on request
				req.setAttribute("statusCode", user.getStatusCode());
				req.setAttribute("isWebpage", "1");
			}	
		}
		return profileId;
	}


	/**
	 * Sets the appropriate time range value to the request
	 * @param req
	 */
	protected void setDateRange(ActionRequest req) {
		//leave the data alone if set by another action (UpdatesScheduledAction)
		if (req.getAttribute("dateRange") != null) return;

		String timeRangeCd = StringUtil.checkVal(req.getParameter("timeRangeCd"));
		String dateRange = null;

		//determine the date range
		if(UpdatesWeeklyReportAction.TIME_RANGE_WEEKLY.equalsIgnoreCase(timeRangeCd)){
			dateRange = DateUtil.previousWeek(DateFormat.MEDIUM);
		}else{
			dateRange = DateUtil.getDate(-1, DateFormat.MEDIUM);
		}

		req.setAttribute("dateRange", dateRange);
	}

	/*
	 * (non-Javadoc)
	 * @see com.biomed.smarttrak.admin.AbstractTreeAction#getCacheKey()
	 */
	@Override
	public String getCacheKey() {
		return null;
	}
}
