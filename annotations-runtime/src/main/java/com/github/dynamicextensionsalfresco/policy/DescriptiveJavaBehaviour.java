package com.github.dynamicextensionsalfresco.policy;

import java.lang.reflect.Method;

import org.alfresco.repo.policy.JavaBehaviour;

public class DescriptiveJavaBehaviour extends JavaBehaviour{
	
	private Method method;

	public DescriptiveJavaBehaviour(Object instance, Method method, NotificationFrequency frequency) {
		super(instance, method.getName(), frequency);
		this.method = method;
	}

	public String toString() {
        return method.getDeclaringClass().getName() + "." + method.getName();
    }
}
