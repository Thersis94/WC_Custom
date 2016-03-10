package com.depuysynthes.huddle.solr;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.search.solr.FormSolrIndexer;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

public class HuddleSolrFormIndexer extends FormSolrIndexer {
	
	public HuddleSolrFormIndexer(Properties config) {
		super(config);
		moduleType = "HUDDLE_FORM_GROUP";
	}
	
	public static FormSolrIndexer makeInstance(Map<String, Object> attributes) {
		Properties props = new Properties();
		props.putAll(attributes);
		return new HuddleSolrFormIndexer(props);
	}
	
	
	/**
	 * Get all the forms associated with the current form group and remove those
	 * forms from solr
	 * 
	 * Important Note:
	 * If a form has been added to multiple form groups and is removed from one
	 * it is still removed from solr even though it should stay.  This is a known
	 * potential use case but since this is a custom indexer and sharing between
	 * groups is not expected this is being left alone for the moment.
	 * @param huddleGroupId
	 */
	public void clearByGroup(String huddleGroupId) {
		StringBuilder sql = new StringBuilder(325);
		sql.append("select FORM_ID from ").append(config.get(Constants.CUSTOM_DB_SCHEMA)).append("HUDDLE_FORM_GROUP ");
		sql.append("where FORM_GROUP_ID = ?");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			StringBuilder ids = new StringBuilder();
			ps.setString(1, huddleGroupId);
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				if (ids.length() > 0) ids.append(" OR ");
				ids.append(rs.getString(1));
			}
			if (ids.length() == 0) return;
			log.debug(SearchDocumentHandler.DOCUMENT_ID + ":(" + ids + ")");
			makeServer().deleteByQuery(SearchDocumentHandler.DOCUMENT_ID + ":(" + ids.toString()+")");
		} catch (Exception e) {
			log.error("Failed to get forms for group: " + huddleGroupId, e);
		}
	}
	
	
	/**
	 * adds operating company to the SolrDocument, for Huddle
	 */
	@Override
	protected SolrDocumentVO makeNewDocument(ResultSet rs) throws SQLException {
		HuddleSolrFormVO doc = new HuddleSolrFormVO(getIndexType());
		doc.setDocumentId(rs.getString("ACTION_GROUP_ID"));
		doc.addOrganization(rs.getString("ORGANIZATION_ID"));
		doc.addRole(rs.getInt("ROLE_ORDER_NO"));
		doc.setTitle(rs.getString("ACTION_NM"));
		doc.setDocumentUrl(parsePDFPath(rs.getString("ORGANIZATION_ID"), rs.getString("PDF_FILE_PATH")));
		doc.setModule(DEF_MODULE_TYPE);
		//the only huddle-specific field we need, at the moment!
		doc.setSpecialty(rs.getString("attrib2_txt"));
		return doc;
	}
	
	
	/**
	 * Build the query for this form indexer
	 * @param groupId
	 * @return
	 */
	@Override
	protected String buildQuery(String groupId) {
		StringBuilder sql = new StringBuilder(1000);
		sql.append("select fa.ACTION_GROUP_ID, fa.ACTION_NM, sa.attrib2_txt, ");
		sql.append("fa.ORGANIZATION_ID,PDF_FILE_PATH, ff.FORM_ID, min(ROLE_ORDER_NO) as ROLE_ORDER_NO ");
		sql.append("from SB_ACTION sa ");
		sql.append("left join ").append(config.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("HUDDLE_FORM_GROUP hfg on hfg.FORM_GROUP_ID = sa.ACTION_GROUP_ID ");
		sql.append("left join SB_ACTION fa on fa.ACTION_GROUP_ID = hfg.FORM_ID ");
		sql.append("left join FB_FORM ff on ff.ACTION_ID = fa.ACTION_ID ");
		sql.append("left join FORM_PDF fp on fp.ACTION_ID = ff.ACTION_ID ");
		sql.append("inner join PAGE_MODULE pm on pm.ACTION_ID = sa.ACTION_ID ");
		sql.append("inner join PAGE p on p.PAGE_ID = pm.PAGE_ID and p.PAGE_GROUP_ID is null ");
		sql.append("inner join PAGE_MODULE_ROLE pmr on pmr.PAGE_MODULE_ID = pm.PAGE_MODULE_ID ");
		sql.append("left join ROLE r on r.ROLE_ID = pmr.ROLE_ID ");
		sql.append("where sa.MODULE_TYPE_ID = ? ");
		// We never want to add in-progress items to solr.
		sql.append("and (sa.PENDING_SYNC_FLG=0 or sa.PENDING_SYNC_FLG is null) ");
		// With non-approved items already filtered out only the approved
		// item can make it through even when searching by group id
		if (groupId != null) sql.append("and sa.ACTION_GROUP_ID = ? ");
		sql.append("group by fa.ACTION_GROUP_ID, fa.ACTION_NM, sa.attrib2_txt, fa.ORGANIZATION_ID, PDF_FILE_PATH, ff.FORM_ID ");
		sql.append("order by fa.ACTION_GROUP_ID, ff.FORM_ID");
		log.debug(sql);
		return sql.toString();
	}
	
}
