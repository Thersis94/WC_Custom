package com.ram.action.user;

// JDK 1.7
import java.util.Comparator;
import java.io.Serializable;

// RAM Libs
import com.ram.datafeed.data.RAMUserVO;

// SMTBaseLibs 2.0
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: RAMUserComparator.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Jun 03, 20147
 ****************************************************************************/
public class RAMUserComparator implements Comparator<RAMUserVO>, Serializable {
	public static final long serialVersionUID = 1l;
	
	/**
	 * Compares using the last name and then first name and then state
	 */
	public int compare(RAMUserVO o1, RAMUserVO o2) {
		// Check the objects for null
		if (o1 == null && o2 == null) return 0;
		if (o1 == null) return -1;
		else if (o2 == null) return 1;
		String firstName = StringUtil.checkVal(o1.getLastName()).concat(StringUtil.checkVal(o1.getFirstName()));
		String secondName = StringUtil.checkVal(o2.getLastName()).concat(StringUtil.checkVal(o2.getFirstName()));
		
		return firstName.compareToIgnoreCase(secondName);
		
	}

}
