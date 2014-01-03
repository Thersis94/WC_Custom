package com.ansmed.sb.patienttracker.comparator;

// JDK 6
import java.util.Comparator;

// WC 2.0 libs
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.tracker.vo.AssigneeVO;

/****************************************************************************
 * <b>Title</b>: AssigneeStringComparator.java<p/>
 * <b>Description: </b> Compares assignee availability value.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Jan 29, 2013
 ****************************************************************************/

public class AssigneeAvailabilityComparator implements Comparator<AssigneeVO> {
	
	private final String AVAILABILITY_FIELD_ID = "c0a802419eef92a7e01d18d8f424f76";
	private String sortValue = null;
	private String sortType = "asc";
	public int compare(AssigneeVO a1, AssigneeVO a2) {
		
		boolean aHas = false;
		boolean bHas = false;
		
		aHas = this.findSortValue(a1);
		bHas = this.findSortValue(a2);
		
		if (aHas && !bHas) {
			return (sortType.equals("desc") ? 1 : -1);
		} else if (bHas && !aHas) {
			return (sortType.equals("desc") ? -1 : 1);
		} else {
			return 0;
		}
	}
	

	/**
	 * Determines if the assignee has a form response value that
	 * matches the sort value.
	 * @param a
	 * @return
	 */
	private boolean findSortValue(AssigneeVO a) {
		boolean found = false;
		if (sortValue != null) {
			if (a.getTransaction() != null) {
				if (a.getTransaction().getCustomData() != null) {
					if (a.getTransaction().getCustomData().get(AVAILABILITY_FIELD_ID) != null) {
						if (a.getTransaction().getCustomData().get(AVAILABILITY_FIELD_ID).getResponses() != null) {
							for (String resp : a.getTransaction().getCustomData().get(AVAILABILITY_FIELD_ID).getResponses()) {
								if (resp.equalsIgnoreCase(sortValue)) {
									found = true;
									break;
								}
							}
						}
					}
				}
			}
		}
		return found;
	}

	/**
	 * @param sortValue the sortValue to set
	 */
	public void setSortValue(String sortValue) {
		this.sortValue = sortValue;
	}

	/**
	 * @param sortOrder the sortOrder to set
	 */
	public void setSortType(String sortType) {
		this.sortType = StringUtil.checkVal(sortType, "asc");
	}

}
