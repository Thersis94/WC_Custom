package com.depuysynthes.srt.util;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.depuysynthes.srt.vo.SRTMilestoneRuleVO;
import com.depuysynthes.srt.vo.SRTMilestoneVO;
import com.depuysynthes.srt.vo.SRTProjectVO;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.data.vo.GenericQueryVO.Operator;

/****************************************************************************
 * <b>Title:</b> SRTMilestoneTest.java <b>Project:</b> WC_Custom
 * <b>Description:</b> Tests that MilestoneUtil is working as intended.
 * <b>Copyright:</b> Copyright (c) 2018 <b>Company:</b> Silicon Mountain
 * Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Mar 12, 2018
 ****************************************************************************/
public class SRTMilestoneTest {
	private SRTProjectVO p;
	private List<SRTMilestoneVO> milestones;


	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		buildProjectRecord();
		buildMilestoneVOs();
	}


	/**
	 * 
	 */
	private void buildProjectRecord() {
		p = new SRTProjectVO();
		p.setProjectId("Test 1");
		p.setCreateDt(Convert.getCurrentTimestamp());
		p.setEngineerId("user");
		p.setDesignerId("designer");
	}


	/**
	 * 
	 */
	private void buildMilestoneVOs() {
		milestones = new ArrayList<>();
		SRTMilestoneVO m = new SRTMilestoneVO();
		m.setMilestoneId("PROJECT_START");
		m.setOpCoId("US_SPINE");
		SRTMilestoneRuleVO rule1 = new SRTMilestoneRuleVO();
		rule1.setMilestoneRuleId("rule1");
		rule1.setFieldNm("createDt");
		rule1.setOperandType(Operator.notEmpty);
		m.addRule(rule1);
		milestones.add(m);

		SRTMilestoneVO e = new SRTMilestoneVO();
		e.setMilestoneId("ENGINEER_START");
		e.setOpCoId("US_SPINE");
		e.setParentId("PROJECT_START");
		SRTMilestoneRuleVO rule2 = new SRTMilestoneRuleVO();
		rule2.setMilestoneRuleId("rule2");
		rule2.setFieldNm("engineerId");
		rule2.setOperandType(Operator.notEmpty);
		e.addRule(rule2);
		milestones.add(e);

		SRTMilestoneVO d = new SRTMilestoneVO();
		d.setMilestoneId("DESIGNER_START");
		d.setOpCoId("US_SPINE");
		d.setParentId("ENGINEER_START");
		SRTMilestoneRuleVO rule3 = new SRTMilestoneRuleVO();
		rule3.setMilestoneRuleId("rule3");
		rule3.setFieldNm("designerId");
		rule3.setOperandType(Operator.notEmpty);
		d.addRule(rule3);
		milestones.add(e);

	}


	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}


	@Test
	public void testStart() {
		SRTMilestoneUtil.checkGates(p, milestones);
		assertTrue(p.getMilestone("PROJECT_START") != null);
	}


	@Test
	public void testEngineer() {
		SRTMilestoneUtil.checkGates(p, milestones);
		assertTrue(p.getMilestone("ENGINEER_START") != null);
	}


	@Test
	public void testDesigner() {
		SRTMilestoneUtil.checkGates(p, milestones);
		assertTrue(p.getMilestone("DESIGNER_START") == null);
	}
}