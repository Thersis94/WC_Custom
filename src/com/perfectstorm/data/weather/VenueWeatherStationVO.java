package com.perfectstorm.data.weather;

// JDK 1.8.x
import java.sql.ResultSet;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: VenueWeatherStationVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value Object storing a weather station associated to a venue
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 11, 2019
 * @updates:
 ****************************************************************************/
@Table(name="ps_venue_weather_station")
public class VenueWeatherStationVO extends WeatherStationVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2145790220855585523L;
	
	// Members
	private String venueWeatherStationId;
	private String venueId;

	/**
	 * 
	 */
	public VenueWeatherStationVO() {
		super();
	}

	/**
	 * @param req
	 */
	public VenueWeatherStationVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public VenueWeatherStationVO(ResultSet rs) {
		super(rs);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.data.parser.BeanDataVO#toString()
	 */
	@Override
	public String toString() {
		return StringUtil.getToString(this);
	}

	/**
	 * @return the venueWeatherStationId
	 */
	@Column(name="venue_weather_station_id", isPrimaryKey=true)
	public String getVenueWeatherStationId() {
		return venueWeatherStationId;
	}

	/**
	 * @return the venueId
	 */
	@Column(name="venue_id")
	public String getVenueId() {
		return venueId;
	}

	/**
	 * @param venueWeatherStationId the venueWeatherStationId to set
	 */
	public void setVenueWeatherStationId(String venueWeatherStationId) {
		this.venueWeatherStationId = venueWeatherStationId;
	}

	/**
	 * @param venueId the venueId to set
	 */
	public void setVenueId(String venueId) {
		this.venueId = venueId;
	}
}

