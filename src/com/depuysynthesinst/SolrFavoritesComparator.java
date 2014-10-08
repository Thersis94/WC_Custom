package com.depuysynthesinst;

import java.util.Comparator;

import org.apache.solr.common.SolrDocument;

import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: SolrFavoritesComparator.java<p/>
 * <b>Description: Sorts the Collection based on whether each asset is tagged as a favorite (of the user).</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Sep 24, 2014
 ****************************************************************************/
public class SolrFavoritesComparator implements Comparator<SolrDocument> {

	public SolrFavoritesComparator() {
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compare(SolrDocument o1, SolrDocument o2) {
		Integer o1Tag = Convert.formatInteger((Integer)o1.getFieldValue(SolrSearchWrapper.FAVORITES), 0);
		Integer o2Tag = Convert.formatInteger((Integer)o2.getFieldValue(SolrSearchWrapper.FAVORITES), 0);
		return o1Tag.compareTo(o2Tag);
	}

}
