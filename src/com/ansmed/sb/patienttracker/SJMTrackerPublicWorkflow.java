package com.ansmed.sb.patienttracker;

// JDK 1.6
import java.sql.SQLException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

// SMT base libs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;

// Sitebuilder II libs
import com.smt.sitebuilder.action.tracker.vo.AssigneeVO;
import com.smt.sitebuilder.action.tracker.PatientAction;
import com.smt.sitebuilder.action.tracker.PatientManager;
import com.smt.sitebuilder.action.tracker.vo.PatientVO;
import com.smt.sitebuilder.action.tracker.TrackerAction;
import com.smt.sitebuilder.action.tracker.TrackerDataContainer;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.vo.GenericQueryVO;
import com.smt.sitebuilder.data.vo.QueryParamVO;
import com.smt.sitebuilder.data.vo.GenericQueryVO.Operator;

/****************************************************************************
* <b>Title</b>SJMTrackerPublicWorkflow.java<p/>
* <b>Description: </b> 
* <p/>
* <b>Copyright:</b> Copyright (c) 2011<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author Dave Bargerhuff
* @version 1.0
* @since Apr 26, 2011
* <b>Changes: </b>
****************************************************************************/
public class SJMTrackerPublicWorkflow extends TrackerAction {
	
	public static final String DAILY_AVAILABILITY_FIELD_ID = "c0a802419eef92a7e01d18d8f424f76";

	public SJMTrackerPublicWorkflow() {
		super();
	}
	
	public SJMTrackerPublicWorkflow(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("SJMTrackerPublicWorkflow retrieve...");
		TrackerDataContainer tdc = new TrackerDataContainer();
		// 1. retrieve form
		tdc.setForm(retrieveForm(req, "patient"));
		
		// 2. retrieve "today's" ambassadors, place on container
		List<AssigneeVO> assignees = this.retrieveAmbassadors();
		tdc.setAssignees(assignees);
		
		// 3. put the container onto the module
		ModuleVO mod = (ModuleVO) req.getAttribute(Constants.MODULE_DATA);
		mod.setActionData(tdc);
		req.setAttribute(Constants.MODULE_DATA, mod);
	}
	
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("SJMTrackerPublicWorkflow build...");
		
		// 1. look up patient form actionId
		String formActionId = this.retrieveFormActionId(req, "patient");		
		req.setAttribute("formActionId", formActionId);
		log.debug("formActionId: " + formActionId);
		
		// 2. build patient base record and form data
		SMTActionInterface sai = null;
		sai = new PatientManager(this.actionInit);
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		sai.build(req);
		
		// retrieve newly created patient from request
		PatientVO patient = (PatientVO)req.getAttribute(PatientAction.PATIENT_DATA);
		
		// 3. determine who to assign
		chooseAmbassador(req, patient);
				
		// 4. make assignment
		sai = new SJMAssignmentFacade(this.actionInit);
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		sai.build(req);
		
		// 5. redirect
    	StringBuffer url = new StringBuffer();
    	PageVO page = (PageVO)req.getAttribute(Constants.PAGE_DATA);
    	
    	url.append(StringUtil.checkVal(attributes.get("contextPath")));
    	url.append(page.getFullPath()).append("?patientSubmitted=true");
    	req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
    	req.setAttribute(Constants.REDIRECT_URL, url.toString());
    	log.debug("SJMTrackerPublicWorkflow redirect URL: " + url);
	}
	
	/**
	 * Retrieves all ambassadors available today
	 * @return
	 */
	private List<AssigneeVO> retrieveAmbassadors() {
		GenericQueryVO cqv = buildTodaysQuery();
		AmbassadorRetriever sar = new AmbassadorRetriever();
		sar.setQuery(cqv);
		List<AssigneeVO> ambassadors = null;
		
		try {
			ambassadors = sar.retrieveAmbassadors();	
		} catch (SQLException sqle) {
			log.error("Error retrieving base records for ambassadors, ", sqle);
		}
		return ambassadors;
	}
	
	/**
	 * Determines the ambassador assigneeId to pass to the assignment manager
	 * @param req
	 * @param patient
	 * @return
	 */
	private void chooseAmbassador(ActionRequest req, PatientVO patient) {
		String assigneeId = StringUtil.checkVal(req.getParameter("assigneeId"));
		// if assigneeId does not exist on request, find an ambassador match
		if (assigneeId.length() == 0) {
			List<AssigneeVO> ambassadors = this.retrieveAmbassadors();
			// find a match
			AmbassadorMatcher am = new AmbassadorMatcher();
			am.setDbConn(dbConn);
			am.setPatient(patient);
			am.setAmbassadors(ambassadors);
			AssigneeVO avo = am.findAmbassadorMatch();
			// set the assigneeId on the request for use by assignment manager
			req.setParameter("assigneeId", avo.getAssigneeId(), true);
		}
	}
	
	/**
	 * Builds query object with a query param that specifies the ambassador availability field 
	 * and a value representing "today".  The "today" value is the short form of the name of the weekday
	 * @return
	 */
	private GenericQueryVO buildTodaysQuery() {
		Calendar cal = GregorianCalendar.getInstance();
		String today = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, new Locale("US"));
		log.debug("searching for ambassadors available 'today': " + today);
		GenericQueryVO g = new GenericQueryVO(AmbassadorRetriever.FORM_ID);
		g.setOrganizationId(AmbassadorRetriever.ORGANIZATION_ID);
		
		// set up param for filtering by today's name value
		QueryParamVO q1 = new QueryParamVO(DAILY_AVAILABILITY_FIELD_ID, false);
		q1.setOperator(Operator.like);
		q1.setValues(new String[] {today});
		g.addConditional(q1);
		return g;
	}

}
