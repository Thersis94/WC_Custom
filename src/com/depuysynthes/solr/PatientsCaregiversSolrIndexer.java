package com.depuysynthes.solr;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrException;

import com.depuysynthes.solr.data.PatientsCaregiversVO;
import com.siliconmtn.action.ActionException;
import com.smt.sitebuilder.search.solr.FileSolrIndexer;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title</b>: PatientsCaregiversSolrIndexer.java<p/>
 * <b>Description: Patients and caregivers specific remote file indexer. 
 * Contains all information specific to these document types for the file 
 * indexer</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @since Oct 3, 2016
 ****************************************************************************/

public class PatientsCaregiversSolrIndexer extends FileSolrIndexer {
	
	public PatientsCaregiversSolrIndexer(Properties config) {
		super(config, "PC_");
	}
	
	public static PatientsCaregiversSolrIndexer makeInstance(Map<String, Object> attributes) {
		Properties props = new Properties();
		props.putAll(attributes);
		return new PatientsCaregiversSolrIndexer(props);
	}

	@Override
	public void purgeIndexItems(SolrClient solr) throws IOException {
		try {
			solr.deleteByQuery("pcg_s:true");
		} catch (SolrServerException e) {
			throw new IOException(e);
		}
	}

	@Override
	public String getIndexType() {
		return new PatientsCaregiversVO().getSolrIndex();
	}

	@Override
	protected SolrDocumentVO getNewDoc(Entry<String, byte[]> file) throws ActionException {
		SolrDocumentVO doc = new PatientsCaregiversVO();
		if (file != null) doc.setData(file);
		return doc;
	}

	@Override
	public void addSingleItem(String arg0) throws SolrException {
		// This function should never be called as the indexer will only
		// be run for all documents at once
	}
}
