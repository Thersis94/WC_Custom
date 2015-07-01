/**
 * 
 */
package com.fastsigns.action.franchise;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.fastsigns.action.franchise.centerpage.FranchiseLocationInfoAction;
import com.fastsigns.action.franchise.vo.FranchiseVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.blog.BlogCategoryVO;
import com.smt.sitebuilder.action.blog.BlogGroupVO;
import com.smt.sitebuilder.action.blog.BlogVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>FranchiseBlogAction.java<p/>
 * <b>Description: Fetches franchise data to be used with the core blog </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Erik Wingo
 * @since Jun 30, 2015
 * <b>Changes: </b>
 ****************************************************************************/
public class FranchiseBlogAction extends SBActionAdapter {

	/**
	 * key used for storing the franchise info in the request
	 */
	public static String FRANCHISE_INFO = "franchiseInfo";
	
	/**
	 * Default Constructor
	 */
	public FranchiseBlogAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public FranchiseBlogAction(ActionInitVO arg0) {
		super(arg0);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException{
		
		//get franchise info for the current site
		String franId = StringUtil.checkVal(CenterPageAction.getFranchiseId(req),null);
		//skip lookup if we can't identify the franchise
		if (franId == null){
			log.error("Cannot get franchise info: Missing franchiseId.");
			return;
		}
		
		//grab the franchise's location data
		FranchiseLocationInfoAction fla = new FranchiseLocationInfoAction(actionInit);
		fla.setAttributes(attributes);
		fla.setDBConnection(dbConn);
		
		FranchiseVO fran = fla.getLocationInfo(franId, false);
		req.setAttribute(FRANCHISE_INFO, fran);
		
		try{
			getBlogs(req);
		} catch(SQLException sqle){
			log.error(sqle);
			throw new ActionException(sqle);
		}
	}
	
	/**
	 * Get the list of corporate blogs.
	 * @param req
	 * @throws SQLException
	 */
	protected void getBlogs(SMTServletRequest req) throws SQLException{
		Boolean isPreview = Convert.formatBoolean(req.getAttribute(Constants.PAGE_PREVIEW), false);
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		String org = (site.getCountryCode().equalsIgnoreCase("US") ? "FTS" : "FTS_"+site.getCountryCode());
		
		StringBuilder sql = new StringBuilder(650);
		sql.append("select * from BLOG a ");
		sql.append("inner join BLOGGER b on a.BLOGGER_ID = b.BLOGGER_ID ");
		sql.append("left outer join BLOG_CATEGORY_XR c on a.BLOG_ID = c.BLOG_ID ");
		sql.append("left outer join BLOG_CATEGORY d on c.BLOG_CATEGORY_ID = d.BLOG_CATEGORY_ID ");
		sql.append("inner join sb_action e on a.action_id = e.action_id ");
		sql.append("where a.action_id in (select top 1 ACTION_ID from SB_ACTION ");
		sql.append("where ORGANIZATION_ID = ? and MODULE_TYPE_ID='BLOG' ");
		if (! isPreview)
			sql.append("and PENDING_SYNC_FLG=0 ");
		sql.append(") and approval_flg=1 ");
		sql.append("order by publish_dt desc");
		
		//setup the blog group
		BlogGroupVO blog = new BlogGroupVO();
		blog.setActionName(actionInit.getName());
		blog.setActionId(actionInit.getActionId());
		
		
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			int i = 0;
			ps.setString(++i, org);
			
			ResultSet rs = ps.executeQuery();
			BlogVO vo = null;
			String blogUrl = null;
			
			while (rs.next()) {
				blogUrl = rs.getString("blog_url");
				if (blog.getBlogs().containsKey(blogUrl)) {
					vo = blog.getBlog(blogUrl);
				} else {
					vo = new BlogVO(rs);
				}
				//add categories to group
				vo.addCategory(new BlogCategoryVO(rs));
				blog.addBlog(vo);
			}
		}
		//set blog list as mod data
		putModuleData(blog);
	}
}
