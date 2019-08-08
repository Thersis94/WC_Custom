package com.biomed.smarttrak.admin;

import java.util.Map;

import com.biomed.smarttrak.util.SmarttrakTree;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> ManagementAction.java<br/>
 * <b>Description: Superclass to Smarttrak management actions.  Particularly Market/Company/Product 
 * which share similar functions.</b> 
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2017<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Jul 14, 2017
 ****************************************************************************/
public class ManagementAction extends SBActionAdapter {

	//common string constants to appease Sonarqube and avoid hard-coded duplication
	protected static final String LEFT_OUTER_JOIN = "left outer join ";
	protected static final String INNER_JOIN = "inner join ";
	protected static final String INSERT_INTO = "insert into ";
	
	protected String customDbSchema;
	
	AuthorAction authorAction;
	SectionHierarchyAction hierarchyAction;

	/**
	 * 
	 */
	public ManagementAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public ManagementAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	
	private void initAuthors() {
		authorAction = new AuthorAction();
		authorAction.setDBConnection(getDBConnection());
		authorAction.setAttributes(getAttributes());
	}
	
	
	private void initHierarchy() {
		hierarchyAction = new SectionHierarchyAction();
		hierarchyAction.setDBConnection(getDBConnection());
		hierarchyAction.setAttributes(getAttributes());
	}
	


	
	/**
	 * @return
	 */
	public SmarttrakTree loadDefaultTree() {
		//defer loading the action until we need it
		if (hierarchyAction == null) initHierarchy();
		
		// return the section hierarchy Tree from the hierarchy action
		SmarttrakTree t = hierarchyAction.loadDefaultTree();
		t.buildNodePaths();
		
		return t;
	}
	
	
	/**
	 * Load the full tree into the request
	 * @return
	 */
	public void loadFullTree(ActionRequest req) {
		//defer loading the action until we need it
		if (hierarchyAction == null) initHierarchy();
		hierarchyAction.loadFullTree(req);
	}


	/**
	 * @param req
	 */
	public void loadAuthors(ActionRequest req) {
		//defer loading the action until we need it
		if (authorAction == null) initAuthors();
		
		authorAction.loadAuthors(req);
	}
	


	/**
	 * @return
	 */
	protected Map<String, String> loadAuthorTitles(String profileId) {
		//defer loading the action until we need it
		if (authorAction == null) initAuthors();
		
		return authorAction.loadAuthorTitles(profileId);
	}

	@Override
	public void setAttributes(Map<String, Object> attr) {
		super.setAttributes(attr);
		customDbSchema = (String) attr.get(Constants.CUSTOM_DB_SCHEMA);
	}

	@Override
	public void setAttributes(Map<String, Object> attr, boolean shareAttributes) {
		super.setAttributes(attr, shareAttributes);
		customDbSchema = (String) attr.get(Constants.CUSTOM_DB_SCHEMA);
	}
}