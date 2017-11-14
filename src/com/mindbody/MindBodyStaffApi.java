package com.mindbody;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.axis2.AxisFault;

import com.mindbody.vo.staff.MindBodyAddOrUpdateStaffConfig;
import com.mindbody.vo.staff.MindBodyGetStaffConfig;
import com.mindbody.vo.staff.MindBodyGetStaffImgUrl;
import com.mindbody.vo.staff.MindBodyGetStaffPermissionsConfig;
import com.mindbody.vo.staff.MindBodyStaffConfig;
import com.mindbody.vo.staff.MindBodyValidateStaffLogin;
import com.mindbodyonline.clients.api._0_5_1.AddOrUpdateStaffDocument;
import com.mindbodyonline.clients.api._0_5_1.AddOrUpdateStaffRequest;
import com.mindbodyonline.clients.api._0_5_1.AddOrUpdateStaffResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.ArrayOfPermission;
import com.mindbodyonline.clients.api._0_5_1.ArrayOfStaff;
import com.mindbodyonline.clients.api._0_5_1.GetStaffDocument;
import com.mindbodyonline.clients.api._0_5_1.GetStaffImgURLDocument;
import com.mindbodyonline.clients.api._0_5_1.GetStaffImgURLRequest;
import com.mindbodyonline.clients.api._0_5_1.GetStaffImgURLResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.GetStaffPermissionsDocument;
import com.mindbodyonline.clients.api._0_5_1.GetStaffPermissionsRequest;
import com.mindbodyonline.clients.api._0_5_1.GetStaffPermissionsResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.GetStaffRequest;
import com.mindbodyonline.clients.api._0_5_1.GetStaffResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.Staff;
import com.mindbodyonline.clients.api._0_5_1.Staff_x0020_ServiceStub;
import com.mindbodyonline.clients.api._0_5_1.ValidateLoginRequest;
import com.mindbodyonline.clients.api._0_5_1.ValidateStaffLoginDocument;
import com.mindbodyonline.clients.api._0_5_1.ValidateStaffLoginResponseDocument;


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
	 * @see com.mindbody.MindBodyApiIntfc#getDocument(com.mindbody.MindBodyCallVO)
	 */
	@Override
	public List<Object> getDocument(MindBodyStaffConfig config) throws RemoteException {
		List<Object> resp = null;

		if(config.isValid()) {
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
					break;
			}
		} else {
			throw new IllegalArgumentException("Config Not Valid.");
		}
		return resp;
	}

	/**
	 * 
	 * @param config
	 * @return
	 * @throws RemoteException
	 */
	private List<Object> getStaff(MindBodyGetStaffConfig config) throws RemoteException {
		List<Object> staff = new ArrayList<>();
		GetStaffRequest req = GetStaffRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureGetStaffRequest(req, config);

		GetStaffDocument doc = GetStaffDocument.Factory.newInstance();
		doc.getGetStaff().setRequest(req);

		Staff_x0020_ServiceStub client = getConfiguredStub();
		GetStaffResponseDocument res = client.getStaff(doc);
		ArrayOfStaff permArr = res.getGetStaffResponse().getGetStaffResult().getStaffMembers();

		staff.addAll(Arrays.asList(permArr.getStaffArray()));

		return staff;
	}

	/**
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
	 * 
	 * @param config
	 * @return
	 * @throws RemoteException
	 */
	private List<Object> getStaffPermissions(MindBodyGetStaffPermissionsConfig config) throws RemoteException {
		List<Object> permissions = new ArrayList<>();
		GetStaffPermissionsRequest req = GetStaffPermissionsRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureGetStaffPermissionsRequest(req, config);

		GetStaffPermissionsDocument doc = GetStaffPermissionsDocument.Factory.newInstance();
		doc.getGetStaffPermissions().setRequest(req);

		Staff_x0020_ServiceStub client = getConfiguredStub();
		GetStaffPermissionsResponseDocument res = client.getStaffPermissions(doc);
		ArrayOfPermission permArr = res.getGetStaffPermissionsResponse().getGetStaffPermissionsResult().getPermissions();

		permissions.addAll(Arrays.asList(permArr.getPermissionArray()));

		return permissions;
	}

	/**
	 * @param req
	 * @param config
	 */
	private void configureGetStaffPermissionsRequest(GetStaffPermissionsRequest req, MindBodyGetStaffPermissionsConfig config) {
		req.setStaffID(config.getStaffId());
	}

	/**
	 * 
	 * @param config
	 * @return
	 * @throws RemoteException
	 */
	private List<Object> addOrUpdateStaff(MindBodyAddOrUpdateStaffConfig config) throws RemoteException {
		List<Object> staff = new ArrayList<>();
		AddOrUpdateStaffRequest req = AddOrUpdateStaffRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureAddOrUpdateStaffRequest(req, config);

		AddOrUpdateStaffDocument doc = AddOrUpdateStaffDocument.Factory.newInstance();
		doc.getAddOrUpdateStaff().setRequest(req);

		Staff_x0020_ServiceStub client = getConfiguredStub();
		AddOrUpdateStaffResponseDocument res = client.addOrUpdateStaff(doc);
		ArrayOfStaff staffArr = res.getAddOrUpdateStaffResponse().getAddOrUpdateStaffResult().getStaff();

		staff.addAll(Arrays.asList(staffArr.getStaffArray()));

		return staff;
	}

	/**
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
	 * 
	 * @param config
	 * @return
	 * @throws RemoteException
	 */
	private List<Object> getStaffImgUrl(MindBodyGetStaffImgUrl config) throws RemoteException {
		List<Object> images = new ArrayList<>();
		GetStaffImgURLRequest req = GetStaffImgURLRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureGetStaffImageUrlRequest(req, config);

		GetStaffImgURLDocument doc = GetStaffImgURLDocument.Factory.newInstance();
		doc.addNewGetStaffImgURL().setRequest(req);

		Staff_x0020_ServiceStub client = getConfiguredStub();
		GetStaffImgURLResponseDocument res = client.getStaffImgURL(doc);
		String s = res.getGetStaffImgURLResponse().getGetStaffImgURLResult().getImageURL();

		images.add(s);
		return images;
	}

	/**
	 * @param req
	 * @param config
	 */
	private void configureGetStaffImageUrlRequest(GetStaffImgURLRequest req, MindBodyGetStaffImgUrl config) {
		req.setStaffID(config.getStaffId());
	}

	/**
	 * 
	 * @param config
	 * @return
	 * @throws RemoteException
	 */
	private List<Object> validateStaffLogin(MindBodyValidateStaffLogin config) throws RemoteException {
		List<Object> staff = new ArrayList<>();
		ValidateLoginRequest req = ValidateLoginRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureValidateStaffLoginRequest(req, config);

		ValidateStaffLoginDocument doc = ValidateStaffLoginDocument.Factory.newInstance();
		doc.addNewValidateStaffLogin().setRequest(req);

		Staff_x0020_ServiceStub client = getConfiguredStub();
		ValidateStaffLoginResponseDocument res = client.validateStaffLogin(doc);
		Staff s = res.getValidateStaffLoginResponse().getValidateStaffLoginResult().getStaff();

		staff.add(s);
		return staff;
	}

	/**
	 * @param req
	 * @param config
	 */
	private void configureValidateStaffLoginRequest(ValidateLoginRequest req, MindBodyValidateStaffLogin config) {
		req.setUsername(config.getUserName());
		req.setPassword(config.getPassword());
	}
}
