package com.mts.action.email;

// JDK 1.8.x
import java.io.IOException;
import java.util.ArrayList;

// Google Gson 2.x
import com.google.gson.Gson;

// MTS Libs
import com.mts.action.email.data.EmailEventVO;
import com.mts.action.email.data.EventVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.io.http.SMTHttpConnectionManager;

// WC Libs 
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.util.CacheAdministrator;

/****************************************************************************
 * <b>Title</b>: EventEmailWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Gets the events from the MTS events page and adds to the email.
 * Events are cached for 3 days, so the data will only be pulled periodically
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 18, 2020
 * @updates:
 ****************************************************************************/
public class EventEmailWidget extends SimpleActionAdapter {
	/**
	 * URL of the MTS Event calendar
	 */
	public static final String EVENT_URL = "https://www.medtechstrategist.com/events?format=json";
	
	/**
	 * Cache key id of the event json
	 */
	public static final String WC_CACHE_KEY = "MTS_CACHE_EVENT";
	
	/**
	 * 
	 */
	public EventEmailWidget() {
		super();
	}

	/**
	 * @param arg0
	 */
	public EventEmailWidget(ActionInitVO arg0) {
		super(arg0);
	}
	
	/*
	 * (non-javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("retrieving Email Event");
		try {
			EventVO events = getEvents();
			setModuleData(events.getUpcoming(), events.getUpcoming().size());
			
		} catch (Exception e) {
			log.error("Unable to retrieve events", e);
			setModuleData(new ArrayList<EmailEventVO>(), 0, e.getLocalizedMessage());
		}
	}
	
	/**
	 * Retrieves the events from the MTS Event website
	 * @throws IOException
	 */
	public EventVO getEvents() throws IOException {
		// Check from cache first
		Object cacheItem = new CacheAdministrator(attributes).readObjectFromCache(WC_CACHE_KEY);
		if (cacheItem != null) return (EventVO)cacheItem; 
		
		// If not in cache, retrieve
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		byte[] data = conn.retrieveData(EVENT_URL);
		
		// Parse into a Java Object
		Gson g = new Gson();
		EventVO event = g.fromJson(new String(data), EventVO.class);
		
		// Add to cache for 3 days
		new CacheAdministrator(attributes).writeToCache(WC_CACHE_KEY, event, 259200);
		
		// Return the newly retrieved element
		return event;

	}

}
