package com.ansmed.sb.util;

import java.util.HashMap;
import java.util.Map;

/****************************************************************************
 * <b>Title</b>: EpiducerNewsletterScheduler.java<p/>
 * <b>Description: </b> Master schedule of all newsletter sends that need to occur after completion 
 * of Epiducer training courses. 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since July 27, 2011
 ****************************************************************************/
public class EpiducerNewsletterScheduler {
	
	private Map<String,Map<String,Integer>> schedule = new HashMap<String,Map<String,Integer>>();
	
	public EpiducerNewsletterScheduler() {
		loadSchedule();
	}
	
	/**
	 * Returns map of "today's" sends or null if no sends for "today"
	 * @param today
	 * @return
	 */
	public Map<String,Integer> retrieveTodaysSchedule(String today) {
		return schedule.get(today);
	}
	
	/**
	 * Initializes the send schedule date map values.
	 */
	private void loadSchedule() {
		Map<String,Integer> sends = new HashMap<String,Integer>();		
		// 8/1/2011 sends
		//sends.put("TIME - Plano, TX, 07/30/11", 1);
		//schedule.put("08/01/2011", sends);
		
		// 08/16/2011 - Tuesday
		//sends = new HashMap<String,Integer>();
		//sends.put("TIME - Plano, TX, 07/30/11", 2);
		sends.put("TIME - Plano, 08/13/11", 1);
		sends.put("TIME - Plano, 08/14/11", 1);
		schedule.put("08/16/2011", sends);
		
		// 08/23/2011
		//sends = new HashMap<String,Integer>();
		//sends.put("TIME - Plano, TX, 08/20/11", 1);
		//sends.put("TIME - Plano, TX, 08/21/11", 1);
		//schedule.put("08/23/2011", sends);
		
		// 08/30/2011
		//sends = new HashMap<String,Integer>();
		//sends.put("TIME - Plano, TX, 07/30/11", 3);
		//sends.put("TIME - Plano, 08/13/11", 2);
		//sends.put("TIME - Plano, 08/14/11", 2);
		//schedule.put("08/30/2011", sends);
		
		// 09/06/2011
		//sends = new HashMap<String,Integer>();
		//sends.put("TIME - Plano, TX, 08/20/11", 2);
		//sends.put("TIME - Plano, TX, 08/21/11", 2);
		//schedule.put("09/06/2011", sends);
		
		// 09/14/2011 MANUAL SEND
		sends = new HashMap<String,Integer>();
		sends.put("TIME - Plano, TX, 07/30/11", 1);
		sends.put("TIME - Plano, 08/13/11", 1);
		sends.put("TIME - Plano, 08/14/11", 1);
		sends.put("TIME - Plano, TX, 08/20/11", 1);
		sends.put("TIME - Plano, TX, 08/21/11", 1);
		sends.put("TIME - Plano, TX, 09/10/11", 1);
		sends.put("TIME - Plano, TX, 09/11/11", 1);
		schedule.put("09/14/2011", sends);
		
		// 09/21/2011
		sends = new HashMap<String,Integer>();
		sends.put("Napa, CA, 09/15/11", 1);
		sends.put("SimSurg - San Francisco, CA, 09/17/11", 1);
		schedule.put("09/21/2011", sends);
		
		// 09/28/2011 MANUAL SEND
		sends = new HashMap<String,Integer>();
		sends.put("TIME - Plano, TX, 07/30/11", 2);
		sends.put("TIME - Plano, 08/13/11", 2);
		sends.put("TIME - Plano, 08/14/11", 2);
		sends.put("TIME - Plano, TX, 08/20/11", 2);
		sends.put("TIME - Plano, TX, 08/21/11", 2);
		sends.put("TIME - Plano, TX, 09/10/11", 2);
		sends.put("TIME - Plano, TX, 09/11/11", 2);
		sends.put("Napa, CA, 09/15/11", 2);
		sends.put("SimSurg - San Francisco, CA, 09/17/11", 2);
		schedule.put("09/28/2011", sends);
		
		// 09/29/2011
		sends = new HashMap<String,Integer>();
		sends.put("VISTA - Baltimore, MD, 09/25/11", 1);
		schedule.put("09/29/2011", sends);
		
		// 10/13/2011 
		sends = new HashMap<String,Integer>();
		//sends.put("VISTA - Baltimore, MD, 09/24/11", 1);
		sends.put("OLC - Rosemont, IL, 10/08/11", 1);
		sends.put("OLC - Rosemont, IL, 10/09/11", 1);
		schedule.put("10/13/2011", sends);
		
		// 10/17/2011
		sends = new HashMap<String,Integer>();
		sends.put("VISTA - Baltimore, MD, 09/25/11", 2);
		schedule.put("10/17/2011", sends);
		
		
		// 10/26/2011
		sends = new HashMap<String,Integer>();
		sends.put("OLC - Rosemont, IL, 10/08/11", 2);
		sends.put("OLC - Rosemont, IL, 10/09/11", 2);
		sends.put("Nicolson Center - Celebration, FL, 10/23/11", 1);
		schedule.put("10/26/2011", sends);
		
		
		// 11/03/2011
		sends = new HashMap<String,Integer>();
		sends.put("Cleveland Clinic - Cleveland OH, 10/29/11", 1);
		schedule.put("11/03/2011", sends);

		// 11/09/2011
		sends = new HashMap<String,Integer>();
		sends.put("Nicolson Center - Celebration, FL, 10/23/11", 2);
		sends.put("TIME - Plano, TX, 11/05/11", 1);
		sends.put("TIME - Plano, TX, 11/06/11", 1);
		schedule.put("11/09/2011", sends);
		
		/*
		// 11/15/2011
		sends = new HashMap<String,Integer>();
		sends.put("Napa, CA, 09/15/11", 5);
		sends.put("SimSurg - San Francisco, CA, 09/17/11", 5);
		schedule.put("11/15/2011", sends);
		
		
		// 11/22/2011
		sends = new HashMap<String,Integer>();
		sends.put("TIME - Plano, TX, 09/10/11", 6);
		sends.put("TIME - Plano, TX, 09/11/11", 6);
		sends.put("VISTA - Baltimore, MD, 09/24/11", 5);
		sends.put("VISTA - Baltimore, MD, 09/25/11", 5);
		sends.put("OLC - Rosemont, IL, 10/08/11", 4);
		sends.put("OLC - Rosemont, IL, 10/09/11", 4);
		sends.put("Nicolson Center - Celebration, FL, 10/23/11", 3);
		sends.put("TIME - Plano, TX, 11/05/11", 2);
		sends.put("TIME - Plano, TX, 11/06/11", 2);
		schedule.put("11/22/2011", sends);
		
		// 11/29/2011
		sends = new HashMap<String,Integer>();
		sends.put("Napa, CA, 09/15/11", 6);
		sends.put("SimSurg - San Francisco, CA, 09/17/11", 6);
		schedule.put("11/29/2011", sends);
		
		// 12/06/2011
		sends = new HashMap<String,Integer>();
		sends.put("VISTA - Baltimore, MD, 09/24/11", 6);
		sends.put("VISTA - Baltimore, MD, 09/25/11", 6);
		sends.put("OLC - Rosemont, IL, 10/08/11", 5);
		sends.put("OLC - Rosemont, IL, 10/09/11", 5);
		sends.put("Nicolson Center - Celebration, FL, 10/23/11", 4);
		sends.put("TIME - Plano, TX, 11/05/11", 3);
		sends.put("TIME - Plano, TX, 11/06/11", 3);
		schedule.put("12/06/2011", sends);
		
		// 12/13/2011
		sends = new HashMap<String,Integer>();
		sends.put("Oquendo Center - Las Vegas, NV (NANS), 12/08/11", 1);
		schedule.put("12/13/2011", sends);		
		
		// 12/20/2011
		sends = new HashMap<String,Integer>();
		sends.put("OLC - Rosemont, IL, 10/08/11", 6);
		sends.put("OLC - Rosemont, IL, 10/09/11", 6);
		sends.put("Nicolson Center - Celebration, FL, 10/23/11", 5);
		sends.put("TIME - Plano, TX, 11/05/11", 4);
		sends.put("TIME - Plano, TX, 11/06/11", 4);
		sends.put("TIME - Plano, TX, 12/17/11", 1);
		schedule.put("12/20/2011", sends);
		
		// 12/27/2011
		sends = new HashMap<String,Integer>();
		sends.put("Oquendo Center - Las Vegas, NV (NANS), 12/08/11", 2);
		schedule.put("12/27/2011", sends);
		
		// 01/03/2012
		sends = new HashMap<String,Integer>();
		sends.put("Nicolson Center - Celebration, FL, 10/23/11", 6);
		sends.put("TIME - Plano, TX, 11/05/11", 5);
		sends.put("TIME - Plano, TX, 11/06/11", 5);
		sends.put("TIME - Plano, TX, 12/17/11", 2);
		schedule.put("01/03/2012", sends);
		
		// 01/10/2012
		sends = new HashMap<String,Integer>();
		sends.put("Oquendo Center - Las Vegas, NV (NANS), 12/08/11", 3);
		schedule.put("01/10/2012", sends);
		
		// 01/17/2012
		sends = new HashMap<String,Integer>();
		sends.put("TIME - Plano, TX, 11/05/11", 6);
		sends.put("TIME - Plano, TX, 11/06/11", 6);
		sends.put("TIME - Plano, TX, 12/17/11", 3);
		schedule.put("01/17/2012", sends);
		
		// 01/24/2012
		sends = new HashMap<String,Integer>();
		sends.put("Oquendo Center - Las Vegas, NV (NANS), 12/08/11", 4);
		schedule.put("01/24/2012", sends);
		
		// 01/31/2012
		sends = new HashMap<String,Integer>();
		sends.put("TIME - Plano, TX, 12/17/11", 4);
		schedule.put("01/31/2012", sends);
		
		// 02/07/2012
		sends = new HashMap<String,Integer>();
		sends.put("Oquendo Center - Las Vegas, NV (NANS), 12/08/11", 5);
		schedule.put("02/07/2012", sends);
		
		// 02/15/2012
		sends = new HashMap<String,Integer>();
		sends.put("TIME - Plano, TX, 12/17/11", 5);
		schedule.put("02/15/2012", sends);
		
		// 02/21/2012
		sends = new HashMap<String,Integer>();
		sends.put("Oquendo Center - Las Vegas, NV (NANS), 12/08/11", 6);		
		schedule.put("02/21/2012", sends);
		
		// 02/29/2012
		sends = new HashMap<String,Integer>();
		sends.put("TIME - Plano, TX, 12/17/11", 6);
		schedule.put("02/29/2012", sends);
		*/
	}
	
}
