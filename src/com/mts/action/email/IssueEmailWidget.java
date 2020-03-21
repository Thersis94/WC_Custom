package com.mts.action.email;

// MTS imports
import com.mts.publication.action.IssueAction;
import com.mts.publication.action.IssueArticleAction;
import com.mts.publication.data.IssueVO;
import com.mts.publication.data.PublicationTeaserVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;

// WC Imports
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title</b>: IssueEmailWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> This widget injects the latest issue articles and overview
 * into an MTS email showing/describing the contents of the latest issue
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Mar 11, 2020
 * @updates:
 ****************************************************************************/
public class IssueEmailWidget extends SimpleActionAdapter {

	/**
	 * 
	 */
	public IssueEmailWidget() {
		super();
	}

	/**
	 * @param arg0
	 */
	public IssueEmailWidget(ActionInitVO arg0) {
		super(arg0);
	}
	
	/*
	 * (non-javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("retrieving");

		// Get the id for the publication
		String id = req.getParameter("strategistPublicationId");
		if (id.contains("#")) id = req.getParameter("pathwaysPublicationId");

		// Load the latest issue
		IssueAction is = new IssueAction(getDBConnection(), getAttributes());
		IssueVO issue = is.getLatestIssue(id);
		
		// Get the issue documents
		IssueArticleAction iaa = new IssueArticleAction(getDBConnection(), getAttributes());
		PublicationTeaserVO ptvo = iaa.getArticleTeasers(issue.getPublicationId(), "", 1, 0);
		if (ptvo != null && ! ptvo.getDocuments().isEmpty()) issue.setDocuments(ptvo.getDocuments());
		log.debug("Number of articles: " + issue.getDocuments().size());

		// Send the data to the view
		setModuleData(issue, 1);
	}
}
