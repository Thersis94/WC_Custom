package com.biomed.smarttrak.admin.report;

// Java 8
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

//WC custom
import com.biomed.smarttrak.action.AdminControllerAction.Section;
import com.biomed.smarttrak.vo.LinkVO;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;

// WebCrescendo
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: LinkReportAction.java</p>
 <p><b>Description: Loads the data for the Broken Links Excel report.</b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Apr 2, 2017
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
		return loadLinks(site);
	}


	/**
	 * @param site
	 * @param hasParameter
	 * @return
	 */
	private List<LinkVO> loadLinks(SiteVO site) {
		String qsPath = (String) getAttribute(Constants.QS_PATH);
		List<LinkVO> data = new ArrayList<>(5000);

		StringBuilder sql = new StringBuilder(250);
		sql.append("select url_txt, status_no, check_dt, ");
		sql.append("coalesce(company_id,product_id,insight_id,update_id,market_id) as id, ");
		sql.append("case when company_id is not null then 'COMPANY' when product_id is not null then 'PRODUCT' ");
		sql.append("when insight_id is not null then 'INSIGHT' when market_id is not null then 'MARKET' else 'UPDATE' end as section ");
		sql.append("from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("biomedgps_link ");
		sql.append("where status_no=404 order by section, id");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				LinkVO vo = LinkVO.makeForUrl(rs.getString("section"),rs.getString("id"),rs.getString("url_txt"));
				vo.setLastChecked(rs.getDate("check_dt"));
				vo.setOutcomeNo(rs.getInt("status_no"));
				setUrls(site.getFullSiteAlias(), vo, qsPath);
				data.add(vo);
			}

		} catch (SQLException sqle) {
			log.error("could not load links list", sqle);
		}

		return data;
	}


	/**
	 * format a URL to the public-facing page containing the broken link - so they can confirm it.
	 * @param vo
	 */
	protected void setUrls(String fqdn, LinkVO vo, String qsPath) {
		String sec;
		String actionType;
		if (Section.COMPANY.name().equals(vo.getSection())) {
			sec = Section.COMPANY.getPageURL();
			actionType="companyAdmin&companyId=";
		} else if (Section.PRODUCT.name().equals(vo.getSection())) {
			sec = Section.PRODUCT.getPageURL();
			actionType="productAdmin&productId=";
		} else if (Section.INSIGHT.name().equals(vo.getSection())) {
			sec = Section.INSIGHT.getPageURL();
			actionType="insights&insightId=";
		} else if (Section.MARKET.name().equals(vo.getSection())) {
			sec = Section.MARKET.getPageURL();
			actionType="marketAdmin&marketId=";
		} else {
			//updates - they don't have a page - just link to the homepage /qs/<id>
			sec = "";
			actionType="updates&updateId=";
		}

		//add FQDN to relative (presumed local) URLs
		if (!StringUtil.isEmpty(vo.getUrl()) && vo.getUrl().startsWith("/")) 
			vo.setUrl(fqdn + vo.getUrl());
		
		vo.setPublicUrl(new StringBuilder(100).append(fqdn).append("/").append(sec).append(qsPath).append(vo.getObjectId()).toString());
		vo.setAdminUrl(new StringBuilder(100).append(fqdn).append("/manage?actionType=").append(actionType).append(vo.getObjectId()).toString());
	}
}