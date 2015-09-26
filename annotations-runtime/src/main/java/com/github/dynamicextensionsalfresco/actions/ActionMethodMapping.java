package com.github.dynamicextensionsalfresco.actions;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.rule.RuleServiceException;
import org.springframework.util.ReflectionUtils;
import org.alfresco.service.cmr.action.Action;

public class ActionMethodMapping {
	
	private Object bean;
	private Method method;
	
	private int nodeRefParameterIndex = -1;

	private int actionParameterIndex = -1;

    private int parameterCount;

    private Map<String, ParameterMapping> parameterMappingsByName = new HashMap<>();
	
	public ActionMethodMapping(Object bean, Method method) {
		super();
		this.bean = bean;
		this.method = method;
		this.parameterCount = method.getParameterTypes().length;
	}
	
	public int getActionParameterIndex() {
		return actionParameterIndex;
	}

	public void setActionParameterIndex(int actionParameterIndex) {
		this.actionParameterIndex = actionParameterIndex;
	}

	public int getNodeRefParameterIndex() {
		return nodeRefParameterIndex;
	}

	public void setNodeRefParameterIndex(int nodeRefParameterIndex) {
		this.nodeRefParameterIndex = nodeRefParameterIndex;
	}

	public void invokeActionMethod(Action action, NodeRef nodeRef) {
        Object[] parameters = new Object[parameterCount];
        if (nodeRefParameterIndex > -1) {
            parameters[nodeRefParameterIndex] = nodeRef;
        }
        if (actionParameterIndex > -1) {
            parameters[actionParameterIndex] = action;
        }
        for (Entry<String, ParameterMapping> entry : parameterMappingsByName.entrySet()) {
        	ParameterMapping parameterMapping = entry.getValue();
            Serializable value = action.getParameterValue(parameterMapping.getName());
            if (parameterMapping.isMandatory() && value == null) {
                /*
                 * We throw RuleServiceException just as ParameterizedItemAbstractBase does when it encounters a missing
                 * value for a mandatory property.
                 */
                throw new RuleServiceException("Parameter '${parameterMapping.getName()}' is mandatory, but no value was given.");
            }
            /* Single values for a multi-valued property are wrapped in an ArrayList automatically. */
            if (parameterMapping.isMultivalued() && (value instanceof Collection<?>) == false) {
                value = new ArrayList<>(Arrays.asList(value));
            }
            parameters[parameterMapping.getIndex()] = value;
        }

        ReflectionUtils.invokeMethod(method, bean, parameters);
    }

    public Boolean hasParameter(String name) {
        return parameterMappingsByName.containsKey(name);
    }

    public void addParameterMapping(ParameterMapping parameterMapping) {
        String name = parameterMapping.getName();
        if (parameterMappingsByName.containsKey(name) == false) {
            parameterMappingsByName.put(name, parameterMapping);
        } else {
            throw new IllegalStateException("Duplicate parameter name '$name'.");
        }
    }

}
