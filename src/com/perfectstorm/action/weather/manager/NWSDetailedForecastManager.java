package com.perfectstorm.action.weather.manager;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

// Gson 2.3
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.perfectstorm.action.weather.VenueForecastManager;
import com.perfectstorm.data.weather.WeatherStationVO;
import com.perfectstorm.data.weather.forecast.ForecastVO;
import com.perfectstorm.data.weather.nws.TimeValueVO;
import com.perfectstorm.data.weather.nws.detail.ForecastDetailVO;
import com.perfectstorm.data.weather.nws.detail.WeatherAttribueVO;
import com.perfectstorm.data.weather.nws.detail.WeatherDetailVO;
import com.perfectstorm.data.weather.nws.detail.WeatherPointVO;
import com.siliconmtn.action.ActionException;

// SMT Base Libs
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: NWSDetailedForecastManager.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the calls to the Weather.gov detail
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 12, 2019
 * @updates:
 ****************************************************************************/

public class NWSDetailedForecastManager implements ForecastManagerInterface {
	private static Logger log = Logger.getLogger(NWSDetailedForecastManager.class);
	private static final String NWS_POINT_URL = "https://api.weather.gov/points/%f,%f";
	private static final String NWS_GRID_DATA_URL = "https://api.weather.gov/gridpoints/%s/%d,%d";
	
	private static final String GET_TEMPERATURE = "getTemperature";
	private static final String GET_WIND = "getWind";
	private static final String GET_PRECIPITATION = "getPrecipitation";
	private static final String GET_HAZARD = "getHazard";
	private static final String GET_CONDITION = "getCondition";

	private double latitude;
	private double longitude;
	private WeatherStationVO station = null;

	public NWSDetailedForecastManager() {
		super();
	}

	/**
	 * Constructs a detailed forecast manager with the required coordinates.
	 * 
	 * @param latitude
	 * @param longitude
	 */
	public NWSDetailedForecastManager(double latitude, double longitude) {
		this();
		this.latitude = latitude;
		this.longitude = longitude;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure();

		NWSDetailedForecastManager nws = new NWSDetailedForecastManager();
		nws.setCoordinates(39.8595789, -104.9447375);
		nws.retrieveForecast();
	}
	
	/* (non-Javadoc)
	 * @see com.perfectstorm.action.weather.manager.ForecastManagerInterface#retrieveForecast()
	 */
	public Map<String, ForecastVO> retrieveForecast() throws ActionException {
		Map<String, ForecastVO> forecast;
		
		try {
			WeatherDetailVO detail = getForecast();
			forecast = processData(detail);
		} catch (IOException e) {
			throw new ActionException(e);
		}
		
		return forecast;
	}

	/**
	 * Processes the NWS data into our normalized beans.
	 * 
	 * @param detail
	 * @return
	 * @throws ActionException 
	 */
	private Map<String, ForecastVO> processData(WeatherDetailVO detail) throws ActionException {
		Map<String, ForecastVO> forecast = createForecastSkeleton(detail.getProperties().getValidTimes());
		
		// Map the NWS data to our normalized beans.
		ForecastDetailVO nwsForecast = detail.getProperties();
		
		// Populate data into the beans
		populateData(nwsForecast.getTemperature(), forecast, GET_TEMPERATURE, "setTemperature", double.class);
		populateData(nwsForecast.getDewpoint(), forecast, GET_TEMPERATURE, "setDewPoint", double.class);
		populateData(nwsForecast.getMaxTemperature(), forecast, GET_TEMPERATURE, "setMaxTemperature", double.class);
		populateData(nwsForecast.getMinTemperature(), forecast, GET_TEMPERATURE, "setMinTemperature", double.class);
		populateData(nwsForecast.getRelativeHumidity(), forecast, GET_PRECIPITATION, "setRelativeHumidity", int.class);
		populateData(nwsForecast.getApparentTemperature(), forecast, GET_TEMPERATURE, "setApparentTemperature", double.class);
		populateData(nwsForecast.getHeatIndex(), forecast, GET_TEMPERATURE, "setHeatIndex", double.class);
		populateData(nwsForecast.getWindChill(), forecast, GET_TEMPERATURE, "setWindChill", double.class);
		populateData(nwsForecast.getSkyCover(), forecast, GET_CONDITION, "setSkyCover", int.class);
		populateData(nwsForecast.getWindDirection(), forecast, GET_WIND, "setDirection", int.class);
		populateData(nwsForecast.getWindSpeed(), forecast, GET_WIND, "setSpeed", double.class);
		populateData(nwsForecast.getWindGust(), forecast, GET_WIND, "setGustSpeed", double.class);
		populateData(nwsForecast.getProbabilityOfPrecipitation(), forecast, GET_PRECIPITATION, "setProbability", int.class);
		populateData(nwsForecast.getQuantitativePrecipitation(), forecast, GET_PRECIPITATION, "setQuantity", double.class);
		populateData(nwsForecast.getIceAccumulation(), forecast, GET_PRECIPITATION, "setIceAccumulation", double.class);
		populateData(nwsForecast.getSnowfallAmount(), forecast, GET_PRECIPITATION, "setSnowFall", double.class);
		populateData(nwsForecast.getSnowLevel(), forecast, GET_PRECIPITATION, "setSnowLevel", double.class);
		populateData(nwsForecast.getCeilingHeight(), forecast, GET_CONDITION, "setCeilingHeight", double.class);
		populateData(nwsForecast.getVisibility(), forecast, GET_CONDITION, "setVisibility", double.class);
		populateData(nwsForecast.getTransportWindSpeed(), forecast, GET_WIND, "setTransportSpeed", double.class);
		populateData(nwsForecast.getTransportWindDirection(), forecast, GET_WIND, "setTransportDirection", int.class);
		populateData(nwsForecast.getMixingHeight(), forecast, GET_HAZARD, "setMixingHeight", double.class);
		populateData(nwsForecast.getTwentyFootWindSpeed(), forecast, GET_WIND, "setTwentyFootSpeed", double.class);
		populateData(nwsForecast.getTwentyFootWindDirection(), forecast, GET_WIND, "setTwentyFootDirection", int.class);
		populateData(nwsForecast.getProbabilityOfTropicalStormWinds(), forecast, GET_WIND, "setTropicalStormWindProbability", int.class);
		populateData(nwsForecast.getProbabilityOfHurricaneWinds(), forecast, GET_WIND, "setHurricaneStormWindProbability", int.class);
		populateData(nwsForecast.getLightningActivityLevel(), forecast, GET_CONDITION, "setLightningActivityLevel", int.class);
		populateData(nwsForecast.getProbabilityOfThunder(), forecast, GET_CONDITION, "setThunderProbability", int.class);
		
		return forecast;
	}
	
	/**
	 * Populate data into the associated VOs/methods
	 * 
	 * @param data
	 * @param forecast
	 * @param elementGetMethod
	 * @param dataSetMethod
	 * @param dataSetType
	 * @throws ActionException
	 */
	private void populateData(WeatherAttribueVO data, Map<String, ForecastVO> forecast, String elementGetMethod, String dataSetMethod, Class<?> dataSetType) throws ActionException {
		String unitOfMeasure = "";
		if (!StringUtil.isEmpty(data.getUom())) {
			String[] uom = data.getUom().split(":");
			unitOfMeasure = uom.length > 1 ? uom[1] : "";
		}
		
		for (TimeValueVO value : data.getValues()) {
			for (int durHour = 0; durHour < value.getDuration(); durHour++) {
				LocalDateTime date = value.getUtcDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
				int monthLen = date.toLocalDate().lengthOfMonth();
				
				// Calculate values for the map key
				int hour = (date.getHour() + durHour) % 24;
				int day = date.getDayOfMonth() + ((date.getHour() + durHour) / 24);
				day = day > monthLen ? day - monthLen : day;
				
				// Dynamically populate the value into the associated VO
				ForecastVO fvo = forecast.get(day + "_" + hour);
				fvo.setStartDate(Date.from(date.plusHours(durHour).atZone(ZoneId.systemDefault()).toInstant()));
				double normalizedValue = VenueForecastManager.normalizeByUnitOfMeasure(value.getValue(), unitOfMeasure);
				try {
					Method elementGetter = fvo.getClass().getMethod(elementGetMethod);
					Object element = elementGetter.invoke(fvo);
					Method dataSetter = element.getClass().getMethod(dataSetMethod, dataSetType);
					
					if (dataSetType == int.class) {
						dataSetter.invoke(element, (int) Math.round(normalizedValue));
					} else {
						dataSetter.invoke(element, normalizedValue);
					}
				} catch(Exception e) {
					throw new ActionException(e);
				}
			}
		}
	}
	
	/**
	 * Creates a base skeleton to hold the detailed forecast data in our normalized beans.
	 * 
	 * @return
	 */
	private Map<String, ForecastVO> createForecastSkeleton(String validTime) {
		Map<String, ForecastVO> skeleton = new HashMap<>();
		Date startDate = Convert.formatDate("yyyy-MM-dd'T'HH:mm:ss", validTime.substring(0, 19));
		
		LocalDate today = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		int monthDay = today.getDayOfMonth();
		int monthLen = today.lengthOfMonth();
		
		// The forecast will span ten days, with data recorded hour by hour
		for (int day = 0; day < 10; day++) {
			for (int hour = 0; hour < 24; hour++) {
				ForecastVO fvo = new ForecastVO();
				skeleton.put(monthDay + "_" + hour, fvo);
			}
			monthDay = monthDay == monthLen ? 1 : monthDay + 1;
		}
		
		return skeleton;
	}

	/* (non-Javadoc)
	 * @see com.perfectstorm.action.weather.manager.ForecastManagerInterface#setCoordinates(double, double)
	 */
	public void setCoordinates(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	/* (non-Javadoc)
	 * @see com.perfectstorm.action.weather.manager.ForecastManagerInterface#setWeatherStation(com.perfectstorm.data.weather.WeatherStationVO)
	 */
	@Override
	public void setWeatherStation(WeatherStationVO station) {
		this.station = station;
	}
	
	/**
	 * Retrieve the extended forecast data from the NWS.
	 * 
	 * @return
	 * @throws IOException
	 */
	private WeatherDetailVO getForecast() throws IOException {
		Gson gson = new GsonBuilder().create();
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		String gridDataUrl;
		
		if (station == null) {
			// Get the forecast grid data url associated to this coordinate (point)
			String pointUrl = String.format(NWS_POINT_URL, latitude, longitude);
			byte[] pointData = conn.retrieveData(pointUrl);
			WeatherPointVO wpvo = gson.fromJson(new String(pointData), WeatherPointVO.class);
			gridDataUrl = wpvo.getProperties().getForecastGridData();
		} else {
			gridDataUrl = String.format(NWS_GRID_DATA_URL, station.getForecastOfficeCode(), station.getForecastGridXNo(), station.getForecastGridYNo());
		}
		
		// Get the detailed forecast data
		byte[] gridData = conn.retrieveData(gridDataUrl);
		log.info("Forecast Grid Data URL: " + gridDataUrl);

		// Parse the data into an object
		return gson.fromJson(new String(gridData), WeatherDetailVO.class);
	}
}

