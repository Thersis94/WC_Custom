package com.mindbody;

import java.rmi.RemoteException;

import org.apache.axis2.AxisFault;

import com.mindbody.util.MindBodyUtil;
import com.mindbody.vo.MindBodyResponseVO;
import com.mindbody.vo.site.MindBodyGetLocationsConfig;
import com.mindbody.vo.site.MindBodyGetProgramsConfig;
import com.mindbody.vo.site.MindBodyGetRelationshipsConfig;
import com.mindbody.vo.site.MindBodyGetSessionTypesConfig;
import com.mindbody.vo.site.MindBodySiteConfig;
import com.mindbodyonline.clients.api._0_5_1.GetLocationsDocument;
import com.mindbodyonline.clients.api._0_5_1.GetLocationsRequest;
import com.mindbodyonline.clients.api._0_5_1.GetLocationsResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.GetLocationsResult;
import com.mindbodyonline.clients.api._0_5_1.GetProgramsDocument;
import com.mindbodyonline.clients.api._0_5_1.GetProgramsRequest;
import com.mindbodyonline.clients.api._0_5_1.GetProgramsResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.GetProgramsResult;
import com.mindbodyonline.clients.api._0_5_1.GetRelationshipsDocument;
import com.mindbodyonline.clients.api._0_5_1.GetRelationshipsRequest;
import com.mindbodyonline.clients.api._0_5_1.GetRelationshipsResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.GetRelationshipsResult;
import com.mindbodyonline.clients.api._0_5_1.GetSessionTypesDocument;
import com.mindbodyonline.clients.api._0_5_1.GetSessionTypesRequest;
import com.mindbodyonline.clients.api._0_5_1.GetSessionTypesResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.GetSessionTypesResult;
import com.mindbodyonline.clients.api._0_5_1.Location;
import com.mindbodyonline.clients.api._0_5_1.Program;
import com.mindbodyonline.clients.api._0_5_1.Relationship;
import com.mindbodyonline.clients.api._0_5_1.SessionType;
import com.mindbodyonline.clients.api._0_5_1.Site_x0020_ServiceStub;
import com.siliconmtn.common.http.HttpStatus;

/****************************************************************************
 * <b>Title:</b> MindBodySiteApi.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manage Mind Body Site Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 26, 2017
 ****************************************************************************/
public class MindBodySiteApi extends AbstractMindBodyApi<Site_x0020_ServiceStub, MindBodySiteConfig> {

	public enum SiteDocumentType {
		GET_SITES,
		GET_LOCATIONS,
		GET_ACTIVATION_CODE,
		GET_PROGRAMS,
		GET_SESSION_TYPES,
		GET_RESOURCES,
		GET_RELATIONSHIPS,
		GET_GENDERS,
		GET_PROSPECT_STAGES,
		GET_MOBILE_PROVIDERS
	}
	/* (non-Javadoc)
	 * @see com.mindbody.MindBodyApiIntfc#getStub()
	 */
	@Override
	public Site_x0020_ServiceStub getStub() throws AxisFault {
		return new Site_x0020_ServiceStub();
	}

	/* (non-Javadoc)
	 * @see com.mindbody.AbstractMindBodyApi#processRequest(com.mindbody.vo.MindBodyConfig)
	 */
	@Override
	protected MindBodyResponseVO processRequest(MindBodySiteConfig config) throws RemoteException {
		MindBodyResponseVO resp;
		switch(config.getType()) {
			case GET_LOCATIONS:
				resp = getLocations((MindBodyGetLocationsConfig)config);
				break;
			case GET_PROGRAMS:
				resp = getPrograms((MindBodyGetProgramsConfig)config);
				break;
			case GET_RELATIONSHIPS:
				resp = getRelationships((MindBodyGetRelationshipsConfig)config);
				break;
			case GET_SESSION_TYPES:
				resp = getSessionTypes((MindBodyGetSessionTypesConfig)config);
				break;
			default:
				log.error("Endpoint Not Supported.");
				resp = buildErrorResponse(HttpStatus.CD_501_NOT_IMPLEMENTED, "Endpoint Not Supported");
				break;
			
		}
		return resp;
	}

	/**
	 * @param config
	 * @return
	 * @throws RemoteException 
	 */
	private MindBodyResponseVO getLocations(MindBodyGetLocationsConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		GetLocationsRequest req = GetLocationsRequest.Factory.newInstance();
		prepareRequest(req, config);

		GetLocationsDocument doc = GetLocationsDocument.Factory.newInstance();
		doc.getGetLocations().setRequest(req);

		Site_x0020_ServiceStub client = getConfiguredStub();
		GetLocationsResponseDocument res = client.getLocations(doc);
		GetLocationsResult r = res.getGetLocationsResponse().getGetLocationsResult();
		resp.populateResponseFields(r);
		if(resp.isValid()) {
			for(Location l : r.getLocations().getLocationArray()) {
				resp.addResults(MindBodyUtil.convertLocation(l));
			}
		}

		return resp;
	}

	/**
	 * @param config
	 * @return
	 * @throws RemoteException 
	 */
	private MindBodyResponseVO getPrograms(MindBodyGetProgramsConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		GetProgramsRequest req = GetProgramsRequest.Factory.newInstance();
		prepareRequest(req, config);

		GetProgramsDocument doc = GetProgramsDocument.Factory.newInstance();
		doc.getGetPrograms().setRequest(req);

		Site_x0020_ServiceStub client = getConfiguredStub();
		GetProgramsResponseDocument res = client.getPrograms(doc);
		GetProgramsResult r = res.getGetProgramsResponse().getGetProgramsResult();
		resp.populateResponseFields(r);
		if(resp.isValid()) {
			for(Program p : r.getPrograms().getProgramArray()) {
				resp.addResults(MindBodyUtil.convertProgram(p));
			}
		}
		return resp;
	}

	/**
	 * @param config
	 * @return
	 */
	private MindBodyResponseVO getRelationships(MindBodyGetRelationshipsConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		GetRelationshipsRequest req = GetRelationshipsRequest.Factory.newInstance();
		prepareRequest(req, config);

		GetRelationshipsDocument doc = GetRelationshipsDocument.Factory.newInstance();
		doc.getGetRelationships().setRequest(req);

		Site_x0020_ServiceStub client = getConfiguredStub();
		GetRelationshipsResponseDocument res = client.getRelationships(doc);
		GetRelationshipsResult r = res.getGetRelationshipsResponse().getGetRelationshipsResult();
		resp.populateResponseFields(r);
		if(resp.isValid()) {
			for(Relationship rel : r.getRelationships().getRelationshipArray()) {
				resp.addResults(MindBodyUtil.convertRelationship(rel));
			}
		}
		return resp;
	}

	/**
	 * @param config
	 * @return
	 */
	private MindBodyResponseVO getSessionTypes(MindBodyGetSessionTypesConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		GetSessionTypesRequest req = GetSessionTypesRequest.Factory.newInstance();
		prepareRequest(req, config);

		GetSessionTypesDocument doc = GetSessionTypesDocument.Factory.newInstance();
		doc.getGetSessionTypes().setRequest(req);

		Site_x0020_ServiceStub client = getConfiguredStub();
		GetSessionTypesResponseDocument res = client.getSessionTypes(doc);
		GetSessionTypesResult r = res.getGetSessionTypesResponse().getGetSessionTypesResult();
		resp.populateResponseFields(r);
		if(resp.isValid()) {
			for(SessionType rel : r.getSessionTypes().getSessionTypeArray()) {
				resp.addResults(MindBodyUtil.convertSessionType(rel));
			}
		}
		return resp;
	}
}