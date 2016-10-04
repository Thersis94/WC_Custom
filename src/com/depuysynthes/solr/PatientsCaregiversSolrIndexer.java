package com.depuysynthes.solr;

import java.util.Map;
import java.util.Properties;

import com.smt.sitebuilder.search.solr.FileSolrIndexer;

public class PatientsCaregiversSolrIndexer extends FileSolrIndexer {
	
	public PatientsCaregiversSolrIndexer(Properties config) {
		this.config = config;
		this.prefix = "PC_";
		buildParams();
	}
	
	public static PatientsCaregiversSolrIndexer makeInstance(Map<String, Object> attributes) {
		Properties props = new Properties();
		props.putAll(attributes);
		return new PatientsCaregiversSolrIndexer(props);
	}
}
