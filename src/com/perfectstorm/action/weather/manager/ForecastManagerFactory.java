package com.perfectstorm.action.weather.manager;

import java.lang.reflect.Constructor;

import com.siliconmtn.action.ActionException;

/****************************************************************************
 * <b>Title:</b> ForecastManagerFactory.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Factory that returns a weather forecast manager.
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Feb 14 2019
 * @updates:
 ****************************************************************************/
public class ForecastManagerFactory {

	public enum ForecastManager {
		NWS_EXTENDED(NWSExtendedForecastManager.class),
		NWS_DETAILED(NWSDetailedForecastManager.class);
		
		private Class<? extends ForecastManagerInterface> cls;
		
		private ForecastManager(Class<? extends ForecastManagerInterface> cls) {
			this.cls = cls;
		}
		
		public Class<? extends ForecastManagerInterface> getCls() {
			return this.cls;
		}
	}
	
	private ForecastManagerFactory() {
		// Intentionally private
	}

	/**
	 * Gets the specified weather forecast manager.
	 * 
	 * @param manager
	 * @param latitude
	 * @param longitude
	 * @return
	 * @throws ActionException 
	 */
	public static ForecastManagerInterface getManager(ForecastManager manager, double latitude, double longitude) throws ActionException {
		// Get the specified forecast manager
		ForecastManagerInterface fmi;
		try {
			Class<?> cls = manager.getCls();
			Constructor<?> constructor = cls.getConstructor();
			fmi = (ForecastManagerInterface) constructor.newInstance();
		} catch (Exception e) {
			throw new ActionException("Could not instantiate forecast manager class.", e);
		}
		
		// Set the coordinates on the forecast manager
		fmi.setCoordinates(latitude, longitude);

		return fmi;
	}
}
