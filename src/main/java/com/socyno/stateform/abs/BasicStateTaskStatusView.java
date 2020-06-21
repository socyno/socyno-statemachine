package com.socyno.stateform.abs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.v1.FieldSimpleOption;
import com.github.reinert.jjschema.v1.FieldType;
import com.socyno.base.bscfield.FieldTextDelta;
import com.socyno.base.bscmodel.SimpleLock;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class BasicStateTaskStatusView {
    
    public static enum TaskStatus {
        PENDING,
        RUNNING,
        FINISHED
        ;
        
        public String getValue( ) {
            return name().toLowerCase();
        }
        
        public static TaskStatus fromLock(@NonNull SimpleLock lock) {
            if (lock.getLocked() == null) {
                return FINISHED;
            }
            if (lock.getLocked() == 0) {
                return PENDING;
            }
            return RUNNING;
        }
        
        public static String valueFromLock(@NonNull SimpleLock lock) {
            return fromLock(lock).getValue();
        }
    }
    
    public static enum TaskResult {
        UNKNOWN,
        SUCCESS,
        FAILURE
        ;
        
        public String getValue( ) {
            return name().toLowerCase();
        }
        
        public static TaskResult fromLock(@NonNull SimpleLock lock) {
            if (lock.getResult() == null) {
                return UNKNOWN;
            }
            if (lock.getResult()) {
                return SUCCESS;
            }
            return FAILURE;
        }
        
        public static String valueFromLock(@NonNull SimpleLock lock) {
            return fromLock(lock).getValue();
        }
    }
    
    public static class FieldOptionsRunning extends FieldType {
    	
    	private final static FieldOptionsRunning INSTANCE = new FieldOptionsRunning();
        
        public static FieldOptionsRunning getInstance() {
            return INSTANCE;
        }
        
        @SuppressWarnings("serial")
        private final static List<FieldSimpleOption> options = new ArrayList<FieldSimpleOption>() {{
            add(FieldSimpleOption.create(TaskStatus.PENDING.getValue(),     "待运行"));
            add(FieldSimpleOption.create(TaskStatus.RUNNING.getValue(),     "运行中"));
            add(FieldSimpleOption.create(TaskStatus.FINISHED.getValue(),    "已结束"));
        }};
        @Override
        public FieldOptionsType getOptionsType() {
            return FieldOptionsType.STATIC;
        }
        
        @Override
        public List<FieldSimpleOption> getStaticOptions() {
            return Collections.unmodifiableList(options);
        }
    }
    
    public static class FieldOptionsResult extends FieldType {
    	
    	private final static FieldOptionsResult INSTANCE = new FieldOptionsResult();
        
        public static FieldOptionsResult getInstance() {
            return INSTANCE;
        }
        
        @SuppressWarnings("serial")
        private final static List<FieldSimpleOption> options = new ArrayList<FieldSimpleOption>() {{
            add(FieldSimpleOption.create(TaskResult.UNKNOWN.getValue(),   "待定"));
            add(FieldSimpleOption.create(TaskResult.SUCCESS.getValue(),   "成功"));
            add(FieldSimpleOption.create(TaskResult.FAILURE.getValue(),   "失败"));
        }};
        
        @Override
        public FieldOptionsType getOptionsType() {
            return FieldOptionsType.STATIC;
        }
        
        @Override
        public List<FieldSimpleOption> getStaticOptions() {
            return Collections.unmodifiableList(options);
        }
    }
    
    @Attributes(title = "编号")
    private Long id;
    
    @Attributes(title = "标题", position = 1010)
    private String title;
    
    @Attributes(title = "状态", position = 1020, type = FieldOptionsRunning.class)
    private String status;
    
    @Attributes(title = "结果", position = 1030, type = FieldOptionsResult.class)
    private String result;
    
    @Attributes(title = "类型")
    private String targetType;
    
    @Attributes(title = "标的")
    private String targetId;
    
    @Attributes(title = "创建时间", position = 1060)
    private Date createdAt;
    
    @Attributes(title = "创建人", position = 1070)
    private String createdBy;
    
    @Attributes(title = "结束时间", position = 1080)
    private Date completedAt;

    @Attributes(title = "实时日志", position = 2000, type = FieldTextDelta.class)
    private String logsTextDelta;
}