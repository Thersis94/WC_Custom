package com.biomed.smarttrak.admin;

//jdk 1.8.x
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
		//String sectionNm = StringUtil.checkVal(req.getParameter("sectionNm"));

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

		//set the appropriate time range onto request for view
		setDateRange(req);

		putModuleData(t, updates.size(), false);
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
	 * Retrieves a tree collection of nodes based on the sub-root sections. Each
	 * tree contains each sub-root section's hierarchy.
	 * @return
	 
	protected Map<String, Tree> retrieveTreeCollection(String sectionName){
		Map<String, Tree> collection = new LinkedHashMap<>();

		//load the tree once
		Tree t = loadDefaultTree();

		for (Node node : t.getRootNode().getChildren()) {
			//if section name is valid, retrieve only that section of tree
			if(!sectionName.isEmpty()){
				if(node.getNodeName().equalsIgnoreCase(sectionName)){
					addSectionTree(node, collection);
					break;					
				}
			}else{
				addSectionTree(node, collection);
			}
		}
		return collection;
	}*/

	/**
	 * Helper method that assigns a new section Tree to the given collection
	 * @param node
	 * @param collection
	
	protected void addSectionTree(Node node, Map<String, Tree> collection){
		Tree sectionTree = new Tree(node.getChildren());
		sectionTree.setRootNode(node);
		collection.put(node.getNodeId(), sectionTree);
	} */
	//
	//	/**
	//	 * Builds a proper collection of updates with their corresponding section(s)
	//	 * that they belong to.
	//	 * @param req
	//	 * @param treeCollection
	//	 * @return
	//	 * @throws ActionException
	//	 */
	//	protected Map<String, Map<Node, List<UpdateVO>>> buildUpdatesHierarchy(ActionRequest req, Tree t) throws ActionException{
	//		Map<String, Map<Node, List<UpdateVO>>> updatesHierarchyMap = new LinkedHashMap<>();
	//
	//
	//		//Create a brand new collection for each tree for tracking updates
	//		masterCollection = new HashMap<>();
	//
	//		//iterate through each section tree hierarchy and add the corresponding update(s)
	//		for (Node n : t.getRootNode().getChildren()) {
	//			log.debug("starting " + n.getNodeId());
	//			String rootSectionId = n.getNodeName();
	//
	//			//add root section id, with sub-section/updates, to the final collection
	//			//only if it's sub-section map is not empty
	//			Map<Node, List<UpdateVO>> subSectionMap = getSubSectionUpdates(n.getChildren(), updates);
	//			if (!subSectionMap.isEmpty()) {
	//				//prune the sub section map of any "meaningfully" similar updates
	//				pruneSection(subSectionMap);
	//
	//				//add to final collection
	//				updatesHierarchyMap.put(rootSectionId, subSectionMap);
	//			}
	//
	//		}
	//		return updatesHierarchyMap;
	//	}
	//
	//	/**
	//	 * Returns a mapping of a sub section with it's associated updates.
	//	 * @param nodes
	//	 * @param updates
	//	 * @return
	//	 */
	//	protected Map<Node, List<UpdateVO>> getSubSectionUpdates(List<Node> nodes, List<UpdateVO> updates) {
	//		Map<Node, List<UpdateVO>> subSecUpdates = new LinkedHashMap<>();
	//
	//		for (Node node : nodes) { //level 2 nodes - OrthoGPS, RegenGPS, etc.
	//			List<UpdateVO> data = findSectionUpdates(node, updates);
	//			if (data == null || data.isEmpty()) continue;
	//
	//			if (node.getDepthLevel() > 3){
	//				node.setNodeName(node.getParentName());
	//			}
	//			//associate sub sub level updates
	//			subSecUpdates.put(node, data);
	//			log.debug("added " + data.size() + "  to  " + node.getNodeName());
	//		}
	//
	//		return subSecUpdates;
	//	}
	//
	//	/***
	//	 * Helper method that returns a list of related updates for a specific section 
	//	 * hierarchy
	//	 * @param updateSections
	//	 * @param node
	//	 * @param update
	//	 * @return
	//	 */
	//	private List<UpdateVO> findSectionUpdates(Node node, List<UpdateVO> updates){
	//		List<UpdateVO> data = new ArrayList<>();
	//
	//		//for each section that each update is tied to - pinpoint the ones which correspond to this Node.
	//		Iterator<UpdateVO> iter = updates.iterator();
	//		while (iter.hasNext()) {
	//			UpdateVO update = iter.next();
	//			for (UpdateXRVO xrvo : update.getUpdateSections()) {
	//				if (node.getNodeId().equals(xrvo.getSectionId())) {
	//					//track any applicable updates
	//					data.add(update);
	//					iter.remove(); //remove it from the list of updates - we only want to display it once.
	//				}
	//			}
	//		}
	//
	//		return data;
	//	}


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
			}	
		}
		return profileId;
	}
	//
	//	/**
	//	 * Searches over the section map for similar updates for relevant hierarchy sections
	//	 * @param sectionUpdates
	//	 */
	//	private void pruneSection(Map<Node, List<UpdateVO>> sectionUpdates) {	
	//		for (Entry<Node, List<UpdateVO>> entry : sectionUpdates.entrySet()) {
	//			Node currentNode = entry.getKey();
	//
	//			/*Do NOT prune updates from any entry with depth level 3. This is currently 
	//			 *the only depth level that will display unique headings(view-wise) 
	//			 *and allows contextually similar updates */
	//			if (currentNode.getDepthLevel() != UNIQUE_DEPTH_LEVEL)
	//				pruneUpdates(entry.getValue());
	//		}
	//	}
	//
	//	/**
	//	 * Removes any updates from the passed collection based on associated depth level
	//	 * @param updateCollection
	//	 */
	//	private void pruneUpdates(List<UpdateVO> updateCollection){
	//		log.debug("size=" + updateCollection.size());
	//		for (int i =0; i < updateCollection.size(); i++) {
	//			UpdateVO update = updateCollection.get(i);
	//
	//			//grab the update from the master collection
	//			UpdateTrackerVO tracker = masterCollection.get(update.getUpdateId());
	//
	//			//determine if update a unique level, if so prune it from ALL other sections
	//			if (tracker.isUniqueLevel()) {
	//				updateCollection.remove(update);
	//			} else if (tracker.getTrackingCount() > 1) {
	//				/*otherwise only remove it up to (x)number of additional times it repeats
	//				 * throughout all other sections. Leaving only one occurrence.*/
	//				updateCollection.remove(update);
	//				tracker.setTrackingCount(tracker.getTrackingCount() - 1);
	//			}
	//		}
	//	}

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
