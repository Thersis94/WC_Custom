package com.mindbody;

import java.rmi.RemoteException;

import org.apache.axis2.AxisFault;

import com.mindbody.vo.MindBodyResponseVO;
import com.mindbody.vo.staff.MindBodyAddOrUpdateStaffConfig;
import com.mindbody.vo.staff.MindBodyGetStaffConfig;
import com.mindbody.vo.staff.MindBodyGetStaffImgUrl;
import com.mindbody.vo.staff.MindBodyGetStaffPermissionsConfig;
import com.mindbody.vo.staff.MindBodyStaffConfig;
import com.mindbody.vo.staff.MindBodyValidateStaffLogin;

//Mind Body Staff Api Jar
import com.mindbodyonline.clients.api._0_5_1.AddOrUpdateStaffDocument;
import com.mindbodyonline.clients.api._0_5_1.AddOrUpdateStaffRequest;
import com.mindbodyonline.clients.api._0_5_1.AddOrUpdateStaffResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.AddOrUpdateStaffResult;
import com.mindbodyonline.clients.api._0_5_1.GetStaffDocument;
import com.mindbodyonline.clients.api._0_5_1.GetStaffImgURLDocument;
import com.mindbodyonline.clients.api._0_5_1.GetStaffImgURLRequest;
import com.mindbodyonline.clients.api._0_5_1.GetStaffImgURLResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.GetStaffImgURLResult;
import com.mindbodyonline.clients.api._0_5_1.GetStaffPermissionsDocument;
import com.mindbodyonline.clients.api._0_5_1.GetStaffPermissionsRequest;
import com.mindbodyonline.clients.api._0_5_1.GetStaffPermissionsResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.GetStaffPermissionsResult;
import com.mindbodyonline.clients.api._0_5_1.GetStaffRequest;
import com.mindbodyonline.clients.api._0_5_1.GetStaffResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.GetStaffResult;
import com.mindbodyonline.clients.api._0_5_1.Staff_x0020_ServiceStub;
import com.mindbodyonline.clients.api._0_5_1.ValidateLoginRequest;
import com.mindbodyonline.clients.api._0_5_1.ValidateLoginResult;
import com.mindbodyonline.clients.api._0_5_1.ValidateStaffLoginDocument;
import com.mindbodyonline.clients.api._0_5_1.ValidateStaffLoginResponseDocument;

//Base Libs
import com.siliconmtn.common.http.HttpStatus;

/****************************************************************************
 * <b>Title:</b> MindBodyStaffApi.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manage Mind Body Class Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 3, 2017
 ****************************************************************************/
public class MindBodyStaffApi extends AbstractMindBodyApi<Staff_x0020_ServiceStub, MindBodyStaffConfig> {

	public enum StaffDocumentType {
		GET_STAFF,
		GET_STAFF_PERMISSIONS,
		ADD_OR_UPDATE_STAFF,
		GET_STAFF_IMG_URL,
		VALIDATE_STAFF_LOGIN
	}

	/**
	 * 
	 */
	public MindBodyStaffApi() {
		super();
	}

	/* (non-Javadoc)
	 * @see com.mindbody.MindBodyApiIntfc#getStub()
	 */
	@Override
	public Staff_x0020_ServiceStub getStub() throws AxisFault {
		return new Staff_x0020_ServiceStub();
	}

	/* (non-Javadoc)
	 * @see com.mindbody.AbstractMindBodyApi#processRequest(com.mindbody.vo.MindBodyConfig)
	 */
	@Override
	protected MindBodyResponseVO processRequest(MindBodyStaffConfig config) throws RemoteException {
		MindBodyResponseVO resp;
		switch(config.getType()) {
			case ADD_OR_UPDATE_STAFF:
				resp = addOrUpdateStaff((MindBodyAddOrUpdateStaffConfig) config);
				break;
			case GET_STAFF:
				resp = getStaff((MindBodyGetStaffConfig) config);
				break;
			case GET_STAFF_IMG_URL:
				resp = getStaffImgUrl((MindBodyGetStaffImgUrl) config);
				break;
			case GET_STAFF_PERMISSIONS:
				resp = getStaffPermissions((MindBodyGetStaffPermissionsConfig) config);
				break;
			case VALIDATE_STAFF_LOGIN:
				resp = validateStaffLogin((MindBodyValidateStaffLogin) config);
				break;
			default:
				log.error("Endpoint Not Supported.");
				resp = buildErrorResponse(HttpStatus.CD_501_NOT_IMPLEMENTED, "Endpoint Not Supported");
				break;
		}
		return resp;
	}

	/**
	 * Manage Building, Configuring and Executing the GetStaff Endpoint
	 * @param config
	 * @return
	 * @throws RemoteException
	 */
	private MindBodyResponseVO getStaff(MindBodyGetStaffConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		GetStaffRequest req = GetStaffRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureGetStaffRequest(req, config);

		GetStaffDocument doc = GetStaffDocument.Factory.newInstance();
		doc.getGetStaff().setRequest(req);

		Staff_x0020_ServiceStub client = getConfiguredStub();
		GetStaffResponseDocument res = client.getStaff(doc);
		GetStaffResult r = res.getGetStaffResponse().getGetStaffResult();
		resp.populateResponseFields(r);
		if(resp.isValid()) {
			resp.addResults((Object [])r.getStaffMembers().getStaffArray());
		}

		return resp;
	}

	/**
	 * Configures the GetStaff Endpoint
	 * @param req
	 * @param config
	 */
	private void configureGetStaffRequest(GetStaffRequest req, MindBodyGetStaffConfig config) {
		if(!config.getStaffIds().isEmpty()) {
			req.setStaffIDs(MindBodyUtil.buildArrayOfLong(config.getStaffIds()));
		}

		req.setStaffCredentials(config.getStaffCredentials());

		if(config.getSessionTypeId() != null) {
			req.setSessionTypeID(config.getSessionTypeId());
		}

		if(config.getLocationId() != null) {
			req.setLocationID(config.getLocationId());
		}

		req.setFilters(MindBodyUtil.buildArrayOfStaffFilter(config.getFilters()));
	}

	

	/**
	 * Manage Building, Configuring and Executing the GetStaffPermissions Endpoint
	 * @param config
	 * @return
	 * @throws RemoteException
	 */
	private MindBodyResponseVO getStaffPermissions(MindBodyGetStaffPermissionsConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		GetStaffPermissionsRequest req = GetStaffPermissionsRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureGetStaffPermissionsRequest(req, config);

		GetStaffPermissionsDocument doc = GetStaffPermissionsDocument.Factory.newInstance();
		doc.getGetStaffPermissions().setRequest(req);

		Staff_x0020_ServiceStub client = getConfiguredStub();
		GetStaffPermissionsResponseDocument res = client.getStaffPermissions(doc);
		GetStaffPermissionsResult r = res.getGetStaffPermissionsResponse().getGetStaffPermissionsResult();
		resp.populateResponseFields(r);
		if(resp.isValid()) {
			resp.addResults((Object [])r.getPermissions().getPermissionArray());
		}

		return resp;
	}

	/**
	 * Manage Configuring the GetStaffPermissions Endpoint
	 * @param req
	 * @param config
	 */
	private void configureGetStaffPermissionsRequest(GetStaffPermissionsRequest req, MindBodyGetStaffPermissionsConfig config) {
		req.setStaffID(config.getStaffId());
	}

	/**
	 * Manage Building, Configuring and Executing the AddOrUpdateStaff Endpoint
	 * @param config
	 * @return
	 * @throws RemoteException
	 */
	private MindBodyResponseVO addOrUpdateStaff(MindBodyAddOrUpdateStaffConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		AddOrUpdateStaffRequest req = AddOrUpdateStaffRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureAddOrUpdateStaffRequest(req, config);

		AddOrUpdateStaffDocument doc = AddOrUpdateStaffDocument.Factory.newInstance();
		doc.getAddOrUpdateStaff().setRequest(req);

		Staff_x0020_ServiceStub client = getConfiguredStub();
		AddOrUpdateStaffResponseDocument res = client.addOrUpdateStaff(doc);
		AddOrUpdateStaffResult r = res.getAddOrUpdateStaffResponse().getAddOrUpdateStaffResult();
		resp.populateResponseFields(r);
		if(resp.isValid()) {
			resp.addResults((Object [])r.getStaff().getStaffArray());
		}

		return resp;
	}

	/**
	 * Manage Configuring the AddOrUpdateStaff Endpoint
	 * @param req
	 * @param config
	 */
	private void configureAddOrUpdateStaffRequest(AddOrUpdateStaffRequest req, MindBodyAddOrUpdateStaffConfig config) {
		if(config.isTest()) {
			req.setTest(config.isTest());
		}

		if(!config.getStaff().isEmpty()) {
			req.setStaff(MindBodyUtil.buildArrayOfStaff(config.getStaff()));
		}

		req.setUpdateAction(config.getUpdateAction());
	}

	/**
	 * Manage Building, Configuring and Executing the GetStaffImgUrl Endpoint
	 * @param config
	 * @return
	 * @throws RemoteException
	 */
	private MindBodyResponseVO getStaffImgUrl(MindBodyGetStaffImgUrl config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		GetStaffImgURLRequest req = GetStaffImgURLRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureGetStaffImageUrlRequest(req, config);

		GetStaffImgURLDocument doc = GetStaffImgURLDocument.Factory.newInstance();
		doc.addNewGetStaffImgURL().setRequest(req);

		Staff_x0020_ServiceStub client = getConfiguredStub();
		GetStaffImgURLResponseDocument res = client.getStaffImgURL(doc);
		GetStaffImgURLResult r = res.getGetStaffImgURLResponse().getGetStaffImgURLResult();
		resp.populateResponseFields(r);
		if(resp.isValid()) {
			resp.addResults(r.getImageURL());
		}

		return resp;
	}

	/**
	 * Manage Configuring the GetStaffImageUrl Endpoint
	 * @param req
	 * @param config
	 */
	private void configureGetStaffImageUrlRequest(GetStaffImgURLRequest req, MindBodyGetStaffImgUrl config) {
		req.setStaffID(config.getStaffId());
	}

	/**
	 * Manage Building, Configuring and Executing the ValidateStaffLogin Endpoint
	 * @param config
	 * @return
	 * @throws RemoteException
	 */
	private MindBodyResponseVO validateStaffLogin(MindBodyValidateStaffLogin config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		ValidateLoginRequest req = ValidateLoginRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureValidateStaffLoginRequest(req, config);

		ValidateStaffLoginDocument doc = ValidateStaffLoginDocument.Factory.newInstance();
		doc.addNewValidateStaffLogin().setRequest(req);

		Staff_x0020_ServiceStub client = getConfiguredStub();
		ValidateStaffLoginResponseDocument res = client.validateStaffLogin(doc);
		ValidateLoginResult r = res.getValidateStaffLoginResponse().getValidateStaffLoginResult();
		resp.populateResponseFields(r);
		if(resp.isValid()) {
			resp.addResults(r.getStaff());
		}

		return resp;
	}

	/**
	 * Manage Configuring the ValidateStaffLogin Endpoint
	 * @param req
	 * @param config
	 */
	private void configureValidateStaffLoginRequest(ValidateLoginRequest req, MindBodyValidateStaffLogin config) {
		req.setUsername(config.getUserName());
		req.setPassword(config.getPassword());
	}
}
