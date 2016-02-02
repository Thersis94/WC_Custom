package com.depuysynthes.huddle.solr;

import java.util.Map;
import java.util.Properties;

import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.solr.FormSolrIndexer;

public class HuddleSolrFormIndexer extends FormSolrIndexer {
	
	public HuddleSolrFormIndexer(Properties config) {
		super(config, "HUDDLE_FORM_GROUP");
	}
	
	public static FormSolrIndexer makeInstance(Map<String, Object> attributes) {
		Properties props = new Properties();
		props.putAll(attributes);
		return new HuddleSolrFormIndexer(props);
	}
	
	
	/**
	 * Build the query for this form indexer
	 * @param groupId
	 * @return
	 */
	@Override
	protected String buildQuery(String groupId) {
		StringBuilder sql = new StringBuilder(1000);
		
		sql.append("select fa.ACTION_GROUP_ID, fa.ACTION_NM, fa.ORGANIZATION_ID,PDF_FILE_PATH, ROLE_ORDER_NO ");
		sql.append("from SB_ACTION sa ");
		sql.append("left join ").append(config.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("HUDDLE_FORM_GROUP hfg on hfg.FORM_GROUP_ID = sa.ACTION_GROUP_ID ");
		sql.append("left join SB_ACTION fa on fa.ACTION_GROUP_ID = hfg.FORM_ID ");
		sql.append("left join FB_FORM ff on ff.ACTION_ID = fa.ACTION_ID ");
		sql.append("left join FORM_PDF fp on fp.ACTION_ID = ff.ACTION_ID ");
		sql.append("inner join PAGE_MODULE pm on pm.ACTION_ID = sa.ACTION_ID ");
		sql.append("inner join PAGE p on p.PAGE_ID = pm.PAGE_ID and p.PAGE_GROUP_ID is null ");
		sql.append("inner join PAGE_MODULE_ROLE pmr ");
		sql.append("on pmr.PAGE_MODULE_ID = pm.PAGE_MODULE_ID ");
		sql.append("left join ROLE r on r.ROLE_ID = pmr.ROLE_ID ");
		// We never want to add in progress items to solr.
		sql.append("where sa.MODULE_TYPE_ID = ? and (sa.PENDING_SYNC_FLG = 0 ");
		sql.append("or sa.PENDING_SYNC_FLG is null) ");
		// With non-approved items already filtered out only the approved
		// item can make it through even when searching by group id
		if (groupId != null) sql.append("and sa.ACTION_GROUP_ID = ? ");
		sql.append("order by sa.ACTION_GROUP_ID, ff.FORM_ID");
		log.info(sql);
		
		return sql.toString();
	}
	
}
