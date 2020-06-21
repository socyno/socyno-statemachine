package com.socyno.stateform.abs;

import java.util.Map;

import com.github.reinert.jjschema.SchemaIgnore;
import com.socyno.base.bscsqlutil.AbstractSqlStatement;

import lombok.Data;
import lombok.Getter;
import lombok.AccessLevel;

@Data
public abstract class AbstractStateFormQuery {
    @Getter(AccessLevel.NONE)
    private long page = 1;
    
    @Getter(AccessLevel.NONE)
    private int  limit = 20;
    
    public AbstractStateFormQuery() {
        this(null, null);
    }
    
    public AbstractStateFormQuery(Integer limit) {
        this(limit, null);
    }
    
    public AbstractStateFormQuery(Integer limit, Long page) {
        if (page != null && page > 0) { 
            this.page = page;
        }
        if (limit != null && limit > 0) { 
            this.limit = limit;
        }
    }
    
    public abstract AbstractSqlStatement prepareSqlTotal() throws Exception;
    public abstract AbstractSqlStatement prepareSqlQuery() throws Exception;
    
    public final int getLimit() {
        return limit <= 0 ? 20 : limit;
    }
    
    public final long getPage() {
        return (page <= 0 ? 1 : page);
    }
    
    @SchemaIgnore
    public final long getOffset() {
        return (getPage()  - 1) * getLimit();
    }
    
    @SchemaIgnore
    public Map<String, String> getFieldMapper() {
        return null;
    }
}
