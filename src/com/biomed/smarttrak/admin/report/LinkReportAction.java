package com.biomed.smarttrak.admin.report;

// Java 8
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//WC custom
import com.biomed.smarttrak.admin.AccountAction;
import com.biomed.smarttrak.admin.AccountPermissionAction;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.vo.AccountVO.Type;
import com.biomed.smarttrak.vo.LinkVO;
import com.biomed.smarttrak.vo.UserVO;
import com.biomed.smarttrak.vo.UserVO.RegistrationMap;
import com.biomed.smarttrak.vo.UserVO.Status;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WebCrescendo
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: AccountsReportAction.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Mar 6, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class LinkReportAction extends SimpleActionAdapter {

	/**
	 * Constructor
	 */
	public LinkReportAction() {
		super();
	}

	/**
	 * Constructor
	 */
	public LinkReportAction(ActionInitVO arg0) {
		super(arg0);
	}

	/**
	 * Retrieves the user list report data.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	public List<LinkVO> retrieveData(ActionRequest req) throws ActionException {
		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
		return loadLinks(site, req.hasParameter("all"));
	}

	/**
	 * @param site
	 * @param hasParameter
	 * @return
	 */
	private List<LinkVO> loadLinks(SiteVO site, boolean hasParameter) {
		List<LinkVO> data = new ArrayList<>(5000);
		
		return data;
	}
}