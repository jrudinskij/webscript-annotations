package com.jr;

import java.io.IOException;

import org.springframework.extensions.webscripts.WebScriptResponse;

import com.github.dynamicextensionsalfresco.webscripts.annotations.HttpMethod;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Lifecycle;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;

@WebScript(families={"joxoxo"}, description="joxoxo description", defaultFormat = "json", lifecycle=Lifecycle.SAMPLE)
public class JrWebScript {

	@Uri(value="/dynamic-extensions/examples/categories", method = HttpMethod.GET)
	public void doIt(final WebScriptResponse response) throws IOException{
		response.getWriter().write("hello world");
	}
}