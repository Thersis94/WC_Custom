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
import com.biomed.smarttrak.vo.SectionVO;
import com.biomed.smarttrak.vo.UpdatesVO;
import com.biomed.smarttrak.vo.UpdatesXRVO;

//smt base libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.orm.DBProcessor;
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
	
	/**
	 * No arg-constructor for initialization
	 */
	public UpdatesSectionHierarchyAction(){
		super();
	}
	
	/**
	 * Initializies class with ActionInitVO
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
		
		//Retrieve root level sections. 
		List<SectionVO> rootSections = fetchSubRootList();
		
		/*Create separate trees structures for all of the 
		*root level sections. This will create our groupings. */
		Map<String, Tree> treeCollection = retrieveTreeCollection(rootSections);
		
		//Add the updates to appropriate groupings
		Map<List<Node>, List<UpdatesVO>> data = buildUpdatesHierarchy(req, treeCollection);
		
		putModuleData(data);
	}
	
	
	/**
	 * Retrieves the listing of sections that are direct children of root
	 * @return
	 */
	protected List<SectionVO> fetchSubRootList(){
		StringBuilder sql = new StringBuilder(200);
		sql.append("select * from ");
		sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("biomedgps_section a ");
		sql.append("where parent_id = ? ");
		sql.append("order by parent_id, order_no, section_nm");
		
		List<Object> vals = new ArrayList<>();
		vals.add("MASTER_ROOT"); //get direct children sections
		
		//retrieve data from db processor
		DBProcessor db = new DBProcessor(dbConn);
		List<Object> data = db.executeSelect(sql.toString(), vals, new SectionVO());
		
		//cast back into SectionVO's
		List<SectionVO> rootSections = new ArrayList<>();
		for (Object object : data) {
			rootSections.add( (SectionVO) object);
		}
		return rootSections;
	}
	
	/**
	 * Retrieves a tree collection of nodes based on the sub-root sections. Each
	 * tree contains each sub-root section's hierarchy.
	 * @param roots
	 * @return
	 */
	protected Map<String, Tree> retrieveTreeCollection(List<SectionVO> roots){
		Map<String, Tree> collection = new LinkedHashMap<>();
		
		//set the sectionId to specify the root node for each of our tree structures
		for (SectionVO root : roots) {
			//add to collection
			collection.put(root.getSectionId(), loadTree(root.getSectionId()));
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
	protected Map<List<Node>, List<UpdatesVO>> buildUpdatesHierarchy(ActionRequest req, 
			Map<String, Tree> treeCollection) throws ActionException{
		Map<List<Node>, List<UpdatesVO>> updatesHierarchyMap = new LinkedHashMap<>();
		
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
			
			//locate the related updates
			List<UpdatesVO> relatedUpdates = locateSectionUpdates(updates, t);
			
			//get the current section's list of nodes from tree
			List<Node> subRootSection = t.preorderList(true);
			
			//add this tree, with updates, to the final collection
			updatesHierarchyMap.put(subRootSection, relatedUpdates);
		}
		return updatesHierarchyMap;
	}
	
	/***
	 * Helper method that returns a list of related updates for a specific section 
	 * hierarchy
	 * @param updateSections
	 * @param t
	 * @param update
	 * @return
	 */
	private List<UpdatesVO> locateSectionUpdates(List<UpdatesVO> updates, Tree tree){
		List<UpdatesVO> relatedUpdates = new ArrayList<>();
		
		for (UpdatesVO update : updates) {
			//get list of sections for this update
			List<UpdatesXRVO>updateSections = update.getXRSections();
			
			/*Attempt to find section within tree structure. If so the current 
			 * update belongs to this tree.*/			
			for (UpdatesXRVO updatesXRVO : updateSections) {						
				if(updatesXRVO.getSectionId() != null){
					Node node = tree.findNode(updatesXRVO.getSectionId());
					if(node != null) relatedUpdates.add(update);
				}
			}			
		}
		
		return relatedUpdates;
	}
}
