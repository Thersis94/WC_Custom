package com.biomed.smarttrak.admin.report;

// JDK 8
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// WC custom
import com.biomed.smarttrak.vo.AccountVO;
import com.biomed.smarttrak.vo.UserVO;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;

// WebCrescendo
import com.smt.sitebuilder.action.SimpleActionAdapter;

/*****************************************************************************
 <p><b>Title</b>: UserUtilizationReportAction.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Feb 21, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class UserUtilizationReportAction extends SimpleActionAdapter {
	
	public static final String STATUS_NO_INACTIVE = "I";
	public static final String PHONE_TYPE_HOME = "HOME";
	
	/**
	* Constructor
	*/
	public UserUtilizationReportAction() {
		super();
	}

	/**
	* Constructor
	*/
	public UserUtilizationReportAction(ActionInitVO arg0) {
		super(arg0);
	}

	/**
	 * Retrieves the user utilization data and returns it as a Map of AccountVO mapped to a List of UserVO for
	 * each account.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	public Map<AccountVO, List<UserVO>> retrieveUserUtilization(ActionRequest req) throws ActionException {

		return new LinkedHashMap<>();

	}

}
