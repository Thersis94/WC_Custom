package com.ansmed.sb.comparator.revenue;

// Java Libs
import java.util.Comparator;

// SB ANS Libs
import com.ansmed.sb.report.RevenueActualVO;

/****************************************************************************
 * <b>Title</b>: RevenueTrialsComparator.java<p/>
 * <b>Description: </b> Compares trials property.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Jun 17, 2009
 ****************************************************************************/

public class RevenueTrialsComparator implements Comparator<RevenueActualVO> {
	
	public int compare(RevenueActualVO r1, RevenueActualVO r2) {
		
		int trials1 = r1.getActualsData().getTotalTrials();
		int trials2 = r2.getActualsData().getTotalTrials();
		
		if (trials1 < trials2) {
			return 1;
		} else if (trials1 > trials2) {
			return -1;
		} else {
			return 0;
		}
	}
}
