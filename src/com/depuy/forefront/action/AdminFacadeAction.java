package com.depuy.forefront.action;


import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBActionAdapter;

public class AdminFacadeAction extends SBActionAdapter {
	public final static int HOSPITAL_ACTION = 1;
	public final static int PROGRAM_ACTION = 2;
	public final static int HOSPITAL_INST_ACTION = 3;
	public final static int STAGE_ACTION = 4;
	public final static int EXERCISE_ACTION = 5;
	public final static int LIST_ITEM_ACTION = 6;
	public final static int ROUTINE_ACTION = 7;
	public final static int ACTION_PLAN_ACTION = 8;
	public final static int EXERCISE_ASSOC_ACTION = 9;
	public final static int ACTION_PLAN_ASSOC_ACTION = 10;
	public final static int TREATMENT_CALENDAR_ACTION = 11;
	public final static int TREATMENT_CALENDAR_ITEM_ACTION = 12;
	public final static int TREATMENT_CALENDAR_ITEM_ASSOC_ACTION = 13;
	
	//use the siteId of the parent site, so when it's cache is clear so is the entire system!
	public final static String CACHE_GROUP = "PILOT_1";
	

	public void delete(ActionRequest req) throws ActionException {
		loadAction(req).delete(req);
	}
	public void retrieve(ActionRequest req) throws ActionException {
		loadAction(req).retrieve(req);
	}
	
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	public void build(ActionRequest req) throws ActionException {
		loadAction(req).build(req);
		super.clearCacheByGroup(CACHE_GROUP);
	}
	
	private ActionInterface loadAction(ActionRequest req) {
		ActionInterface sb = null;
		int type = Convert.formatInteger(req.getParameter("actionType"));
		switch(type) {
			case HOSPITAL_INST_ACTION:
				//set the hospitalId onto session if it was passed.
				if (req.getParameter("setHospitalId") != null) {
					req.getSession().setAttribute(HospitalAction.HOSPITAL_INST_ID, req.getParameter(HospitalAction.HOSPITAL_INST_ID));
					req.getSession().setAttribute(HospitalAction.HOSPITAL_ID, req.getParameter("setHospitalId"));
					req.getSession().setAttribute(HospitalAction.HOSPITAL_NM, req.getParameter(HospitalAction.HOSPITAL_NM));
				}
				
				sb = new HospitalInstanceAction(this.actionInit);
				break;
			case HOSPITAL_ACTION:
				sb = new HospitalAction(this.actionInit);
				break;
			case PROGRAM_ACTION:
				sb = new ProgramAction(this.actionInit);
				break;
			case STAGE_ACTION:
				sb = new StageAction(this.actionInit);
				break;
			case EXERCISE_ACTION:
				sb = new ExerciseAction(this.actionInit);
				break;
			case EXERCISE_ASSOC_ACTION:
				sb = new ExerciseAssocAction(this.actionInit);
				break;
			case LIST_ITEM_ACTION:
				sb = new ListAction(this.actionInit);
				break;
			case ROUTINE_ACTION:
				sb = new RoutineAction(this.actionInit);
				break;
			case ACTION_PLAN_ACTION:
				sb = new ActionPlanAction(this.actionInit);
				break;
			case ACTION_PLAN_ASSOC_ACTION:
				sb = new ActionPlanAssocAction(this.actionInit);
				break;
			case TREATMENT_CALENDAR_ACTION:
				sb = new TreatCalAction(this.actionInit);
				break;
			case TREATMENT_CALENDAR_ITEM_ACTION:
				sb = new TreatCalItemAction(this.actionInit);
				break;
			case TREATMENT_CALENDAR_ITEM_ASSOC_ACTION:
				sb = new TreatCalAssocAction(this.actionInit);
				break;
			default:
				//if program not selected default to program action, otherwise default to HospInstAction
				String progId = (String) req.getSession().getAttribute(ProgramAction.PROGRAM_ID);
				
				//set the programId onto session if it was passed.
				if (req.getParameter("setProgramId") != null) {
					progId = req.getParameter("setProgramId");
					req.getSession().setAttribute(ProgramAction.PROGRAM_ID, progId);
					req.getSession().setAttribute(ProgramAction.PROGRAM_NM, req.getParameter(ProgramAction.PROGRAM_NM));
				}
				
				if (progId == null || progId.length() == 0) {
					sb = new ProgramAction(this.actionInit);
				} else {
					sb = new HospitalInstanceAction(this.actionInit);
				}
		}
		log.debug("loaded action: " + sb.getClass().getName());
		sb.setAttributes(attributes);
		sb.setDBConnection(dbConn);
		return sb;
	}

}
