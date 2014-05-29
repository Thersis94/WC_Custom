package com.ram.action.user;

// JDK 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// RAMDataFeed
import com.ram.datafeed.data.CustomerVO;

// SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;

// WebCrescendo 2.0
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title: </b>RamUserAction.java <p/>
 * <b>Project: </b>WC_Custom <p/>
 * <b>Description: </b>
 * </p>
 * <b>Copyright: </b>Copyright (c) 2014<p/>
 * <b>Company: </b>Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0<p/>
 * @since May 28, 2014<p/>
 *<b>Changes: </b>
 * May 28, 2014: David Bargerhuff: Created class.
 ****************************************************************************/
public class RamUserAction extends SBActionAdapter {
	
	/**
	 * 
	 */
	public RamUserAction() {
		super(new ActionInitVO());
	}

	/**
	 * @param actionInit
	 */
	public RamUserAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("RamUserAction retrieve...");
		
		List<CustomerVO> data = new ArrayList<>();
		if (! Convert.formatBoolean(req.getParameter("addUser"))) {
		} else {
			
		}
		ModuleVO modVo = (ModuleVO) attributes.get(Constants.MODULE_DATA);
        modVo.setDataSize(data.size());
        modVo.setActionData(data);
        this.setAttribute(Constants.MODULE_DATA, modVo);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("RamUserAction build...");
		Object msg = null;
        // Build the redirect and messages
		// Setup the redirect.
		StringBuilder url = new StringBuilder();
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		url.append(page.getRequestURI());
		if (msg != null) url.append("?msg=").append(msg);
		
		putModuleData(msg);
		
		log.debug("RamUserAction redir: " + url);
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
	}
	
}
