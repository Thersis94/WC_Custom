package com.biomed.smarttrak.admin.user;

import java.util.Comparator;

// WC_Custom
import com.biomed.smarttrak.vo.UserVO;

/*****************************************************************************
 <p><b>Title</b>: UserNameComparator.java</p>
 <p><b>Description: </b>Sorts UserVOs by name.  Last name is compared first.  If last
 names are equal, first names are then compared.</p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Feb 2, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class UserNameComparator implements Comparator<UserVO> {

	/**
	* Constructor
	*/
	public UserNameComparator() {
		// constructor stub
	}

	public int compare(UserVO vo1, UserVO vo2) {
		int retVal = vo1.getLastName().compareToIgnoreCase(vo2.getLastName());
		if (retVal == 0) {
			retVal = vo1.getFirstName().compareToIgnoreCase(vo2.getFirstName());
		}

		return retVal;
	}	    	

}
