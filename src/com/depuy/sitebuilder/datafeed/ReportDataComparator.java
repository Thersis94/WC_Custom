package com.depuy.sitebuilder.datafeed;

import java.util.Comparator;
import java.io.Serializable;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: UserDataComparator.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2007<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Feb 27, 2007
 ****************************************************************************/
public class ReportDataComparator implements Comparator<QualifiedByCityReport.ReportData>, Serializable {
	public static final long serialVersionUID = 1l;
	
	/**
	 * Compares using the last name and then first name and then state
	 */
	public int compare(QualifiedByCityReport.ReportData first, QualifiedByCityReport.ReportData second) {
		// Check the objects for null
		if (first == null && second == null) return 0;
		if (first == null) return -1;
		else if (second == null) return 1;
		
		// Compare lkast names.  If they are not the samr, return the value
		int val = StringUtil.checkVal(first.getLastName().toLowerCase()).compareTo(second.getLastName().toLowerCase());
		if (val != 0) return val;
		
		// Compare the first name
		val = StringUtil.checkVal(first.getFirstName().toUpperCase()).compareTo(second.getFirstName().toLowerCase());
		if (val != 0) return val;
		
		// Compare the first name
		val = StringUtil.checkVal(first.getState().toUpperCase()).compareTo(second.getState().toUpperCase());
		if (val != 0) return val;
		
		return val;
	}

}
