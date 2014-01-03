package com.ansmed.sb.comparator.revenue;

// Java Libs
import java.util.Comparator;

// SB ANS Libs
import com.ansmed.sb.report.RevenueActualVO;

/****************************************************************************
 * <b>Title</b>: RevenuePhysComparator.java<p/>
 * <b>Description: </b> Compares physician's last name property.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Jun 22, 2009
 ****************************************************************************/

public class RevenuePhysComparator implements Comparator<RevenueActualVO> {
	
	public int compare(RevenueActualVO r1, RevenueActualVO r2) {
		
		String s1 = r1.getSurgeonLastName();
		String s2 = r2.getSurgeonLastName();
		
		return s1.compareToIgnoreCase(s2);
		
	}
}
