package com.mindbody;

import java.rmi.RemoteException;

import org.apache.axis2.AxisFault;

import com.mindbody.vo.MindBodyResponseVO;
import com.mindbody.vo.classes.MindBodyAddClientsToClassConfig;
import com.mindbody.vo.classes.MindBodyClassConfig;
import com.mindbody.vo.classes.MindBodyGetClassDescConfig;
import com.mindbody.vo.classes.MindBodyGetClassScheduleConfig;
import com.mindbody.vo.classes.MindBodyGetClassesConfig;
import com.mindbody.vo.classes.MindBodyGetCoursesConfig;
import com.mindbody.vo.classes.MindBodyGetEnrollmentsConfig;
import com.mindbody.vo.classes.MindBodyRemoveClientsFromClassesConfig;

//Mind Body Class API Jar
import com.mindbodyonline.clients.api._0_5_1.AddClientsToClassesDocument;
import com.mindbodyonline.clients.api._0_5_1.AddClientsToClassesRequest;
import com.mindbodyonline.clients.api._0_5_1.AddClientsToClassesResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.AddClientsToClassesResult;
import com.mindbodyonline.clients.api._0_5_1.Class_x0020_ServiceStub;
import com.mindbodyonline.clients.api._0_5_1.GetClassDescriptionsDocument;
import com.mindbodyonline.clients.api._0_5_1.GetClassDescriptionsRequest;
import com.mindbodyonline.clients.api._0_5_1.GetClassDescriptionsResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.GetClassDescriptionsResult;
import com.mindbodyonline.clients.api._0_5_1.GetClassSchedulesDocument;
import com.mindbodyonline.clients.api._0_5_1.GetClassSchedulesRequest;
import com.mindbodyonline.clients.api._0_5_1.GetClassSchedulesResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.GetClassSchedulesResult;
import com.mindbodyonline.clients.api._0_5_1.GetClassesDocument;
import com.mindbodyonline.clients.api._0_5_1.GetClassesRequest;
import com.mindbodyonline.clients.api._0_5_1.GetClassesResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.GetClassesResult;
import com.mindbodyonline.clients.api._0_5_1.GetCoursesDocument;
import com.mindbodyonline.clients.api._0_5_1.GetCoursesRequest;
import com.mindbodyonline.clients.api._0_5_1.GetCoursesResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.GetCoursesResult;
import com.mindbodyonline.clients.api._0_5_1.GetEnrollmentsDocument;
import com.mindbodyonline.clients.api._0_5_1.GetEnrollmentsRequest;
import com.mindbodyonline.clients.api._0_5_1.GetEnrollmentsResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.GetEnrollmentsResult;
import com.mindbodyonline.clients.api._0_5_1.RemoveClientsFromClassesDocument;
import com.mindbodyonline.clients.api._0_5_1.RemoveClientsFromClassesRequest;
import com.mindbodyonline.clients.api._0_5_1.RemoveClientsFromClassesResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.RemoveClientsFromClassesResult;

//Base Libs
import com.siliconmtn.common.http.HttpStatus;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title:</b> MindBodyClassApiAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manage Mind Body Class Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 3, 2017
 ****************************************************************************/
public class MindBodyClassApi extends AbstractMindBodyApi<Class_x0020_ServiceStub, MindBodyClassConfig> {

	public enum ClassDocumentType {
		GET_CLASSES, 
		UPDATE_CLIENT_VISITS, 
		GET_CLASS_VISITS, 
		GET_CLASS_DESC,
		GET_ENROLLMEMTS,
		GET_CLASS_SCHEDULE,
		ADD_CLIENTS_TO_CLASS,
		REMOVE_CLIENTS_FROM_CLASS,
		ADD_CLIENTS_TO_ENROLLMENTS,
		REMOVE_FROM_WAITLIST,
		GET_SEMESTERS,
		GET_COURSES,
		GET_WAITLIST_ENTRIES,
		SUBSTITUTE_CLASS_TEACHER,
		CANCEL_SINGLE_CLASS
	}


	/**
	 * 
	 */
	public MindBodyClassApi() {
		super();
	}


	/*
	 * (non-Javadoc)
	 * @see com.mindbody.AbstractMindBodyApi#buildStub()
	 */
	@Override
	public Class_x0020_ServiceStub getStub() throws AxisFault {
		return new Class_x0020_ServiceStub();
	}


	/* (non-Javadoc)
	 * @see com.mindbody.AbstractMindBodyApi#processRequest(com.mindbody.vo.MindBodyConfig)
	 */
	@Override
	protected MindBodyResponseVO processRequest(MindBodyClassConfig config) throws RemoteException {
		MindBodyResponseVO resp;
		switch (config.getType()) {
			case GET_CLASSES:
				resp = getClasses((MindBodyGetClassesConfig) config);
				break;
			case GET_CLASS_DESC: 
				resp = getClassDescriptions((MindBodyGetClassDescConfig) config);
				break;
			case ADD_CLIENTS_TO_CLASS:
				resp = addClientsToClass((MindBodyAddClientsToClassConfig) config);
				break;
			case GET_CLASS_SCHEDULE:
				resp = getClassSchedule((MindBodyGetClassScheduleConfig) config);
				break;
			case GET_COURSES:
				resp = getCourses((MindBodyGetCoursesConfig) config);
				break;
			case GET_ENROLLMEMTS:
				resp = getEnrollments((MindBodyGetEnrollmentsConfig) config);
				break;
			case REMOVE_CLIENTS_FROM_CLASS:
				resp = removeClientsFromClass((MindBodyRemoveClientsFromClassesConfig) config);
				break;
			default:
				log.info("Endpoint not supported.");
				resp = buildErrorResponse(HttpStatus.CD_501_NOT_IMPLEMENTED, "Endpoint Not Supported");
				break;
		}
		return resp;
	}


	/**
	 * Manage Building, Configuring and Executing the GetClasses Endpoint
	 * @param config
	 * @return
	 * @throws RemoteException
	 */
	private MindBodyResponseVO getClasses(MindBodyGetClassesConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		GetClassesRequest req = GetClassesRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureClassRequest(req, config);

		GetClassesDocument doc = GetClassesDocument.Factory.newInstance();
		doc.addNewGetClasses().setRequest(req);

		Class_x0020_ServiceStub client = getConfiguredStub();
		GetClassesResponseDocument res = client.getClasses(doc);
		GetClassesResult r = res.getGetClassesResponse().getGetClassesResult();
		resp.populateResponseFields(r);
		if(resp.isValid()) {
			resp.addResults((Object [])r.getClasses().getClass1Array());
		}
		return resp;
	}


	/**
	 * Manage Configuring the GetClasses Request.
	 * @param req
	 * @param config
	 */
	private void configureClassRequest(GetClassesRequest req, MindBodyGetClassesConfig config) {

		//Set Any Class Description Ids
		if(!config.getClassDescriptionIds().isEmpty()) {
			req.setClassDescriptionIDs(MindBodyUtil.buildArrayOfInt(config.getClassDescriptionIds()));
		}

		//Set Any Class Ids
		if(!config.getClassIds().isEmpty()) {
			req.setClassIDs(MindBodyUtil.buildArrayOfInt(config.getClassIds()));
		}

		//Set Any Staff Ids
		if(!config.getStaffIds().isEmpty()) {
			req.setStaffIDs(MindBodyUtil.buildArrayOfLong(config.getStaffIds()));
		}

		//Set Start DateTime
		if(config.getStartDt() != null) {
			req.setStartDateTime(Convert.toCalendar(config.getStartDt()));
		}

		//Set End DateTime
		if(config.getEndDt() != null) {
			req.setEndDateTime(Convert.toCalendar(config.getEndDt()));
		}

		//Set Client Id if present (Use first)
		if(!config.getClientIds().isEmpty()) {
			req.setClientID(config.getClientIds().get(0));
		}

		//Set Any Program Ids
		if(!config.getProgramIds().isEmpty()) {
			req.setProgramIDs(MindBodyUtil.buildArrayOfInt(config.getProgramIds()));
		}

		//Set Any SessionTypeIds
		if(!config.getSessionTypeIds().isEmpty()) {
			req.setSessionTypeIDs(MindBodyUtil.buildArrayOfInt(config.getSessionTypeIds()));
		}

		//Set Any Location Ids
		if(!config.getClassDescriptionIds().isEmpty()) {
			req.setLocationIDs(MindBodyUtil.buildArrayOfInt(config.getLocationIds()));
		}

		//Set Any Semester Ids
		if(!config.getSemesterIds().isEmpty()) {
			req.setSemesterIDs(MindBodyUtil.buildArrayOfInt(config.getSemesterIds()));
		}

		//Set Hide Canceled Classes Flag
		if(config.isHideCanceledClasses()) {
			req.setHideCanceledClasses(config.isHideCanceledClasses());
		}

		//Set Use Schedulign Window Flag
		if(config.isUseSchedulingWindow()) {
			req.setSchedulingWindow(config.isUseSchedulingWindow());
		}

		//Set Last Modified Time
		if(config.getModifiedDt() != null) {
			req.setLastModifiedDate(Convert.toCalendar(config.getModifiedDt()));
		}
	}


	/**
	 * Manage Building, Configuring and Executing the GetClassDescription Endpoint
	 * @param config
	 * @return
	 * @throws RemoteException 
	 */
	private MindBodyResponseVO getClassDescriptions(MindBodyGetClassDescConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		GetClassDescriptionsRequest req = GetClassDescriptionsRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureClassDescriptionRequest(req, config);

		GetClassDescriptionsDocument doc = GetClassDescriptionsDocument.Factory.newInstance();
		doc.addNewGetClassDescriptions().setRequest(req);

		Class_x0020_ServiceStub client = getConfiguredStub();
		GetClassDescriptionsResponseDocument res = client.getClassDescriptions(doc);
		GetClassDescriptionsResult r = res.getGetClassDescriptionsResponse().getGetClassDescriptionsResult();
		resp.populateResponseFields(r);
		if(resp.isValid()) {
			resp.addResults((Object [])r.getClassDescriptions().getClassDescriptionArray());
		}

		return resp;
	}


	/**
	 * Manages Configuring the ClassDescriptionRequest object.
	 * @param req
	 * @param config
	 */
	private void configureClassDescriptionRequest(GetClassDescriptionsRequest req, MindBodyGetClassDescConfig config) {

		//Set Any Class Description Ids
		if(!config.getClassDescriptionIds().isEmpty()) {
			req.setClassDescriptionIDs(MindBodyUtil.buildArrayOfInt(config.getClassDescriptionIds()));
		}

		//Set Any Program Ids
		if(!config.getProgramIds().isEmpty()) {
			req.setProgramIDs(MindBodyUtil.buildArrayOfInt(config.getProgramIds()));
		}

		//Set Any Staff Ids
		if(!config.getStaffIds().isEmpty()) {
			req.setStaffIDs(MindBodyUtil.buildArrayOfLong(config.getStaffIds()));
		}

		//Set Any Location Ids
		if(!config.getClassDescriptionIds().isEmpty()) {
			req.setLocationIDs(MindBodyUtil.buildArrayOfInt(config.getLocationIds()));
		}

		//Set Start Class Time
		if(config.getStartDt() != null) {
			req.setStartClassDateTime(Convert.toCalendar(config.getStartDt()));
		}

		//Set End Class Time
		if(config.getEndDt() != null) {
			req.setEndClassDateTime(Convert.toCalendar(config.getEndDt()));
		}
	}


	/**
	 * Manage Building, Configuring and Executing the AddClientsToClass Endpoint
	 * @param config
	 * @return
	 */
	private MindBodyResponseVO addClientsToClass(MindBodyAddClientsToClassConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		AddClientsToClassesRequest req = AddClientsToClassesRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureAddClientsToClassRequest(req, config);

		AddClientsToClassesDocument doc = AddClientsToClassesDocument.Factory.newInstance();
		doc.addNewAddClientsToClasses().setRequest(req);

		Class_x0020_ServiceStub client = getConfiguredStub();
		AddClientsToClassesResponseDocument res = client.addClientsToClasses(doc);
		AddClientsToClassesResult r = res.getAddClientsToClassesResponse().getAddClientsToClassesResult();
		resp.populateResponseFields(r);
		if(resp.isValid()) {
			resp.addResults((Object [])r.getClasses().getClass1Array());
		}

		return resp;
	}


	/**
	 * Manage Configuring the AddClientsToClass Request.
	 * @param req
	 * @param config
	 */
	private void configureAddClientsToClassRequest(AddClientsToClassesRequest req, MindBodyAddClientsToClassConfig config) {

		if(!config.getClientIds().isEmpty()) {
			req.setClientIDs(MindBodyUtil.buildArrayOfString(config.getClientIds()));
		}
		//Set Any Class Ids
		if(!config.getClassIds().isEmpty()) {
			req.setClassIDs(MindBodyUtil.buildArrayOfInt(config.getClassIds()));
		}

		//Set RequirePayment Flag and ClientServiceId
		if(config.isRequirePayment()) {
			req.setRequirePayment(config.isRequirePayment());
			req.setClientServiceID(config.getClientServiceId());
		}

		//Set Use WaitList Flag and WaitListEntryId
		if(config.isUseWaitList()) {
			req.setWaitlist(config.isUseWaitList());
			req.setWaitlistEntryID(config.getWaitListEntryId());
		}

		//Set Send Email Flag
		if(config.isSendEmail()) {
			req.setSendEmail(config.isSendEmail());
		}
	}


	/**
	 * Manage Building, Configuring and Executing the GetClassSchedule Endpoint
	 * @param config
	 * @return
	 */
	private MindBodyResponseVO getClassSchedule(MindBodyGetClassScheduleConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		GetClassSchedulesRequest req = GetClassSchedulesRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureClassScheduleRequest(req, config);

		GetClassSchedulesDocument doc = GetClassSchedulesDocument.Factory.newInstance();
		doc.addNewGetClassSchedules().setRequest(req);

		Class_x0020_ServiceStub client = getConfiguredStub();
		GetClassSchedulesResponseDocument res = client.getClassSchedules(doc);
		GetClassSchedulesResult r = res.getGetClassSchedulesResponse().getGetClassSchedulesResult();
		resp.populateResponseFields(r);
		if(resp.isValid()) {
			resp.addResults((Object [])r.getClassSchedules().getClassScheduleArray());
		}

		return resp;
	}


	/**
	 * Manage Configuring the ClassSchedule Request.
	 * @param req
	 * @param config
	 */
	private void configureClassScheduleRequest(GetClassSchedulesRequest req, MindBodyGetClassScheduleConfig config) {

		//Set Any Location Ids
		if(!config.getLocationIds().isEmpty()) {
			req.setLocationIDs(MindBodyUtil.buildArrayOfInt(config.getLocationIds()));
		}

		//Set Any Class Schedule Ids
		if(!config.getClassScheduleIds().isEmpty()) {
			req.setClassScheduleIDs(MindBodyUtil.buildArrayOfInt(config.getClassScheduleIds()));
		}

		//Set Any Staff Ids
		if(!config.getStaffIds().isEmpty()) {
			req.setStaffIDs(MindBodyUtil.buildArrayOfLong(config.getStaffIds()));
		}

		//Set Any Program Ids
		if(!config.getProgramIds().isEmpty()) {
			req.setProgramIDs(MindBodyUtil.buildArrayOfInt(config.getProgramIds()));
		}

		//Set Any SessionTypeIds
		if(!config.getSessionTypeIds().isEmpty()) {
			req.setSessionTypeIDs(MindBodyUtil.buildArrayOfInt(config.getSessionTypeIds()));
		}

		//Set Start DateTime
		if(config.getStartDt() != null) {
			req.setStartDate(Convert.toCalendar(config.getStartDt()));
		}

		//Set End DateTime
		if(config.getEndDt() != null) {
			req.setEndDate(Convert.toCalendar(config.getEndDt()));
		}
	}


	/**
	 * Manage Building, Configuring and Executing the GetCourses Endpoint
	 * @param config
	 * @return
	 */
	private MindBodyResponseVO getCourses(MindBodyGetCoursesConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		GetCoursesRequest req = GetCoursesRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureCoursesRequest(req, config);

		GetCoursesDocument gcd = GetCoursesDocument.Factory.newInstance();
		gcd.addNewGetCourses().setRequest(req);

		Class_x0020_ServiceStub client = getConfiguredStub();
		GetCoursesResponseDocument res = client.getCourses(gcd);
		GetCoursesResult r = res.getGetCoursesResponse().getGetCoursesResult();
		resp.populateResponseFields(r);
		if(resp.isValid()) {
			resp.addResults((Object [])r.getCourses().getCourseArray());
		}

		return resp;
	}


	/**
	 * Manage Configuring the GetCourses Request.
	 * @param req
	 * @param config
	 */
	private void configureCoursesRequest(GetCoursesRequest req, MindBodyGetCoursesConfig config) {
		if(!config.getLocationIds().isEmpty()) {
			req.setLocationIDs(MindBodyUtil.buildArrayOfInt(config.getLocationIds()));
		}

		if(!config.getCourseIds().isEmpty()) {
			req.setCourseIDs(MindBodyUtil.buildArrayOfLong(config.getCourseIds()));
		}

		if(!config.getStaffIds().isEmpty()) {
			req.setStaffIDs(MindBodyUtil.buildArrayOfLong(config.getStaffIds()));
		}

		if(!config.getProgramIds().isEmpty()) {
			req.setProgramIDs(MindBodyUtil.buildArrayOfInt(config.getProgramIds()));
		}

		if(config.getStartDt() != null) {
			req.setStartDate(Convert.toCalendar(config.getStartDt()));
		}

		if(config.getEndDt() != null) {
			req.setEndDate(Convert.toCalendar(config.getEndDt()));
		}

		if(!config.getSemesterIds().isEmpty()) {
			req.setSemesterIDs(MindBodyUtil.buildArrayOfInt(config.getSemesterIds()));
		}
	}


	/**
	 * Manage Building, Configuring and Executing the GetEnrollments Endpoint
	 * @param config
	 * @return
	 */
	private MindBodyResponseVO getEnrollments(MindBodyGetEnrollmentsConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		GetEnrollmentsRequest req = GetEnrollmentsRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureEnrollmentsRequest(req, config);

		GetEnrollmentsDocument gcd = GetEnrollmentsDocument.Factory.newInstance();
		gcd.addNewGetEnrollments().setRequest(req);

		Class_x0020_ServiceStub client = getConfiguredStub();
		GetEnrollmentsResponseDocument res = client.getEnrollments(gcd);
		GetEnrollmentsResult r = res.getGetEnrollmentsResponse().getGetEnrollmentsResult();
		resp.populateResponseFields(r);
		if(resp.isValid()) {
			resp.addResults((Object [])r.getEnrollments().getClassScheduleArray());
		}

		return resp;
	}


	/**
	 * Manage Configuring the GetEnrollments Request.
	 * @param req
	 * @param config
	 */
	private void configureEnrollmentsRequest(GetEnrollmentsRequest req, MindBodyGetEnrollmentsConfig config) {
		if(!config.getLocationIds().isEmpty()) {
			req.setLocationIDs(MindBodyUtil.buildArrayOfInt(config.getLocationIds()));
		}

		if(!config.getClassScheduleIds().isEmpty()) {
			req.setClassScheduleIDs(MindBodyUtil.buildArrayOfInt(config.getClassScheduleIds()));
		}

		if(!config.getStaffIds().isEmpty()) {
			req.setStaffIDs(MindBodyUtil.buildArrayOfLong(config.getStaffIds()));
		}

		if(!config.getProgramIds().isEmpty()) {
			req.setProgramIDs(MindBodyUtil.buildArrayOfInt(config.getProgramIds()));
		}

		if(!config.getSessionTypeIds().isEmpty()) {
			req.setSessionTypeIDs(MindBodyUtil.buildArrayOfInt(config.getSessionTypeIds()));
		}

		if(!config.getSemesterIds().isEmpty()) {
			req.setSemesterIDs(MindBodyUtil.buildArrayOfInt(config.getSemesterIds()));
		}

		if(!config.getCourseIds().isEmpty()) {
			req.setCourseIDs(MindBodyUtil.buildArrayOfLong(config.getCourseIds()));
		}

		if(config.getStartDt() != null) {
			req.setStartDate(Convert.toCalendar(config.getStartDt()));
		}

		if(config.getEndDt() != null) {
			req.setEndDate(Convert.toCalendar(config.getEndDt()));
		}
	}


	/**
	 * Manage Building, Configuring and Exectuing the RemoveClientsFromClass Endpoint
	 * @param config
	 * @return
	 */
	private MindBodyResponseVO removeClientsFromClass(MindBodyRemoveClientsFromClassesConfig config) throws RemoteException {
		MindBodyResponseVO resp = new MindBodyResponseVO();
		RemoveClientsFromClassesRequest req = RemoveClientsFromClassesRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureRemoveClientsFromClassRequest(req, config);

		RemoveClientsFromClassesDocument gcd = RemoveClientsFromClassesDocument.Factory.newInstance();
		gcd.addNewRemoveClientsFromClasses().setRequest(req);

		Class_x0020_ServiceStub client = getConfiguredStub();
		RemoveClientsFromClassesResponseDocument res = client.removeClientsFromClasses(gcd);
		RemoveClientsFromClassesResult r = res.getRemoveClientsFromClassesResponse().getRemoveClientsFromClassesResult();
		resp.populateResponseFields(r);
		if(resp.isValid()) {
			resp.addResults((Object [])r.getClasses().getClass1Array());
		}

		return resp;
	}


	/**
	 * Manage Configuring the RemoveClientsFromClass Request.
	 * @param req
	 * @param config
	 */
	private void configureRemoveClientsFromClassRequest(RemoveClientsFromClassesRequest req, MindBodyRemoveClientsFromClassesConfig config) {

		//Set And Client Ids
		if(!config.getClientIds().isEmpty()) {
			req.setClientIDs(MindBodyUtil.buildArrayOfString(config.getClientIds()));
		}

		//Set Any Class Ids
		if(!config.getClassIds().isEmpty()) {
			req.setClassIDs(MindBodyUtil.buildArrayOfInt(config.getClassIds()));
		}

		//Set Test Flag
		if(config.isTest()) {
			req.setTest(config.isTest());
		}

		//Set SendEmail Flag
		if(config.isSendEmail()) {
			req.setSendEmail(config.isSendEmail());
		}

		//Set LateCancel Flag
		if(config.isAllowLateCancel()) {
			req.setLateCancel(config.isAllowLateCancel());
		}
	}
}