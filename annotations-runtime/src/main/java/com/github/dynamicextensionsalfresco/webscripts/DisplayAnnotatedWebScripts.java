package com.github.dynamicextensionsalfresco.webscripts;

import java.util.HashMap;
import java.util.Map;

import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class DisplayAnnotatedWebScripts extends DeclarativeWebScript {
    
    private WebScriptUriRegistry webScriptUriRegistry;
    
    public void setWebScriptUriRegistry(WebScriptUriRegistry webScriptUriRegistry) {
        this.webScriptUriRegistry = webScriptUriRegistry;
    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache){
		Map<String, Object> model = new HashMap<String, Object>();
        model.put("webScripts", webScriptUriRegistry.getWebScripts());
        return model;
	}

}
