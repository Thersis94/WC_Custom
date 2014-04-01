package com.venture.cs.action.vo;

// SMTBaseLibs 2.0
import com.siliconmtn.annotations.DB_Validation;
import com.siliconmtn.annotations.DataType;
import com.siliconmtn.annotations.DatabaseColumn;
import com.siliconmtn.annotations.Importable;
import com.siliconmtn.security.UserDataVO;

// WebCrescendo 2.0
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.dealer.DealerLocationVO;

/****************************************************************************
 *<b>Title</b>: VehicleOwnerArchiveVO<p/>
 * Stores the information related to a vehicle that resides in the vehicle owner archive table.  Also
 * used for importing vehicles into the vehicle owner archive table (see method annotations).<p/>
 *Copyright: Copyright (c) 2014<p/>
 *Company: SiliconMountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Mar 28, 2014
 ****************************************************************************/

public class VehicleArchiveImportVO extends SBModuleVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7255361241820521459L;
	private static final String ARCHIVE_TABLE = "VENTURE_VEHICLE_OWNER_ARCHIVE";
	public static final Integer ARCHIVE_DEALER_TYPE_ID = new Integer(10);
	private DealerLocationVO dealer;
	private VehicleVO vehicle;
	private UserDataVO owner;
	
	/**
	 * Constructor - default
	 */
	public VehicleArchiveImportVO() {
		this(new DealerLocationVO(), new VehicleVO(), new UserDataVO());
	}
	
	/**
	 * 
	 * @param dealer
	 * @param vehicle
	 * @param owner
	 */
	public VehicleArchiveImportVO(DealerLocationVO dealer, VehicleVO vehicle, UserDataVO owner) {
		this.setDealer(dealer);
		this.setVehicle(vehicle);
		this.setOwner(owner);
	}
	
	/**
	 * @return the dealer
	 */
	public DealerLocationVO getDealer() {
		return dealer;
	}

	/**
	 * @param dealer the dealer to set
	 */
	public void setDealer(DealerLocationVO dealer) {
		this.dealer = dealer;
	}

	/**
	 * @return the vehicle
	 */
	public VehicleVO getVehicle() {
		return vehicle;
	}

	/**
	 * @param vehicle the vehicle to set
	 */
	public void setVehicle(VehicleVO vehicle) {
		this.vehicle = vehicle;
	}

	/**
	 * @return the owner
	 */
	public UserDataVO getOwner() {
		return owner;
	}

	/**
	 * @param owner the owner to set
	 */
	public void setOwner(UserDataVO owner) {
		this.owner = owner;
	}
	
	// VEHICLE info
	/**
	 * 
	 * @return
	 */
	public String getVin() {
		return vehicle.getVin();
	}

	/**
	 * 
	 * @param vin
	 */
	@Importable(name = "VIN", type = DataType.STRING)
	@DatabaseColumn(column = "VIN", dataType = "varchar(20)", table = ARCHIVE_TABLE)
	@DB_Validation(isReq=true)
	public void setVin(String vin) {
		vehicle.setVin(vin);
	}

	/**
	 * 
	 * @return
	 */
	public String getMake() {
		return vehicle.getMake();
	}
	
	/**
	 * 
	 * @param make
	 */
	@Importable(name = "Make", type = DataType.STRING)
	@DatabaseColumn(column = "MAKE", dataType = "varchar(32)", table = ARCHIVE_TABLE)
	@DB_Validation(isReq=true)
	public void setMake(String make) {
		vehicle.setMake(make);
	}
	
	/**
	 * 
	 * @return
	 */
	public String getModel() {
		return vehicle.getModel();
	}
		
	/**
	 * 
	 * @param model
	 */
	@Importable(name = "Model", type = DataType.STRING)
	@DatabaseColumn(column = "MODEL", dataType = "varchar(32)", table = ARCHIVE_TABLE)
	@DB_Validation(isReq=true)
	public void setModel(String model) {
		vehicle.setModel(model);
	}

	/**
	 * 
	 * @return
	 */
	public String getPurchaseDate() {
		return vehicle.getPurchaseYear();
	}
	
	/**
	 * 
	 * @param purchaseDate
	 */
	@Importable(name = "Purchase Date", type = DataType.STRING)
	@DatabaseColumn(column = "PURCHASE_DT", dataType = "varchar(15)", table = ARCHIVE_TABLE)
	@DB_Validation(isReq=true)
	public void setPurchaseDate(String purchaseDate) {
		vehicle.setPurchaseYear(purchaseDate);
	}
	
	// OWNER info
	/**
	 * 
	 * @return
	 */
	public String getOwnerName() {
		return owner.getFullName();
	}
	
	/**
	 * 
	 * @param ownerName
	 */
	@Importable(name = "Owner Name", type = DataType.STRING)
	@DatabaseColumn(column = "OWNER_NM", dataType = "varchar(128)", table = ARCHIVE_TABLE)
	@DB_Validation(isReq=true)
	public void setOwnerName(String ownerName) {
		owner.setName(ownerName);
	}
	
	/**
	 * 
	 * @return
	 */
	public String getAddress() {
		return owner.getAddress();
	}
	
	/**
	 * 
	 * @param address
	 */
	@Importable(name = "Address Text", type = DataType.STRING)
	@DatabaseColumn(column = "ADDRESS_TXT", dataType = "varchar(160)", table = ARCHIVE_TABLE)
	@DB_Validation(isReq=true)
	public void setAddress(String address) {
		owner.setAddress(address);
	}
	
	/**
	 * 
	 * @return
	 */
	public String getCity() {
		return owner.getCity();
	}
		
	/**
	 * 
	 * @param city
	 */
	@Importable(name = "City Name", type = DataType.STRING)
	@DatabaseColumn(column = "CITY_NM", dataType = "varchar(80)", table = ARCHIVE_TABLE)
	@DB_Validation(isReq=true)
	public void setCity(String city) {
		owner.setCity(city);
	}

	/**
	 * 
	 * @return
	 */
	public String getState() {
		return owner.getState();
	}
	
	/**
	 * 
	 * @param state
	 */
	@Importable(name = "State Code", type = DataType.STRING)
	@DatabaseColumn(column = "STATE_CD", dataType = "char(3)", table = ARCHIVE_TABLE)
	@DB_Validation(isReq=true)
	public void setState(String state) {
		owner.setState(state);
	}
	
	/**
	 * 
	 * @return
	 */
	public String getZipCode() {
		return owner.getZipCode();
	}
	
	/**
	 * 
	 * @param zipCode
	 */
	@Importable(name = "Zip Code", type = DataType.STRING)
	@DatabaseColumn(column = "ZIP_CD", dataType = "varchar(9)", table = ARCHIVE_TABLE)
	@DB_Validation(isReq=true)
	public void setZipCode(String zipCode) {
		owner.setZipCode(zipCode);
	}
	
	/**
	 * 
	 * @return
	 */
	public String getCountryCode() {
		return owner.getCountryCode();
	}
	
	/**
	 * 
	 * @param countryCode
	 */
	@Importable(name = "Country Code", type = DataType.STRING)
	@DatabaseColumn(column = "COUNTRY_CD", dataType = "char(3)", table = ARCHIVE_TABLE)
	@DB_Validation(isReq=true)
	public void setCountryCode(String countryCode) {
		owner.setCountryCode(countryCode);
	}
	
	// DEALER info
	/**
	 * 
	 * @return
	 */
	public String getDealerId() {
		return dealer.getDealerId();
	}
	
	/**
	 * @param dealerId the dealerId to set
	 */
	@Importable(name = "Dealer Id", type = DataType.STRING)
	@DatabaseColumn(column = "DEALER_ID", dataType = "varchar(32)", table = ARCHIVE_TABLE)
	@DB_Validation(isReq=true)
	public void setDealerId(String dealerId) {
		dealer.setDealerId(dealerId);
	}
	
	/**
	 * 
	 * @return
	 */
	public String getLocationName() {
		return dealer.getLocationName();
	}
	
	/**
	 * 
	 * @param locationName
	 */
	@Importable(name = "Location Name", type = DataType.STRING)
	@DatabaseColumn(column = "LOCATION_NM", dataType = "varchar(200)", table = ARCHIVE_TABLE)
	@DB_Validation(isReq=true)
	public void setLocationName(String locationName) {
		dealer.setLocationName(locationName);
	}
	
}
