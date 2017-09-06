package com.biomed.smarttrak.admin;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.biomed.smarttrak.vo.PermissionVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> AccountPermissionEmbedAction.java<br/>
 * <b>Description:</b>  This action's sole purpose is to embed in the Welcome Msg. user email.  It loads the list of Sections
 * the user's account is authorized for and displays the Tree as an indented hml list, embedded in the notification email.
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2017<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Aug 22, 2017
 ****************************************************************************/
public class AccountPermissionEmbedAction extends SimpleActionAdapter {

	public AccountPermissionEmbedAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public AccountPermissionEmbedAction(ActionInitVO arg0) {
		super(arg0);
	}


	/*
	 * Load the accounts' permission tree and go to View (which is Freemarker)
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		AccountPermissionAction act = new AccountPermissionAction();
		act.setDBConnection(getDBConnection());
		act.setAttributes(getAttributes());
		act.retrieve(req);

		formatDisplayData();
	}


	/**
	 * Converts the Tree obtained from AccountPermissionAction into a simple Map<String, List> Freemarker can work with.
	 */
	protected void formatDisplayData() {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		Tree t = (Tree) mod.getActionData();
		if (t == null) return;

		Map<String, Set<String>> data = new LinkedHashMap<>();
		for (Node n1 : t.getRootNode().getChildren()) {
			for (Node n2 : n1.getChildren()) {
				String lvl2 = n2.getNodeName();
				Set<String> children = loadChildren(n2);
				if (!children.isEmpty()) {
					data.put(lvl2, children);
					log.debug(lvl2 + " has " + children.size());
				}
			}
		}

		putModuleData(data);
	}


	/**
	 * separation from above to keep code readable.  this is the nested iteration at the level where permissions are set.
	 * @param n2
	 * @return
	 */
	private Set<String> loadChildren(Node n2) {
		Set<String> children = new LinkedHashSet<>();
		for (Node n3 : n2.getChildren()) {
			PermissionVO perm3 = (PermissionVO) n3.getUserObject();
			if (perm3.isBrowseAuth())
				children.add(n3.getNodeName());
		}
		return children;
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
