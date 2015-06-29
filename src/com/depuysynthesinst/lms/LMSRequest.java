package com.depuysynthesinst.lms;

import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;

/****************************************************************************
 * <b>Title</b>: LMSRequest.java<p/>
 * <b>Description: formats the SOAP call using values from WC.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jun 27, 2015
 ****************************************************************************/
public class LMSRequest {

	//all of the calls supported by the LMS, see DSI documentation/swimlanes (naming correlations)
	public enum RequestType {
		usrLogin,
		usrHolding, usrMigrate,
		usrCreate, usrUpdate,
		usrActive, usrPoints,
		crsList, crsLaunch;
	}
	
	public LMSRequest() {
	}
	
	/**
	 * similar to a factory method pattern, creates the LMSRequest paired to the ReqestType given
	 * @param type
	 * @return
	 * @throws InvalidDataException
	 */
	public LMSRequest createRequest(RequestType type, UserDataVO user, SMTServletRequest req) throws InvalidDataException {
		switch (type) {
			case usrLogin: 
				return usrLoginRequest(user);
			case crsLaunch:
				return null;
			case crsList:
				return null;
			case usrActive:
				return null;
			case usrCreate:
				return null;
			case usrHolding:
				return null;
			case usrMigrate:
				return null;
			case usrPoints:
				return null;
			case usrUpdate:
				return null;
		}
		
		throw new InvalidDataException("unknown request type: " + type);
	}
	
	
	private LMSRequest usrLoginRequest(UserDataVO user) {
		return new LMSRequest();
	}

}
