package com.depuysynthesinst;

import java.util.Comparator;

import org.apache.solr.common.SolrDocument;

import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: PageviewSolrComparator.java<p/>
 * <b>Description: sorts the Collection based on page view counts for each asset.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Sep 24, 2014
 ****************************************************************************/
public class SolrPageviewComparator implements Comparator<SolrDocument> {

	public SolrPageviewComparator() {
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compare(SolrDocument o1, SolrDocument o2) {
		int o1Views = Convert.formatInteger((Integer)o1.getFieldValue(SolrSearchWrapper.PAGEVIEWS), 0).intValue();
		int o2Views = Convert.formatInteger((Integer)o2.getFieldValue(SolrSearchWrapper.PAGEVIEWS), 0).intValue();
		
		//this is an inverted .compareTo() implementation; show the ones with the highest #s first.
		if (o1Views < o2Views) return 1;
		else if (o1Views > o2Views) return -1;
		else return 0;
	}

}
