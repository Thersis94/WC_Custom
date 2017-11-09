package com.mindbody;

import java.rmi.RemoteException;
import java.util.List;

import org.apache.axis2.AxisFault;
import com.mindbody.vo.clients.MindBodyClientConfig;
import com.mindbodyonline.clients.api._0_5_1.Client_x0020_ServiceStub;

/****************************************************************************
 * <b>Title:</b> MindbodyClientApi.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> TODO
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 3, 2017
 ****************************************************************************/
public class MindbodyClientApi extends AbstractMindBodyApi<Client_x0020_ServiceStub, MindBodyClientConfig> {

	public enum ClientDocumentType {}

	/**
	 * 
	 */
	public MindbodyClientApi() {
		super();
	}

	/* (non-Javadoc)
	 * @see com.mindbody.MindBodyApiIntfc#getStub()
	 * TODO - COMPLETE METHOD BODY
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
