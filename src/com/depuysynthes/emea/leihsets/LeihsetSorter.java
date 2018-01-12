package com.depuysynthes.emea.leihsets;

import java.util.Comparator;

/****************************************************************************
 * <b>Title:</b> LeihsetSorter.java<br/>
 * <b>Description:</b> 
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2017<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Dec 11, 2017
 ****************************************************************************/
public class LeihsetSorter implements Comparator<LeihsetVO> {

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(LeihsetVO o1, LeihsetVO o2) {
		if (o1 == null) return 1;
		if (o2 == null) return -1;

		int val = o1.getLeihsetName().compareToIgnoreCase(o2.getLeihsetName());
		if (val != 0) return val;

		return Integer.compare(o1.getOrderNo(), o2.getOrderNo());
	}
}