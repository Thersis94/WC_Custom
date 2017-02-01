package com.biomed.smarttrak;

import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: FinancialDashScenarioAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Jan 24, 2017
 ****************************************************************************/

public class FinancialDashScenarioAction extends SBActionAdapter {

	public FinancialDashScenarioAction() {
		super();
	}

	public FinancialDashScenarioAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	public void delete(ActionRequest req) throws ActionException {
		super.delete(req);
	}
	
	public void retrieve(ActionRequest req) throws ActionException {
		super.retrieve(req);

		List<FinancialDashScenarioVO> scenarios = new ArrayList<>();
		for (int i=0; i < 5; i++) {
			FinancialDashScenarioVO scenario = new FinancialDashScenarioVO();
			scenario.setTempData();
			scenarios.add(scenario);
		}
		
		this.putModuleData(scenarios);
	}
	
	public void build(ActionRequest req) throws ActionException {
		super.build(req);
		String scenarioName = StringUtil.checkVal(req.getParameter("scenarioName"));
		String scenarioRole = StringUtil.checkVal(req.getParameter("scenarioRole"));
		String updateType = StringUtil.checkVal(req.getParameter("type")); 

		log.debug("Editing Scenario: Name - " + scenarioName + " | Role - " + scenarioRole + " | Update Type: " + updateType);
	}
	
	public void list(ActionRequest req) throws ActionException {
		super.list(req);
	}

	public void update(ActionRequest req) throws ActionException {
		super.update(req);
	}
}
