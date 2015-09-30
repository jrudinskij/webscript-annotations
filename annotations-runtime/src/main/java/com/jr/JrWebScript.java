package com.jr;

import java.io.IOException;

import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.github.dynamicextensionsalfresco.webscripts.annotations.HttpMethod;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Lifecycle;
import com.github.dynamicextensionsalfresco.webscripts.annotations.RequestParam;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;

@Component
@WebScript(families={"joxoxo"}, description="joxoxo sss description", defaultFormat = "json")
public class JrWebScript {

	@Uri(value="/dynamic-extensions/examples/categories", method = HttpMethod.GET)
	public void doIt(@RequestParam(required=false) final String param, final WebScriptResponse response) throws IOException{
	    System.out.println("This is param: " + param);
		response.getWriter().write("hello world");
	}
}