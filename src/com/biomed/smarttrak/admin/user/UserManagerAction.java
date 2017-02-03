package com.biomed.smarttrak.admin.user;

// Java 7
import java.util.ArrayList;
import java.util.List;

// WC Custom libs
import com.biomed.smarttrak.vo.UserVO;

//SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;

//WebCrescendo libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: UserManagerAction.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Feb 1, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class UserManagerAction extends SBActionAdapter {
	
	/**
	 * Constructor
	 */
	public UserManagerAction() {
		// constructor stub
		super();
	}
	
	/**
	 * Constructor
	 */
	public UserManagerAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO)getAttribute(Constants.MODULE_DATA);
		List<UserVO> users;
		
		try {
			UserManager um = new UserManager(dbConn,attributes);
			um.setUserId(req.getParameter("userId"));
			users = um.retrieveCompleteUser();
			
		} catch (Exception ae) {
			users = new ArrayList<>();
			mod.setError(ae.getMessage(),ae);
		}
		
		this.putModuleData(users, users.size(), false, mod.getErrorMessage(), mod.getErrorCondition());
		
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		// for admin subsite: will update Smarttrak user data.
	}
	
}
