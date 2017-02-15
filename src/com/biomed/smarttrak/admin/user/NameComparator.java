package com.biomed.smarttrak.admin.user;

import java.util.Comparator;
import java.util.List;

import com.biomed.smarttrak.vo.UserVO;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: NameComparator.java<p/>
 * <b>Description: Compares VOs that extend the HumanNameIntfc and sorts them by name.
 * usage: Collections.sort(List<? implements HumanNameIntfc>)</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Feb 11, 2017
 ****************************************************************************/
public class NameComparator implements Comparator<Object> {

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compare(Object o1, Object o2) {
		HumanNameIntfc vo1 = (HumanNameIntfc) o1;
		String name1 = vo1.getFirstName() + vo1.getLastName();
		HumanNameIntfc vo2 = (HumanNameIntfc) o2;
		String name2 = vo2.getFirstName() + vo2.getLastName();
		return StringUtil.checkVal(name1).compareTo(name2);
	}


	/**
	 * reusable method that complements the above comparator - performs the decryption of the 
	 * name fields used by the comparator
	 * @param data
	 * @param encryptKey
	 */
	public void decryptNames(List<? extends HumanNameIntfc>  data,  String encryptKey) {
		StringEncrypter se;
		try {
			se = new StringEncrypter(encryptKey);
		} catch (EncryptionException e1) {
			return; //cannot use the decrypter, fail fast
		}

		for (HumanNameIntfc o : data) {
			try {
				HumanNameIntfc vo = (HumanNameIntfc) o;
				vo.setFirstName(se.decrypt(vo.getFirstName()));
				vo.setLastName(se.decrypt(vo.getLastName()));

				if (vo instanceof UserVO) {
					UserVO user = (UserVO) vo;
					user.setEmailAddress(se.decrypt(user.getEmailAddress()));
				}

			} catch (Exception e) {
				//ignoreable
			}
		}
	}
}