package com.mindbody.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.mindbody.security.MindBodyUserVO;
import com.mindbody.vo.MindBodyCredentialVO;
import com.mindbody.vo.classes.MBClassDescriptionVO;
import com.mindbody.vo.classes.MBClassScheduleVO;
import com.mindbody.vo.classes.MBClassVO;
import com.mindbody.vo.classes.MBLevelVO;
import com.mindbody.vo.classes.MBLocationVO;
import com.mindbody.vo.classes.MBProgramVO;
import com.mindbody.vo.classes.MBSessionTypeVO;
import com.mindbody.vo.classes.MBStaffVO;
import com.mindbody.vo.clients.MBClientServiceVO;
import com.mindbody.vo.clients.MBVisitVO;
import com.mindbody.vo.sales.MBPaymentVO;
import com.mindbody.vo.sales.MBPurchasedItemVO;
import com.mindbody.vo.sales.MBSaleItemVO;
import com.mindbody.vo.sales.MBSaleVO;
import com.mindbody.vo.sales.MBServiceVO;
import com.mindbody.vo.site.MBRelationshipVO;
import com.mindbody.vo.staff.MBPermissionVO;
import com.mindbodyonline.clients.api._0_5_1.ArrayOfCartItem;
import com.mindbodyonline.clients.api._0_5_1.ArrayOfClient;
import com.mindbodyonline.clients.api._0_5_1.ArrayOfClientService;
import com.mindbodyonline.clients.api._0_5_1.ArrayOfInt;
import com.mindbodyonline.clients.api._0_5_1.ArrayOfLong;
import com.mindbodyonline.clients.api._0_5_1.ArrayOfPayment;
import com.mindbodyonline.clients.api._0_5_1.ArrayOfPaymentInfo;
import com.mindbodyonline.clients.api._0_5_1.ArrayOfPurchasedItem;
import com.mindbodyonline.clients.api._0_5_1.ArrayOfStaff;
import com.mindbodyonline.clients.api._0_5_1.ArrayOfStaffFilter;
import com.mindbodyonline.clients.api._0_5_1.ArrayOfString;
import com.mindbodyonline.clients.api._0_5_1.CartItem;
import com.mindbodyonline.clients.api._0_5_1.Class;
import com.mindbodyonline.clients.api._0_5_1.ClassDescription;
import com.mindbodyonline.clients.api._0_5_1.ClassSchedule;
import com.mindbodyonline.clients.api._0_5_1.Client;
import com.mindbodyonline.clients.api._0_5_1.ClientCreditCard;
import com.mindbodyonline.clients.api._0_5_1.ClientService;
import com.mindbodyonline.clients.api._0_5_1.Level;
import com.mindbodyonline.clients.api._0_5_1.Location;
import com.mindbodyonline.clients.api._0_5_1.Payment;
import com.mindbodyonline.clients.api._0_5_1.PaymentInfo;
import com.mindbodyonline.clients.api._0_5_1.Permission;
import com.mindbodyonline.clients.api._0_5_1.Program;
import com.mindbodyonline.clients.api._0_5_1.PurchasedItem;
import com.mindbodyonline.clients.api._0_5_1.Relationship;
import com.mindbodyonline.clients.api._0_5_1.Sale;
import com.mindbodyonline.clients.api._0_5_1.SaleItem;
import com.mindbodyonline.clients.api._0_5_1.Service;
import com.mindbodyonline.clients.api._0_5_1.SessionType;
import com.mindbodyonline.clients.api._0_5_1.ShoppingCart;
import com.mindbodyonline.clients.api._0_5_1.Staff;
import com.mindbodyonline.clients.api._0_5_1.StaffFilter;
import com.mindbodyonline.clients.api._0_5_1.Visit;
import com.siliconmtn.commerce.ShoppingCartItemVO;
import com.siliconmtn.commerce.payment.PaymentVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title:</b> MindBodyUtil.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Util for converting to MindBody Collections.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 13, 2017
 ****************************************************************************/
public class MindBodyUtil {

	public static final String MINDBODY_CLIENT_ID = "MINDBODY_CLIENT_ID";

	private MindBodyUtil() {
		//Purposefully Blank.
	}

	/**
	 * Builds a MindBody ArrayOfInt Object from provided List.
	 * @param config
	 * @param req
	 */
	public static ArrayOfInt buildArrayOfInt(List<Integer> vals) {
		ArrayOfInt intArr = ArrayOfInt.Factory.newInstance();
		for(int i : vals) {
			intArr.addInt(i);
		}
		return intArr;
	}

	/**
	 * Builds a MindBody ArrayOfLong Object from provided List.
	 * @param config
	 * @param req
	 */
	public static ArrayOfLong buildArrayOfLong(List<Long> vals) {
		ArrayOfLong longArr = ArrayOfLong.Factory.newInstance();
		for(long i : vals) {
			longArr.addLong(i);
		}
		return longArr;
	}

	/**
	 * Builds a MindBody ArrayOfInt Object from provided List.
	 * @param config
	 * @param req
	 */
	public static ArrayOfString buildArrayOfString(List<String> vals) {
		ArrayOfString stringArr = ArrayOfString.Factory.newInstance();
		for(String i : vals) {
			stringArr.addString(i);
		}
		return stringArr;
	}


	/**
	 * Builds a MindBody ArrayOfStaff Object from provided List.
	 * @param staff
	 * @return
	 */
	public static ArrayOfStaff buildArrayOfStaff(List<Staff> staff) {
		ArrayOfStaff staffArr = ArrayOfStaff.Factory.newInstance();

		for(int i = 0; i < staff.size(); i++) {
			staffArr.setStaffArray(0, staff.get(i));
		}
		return staffArr;
	}

	/**
	 * Builds a MindBody ArrayOfStaffFilter Object from provided List.
	 * @param filters
	 * @return
	 */
	public static ArrayOfStaffFilter buildArrayOfStaffFilter(List<StaffFilter.Enum> filters) {
		ArrayOfStaffFilter staffArr = ArrayOfStaffFilter.Factory.newInstance();

		for(int i = 0; i < filters.size(); i++) {
			staffArr.addStaffFilter(filters.get(i));
		}
		return staffArr;
	}

	/**
	 * @param client
	 * @return
	 */
	public static Client convertClient(UserDataVO user) {
		Client client = Client.Factory.newInstance();
		client.setID(user.getProfileId());
		client.setFirstName(user.getFirstName());
		client.setMiddleName(user.getMiddleName());
		client.setLastName(user.getLastName());
		client.setEmail(user.getEmailAddress());
		client.setAddressLine1(user.getAddress());
		client.setAddressLine2(user.getAddress2());
		client.setCity(user.getCity());
		client.setState(user.getState());
		client.setPostalCode(user.getZipCode());
		client.setCountry(user.getCountryCode());
		client.setHomePhone(user.getMainPhone());
		client.setMobilePhone(StringUtil.checkVal(user.getMobilePhone(), user.getPhoneNumbers().get(0).getPhoneNumber()));
		client.setWorkPhone(StringUtil.checkVal(user.getWorkPhone(), user.getPhoneNumbers().get(0).getPhoneNumber()));
		client.setBirthDate(Convert.toCalendar(user.getBirthDate()));
		client.setUsername(user.getEmailAddress());
		client.setPassword(user.getProfileId());
		client.setPromotionalEmailOptIn(Convert.formatBoolean(user.getAllowCommunication()));
		client.setReferredBy((String) user.getAttribute("referredBy"));

		return client;
	}

	/**
	 * Builds a MindBody ArrayOfClient Object from provided List.
	 * @param clients
	 * @return
	 */
	public static ArrayOfClient buildArrayOfClients(List<Client> clients) {
		ArrayOfClient clientArr = ArrayOfClient.Factory.newInstance();
		for(int i = 0; i < clients.size(); i++) {
			clientArr.addNewClient();
			clientArr.setClientArray(i, clients.get(i));
		}

		return clientArr;
	}

	/**
	 * Builds a MindBody ArrayOfClientServices Object from provided List.
	 * @param clientServices
	 * @return
	 */
	public static ArrayOfClientService buildArrayOfClientService(List<ClientService> clientServices) {
		ArrayOfClientService serviceArr = ArrayOfClientService.Factory.newInstance();
		for(int i = 0; i < clientServices.size(); i++) {
			serviceArr.setClientServiceArray(i, clientServices.get(i));
		}

		return serviceArr;
	}

	/**
	 * @param cartItems
	 * @return
	 */
	public static ArrayOfCartItem buildArrayOfCartItem(List<ShoppingCartItemVO> cartItems) {
		ArrayOfCartItem cis = ArrayOfCartItem.Factory.newInstance();
		for(ShoppingCartItemVO item : cartItems) {
			cis.addNewCartItem();
			cis.setCartItemArray(0, convertCartItem(item));
		}
		return cis;
	}

	/**
	 * @param item
	 * @return
	 */
	private static CartItem convertCartItem(ShoppingCartItemVO item) {
		//TODO - Need To Convert our ShoppingCartItem to MindBody CartItem
		return null;
	}

	/**
	 * @param payments
	 * @return
	 */
	public static ArrayOfPaymentInfo buildArrayOfPayments(List<PaymentVO> payments) {
		ArrayOfPaymentInfo aopi = ArrayOfPaymentInfo.Factory.newInstance();
		for(PaymentVO pmt : payments) {
			aopi.addNewPaymentInfo();
			aopi.setPaymentInfoArray(0, buildPaymentInfo(pmt));
		}
		return aopi;
	}

	/**
	 * @param pmt
	 * @return
	 */
	private static PaymentInfo buildPaymentInfo(PaymentVO pmt) {
		return null;
	}

	/**
	 * Configure MindBody Client VO to UserDataVO
	 * @param client
	 * @return
	 */
	public static MindBodyUserVO convertClientData(Client client) {
		MindBodyUserVO user = new MindBodyUserVO();
		user.setAllowCommunication(Convert.formatInteger(client.getPromotionalEmailOptIn()));
		user.setFirstName(client.getFirstName());
		user.setLastName(client.getLastName());
		user.setAddress(client.getAddressLine1());
		user.setCity(client.getCity());
		user.setState(client.getState());
		user.setCountryCode(client.getCountry());
		user.setZipCode(client.getPostalCode());
		user.setMobilePhone(client.getMobilePhone());
		user.setProfileId(client.getID());
		user.setEmailAddress(client.getEmail());

		if(client.isSetClientCreditCard()) {
			user.setClientCardData(convertClientCreditCart(client.getClientCreditCard()));
			user.setBillingAddress(convertBillingAddress(client.getClientCreditCard()));
		}

		return user;
	}

	/**
	 * @param clientCreditCard
	 * @return
	 */
	public static com.siliconmtn.gis.Location convertBillingAddress(ClientCreditCard cc) {
		com.siliconmtn.gis.Location loc = new com.siliconmtn.gis.Location();
		loc.setAddress(cc.getAddress());
		loc.setState(cc.getState());
		loc.setZipCode(cc.getPostalCode());
		loc.setCity(cc.getCity());
		return loc;
	}

	/**
	 * @param clientCreditCard
	 * @return
	 */
	public static PaymentVO convertClientCreditCart(ClientCreditCard cc) {
		PaymentVO card = new PaymentVO();
		card.setPaymentName(cc.getCardHolder());
		card.setPaymentType(cc.getCardType());
		card.setExpirationMonth(cc.getExpMonth());
		card.setExpirationYear(cc.getExpYear());
		card.setPaymentNumberSuffix(cc.getLastFour());
		return card;
	}

	/**
	 * Return a Staff User Credential Object from given config.
	 * @param config
	 * @return
	 */
	public static MindBodyCredentialVO buildStaffCredentials(Map<String, String> config) {
		String staffNm = StringUtil.checkVal(config.get(MindBodyCredentialVO.MINDBODY_STAFF_ID));
		String staffPass = StringUtil.checkVal(config.get(MindBodyCredentialVO.MINDBODY_STAFF_PASS));
		int siteId = Integer.parseInt(StringUtil.checkVal(config.get(MindBodyCredentialVO.MINDBODY_SITE_ID)));
		return buildCredentials(staffNm, staffPass, Arrays.asList(siteId));
	}

	/**
	 * Return a SourceCredential Object from given config.
	 * @param config
	 * @return
	 */
	public static MindBodyCredentialVO buildSourceCredentials(Map<String, String> config) {
		String sourceName = StringUtil.checkVal(config.get(MindBodyCredentialVO.MINDBODY_SOURCE_NAME));
		String apiKey = StringUtil.checkVal(config.get(MindBodyCredentialVO.MINDBODY_API_KEY));
		int siteId = Integer.parseInt(StringUtil.checkVal(config.get(MindBodyCredentialVO.MINDBODY_SITE_ID)));
		return buildCredentials(sourceName, apiKey, Arrays.asList(siteId));

	}

	/**
	 * Generate a MindBodyCredentialVO from given data.
	 * @param user
	 * @param pass
	 * @param siteIds
	 * @return
	 */
	private static MindBodyCredentialVO buildCredentials(String user, String pass, List<Integer> siteIds) {
		return new MindBodyCredentialVO(user, pass, siteIds);
	}

	/**
	 * Generates a ClientCreditCard VO from a PaymentVO
	 * @param loc 
	 * @param card
	 * @return
	 */
	public static ClientCreditCard convertClientCreditCard(com.siliconmtn.gis.Location loc, PaymentVO card) {
		ClientCreditCard cc = ClientCreditCard.Factory.newInstance();

		cc.setCardNumber(card.getPaymentNumber());
		cc.setExpMonth(card.getExpirationMonth());
		cc.setExpYear(card.getExpirationYear());
		cc.setCardType(card.getPaymentType());
		cc.setLastFour(card.getPaymentNumberSuffix());
		cc.setCardHolder(card.getPaymentName());

		cc.setAddress(loc.getAddress());
		cc.setCity(loc.getCity());
		cc.setState(loc.getState());
		cc.setPostalCode(loc.getZipCode());

		return cc;
	}

	/**
	 * Convert a Mindbody Class to a MBClassVO
	 * @param c
	 * @return
	 */
	public static MBClassVO convertClassData(Class c) {
		MBClassVO mbc = new MBClassVO();
		if(c.isSetLocation()) {
			mbc.setLocation(convertLocation(c.getLocation()));
		}
		if(c.isSetClassDescription()) {
			mbc.setClassDescription(convertClassDescription(c.getClassDescription()));
		}
		if(c.isSetStaff()) {
			mbc.setStaff(convertStaff(c.getStaff()));
		}

		mbc.setClassScheduleId(c.getClassScheduleID());
		mbc.setMaxCapacity(c.getMaxCapacity());
		mbc.setWebCapacity(c.getWebCapacity());
		mbc.setTotalBooked(c.getTotalBooked());
		mbc.setTotalBookedWaitlist(c.getTotalBookedWaitlist());
		mbc.setWebBooked(c.getWebBooked());
		mbc.setSemesterId(c.getSemesterID());
		mbc.setId(c.getID());
		mbc.setCancelled(c.getIsCanceled());
		mbc.setHasSubstitute(c.getSubstitute());
		mbc.setActive(c.getActive());
		mbc.setWaitlistAvailable(c.getIsWaitlistAvailable());
		mbc.setEnrolled(c.getIsEnrolled());
		mbc.setHideCancelled(c.getHideCancel());
		mbc.setAvailable(c.getIsAvailable());
		mbc.setStartDateTime(c.getStartDateTime().getTime());
		mbc.setEndDateTime(c.getEndDateTime().getTime());
		mbc.setLastModifiedDateTime(c.getLastModifiedDateTime().getTime());

		return mbc;
	}

	/**
	 * Converts a Mindbody Location to a MBLocationVO
	 * @param location
	 * @return
	 */
	public static MBLocationVO convertLocation(Location l) {
		MBLocationVO loc = new MBLocationVO();
		loc.setSiteId(l.getSiteID());
		loc.setBusinessDescription(l.getBusinessDescription());
		loc.setHasClasses(l.getHasClasses());
		loc.setId(l.getID());
		loc.setName(l.getName());
		loc.setAddress(l.getAddress());
		loc.setAddress2(l.getAddress2());
		loc.setTax1(l.getTax1());
		loc.setTax2(l.getTax2());
		loc.setTax3(l.getTax3());
		loc.setTax4(l.getTax4());
		loc.setTax5(l.getTax5());
		loc.setPhone(l.getPhone());
		loc.setPhoneExtension(l.getPhoneExtension());
		loc.setCity(l.getCity());
		loc.setState(l.getStateProvCode());
		loc.setZipCode(l.getPostalCode());
		loc.setLatitude(l.getLatitude());
		loc.setLongitude(l.getLongitude());
		loc.setAdditionalImageUrls(Arrays.asList(l.getAdditionalImageURLs().getStringArray()));
		loc.setFacilitySquareFeet(l.getFacilitySquareFeet());
		loc.setTreatmentRooms(l.getTreatmentRooms());

		return loc;
	}

	/**
	 * Converts a Mindbody Staff to a MBStaffVO
	 * @param staff
	 * @return
	 */
	public static MBStaffVO convertStaff(Staff s) {
		MBStaffVO staff = new MBStaffVO();
		staff.setAddress(s.getAddress());
		staff.setCity(s.getCity());
		staff.setState(s.getState());
		staff.setCountryCode(s.getCountry());
		staff.setZipCode(s.getPostalCode());
		staff.setSortOrder(s.getSortOrder());
		staff.setAppointmentTrn(s.getAppointmentTrn());
		staff.setReservationTrn(s.getReservationTrn());
		staff.setIndependentContractor(s.getIndependentContractor());
		staff.setAlwaysAllowDoubleBooking(s.getAlwaysAllowDoubleBooking());
		staff.setProfileId(Long.toString(s.getID()));
		staff.setName(s.getName());
		staff.setProfileImage(s.getImageURL());
		staff.setBio(s.getBio());
		staff.setGenderCode(s.getIsMale() ? "Male" : "Female");
		return staff;
	}

	/**
	 * Convert a Mindbody ClassDescription to MBClassDescriptionVO
	 * @param classDescription
	 * @return
	 */
	public static MBClassDescriptionVO convertClassDescription(ClassDescription cd) {
		MBClassDescriptionVO desc = new MBClassDescriptionVO();
		desc.setAction(cd.getAction());
		desc.setActive(cd.getActive());
		desc.setDescription(cd.getDescription());
		desc.setId(cd.getID());
		desc.setImageUrl(cd.getImageURL());
		desc.setLastUpdated(cd.getLastUpdated().getTime());
		desc.setName(cd.getName());
		desc.setNotes(cd.getNotes());
		desc.setPreReq(cd.getPrereq());
		if(cd.isSetProgram()) {
			desc.setProgram(convertProgram(cd.getProgram()));
		}
		if(cd.isSetSessionType()) {
			desc.setSessionType(convertSessionType(cd.getSessionType()));
		}
		if(cd.isSetLevel()) {
			desc.setLevel(convertLevel(cd.getLevel()));
		}
		return desc;
	}

	/**
	 * Convert a Mindbody Level to MBLevelVO
	 * @param level
	 * @return
	 */
	public static MBLevelVO convertLevel(Level l) {
		MBLevelVO level = new MBLevelVO();
		level.setId(l.getID());
		level.setDescription(l.getDescription());
		level.setName(l.getName());
		return level;
	}

	/**
	 * Convert a Mindbody SessionType to MBSessionTypeVO
	 * @param sessionType
	 * @return
	 */
	public static MBSessionTypeVO convertSessionType(SessionType st) {
		MBSessionTypeVO sess = new MBSessionTypeVO();
		sess.setDefaultTimeLength(st.getDefaultTimeLength());
		sess.setProgramId(st.getProgramID());
		sess.setNumDeducted(st.getNumDeducted());
		sess.setId(st.getID());
		sess.setName(st.getName());
		return sess;
	}

	/**
	 * Convert a Mindbody Program to a MBProgramVO
	 * @param program
	 * @return
	 */
	public static MBProgramVO convertProgram(Program p) {
		MBProgramVO program = new MBProgramVO();
		program.setId(p.getID());
		program.setName(p.getName());
		program.setScheduleType(p.getScheduleType());
		program.setCancelOffset(p.getCancelOffset());
		return program;
	}

	/**
	 * @param o
	 * @return
	 */
	public static MBClassScheduleVO convertClassSchedule(ClassSchedule cs) {
		MBClassScheduleVO sched = new MBClassScheduleVO();
		if(cs.isSetLocation()) {
			sched.setLocation(convertLocation(cs.getLocation()));
		}
		if(cs.isSetClassDescription()) {
			sched.setClassDescription(convertClassDescription(cs.getClassDescription()));
		}
		if(cs.isSetStaff()) {
			sched.setStaff(convertStaff(cs.getStaff()));
		}
		sched.setId(cs.getID());
		sched.setSemesterId(cs.getSemesterID());
		sched.setDaySunday(cs.getDaySunday());
		sched.setDayMonday(cs.getDayMonday());
		sched.setDayTuesday(cs.getDayTuesday());
		sched.setDayWednesday(cs.getDayWednesday());
		sched.setDayThursday(cs.getDayThursday());
		sched.setDayFriday(cs.getDayFriday());
		sched.setDaySaturday(cs.getDaySaturday());
		sched.setStartDate(cs.getStartDate().getTime());
		sched.setEndDate(cs.getEndDate().getTime());
		sched.setStartTime(cs.getStartTime().getTime());
		sched.setEndTime(cs.getEndTime().getTime());

		return sched;
	}

	/**
	 * @param v
	 * @return
	 */
	public static MBVisitVO convertVisits(Visit v) {
		MBVisitVO visit = new MBVisitVO();

		visit.setAppointmentId(v.getAppointmentID());
		visit.setAppointmentStatus(v.getAppointmentStatus());
		visit.setAppointmentGenderPreference(v.getAppointmentGenderPreference());
		visit.setClassId(v.getClassID());
		visit.setClient(convertClientData(v.getClient()));
		visit.setEndDateTime(v.getEndDateTime().getTime());
		visit.setId(v.getID());
		visit.setLastModifiedDateTime(v.getLastModifiedDateTime().getTime());
		visit.setLateCancelled(v.getLateCancelled());
		visit.setLocation(convertLocation(v.getLocation()));
		visit.setMakeUp(v.getMakeUp());
		visit.setName(v.getName());
		visit.setService(convertClientService(v.getService()));
		visit.setSignedIn(v.getSignedIn());
		visit.setStaff(convertStaff(v.getStaff()));
		visit.setStartDateTime(v.getStartDateTime().getTime());
		visit.setWebSignup(v.getWebSignup());
		return visit;
	}

	/**
	 * @param service
	 * @return
	 */
	public static MBClientServiceVO convertClientService(ClientService service) {
		MBClientServiceVO s = new MBClientServiceVO();
		s.setAction(service.getAction());
		s.setActiveDate(service.getActiveDate().getTime());
		s.setCount(service.getCount());
		s.setCurrent(service.getCurrent());
		s.setExpirationDate(service.getExpirationDate().getTime());
		s.setId(service.getID());
		s.setName(service.getName());
		s.setPaymentDate(service.getPaymentDate().getTime());
		s.setProgram(convertProgram(service.getProgram()));
		s.setRemaining(service.getRemaining());
		s.setSiteId(service.getSiteID());
		return s;
	}

	/**
	 * @param s
	 * @return
	 */
	public static MBSaleItemVO convertSaleItem(SaleItem s) {
		MBSaleItemVO item = new MBSaleItemVO();
		item.setAccountPayment(s.getAccountPayment());
		item.setAmountPaid(s.getAmountPaid());
		item.setDescription(s.getDescription());
		item.setDiscount(s.getDiscount());
		item.setPrice(s.getPrice());
		item.setQuantity(s.getQuantity());
		item.setReturned(s.getReturned());
		item.setSale(convertSale(s.getSale()));
		item.setTax(s.getTax());
		return item;
	}

	/**
	 * @param sale
	 */
	public static MBSaleVO convertSale(Sale sale) {
		MBSaleVO s = new MBSaleVO();
		s.setClientId(sale.getClientID());
		s.setId(sale.getID());
		s.setLocation(convertLocation(sale.getLocation()));
		s.setPayments(convertPayments(sale.getPayments()));
		s.setPurchasedItems(convertPurchasedItems(sale.getPurchasedItems()));
		s.setSaleDate(sale.getSaleDate().getTime());
		s.setSaleDateTime(sale.getSaleDateTime().getTime());
		s.setSaleTime(sale.getSaleTime().getTime());
		
		return s;
	}

	/**
	 * @param purchasedItems
	 * @return
	 */
	public static List<MBPurchasedItemVO> convertPurchasedItems(ArrayOfPurchasedItem purchasedItems) {
		List<MBPurchasedItemVO> purchases = new ArrayList<>();
		for(PurchasedItem p : purchasedItems.getPurchasedItemArray()) {
			purchases.add(convertPurchasedItem(p));
		}
		return purchases;
	}

	/**
	 * @param p
	 * @return
	 */
	public static MBPurchasedItemVO convertPurchasedItem(PurchasedItem p) {
		MBPurchasedItemVO purchase = new MBPurchasedItemVO();
		purchase.setId(p.getID());
		purchase.setService(p.getIsService());
		return purchase;
	}

	/**
	 * @param payments
	 * @return
	 */
	public static List<MBPaymentVO> convertPayments(ArrayOfPayment payments) {
		List<MBPaymentVO> pmts = new ArrayList<>();
		for(Payment p : payments.getPaymentArray()) {
			pmts.add(convertPayment(p));
		}
		return pmts;
	}

	/**
	 * @param p
	 * @return
	 */
	public static MBPaymentVO convertPayment(Payment p) {
		MBPaymentVO payment = new MBPaymentVO();
		payment.setAmount(p.getAmount());
		payment.setId(p.getID());
		payment.setLastFour(p.getLastFour());
		payment.setMethod(p.getMethod());
		payment.setNotes(p.getNotes());
		payment.setType(p.getType());

		return payment;
	}

	/**
	 * @param r
	 * @return
	 */
	public static MBRelationshipVO convertRelationship(Relationship r) {
		MBRelationshipVO rel = new MBRelationshipVO();
		rel.setId(r.getID());
		rel.setRelationshipName1(r.getRelationshipName1());
		rel.setRelationshipName2(r.getRelationshipName2());
		return rel;
	}

	/**
	 * @param p
	 * @return
	 */
	public static Object convertPermission(Permission p) {
		MBPermissionVO perm = new MBPermissionVO();
		perm.setDisplayName(p.getDisplayName());
		perm.setName(p.getName());
		perm.setValue(p.getValue());
		return perm;
	}

	/**
	 * @param shoppingCart
	 * @return
	 */
	public static com.siliconmtn.commerce.ShoppingCartVO convertShoppingCart(ShoppingCart sc) {
		com.siliconmtn.commerce.ShoppingCartVO cart = new com.siliconmtn.commerce.ShoppingCartVO();
		if(sc.isSetCartItems()) {
			for(CartItem ci : sc.getCartItems().getCartItemArray()) {
				cart.add(convertShoppingCartItem(ci));
			}
		}

		return cart;
	}

	/**
	 * @param ci
	 * @return
	 */
	public static ShoppingCartItemVO convertShoppingCartItem(CartItem ci) {
		return null;
	}

	/**
	 * @param s
	 * @return
	 */
	public static MBServiceVO convertService(Service s) {
		MBServiceVO service = new MBServiceVO();
		service.setAction(s.getAction());
		service.setCount(s.getCount());
		service.setId(s.getID());
		service.setName(s.getName());
		service.setOnlinePrice(s.getOnlinePrice());
		service.setPrice(s.getPrice());
		service.setProductId(s.getProductID());
		service.setProgramId(s.getProgramID());
		service.setTaxIncluded(s.getTaxIncluded());
		service.setTaxRate(s.getTaxRate());
		return service;
	}
}