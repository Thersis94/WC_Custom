package com.depuysynthes.srt.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.log4j.Logger;

import com.depuysynthes.srt.vo.SRTMilestoneRuleVO;
import com.depuysynthes.srt.vo.SRTMilestoneVO;
import com.depuysynthes.srt.vo.SRTProjectMilestoneVO;
import com.depuysynthes.srt.vo.SRTProjectVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.data.vo.GenericQueryVO.Operator;

/****************************************************************************
 * <b>Title:</b> SRTMilestoneUtil.java <b>Project:</b> WC_Custom
 * <b>Description:</b> Performs Milestone checks. <b>Copyright:</b> Copyright
 * (c) 2018 <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Mar 12, 2018
 ****************************************************************************/
public class SRTMilestoneUtil {
	private static Logger log;
	static {
		log = Logger.getLogger(SRTMilestoneUtil.class);
	}


	private SRTMilestoneUtil() {
		// Hide default constructor
	}


	/**
	 * Iterate Milestones and add new Milestones if all rules are passed.
	 *
	 * @param project
	 * @param milestones
	 */
	public static void checkGates(SRTProjectVO project, List<SRTMilestoneVO> milestones) {
		for (SRTMilestoneVO milestone : milestones) {
			// If all Rules have been passed, add the milestone to the Project.
			if (processRules(project, milestone.getRules()) && checkParent(project, milestone)) {
				project.addMilestone(new SRTProjectMilestoneVO(milestone.getMilestoneId(), project.getProjectId()));
			}
		}
	}


	/**
	 * If the Milestone has a required Parent, verify it's on the Project.
	 *
	 * @param project
	 * @param milestone
	 * @return
	 */
	private static boolean checkParent(SRTProjectVO project, SRTMilestoneVO milestone) {
		boolean passed = true;
		if (!StringUtil.isEmpty(milestone.getParentId()) && !project.getMilestones().containsKey(milestone.getParentId())) {
			passed = false;
		}
		return passed;
	}


	/**
	 * Iterate the Milestone Rules and verify that they all pass.
	 * @param project
	 * @param rule
	 */
	private static boolean processRules(SRTProjectVO project, List<SRTMilestoneRuleVO> rules) {
		boolean passed = true;

		// Iterate all rules.
		for (SRTMilestoneRuleVO rule : rules) {

			/*
			 * If passed is false, break rules processing. Else passed is true
			 * if checkRule is also true.
			 */
			if (!passed) {
				break;
			} else {
				passed = passed && checkRule(project, rule);
			}
		}

		return passed;
	}


	/**
	 * Check given rule against the passed Project.
	 * 
	 * @param project
	 * @param rule
	 * @return
	 */
	private static <T extends Comparable<T>> boolean checkRule(SRTProjectVO project, SRTMilestoneRuleVO rule) {
		T fieldVal = null;
		try {

			// Retrieve the fieldValue off the SRTPRoject Record for the given
			// fieldNm.
			fieldVal = mapProjectVal(project, rule.getFieldNm());

			// Default compareVal to null
			T compareVal = null;

			/*
			 * If we got a fieldValue back, get a compare Value of the same type
			 * with the rules field Value. Note: This works assuming a String
			 * Constructor on whatever fieldVal Object type is returned.
			 */
			if (fieldVal != null) {
				compareVal = mapCompareVal(rule.getFieldVal(), fieldVal.getClass());
			}

			// Perform comparison on fieldVal and compareVal using provided
			// operandType.
			return compare(fieldVal, compareVal, rule.getOperandType());
		} catch (NoSuchFieldException e) {
			log.error(StringUtil.join(rule.getFieldNm(), " is not available on Project Record."));
		} catch (IllegalArgumentException | NoSuchMethodException	| InstantiationException e) {
			if (fieldVal != null) {
				log.error(StringUtil.join("Could not create a comparison Object for fieldType: ", fieldVal.getClass().getName()), e);
			} else {
				log.error(StringUtil.join("Could not create a comparison Object"), e);
			}
		}

		return false;
	}


	/**
	 * Perform actual Comparison of fieldValue vs compareValue according
	 * to the passed operandType.
	 * @param fieldVal - Value from the Project Record.
	 * @param compareVal - Value from the Rule Record.  Null if empty.
	 * @param operandType - Comparison Operation from Rule Record.
	 * @return
	 */
	private static <T extends Comparable<T>> boolean compare(T fieldVal, T compareVal, Operator operandType) {
		boolean passed = false;
		switch (operandType) {
			case equals:
				passed = fieldVal.equals(compareVal);
				break;
			case notEquals:
				passed = !fieldVal.equals(compareVal);
				break;
			case notEmpty:
				passed = !StringUtil.isEmpty(StringUtil.checkVal(fieldVal));
				break;
			case greaterThan:
				passed = fieldVal.compareTo(compareVal) > 0;
				break;
			case lessThan:
				break;
			case like:
			case between:
			case in:
			default:
				log.warn(StringUtil.join(operandType.toString(), " Not Supported "));
				break;
		}

		return passed;
	}


	/**
	 * Generate a compareValue with the given fieldVal and same type as passed
	 * compareClass.
	 *
	 * @param fieldVal - FieldValue we want to compare against from Rule Record.
	 * @param compareClass - The Class type of the Testing Objects field we want to compare against. 
	 * @return
	 * @throws InstantiationException
	 * @throws NoSuchMethodException
	 */
	@SuppressWarnings("unchecked")
	private static <T extends Comparable<T>> T mapCompareVal(String fieldVal, Class<?> compareClass) throws NoSuchMethodException, InstantiationException {
		T compareVal = null;

		//If we are working with nulls on either side, return.
		if (fieldVal != null && compareClass != null) {

			//Attempt to get a constructor for the comparisonClass that takes a String Argument.
			Constructor<?> con = compareClass.getConstructor(String.class);
			try {

				//Attempt to instantiate an object.
				Object v = con.newInstance(fieldVal);

				/*
				 * If it is of comparable Type, we can work with this.
				 * Else throw Exception.
				 */
				if (Comparable.class.isAssignableFrom(compareClass.getClass())) {
					compareVal = (T) v;
				} else {
					throw new ClassCastException("Field not of type comparable.");
				}
			} catch (IllegalAccessException | InvocationTargetException e) {
				log.error("Error Generating comparison Object", e);
			}
		}

		return compareVal;
	}


	/**
	 * Retrieve value of fieldNm off Passed Object
	 * 
	 * @param lookupObject - The Object containing the fieldNm we want to retrieve.
	 * @param fieldNm - the Field we want.
	 * @return
	 * @throws NoSuchFieldException
	 */
	@SuppressWarnings("unchecked")
	private static <T extends Comparable<T>> T mapProjectVal(Object lookupObject, String fieldNm) throws NoSuchFieldException {
		T value = null;

		//If lookupObject is null, we can't work with this.
		if(lookupObject != null) {
			try {

				//Get the ClassNm of the LookupObject
				Class<?> c = lookupObject.getClass();

				//Attempt to find a field with given fieldNm on the Class.
				Field f = c.getDeclaredField(fieldNm);

				//Set accessible to true so we can read it.
				f.setAccessible(true);

				//Retrieve Value off Object. (Needs Accessible true)
				Object v = f.get(lookupObject);

				/*
				 * If it is of comparable Type, we can work with this.
				 * Else throw Exception.
				 */
				if (Comparable.class.isAssignableFrom(f.getType())) {
					value = (T) v;
				} else {
					throw new ClassCastException("Field not of type comparable.");
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				log.error("could not process fieldNm: " + fieldNm, e);
			}
		}
		return value;
	}
}