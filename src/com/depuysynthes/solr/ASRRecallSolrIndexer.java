package com.depuysynthes.solr;

import java.util.Map;
import java.util.Properties;

import com.smt.sitebuilder.search.solr.FileSolrIndexer;

public class ASRRecallSolrIndexer extends FileSolrIndexer {
	
	public ASRRecallSolrIndexer(Properties config) {
		this.config = config;
		this.prefix = "ASR_";
		buildParams();
	}
	
	public static ASRRecallSolrIndexer makeInstance(Map<String, Object> attributes) {
		Properties props = new Properties();
		props.putAll(attributes);
		return new ASRRecallSolrIndexer(props);
	}
}
