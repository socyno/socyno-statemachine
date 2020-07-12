package com.socyno.stateform.authority;

import lombok.Getter;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import com.socyno.stateform.service.StateFormService;

@Getter
public class AuthorityStateFormParser implements ApplicationListener<ContextRefreshedEvent> {
    public void onApplicationEvent(ContextRefreshedEvent event)  {
        // 判断SPRING容器是否加载完成
        if (event.getApplicationContext().getParent() == null) {
            try {
                StateFormService.parseStateFormRegister();
            } catch (Exception e) {
                throw new Error("加载或解析通用流程表单数据失败", e);
            }
        }
    }
}
