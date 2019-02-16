package com.perfectstorm.action.weather;
//java 1.8
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
//WC custom
import com.perfectstorm.data.weather.SunTimeVO;
//SMT baselibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.exception.InvalidDataException;
//
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: SunTimeCalculatorAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Calculates the apparent sun rise and sun set based on date and
 * the lat long sent in via request.  
 * 
 * West longitudes should be a negative number.  
 * and example url string would be 
 * /json?json=true&gson=true&amid=sunTime&longitudeNumber=-105.2705&latitudeNumber=40.0150&calcDate=2019-02-15&timeZoneName=America%2FDenver
 * 
 * if no timezone is supplied the class vo will pick the default timezone of the vm,
 * if no date is supplied the class will set the calc date to 
 * 
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Feb 14, 2019
 * @updates:
 ****************************************************************************/
public class SunTimeCalculatorAction extends SBActionAdapter {
	
	 /**
	  * The altitude of the sun (solar elevation angle) at the moment of sunrise or sunset: -0.833 
	  */ 
	 public static final double SUN_ALTITUDE_SUNRISE_SUNSET = -0.833; 
	 
	 /**
	  * The altitude of the sun (solar elevation angle) at the moment of civil twilight: -6.0 
	  */ 
	 public static final double SUN_ALTITUDE_CIVIL_TWILIGHT = -6.0; 
	 
	 /**
	  * The altitude of the sun (solar elevation angle) at the moment of nautical twilight: -12.0 
	  */ 
	 public static final double SUN_ALTITUDE_NAUTICAL_TWILIGHT = -12.0; 
	 
	 /**
	  * The altitude of the sun (solar elevation angle) at the moment of astronomical twilight: -18.0 
	  */ 
	 public static final double SUN_ALTITUDE_ASTRONOMICAL_TWILIGHT = -18.0; 
	 
	 private static final int JULIAN_DATE_2000_01_01 = 2451545; 
	 private static final double CONST_0009 = 0.0009; 
	 private static final double CONST_360 = 360; 

	/**
	 * 
	 */
	public SunTimeCalculatorAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public SunTimeCalculatorAction(ActionInitVO actionInit) {
		super(actionInit);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("Sun time calculate retrieve called"); 

		try {
			
			if (!req.hasParameter("longitudeNumber") || !req.hasParameter("latitudeNumber")) {
				throw new InvalidDataException("Latitude and Longitude are required for calculations ");
			}
			SunTimeVO stvo = new SunTimeVO(req);
			stvo.setSourceDate(req.getDateParameter("calcDate", new Date()));
			stvo.setTimeZoneName(req.getStringParameter("timeZoneName", TimeZone.getDefault().getID()));
		
			putModuleData(calculateSunTimes(stvo));
		} catch (InvalidDataException e) {
			putModuleData(null, 0, false, e.getLocalizedMessage(), true);
			log.error("could not calculate sun times ",e);
		}
		
	}
	

	/**
	  * takes the day month and year of the date supplied and uses the long lat to calculate sun rise sun set
	 * @param calcDate
	 * @param longitude
	 * @param latitude
	 * @return
	 * @throws InvalidDataException 
	 */
	private SunTimeVO calculateSunTimes(SunTimeVO stvo) throws InvalidDataException {
		
		Calendar cal = new GregorianCalendar();
		cal.setTime(stvo.getSourceDate());
		
		double latitudeRad = Math.toRadians(stvo.getLatitudeNumber()); 
		//invert the sign of the long no
		stvo.setLongitudeNumber(-stvo.getLongitudeNumber());
		 
		double julianDate = getJulianDate(cal); 
		double nstar = julianDate - JULIAN_DATE_2000_01_01 - CONST_0009 -stvo.getLongitudeNumber()/ CONST_360; 
		double n = Math.round(nstar); 
		double jstar = JULIAN_DATE_2000_01_01 + CONST_0009 +stvo.getLongitudeNumber()/ CONST_360 + n; 
		double m = Math.toRadians((357.5291 + 0.98560028 * (jstar - JULIAN_DATE_2000_01_01)) % CONST_360); 
		double c = 1.9148 * Math.sin(m) + 0.0200 * Math.sin(2 * m) + 0.0003 * Math.sin(3 * m); 
		double lambda = Math.toRadians((Math.toDegrees(m) + 102.9372 + c + 180) % CONST_360); 
		double jtransit = jstar + 0.0053 * Math.sin(m) - 0.0069 * Math.sin(2 * lambda); 
		double delta = Math.asin(Math.sin(lambda) * Math.sin(Math.toRadians(23.439))); 
		double omega = Math.acos((Math.sin(Math.toRadians(SUN_ALTITUDE_SUNRISE_SUNSET)) - Math 
		    .sin(latitudeRad) * Math.sin(delta)) / (Math.cos(latitudeRad) * Math.cos(delta))); 
		//makesure Omega is a number
		if (Double.isNaN(omega)) { 
			throw new InvalidDataException("Calculated Omage Value is not a valid number ");
		} 
		// Sunset 
		double jset = JULIAN_DATE_2000_01_01 + CONST_0009 + ((Math.toDegrees(omega) + stvo.getLongitudeNumber()) / CONST_360 + n + 0.0053 
		    * Math.sin(m) - 0.0069 * Math.sin(2 * lambda)); 
		// Sunrise 
		double jrise = jtransit - (jset - jtransit); 
		// Convert sunset and sunrise to Gregorian dates, in UTC 
		Date sRiseLocal = getGregorianDate(jrise, stvo.getTimeZoneName()); 
		Date sSetLocal = getGregorianDate(jset, stvo.getTimeZoneName()); 
		
		stvo.setSunriseDate(sRiseLocal);
		stvo.setSunsetDate(sSetLocal);
		
		//put return the long no to its starting sign so the vo is representing what was submitted
		stvo.setLongitudeNumber(-stvo.getLongitudeNumber());
		
		return stvo;
	}

	/**
	 * converts the date from gregoria to Julian because astromonical dates are 
	 * recored in Julian calender
	 * @param gregorianDate
	 * @return
	 */
	private static double getJulianDate( Calendar gregorianDate) { 
		
		  TimeZone tzUTC = TimeZone.getTimeZone("UTC"); 
		  Calendar gregorianDateUTC = Calendar.getInstance(tzUTC); 
		  gregorianDateUTC.setTimeInMillis(gregorianDate.getTimeInMillis()); 
		  // For the year (Y) astronomical year numbering is used, thus 1 BC is 0, 
		  // 2 BC is -1, and 4713 BC is -4712. 
		  int year = gregorianDateUTC.get(Calendar.YEAR); 
		  int month = gregorianDateUTC.get(Calendar.MONTH) + 1; 
		  int day = gregorianDateUTC.get(Calendar.DAY_OF_MONTH); 
		  int a = (14 - month) / 12; 
		  int y = year + 4800 - a; 
		  int m = month + 12 * a - 3; 
		 
		  int julianDay = day + (153 * m + 2) / 5 + 365 * y + (y / 4) - (y / 100) 
		    + (y / 400) - 32045; 
		  int hour = gregorianDateUTC.get(Calendar.HOUR_OF_DAY); 
		  int minute = gregorianDateUTC.get(Calendar.MINUTE); 
		  int second = gregorianDateUTC.get(Calendar.SECOND); 
		 
		  return julianDay + ((double) hour - 12) / 24 
		    + ((double) minute) / 1440 + ((double) second) / 86400; 
		 } 
	/**
	 * returns the dates back to the more modern gregorian calender
	 * @param julianDate
	 * @return
	 */
	 private static Date getGregorianDate(double julianDate, String timeZoneName) {
		 
		 final int DAYS_PER_4000_YEARS = 146097; 
		 final int DAYS_PER_CENTURY = 36524; 
		 final int DAYS_PER_4_YEARS = 1461; 
		 final int DAYS_PER_5_MONTHS = 153; 
		 
		 int jj = (int) (julianDate + 0.5); 
		 int j = jj + 32044;  
		 int g = j / DAYS_PER_4000_YEARS; 
		 int dg = j % DAYS_PER_4000_YEARS; 
		 int c = ((dg / DAYS_PER_CENTURY + 1) * 3) / 4; 
		 int dc = dg - c * DAYS_PER_CENTURY; 
		 int b = dc / DAYS_PER_4_YEARS; 
		 int db = dc % DAYS_PER_4_YEARS; 
		 int a = ((db / 365 + 1) * 3) / 4; 
		 int da = db - a * 365; 
		 int y = g * 400 + c * 100 + b * 4 + a; 
		 int m = (da * 5 + 308) / DAYS_PER_5_MONTHS - 2; 
		 int d = da - ((m + 4) * DAYS_PER_5_MONTHS) / 5 + 122; 
		 int year = y - 4800 + (m + 2) / 12; 
		 int month = (m + 2) % 12; 
		 int day = d + 1; 
		 // Apply the fraction of the day in the Julian date to the Gregorian date. 
		 double dayFraction = (julianDate + 0.5) - jj; 
		 int hours = (int) (dayFraction * 24); 
		 int minutes = (int) ((dayFraction * 24 - hours) * 60d); 
		 int seconds = (int) ((dayFraction * 24 * 3600 - (hours * 3600 + minutes * 60)) + .5); 
		 // Create the gregorian date in UTC. 
		 Calendar gregorianDateUTC = Calendar.getInstance(TimeZone.getTimeZone("UTC")); 
		  
		 gregorianDateUTC.set(Calendar.YEAR, year); 
		 gregorianDateUTC.set(Calendar.MONTH, month); 
		 gregorianDateUTC.set(Calendar.DAY_OF_MONTH, day); 
		 gregorianDateUTC.set(Calendar.HOUR_OF_DAY, hours); 
		 gregorianDateUTC.set(Calendar.MINUTE, minutes); 
		 gregorianDateUTC.set(Calendar.SECOND, seconds); 
		 gregorianDateUTC.set(Calendar.MILLISECOND, 0); 
		 
		 // Convert to a Gregorian date in the local time zone. 
		 Calendar gregorianDate = Calendar.getInstance(TimeZone.getTimeZone("MST")); 
		 gregorianDate.setTimeInMillis(gregorianDateUTC.getTimeInMillis()); 
		 //convert the date time to the correct timezone
		 TimeZone tz = TimeZone.getTimeZone(timeZoneName);
		 long offset= tz.getOffset(new Date().getTime()) / 3600000;
		  
		 long number = gregorianDateUTC.getTime().getTime() + offset;

		 return new Date(number); 
	 } 
}
