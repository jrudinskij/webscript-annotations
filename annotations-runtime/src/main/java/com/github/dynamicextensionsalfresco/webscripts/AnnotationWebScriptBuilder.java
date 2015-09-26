package com.github.dynamicextensionsalfresco.webscripts;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.extensions.webscripts.Description;
import org.springframework.extensions.webscripts.Description.RequiredAuthentication;
import org.springframework.extensions.webscripts.Description.RequiredTransaction;
import org.springframework.extensions.webscripts.Description.TransactionCapability;
import org.springframework.extensions.webscripts.DescriptionImpl;
import org.springframework.extensions.webscripts.TransactionParameters;
import org.springframework.extensions.webscripts.WebScript;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import com.github.dynamicextensionsalfresco.webscripts.annotations.Attribute;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication;
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Before;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Cache;
import com.github.dynamicextensionsalfresco.webscripts.annotations.ExceptionHandler;
import com.github.dynamicextensionsalfresco.webscripts.annotations.FormatStyle;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Lifecycle;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Transaction;
import com.github.dynamicextensionsalfresco.webscripts.annotations.TransactionType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.arguments.HandlerMethodArgumentsResolver;

public class AnnotationWebScriptBuilder implements BeanFactoryAware{
	
	private ConfigurableListableBeanFactory beanFactory;
	private HandlerMethodArgumentsResolver handlerMethodArgumentsResolver;
	private String trailingSlashExpression = "/$";
	private String leadingSlashExpression = "^/";

	public AnnotationWebScriptBuilder(HandlerMethodArgumentsResolver handlerMethodArgumentsResolver) {
		this.handlerMethodArgumentsResolver = handlerMethodArgumentsResolver;
	}
	

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	public List<WebScript> createWebScripts(String beanName) {
		
//		Consumer<String> logger = s ->{};
//		if("JrWebScript".equals(beanName)){
//			logger = s -> System.out.println("\n\n !!!!!!!  " + s + "  \n");
//		}
		Class<?> beanType = beanFactory.getType(beanName);
		if(beanType == null){
			return Collections.emptyList();
		}
		com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript webScriptAnnotation =
				beanFactory.findAnnotationOnBean(beanName, com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript.class);
		if(webScriptAnnotation == null){
			return Collections.emptyList();
		}
		
		String baseUri = webScriptAnnotation.baseUri();
		if (StringUtils.hasText(baseUri) && baseUri.startsWith("/") == false) {
            throw new RuntimeException("@WebScript baseUri for class '" + beanType.getName() + "' does not start with a slash: '"+baseUri+"'");
        }
		
		HandlerMethods handlerMethods = new HandlerMethods();
		ReflectionUtils.doWithMethods(beanType, (method) -> {
			Before before = AnnotationUtils.findAnnotation(method, Before.class);
			if(before == null){
				return;
			}
			if (AnnotationUtils.findAnnotation(method, Attribute.class) != null || AnnotationUtils.findAnnotation(method, Uri.class) != null) {
                throw new RuntimeException("Cannot combine @Before, @Attribute and @Uri on a single method. Method: " + ClassUtils.getQualifiedMethodName(method));
            }
            handlerMethods.getBeforeMethods().add(method);
		});
		
		ReflectionUtils.doWithMethods(beanType, (method) -> {
			Attribute attribute = AnnotationUtils.findAnnotation(method, Attribute.class);
			if(attribute == null){
				return;
			}
			if (AnnotationUtils.findAnnotation(method, Before.class) != null || AnnotationUtils.findAnnotation(method, Uri.class) != null) {
                throw new RuntimeException("Cannot combine @Before, @Attribute and @Uri on a single method. Method: " + ClassUtils.getQualifiedMethodName(method));
            }
			if (method.getReturnType() == Void.TYPE) {
                throw new RuntimeException("@Attribute methods cannot have a void return type.");
            }
            handlerMethods.getBeforeMethods().add(method);
		});
		
		ReflectionUtils.doWithMethods(beanType, (method) -> {
			ExceptionHandler exceptionHandler = AnnotationUtils.findAnnotation(method, ExceptionHandler.class);
			if(exceptionHandler == null){
				return;
			}
			if (AnnotationUtils.findAnnotation(method, Attribute.class) != null || AnnotationUtils.findAnnotation(method, Before.class) != null 
					|| AnnotationUtils.findAnnotation(method, Uri.class) != null) {
                throw new RuntimeException("Cannot combine @Before, @Attribute @ExceptionHandler or @Uri on a single method. Method: " + ClassUtils.getQualifiedMethodName(method));
            }
            handlerMethods.getExceptionHandlerMethods().add(new ExceptionHandlerMethod(exceptionHandler, method));
		});
		
		List<WebScript> webScripts = new ArrayList<>();
		ReflectionUtils.doWithMethods(beanType, (method) -> {
			Uri uri = AnnotationUtils.findAnnotation(method, Uri.class);
			if (uri != null) {
				WebScript webScript = createWebScript(beanName, webScriptAnnotation, uri, handlerMethods.createForUriMethod(method));
                webScripts.add(webScript);
            }
		});

		Set<String> ids = new HashSet<String>();
        for (WebScript webScript : webScripts) {
            String webscriptId = webScript.getDescription().getId();
            boolean notContained = ids.add(webscriptId);
            if (!notContained) {
                throw new IllegalStateException("Duplicate Web Script ID \"" + webscriptId 
                		+ "\" Make sure handler methods of annotation-based Web Scripts have unique names.");
            }
        }
		
		return webScripts;
	}
	
	protected AnnotationWebScript createWebScript(String beanName, com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript webScript, 
			Uri uri, HandlerMethods handlerMethods) {
		DescriptionImpl description = new DescriptionImpl();
        if (StringUtils.hasText(webScript.defaultFormat())) {
            description.setDefaultFormat(webScript.defaultFormat());
        }
        String baseUri = webScript.baseUri();
        handleHandlerMethodAnnotation(uri, handlerMethods.getUriMethod(), description, baseUri);
        handleTypeAnnotations(beanName, webScript, description);
        String id = String.format("%s.%s.%s", generateId(beanName), handlerMethods.getUriMethod().getName(), description.getMethod().toLowerCase());
        description.setId(id);
        Object handler = beanFactory.getBean(beanName);
        description.setStore(new DummyStore());
        return createWebScript(description, handler, handlerMethods);
    }
	
	protected AnnotationWebScript createWebScript(Description description, Object handler, HandlerMethods handlerMethods){
        return new AnnotationWebScript(description, handler, handlerMethods, handlerMethodArgumentsResolver);
    }
	
	protected void handleHandlerMethodAnnotation(Uri uri, Method method, DescriptionImpl description, String baseUri) {
        Assert.notNull(uri, "Uri cannot be null.");
        Assert.notNull(method, "HttpMethod cannot be null.");
        Assert.notNull(description, "Description cannot be null.");

        String[] uris = null;
        if (uri.value().length > 0) {
        	uris = Arrays.asList(uri.value())
	        		.stream()
	        		.map(val -> baseUri.replace(trailingSlashExpression, "") + "/" + val.replaceAll(leadingSlashExpression, ""))
	        		.collect(Collectors.toList())
	        		.toArray(new String[0]);
        } else if (StringUtils.hasText(baseUri)) {
            uris = new String[]{baseUri.replace(trailingSlashExpression, "")};
        } else {
            throw new RuntimeException(
                    "No value specified for @Uri on method '%s' and no base URI found for @WebScript on class."
                            .format(ClassUtils.getQualifiedMethodName(method))
            );
        }
        description.setUris(uris);
        /*
		 * For the sake of consistency we translate the HTTP method from the HttpMethod enum. This also shields us from
		 * changes in the HttpMethod enum names.
		 */
        description.setMethod(uri.method().name());
        /*
		 * Idem dito for FormatStyle.
		 */
        
        description.setFormatStyle(getFormatStyle(uri.formatStyle()));
        if (StringUtils.hasText(uri.defaultFormat())) {
            description.setDefaultFormat(uri.defaultFormat());
        }
        
        description.setMultipartProcessing(uri.multipartProcessing());

        Authentication methodAuthentication = method.getAnnotation(Authentication.class);
        if (methodAuthentication != null) {
            handleAuthenticationAnnotation(methodAuthentication, description);
        }

        Transaction methodTransaction = method.getAnnotation(Transaction.class);
        if (methodTransaction != null) {
            handleTransactionAnnotation(methodTransaction, description);
        }
    }
	
	private org.springframework.extensions.webscripts.Description.FormatStyle getFormatStyle(FormatStyle style){
		if(FormatStyle.ANY == style){
			return org.springframework.extensions.webscripts.Description.FormatStyle.any;
		}
		if(FormatStyle.ARGUMENT == style){
			return org.springframework.extensions.webscripts.Description.FormatStyle.argument;
		}
		if(FormatStyle.EXTENSION == style){
			return org.springframework.extensions.webscripts.Description.FormatStyle.extension;
		}
		return org.springframework.extensions.webscripts.Description.FormatStyle.any;
	}
	
	protected void handleTypeAnnotations(String beanName, com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript webScript, DescriptionImpl description) {
        handleWebScriptAnnotation(webScript, beanName, description);

        if (description.getRequiredAuthentication() == null) {
        	Authentication authentication = beanFactory.findAnnotationOnBean(beanName, Authentication.class);
        	if(authentication == null){
        	   authentication = getDefaultAuthenticationAnnotation();
        	}
            handleAuthenticationAnnotation(authentication, description);
        }

        if (description.getRequiredTransactionParameters() == null) {
        	Transaction transaction = beanFactory.findAnnotationOnBean(beanName, Transaction.class);
            if (transaction == null) {
                if (description.getMethod().equals("GET")) {
                    transaction = getDefaultReadonlyTransactionAnnotation();
                } else {
                    transaction = getDefaultReadWriteTransactionAnnotation();
                }
            }
            handleTransactionAnnotation(transaction, description);
        }

        Cache cache = beanFactory.findAnnotationOnBean(beanName, Cache.class);
        if(cache == null){
        	cache = getDefaultCacheAnnotation();
        }
        handleCacheAnnotation(cache, beanName, description);

        description.setDescPath("");
    }
	
	protected void handleWebScriptAnnotation(com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript webScript,
			String beanName, DescriptionImpl description) {
        Assert.notNull(webScript, "Annotation cannot be null.");
        Assert.hasText(beanName, "Bean name cannot be empty.");
        Assert.notNull(description, "Description cannot be null.");
        Assert.hasText(description.getMethod(), "Description method is not specified.");

        if (StringUtils.hasText(webScript.value())) {
            description.setShortName(webScript.value());
        } else {
            description.setShortName(generateShortName(beanName));
        }
        
        if (StringUtils.hasText(webScript.description())) {
            description.setDescription(webScript.description()); 
        } else {
            description.setDescription(String.format("Annotation-based WebScript for class %s", beanFactory.getType(beanName).getName()));
        }
        if (webScript.families().length > 0) {
            description.setFamilys(new LinkedHashSet<>(Arrays.asList(webScript.families())));
        }
        description.setLifecycle(getLifecycle(webScript.lifecycle()));
    }
	
	private Description.Lifecycle getLifecycle(Lifecycle lifecycle){
		switch(lifecycle){
			case NONE: return Description.Lifecycle.none;
			case DRAFT: return Description.Lifecycle.draft;
			case DRAFT_PUBLIC_API: return Description.Lifecycle.draft_public_api;
			case DEPRECATED: return Description.Lifecycle.deprecated;
			case INTERNAL: return Description.Lifecycle.internal;
			case PUBLIC_API: return Description.Lifecycle.public_api;
			case SAMPLE: return Description.Lifecycle.sample;
		}
		return Description.Lifecycle.none;
	}
	
	protected void handleAuthenticationAnnotation(Authentication authentication, DescriptionImpl description) {
        Assert.notNull(authentication, "Annotation cannot be null.");
        Assert.notNull(description, "Description cannot be null.");
        if (StringUtils.hasText(authentication.runAs())) {
            description.setRunAs(authentication.runAs());
        }
        description.setRequiredAuthentication(getAuthentication(authentication.value()));
    }
	
	protected RequiredAuthentication getAuthentication(AuthenticationType authentication){
		switch(authentication){
			case NONE: return RequiredAuthentication.none;
			case GUEST: return RequiredAuthentication.guest;
			case USER: return RequiredAuthentication.user;
			case ADMIN: return RequiredAuthentication.admin;
		}
		return RequiredAuthentication.none;
	}
	
	protected void handleTransactionAnnotation(Transaction transaction, DescriptionImpl description) {
        Assert.notNull(transaction, "Annotation cannot be null.");
        Assert.notNull(description, "Description cannot be null.");

        TransactionParameters transactionParameters = new TransactionParameters();
        transactionParameters.setRequired(getRequiredTransaction(transaction.value()));
        
        if (transaction.readOnly()) {
            transactionParameters.setCapability(TransactionCapability.readonly); 
        } else {
            transactionParameters.setCapability(TransactionCapability.readwrite);
        }
        
        transactionParameters.setBufferSize(transaction.bufferSize());
        description.setRequiredTransactionParameters(transactionParameters);
    }
	
	protected RequiredTransaction getRequiredTransaction(TransactionType transaction){
		switch(transaction){
			case NONE: return RequiredTransaction.none;
			case REQUIRED: return RequiredTransaction.required;
			case REQUIRES_NEW: return RequiredTransaction.requiresnew;
		}
		return RequiredTransaction.none;
	}
	
	protected void handleCacheAnnotation(Cache cache, String beanName, DescriptionImpl description) {
        Assert.notNull(cache, "Annotation cannot be null.");
        Assert.hasText(beanName, "Bean name cannot be empty.");
        Assert.notNull(description, "Description cannot be null.");

        org.springframework.extensions.webscripts.Cache requiredCache = new org.springframework.extensions.webscripts.Cache();
        requiredCache.setNeverCache(cache.neverCache());
        requiredCache.setIsPublic(cache.isPublic());
        requiredCache.setMustRevalidate(cache.mustRevalidate());
        description.setRequiredCache(requiredCache);
    }

    protected String generateId(String beanName) {
        Assert.hasText(beanName, "Bean name cannot be empty");
        Class<?> clazz = beanFactory.getType(beanName);
        return clazz.getName();
    }

    protected String generateShortName(String beanName) {
        Assert.hasText(beanName, "Bean name cannot be empty");
        Class<?> clazz = beanFactory.getType(beanName);
        return ClassUtils.getShortName(clazz);
    }
	
    /*
	 * These methods use local classes to obtain annotations with default settings.
	 */
    private Authentication getDefaultAuthenticationAnnotation() {
    	@Authentication
    	class Default{}
        return Default.class.getAnnotation(Authentication.class);
    }

    private Transaction getDefaultReadWriteTransactionAnnotation() {
        @Transaction
        class Default{}
        return Default.class.getAnnotation(Transaction.class);
    }

    private Transaction getDefaultReadonlyTransactionAnnotation()  {
        @Transaction(readOnly = true)
        class Default{}
        return Default.class.getAnnotation(Transaction.class);
    }

    private Cache getDefaultCacheAnnotation()  {
        @Cache
        class Default{}
        return Default.class.getAnnotation(Cache.class);
    }

    private com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript getDefaultWebScriptAnnotation()  {
        @com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript
        class Default{}
        return Default.class.getAnnotation(com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript.class);
    }
}
