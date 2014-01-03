package com.ansmed.sb.comparator;

// Java Libs
import java.util.Comparator;

// SB ANS Libs
import com.ansmed.sb.report.RankActualVO;

/****************************************************************************
 * <b>Title</b>: RankComparator.java<p/>
 * <b>Description: </b> Compares RankActualVOs rank property.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since May 07, 2009
 ****************************************************************************/

public class PercentageGrowthComparator implements Comparator<RankActualVO> {
	
	public int compare(RankActualVO r1, RankActualVO r2) {
		
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
