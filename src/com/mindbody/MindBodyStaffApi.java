package com.mindbody;

import java.rmi.RemoteException;
import java.util.List;

import org.apache.axis2.AxisFault;

import com.mindbody.vo.staff.MindBodyStaffVO;
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
public class MindBodyStaffApi extends AbstractMindBodyApi<Staff_x0020_ServiceStub, MindBodyStaffVO> {

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
	 * TODO - COMPLETE METHOD BODY
	 */
	@Override
	public List<Object> getDocument(MindBodyStaffVO call) throws RemoteException {
		return null;
	}

}
