package com.rezdox.data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;


/****************************************************************************
 * <b>Title</b>: BusinessCategoryList.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> list that looks up business categories and sub categories
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Mar 6, 2018
 * @updates:
 ****************************************************************************/
public class BusinessCategoryList extends SimpleActionAdapter {

	public BusinessCategoryList() {
		super();
	}

	public BusinessCategoryList(ActionInitVO arg0) {
		super(arg0);
	}

	/**
	 * @param dbConnection
	 * @param attributes
	 */
	public BusinessCategoryList(SMTDBConnection dbConnection, Map<String, Object> attributes) {
		this();
		setDBConnection(dbConnection);
		setAttributes(attributes);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		List<GenericVO> data = null;

		if (req.hasParameter("loadAll")) {
			data = retrieveAllCategories();
		} else {
			data = retrieveCategories(req.getParameter("businessCat"));
		}
		putModuleData(data, data.size(),false);
	}

	/**
	 *load the VOs into Nodes and onto a Tree for sorting.  Return the sorted list
	 * @param data
	 * @return
	 */
	private List<GenericVO> retrieveAllCategories() {
		StringBuilder sql = new StringBuilder(150);
		sql.append("select business_category_cd, parent_cd, category_nm from ");
		sql.append(getCustomSchema()).append("rezdox_business_category order by category_nm");
		log.debug(sql);

		List<Node> nodes = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Node n = new Node(rs.getString(1), rs.getString(2));
				n.setNodeName(rs.getString(3));
				nodes.add(n);
			}

		} catch (SQLException sqle) {
			log.error("could not load all categories", sqle);
		}

		//sort the list using a Tree.
		//turn the list of Nodes into a list of GenericVOs, so the return type is consistent.
		nodes = new Tree(nodes).preorderList();
		List<GenericVO> data = new ArrayList<>(nodes.size());
		for (Node n : nodes) {
			String name = n.getNodeName();
			//indent child records
			if (!StringUtil.isEmpty(n.getParentId())) name = "\t" + name;
			
			data.add(new GenericVO(n.getNodeId(), name));
		}

		return data;
	}


	/**
	 * Reusable retrieve to get the list outside of a Request
	 * @param parentCode
	 * @return
	 */
	public List<GenericVO> retrieveCategories(String parentCode) {
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select business_category_cd as key, category_nm as value from ");
		sql.append(schema).append("rezdox_business_category ");

		if (!StringUtil.isEmpty(parentCode)) {
			sql.append("where parent_cd = ? ");
			sql.append("order by category_nm ");
			params.add(parentCode);
		} else {
			sql.append("where parent_cd is null ");
			sql.append("order by order_no ");
		}

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		List<GenericVO> data = db.executeSelect(sql.toString(), params, new GenericVO());
		log.debug("sql " + sql.toString() + " params " + params + " size " + data.size());
		return data;
	}
}
