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
import com.siliconmtn.util.Convert;
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
	public static final String LINK_ID = "linkId"; //req param

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
		boolean reviewFlg = Convert.formatBoolean(req.getParameter("reviewFlag"));
		return loadLinks(site, reviewFlg);
	}

	@Override
	public void build(ActionRequest req) throws ActionException{
		if(req.hasParameter("markedReview")){
			markedReview(StringUtil.checkVal(req.getParameter(LINK_ID)));
		}		
	}

	/**
	 *	Updates LinkVO's status to reviewed in db
	 * @param linkId
	 */
	protected void markedReview(String linkId) throws ActionException{
		//build the query
		StringBuilder sql = new StringBuilder(100);
		sql.append("UPDATE ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_LINK set review_flg = 1 where link_id = ?");
		log.debug(sql);

		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			ps.setString(1, linkId);
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error("Error attempting to update record: " + e);
			throw new ActionException(e);
		}
		// Pass along link id for callback method on client side.
		super.putModuleData(linkId);
	}


	/**
	 * @param site
	 * @param reviewFlag
	 * @return
	 * @throws ActionException 
	 */
	private List<LinkVO> loadLinks(SiteVO site, boolean reviewFlag) throws ActionException {
		String qsPath = (String) getAttribute(Constants.QS_PATH);
		List<LinkVO> data = new ArrayList<>(5000);

		StringBuilder sql = new StringBuilder(250);
		sql.append("select l.link_id, l.url_txt, l.status_no, l.check_dt, l.review_flg, l.content_id, ");
		sql.append("coalesce(l.company_id,l.product_id,l.insight_id,l.update_id,l.market_id) as id, ");
		sql.append("coalesce(c.company_nm,p.product_nm,i.title_txt,u.title_txt,m.market_nm) as nm, ");
		sql.append("case when l.company_id is not null then 'COMPANY' when l.product_id is not null then 'PRODUCT' ");
		sql.append("when l.insight_id is not null then 'INSIGHT' when l.market_id is not null then 'MARKET' else 'UPDATE' end as section ");
		sql.append("from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("biomedgps_link l ");
		sql.append("left join custom.biomedgps_company c on c.company_id = l.company_id ");
		sql.append("left join custom.biomedgps_product p on p.product_id = l.product_id ");
		sql.append("left join custom.biomedgps_update u on u.update_id = l.update_id ");
		sql.append("left join custom.biomedgps_market m on m.market_id = l.market_id ");
		sql.append("left join custom.biomedgps_insight i on i.insight_id = l.insight_id ");
		sql.append("where l.status_no=404 ");
		if(!reviewFlag) sql.append("and l.review_flg=0 ");
		sql.append("order by section, id");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				LinkVO vo = LinkVO.makeForUrl(rs);
				setUrls(site.getFullSiteAlias(), vo, qsPath);
				data.add(vo);
			}

		} catch (SQLException sqle) {
			log.error("could not load links list", sqle);
			throw new ActionException(sqle);
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
		String contentType;
		if (Section.COMPANY.name().equals(vo.getSection())) {
			sec = Section.COMPANY.getPageURL();
			actionType="companyAdmin&companyId=";
			contentType="&attributeTypeCd=HTML&actionTarget=COMPANYATTRIBUTE&companyAttributeId=";
		} else if (Section.PRODUCT.name().equals(vo.getSection())) {
			sec = Section.PRODUCT.getPageURL();
			actionType="productAdmin&productId=";
			contentType="&actionTarget=PRODUCTATTRIBUTE&attributeTypeCd=HTML&productAttributeId=";
		} else if (Section.INSIGHT.name().equals(vo.getSection())) {
			sec = Section.INSIGHT.getPageURL();
			actionType="insights&insightId=";
			vo.setSection("ANALYSIS"); //override cosmetic label
			contentType="&activeTab=";
		} else if (Section.MARKET.name().equals(vo.getSection())) {
			sec = Section.MARKET.getPageURL();
			actionType="marketAdmin&marketId=";
			contentType="&actionTarget=MARKETATTRIBUTE&attributeTypeCd=HTML&marketAttributeId=";
		} else {
			//updates - they don't have a page - just link to the homepage /qs/<id>
			sec = "";
			actionType="updates&updateId=";
			contentType="";
		}

		//add FQDN to relative (presumed local) URLs
		if (!StringUtil.isEmpty(vo.getUrl()) && vo.getUrl().startsWith("/")) 
			vo.setUrl(fqdn + vo.getUrl());

		vo.setPublicUrl(StringUtil.join(fqdn, sec, qsPath, vo.getObjectId()));
		vo.setAdminUrl(StringUtil.join(fqdn, "/manage?actionType=", actionType, vo.getObjectId(), contentType, vo.getContentId()));
	}
}