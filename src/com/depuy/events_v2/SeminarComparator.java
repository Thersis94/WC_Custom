/**
 * 
 */
package com.depuy.events_v2;

import java.util.Comparator;
import java.util.Date;

import com.depuy.events_v2.vo.DePuyEventSeminarVO;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: SeminarComparator.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 23, 2014
 ****************************************************************************/
public final class SeminarComparator {
	
	/*
	 * Simple String Comparator that sorts based on the Seminars Joint Label
	 */
	public class ProductComparator implements Comparator<DePuyEventSeminarVO> {
		public int compare(DePuyEventSeminarVO o1, DePuyEventSeminarVO o2) {
			return o1.getJointLabel().compareTo(o2.getJointLabel());
		}
	}
	
	/*
	 * Simple String Comparator that sorts based on the Events Joint RSVP Code
	 * sort descending
	 */
	public class RSVPComparator implements Comparator<DePuyEventSeminarVO> {
		public int compare(DePuyEventSeminarVO o1, DePuyEventSeminarVO o2) {
			Integer cd1 = Convert.formatInteger(o1.getEvents().get(0).getRSVPCode());
			Integer cd2 = Convert.formatInteger(o2.getEvents().get(0).getRSVPCode());
			return cd2.compareTo(cd1);
		}
	}
	
	/*
	 * Simple Date Comparator that sorts based on the Events Start Date
	 */
	public class DateComparator implements Comparator<DePuyEventSeminarVO> {
		public int compare(DePuyEventSeminarVO o1, DePuyEventSeminarVO o2) {
			Date d1 = o1.getEarliestEventDate();
			Date d2 = o2.getEarliestEventDate();
			return d1.compareTo(d2);
		}
	}
	
	/*
	 * Simple String Comparator that sorts based on the Events Type Code
	 */
	public class TypeComparator implements Comparator<DePuyEventSeminarVO> {
		public int compare(DePuyEventSeminarVO o1, DePuyEventSeminarVO o2) {
			return o1.getEvents().get(0).getEventTypeCd().compareTo(o2.getEvents().get(0).getEventTypeCd());
		}
	}
	
	/*
	 * Simple String Comparator that sorts based on the Events Status Flag
	 */
	public class StatusComparator implements Comparator<DePuyEventSeminarVO> {
		public int compare(DePuyEventSeminarVO o1, DePuyEventSeminarVO o2) {
			return o1.getStatusFlg().compareTo(o2.getStatusFlg());
		}
	}
	
	/*
	 * Simple String Comparator that sorts based on the Seminars Owners Full Name
	 */
	public class OwnerComparator implements Comparator<DePuyEventSeminarVO> {
		public int compare(DePuyEventSeminarVO o1, DePuyEventSeminarVO o2) {
			return o1.getOwner().getFullName().compareTo(o2.getOwner().getFullName());
		}
	}
}
