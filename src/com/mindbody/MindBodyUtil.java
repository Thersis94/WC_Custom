package com.mindbody;

import java.util.List;

import com.mindbodyonline.clients.api._0_5_1.ArrayOfClient;
import com.mindbodyonline.clients.api._0_5_1.ArrayOfClientService;
import com.mindbodyonline.clients.api._0_5_1.ArrayOfInt;
import com.mindbodyonline.clients.api._0_5_1.ArrayOfLong;
import com.mindbodyonline.clients.api._0_5_1.ArrayOfStaff;
import com.mindbodyonline.clients.api._0_5_1.ArrayOfStaffFilter;
import com.mindbodyonline.clients.api._0_5_1.ArrayOfString;
import com.mindbodyonline.clients.api._0_5_1.Client;
import com.mindbodyonline.clients.api._0_5_1.ClientService;
import com.mindbodyonline.clients.api._0_5_1.Staff;
import com.mindbodyonline.clients.api._0_5_1.StaffFilter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;

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
		client.setCountry(user.getCounty());
		client.setHomePhone(user.getMainPhone());
		client.setMobilePhone(user.getMobilePhone());
		client.setWorkPhone(user.getWorkPhone());
		client.setBirthDate(Convert.toCalendar(user.getBirthDate()));
		client.setUsername(user.getEmailAddress());
		client.setPassword(user.getPassword());
		client.setPromotionalEmailOptIn(Convert.formatBoolean(user.getAllowCommunication()));
		client.setGender(user.getGenderCode());

		
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
	 * Configure MindBody Client VO to UserDataVO
	 * @param client
	 * @return
	 */
	public static UserDataVO convertClientData(Client client) {
		UserDataVO user = new UserDataVO();
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

		return user;
	}
}