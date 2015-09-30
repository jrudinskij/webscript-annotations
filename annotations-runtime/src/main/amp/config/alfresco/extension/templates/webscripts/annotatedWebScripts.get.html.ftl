<#import "/org/springframework/extensions/webscripts/webscripts.lib.html.ftl" as wsLib/>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
   <body>
      <div>
          <h2>Annotated Web Scripts</h2>
          <br/>
          <#if webScripts?size == 0>
            No registered annotated web scripts 
          </#if>
          <#if webScripts?size &gt; 0>
                 <#list webScripts as webscript>
                    
                    <#assign desc = webscript.description>
                    
                    <span class="mainSubTitle">${desc.shortName}</span>
                    
                    <table>
                       <#list desc.URIs as uri>
                       <tr><td><a href="${url.serviceContext}${uri?html}">${desc.method?html} ${url.serviceContext}${uri?html}</a></td></tr>
                       </#list>
                    </table>
                    
                    <#if desc.description??>
                        <table>
                           <tr><td>---</td></tr>
                           <tr><td>${desc.description}</td></tr>
                           <tr><td>---</td></tr>
                        </table>
                    </#if>
                    
                    <table>
                       <tr><td>Authentication:</td><td>${desc.requiredAuthentication}</td></tr>
                       <tr><td>Transaction:</td><td>${desc.requiredTransaction}</td></tr>
                       <tr><td>Format Style:</td><td>${desc.formatStyle}</td></tr>
                       <tr><td>Default Format:</td><td>${desc.defaultFormat!"<i>Determined at run-time</i>"}</td></tr>
                       
                        <#if desc.lifecycle != 'none'>
                            <tr><td>Lifecycle:</td><td>${desc.lifecycle}</td></tr>
                        </#if>
                        
                       <tr><td></td></tr>
                    </table>
                    
                    <br/>
                    
                 </#list>  
          </#if> 
      </div>
   </body>
</html>