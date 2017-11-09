package com.mindbody;

import java.util.List;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.client.Stub;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.log4j.Logger;

import com.mindbody.vo.MindBodyConfig;
import com.mindbodyonline.clients.api._0_5_1.ArrayOfInt;
import com.mindbodyonline.clients.api._0_5_1.ArrayOfLong;
import com.mindbodyonline.clients.api._0_5_1.ArrayOfString;
import com.mindbodyonline.clients.api._0_5_1.MBRequest;
import com.mindbodyonline.clients.api._0_5_1.SourceCredentials;
import com.mindbodyonline.clients.api._0_5_1.UserCredentials;
import com.mindbodyonline.clients.api._0_5_1.XMLDetailLevel;

/****************************************************************************
 * <b>Title:</b> AbstractMindBodyApi.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages all the commonalities between the various
 * MindBody API sets.
 * <b>Copyright:</b> Copyright (c) 2017 
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 2, 2017
 ****************************************************************************/
public abstract class AbstractMindBodyApi<T extends Stub, S extends MindBodyConfig> implements MindBodyApiIntfc<T, S> {

	protected Logger log;
	public static final int PAGE_SIZE = 25;

	/**
	 * 
	 */
	public AbstractMindBodyApi() {
		log = Logger.getLogger(getClass());
	}


	/**
	 * Manage Generating UserCredentials which are used on most API Calls.
	 * 
	 * @param string
	 * @param string2
	 * @param i
	 * @return
	 */
	@Override
	public UserCredentials getUserCredentials(String userName, String password, List<Integer> siteIds) {
		UserCredentials uc = UserCredentials.Factory.newInstance();
		uc.setPassword(password);
		uc.setUsername(userName);
		uc.setSiteIDs(buildArrayOfInt(siteIds));

		return uc;
	}


	/**
	 * Manage Generating SourceCredentials which are required on all API Calls.
	 * @param sourceName
	 * @param password
	 * @param siteIds
	 */
	@Override
	public SourceCredentials getSourceCredentials(String sourceName, String password, List<Integer> siteIds) {
		SourceCredentials sc = SourceCredentials.Factory.newInstance();
		sc.setPassword(password);
		sc.setSourceName(sourceName);
		sc.setSiteIDs(buildArrayOfInt(siteIds));

		return sc;
	}


	/**
	 * Manage configuring the Client that the API will use for generating calls.
	 * Need to set Chunked on the client so that the header Length will be
	 * correct.
	 * @throws AxisFault 
	 */
	@Override
	public T getConfiguredStub() throws AxisFault {
		T stub = getStub();
		ServiceClient client = stub._getServiceClient();
		client.getOptions().setProperty(HTTPConstants.CHUNKED, false);

		return stub;
	}

	/**
	 * Populates the MBRequest with generic fields common to all the calls.
	 * @param req
	 * @param config
	 */
	public void prepareRequest(MBRequest req, S config) {
		if(config.isValid()) {

			//Always Add Source Credentials.
			req.setSourceCredentials(getSourceCredentials(config.getSourceName(), config.getSourceKey(), config.getSiteIds()));

			//If Config has User Credentials, add them.
			if(config.hasUser()) {
				req.setUserCredentials(getUserCredentials(config.getUserName(), config.getUserPass(), config.getSiteIds()));
			}

			//Set Standard Config Params.
			req.setXMLDetail(XMLDetailLevel.FULL);
			req.setPageSize(config.getPageSize());
			req.setCurrentPageIndex(config.getPageNo());

			//Set Any fields configured on the Config.
			if(config.hasFields()) {
				ArrayOfString fields = ArrayOfString.Factory.newInstance();
				for(String f : config.getFields()) {
					fields.addString(f);
				}
				req.setFields(fields);
			}
		} else {
			throw new IllegalArgumentException("Config Object is Invalid.");
		}
	}

	/**
	 * Builds a MindBody ArrayOfInt Object from provided List.
	 * @param config
	 * @param req
	 */
	protected ArrayOfInt buildArrayOfInt(List<Integer> vals) {
		ArrayOfInt locIds = ArrayOfInt.Factory.newInstance();
		for(int i : vals) {
			locIds.addInt(i);
		}
		return locIds;
	}

	/**
	 * Builds a MindBody ArrayOfLong Object from provided List.
	 * @param config
	 * @param req
	 */
	protected ArrayOfLong buildArrayOfLong(List<Long> vals) {
		ArrayOfLong locIds = ArrayOfLong.Factory.newInstance();
		for(long i : vals) {
			locIds.addLong(i);
		}
		return locIds;
	}
}