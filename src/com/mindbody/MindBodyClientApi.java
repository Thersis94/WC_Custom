package com.mindbody;

import java.rmi.RemoteException;

import org.apache.axis2.AxisFault;
import org.mortbay.jetty.HttpStatus;

import com.mindbody.vo.MindBodyResponseVO;
import com.mindbody.vo.clients.MindBodyAddArrivalConfig;
import com.mindbody.vo.clients.MindBodyAddOrUpdateClientsConfig;
import com.mindbody.vo.clients.MindBodyClientConfig;
import com.mindbody.vo.clients.MindBodyGetClientAccountBalancesConfig;
import com.mindbody.vo.clients.MindBodyGetClientPurchasesConfig;
import com.mindbody.vo.clients.MindBodyGetClientScheduleConfig;
import com.mindbody.vo.clients.MindBodyGetClientServicesConfig;
import com.mindbody.vo.clients.MindBodyGetClientVisitsConfig;
import com.mindbody.vo.clients.MindBodyGetClientsConfig;
import com.mindbody.vo.clients.MindBodyGetCustomClientFieldsConfig;
import com.mindbody.vo.clients.MindBodyGetRequiredClientFieldsConfig;
import com.mindbody.vo.clients.MindBodySendUserNewPasswordConfig;
import com.mindbody.vo.clients.MindBodyUpdateClientServicesConfig;
import com.mindbody.vo.clients.MindBodyUploadClientDocumentConfig;
import com.mindbody.vo.clients.MindBodyValidateLoginConfig;
import com.mindbodyonline.clients.api._0_5_1.AddArrivalDocument;
import com.mindbodyonline.clients.api._0_5_1.AddArrivalRequest;
import com.mindbodyonline.clients.api._0_5_1.AddArrivalResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.AddArrivalResult;
import com.mindbodyonline.clients.api._0_5_1.AddOrUpdateClientsDocument;
import com.mindbodyonline.clients.api._0_5_1.AddOrUpdateClientsRequest;
import com.mindbodyonline.clients.api._0_5_1.AddOrUpdateClientsResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.AddOrUpdateClientsResult;
import com.mindbodyonline.clients.api._0_5_1.ClientSendUserNewPasswordRequest;
import com.mindbodyonline.clients.api._0_5_1.Client_x0020_ServiceStub;
import com.mindbodyonline.clients.api._0_5_1.GetClientAccountBalancesDocument;
import com.mindbodyonline.clients.api._0_5_1.GetClientAccountBalancesRequest;
import com.mindbodyonline.clients.api._0_5_1.GetClientAccountBalancesResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.GetClientAccountBalancesResult;
import com.mindbodyonline.clients.api._0_5_1.GetClientPurchasesDocument;
import com.mindbodyonline.clients.api._0_5_1.GetClientPurchasesRequest;
import com.mindbodyonline.clients.api._0_5_1.GetClientPurchasesResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.GetClientPurchasesResult;
import com.mindbodyonline.clients.api._0_5_1.GetClientScheduleDocument;
import com.mindbodyonline.clients.api._0_5_1.GetClientScheduleRequest;
import com.mindbodyonline.clients.api._0_5_1.GetClientScheduleResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.GetClientScheduleResult;
import com.mindbodyonline.clients.api._0_5_1.GetClientServicesDocument;
import com.mindbodyonline.clients.api._0_5_1.GetClientServicesRequest;
import com.mindbodyonline.clients.api._0_5_1.GetClientServicesResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.GetClientServicesResult;
import com.mindbodyonline.clients.api._0_5_1.GetClientVisitsDocument;
import com.mindbodyonline.clients.api._0_5_1.GetClientVisitsRequest;
import com.mindbodyonline.clients.api._0_5_1.GetClientVisitsResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.GetClientVisitsResult;
import com.mindbodyonline.clients.api._0_5_1.GetClientsDocument;
import com.mindbodyonline.clients.api._0_5_1.GetClientsRequest;
import com.mindbodyonline.clients.api._0_5_1.GetClientsResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.GetClientsResult;
import com.mindbodyonline.clients.api._0_5_1.GetCustomClientFieldsDocument;
import com.mindbodyonline.clients.api._0_5_1.GetCustomClientFieldsRequest;
import com.mindbodyonline.clients.api._0_5_1.GetCustomClientFieldsResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.GetCustomClientFieldsResult;
import com.mindbodyonline.clients.api._0_5_1.GetRequiredClientFieldsDocument;
import com.mindbodyonline.clients.api._0_5_1.GetRequiredClientFieldsRequest;
import com.mindbodyonline.clients.api._0_5_1.GetRequiredClientFieldsResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.GetRequiredClientFieldsResult;
import com.mindbodyonline.clients.api._0_5_1.SendUserNewPasswordDocument;
import com.mindbodyonline.clients.api._0_5_1.SendUserNewPasswordResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.UpdateClientServicesDocument;
import com.mindbodyonline.clients.api._0_5_1.UpdateClientServicesRequest;
import com.mindbodyonline.clients.api._0_5_1.UpdateClientServicesResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.UpdateClientServicesResult;
import com.mindbodyonline.clients.api._0_5_1.UploadClientDocumentDocument1;
import com.mindbodyonline.clients.api._0_5_1.UploadClientDocumentRequest;
import com.mindbodyonline.clients.api._0_5_1.UploadClientDocumentResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.UploadClientDocumentResult;
import com.mindbodyonline.clients.api._0_5_1.ValidateLoginDocument;
import com.mindbodyonline.clients.api._0_5_1.ValidateLoginRequest;
import com.mindbodyonline.clients.api._0_5_1.ValidateLoginResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.ValidateLoginResult;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title:</b> MindbodyClientApi.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manage Mind Body Client Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 3, 2017
 ****************************************************************************/
public class MindBodyClientApi extends AbstractMindBodyApi<Client_x0020_ServiceStub, MindBodyClientConfig> {

	public enum ClientDocumentType {
		ADD_ARRIVAL,
		ADD_OR_UPDATE_CLIENTS,
		UPDATE_CLIENT_CROSS_REGIONAL,
		GET_CLIENTS,
		GET_CROSS_REGIONAL_CLIENT_ASSOCIATIONS,
		GET_CUSTOM_CLIENT_FIELDS,
		GET_CLIENT_INDEXES,
		GET_CLIENT_CONTACT_LOGS,
		ADD_OR_UPDATE_CONTACT_LOGS,
		GET_CONTACT_LOG_TYPES,
		UPLOAD_CLIENT_DOCUMENT,
		GET_CLIENT_FORMULA_NOTES,
		ADD_CLIENT_FORMULA_NOTE,
		DELETE_CLIENT_FORMULA_NOTE,
		GET_CLIENT_REFERRAL_TYPES,
		GET_ACTIVE_CLIENT_MEMBERSHIPS,
		GET_CLIENT_CONTRACTS,
		GET_CLIENT_ACCOUNT_BALANCES,
		GET_CLIENT_SERVICES,
		GET_CLIENT_VISITS,
		GET_CLIENT_PURCHASES,
		GET_CLIENT_SCHEDULE,
		GET_REQUIRED_CLIENT_FIELDS,
		VALIDATE_LOGIN,
		UPDATE_CLIENT_SERVICES,
		SEND_USER_NEW_PASSWORD
	}

	/**
	 * 
	 */
	public MindBodyClientApi() {
		super();
	}

	/* (non-Javadoc)
	 * @see com.mindbody.MindBodyApiIntfc#getStub()
	 */
	@Override
	public Client_x0020_ServiceStub getStub() throws AxisFault {
		return new Client_x0020_ServiceStub();
	}

	/* (non-Javadoc)
	 * @see com.mindbody.AbstractMindBodyApi#processRequest(com.mindbody.vo.MindBodyConfig)
	 */
	@Override
	protected MindBodyResponseVO processRequest(MindBodyClientConfig config) throws RemoteException {
		MindBodyResponseVO resp;
		switch (config.getType()) {
			case ADD_ARRIVAL:
				resp = addArrival((MindBodyAddArrivalConfig)config);
				break;
			case ADD_OR_UPDATE_CLIENTS:
				resp = addOrUpdateClients((MindBodyAddOrUpdateClientsConfig)config);
				break;
			case GET_CLIENTS:
				resp = getClients((MindBodyGetClientsConfig)config);
				break;
			case GET_CLIENT_ACCOUNT_BALANCES:
				resp = getClientAccountBalances((MindBodyGetClientAccountBalancesConfig)config);
				break;
			case GET_CLIENT_PURCHASES:
				resp = getClientPurchases((MindBodyGetClientPurchasesConfig)config);
				break;
			case GET_CLIENT_SCHEDULE:
				resp = getClientSchedule((MindBodyGetClientScheduleConfig)config);
				break;
			case GET_CLIENT_SERVICES:
				resp = getClientServices((MindBodyGetClientServicesConfig)config);
				break;
			case GET_CLIENT_VISITS:
				resp = getClientVisits((MindBodyGetClientVisitsConfig)config);
				break;
			case GET_CUSTOM_CLIENT_FIELDS:
				resp = getCustomClientFields((MindBodyGetCustomClientFieldsConfig)config);
				break;
			case GET_REQUIRED_CLIENT_FIELDS:
				resp = getRequiredClientFields((MindBodyGetRequiredClientFieldsConfig)config);
				break;
			case SEND_USER_NEW_PASSWORD:
				resp = sendUserNewPassword((MindBodySendUserNewPasswordConfig)config);
				break;
			case UPDATE_CLIENT_SERVICES:
				resp = updateClientServices((MindBodyUpdateClientServicesConfig)config);
				break;
			case UPLOAD_CLIENT_DOCUMENT:
				resp = uploadClientDocument((MindBodyUploadClientDocumentConfig)config);
				break;
			case VALIDATE_LOGIN:
				resp = validateLogin((MindBodyValidateLoginConfig)config);
				break;
			default:
				log.warn("Endpoint Not Supported.");
				resp = buildErrorResponse(HttpStatus.ORDINAL_501_Not_Implemented, "Endpoint Not Supported");
				break;
		}
		return resp;
	}

	/**
	 * @param config
	 * @return
	 */
	private MindBodyResponseVO sendUserNewPassword(MindBodySendUserNewPasswordConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		ClientSendUserNewPasswordRequest req = ClientSendUserNewPasswordRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureSendUserNewPasswordRequest(req, config);

		SendUserNewPasswordDocument doc = SendUserNewPasswordDocument.Factory.newInstance();
		doc.addNewSendUserNewPassword().setRequest(req);

		Client_x0020_ServiceStub client = getConfiguredStub();
		SendUserNewPasswordResponseDocument res = client.sendUserNewPassword(doc);
		resp.populateResponseFields(res.getSendUserNewPasswordResponse().getSendUserNewPasswordResult());

		return resp;
	}

	/**
	 * @param req
	 * @param config
	 */
	private void configureSendUserNewPasswordRequest(ClientSendUserNewPasswordRequest req, MindBodySendUserNewPasswordConfig config) {
		req.setUserEmail(config.getUserEmail());
		req.setUserFirstName(config.getUserFirstName());
		req.setUserLastName(config.getUserLastName());
	}

	/**
	 * @param config
	 * @return
	 */
	private MindBodyResponseVO updateClientServices(MindBodyUpdateClientServicesConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		UpdateClientServicesRequest req = UpdateClientServicesRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureUpdateClientServicesRequest(req, config);

		UpdateClientServicesDocument doc = UpdateClientServicesDocument.Factory.newInstance();
		doc.addNewUpdateClientServices().setRequest(req);

		Client_x0020_ServiceStub client = getConfiguredStub();
		UpdateClientServicesResponseDocument res = client.updateClientServices(doc);
		UpdateClientServicesResult r = res.getUpdateClientServicesResponse().getUpdateClientServicesResult();
		resp.populateResponseFields(r);
		if(resp.isValid()) {
			resp.addResults((Object [])r.getClientServices().getClientServiceArray());
		}

		return resp;
	}

	/**
	 * @param req
	 * @param config
	 */
	private void configureUpdateClientServicesRequest(UpdateClientServicesRequest req, MindBodyUpdateClientServicesConfig config) {
		req.setClientServices(MindBodyUtil.buildArrayOfClientService(config.getClientServices()));

		if(config.isTest()) {
			req.setTest(config.isTest());
		}
	}

	/**
	 * @param config
	 * @return
	 */
	private MindBodyResponseVO validateLogin(MindBodyValidateLoginConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		ValidateLoginRequest req = ValidateLoginRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureValidateLoginRequest(req, config);

		ValidateLoginDocument doc = ValidateLoginDocument.Factory.newInstance();
		doc.addNewValidateLogin().setRequest(req);

		Client_x0020_ServiceStub client = getConfiguredStub();
		ValidateLoginResponseDocument res = client.validateLogin(doc);
		ValidateLoginResult r = res.getValidateLoginResponse().getValidateLoginResult();
		
		resp.populateResponseFields(r);
		if(resp.isValid()) {
			resp.addResults(MindBodyUtil.convertClientData(r.getClient()));
		}

		return resp;
	}

	/**
	 * @param req
	 * @param config
	 */
	private void configureValidateLoginRequest(ValidateLoginRequest req, MindBodyValidateLoginConfig config) {
		req.setUsername(config.getUserName());
		req.setPassword(config.getPassword());
	}

	/**
	 * @param config
	 * @return
	 */
	private MindBodyResponseVO getRequiredClientFields(MindBodyGetRequiredClientFieldsConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		GetRequiredClientFieldsRequest req = GetRequiredClientFieldsRequest.Factory.newInstance();
		prepareRequest(req, config);

		GetRequiredClientFieldsDocument doc = GetRequiredClientFieldsDocument.Factory.newInstance();
		doc.addNewGetRequiredClientFields().setRequest(req);

		Client_x0020_ServiceStub client = getConfiguredStub();
		GetRequiredClientFieldsResponseDocument res = client.getRequiredClientFields(doc);
		GetRequiredClientFieldsResult r = res.getGetRequiredClientFieldsResponse().getGetRequiredClientFieldsResult();
		resp.populateResponseFields(r);
		if(resp.isValid()) {
			resp.addResults((Object [])r.getRequiredClientFields().getStringArray());
		}

		return resp;
	}

	/**
	 * @param config
	 * @return
	 */
	private MindBodyResponseVO getClientSchedule(MindBodyGetClientScheduleConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		GetClientScheduleRequest req = GetClientScheduleRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureGetClientScheduleRequest(req, config);

		GetClientScheduleDocument doc = GetClientScheduleDocument.Factory.newInstance();
		doc.addNewGetClientSchedule().setRequest(req);

		Client_x0020_ServiceStub client = getConfiguredStub();
		GetClientScheduleResponseDocument res = client.getClientSchedule(doc);
		GetClientScheduleResult r = res.getGetClientScheduleResponse().getGetClientScheduleResult();

		resp.populateResponseFields(r);
		if(resp.isValid()) {
			resp.addResults((Object [])r.getVisits().getVisitArray());
		}
		return resp;
	}

	/**
	 * @param req
	 * @param config
	 */
	private void configureGetClientScheduleRequest(GetClientScheduleRequest req, MindBodyGetClientScheduleConfig config) {
		req.setClientID(config.getClientId());

		if(config.getStartDate() != null) {
			req.setStartDate(Convert.toCalendar(config.getStartDate()));
		}

		if(config.getEndDate() != null) {
			req.setEndDate(Convert.toCalendar(config.getEndDate()));
		}
	}

	/**
	 * @param config
	 * @return
	 */
	private MindBodyResponseVO getClientPurchases(MindBodyGetClientPurchasesConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		GetClientPurchasesRequest req = GetClientPurchasesRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureGetClientPurchasesRequest(req, config);

		GetClientPurchasesDocument doc = GetClientPurchasesDocument.Factory.newInstance();
		doc.addNewGetClientPurchases().setRequest(req);

		Client_x0020_ServiceStub client = getConfiguredStub();
		GetClientPurchasesResponseDocument res = client.getClientPurchases(doc);
		GetClientPurchasesResult r = res.getGetClientPurchasesResponse().getGetClientPurchasesResult();
		resp.populateResponseFields(r);
		if(resp.isValid()) {
			resp.addResults((Object [])r.getPurchases().getSaleItemArray());
		}

		return resp;
	}

	/**
	 * @param req
	 * @param config
	 */
	private void configureGetClientPurchasesRequest(GetClientPurchasesRequest req, MindBodyGetClientPurchasesConfig config) {
		req.setClientID(config.getClientId());

		if(config.getStartDate() != null) {
			req.setStartDate(Convert.toCalendar(config.getStartDate()));
		}

		if(config.getEndDate() != null) {
			req.setEndDate(Convert.toCalendar(config.getEndDate()));
		}

		if(config.getSaleId() != null) {
			req.setSaleID(config.getSaleId());
		}
	}

	/**
	 * @param config
	 * @return
	 */
	private MindBodyResponseVO getClientVisits(MindBodyGetClientVisitsConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		GetClientVisitsRequest req = GetClientVisitsRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureGetClientVisitsRequest(req, config);

		GetClientVisitsDocument doc = GetClientVisitsDocument.Factory.newInstance();
		doc.addNewGetClientVisits().setRequest(req);

		Client_x0020_ServiceStub client = getConfiguredStub();
		GetClientVisitsResponseDocument res = client.getClientVisits(doc);
		GetClientVisitsResult r = res.getGetClientVisitsResponse().getGetClientVisitsResult();
		resp.populateResponseFields(r);
		if(resp.isValid()) {
			resp.addResults((Object [])r.getVisits().getVisitArray());
		}

		return resp;
	}

	/**
	 * @param req
	 * @param config
	 */
	private void configureGetClientVisitsRequest(GetClientVisitsRequest req, MindBodyGetClientVisitsConfig config) {
		req.setClientID(config.getClientId());

		if(config.getStartDate() != null) {
			req.setStartDate(Convert.toCalendar(config.getStartDate()));
		}

		if(config.getEndDate() != null) {
			req.setEndDate(Convert.toCalendar(config.getEndDate()));
		}

		if(config.isUnpaidsOnly()) {
			req.setUnpaidsOnly(config.isUnpaidsOnly());
		}
	}

	/**
	 * @param config
	 * @return
	 */
	private MindBodyResponseVO getClientServices(MindBodyGetClientServicesConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		GetClientServicesRequest req = GetClientServicesRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureGetClientServicesRequest(req, config);

		GetClientServicesDocument doc = GetClientServicesDocument.Factory.newInstance();
		doc.addNewGetClientServices().setRequest(req);

		Client_x0020_ServiceStub client = getConfiguredStub();
		GetClientServicesResponseDocument res = client.getClientServices(doc);
		GetClientServicesResult r = res.getGetClientServicesResponse().getGetClientServicesResult();
		resp.populateResponseFields(r);
		if(resp.isValid()) {
			resp.addResults((Object [])r.getClientServices().getClientServiceArray());
		}

		return resp;
	}

	/**
	 * @param req
	 * @param config
	 */
	private void configureGetClientServicesRequest(GetClientServicesRequest req, MindBodyGetClientServicesConfig config) {
		req.setClientID(config.getClientId());

		if(config.getClassId() != null) {
			req.setClassID(config.getClassId());
		}

		if(!config.getProgramIds().isEmpty()) {
			req.setProgramIDs(MindBodyUtil.buildArrayOfInt(config.getProgramIds()));
		}

		if(!config.getSessionTypeIds().isEmpty()) {
			req.setSessionTypeIDs(MindBodyUtil.buildArrayOfInt(config.getSessionTypeIds()));
		}

		if(!config.getLocationIds().isEmpty()) {
			req.setLocationIDs(MindBodyUtil.buildArrayOfInt(config.getLocationIds()));
		}

		if(config.getVisitCount() != null) {
			req.setVisitCount(config.getVisitCount());
		}

		if(config.getStartDate() != null) {
			req.setStartDate(Convert.toCalendar(config.getStartDate()));
		}

		if(config.getEndDate() != null) {
			req.setEndDate(Convert.toCalendar(config.getEndDate()));
		}

		if(config.isShowActiveOnly()) {
			req.setShowActiveOnly(config.isShowActiveOnly());
		}

	}

	/**
	 * @param config
	 * @return
	 */
	private MindBodyResponseVO getClientAccountBalances(MindBodyGetClientAccountBalancesConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		GetClientAccountBalancesRequest req = GetClientAccountBalancesRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureGetClientAccountBalancesRequest(req, config);

		GetClientAccountBalancesDocument doc = GetClientAccountBalancesDocument.Factory.newInstance();
		doc.addNewGetClientAccountBalances().setRequest(req);

		Client_x0020_ServiceStub client = getConfiguredStub();
		GetClientAccountBalancesResponseDocument res = client.getClientAccountBalances(doc);
		GetClientAccountBalancesResult r = res.getGetClientAccountBalancesResponse().getGetClientAccountBalancesResult();
		resp.populateResponseFields(r);
		if(resp.isValid()) {
			resp.addResults((Object [])r.getClients().getClientArray());
		}

		return resp;
	}

	/**
	 * @param req
	 * @param config
	 */
	private void configureGetClientAccountBalancesRequest(GetClientAccountBalancesRequest req, MindBodyGetClientAccountBalancesConfig config) {
		req.setClientIDs(MindBodyUtil.buildArrayOfString(config.getClientIds()));

		if(config.getBalanceDate() != null) {
			req.setBalanceDate(Convert.toCalendar(config.getBalanceDate()));
		}

		if(config.getClassId() != null) {
			req.setClassID(config.getClassId());
		}
	}

	/**
	 * @param config
	 * @return
	 */
	private MindBodyResponseVO uploadClientDocument(MindBodyUploadClientDocumentConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		UploadClientDocumentRequest req = UploadClientDocumentRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureUploadClientDocumentRequest(req, config);

		UploadClientDocumentDocument1 doc = UploadClientDocumentDocument1.Factory.newInstance();
		doc.addNewUploadClientDocument().setRequest(req);

		Client_x0020_ServiceStub client = getConfiguredStub();
		UploadClientDocumentResponseDocument res = client.uploadClientDocument(doc);
		UploadClientDocumentResult r = res.getUploadClientDocumentResponse().getUploadClientDocumentResult();
		resp.populateResponseFields(r);

		return resp;
	}

	/**
	 * @param req
	 * @param config
	 */
	private void configureUploadClientDocumentRequest(UploadClientDocumentRequest req, MindBodyUploadClientDocumentConfig config) {
		req.setClientID(config.getClientIds().get(0));

		req.setFileName(config.getFileName());

		req.setBytes(config.getBytes());
	}

	/**
	 * @param config
	 * @return
	 */
	private MindBodyResponseVO getCustomClientFields(MindBodyGetCustomClientFieldsConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		GetCustomClientFieldsRequest req = GetCustomClientFieldsRequest.Factory.newInstance();
		prepareRequest(req, config);

		GetCustomClientFieldsDocument doc = GetCustomClientFieldsDocument.Factory.newInstance();
		doc.addNewGetCustomClientFields().setRequest(req);

		Client_x0020_ServiceStub client = getConfiguredStub();
		GetCustomClientFieldsResponseDocument res = client.getCustomClientFields(doc);
		GetCustomClientFieldsResult r = res.getGetCustomClientFieldsResponse().getGetCustomClientFieldsResult();
		resp.populateResponseFields(r);
		if(resp.isValid()) {
			resp.addResults((Object [])r.getCustomClientFields().getCustomClientFieldArray());
		}
		return resp;
	}

	/**
	 * @param config
	 * @return
	 */
	private MindBodyResponseVO addOrUpdateClients(MindBodyAddOrUpdateClientsConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		AddOrUpdateClientsRequest req = AddOrUpdateClientsRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureAddOrUpdateClientsRequest(req, config);

		AddOrUpdateClientsDocument doc = AddOrUpdateClientsDocument.Factory.newInstance();
		doc.addNewAddOrUpdateClients().setRequest(req);

		Client_x0020_ServiceStub client = getConfiguredStub();
		AddOrUpdateClientsResponseDocument res = client.addOrUpdateClients(doc);
		AddOrUpdateClientsResult r = res.getAddOrUpdateClientsResponse().getAddOrUpdateClientsResult();
		resp.populateResponseFields(r);
		if(resp.isValid()) {
			resp.addResults((Object [])r.getClients().getClientArray());
		}
		return resp;
	}

	/**
	 * @param req
	 * @param config
	 */
	private void configureAddOrUpdateClientsRequest(AddOrUpdateClientsRequest req, MindBodyAddOrUpdateClientsConfig config) {
		req.setClients(MindBodyUtil.buildArrayOfClients(config.getClients()));

		if(config.isSendEmail()) {
			req.setSendEmail(config.isSendEmail());
		}
	}

	/**
	 * @param config
	 * @return
	 */
	private MindBodyResponseVO getClients(MindBodyGetClientsConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		GetClientsRequest req = GetClientsRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureGetClientsRequest(req, config);

		GetClientsDocument doc = GetClientsDocument.Factory.newInstance();
		doc.addNewGetClients().setRequest(req);

		Client_x0020_ServiceStub client = getConfiguredStub();
		GetClientsResponseDocument res = client.getClients(doc);
		GetClientsResult r = res.getGetClientsResponse().getGetClientsResult();
		resp.populateResponseFields(r);
		if(resp.isValid()) {
			resp.addResults((Object [])r.getClients().getClientArray());
		}
		return resp;
	}

	/**
	 * @param req
	 * @param config
	 */
	private void configureGetClientsRequest(GetClientsRequest req, MindBodyGetClientsConfig config) {
		req.setSearchText(config.getSearchText());

		if(!config.getClientIds().isEmpty()) {
			req.setClientIDs(MindBodyUtil.buildArrayOfString(config.getClientIds()));
		}

		if(config.isProspect()) {
			req.setIsProspect(config.isProspect());
		}

		if(config.getLastModifiedDate() != null) {
			req.setLastModifiedDate(Convert.toCalendar(config.getLastModifiedDate()));
		}
	}

	/**
	 * @param config
	 * @return
	 * @throws RemoteException 
	 */
	private MindBodyResponseVO addArrival(MindBodyAddArrivalConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		AddArrivalRequest req = AddArrivalRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureAddArrivalRequest(req, config);

		AddArrivalDocument doc = AddArrivalDocument.Factory.newInstance();
		doc.addNewAddArrival().setRequest(req);

		Client_x0020_ServiceStub client = getConfiguredStub();
		AddArrivalResponseDocument res = client.addArrival(doc);
		AddArrivalResult r = res.getAddArrivalResponse().getAddArrivalResult();
		resp.populateResponseFields(r);
		if(resp.isValid()) {
			resp.addResults(r.getArrivalAdded());
		}

		return resp;
	}

	/**
	 * @param req
	 * @param config
	 */
	private void configureAddArrivalRequest(AddArrivalRequest req, MindBodyAddArrivalConfig config) {
		req.setClientID(config.getClientId());
		req.setLocationID(config.getLocationId());
	}
}