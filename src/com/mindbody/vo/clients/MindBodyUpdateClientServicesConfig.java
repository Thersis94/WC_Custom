package com.mindbody.vo.clients;

import java.util.ArrayList;
import java.util.List;

import com.mindbody.MindBodyClientApi.ClientDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;
import com.mindbodyonline.clients.api._0_5_1.ClientService;

/****************************************************************************
 * <b>Title:</b> MindBodyUpdateClientServicesConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages UpdateClientServices Endpoint unique Configuration.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 14, 2017
 ****************************************************************************/
public class MindBodyUpdateClientServicesConfig extends MindBodyClientConfig {

	private List<ClientService> clientServices;
	private boolean test;

	/**
	 * @param type
	 * @param source
	 * @param user
	 */
	public MindBodyUpdateClientServicesConfig(MindBodyCredentialVO source, MindBodyCredentialVO user) {
		super(ClientDocumentType.UPDATE_CLIENT_SERVICES, source, user);
		this.clientServices = new ArrayList<>();
	}

	/**
	 * @return the clientServices
	 */
	public List<ClientService> getClientServices() {
		return clientServices;
	}

	/**
	 * @return the test
	 */
	public boolean isTest() {
		return test;
	}

	/**
	 * @param clientServices the clientServices to set.
	 */
	public void setClientServices(List<ClientService> clientServices) {
		this.clientServices = clientServices;
	}

	public void addClientService(ClientService cs) {
		this.clientServices.add(cs);
	}
	/**
	 * @param test the test to set.
	 */
	public void setTest(boolean test) {
		this.test = test;
	}

	@Override
	public boolean isValid() {
		return super.isValid() && !getClientServices().isEmpty();
	}
}