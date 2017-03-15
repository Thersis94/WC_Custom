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
import com.biomed.smarttrak.vo.UpdatesVO;
import com.biomed.smarttrak.vo.UpdatesXRVO;

//smt base libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
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

public class UpdatesSectionHierarchyAction extends SectionHierarchyAction {
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
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	public void retrieve(ActionRequest req) throws ActionException{
		log.debug("Retrieving updates section hierarchy listing...");
	
		/*Create separate trees structures for all of the 
		*root level sections. This will create our groupings. */
		Map<String, Tree> treeCollection = retrieveTreeCollection();
		
		//Add the updates to appropriate groupings
		Map<String, Map<String, List<UpdatesVO>>> data = buildUpdatesHierarchy(req, treeCollection);
		
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
	@SuppressWarnings("unchecked")
	protected Map<String, Map<String, List<UpdatesVO>>> buildUpdatesHierarchy(ActionRequest req, 
			Map<String, Tree> treeCollection) throws ActionException{
		Map<String, Map<String, List<UpdatesVO>>> updatesHierarchyMap = new LinkedHashMap<>();
		
		//retrieve the list of daily/weekly updates
		UpdatesWeeklyReportAction uwr = new UpdatesWeeklyReportAction();
		uwr.setAttributes(attributes);
		uwr.setDBConnection(dbConn);
		uwr.retrieve(req);
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		List<UpdatesVO> updates = (List<UpdatesVO>) mod.getActionData();
		log.debug("Number of updates retrieved: " + updates.size());
		
		//iterate through each section tree hierarchy and add the corresponding update(s)
		Set<Entry<String, Tree>> treeSet = treeCollection.entrySet();
		for (Entry<String, Tree> entry : treeSet) {		
			Tree t = entry.getValue();
			String rootSectionId = t.getRootNode().getNodeName();
			
			//Build the mapping for top-level sections, sub-sections, and updates
			List<Node> nodes = t.preorderList();	
			Map<String, List<UpdatesVO>> subSectionMap = new LinkedHashMap<>();
			for (Node node : nodes) {
				if(SECTION_XR_DEPTH == node.getDepthLevel() ){					
					//locate the related updates and add to map
					subSectionMap.put(node.getNodeName(), locateSectionUpdates(updates, node));
				}		
			}
			//add root section id, with sub-section/updates, to the final collection
			updatesHierarchyMap.put(rootSectionId, subSectionMap);
		}
		return updatesHierarchyMap;
	}
	/***
	 * Helper method that returns a list of related updates for a specific section 
	 * hierarchy
	 * @param updateSections
	 * @param node
	 * @param update
	 * @return
	 */
	private List<UpdatesVO> locateSectionUpdates(List<UpdatesVO> updates, Node node){
		List<UpdatesVO> relatedUpdates = new ArrayList<>();
		
		for (UpdatesVO update : updates) {
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
	private void associateUpdates(UpdatesVO update, List<UpdatesVO> holder,
			Node nodeToMatch){		
		//get list of sections for this update
		List<UpdatesXRVO>updateSections = update.getUpdateSections();
		
		/*Attempt to match section to the current update. If so the current 
		 * update belongs to this section.*/			
		for (UpdatesXRVO updatesXRVO : updateSections) {						
			if(updatesXRVO.getSectionId() != null 
					&& nodeToMatch.getNodeId().equals(updatesXRVO.getSectionId())){
				holder.add(update);			
			}
		}			
	}
}
