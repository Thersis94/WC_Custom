package com.depuysynthes.action;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;

public class MIRSubmissionVO extends UserDataVO {

	private static final long serialVersionUID = -8316947595121339926L;



	public MIRSubmissionVO() {
		super();
	}

	public MIRSubmissionVO(ActionRequest req) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String toString() {
		return StringUtil.getToString(this, false, 0, "|");
	}

}