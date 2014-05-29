package com.ram.action;

// JDK 1.7.x
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ram.datafeed.data.AuditorVO;
import com.ram.datafeed.data.InventoryEventAuditorVO;
import com.ram.datafeed.data.InventoryEventReturnVO;
import com.ram.datafeed.data.InventoryEventVO;
//import com.ram.datafeed.data.InventoryEventVO;
//SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.http.SMTServletRequest;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: EventAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b>Manages the vent data for the ram analytics engine
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james camire
 * @version 1.0
 * @since May 27, 2014<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class EventAction extends SBActionAdapter {

	/**
	 * 
	 */
	public EventAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public EventAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.info("Event Action Called");


		
		Map<String, Object> data = new HashMap<>();
		List<InventoryEventVO> items = getEvents();
		data.put("count", items.size());
		data.put("data", items);
		data.put(GlobalConfig.SUCCESS_KEY, Boolean.TRUE);
		this.putModuleData(data, 3, false);
	}
	
	
	private List<InventoryEventVO> getEvents() {
		List<InventoryEventVO> items = new ArrayList<>();
		items.add(getEvent(1));
		items.add(getEvent(2));
		items.add(getEvent(3));
		return items;
	}
	
	private InventoryEventVO getEvent(int i) {
		InventoryEventVO event = new InventoryEventVO();
		event.setInventoryEventId(1234 * i);
		event.setLocationName("Some Location: " + i);
		event.setScheduleDate(new Date());
		event.setDataLoadCompleteDate(new Date());
		if ((i % 2) == 0) event.setInventoryCompleteDate(new Date());
		
		InventoryEventReturnVO ret = new InventoryEventReturnVO();
		ret.setEventReturnId(789 * i);
		event.addReturnProduct(ret);
		
		ret = new InventoryEventReturnVO();
		ret.setEventReturnId(345 * i);
		event.addReturnProduct(ret);
		
		
		InventoryEventAuditorVO aud = new InventoryEventAuditorVO();
		aud.setAuditorName("James Camire");
		event.addAuditor(aud);
		
		aud = new InventoryEventAuditorVO();
		aud.setAuditorName("Billy Larsen");
		event.addAuditor(aud);
		return event;
	}

}
