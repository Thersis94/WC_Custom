package com.biomed.smarttrak.util;

// Java 8
import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;

// Solr 5.5
import org.apache.solr.client.solrj.SolrClient;

// SMT base libs
import com.siliconmtn.action.ActionException;

// WC Core
import com.smt.sitebuilder.util.solr.SecureSolrDocumentVO;
import com.smt.sitebuilder.util.solr.SolrActionUtil;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title</b>: SmarttrakSolrUtil.java<p/>
 * <b>Description: Decorates the WC core utility to ensure ACL permissions get attached to documents.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Feb 24, 2017
 ****************************************************************************/
public class SmarttrakSolrUtil extends SolrActionUtil {

	/**
	 * @param attributes
	 */
	public SmarttrakSolrUtil(Map<String, Object> attributes) {
		super(attributes);
	}

	/**
	 * @param server
	 */
	public SmarttrakSolrUtil(SolrClient server) {
		super(server);
	}

	/**
	 * @param attributes
	 * @param hardCommit
	 */
	public SmarttrakSolrUtil(Map<String, Object> attributes, boolean hardCommit) {
		super(attributes, hardCommit);
	}

	@Override
	public void addDocument(SolrDocumentVO doc) throws ActionException {
		if (!(doc instanceof SecureSolrDocumentVO)) 
			throw new ActionException("document is not secure.  cannot be added");

		super.addDocument(doc);
	}


	/**
	 * test the documents to ensure they're SecureSolrDocumentVOs.  Negates them from the push if not.
	 */
	@Override
	public void addDocuments(Collection<? extends SolrDocumentVO> docs) throws ActionException {
		//test the documents in the list to ensure they're ACL-bearing.  removes if not.
		docs.removeIf(isInsecureDoc());

		super.addDocuments(docs);
	}


	/**
	 * Java 8 Predicate - tests the object to ensure it's a Secure VO.
	 * @return true or false "Predicate"
	 */
	public static Predicate<SolrDocumentVO> isInsecureDoc() {
		return p -> !(p instanceof SecureSolrDocumentVO);
	}
}