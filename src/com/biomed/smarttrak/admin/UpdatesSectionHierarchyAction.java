package com.biomed.smarttrak.admin;

//jdk 1.8.x
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

//wc_custom libs
import com.biomed.smarttrak.action.UpdatesWeeklyReportAction;
import com.biomed.smarttrak.vo.UpdateVO;
import com.biomed.smarttrak.vo.UpdateXRVO;

//smt base libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
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
 ****************************************************************************/

public class UpdatesSectionHierarchyAction extends AbstractTreeAction {
	/*Specifies how deep down the section hierarchy tree we intend to traverse*/
	protected static final int SECTION_XR_DEPTH = 3;
	
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
	public void retrieve(ActionRequest req) throws ActionException{
		log.debug("Retrieving updates section hierarchy listing...");
	
		/*Create separate trees structures for all of the 
		*root level sections. This will create our groupings. */
		Map<String, Tree> treeCollection = retrieveTreeCollection();

		//Add the updates to appropriate groupings
		Map<String, Map<String, List<UpdateVO>>> data = buildUpdatesHierarchy(req, treeCollection);	
		
		putModuleData(data);
	}
	
	/**
	 * Retrieves a tree collection of nodes based on the sub-root sections. Each
	 * tree contains each sub-root section's hierarchy.
	 * @return
	 */
	protected Map<String, Tree> retrieveTreeCollection(){
		Map<String, Tree> collection = new LinkedHashMap<>();
		
		//load the tree once
		Tree t = loadDefaultTree();
		
		for (Node node : t.getRootNode().getChildren()) {
			Tree sectionTree = new Tree(node.getChildren());
			sectionTree.setRootNode(node);
			collection.put(node.getNodeId(), sectionTree);
		}
		return collection;
	}
	
	/**
	 * Builds a proper collection of updates with their corresponding section(s)
	 * that they belong to.
	 * @param req
	 * @param treeCollection
	 * @return
	 * @throws ActionException
	 */
	protected Map<String, Map<String, List<UpdateVO>>> buildUpdatesHierarchy(ActionRequest req, 
			Map<String, Tree> treeCollection) throws ActionException{
		Map<String, Map<String, List<UpdateVO>>> updatesHierarchyMap = new LinkedHashMap<>();
		
		//get list of updates
		List<UpdateVO> updates = fetchUpdates(req);
		log.debug("Number of updates retrieved: " + updates.size());

		//iterate through each section tree hierarchy and add the corresponding update(s)
		Set<Entry<String, Tree>> treeSet = treeCollection.entrySet();
		for (Entry<String, Tree> entry : treeSet) {
			Tree t = entry.getValue();
			String rootSectionId = t.getRootNode().getNodeName();
			
			//Build the mapping for top-level sections, sub-sections, and updates
			List<Node> nodes = t.preorderList();
			
			//add root section id, with sub-section/updates, to the final collection
			//only if it's sub-section map is not empty
			Map<String, List<UpdateVO>> subSectionMap = getSubSectionUpdates(nodes, updates);
			if(!subSectionMap.isEmpty()) 
				updatesHierarchyMap.put(rootSectionId, subSectionMap);
		}
		return updatesHierarchyMap;
	}
	
	/**
	 * Returns a mapping of a sub section with it's associated updates.
	 * @param nodes
	 * @param updates
	 * @return
	 */
	protected Map<String, List<UpdateVO>> getSubSectionUpdates(List<Node> nodes, List<UpdateVO> updates){
		Map<String, List<UpdateVO>> subSectionMap = new LinkedHashMap<>();
		for (Node node : nodes) {
			if(SECTION_XR_DEPTH == node.getDepthLevel() ){		
				//locate the related updates and add to map
				List<UpdateVO> holder = locateSectionUpdates(updates, node);
				if(!holder.isEmpty()) subSectionMap.put(node.getNodeName(), holder);
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
	private List<UpdateVO> locateSectionUpdates(List<UpdateVO> updates, Node node){
		List<UpdateVO> relatedUpdates = new ArrayList<>();
		
		for (UpdateVO update : updates) {
			associateUpdates(update, relatedUpdates, node);
		}
		return relatedUpdates;
	}
	
	/**
	 * Helper method, handles associating updates to their related sections
	 * @param update
	 * @param holder
	 * @param nodeToMatch
	 */
	private void associateUpdates(UpdateVO update, List<UpdateVO> holder,
			Node nodeToMatch){
		//get list of sections for this update
		List<UpdateXRVO>updateSections = update.getUpdateSections();
		
		/*Attempt to match section to the current update. If so the current 
		 * update belongs to this section.*/			
		for (UpdateXRVO updatesXRVO : updateSections) {
			if(updatesXRVO.getSectionId() != null
					&& nodeToMatch.getNodeId().equals(updatesXRVO.getSectionId())){
				holder.add(update);
			}
		}
	}
	
	/**
	 * Retrieves the correct updates, either scheduled or general list of updates
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	@SuppressWarnings("unchecked")
	protected List<UpdateVO>fetchUpdates(ActionRequest req) throws ActionException{
		ActionInterface actInf;
		//if account id is present, return the list of scheduled updates
		if(req.hasParameter("accountId")){
			actInf =  new UpdatesScheduledAction();
		}else{//retrieve the list of daily/weekly updates
			actInf = new UpdatesWeeklyReportAction();
		}
		actInf.setAttributes(attributes);
		actInf.setDBConnection(dbConn);
		actInf.retrieve(req);
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		List<UpdateVO> updates = (List<UpdateVO>) mod.getActionData();
		
		return updates;
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
