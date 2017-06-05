/**
 *
 */
package com.biomed.smarttrak.vo;

import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: GapProductVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Data VO for holding Gap Analysis Product List Info.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Feb 21, 2017
 ****************************************************************************/
@Table(name="BIOMEDGPS_COMPANY")
public class GapProductVO {

	private String companyNm;
	private String sectionNm;
	private String sectionParentNm;
	private String columnNm;
	private List<ProductVO> products;

	public GapProductVO() {
		this.products = new ArrayList<>();
	}

	/**
	 * @return the companyNm
	 */
	@Column(name="company_nm")
	public String getCompanyNm() {
		return companyNm;
	}

	/**
	 * @return the sectionGrandParentNm
	 */
	@Column(name="section_parent_nm")
	public String getSectionParentNm() {
		return sectionParentNm;
	}

	/**
	 * @return the sectionParentNm
	 */
	@Column(name="section_nm")
	public String getSectionNm() {
		return sectionNm;
	}

	/**
	 * @return the columnNm
	 */
	@Column(name="column_nm")
	public String getColumnNm() {
		return columnNm;
	}

	/**
	 * @return the products
	 */
	public List<ProductVO> getProducts() {
		return products;
	}

	/**
	 * @param companyNm the companyNm to set.
	 */
	public void setCompanyNm(String companyNm) {
		this.companyNm = companyNm;
	}

	/**
	 * @param sectionGrandParentNm the sectionGrandParentNm to set.
	 */
	public void setSectionNm(String sectionNm) {
		this.sectionNm = sectionNm;
	}

	/**
	 * @param sectionParentNm the sectionParentNm to set.
	 */
	public void setSectionParentNm(String sectionParentNm) {
		this.sectionParentNm = sectionParentNm;
	}

	/**
	 * @param columnNm the columnNm to set.
	 */
	public void setColumnNm(String columnNm) {
		this.columnNm = columnNm;
	}

	/**
	 * @param products the products to set.
	 */
	public void setProducts(List<ProductVO> products) {
		this.products = products;
	}

	@BeanSubElement()
	public void addProduct(ProductVO p) {
		if(p != null) {
			products.add(p);
		}
	}
}
