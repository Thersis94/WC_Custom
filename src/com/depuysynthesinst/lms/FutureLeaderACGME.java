package com.depuysynthesinst.lms;

/****************************************************************************
 * <b>Title</b>: FutureLeaderACGME.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 21, 2015
 ****************************************************************************/
public class FutureLeaderACGME {

	/*
	 * the key names here really have no meaning, since we can't put the "-" in them.  use getCode() instead
	 * key(String ACGME Code, String full name, String hierarchy as seen by Solr, String wcPageAlias for this Anatomy)
	 *
	 * Added NURSE Key per DSI Phase IIC requirements.
	 */
	public enum ACGME {
		GEN("GEN", "General Principles & Fundamentals","Future Leaders~General Principles & Fundamentals","general"),
		ORTH("ORTH","Orthopaedics","Future Leaders~Orthopaedics","orthopaedics"),
		ORTHTRA("ORTH-TRA", "Trauma","Future Leaders~Orthopaedics~Trauma","orthpaedics"),
		ORTHSE("ORTH-SE", "Shoulder & Elbow","Future Leaders~Orthopaedics~Shoulder & Elbow","orthpaedics"),
		ORTHHW("ORTH-HW", "Hand & Wrist","Future Leaders~Orthopaedics~Hand & Wrist","orthpaedics"),
		ORTHSP("ORTH-SP", "Spine","Future Leaders~Orthopaedics~Spine","orthpaedics"),
		ORTHCW("ORTH-CW", "Chest Wall","Future Leaders~Orthopaedics~Chest Wall","orthpaedics"),
		ORTHHIP("ORTH-HIP", "Hip","Future Leaders~Orthopaedics~Hip","orthpaedics"),
		ORTHKNE("ORTH-KNE", "Knee","Future Leaders~Orthopaedics~Knee","orthpaedics"),
		ORTHFTA("ORTH-FTA", "Foot & Ankle","Future Leaders~Orthopaedics~Foot & Ankle","orthpaedics"),
		MSO("MSO", "Musculoskeletal Oncology","Future Leaders~Musculoskeletal Oncology","musculoskeletal"),
		NES("NES", "Neurological Surgery","Future Leaders~Neurological Surgery","neurological"),
		OTO("OTO", "Otolaryngology","Future Leaders~Otolaryngology","otolaryngology"),
		OMS("OMS", "Oral Maxillofacial Surgery","Future Leaders~Oral Maxillofacial Surgery","oral-maxillofacial"),
		PRS("PRS", "Plastic & Reconstructive Surgery","Future Leaders~Plastic & Reconstructive Surgery","plastic-reconstructive"),
		POD("POD", "Podiatric Medicine","Future Leaders~Podiatric Medicine","podiatric"),
		VET("VET", "Veterinary Medicine","Future Leaders~Veterinary Medicine","veterinary"),
		NURSE("NURSE", "Nursing Education", "Nurse Education~Continuing Education", "nursing-education");


		private String code;
		private String name;
		private String hierarchy;
		private String pageAlias;
		
		private ACGME(String cd, String n, String h, String a) {
			this.code = cd;
			this.name = n;
			this.hierarchy = h;
			this.pageAlias = a;
		}

		public String getCode() { return code; }
		public String getName() { return name; }
		public String getHierarchy() { return hierarchy; }
		public String getPageAlias() { return pageAlias; }
	}
	
	
	/**
	 * returns a Map<code, name> from the ACGME enum
	 * @return
	
	public static Map<String, String> getCodeNameMap() {
		Map<String, String> data = new HashMap<>();
		for (ACGME tok : ACGME.values())
			data.put(tok.getCode(), tok.getName());
		
		return data;
	}
	 */
	public static String getCodeFromAlias(String pgAlias) {
		for (ACGME tok : ACGME.values()) {
			if (tok.getPageAlias().equals(pgAlias))
				return tok.getCode();
		}
		return null;
	}
	
	public static String getNameFromCode(String code) {
		for (ACGME tok : ACGME.values()) {
			if (tok.getCode().equals(code))
				return tok.getName();
		}
		return null;
	}
	
	public static String getHierarchyFromCode(String code) {
		for (ACGME tok : ACGME.values()) {
			if (tok.getCode().equals(code))
				return tok.getHierarchy();
		}
		return null;
	}
}
