package com.dianjinshou.modules.storage.vo;

import java.util.HashMap;
import java.util.Map;

public class OpenTargetVO {

    private String target;
    private String routeName;
    private Map<String, Object> params = new HashMap<String, Object>();
    private Boolean readonly;

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }

    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }

    public Boolean getReadonly() { return readonly; }
    public void setReadonly(Boolean readonly) { this.readonly = readonly; }
}
