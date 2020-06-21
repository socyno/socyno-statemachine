package com.socyno.stateform.service;

import com.socyno.base.bscservice.AbstractLogService;
import com.socyno.base.bscsqlutil.AbstractDao;

import lombok.Getter;

public class SimpleLogService extends AbstractLogService {

	protected AbstractDao getDao() {
		return TenantSpecialDataSource.getMain();
	}
	
	@Getter
    private final static SimpleLogService instance = new SimpleLogService();
}
