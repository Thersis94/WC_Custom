package com.fastsigns.action.franchise.centerpage.adapter;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.fastsigns.action.franchise.vo.CenterModuleOptionVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.contact.ContactFacadeAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: ContactAdapterAction <p/>
 * <b>Project</b>: Fastsigns <p/>
 * <b>Description: </b> This class works as an adapter to proxy a ContactUs call
 * from within CenterPageAction.  
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Billy Larsen
 * @version 1.0
 * @since Feb. 2, 2013<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class ContactAdapterAction extends SBActionAdapter {

	public ContactAdapterAction(){
		super();
	}
	
	public ContactAdapterAction(ActionInitVO avo){
		super(avo);
	}
	
	/**
	 * All requests enter here and then get proxied out to Build/Retrieve of
	 * the ContactFacadeAction.
	 */
	public void build(SMTServletRequest req) throws ActionException {
		SMTActionInterface ai = new ContactFacadeAction(this.actionInit);
		ai.setAttributes(attributes);
		ai.setDBConnection(dbConn);
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
			
			//On submission we run a build.
			if(StringUtil.checkVal(req.getParameter("innerReqType")).equals(AdminConstants.REQ_BUILD)){
				ai.build(req);	
				//place the module Option response in the moduleVo
				mod.setActionData(retrieveResponse(Convert.formatInteger(req.getParameter("actionOptionId"))));
				
				//Turn of the redirect request
		    	req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.FALSE);
		    	
		    	//Set isResponse so the view knows to just print the responseText.
				req.setParameter("isResponse", "true");
			}
			else{
				ai.retrieve(req);
				req.setParameter("contactUs", "true");
			}
		}

	/**
	 * We replace the response from the ContactAction with the response held on 
	 * the Module Option in Webedit.
	 */
	private CenterModuleOptionVO retrieveResponse(int optionId) {
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		sql.append("select * from ").append(customDb).append("FTS_CP_MODULE_OPTION ");
		sql.append("where CP_MODULE_OPTION_ID = ?");
		PreparedStatement ps = null;
		CenterModuleOptionVO cmvo = null;
		try{
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, optionId);
			ResultSet rs = ps.executeQuery();
			rs.next();
			cmvo = new CenterModuleOptionVO(rs);
		} catch(SQLException sqle){
			log.debug(sqle);
		} finally {
			try {
				if (ps != null) ps.close();
			} catch (SQLException e) {
				log.debug(e);
			}
		}
		return cmvo;
		
	}
}
		