package com.codman.cu.tracking;

import java.util.Comparator;

import com.codman.cu.tracking.vo.UnitVO;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: UnitNameComparator.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 26, 2010
 ****************************************************************************/
public class UnitNameComparator implements Comparator<UnitVO>{
	
	public static final long serialVersionUID = 2l;
	private String person = "";
	private boolean desc = false;
	
	/**
	 * Person type to compare (rep, modifier, physician...). Missing or invalid
	 * person defaults to account name.
	 * @param person
	 * @param desc
	 */
	public UnitNameComparator(String person, boolean desc){
		super();
		this.person = person;
		this.desc = desc;
	}
	
	/**
	 * Compare based on a name value passed in the constructor.
	 */
	public int compare(UnitVO o1, UnitVO o2){
		// Check the objects for null
		if (o1 == null && o2 == null) return 0;
		if (o1 == null) return -1;
		else if (o2 == null) return 1;
		
		int result = 0;
		//Values will be set to these variables to avoid null pointer exceptions
		String first = null;
		String second = null;
		
		switch(person){
		case "rep":
			first = o1.getRepName();
			second = o2.getRepName();
			break;
		case "physician":
			first = o1.getPhysicianName();
			second = o2.getPhysicianName();
			break;
		case "modifier":
			first = o1.getModifyingUserName();
			second = o2.getModifyingUserName();
			break;
		default:
			first = o1.getAccountName();
			second = o2.getAccountName();
			break;
		}
		result = StringUtil.checkVal(first).compareToIgnoreCase(StringUtil.checkVal(second));
		//if they want descending, invert the result
		return ( desc ? result*-1 : result) ;
	}
}