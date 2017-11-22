package com.mindbody.vo.clients;

import java.util.ArrayList;
import java.util.List;

import com.mindbody.MindBodyClientApi.ClientDocumentType;
import com.mindbody.util.MindBodyUtil;
import com.mindbody.vo.MindBodyCredentialVO;
import com.mindbodyonline.clients.api._0_5_1.Client;
import com.siliconmtn.commerce.payment.PaymentVO;
import com.siliconmtn.security.UserDataVO;

/****************************************************************************
 * <b>Title:</b> MindBodyAddOrUpdateClientsConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages AddOrUpdateClients Endpoint unique Configuration.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 13, 2017
 ****************************************************************************/
public class MindBodyAddOrUpdateClientsConfig extends MindBodyClientConfig {

	private List<Client> clients;
	private boolean sendEmail;
	private boolean test;
	/**
	 * @param type
	 * @param source
	 * @param user
	 */
	public MindBodyAddOrUpdateClientsConfig(MindBodyCredentialVO source, MindBodyCredentialVO user) {
		super(ClientDocumentType.ADD_OR_UPDATE_CLIENTS, source, user);
		clients = new ArrayList<>();
	}

	/**
	 * @return the clients
	 */
	public List<Client> getClients() {
		return clients;
	}

	/**
	 * @return the sendEmail
	 */
	public boolean isSendEmail() {
		return sendEmail;
	}

	public boolean isTest() {
		return test;
	}

	public void addClient(Client client) {
		clients.add(client);
	}

	public void addClient(UserDataVO client, PaymentVO card) {
		Client c = MindBodyUtil.convertClient(client);

		if(card != null) {
			c.setClientCreditCard(MindBodyUtil.convertClientCreditCard(client.getLocation(), card));
		}

		clients.add(c);
	}

	/**
	 * @param clients the clients to set.
	 */
	public void setClients(List<Client> clients) {
		this.clients = clients;
	}

	/**
	 * @param sendEmail the sendEmail to set.
	 */
	public void setSendEmail(boolean sendEmail) {
		this.sendEmail = sendEmail;
	}

	public void setTest(boolean test) {
		this.test = test;
	}
}