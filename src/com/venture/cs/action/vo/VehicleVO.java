package com.venture.cs.action.vo;

// JDK 7
import java.sql.ResultSet;
import java.util.ArrayList;

// SMTBaseLibs 2.0
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;

// WebCrescendo 2.0
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.dealer.DealerLocationVO;

/****************************************************************************
 *<b>Title</b>: VehicleVO<p/>
 * Stores the information related to a vehicle <p/>
 *Copyright: Copyright (c) 2013<p/>
 *Company: SiliconMountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since July 23, 2013
 ****************************************************************************/

public class VehicleVO extends SBModuleVO {

	private static final long serialVersionUID = 1L;

	private String vehicleId;
	private String vin;
	private UserDataVO owner;
	private DealerLocationVO dealer;
	private ArrayList<TicketVO> tickets;
	private ArrayList<ActivityVO> activity;
	private String make;
	private String model;
	private String year;
	private String purchaseYear;
	private int freezeFlag;
	private int requiresAction = 0;//The user cannot directly set this variable, a ticket that requires action needs to be added for this to be changed
	
	/**
	 * Constructor - default
	 */
	public VehicleVO() {
		this.setOwner(new UserDataVO());
		this.setDealer(new DealerLocationVO());
		this.setTickets(new ArrayList<TicketVO>());
	}
	
	/**
	 * Constructor using request parameters to populate members
	 * @param req
	 */
	public VehicleVO(SMTServletRequest req) {
		this.setVehicleId(req.getParameter("vehicleId"));
		this.setVin(req.getParameter("vin"));
		this.setMake(req.getParameter("make"));
		this.setModel(req.getParameter("model"));
		this.setYear(req.getParameter("year"));
		this.setPurchaseYear(req.getParameter("purchaseDate"));
		this.setFreezeFlag(Convert.formatInteger(req.getParameter("freezeFlag")));
		this.setOwner(new UserDataVO(req));
		this.setDealer(new DealerLocationVO(req));
		this.setTickets(new ArrayList<TicketVO>());
		this.setActivity(new ArrayList<ActivityVO>());
	}
	
	/**
	 * Constructor using ResultSet to populate members
	 * @param rs
	 */
	public VehicleVO(ResultSet rs) {
		DBUtil db = new DBUtil();
		this.setVehicleId(db.getStringVal("VENTURE_VEHICLE_ID", rs));
		this.setVin(db.getStringVal("VIN", rs));
		this.setMake(db.getStringVal("MAKE", rs));
		this.setModel(db.getStringVal("MODEL", rs));
		this.setYear(db.getStringVal("YEAR", rs));
		this.setPurchaseYear(db.getStringVal("PURCHASE_DT", rs));
		this.setFreezeFlag(db.getIntVal("FREEZE_FLG", rs));
		this.setOwner(new UserDataVO(rs));
		this.setDealer(new DealerLocationVO(rs));
		this.setTickets(new ArrayList<TicketVO>());
		this.setActivity(new ArrayList<ActivityVO>());
	}

	public String getVehicleId() {
		return vehicleId;
	}

	public void setVehicleId(String vehicleId) {
		this.vehicleId = vehicleId;
	}

	public String getVin() {
		return vin;
	}

	public void setVin(String vin) {
		this.vin = vin;
	}

	public UserDataVO getOwner() {
		return owner;
	}

	public void setOwner(UserDataVO userDataVO) {
		this.owner = userDataVO;
	}

	public DealerLocationVO getDealer() {
		return dealer;
	}

	public void setDealer(DealerLocationVO dealerLocationVO) {
		this.dealer = dealerLocationVO;
	}
	
	public void addTicket(TicketVO ticket){
		// If this ticket requires action the we set the requires action flag
		if (ticket.getActionReqFlag() == 1)
			this.requiresAction = 1;
		tickets.add(ticket);
	}

	public ArrayList<TicketVO> getTickets() {
		return tickets;
	}

	public void setTickets(ArrayList<TicketVO> tickets) {
		this.tickets = tickets;
	}
	
	public void addActivity(ActivityVO activity) {
		this.activity.add(activity);
	}

	public ArrayList<ActivityVO> getActivity() {
		return activity;
	}

	public void setActivity(ArrayList<ActivityVO> activity) {
		this.activity = activity;
	}

	public String getMake() {
		return make;
	}

	public void setMake(String make) {
		this.make = make;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getYear() {
		return year;
	}

	public void setYear(String year) {
		this.year = year;
	}

	public String getPurchaseYear() {
		return purchaseYear;
	}

	public void setPurchaseYear(String string) {
		this.purchaseYear = string;
	}

	public int getFreezeFlag() {
		return freezeFlag;
	}

	public void setFreezeFlag(int freezeFlag) {
		this.freezeFlag = freezeFlag;
	}

	public int getRequiresAction() {
		return requiresAction;
	}
	
}
