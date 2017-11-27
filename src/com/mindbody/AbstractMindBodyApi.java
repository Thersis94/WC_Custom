package com.mindbody;

import java.rmi.RemoteException;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.client.Stub;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.log4j.Logger;

import com.mindbody.util.MindBodyUtil;
import com.mindbody.vo.MindBodyConfig;
import com.mindbody.vo.MindBodyCredentialVO;
import com.mindbody.vo.MindBodyResponseVO;
//Mind Body Jar
import com.mindbodyonline.clients.api._0_5_1.MBRequest;
import com.mindbodyonline.clients.api._0_5_1.SourceCredentials;
import com.mindbodyonline.clients.api._0_5_1.UserCredentials;
import com.siliconmtn.common.http.HttpStatus;

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
	public UserCredentials getUserCredentials(MindBodyCredentialVO user) {
		UserCredentials uc = UserCredentials.Factory.newInstance();
		uc.setPassword(user.getPassword());
		uc.setUsername(user.getUserName());
		uc.setSiteIDs(MindBodyUtil.buildArrayOfInt(user.getSiteIds()));

		return uc;
	}


	/**
	 * Manage Generating SourceCredentials which are required on all API Calls.
	 * @param sourceName
	 * @param password
	 * @param siteIds
	 */
	@Override
	public SourceCredentials getSourceCredentials(MindBodyCredentialVO source) {
		SourceCredentials sc = SourceCredentials.Factory.newInstance();
		sc.setPassword(source.getPassword());
		sc.setSourceName(source.getUserName());
		sc.setSiteIDs(MindBodyUtil.buildArrayOfInt(source.getSiteIds()));

		return sc;
	}

	/* (non-Javadoc)
	 * @see com.mindbody.MindBodyApiIntfc#getDocument(com.mindbody.vo.MindBodyCallVO)
	 */
	@Override
	public MindBodyResponseVO getDocument(S conf) {
		MindBodyResponseVO resp;

		if(conf.isValid()) {
			try {
				resp = processRequest(conf);
			} catch(RemoteException e) {
				log.error("Problem With Connection.", e);
				resp = buildErrorResponse(HttpStatus.CD_500_INTERNAL_SERVER_ERROR, "Problem Occurred .");
			}
		} else {
			resp = buildErrorResponse(HttpStatus.CD_400_BAD_REQUEST, "Invalid Config Passed.");
		}
		return resp;
	}

	public MindBodyResponseVO getAllDocuments(S conf) {

		MindBodyResponseVO res = getDocument(conf);
		conf.setPagesize(res.getResultCount() + 1);
		res = getDocument(conf);

		return res;
	}

	/**
	 * Manage configuring the Client that the API will use for generating calls.
	 * Need to set Chunked on the client so that the header Length will be
	 * correct.
	 * @throws AxisFault 
	 */
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
			req.setSourceCredentials(getSourceCredentials(config.getSourceCredentials()));

			//If Config has User Credentials, add them.
			if(config.hasUser()) {
				req.setUserCredentials(getUserCredentials(config.getUserCredentials()));
			}

			//Set Standard Config Params.
			req.setXMLDetail(config.getXmlDetailLevel());
			req.setPageSize(config.getPageSize());
			req.setCurrentPageIndex(config.getPageNo());

			//Set Any fields configured on the Config.
			if(config.hasFields()) {
				req.setFields(MindBodyUtil.buildArrayOfString(config.getFields()));
			}
			
		} else {
			throw new IllegalArgumentException("Config Object is Invalid.");
		}
	}

	protected MindBodyResponseVO buildErrorResponse(int errorCode, String message) {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		resp.setErrorCode(errorCode);
		resp.setMessage(message);

		return resp;
	}

	protected abstract MindBodyResponseVO processRequest(S config) throws RemoteException;

}