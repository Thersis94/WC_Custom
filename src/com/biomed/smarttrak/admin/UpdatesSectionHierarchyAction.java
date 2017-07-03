package com.biomed.smarttrak.admin;

//jdk 1.8.x
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


//wc_custom libs
import com.biomed.smarttrak.action.UpdatesWeeklyReportAction;
import com.biomed.smarttrak.vo.UpdateTrackerVO;
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
	private Map<String, UpdateTrackerVO> masterCollection;
	public static final int UNIQUE_DEPTH_LEVEL = 3;
	public static final int TRACKING_DEPTH_LEVEL = 2;
	public static final String PROFILE_ID = "profileId";

	/**
	 * No arg-constructor for initialization
	 */
	public UpdatesSectionHierarchyAction(){
		super();
	}

	/**
	 * Initializes class with ActionInitVO
	 * @param init
	 */
	public UpdatesSectionHierarchyAction(ActionInitVO init){
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
	@Override
	public void retrieve(ActionRequest req) throws ActionException{		
		log.debug("Retrieving updates section hierarchy listing...");
		String sectionNm = StringUtil.checkVal(req.getParameter("sectionNm"));

		/*Create separate trees structures for all of the 
		 *root level sections. This will create our groupings. */
		Map<String, Tree> treeCollection = retrieveTreeCollection(sectionNm);

		//Add the updates to appropriate groupings
		Map<String, Map<Node, List<UpdateVO>>> data = buildUpdatesHierarchy(req, treeCollection);	

		//set the appropriate time range onto request for view
		setDateRange(req);

		putModuleData(data);
	}

	/**
	 * Retrieves a tree collection of nodes based on the sub-root sections. Each
	 * tree contains each sub-root section's hierarchy.
	 * @return
	 */
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
	}

	/**
	 * Helper method that assigns a new section Tree to the given collection
	 * @param node
	 * @param collection
	 */
	protected void addSectionTree(Node node, Map<String, Tree> collection){
		Tree sectionTree = new Tree(node.getChildren());
		sectionTree.setRootNode(node);
		collection.put(node.getNodeId(), sectionTree);
	}

	/**
	 * Builds a proper collection of updates with their corresponding section(s)
	 * that they belong to.
	 * @param req
	 * @param treeCollection
	 * @return
	 * @throws ActionException
	 */
	protected Map<String, Map<Node, List<UpdateVO>>> buildUpdatesHierarchy(ActionRequest req, 
			Map<String, Tree> treeCollection) throws ActionException{
		Map<String, Map<Node, List<UpdateVO>>> updatesHierarchyMap = new LinkedHashMap<>();

		//get list of updates
		List<UpdateVO> updates = fetchUpdates(req);
		log.debug("Number of updates retrieved: " + updates.size());

		//iterate through each section tree hierarchy and add the corresponding update(s)
		Set<Entry<String, Tree>> treeSet = treeCollection.entrySet();
		for (Entry<String, Tree> entry : treeSet) {
			log.debug("starting " + entry.getKey());
			Tree t = entry.getValue();
			String rootSectionId = t.getRootNode().getNodeName();

			//Create a brand new collection for each tree for tracking updates
			masterCollection = new HashMap<>();

			//Build the mapping for top-level sections, sub-sections, and updates
			List<Node> nodes = t.preorderList(true);

			//add root section id, with sub-section/updates, to the final collection
			//only if it's sub-section map is not empty
			Map<Node, List<UpdateVO>> subSectionMap = getSubSectionUpdates(nodes, updates);
			if (!subSectionMap.isEmpty()) {
				//prune the sub section map of any "meaningfully" similar updates
				pruneSection(subSectionMap);

				//add to final collection
				updatesHierarchyMap.put(rootSectionId, subSectionMap);
			}	
		}
		return updatesHierarchyMap;
	}

	/**
	 * Returns a mapping of a sub section with it's associated updates.
	 * @param nodes
	 * @param updates
	 * @return
	 */
	protected Map<Node, List<UpdateVO>> getSubSectionUpdates(List<Node> nodes, List<UpdateVO> updates){
		Map<Node, List<UpdateVO>> subSectionMap = new LinkedHashMap<>();

		for (Node node : nodes) {
			//locate the related updates and add to map
			List<UpdateVO> data = locateSectionUpdates(node, updates);
			if(!data.isEmpty()) {
				//associate sub sub level updates
				subSectionMap.put(node, data);
				log.debug("added " + data.size() + "  to  " + node.getNodeName());
			}
		}

		return subSectionMap;
	}

	/***
	 * Helper method that returns a list of related updates for a specific section 
	 * hierarchy
	 * @param updateSections
	 * @param node
	 * @param update
	 * @return
	 */
	private List<UpdateVO> locateSectionUpdates(Node node, List<UpdateVO> updates){
		List<UpdateVO> relatedUpdates = new ArrayList<>();

		for (UpdateVO update : updates)
			associateUpdates(update, relatedUpdates, node);

		return relatedUpdates;
	}

	/**
	 * Helper method, handles associating updates to their related sections
	 * @param update
	 * @param holder
	 * @param nodeToMatch
	 */
	private void associateUpdates(UpdateVO update, List<UpdateVO> data, Node node) {
		// Attempt to match section to the current update. If so the current update belongs to this section.
		String nodeId = node.getNodeId();
		for (UpdateXRVO xrvo : update.getUpdateSections()) {
			if (xrvo.getSectionId() != null && nodeId.equals(xrvo.getSectionId())) {
				//track any applicable updates
				trackUpdates(node, update);
				data.add(update);
			}
		}
	}

	/**
	 * Handles tracking of relevant updates to master collection
	 * @param nodeToMatch
	 * @param update
	 */
	private void trackUpdates(Node node, UpdateVO update) {
		if (node.getDepthLevel() < TRACKING_DEPTH_LEVEL) return;

		//create the status entry for the update
		boolean isUniqueLevel = node.getDepthLevel() == UNIQUE_DEPTH_LEVEL;

		//grab existing element from collection, otherwise update it's attributes
		UpdateTrackerVO tracker = masterCollection.get(update.getUpdateId());
		if (tracker == null) {
			tracker = new UpdateTrackerVO(update.getUpdateId(), 1, isUniqueLevel);
		} else {
			tracker.setTrackingCount(tracker.getTrackingCount() + 1);
			if (!tracker.isUniqueLevel()) { //only override if not already unique
				tracker.setHasUniqueLevel(isUniqueLevel);
			}
		}
		//add to master collection
		masterCollection.put(update.getUpdateId(), tracker);
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

		if(profileId != null){
			actInf =  new UpdatesScheduledAction();
		}else{//retrieve the list of daily/weekly updates
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

	/**
	 * Searches over the section map for similar updates for relevant hierarchy sections
	 * @param sectionUpdates
	 */
	private void pruneSection(Map<Node, List<UpdateVO>> sectionUpdates) {	
		for (Entry<Node, List<UpdateVO>> entry : sectionUpdates.entrySet()) {
			Node currentNode = entry.getKey();

			/*Do NOT prune updates from any entry with depth level 3. This is currently 
			 *the only depth level that will display unique headings(view-wise) 
			 *and allows contextually similar updates */
			if (currentNode.getDepthLevel() != UNIQUE_DEPTH_LEVEL)
				pruneUpdates(entry.getValue());
		}
	}

	/**
	 * Removes any updates from the passed collection based on associated depth level
	 * @param updateCollection
	 */
	private void pruneUpdates(List<UpdateVO> updateCollection){
		log.debug("size=" + updateCollection.size());
		for (int i =0; i < updateCollection.size(); i++) {
			UpdateVO update = updateCollection.get(i);

			//grab the update from the master collection
			UpdateTrackerVO tracker = masterCollection.get(update.getUpdateId());

			//determine if update a unique level, if so prune it from ALL other sections
			if (tracker.isUniqueLevel()) {
				updateCollection.remove(update);
			} else if (tracker.getTrackingCount() > 1) {
				/*otherwise only remove it up to (x)number of additional times it repeats
				 * throughout all other sections. Leaving only one occurrence.*/
				updateCollection.remove(update);
				tracker.setTrackingCount(tracker.getTrackingCount() - 1);
			}
		}
	}

	/**
	 * Sets the appropriate time range value to the request
	 * @param req
	 */
	protected void setDateRange(ActionRequest req){
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
