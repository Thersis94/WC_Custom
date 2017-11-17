package com.mindbody;

import java.rmi.RemoteException;
import java.util.List;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Stub;

import com.mindbody.vo.MindBodyConfig;
import com.mindbody.vo.MindBodyCredentialVO;
import com.mindbody.vo.MindBodyResponseVO;
import com.mindbodyonline.clients.api._0_5_1.MBRequest;
import com.mindbodyonline.clients.api._0_5_1.SourceCredentials;
import com.mindbodyonline.clients.api._0_5_1.UserCredentials;

/****************************************************************************
 * <b>Title:</b> MindBodyApiIntfc.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Interface for implementing MindBodyAPI Endpoints.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 2, 2017
 ****************************************************************************/
public interface MindBodyApiIntfc<T extends Stub, S extends MindBodyConfig> {

	public static final String MINDBODY_SOURCE_NAME = "mindBodySourceName";
	public static final String MINDBODY_API_KEY = "mindBodyApiKey";
	public static final String MINDBODY_SITE_ID = "mindBodySiteId";
	/**
	 * Build User Credentials Object for API Calls
	 * @param userName
	 * @param password
	 * @param siteIds
	 * @return
	 */
	public UserCredentials getUserCredentials(MindBodyCredentialVO user);


	/**
	 * Build Source Credentials Object for API Calls.  Needed on Every Call.
	 * @param sourceName
	 * @param password
	 * @param siteIds
	 * @return
	 */
	public SourceCredentials getSourceCredentials(MindBodyCredentialVO user);


	/**
	 * Return an Instance of type T.  Used in Concretes to instantiate Actual Object
	 * of Type.
	 * @return
	 * @throws AxisFault
	 */
	public T getStub() throws AxisFault;


	/**
	 * Return an instance of type T that has been properly Configured with default
	 * params.
	 * @return
	 * @throws AxisFault
	 */
	public T getConfiguredStub() throws AxisFault;


	/**
	 * Entry point into all Concrete API Implementations that determines action
	 * from passed config.
	 * @param call
	 * @return
	 * @throws RemoteException
	 */
	public MindBodyResponseVO getDocument(S config);

	/**
	 * Performs default Configuration on the given req object that is standard
	 * for all calls using given config.
	 * @param req
	 * @param call
	 */
	public void prepareRequest(MBRequest req, S config);
}