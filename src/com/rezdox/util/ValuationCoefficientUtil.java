package com.rezdox.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: AppUtil.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Utility class to support returned the correct valuation coefficient
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Mar 6, 2020
 * @updates:
 ****************************************************************************/
public class ValuationCoefficientUtil {
	
	private static Map<Integer, Double> yearMapping = new HashMap<>();
	private static Date startDay;
	private static Date fourYearDay;
	private static Date twentyYearDay;
	
	/**
	 * The coefficients used based on how old a project is to calculate 
	 */
	public enum ValuationCoefficient {
		YEAR_2020(0.611), YEAR_2019(0.572), LESS_THEN_FOUR_YEARS(0.537), 
		MORE_THEN_FOUR_LESS_THEN_TWENTY(0.147), MORE_THEN_TWENTY(0.00) ;

		private double coefficient;
		private ValuationCoefficient(double coefficient) { this.coefficient = coefficient; }
		public double getCoefficient() {return coefficient; }
	}
	
	
	
	/**
	 * included a main method to test that the right coefficient is returned
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Curr Year: " + getValueCoefficient(Convert.formatDate("2020-03-06")));
		System.out.println("Last Year: " + getValueCoefficient(Convert.formatDate("2019-03-06")));
		System.out.println("2018: " + getValueCoefficient(Convert.formatDate("2018-03-06")));
		System.out.println("6 Years: " + getValueCoefficient(Convert.formatDate("2014-03-06")));
		System.out.println("20 Year: " + getValueCoefficient(Convert.formatDate("1990-03-06")));
		
	}
	
	private static void loadMappings() {
		Calendar c = new GregorianCalendar();
		yearMapping.put(c.get(Calendar.YEAR), ValuationCoefficient.YEAR_2020.getCoefficient());
		yearMapping.put(c.get(Calendar.YEAR) - 1, ValuationCoefficient.YEAR_2019.getCoefficient());
		
		startDay = new Date();
		fourYearDay = Convert.formatDate(startDay, Calendar.YEAR, -4);
		twentyYearDay = Convert.formatDate(startDay, Calendar.YEAR, -20);
	}
	
	
	
	public static double getValueCoefficient(Date projectDate) {
		if (yearMapping.isEmpty() || Convert.formatStartDate(startDay).after(Convert.formatStartDate(new Date()))) {
			loadMappings();
		}
		
		Calendar cal = new GregorianCalendar();
		cal.setTime(projectDate);
		int year = cal.get(Calendar.YEAR);
		
		if (yearMapping.containsKey(year)) return yearMapping.get(year);
		if (twentyYearDay.after(projectDate)) return ValuationCoefficient.MORE_THEN_TWENTY.getCoefficient();
		if (fourYearDay.after(projectDate)) return ValuationCoefficient.MORE_THEN_FOUR_LESS_THEN_TWENTY.getCoefficient();
		
		return ValuationCoefficient.LESS_THEN_FOUR_YEARS.getCoefficient();
	}

}
