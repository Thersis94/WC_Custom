package com.depuysynthes.srt.util;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.depuysynthes.srt.vo.SRTProjectMilestoneVO;
import com.depuysynthes.srt.vo.SRTProjectVO;
import com.siliconmtn.common.constants.Operator;
import com.siliconmtn.util.Convert;
import com.siliconmtn.workflow.milestones.MilestoneIntfc;
import com.siliconmtn.workflow.milestones.MilestoneRuleVO;
import com.siliconmtn.workflow.milestones.MilestoneUtil;

/****************************************************************************
 * <b>Title:</b> SRTMilestoneTest.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Tests that MilestoneUtil is working as intended.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Mar 12, 2018
 ****************************************************************************/
public class MilestoneTest {
	private MilestoneIntfc<SRTProjectMilestoneVO> target;
	private List<SRTProjectMilestoneVO> milestones;
	private MilestoneUtil<SRTProjectMilestoneVO> util;

	private static final String PROJECT_START = "PROJECT_START";
	private static final String ENGINEER_START = "ENGINEER_START";
	private static final String OP_CO = "US_SPINE";
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		util = new MilestoneUtil<>();
		buildProjectRecord();
		buildMilestoneVOs();
	}

	/**
	 * 
	 */
	private void buildProjectRecord() {
		SRTProjectVO p = new SRTProjectVO();
		p.setProjectId("Test 1");
		p.setCreateDt(Convert.getCurrentTimestamp());
		p.addLedgerDate("projectStartDt", Convert.getCurrentTimestamp());
		p.setEngineerId("user");
		p.setDesignerId("designer");
		target = p;
	}

	/**
	 * 
	 */
	private void buildMilestoneVOs() {
		milestones = new ArrayList<>();
		SRTProjectMilestoneVO m = new SRTProjectMilestoneVO();
		m.setMilestoneId(PROJECT_START);
		m.setOrganizationId(OP_CO);
		MilestoneRuleVO rule1 = new MilestoneRuleVO();
		rule1.setMilestoneRuleId("rule1");
		rule1.setFieldNm("projectStartDt");
		rule1.setOperandType(Operator.NOT_EMPTY);
		m.addRule(rule1);
		milestones.add(m);

		SRTProjectMilestoneVO e = new SRTProjectMilestoneVO();
		e.setMilestoneId(ENGINEER_START);
		e.setOrganizationId(OP_CO);
		e.setParentId(PROJECT_START);
		MilestoneRuleVO rule2 = new MilestoneRuleVO();
		rule2.setMilestoneRuleId("rule2");
		rule2.setFieldNm("engineerId");
		rule2.setOperandType(Operator.NOT_EMPTY);
		e.addRule(rule2);
		milestones.add(e);

		SRTProjectMilestoneVO d = new SRTProjectMilestoneVO();
		d.setMilestoneId("DESIGNER_START");
		d.setOrganizationId(OP_CO);
		d.setParentId(ENGINEER_START);
		MilestoneRuleVO rule3 = new MilestoneRuleVO();
		rule3.setMilestoneRuleId("rule3");
		rule3.setFieldNm("designerId");
		rule3.setOperandType(Operator.NOT_EMPTY);
		d.addRule(rule3);
		milestones.add(d);
	}

	@Test
	public void testStart() {
		util.checkGates(target, milestones);
		assertTrue(target.getMilestone(PROJECT_START) != null);
	}

	@Test
	public void testEngineer() {
		util.checkGates(target, milestones);
		assertTrue(target.getMilestone(ENGINEER_START) != null);
	}

	@Test
	public void testDesigner() {
		util.checkGates(target, milestones);
		assertTrue(target.getMilestone("DESIGNER_START") != null);
	}
}