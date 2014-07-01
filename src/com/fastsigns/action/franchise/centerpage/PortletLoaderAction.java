package com.fastsigns.action.franchise.centerpage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.fastsigns.action.franchise.vo.FranchiseContainer;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.FacadeActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: PortletLoaderAction <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> This class acts as a Proxy Loader for CenterPageAction
 * to load embedded Actions.  This performs a lookup in the database to map
 * a CenterPageModuleOption to an SB Action and we then forward the call to the
 * appropriate Action Adapter to handle request.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Billy Larsen
 * @version 1.0
 * @since Feb. 15, 2013<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class PortletLoaderAction extends FacadeActionAdapter {
	
	private static Map<String, String> adapters = loadAdapters();
		
	public PortletLoaderAction(ActionInitVO avo) {
		super(avo);
	}
	
	/**
	 * All Requests for PortletLoader Should come through the build method as 
	 * they will be ajax based.  The request will be then be forwarded to the 
	 * proper Action Adapter.
	 */
	public void build(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		
		//Store global variables that may be on the request.
		String hidePf = StringUtil.checkVal(req.getParameter("hidePf"));
		String printerFriendlyTheme = StringUtil.checkVal(req.getParameter("printerFriendlyTheme"));
		
		/*
		 * Create a FranchiseContainer to hold the Data coming back, will work
		 * like ModuleVO in that it holds the embedded actions data.
		 */
		FranchiseContainer fc = new FranchiseContainer();
		String actionId = this.actionInit.getActionId();
		log.debug("This ActionId = " + actionId);
		//Retrieve the embedded actions classpath
		SMTActionInterface ai = getAction(req);
		log.debug("loaded action : " + ai + " with ActionId : " + actionInit.getActionId());
		
		if (ai != null) {
			//Retrieve the Adapter for this action and forward to build.
			ai.build(req);
			
			/*
			 * Retrieve the Module Data after processing and place it in the 
			 * Franchise Container Object and put it back on the Request.
			 */
			mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
			fc.setActionData(mod.getActionData());
			this.putModuleData(fc);
			this.actionInit.setActionId(actionId);
		}
		//Re-append any variables that may be stripped by inner action redirects.
		req.setParameter("hidePf", hidePf);
		req.setParameter("printerFriendlyTheme", printerFriendlyTheme);
	}

	/**
	 * Retrieve the classpath of the given Option Id and places the actionId
	 * on the actionInit for processing requirements.
	 */
	public SMTActionInterface getAction(SMTServletRequest req){
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		int optionId = Convert.formatInteger(req.getParameter("actionOptionId"));
		StringBuilder sql = new StringBuilder();
		sql.append("select a.OPTION_NM, d.CLASS_NM, c.ACTION_ID from ").append(customDb).append("FTS_CP_MODULE_OPTION a ");
		sql.append("inner join ").append(customDb).append("FTS_CP_MODULE_ACTION b ");
		sql.append("on a.FTS_CP_MODULE_ACTION_ID = b.FTS_CP_MODULE_ACTION_ID ");
		sql.append("inner join SB_ACTION c on b.ACTION_ID = c.ACTION_GROUP_ID ");
		sql.append("inner join MODULE_TYPE d on c.MODULE_TYPE_ID = d.MODULE_TYPE_ID ");
		sql.append("where a.CP_MODULE_OPTION_ID = ?");
		log.debug(sql + " | " + optionId);
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, optionId);
			ResultSet rs = ps.executeQuery();
			/*
			 * If we have a result, set the ActionInit and return the classname
			 */
			if (rs.next()) {
				req.setParameter("contactEmailSubject", rs.getString("OPTION_NM"));
				actionInit.setActionId(rs.getString("ACTION_ID"));
				String cls = adapters.get(rs.getString("CLASS_NM"));
				log.debug("Loaded Adapter : " + cls);
				return this.getActionInstance(cls);
			}
		} catch (SQLException e) {
			log.error("sql exception", e);
		} finally {
			try { if (ps != null) ps.close(); } catch (SQLException e) {}
		}
		return null;
	}
	
	/**
	 * Private map that holds classpath -> adapter relationship.  Will be replaced
	 * once we have more classes and a defineable relationship between them.
	 */
	private static Map<String, String> loadAdapters() {
		Map<String, String> a = new HashMap<String, String>();
		a.put(com.smt.sitebuilder.action.contact.ContactFacadeAction.class.getName(), com.fastsigns.action.franchise.centerpage.adapter.ContactAdapterAction.class.getName());
		return a;
	}
}
