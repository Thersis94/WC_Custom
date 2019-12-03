package com.rezdox.action;

import java.util.HashMap;
import java.util.Map;

import com.rezdox.action.RezDoxNotifier.Message;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.blog.BlogFacadeAction;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: BlogAction.java<p/>
 * <b>Description: Wraps core Blog to fire browsre notifications after new articles are published.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2018<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Apr 24, 2018
 ****************************************************************************/
public class BlogAction extends SimpleActionAdapter {

	public BlogAction() {
		super();
	}

	public BlogAction(ActionInitVO arg0) {
		super(arg0);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SimpleActionAdapter#copy(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void copy(ActionRequest req) throws ActionException {
		BlogFacadeAction bfa = new BlogFacadeAction(actionInit);
		bfa.setAttributes(getAttributes());
		bfa.setDBConnection(getDBConnection());
		bfa.copy(req);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SimpleActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		BlogFacadeAction bfa = new BlogFacadeAction(actionInit);
		bfa.setAttributes(getAttributes());
		bfa.setDBConnection(getDBConnection());
		bfa.delete(req);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SimpleActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		BlogFacadeAction bfa = new BlogFacadeAction(actionInit);
		bfa.setAttributes(getAttributes());
		bfa.setDBConnection(getDBConnection());
		bfa.list(req);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SimpleActionAdapter#update(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
		//capture whether notification should be sent, then reset the approvalFlag for upstream behavior
		boolean sendNotifs = "100".equals(req.getParameter("approvalFlag"));
		if (sendNotifs) req.setParameter("approvalFlag", "1");

		BlogFacadeAction bfa = new BlogFacadeAction(actionInit);
		bfa.setAttributes(getAttributes());
		bfa.setDBConnection(getDBConnection());
		bfa.update(req);

		//if we didn't just save a blog ARTICLE, or the one we saved is not approved, return
		if (!req.hasParameter("blogId") || !sendNotifs) 
			return;

		//notify all users that a new blog article has been published
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		RezDoxNotifier notifyUtil = new RezDoxNotifier(site, getDBConnection(), getCustomSchema());
		String blogUrl = "/learn/blog";
		if (req.hasParameter("url"))
			blogUrl += "/" + StringUtil.checkVal(getAttribute(Constants.QS_PATH)) + req.getParameter("url");

		Map<String, Object> params = new HashMap<>();
		params.put("blogUrl", blogUrl);
		notifyUtil.sendToAllMembers(Message.BLOG_NEW, params, null);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		BlogFacadeAction bfa = new BlogFacadeAction(actionInit);
		bfa.setAttributes(getAttributes());
		bfa.setDBConnection(getDBConnection());
		bfa.retrieve(req);
	}
}