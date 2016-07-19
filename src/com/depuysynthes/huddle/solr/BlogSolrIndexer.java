package com.depuysynthes.huddle.solr;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrException.ErrorCode;

import com.depuysynthes.huddle.HuddleUtils;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.blog.BlogAction;
import com.smt.sitebuilder.action.blog.BlogCategoryVO;
import com.smt.sitebuilder.action.blog.BlogGroupVO;
import com.smt.sitebuilder.action.blog.BlogVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SMTAbstractIndex;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.search.solr.data.BlogSolrDocumentVO;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.util.solr.SolrActionUtil;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title</b>: BlogSolrIndexer.java<p/>
 * <b>Description: Adds BLOG data to the Solr index, slightly customized for DSHuddle.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 08, 2016
 ****************************************************************************/
public class BlogSolrIndexer extends SMTAbstractIndex {
	
	private static final int MIN_ROLE_LVL = SecurityController.PUBLIC_REGISTERED_LEVEL;

	/**
	 * @param config
	 */
	public BlogSolrIndexer(Properties config) {
		super(config);
	}
	
	public static BlogSolrIndexer makeInstance(Map<String, Object> attributes) {
		Properties props = new Properties();
		props.putAll(attributes);
		return new BlogSolrIndexer(props);
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#addIndexItems(org.apache.solr.client.solrj.impl.HttpSolrServer)
	 */
	@Override
	public void addIndexItems(SolrClient server) {
		log.info("Indexing Huddle Blog Portlets");

		// add blog portlets that are visible on site pages
		List<BlogGroupVO> data = getBlogs(null);
		indexBlogs(server, data, null);
	}


	/**
	 * Adds blog data to the index for all organizations with blog data.
	 * @param server
	 * @param data
	 */
	@SuppressWarnings("resource")
	private void indexBlogs(SolrClient server, List<BlogGroupVO> data, String blogId) {
		log.info("Found " + data.size() + " pages containing blogs to index.");
		SolrActionUtil solrUtil = new SolrActionUtil(server);
		SolrDocumentVO solrDoc = null;
		List<SolrInputDocument> docs = new ArrayList<>();
		
		for (BlogGroupVO vo : data) {
			for (BlogVO entry : vo.getCurrentBlogsList()) {
				if (blogId != null && !blogId.equals(entry.getBlogId())) continue; //we only want one record

				entry.setUrl(vo.getBlogPath() + entry.getUrl());
				log.debug("Adding blog with URL: " + entry.getUrl() + " | Org: " + entry.getOrganizationId());

				try {
					solrDoc = SolrActionUtil.newInstance(BlogSolrDocumentVO.class.getName());
					solrDoc.setData(entry);
					solrDoc.setSummary(entry.getBlogText());  //store the whole article
					solrDoc.setMetaDesc(entry.getShortDesc());
					solrDoc.setSolrIndex(getIndexType());
					solrDoc.addRole(MIN_ROLE_LVL);
					solrDoc.setModule("BLOG");
					
					//get the dynamically built document, then add a couple of custom fields to it for Huddle's date faceting.
					SolrInputDocument doc = solrUtil.createInputDocument(solrDoc);
					doc.setField(SearchDocumentHandler.START_DATE + "Year_i", Convert.formatDate(entry.getPublishDate(), "yyyy"));
					doc.setField(SearchDocumentHandler.START_DATE + "Month_i", Convert.formatDate(entry.getPublishDate(), "MM"));
					//use opco_s instead of section, so Site Search results align with Products and Events
					Set<String> cats = new HashSet<>();
					for (BlogCategoryVO bCat : entry.getCategories())
						cats.add(bCat.getCategoryName());
					doc.setField(HuddleUtils.SOLR_OPCO_FIELD, cats);

					log.debug("adding to Solr: " + solrDoc);
					docs.add(doc);
					
				} catch (Exception e) {
					log.error("Unable to index blogs: " + StringUtil.getToString(entry), e);
				}
			}
		}
		
		if (docs.size() > 0)
			try {
				server.add(docs);
			} catch (Exception e) {
				log.error("could not index docs", e);
			}
	}


	/**
	 * Loads approved blog portlets that are attached to site pages for all orgs.
	 * @param conn
	 * @return
	 */
	private List<BlogGroupVO> getBlogs(String blogId) {
		SMTActionInterface sai = null;
		List<BlogGroupVO> data = new ArrayList<BlogGroupVO>();

		try(PreparedStatement ps = dbConn.prepareStatement(getBlogDataSql(blogId))) {
			if (blogId != null && blogId.length() > 0) ps.setString(1, blogId);
			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				String url = rs.getString(2) + "/" + config.getProperty(Constants.QS_PATH);
				//ensure pages on subsites are aliased properly.
				String subSiteAlias = StringUtil.checkVal(rs.getString(3));
				if (subSiteAlias.length() > 0) url = "/" + subSiteAlias + url;

				//call the blog action to load the Blog as the website would
				sai = new BlogAction(new ActionInitVO(null, null, rs.getString(1)));
				sai.setDBConnection(new SMTDBConnection(dbConn));
				Map<String, Object> map = new HashMap<String, Object>();
				map.put(Constants.MODULE_DATA, new ModuleVO());
				sai.setAttributes(map);
				sai.retrieve(null);
				ModuleVO mod = (ModuleVO) sai.getAttribute(Constants.MODULE_DATA);

				//add the blog data to the list
				((BlogGroupVO) mod.getActionData()).setBlogPath(url);
				data.add((BlogGroupVO) mod.getActionData());
			}
		} catch(Exception e) {
			log.error("Unable to retrieve blog info. ", e);
		}
		return data;
	}


	/**
	 * Gets the SQL for the blog data query.
	 * @return
	 */
	private String getBlogDataSql(String blogId) {
		StringBuilder sql = new StringBuilder(300);
		sql.append("select distinct a.action_id, c.full_path_txt, s.alias_path_nm, ");
		sql.append("s.organization_id ");
		sql.append("from sb_action a ");
		sql.append("inner join page_module b on a.action_id=b.action_id ");
		sql.append("inner join page_module_role pmr on pmr.page_module_id=b.page_module_id and pmr.role_id='").append(MIN_ROLE_LVL).append("' ");  //only secure portlets
		sql.append("inner join page c on c.page_id=b.page_id ");
		sql.append("inner join page_role pr on pr.page_id=c.page_id and pr.role_id='").append(MIN_ROLE_LVL).append("' "); //only secure pages
		sql.append("inner join site s on c.site_id=s.site_id ");
		sql.append("inner join module_display md on b.module_display_id=md.module_display_id ");
		sql.append("where a.module_type_id='HUDDLE_BLOG' and md.indexable_flg=1 "); //only include pages that contain Views of blog that are considered indexable.
		if (blogId != null && blogId.length() > 0) sql.append("and a.action_id in (select action_id from blog where blog_id=?)");
		log.info(sql + " " + blogId);
		return sql.toString();
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTAbstractIndex#getIndexType()
	 */
	@Override
	public String getIndexType() {
		return HuddleUtils.IndexType.HUDDLE_BLOG.toString();
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#purgeIndexItems(org.apache.solr.client.solrj.impl.HttpSolrServer)
	 */
	@Override
	public void purgeIndexItems(SolrClient server) throws IOException {
		try {
			server.deleteByQuery(SearchDocumentHandler.INDEX_TYPE + ":" + getIndexType());
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
	

	/**
	 * called from com.depuysynthes.huddle.BlogAction to push a single article update into Solr.
	 * We don't need to re-index the entire action for a single article, nor should we have to wait until 
	 * tomorrow for it to appear in search results (the next SolrIndexBuilder run).
	 * @param blogId
	 */
	@Override
	public void addSingleItem(String blogId) throws SolrException {
		log.info("Indexing Single Huddle Blog");
		try (SolrClient server = makeServer()) {
			// load the blog portlet this blogId belongs to, inclusive of the indexable page that it's on
			List<BlogGroupVO> data = getBlogs(blogId);
			indexBlogs(server, data, blogId);
			
			server.commit(false, false); //commit, but don't wait for Solr to acknowledge
			
		} catch (SolrServerException e) {
			log.error("could not commit to Solr");
			throw new SolrException(ErrorCode.BAD_REQUEST, e);
		} catch (IOException e) {
			log.error("could not create solr server");
			throw new SolrException(ErrorCode.SERVICE_UNAVAILABLE, e);
		} 
	}
}