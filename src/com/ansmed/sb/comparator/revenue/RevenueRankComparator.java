package com.ansmed.sb.comparator.revenue;

// Java Libs
import java.util.Comparator;

// SB ANS Libs
import com.ansmed.sb.report.RevenueActualVO;

/****************************************************************************
 * <b>Title</b>: RevenueRankComparator.java<p/>
 * <b>Description: </b> Compares rank property.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Jun 17, 2009
 ****************************************************************************/

public class RevenueRankComparator implements Comparator<RevenueActualVO> {
	
	public int compare(RevenueActualVO r1, RevenueActualVO r2) {
		
		int rank1 = r1.getRank();
		int rank2 = r2.getRank();
		
		if (rank1 < rank2) {
			return 1;
		} else if (rank1 > rank2) {
			return -1;
		} else {
			return 0;
		}
	}
}
