package com.biomed.smarttrak.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.biomed.smarttrak.admin.AbstractTreeAction;
import com.biomed.smarttrak.admin.SectionHierarchyAction;
import com.biomed.smarttrak.admin.UpdatesWeeklyReportAction;
import com.biomed.smarttrak.security.SecurityController;
import com.biomed.smarttrak.util.BiomedLinkCheckerUtil;
import com.biomed.smarttrak.vo.UpdateVO;
import com.biomed.smarttrak.vo.UpdateVO.AnnouncementType;
import com.biomed.smarttrak.vo.UpdateXRVO;
import com.biomed.smarttrak.vo.UserVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionNotAuthorizedException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.DateUtil;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.util.solr.SecureSolrDocumentVO;
import com.smt.sitebuilder.util.solr.SecureSolrDocumentVO.Permission;

/****************************************************************************
 * <b>Title:</b> UpdatesEditionAction.java<br/>
 * <b>Description:</b> Generates the email-accompanying webpage, /updates-edition. 
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2017<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Aug 7, 2017
 ****************************************************************************/
public class UpdatesEditionAction extends SimpleActionAdapter {
	public static final String PROFILE_ID = "profileId";

	public UpdatesEditionAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public UpdatesEditionAction(ActionInitVO arg0) {
		super(arg0);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		
		// Check if this is a verification request from an email.
		// If it is then the user must be logged in to reach this point
		// and can be redirected out to the original link address here.
		if (req.hasParameter(UpdatesEditionDataLoader.REDIRECT_DEST)) {
			String redirect = StringEncoder.urlDecode(req.getParameter(UpdatesEditionDataLoader.REDIRECT_DEST).replace('|', '&'));
			sendRedirect(redirect, "", req);
			return;
		}
		
		
		log.debug("Retrieving Updates Edition listings");

		//check section permissions by name.
		//parent-level permissions are implied; if the user can see any child level they can see this section.
		String marketNm = req.getParameter("marketNm");
		if (!StringUtil.isEmpty(marketNm)) {
			String marketAcl = getMarketAcl(marketNm);
			checkUserHasMarketPermission(marketAcl, req); //this will throw ActionNotAuthorizedException when applicable
		}

		//load the core section hierarchy
		Tree t = loadDefaultTree();
		
		t.buildNodePaths(t.getRootNode(), SearchDocumentHandler.HIERARCHY_DELIMITER, false);
		//load the updates that should be displayed
		List<UpdateVO> updates = fetchUpdates(req);

		//adjust appropriate public links if applicable(updates weekly report)
		adjustContentLinks(updates, req);

		//loop the updates and create a Node on the hierarchy for each of their parent levels (Update Type)
		for (UpdateVO vo : updates) {
			List<UpdateXRVO> secs = vo.getUpdateSections();
			if (secs == null || secs.isEmpty()) continue;
			for (UpdateXRVO xrvo : secs) {
				//log.debug("sec=" + vo.getTitle() + " " + xrvo.getSectionId())
				//find at deepest a section level 3 node.  We may be dealing with a level 4 here
				Node secNode = t.findNode(xrvo.getSectionId());
				while (secNode.getDepthLevel() > 3) {
					secNode = t.findNode(secNode.getParentId());
					//log.debug("found  parent " + secNode)
				}

				//log.debug("secNode =" + secNode.getNodeName() + " depth=" + secNode.getDepthLevel())
				//add a node to the stack for the Type - NodeID must be unique (contextualize it w/sectionId)
				Node typeNode = t.findNode(secNode.getNodeName()+vo.getTypeNm());
				if (typeNode == null) {
					typeNode = new Node(secNode.getNodeName()+vo.getTypeNm(), secNode.getNodeId());
					typeNode.setNodeName(vo.getTypeNm());
					typeNode.setOrderNo(vo.getTypeCd());
					//log.debug("adding node for type: " + typeNode)
					t.findNode(typeNode.getParentId()).addChild(typeNode);
				}
			}
		}

		//iterate the hierarchy and set an easy-to-reference full path we can use to merge the data.
		//while there, add a 4th level that equates to the Update Types.
		//We'll them attach the updates themselves, as the lowest level.
		t = marryUpdatesToNodes(t, updates, Convert.formatBoolean(req.getParameter("orderSort")));
		addAnnouncements(t, updates);

		//set the appropriate time range onto request for view
		setDateRange(req);

		packageDataForDisplay(t, updates);
	}


	/**
	 * Add the announcements to the tree in thier own branch
	 * @param t
	 * @param updates
	 */
	private void addAnnouncements(Tree t, List<UpdateVO> updates) {
		// Create the announcement root node
		Node announcementNode = new Node("ALL_ANNOUNCEMENTS", t.getRootNode().getNodeId());
		announcementNode.setNodeName("Announcements");
		AnnouncementType type = AnnouncementType.NON;
		List<UpdateVO> selUpdates = Collections.emptyList();
		Node n = null;
		for (UpdateVO up : updates) {
			if (type.getValue() != up.getAnnouncementType()) {
				addNode(n, selUpdates, announcementNode);
				type = AnnouncementType.getFromValue(up.getAnnouncementType());
				n = new Node(type.toString(), announcementNode.getNodeId());
				n.setNodeName(type.getName());
				selUpdates = new ArrayList<>();
			}
			
			// Skip the updates that are not announcements.
			if (type != AnnouncementType.NON) {
				selUpdates.add(up);
			}
		}
		
		// Add the straggler
		addNode(n, selUpdates, announcementNode);
		
		announcementNode.setTotalChildren(announcementNode.getNumberChildren());
		// Place the announcements in front of the other items.
		t.getRootNode().getChildren().add(0, announcementNode);
	}
	
	/**
	 * Check to see if the current node is null and, if not,
	 * add it the the announcement root node.
	 * @param n
	 * @param selUpdates
	 * @param announcementNode
	 */
	private void addNode(Node n, List<UpdateVO> selUpdates, Node announcementNode) {
		if (n != null && !selUpdates.isEmpty()) {
			n.setUserObject(selUpdates);
			n.setTotalChildren(1);
			announcementNode.addChild(n);
		}
	}
	
	/**
	 * @return
	 */
	private Tree loadDefaultTree() {
		SectionHierarchyAction sha = new SectionHierarchyAction();
		sha.setDBConnection(getDBConnection());
		sha.setAttributes(getAttributes());
		return sha.loadDefaultTree();
	}


	/**
	 * @param t
	 * @param updates
	 */
	private Tree marryUpdatesToNodes(Tree t, List<UpdateVO> updates, boolean orderSort) {
		//iterate the node tree.  At each level look for updates that belong there.  
		//Compile a list and attach it to the given Node.  Then, preclude that Update from being re-displayed in the same top-level section
		for (Node n : t.getRootNode().getChildren()) {
			//maintain a list of updates already tied to this root section - they cannot appear here twice.
			Set<String> exclusions = new HashSet<>();
			iterateUpdatesForNode(n, t, updates, exclusions, orderSort);
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
	private void iterateUpdatesForNode(Node n, Tree t, List<UpdateVO> updates, Set<String> exclusions, boolean orderSort) {
		//log.debug("depth= " + n.getDepthLevel() + " name=" + n.getNodeName())
		List<UpdateVO> secUpds = new ArrayList<>();
		for (UpdateVO vo : updates) {
			iterateUpdates(vo, n, exclusions, secUpds);
		}

		//if depth is 4 then give these to our parent, level 3
		if (4 == n.getDepthLevel()) {
			Node par = t.findNode(n.getParentId());
			List<UpdateVO> data = (List<UpdateVO>) par.getUserObject();
			if (data == null) data = new ArrayList<>();
			data.addAll(secUpds);
			// Ensure ordering only if we are not relying on the db's ordering
			data = sortData(data, orderSort);
			par.setUserObject(data);
			par.setTotalChildren(data.size());
		} else {
			// Ensure ordering only if we are not relying on the db's ordering
			secUpds = sortData(secUpds, orderSort);
			n.setUserObject(secUpds);
			n.setTotalChildren(secUpds.size());
		}
		//log.debug("saved " + n.getNodeName() + " has " + n.getTotalChildren())

		//dive deeper into this node's children
		for (Node child : n.getChildren())
			this.iterateUpdatesForNode(child, t, updates, exclusions, orderSort);

	}


	/**
	 * Check whether the current node matches any of the current updates's sections
	 * and add it to the list if it hasn't been added for that node already.
	 * @param vo
	 * @param t
	 * @param n
	 * @param exclusions
	 * @param secUpds
	 */
	private void iterateUpdates(UpdateVO vo, Node n, Set<String> exclusions, List<UpdateVO> secUpds) {
		List<UpdateXRVO> secs = vo.getUpdateSections();
		// Checks and storage are done with the parent id to allow updates to 
		// appear in multiple groups while still only appearing once per group.
		String[] ids = StringUtil.checkVal(n.getFullPath()).split(SearchDocumentHandler.HIERARCHY_DELIMITER);
		
		String exclusionId = (ids.length < 2 ? n.getNodeId() : ids[1]) + "_"+vo.getUpdateId();
		//log.debug("exclusionId=" + exclusionId)
		
		if (exclusions.contains(exclusionId) || secs == null || secs.isEmpty()) return;
		for (UpdateXRVO xrvo : secs) {
			if (n.getNodeId().equals(xrvo.getSectionId())) {
				secUpds.add(vo);
				//log.debug(vo.getUpdateId() + " is comitted to " + n.getNodeName() + " &par=" + n.getParentId())
				exclusions.add(exclusionId);
			}
		}
	}

	/**
	 * Sort the Updates given into order determined by the UpdateType Enum.
	 * @param data
	 * @return
	 */
	private List<UpdateVO> sortData(List<UpdateVO> data, boolean orderSort) {
		if (orderSort) {
			Collections.sort(data, new UpdatesTypeComparator());
		} else {
			Collections.sort(data, new UpdatesEditionComparator());
		}
		return data;
	}


	/**
	 * Retrieves the correct updates, either scheduled or general list of updates
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	@SuppressWarnings("unchecked")
	protected List<UpdateVO> fetchUpdates(ActionRequest req) throws ActionException {
		//position the user's profileId where the data load can expect it.
		setProfileId(req);

		ActionInterface actInf = new UpdatesEditionDataLoader();
		actInf.setAttributes(attributes);
		actInf.setDBConnection(dbConn);
		actInf.retrieve(req);
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		return (List<UpdateVO>) mod.getActionData();
	}
	
	/**
	 * Modifies public links to their corresponding manage tool link
	 * @param updates
	 * @param req
	 */
	protected void adjustContentLinks(List<UpdateVO> updates, ActionRequest req) {
		if(!req.hasParameter("modifyLinks")) return; //only perform modification if requested
		
		SiteVO siteData = (SiteVO)req.getAttribute(Constants.SITE_DATA);
		BiomedLinkCheckerUtil linkUtil = new BiomedLinkCheckerUtil(dbConn, siteData);
		
		//update the appropriate links
		for (UpdateVO vo : updates) {
			vo.setMessageTxt(linkUtil.modifySiteLinks(vo.getMessageTxt()));
		}
	}


	/**
	 * Grabs the profileId from either the request or session if available
	 * @param req
	 * @return
	 */
	protected void setProfileId(ActionRequest req) {
		SMTSession ses = req.getSession();
		UserVO user = ses != null ? (UserVO) ses.getAttribute(Constants.USER_DATA) : null;
		if (user != null) {
			req.setParameter(PROFILE_ID, user.getProfileId()); //place on request for downstream
			req.setAttribute("statusCode", user.getLicenseType()); //set their status code on request
		}
	}


	/**
	 * Sets the appropriate time range value to the request
	 * @param req
	 */
	protected void setDateRange(ActionRequest req) {
		//leave the data alone if set by another action (UpdatesScheduledAction)
		if (req.getAttribute("dateRange") != null) return;

		int dailyRange = req.getIntegerParameter("dailyRange", 1);

		//Protect Date Range.
		dailyRange = Math.min(Math.max(dailyRange, -7), 7);

		String timeRangeCd = StringUtil.checkVal(req.getParameter("timeRangeCd"));
		String dateRange = null;

		//determine the date range
		if (UpdatesWeeklyReportAction.TIME_RANGE_WEEKLY.equalsIgnoreCase(timeRangeCd)) {
			dateRange = DateUtil.previousWeek(DateFormat.MEDIUM);
		} else if(dailyRange > 1) {

			//If not a single Date, build start to end and get Date Range.
			Calendar cl = Calendar.getInstance(Locale.getDefault());
			cl.add(Calendar.DATE, dailyRange * -1);

			Calendar cl2 = Calendar.getInstance(Locale.getDefault());
			cl.add(Calendar.DATE, -1);

			dateRange = DateUtil.getDateRangeText(cl.getTime(), cl2.getTime(), DateFormat.MEDIUM);
		} else {
			dateRange = DateUtil.getDate(-1, DateFormat.MEDIUM);
		}

		req.setAttribute("dateRange", dateRange);
	}


	/**
	 * overwritten by subclass (embed action) - puts the data into a container useful to the View.
	 * @param t
	 * @param updates
	 */
	protected void packageDataForDisplay(Tree t, List<UpdateVO> updates) {
		putModuleData(t, updates.size(), false);
	}


	/**
	 * Verify if the user has permissions to view data attached to this market.
	 * If not, redirect.
	 * @param marketNm
	 * @param req
	 * @throws ActionNotAuthorizedException
	 */
	private void checkUserHasMarketPermission(String assetAcl, ActionRequest req) throws ActionNotAuthorizedException {
		SecureSolrDocumentVO svo = new SecureSolrDocumentVO(null);
		svo.addACLGroup(Permission.GRANT, assetAcl);
		SecurityController.getInstance(req).isUserAuthorized(svo, req);
	}


	/**
	 * Helper method builds a Solr like Token for a given marketNm by looking for
	 * a section under the master root that matches the given marketNm.
	 * @param marketNm
	 * @return
	 */
	private String getMarketAcl(String marketNm) {
		StringBuilder sql = new StringBuilder(400);
		String custom = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("select r.solr_token_txt as root_solr, s.solr_token_txt as gps_solr ");
		sql.append("from ").append(custom);
		sql.append("biomedgps_section r ");
		sql.append("inner join ").append(custom).append("biomedgps_section s on r.section_id = s.parent_id ");
		sql.append("where s.section_nm = ? and r.section_id = ?");

		log.debug(sql.toString());
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, marketNm);
			ps.setString(2, AbstractTreeAction.MASTER_ROOT);

			ResultSet rs = ps.executeQuery();

			//If we have a result, build the token hierarchy String.
			if (rs.next()) {
				return rs.getString("root_solr") + "~" + rs.getString("gps_solr");
			}
		} catch (SQLException e) {
			log.error("Error Processing Code", e);
		}
		return null;
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}
}