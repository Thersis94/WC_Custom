package com.mindbody;

import java.rmi.RemoteException;
import java.util.List;

import org.apache.axis2.AxisFault;

import com.mindbody.vo.staff.MindBodyStaffConfig;
import com.mindbodyonline.clients.api._0_5_1.Staff_x0020_ServiceStub;

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
			log.info("Endpoint not supported.");
		} else {
			throw new IllegalArgumentException("Config Not Valid.");
		}
		return resp;	}

}
