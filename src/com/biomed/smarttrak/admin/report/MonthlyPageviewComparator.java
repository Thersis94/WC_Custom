package com.biomed.smarttrak.admin.report;

import java.util.Comparator;

/****************************************************************************
 * <b>Title:</b> MonthlyPageviewComparator.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Comparator for sorting MonthlyPageViewVOs by requestUri.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 16, 2018
 ****************************************************************************/
public class MonthlyPageviewComparator implements Comparator<MonthlyPageViewVO> {

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(MonthlyPageViewVO o1, MonthlyPageViewVO o2) {
		return o1.getRequestUri().compareTo(o2.getRequestUri());
	}

}
