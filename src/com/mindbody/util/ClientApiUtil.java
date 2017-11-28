package com.mindbody.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mindbody.MindBodyClientApi;
import com.mindbody.security.MindBodyUserVO;
import com.mindbody.vo.MindBodyResponseVO;
import com.mindbody.vo.classes.MBClassScheduleVO;
import com.mindbody.vo.clients.MBClientServiceVO;
import com.mindbody.vo.clients.MBVisitVO;
import com.mindbody.vo.clients.MindBodyAddOrUpdateClientsConfig;
import com.mindbody.vo.clients.MindBodyClientConfig;
import com.mindbody.vo.clients.MindBodyGetClientAccountBalancesConfig;
import com.mindbody.vo.clients.MindBodyGetClientPurchasesConfig;
import com.mindbody.vo.clients.MindBodyGetClientReferralTypesConfig;
import com.mindbody.vo.clients.MindBodyGetClientScheduleConfig;
import com.mindbody.vo.clients.MindBodyGetClientServicesConfig;
import com.mindbody.vo.clients.MindBodyGetClientVisitsConfig;
import com.mindbody.vo.clients.MindBodyGetClientsConfig;
import com.mindbody.vo.clients.MindBodyGetCustomClientFieldsConfig;
import com.mindbody.vo.clients.MindBodyGetRequiredClientFieldsConfig;
import com.mindbody.vo.clients.MindBodyValidateLoginConfig;
import com.mindbody.vo.sales.MBSaleItemVO;
import com.siliconmtn.commerce.payment.PaymentVO;
import com.siliconmtn.security.UserDataVO;

/****************************************************************************
 * <b>Title:</b> ClientApiUtil.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Wrap MindBodyConfig Generation and common calls into
 * one location for actions.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 20, 2017
 ****************************************************************************/
public class ClientApiUtil {
	private Map<String, String> config;
	private MindBodyClientApi api;
	public enum MBUserStatus {VALID, INVALID_CREDENTIALS, NOT_PRESENT}


	public ClientApiUtil(Map<String, String> config) {
		this.config = config;
		this.api = new MindBodyClientApi();
	}

	public MindBodyResponseVO addOrUpdateClients(UserDataVO user, PaymentVO card) {
		MindBodyAddOrUpdateClientsConfig callConfig = new MindBodyAddOrUpdateClientsConfig(MindBodyUtil.buildSourceCredentials(config), MindBodyUtil.buildStaffCredentials(config));
		callConfig.addClient(user, card);
		return api.getDocument(callConfig);
	}

	/**
	 * Attempt to retrieve a WC Users Status via MindBody's Client Api.
	 * @param user
	 * @return
	 */
	public MBUserStatus getClientStatus(UserDataVO user) {
		MBUserStatus stat = null;

		MindBodyResponseVO resp = validateClient(user);

		//If User is validated then they already exist.
		if(resp.isValid() && !resp.getResults().isEmpty()) {
			stat = MBUserStatus.VALID;
		} else {
			stat = searchUser(user);
		}
		return stat;
	}

	/**
	 * @param user
	 * @return
	 */
	private MBUserStatus searchUser(UserDataVO user) {
		MindBodyResponseVO r = getClient(user.getEmailAddress());
		MBUserStatus stat = MBUserStatus.NOT_PRESENT;
		/*
		 * If we found a user but couldn't validate then our credentials
		 * are invalid.
		 */
		if(r.isValid() && r.getResultCount() > 0) {
			for(Object o : r.getResults()) {
				if(((UserDataVO)o).getEmailAddress().equalsIgnoreCase(user.getEmailAddress())) {
					stat = MBUserStatus.INVALID_CREDENTIALS;
					break;
				}
			}
		}
		return stat;
	}

	/**
	 * Attempt to lookup a Client in the MindBody System by emailAddress.
	 * @param user
	 * @return
	 */
	public MindBodyResponseVO getClient(String searchText) {
		MindBodyGetClientsConfig clients = new MindBodyGetClientsConfig(MindBodyUtil.buildSourceCredentials(config), MindBodyUtil.buildStaffCredentials(config), true);
		clients.setSearchText(searchText);
		return api.getDocument(clients);
	}

	/**
	 * Attempt to validate a UserDataVO against MindBody's Client Api.
	 * @param user
	 * @return
	 */
	public MindBodyResponseVO validateClient(UserDataVO user) {
		MindBodyValidateLoginConfig login = new MindBodyValidateLoginConfig(MindBodyUtil.buildSourceCredentials(config));
		login.setUserName(user.getEmailAddress());
		login.setPassword(user.getProfileId());
		return api.getDocument(login);
	}

	public MindBodyResponseVO getAccountBalances(String clientId) {
		MindBodyGetClientAccountBalancesConfig conf = new MindBodyGetClientAccountBalancesConfig(MindBodyUtil.buildSourceCredentials(config), MindBodyUtil.buildStaffCredentials(config));
		conf.addClientId(clientId);
		return api.getDocument(conf);
	}

	public MindBodyResponseVO getClientPurchases(String clientId) {
		MindBodyGetClientPurchasesConfig conf = new MindBodyGetClientPurchasesConfig(MindBodyUtil.buildSourceCredentials(config));
		conf.addClientId(clientId);
		return api.getDocument(conf);
	}

	public MindBodyResponseVO getClientSchedule(String clientId) {
		MindBodyGetClientScheduleConfig conf = new MindBodyGetClientScheduleConfig(MindBodyUtil.buildSourceCredentials(config));
		conf.addClientId(clientId);
		return api.getDocument(conf);
	}

	public MindBodyResponseVO getClientServices(String clientId) {
		MindBodyGetClientServicesConfig conf = new MindBodyGetClientServicesConfig(MindBodyUtil.buildSourceCredentials(config));
		conf.addClientId(clientId);
		return api.getDocument(conf);
	}

	public MindBodyResponseVO getClientVisits(String clientId) {
		MindBodyGetClientVisitsConfig conf = new MindBodyGetClientVisitsConfig(MindBodyUtil.buildSourceCredentials(config));
		conf.addClientId(clientId);
		return api.getDocument(conf);
	}

	public List<String> getRequiredClientFields() {
		MindBodyGetRequiredClientFieldsConfig conf = new MindBodyGetRequiredClientFieldsConfig(MindBodyUtil.buildSourceCredentials(config));
		return listResults(conf);
	}

	public List<String> getCustomClientFields() {
		MindBodyGetCustomClientFieldsConfig conf = new MindBodyGetCustomClientFieldsConfig(MindBodyUtil.buildSourceCredentials(config));

		return listResults(conf);
	}

	public List<String> getClientReferralTypes() {
		MindBodyGetClientReferralTypesConfig conf = new MindBodyGetClientReferralTypesConfig(MindBodyUtil.buildSourceCredentials(config));
		return listResults(conf);
	}

	private List<String> listResults(MindBodyClientConfig conf) {
		List<String> fields = new ArrayList<>();

		MindBodyResponseVO resp = api.getDocument(conf);
		if(resp.isValid()) {
			for(Object o : resp.getResults()) {
				fields.add(o.toString());
			}
		}

		return fields;
	}

	@SuppressWarnings("unchecked")
	public void reloadUserData(MindBodyUserVO user) {

		//Load Visits
		MindBodyResponseVO resp = getClientVisits(user.getClientId());
		if(resp.isValid()) {
			user.setVisits((List<MBVisitVO>)(List<?>)resp.getResults());
		}

		//Load Purchases
		resp = getClientPurchases(user.getClientId());
		if(resp.isValid()) {
			user.setPurchases((List<MBSaleItemVO>)(List<?>)resp.getResults());
		}
		//Load Services
		resp = getClientServices(user.getClientId());
		if(resp.isValid()) {
			user.setServices((List<MBClientServiceVO>)(List<?>)resp.getResults());
		}

		//Load Schedule
		resp = getClientSchedule(user.getClientId());
		if(resp.isValid()) {
			user.setSchedule((List<MBClassScheduleVO>)(List<?>)resp.getResults());
		}
	}
}