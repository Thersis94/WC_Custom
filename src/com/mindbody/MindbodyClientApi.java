package com.mindbody;

import java.rmi.RemoteException;
import java.util.List;

import org.apache.axis2.AxisFault;
import com.mindbody.vo.clients.MindBodyClientConfig;
import com.mindbodyonline.clients.api._0_5_1.Client_x0020_ServiceStub;

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
public class MindbodyClientApi extends AbstractMindBodyApi<Client_x0020_ServiceStub, MindBodyClientConfig> {

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
	public MindbodyClientApi() {
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
	 * @see com.mindbody.MindBodyApiIntfc#getDocument(com.mindbody.vo.MindBodyCallVO)
	 * TODO - COMPLETE METHOD BODY
	 */
	@Override
	public List<Object> getDocument(MindBodyClientConfig call) throws RemoteException {
		return null;
	}
}
