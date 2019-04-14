package com.mts.action;

// JDK 1.8.x
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// MTS Imports
import com.mts.admin.action.UserAction;
import com.mts.publication.action.IssueAction;
import com.mts.publication.action.PublicationAction;
import com.mts.publication.data.AssetVO.AssetType;
import com.mts.publication.data.CategoryVO;
import com.mts.publication.data.IssueVO;
import com.mts.publication.data.PublicationVO;
import com.mts.subscriber.data.MTSUserVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: SelectLookupAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Provides a mechanism for looking up key/values for select lists.
 * Each type will return a collection of GenericVOs, which will automatically be
 * available in a select picker
 * as listType
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 4, 2019
 * @updates:
 ****************************************************************************/
public class SelectLookupAction extends SBActionAdapter {

	/**
	 * Key to be passed to utilize this action
	 */
	public static final String SELECT_KEY = "selectType";

	/**
	 * Req value passed form a BS Table search
	 */
	public static final String REQ_SEARCH = "search";

		/**
	 * Assigns the keys for the select type to method mapping.  In the generic vo
	 * the key is the method name.  The value is a boolean which indicates whether
	 * or not the request object is needed in that method 
	 */
	private static Map<String, GenericVO> keyMap = new HashMap<>(16);
	static {
		keyMap.put("activeFlag", new GenericVO("getYesNoLookup", Boolean.FALSE));
		keyMap.put("role", new GenericVO("getRoles", Boolean.FALSE));
		keyMap.put("prefix", new GenericVO("getPrefix", Boolean.FALSE));
		keyMap.put("gender", new GenericVO("getGenders", Boolean.FALSE));
		keyMap.put("category", new GenericVO("getCategories", Boolean.FALSE));
		keyMap.put("users", new GenericVO("getUsers", Boolean.TRUE));
		keyMap.put("editors", new GenericVO("getEditors", Boolean.FALSE));
		keyMap.put("assetTypes", new GenericVO("getAssetTypes", Boolean.FALSE));
		keyMap.put("publications", new GenericVO("getPublications", Boolean.FALSE));
		keyMap.put("issues", new GenericVO("getIssues", Boolean.TRUE));
	}

	/**
	 * 
	 */
	public SelectLookupAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public SelectLookupAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String listType = req.getStringParameter(SELECT_KEY);
		GenericVO vo = keyMap.get(listType);
		
		// If the key is not found, throw a json error
		if (vo == null) {
			putModuleData(null, 0, false, "List Type Not Found in KeyMap", true);
			return;
		}

		try {
			if (Convert.formatBoolean(vo.getValue())) {
				Method method = this.getClass().getMethod(vo.getKey().toString(), req.getClass());
				putModuleData(method.invoke(this, req));
			} else {
				Method method = this.getClass().getMethod(vo.getKey().toString());
				putModuleData(method.invoke(this));
			}

		} catch (Exception e) {
			log.error("Unable to retrieve list: " + listType, e);
		}
	}

	/**
	 * Load a yes no list
	 * @return
	 */
	public List<GenericVO> getYesNoLookup() {
		List<GenericVO> yesNo = new ArrayList<>();

		yesNo.add(new GenericVO("1","Yes"));
		yesNo.add(new GenericVO("0","No"));

		return yesNo;
	}

	/**
	 * Gets the supported locales for the app
	 * @return
	 */
	public List<GenericVO> getRoles() {
		List<GenericVO> data = new ArrayList<>(8);
		/*
		for (RPRole val : RPConstants.RPRole.values()) {
			data.add(new GenericVO(val.getRoleId(), val.getRoleName()));
		}
		*/
		return data;
	}
	
	/**
	 * Retruns a list of user prefixes
	 * @return
	 */
	public List<GenericVO> getPrefix() {
		List<GenericVO> selectList = new ArrayList<>(8);
		selectList.add(new GenericVO("Mr.", "Mr."));
		selectList.add(new GenericVO("Mrs.", "Mrs."));
		selectList.add(new GenericVO("Ms", "Ms."));
		selectList.add(new GenericVO("Miss", "Miss"));

		return selectList;
	}
	
	/**
	 * Gets the supported genders for the app
	 * @return
	 */
	public List<GenericVO> getGenders() {
		List<GenericVO> data = new ArrayList<>(8);
		data.add(new GenericVO("F", "Female"));
		data.add(new GenericVO("M", "Male"));

		return data;
	}
	
	/**
	 * Gets a list of attribute categories
	 * @return
	 */
	public List<GenericVO> getCategories() {
		List<GenericVO> data = new ArrayList<>(64);
		StringBuilder sql = new StringBuilder(128);
		sql.append("select * from ").append(getCustomSchema());
		sql.append("mts_category order by group_cd, parent_cd desc");
		
		DBProcessor db = new DBProcessor(getDBConnection());
		List<CategoryVO> cats = db.executeSelect(sql.toString(), null, new CategoryVO());
		for (CategoryVO cat : cats) {
			if (StringUtil.isEmpty(cat.getParentCode())) 
				data.add(new GenericVO(null, cat.getName()));
			else 
				data.add(new GenericVO(cat.getCategoryCode(), cat.getName()));
		}
		
		return data;
	}

	/**
	 * Returns a list of users
	 * @param req
	 * @return
	 */
	public List<GenericVO> getUsers(ActionRequest req) {
		List<GenericVO> data = new ArrayList<>(10);
		
		return data;
	}
	
	/**
	 * Gets a list of the editors in the system
	 * @return
	 */
	public List<GenericVO> getEditors() {
		List<GenericVO> data = new ArrayList<>(16);
		UserAction ua = new UserAction(getDBConnection(), getAttributes());
		List<MTSUserVO> editors = ua.getEditors();
		
		for (MTSUserVO editor : editors) {
			data.add(new GenericVO(editor.getUserId(), editor.getFullName()));
		}
		
		return data;
	}
	
	/**
	 * gets a lit of asset types for uploading of assets
	 * @return
	 */
	public List<GenericVO> getAssetTypes() {
		List<GenericVO> data = new ArrayList<>(16);
		
		for (AssetType at : AssetType.values()) {
			data.add(new GenericVO(at.name(), at.getAssetName()));
		}
		
		Collections.sort(data, (a, b) -> ((String)a.getValue()).compareTo((String)b.getValue()));
		
		return data;
	}
	
	/**
	 * gets a list of publications
	 * @return
	 */
	public List<GenericVO> getPublications() {
		List<GenericVO> data = new ArrayList<>(16);
		
		PublicationAction pa = new PublicationAction(getDBConnection(), getAttributes());
		List<PublicationVO> pubs = pa.getPublications();
		for (PublicationVO pub : pubs) {
			if (pub.getNumberIssues() > 0) 
				data.add(new GenericVO(pub.getPublicationId(), pub.getName()));
		}
		
		return data;
	}
	
	/**
	 * Gets a list of issues for a given publication
	 * @return
	 */
	public List<GenericVO> getIssues(ActionRequest req) {
		List<GenericVO> data = new ArrayList<>(16);
		BSTableControlVO bst = new BSTableControlVO(req, IssueVO.class);
		bst.setLimit(1000);
		String publicationId = req.getParameter("publicationId");
		
		IssueAction ia = new IssueAction(getDBConnection(), getAttributes());
		GridDataVO<IssueVO> issues = ia.getIssues(publicationId, bst);
		for (IssueVO issue : issues.getRowData()) {
			data.add(new GenericVO(issue.getIssueId(), issue.getName()));
		}
		
		return data;
	}

}
