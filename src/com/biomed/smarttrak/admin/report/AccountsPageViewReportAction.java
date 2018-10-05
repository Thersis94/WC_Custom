package com.biomed.smarttrak.admin.report;

import java.util.Date;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> AccountsPageViewReportAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Builds the Accounts PageView Summary Report.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Oct 5, 2018
 ****************************************************************************/
public class AccountsPageViewReportAction extends SimpleActionAdapter {

	public AccountsPageViewReportAction() {
		super();
	}

	public AccountsPageViewReportAction(ActionInitVO init) {
		super(init);
	}

	/**
	 * @param req
	 * @return
	 */
	public List<AccountPageViewReportVO> retrieveAccountPageViews(ActionRequest req) {
		return null;
	}

	public String generateReportTitle(String accountNm, Date startDt, Date endDt) {
		StringBuilder title = new StringBuilder(150);
		title.append("Pageviews for ").append(StringUtil.checkVal(accountNm, "All"));
		title.append("since").append(Convert.formatDate(startDt, Convert.DATE_SLASH_SHORT_PATTERN));
		title.append(" through ").append(Convert.formatDate(endDt, Convert.DATE_SLASH_SHORT_PATTERN));

		return title.toString();
	}

	/**
	 * @param req
	 * @return
	 * @throws ActionException 
	 */
	public AccountsPageViewReportVO buildReport(ActionRequest req) throws ActionException {
		AccountsPageViewReportVO rpt = new AccountsPageViewReportVO();

		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
		rpt.setSite(site);
		rpt.setData(retrieveAccountPageViews(req));

		return rpt;
	}
}
