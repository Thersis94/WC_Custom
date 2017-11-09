package com.mindbody;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.apache.axis2.AxisFault;

import com.mindbody.vo.classes.MindBodyAddClientsToClassConfig;
import com.mindbody.vo.classes.MindBodyClassConfig;
import com.mindbody.vo.classes.MindBodyGetClassDescConfig;
import com.mindbody.vo.classes.MindBodyGetClassScheduleConfig;
import com.mindbody.vo.classes.MindBodyGetClassesConfig;
import com.mindbody.vo.classes.MindBodyGetCoursesConfig;
import com.mindbody.vo.classes.MindBodyGetEnrollmentsConfig;
import com.mindbody.vo.classes.MindBodyRemoveClientsFromClassesConfig;
import com.mindbodyonline.clients.api._0_5_1.AddClientsToClassesDocument;
import com.mindbodyonline.clients.api._0_5_1.AddClientsToClassesRequest;
import com.mindbodyonline.clients.api._0_5_1.AddClientsToClassesResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.ArrayOfClass;
import com.mindbodyonline.clients.api._0_5_1.ArrayOfClassDescription;
import com.mindbodyonline.clients.api._0_5_1.ArrayOfClassSchedule;
import com.mindbodyonline.clients.api._0_5_1.ArrayOfCourse;
import com.mindbodyonline.clients.api._0_5_1.Class_x0020_ServiceStub;
import com.mindbodyonline.clients.api._0_5_1.GetClassDescriptionsDocument;
import com.mindbodyonline.clients.api._0_5_1.GetClassDescriptionsRequest;
import com.mindbodyonline.clients.api._0_5_1.GetClassDescriptionsResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.GetClassSchedulesDocument;
import com.mindbodyonline.clients.api._0_5_1.GetClassSchedulesRequest;
import com.mindbodyonline.clients.api._0_5_1.GetClassSchedulesResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.GetClassesDocument;
import com.mindbodyonline.clients.api._0_5_1.GetClassesRequest;
import com.mindbodyonline.clients.api._0_5_1.GetClassesResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.GetCoursesDocument;
import com.mindbodyonline.clients.api._0_5_1.GetCoursesRequest;
import com.mindbodyonline.clients.api._0_5_1.GetCoursesResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.GetEnrollmentsDocument;
import com.mindbodyonline.clients.api._0_5_1.GetEnrollmentsRequest;
import com.mindbodyonline.clients.api._0_5_1.GetEnrollmentsResponseDocument;
import com.mindbodyonline.clients.api._0_5_1.RemoveClientsFromClassesDocument;
import com.mindbodyonline.clients.api._0_5_1.RemoveClientsFromClassesRequest;
import com.mindbodyonline.clients.api._0_5_1.RemoveClientsFromClassesResponseDocument;
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
		GET_CLASSES, ADD_CLIENTS_TO_CLASS, GET_ENROLLMEMTS, GET_CLASS_SCHEDULE, REMOVE_CLIENTS_FROM_CLASS, GET_COURSES, GET_CLASS_DESC
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


	/*
	 * (non-Javadoc)
	 * @see com.mindbody.MindBodyApiIntfc#getDocument(java.lang.String)
	 */
	@Override
	public List<Object> getDocument(MindBodyClassConfig config) throws RemoteException {
		List<Object> resp = null;

		if(config.isValid()) {
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
					break;
			}
		} else {
			throw new IllegalArgumentException("Config Not Valid.");
		}
		return resp;
	}



	/**
	 * Manage Building, Configuring and Executing the GetClasses Endpoint
	 * @param config
	 * @return
	 * @throws RemoteException
	 */
	private List<Object> getClasses(MindBodyGetClassesConfig config) throws RemoteException {
		List<Object> classes = new ArrayList<>();
		GetClassesRequest req = GetClassesRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureClassRequest(req, config);

		GetClassesDocument doc = GetClassesDocument.Factory.newInstance();
		doc.addNewGetClasses().setRequest(req);

		Class_x0020_ServiceStub client = getConfiguredStub();
		GetClassesResponseDocument res = client.getClasses(doc);
		ArrayOfClass classArr = res.getGetClassesResponse().getGetClassesResult().getClasses();
		for (int i = 0; i < classArr.sizeOfClass1Array(); i++) {
			classes.add(classArr.getClass1Array(i));
		}
		return classes;
	}


	/**
	 * Manage Configuring the GetClasses Request.
	 * @param req
	 * @param config
	 */
	private void configureClassRequest(GetClassesRequest req, MindBodyGetClassesConfig config) {

		//Set Any Class Description Ids
		if(!config.getClassDescriptionIds().isEmpty()) {
			req.setClassDescriptionIDs(buildArrayOfInt(config.getClassDescriptionIds()));
		}

		//Set Any Program Ids
		if(!config.getProgramIds().isEmpty()) {
			req.setProgramIDs(buildArrayOfInt(config.getProgramIds()));
		}

		//Set Any Staff Ids
		if(!config.getStaffIds().isEmpty()) {
			req.setStaffIDs(buildArrayOfLong(config.getStaffIds()));
		}

		//Set Any Location Ids
		if(!config.getClassDescriptionIds().isEmpty()) {
			req.setLocationIDs(buildArrayOfInt(config.getLocationIds()));
		}

		//Set Start Time
		if(config.getStartDt() != null) {
			req.setStartDateTime(Convert.toCalendar(config.getStartDt()));
		}

		//Set End Time
		if(config.getEndDt() != null) {
			req.setEndDateTime(Convert.toCalendar(config.getEndDt()));
		}
		//Set Last Modified Time
		if(config.getEndDt() != null) {
			req.setLastModifiedDate(Convert.toCalendar(config.getModifiedDt()));
		}
	}


	/**
	 * Manage Building, Configuring and Executing the GetClassDescription Endpoint
	 * @param config
	 * @return
	 * @throws RemoteException 
	 */
	private List<Object> getClassDescriptions(MindBodyGetClassDescConfig config) throws RemoteException {
		List<Object> descriptions = new ArrayList<>();
		GetClassDescriptionsRequest req = GetClassDescriptionsRequest.Factory.newInstance();
		prepareRequest(req, config);
		configureClassDescriptionRequest(req, config);

		GetClassDescriptionsDocument doc = GetClassDescriptionsDocument.Factory.newInstance();
		doc.addNewGetClassDescriptions().setRequest(req);

		Class_x0020_ServiceStub client = getConfiguredStub();
		GetClassDescriptionsResponseDocument res = client.getClassDescriptions(doc);
		ArrayOfClassDescription classArr = res.getGetClassDescriptionsResponse().getGetClassDescriptionsResult().getClassDescriptions();
		for (int i = 0; i < classArr.sizeOfClassDescriptionArray(); i++) {
			descriptions.add(classArr.getClassDescriptionArray(i));
		}
		return descriptions;
	}


	/**
	 * Manages Configuring the ClassDescriptionRequest object.
	 * @param req
	 * @param config
	 */
	private void configureClassDescriptionRequest(GetClassDescriptionsRequest req, MindBodyGetClassDescConfig config) {

		//Set Any Class Description Ids
		if(!config.getClassDescriptionIds().isEmpty()) {
			req.setClassDescriptionIDs(buildArrayOfInt(config.getClassDescriptionIds()));
		}

		//Set Any Program Ids
		if(!config.getProgramIds().isEmpty()) {
			req.setProgramIDs(buildArrayOfInt(config.getProgramIds()));
		}

		//Set Any Staff Ids
		if(!config.getStaffIds().isEmpty()) {
			req.setStaffIDs(buildArrayOfLong(config.getStaffIds()));
		}

		//Set Any Location Ids
		if(!config.getClassDescriptionIds().isEmpty()) {
			req.setLocationIDs(buildArrayOfInt(config.getLocationIds()));
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
	private List<Object> addClientsToClass(MindBodyAddClientsToClassConfig config) throws RemoteException {
		List<Object> classes = new ArrayList<>();
		AddClientsToClassesRequest req = AddClientsToClassesRequest.Factory.newInstance();
		prepareRequest(req, config);

		AddClientsToClassesDocument doc = AddClientsToClassesDocument.Factory.newInstance();
		doc.addNewAddClientsToClasses().setRequest(req);

		Class_x0020_ServiceStub client = getConfiguredStub();
		AddClientsToClassesResponseDocument res = client.addClientsToClasses(doc);
		ArrayOfClass classArr = res.getAddClientsToClassesResponse().getAddClientsToClassesResult().addNewClasses();

		for (int i = 0; i < classArr.sizeOfClass1Array(); i++) {
			classes.add(classArr.getClass1Array(i));
		}

		return classes;
	}


	/**
	 * Manage Building, Configuring and Executing the GetClassSchedule Endpoint
	 * @param config
	 * @return
	 */
	private List<Object> getClassSchedule(MindBodyGetClassScheduleConfig config) throws RemoteException {
		List<Object> classes = new ArrayList<>();
		GetClassSchedulesRequest req = GetClassSchedulesRequest.Factory.newInstance();
		prepareRequest(req, config);

		GetClassSchedulesDocument doc = GetClassSchedulesDocument.Factory.newInstance();
		doc.addNewGetClassSchedules().setRequest(req);

		Class_x0020_ServiceStub client = getConfiguredStub();
		GetClassSchedulesResponseDocument res = client.getClassSchedules(doc);
		ArrayOfClassSchedule classArr = res.getGetClassSchedulesResponse().getGetClassSchedulesResult().getClassSchedules();

		for (int i = 0; i < classArr.sizeOfClassScheduleArray(); i++) {
			classes.add(classArr.getClassScheduleArray(i));
		}

		return classes;
	}


	/**
	 * Manage Building, Configuring and Executing the GetCourses Endpoint
	 * @param config
	 * @return
	 */
	private List<Object> getCourses(MindBodyGetCoursesConfig config) throws RemoteException {
		List<Object> courses = new ArrayList<>();
		GetCoursesRequest req = GetCoursesRequest.Factory.newInstance();
		prepareRequest(req, config);

		GetCoursesDocument gcd = GetCoursesDocument.Factory.newInstance();
		gcd.addNewGetCourses().setRequest(req);

		Class_x0020_ServiceStub client = getConfiguredStub();
		GetCoursesResponseDocument res = client.getCourses(gcd);
		ArrayOfCourse classArr = res.getGetCoursesResponse().getGetCoursesResult().getCourses();
		for (int i = 0; i < classArr.sizeOfCourseArray(); i++) {
			courses.add(classArr.getCourseArray(i));
		}
		return courses;
	}


	/**
	 * Manage Building, Configuring and Executing the GetEnrollments Endpoint
	 * @param config
	 * @return
	 */
	private List<Object> getEnrollments(MindBodyGetEnrollmentsConfig config) throws RemoteException {
		List<Object> enrollments = new ArrayList<>();
		GetEnrollmentsRequest req = GetEnrollmentsRequest.Factory.newInstance();
		prepareRequest(req, config);

		GetEnrollmentsDocument gcd = GetEnrollmentsDocument.Factory.newInstance();
		gcd.addNewGetEnrollments().setRequest(req);

		Class_x0020_ServiceStub client = getConfiguredStub();
		GetEnrollmentsResponseDocument res = client.getEnrollments(gcd);
		ArrayOfClassSchedule classArr = res.getGetEnrollmentsResponse().getGetEnrollmentsResult().getEnrollments();
		for (int i = 0; i < classArr.sizeOfClassScheduleArray(); i++) {
			enrollments.add(classArr.getClassScheduleArray(i));
		}
		return enrollments;
	}


	/**
	 * Manage Building, Configuring and Exectuing the RemoveClientsFromClass Endpoint
	 * @param config
	 * @return
	 */
	private List<Object> removeClientsFromClass(MindBodyRemoveClientsFromClassesConfig config) throws RemoteException {
		List<Object> clients = new ArrayList<>();
		RemoveClientsFromClassesRequest req = RemoveClientsFromClassesRequest.Factory.newInstance();
		prepareRequest(req, config);

		RemoveClientsFromClassesDocument gcd = RemoveClientsFromClassesDocument.Factory.newInstance();
		gcd.addNewRemoveClientsFromClasses().setRequest(req);

		Class_x0020_ServiceStub client = getConfiguredStub();
		RemoveClientsFromClassesResponseDocument res = client.removeClientsFromClasses(gcd);
		ArrayOfClass classArr = res.getRemoveClientsFromClassesResponse().getRemoveClientsFromClassesResult().getClasses();
		for (int i = 0; i < classArr.sizeOfClass1Array(); i++) {
			clients.add(classArr.getClass1Array(i));
		}
		return clients;
	}
}