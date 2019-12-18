package com.biomed.smarttrak.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.ArrayUtils;
import org.apache.solr.client.solrj.SolrClient;

import com.biomed.smarttrak.action.GapAnalysisAction;
import com.biomed.smarttrak.admin.AbstractTreeAction;
import com.biomed.smarttrak.vo.GapTableVO;
import com.biomed.smarttrak.vo.SectionVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.SMTClassLoader;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SMTAbstractIndex;
import com.smt.sitebuilder.util.solr.SolrActionUtil;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title:</b> GAIndexer.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Solr Indexer manages Gap Analysis Company Document data.
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Dec 6, 2019
 ****************************************************************************/
public class GAIndexer extends SMTAbstractIndex {

	public static final String INDEX_TYPE = "BIOMEDGPS_GA_COMPANY";

	public GAIndexer(Properties config) {
		super(config);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#addIndexItems(org.apache.solr.client.solrj.SolrClient)
	 */
	@SuppressWarnings("resource")
	@Override
	public void addIndexItems(SolrClient server) {

		//Prep Vars
		SolrActionUtil util = new SolrActionUtil(server);
		List<SolrDocumentVO> docs;

		// Load all Selectable Nodes
		List<SectionVO> selNodes = loadSelNodes(ArrayUtils.EMPTY_STRING_ARRAY);

		try {
			//Load Documents
			docs = processNodes(selNodes);

			//Save Documents
			util.addDocuments(docs);
		} catch (ActionException e) {
			log.error("Error Processing Code", e);
		}
	}


	/**
	 * Load GapTableVOs from the Action and store off the Company Data for solr.
	 * @param selNodes
	 * @throws ActionException 
	 */
	private List<SolrDocumentVO> processNodes(List<SectionVO> selNodes) throws ActionException {
		List<SolrDocumentVO> docs = new ArrayList<>();
		GapAnalysisAction gaa = loadAction();
		for(SectionVO n : selNodes) {
			ActionRequest req = new ActionRequest();
			req.setParameter("selNodes", n.getSectionId());
			GapTableVO gtv = gaa.getGapTable(req, false);
			gtv.getCompanies().values().stream().forEach(gc -> {
				gc.setSectionId(n.getSectionId());
				gc.setOrderNo(n.getOrderNo());
			});
			docs.addAll(gtv.getCompanies().values());
		}

		return docs;
	}

	/*
	 * Prepare and return a GapAnalysisAction
	 * @return
	 */
	private GapAnalysisAction loadAction() {
			GapAnalysisAction gaa = null;
			try {
				gaa = (GapAnalysisAction) SMTClassLoader.getClassInstance(GapAnalysisAction.class.getCanonicalName());
				gaa.setDBConnection(new SMTDBConnection(dbConn));
				gaa.setAttributes(getAttributes());
			} catch (ClassNotFoundException e) {
				log.error("Error Processing Code", e);
			}
		return gaa;
	}

	/**
	 * Load all Section Nodes that can appear in the Gap Analysis tool.
	 * @param itemIds
	 * @return
	 */
	private List<SectionVO> loadSelNodes(String...itemIds) {
		List<Object> vals = new ArrayList<>();
		vals.add(AbstractTreeAction.MASTER_ROOT);
		if(itemIds != null && itemIds.length > 0) {
			vals.addAll(Arrays.asList(itemIds));
		}
		DBProcessor dbp = new DBProcessor(dbConn, (String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA));
		return dbp.executeSelect(loadSectionSql(itemIds), vals, new SectionVO());
	}

	/**
	 * Build the SQL Query to retrieve Gap Analysis Sections.
	 * @return
	 */
	private String loadSectionSql(String[] itemIds) {
		String schema = (String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(400);
		sql.append(DBUtil.SELECT_CLAUSE).append("section_id, parent_id, section_nm, order_no ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("biomedgps_section as p ");
		sql.append(DBUtil.WHERE_CLAUSE).append("is_gap = 1 and parent_id in (");
		sql.append(DBUtil.SELECT_CLAUSE).append("bs.section_id ").append(DBUtil.FROM_CLAUSE);
		sql.append(schema).append("biomedgps_section as bs ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("biomedgps_section bs2 ");
		sql.append("on bs.parent_id = bs2.section_id where bs2.parent_id = ?)");
		if(itemIds != null && itemIds.length > 0) {
			sql.append(" and section_id in (");
			DBUtil.preparedStatmentQuestion(itemIds.length, sql);
			sql.append(") ");
		}
		sql.append(DBUtil.ORDER_BY).append("order_no, parent_id");

		return sql.toString();
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#indexItems(java.lang.String[])
	 */
	@Override
	public void indexItems(String... itemIds) {
		SolrClient server = makeServer();
		List<SolrDocumentVO> docs;

		// Load all Selectable Nodes
		List<SectionVO> selNodes = loadSelNodes(itemIds);

		try (SolrActionUtil util = new SmarttrakSolrUtil(server)) {
			//Load Documents
			docs = processNodes(selNodes);

			//Save Documents
			util.addDocuments(docs);

		} catch (Exception e) {
			log.error("Failed to index company with id: " + itemIds, e);
		}
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTAbstractIndex#getIndexType()
	 */
	@Override
	public String getIndexType() {
		return INDEX_TYPE;
	}

}
