package com.mts.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.mts.subscriber.action.SubscriptionAction.SubscriptionType;
import com.mts.subscriber.data.MTSUserVO;
import com.mts.subscriber.data.SubscriptionUserVO;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: MTSSubscriberImporter.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Importer of MTS Subscribers 
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 24, 2019
 * @updates:
 ****************************************************************************/
public class MTSSubscriberImporter {
	
	private static final Logger log = Logger.getLogger(MTSSubscriberImporter.class);
	public static final String FILE_LOC = "/home/etewa/Downloads/MTS_subscribers.txt";
	private Map<String, String> typeMap = new HashMap<>();
	
	/**
	 * 
	 */
	public MTSSubscriberImporter() {
		super();
		
		typeMap.put("single", "USER");
		typeMap.put("multi", "MULTIPLE");
		typeMap.put("corporate", "CORPORATE");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		log.info("---------- Starting");
		MTSSubscriberImporter imp = new MTSSubscriberImporter();
		List<MTSUserVO> users = imp.parseUserFile();
		
		log.info("-------- Complete");
	}
	
	
	public List<MTSUserVO> parseUserFile() throws FileNotFoundException, IOException {
		List<MTSUserVO> users = new ArrayList<>(384);
		int ctr = 0;
		try (BufferedReader br = new BufferedReader(new FileReader(new File(FILE_LOC)))) {
			String temp;
			MTSUserVO user = new MTSUserVO();
			while((temp = br.readLine()) != null) {
				String[] cols = temp.split("\\t");
				SubscriptionUserVO subscription = new SubscriptionUserVO();
				subscription.setPublicationId("MED_TECH_STRATEGIST");
				subscription.setUserId(cols[0]);
				user.setUserId(cols[0]);
				user.setSecondaryUserId(cols[0]);
				user.setFirstName(cols[2]);
				user.setLastName(cols[3]);
				user.setCompanyName(cols[4]);
				user.addSubscription(subscription);
				//user.setSubscriptionType(SubscriptionType.valueOf(typeMap.get(cols[6].toLowerCase())));
				user.setCreateDate(Convert.formatDate(cols[7]));
				user.setExpirationDate(Convert.formatDate(cols[8]));
				users.add(user);
				log.info(ctr++);
			}
		}
		
		log.info("Num Users: " + ctr);
		return users;
	}

}
