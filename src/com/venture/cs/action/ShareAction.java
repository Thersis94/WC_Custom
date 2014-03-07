package com.venture.cs.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.MessageSender;

/****************************************************************************
 *<b>Title</b>: ShareAction<p/>
 * Sends this case to a another user on this site <p/>
 *Copyright: Copyright (c) 2013<p/>
 *Company: SiliconMountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since July 23, 2013
 ****************************************************************************/

public class ShareAction extends SBActionAdapter {
	
	public ShareAction() {
		super();
	}

	public ShareAction(ActionInitVO arg0) {
		super(arg0);
	}
	
    public void retrieve(SMTServletRequest req) throws ActionException {
    	ModuleVO modVo = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		
    	StringBuilder sb = new StringBuilder();
    	
    	// Get all the users that are registered with this site
    	sb.append("SELECT p.PROFILE_ID, p.FIRST_NM, p.LAST_NM, p.EMAIL_ADDRESS_TXT FROM PROFILE p ");
    	sb.append("left join PROFILE_ROLE pr on p.PROFILE_ID = pr.PROFILE_ID ");
    	sb.append("WHERE pr.SITE_ID = ?");
    	
    	log.debug(sb.toString() + "|" + req.getParameter("siteId"));
        PreparedStatement ps = null;
        ArrayList<UserDataVO> users = new ArrayList<UserDataVO>();
        
        UserDataVO vo;
    	try {
			String encKey = (String) getAttribute(Constants.ENCRYPT_KEY);
			StringEncrypter se = new StringEncrypter(encKey);
            ps = dbConn.prepareStatement(sb.toString());
            ps.setString(1, req.getParameter("siteId"));
            
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
            	
            	//Decrypt the user data
            	vo = new UserDataVO(rs);
            	vo.setFirstName(se.decrypt(vo.getFirstName()));
            	vo.setLastName(se.decrypt(vo.getLastName()));
            	vo.setEmailAddress(se.decrypt(vo.getEmailAddress()));
            	users.add(vo);
            }
        } catch (SQLException sqle) {
        	log.error("Unable to execute query ", sqle);
        } catch (EncryptionException e) {
        	log.error("Unable to decrypt user data ", e);
		}  finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch(Exception e) {}
            }
        }
        modVo.setActionData(users);
        log.debug("actionData=" + modVo.getActionData());
		// Add the data for viewing
        setAttribute(Constants.MODULE_DATA, modVo);
    }

    public void build(SMTServletRequest req) throws ActionException {
    	StringBuilder message = new StringBuilder();
    	StringBuilder url = new StringBuilder();
    	message.append(req.getParameter("submitter")).append(" has shared the case for vehicle ");
    	message.append(req.getParameter("vin")).append(" with you.");
    	url.append(req.getParameter("site"));
    	url.append("?vehicleId=");
    	url.append(req.getParameter("vehicleId"));
    	
    	SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
    	try {
    		MessageSender ms = new MessageSender(attributes, dbConn);
	    	EmailMessageVO msg = new EmailMessageVO();
	    	msg.setHtmlBody(message.toString() + "<br/><br/><a href='http:/" + url.toString() + "'>" + url.toString() + "</a><br/><br/>" + req.getParameter("comment") + "<br/><br/>This is an automated message.  Please do not respond.");
	    	msg.setTextBody(message.toString() + "\n\n"+ url.toString() +"\n" + req.getParameter("comment") + "\n\nThis is an automated message.  Please do not respond.");
	    	msg.addRecipient(req.getParameter("rcptNm").split("\\|")[0]);
	    	msg.setSubject("Venture RV Case");
	    	msg.setFrom(site.getMainEmail());
			ms.sendMessage(msg);
			log.debug(msg.getHtmlBody());
			log.debug(msg.getTextBody());
			OverviewAction ticket = new OverviewAction();
			ticket.setDBConnection(dbConn);
			ticket.setAttributes(attributes);
			ticket.logActivity(req, "Shared case with: " + req.getParameter("rcptNm").split("\\|")[1]);

		} catch (InvalidDataException e) {
			log.error("Invalid email address ", e);
		}
	
    }

}
