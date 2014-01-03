package com.ansmed.sb.patienttracker.comparator;

// JDK 6
import java.util.Comparator;

// SMTBaseLibs 2.0
import com.siliconmtn.util.StringUtil;

// WC 2.0 libs
import com.smt.sitebuilder.action.tracker.vo.AssignmentVO;

/****************************************************************************
 * <b>Title</b>: AssignmentActionsComparator.java<p/>
 * <b>Description: </b> Custom comparator that compares 'action' display values used in the admin's
 * list of own assignments in the admin dashboard.  The 'action' display values are based on an 
 * assignment's underlying status value.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Feb 05, 2013
 ****************************************************************************/

public class AssignmentActionsComparator implements Comparator<AssignmentVO> {
	
	private String sortType = "asc";
	public int compare(AssignmentVO a1, AssignmentVO a2) {
		
		/*
		 * NOTE:  As of 02-05-2013, admin dashboard 'Action' display values 
		 * are as follows:
		 * 'Needs Acceptance' corresponds to assignment status ID 10 ('Pending')
		 * 'Request Reassignment' corresponds to assignment status ID 50 ('Request Reassignment')
		 * 'Tracking Form' corresponds to assignment status ID 30 ('In Progress')
		 * 
		 * Status ID 20 is unused while status ID 40 is not used in the admin dashboard listing
		 * of an admin's own assignments.
		 */
		int a = a1.getAssignmentStatusId();
		int b = a2.getAssignmentStatusId();
		
		int compVal = 0;
		
		if (a < 60 && b < 60) {
			if (a == b) {
				compVal =  0;
			} else if (a < b) {
				if (a == 10) compVal =  -1;
				else compVal =  1; // a == 30
			} else { // a > b
				if (a == 50) compVal =  -1;
				else compVal =  1; // a == 30
			}
		} else if (a < 60) { // b >= 60
			compVal = -1;
		} else { // a >= 60
			compVal = 1;
		}
		
		if (sortType.equals("desc")) compVal = compVal * -1;
		return compVal;
	}
	
	/**
	 * @param sortOrder the sortOrder to set
	 */
	public void setSortType(String sortType) {
		this.sortType = StringUtil.checkVal(sortType, "asc");
	}

}
