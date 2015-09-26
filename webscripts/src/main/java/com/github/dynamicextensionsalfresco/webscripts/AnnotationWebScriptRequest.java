package com.github.dynamicextensionsalfresco.webscripts;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.extensions.surf.util.Content;
import org.springframework.extensions.webscripts.Description.FormatStyle;
import org.springframework.extensions.webscripts.Match;
import org.springframework.extensions.webscripts.Runtime;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WrappingWebScriptRequest;

public class AnnotationWebScriptRequest implements WrappingWebScriptRequest{ //WebScriptRequest, 
	
	private WebScriptRequest webScriptRequest;
	private Map<String, Object> model = new LinkedHashMap<>();
	private Throwable thrownException;
	
	public AnnotationWebScriptRequest(WebScriptRequest webScriptRequest) {
		this.webScriptRequest = webScriptRequest;
	}
	
	public void setThrownException(Throwable thrownException) {
		this.thrownException = thrownException;
	}

	public WebScriptRequest getWebScriptRequest() {
		return webScriptRequest;
	}

	public Throwable getThrownException() {
		return thrownException;
	}

	public Map<String, Object> getModel() {
		return model;
	}

	public String toString()  {
        return getNext().toString();
    }

    public WebScriptRequest getNext() {
        if (webScriptRequest instanceof WrappingWebScriptRequest) {
            return ((WrappingWebScriptRequest)webScriptRequest).getNext();
        }
        return webScriptRequest;
    }

	public Match getServiceMatch() {
		return webScriptRequest.getServiceMatch();
	}

	public String getServerPath() {
		return webScriptRequest.getServerPath();
	}

	public String getContextPath() {
		return webScriptRequest.getContextPath();
	}

	public String getServiceContextPath() {
		return webScriptRequest.getServiceContextPath();
	}

	public String getServicePath() {
		return webScriptRequest.getServicePath();
	}

	public String getURL() {
		return webScriptRequest.getURL();
	}

	public String getPathInfo() {
		return webScriptRequest.getPathInfo();
	}

	public String getQueryString() {
		return webScriptRequest.getQueryString();
	}

	public String[] getParameterNames() {
		return webScriptRequest.getParameterNames();
	}

	public String getParameter(String name) {
		return webScriptRequest.getParameter(name);
	}

	public String[] getParameterValues(String name) {
		return webScriptRequest.getParameterValues(name);
	}

	public String[] getHeaderNames() {
		return webScriptRequest.getHeaderNames();
	}

	public String getHeader(String name) {
		return webScriptRequest.getHeader(name);
	}

	public String[] getHeaderValues(String name) {
		return webScriptRequest.getHeaderValues(name);
	}

	public String getExtensionPath() {
		return webScriptRequest.getExtensionPath();
	}

	public String getContentType() {
		return webScriptRequest.getContentType();
	}

	public Content getContent() {
		return webScriptRequest.getContent();
	}

	public Object parseContent() {
		return webScriptRequest.parseContent();
	}

	public boolean isGuest() {
		return webScriptRequest.isGuest();
	}

	public String getFormat() {
		return webScriptRequest.getFormat();
	}

	public FormatStyle getFormatStyle() {
		return webScriptRequest.getFormatStyle();
	}

	public String getAgent() {
		return webScriptRequest.getAgent();
	}

	public String getJSONCallback() {
		return webScriptRequest.getJSONCallback();
	}

	public boolean forceSuccessStatus() {
		return webScriptRequest.forceSuccessStatus();
	}

	public Runtime getRuntime() {
		return webScriptRequest.getRuntime();
	}
}
