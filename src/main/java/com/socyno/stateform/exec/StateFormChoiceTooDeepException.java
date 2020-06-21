package com.socyno.stateform.exec;

import com.socyno.base.bscexec.MessageException;
import com.socyno.stateform.abs.AbstractStateChoice;

public class StateFormChoiceTooDeepException extends MessageException {

	private static final long serialVersionUID = 1L;

	public StateFormChoiceTooDeepException(AbstractStateChoice choice) {
		super(String.format("表单流程定义异常，选择器(%s)递归深度超限，可能出现了死循环。",
				choice.getDisplay()));
	}
}
