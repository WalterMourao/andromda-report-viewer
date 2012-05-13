/**
 * 
 */
package com.wim.reportviewer.web;

import java.util.Collection;
import java.util.Map;
import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.el.ValueBinding;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.andromda.presentation.jsf.Messages;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.trinidad.component.UIXComponent;
import org.apache.myfaces.trinidad.component.UIXEditableValue;
import org.apache.myfaces.trinidad.component.UIXIterator;
import org.apache.myfaces.trinidad.component.UIXValue;
import org.apache.myfaces.trinidad.component.core.data.CoreTable;
import org.apache.myfaces.trinidad.context.Agent;
import org.apache.myfaces.trinidad.context.RequestContext;
import org.apache.myfaces.trinidad.render.ExtendedRenderKitService;
import org.apache.myfaces.trinidad.util.ComponentUtils;
import org.apache.myfaces.trinidad.util.Service;

/**
 * @author walter
 *
 */
public class WebAppUtil {

	public static final String NEW_ELEMENT_IN_DIALOG="startNewElementInDialog";

	//para ser chamado pelo EL
	static public String concat(String s1, String s2){
		return StringUtils.defaultString(s1)+StringUtils.defaultString(s2);
	}

	static public String idUsuarioAtual(HttpServletRequest req){
		//TODO depois passar para a camada core pegado do principalstore e tem que alterar o filter.
		return req.getUserPrincipal().getName();
	}

	static public boolean isUserInRoles(String[] roles){

		if (ArrayUtils.isEmpty(roles)){ // no constraints at all
			return true;
		} else {
			javax.faces.context.ExternalContext ctx = getExternalContext();

			if (ctx.getUserPrincipal() == null){ // not logged in
				return false;
			}

			for(String role: roles){
				if(ctx.isUserInRole(role))
					return true;
			}

			return false;
		}
	}

	static public boolean isUserInRoles(String roles){
		roles = StringUtils.trimToNull(roles);
		return roles == null?true:isUserInRoles(roles.split(","));
	}

	//prepara uma url de modo que ela receba o token necessário para ter suporte ao pageflowscope 
	// (e outras coisas não percebidas ainda)
	static public String mkTrinidadURL(String url){
		return getRequestContext().getPageFlowScopeProvider().encodeCurrentPageFlowScopeURL(getFacesContext(), url);
	}

	static public Map<String, Object> getPageFlowScope(){
		return org.apache.myfaces.trinidad.context.RequestContext.getCurrentInstance().getPageFlowScope();
	}

	static public Map<String, Object> getApplicationFlowScope(){
		return org.apache.myfaces.trinidad.context.RequestContext.getCurrentInstance().getApplicationScopedConcurrentMap();
	}

	static public void setBeanProperty(String propertyPath, Object value){
		int dotPos = propertyPath.indexOf('.');
		String beanName = propertyPath.substring(0, dotPos);
		String propertyName = propertyPath.substring(dotPos+1); 
		Object bean = WebAppUtil.getBean(beanName);

		try {
			org.apache.commons.beanutils.PropertyUtils.setProperty(bean, propertyName, value);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	static public Boolean usingPdaRenderKit(){
		return getFacesContext().getRenderKit().getClass().getName().equals("PdaRenderKit");
	}

	static public org.apache.myfaces.trinidad.context.RequestContext getRequestContext(){
		return org.apache.myfaces.trinidad.context.RequestContext.getCurrentInstance();
	}

	/**
	 * A helper method that gets the current request from the faces
	 * context.
	 *
	 * @return the current HttpServletRequest instance.
	 */
	static public javax.servlet.http.HttpServletRequest getRequest()
	{
		return (javax.servlet.http.HttpServletRequest)getFacesContext().getExternalContext().getRequest();
	}

	static public ServletContext getServletContext()
	{
		return (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();		
	}


	/**
	 * Returns an javax.faces.event.FacesEvent parameter value, from its name
	 */        
	static public Object getParameterValue(String parameterName, javax.faces.event.FacesEvent event){
		for(Object uiObject : event.getComponent().getChildren()){
			if(uiObject instanceof javax.faces.component.UIParameter){
				final javax.faces.component.UIParameter param = (javax.faces.component.UIParameter)uiObject;
				if(param.getName().equals(parameterName)) {
					return param.getValue();
				}
			}
		}
		return null;
	}

	/**
	 * A helper method that gets the current reponse from the faces
	 * context.
	 *
	 * @return the current HttpServletReponse instance.
	 */
	static public javax.servlet.http.HttpServletResponse getResponse()
	{
		return (javax.servlet.http.HttpServletResponse)getFacesContext().getExternalContext().getResponse();
	}

	/**
	 * A helper method that gets the current session from the faces
	 * context.
	 *
	 * @param create If the create parameter is true, create (if necessary) and return a
	 *        session instance associated with the current request. If the create
	 *        parameter is false return any existing session instance associated with the
	 *        current request, or return null if there is no such session.
	 * @return the current HttpSession instance.
	 */
	static public javax.servlet.http.HttpSession getSession(final boolean create)
	{
		return (javax.servlet.http.HttpSession)getFacesContext().getExternalContext().getSession(create);
	}

	static public javax.servlet.http.HttpSession getSession()
	{
		return getSession(false);
	}

	/**
	 * Finds the root cause of the given <code>throwable</code> and
	 * adds the message taken from that cause to the faces context messages.
	 *
	 * @param throwable the exception information to add.
	 */
	static public final void addExceptionMessage(
			Throwable throwable)
	{
		String message = null;
		final Throwable rootCause = org.apache.commons.lang.exception.ExceptionUtils.getRootCause(throwable);
		if (rootCause != null)
		{
			message = rootCause.toString();
		}
		if (message == null || message.trim().length() == 0)
		{
			message = throwable.toString();
		}
		addErrorMessage(message);
	}

	/**
	 * Adds a message to the faces context (which will show up on your view when using the
	 * lt;h:messages/gt; tag).
	 *
	 * @param severity the severity of the message
	 * @param message the message to add.
	 */
	static public void addMessage(final String clientId, final javax.faces.application.FacesMessage.Severity severity, final String message)
	{
		//TODO revisar: me parece que assim não está bom pois está ignorando as outras mensagens que podem estar no form... não tenho certeza
		final FacesContext facesContext=getFacesContext();
		facesContext.addMessage(clientId, new FacesMessage(severity, message, null));

		//        final javax.faces.application.FacesMessage facesMessage = new javax.faces.application.FacesMessage(severity, message, message);
		//        final org.apache.myfaces.trinidad.context.RequestContext adfContext = org.apache.myfaces.trinidad.context.RequestContext.getCurrentInstance();
		//        final Object form = adfContext.getPageFlowScope().get("form");
		//        if (form == null){
		//            getFacesContext().addMessage(null, new FacesMessage(severity, message, null));
		//        }else{
		//            try
		//            {
		//                final java.lang.reflect.Method method = form.getClass().getMethod(
		//                    "addJsfMessages",
		//                    new Class[]{javax.faces.application.FacesMessage.class});
		//                method.invoke(form, new Object[]{facesMessage});
		//            }
		//            catch (final Exception exception)
		//            {
		//                throw new RuntimeException(exception);
		//            }
		//        }
	}

	/**
	 * Adds a message to the faces context (which will show up on your view when using the
	 * lt;h:messages/gt; tag).
	 *
	 * @param severity the severity of the message
	 * @param message the message to add.
	 */
	static public void addMessage(final javax.faces.application.FacesMessage.Severity severity, final String message)
	{
		addMessage(null, severity, message);
	}

	/**
	 * Adds the given error <code>message</code> to the current faces context.
	 *
	 * @param message the message to add.
	 */
	static public void addErrorMessage(final String message)
	{
		addMessage(javax.faces.application.FacesMessage.SEVERITY_ERROR, message);
	}

	/**
	 * Adds the given error <code>message</code> to the current faces context.
	 *
	 * @param messageKey the property key of the message to add.
	 */
	static public void addErrorMessageFromProperties(final String messageKey)
	{
		addMessage(javax.faces.application.FacesMessage.SEVERITY_ERROR, Messages.get(messageKey));
	}

	/**
	 * Adds the given info <code>message</code> to the current faces context.
	 *
	 * @param message the message to add.
	 */
	static public void addInfoMessage(final String message)
	{
		addMessage(javax.faces.application.FacesMessage.SEVERITY_INFO, message);
	}

	/**
	 * Adds the given info <code>message</code> to the current faces context.
	 *
	 * @param messageKey the property key of the message to add.
	 */
	static public void addInfoMessageFromProperties(final String messageKey)
	{
		addMessage(javax.faces.application.FacesMessage.SEVERITY_INFO, Messages.get(messageKey));
	}

	/**
	 * Adds the given error <code>message</code> to the current faces context.
	 *
	 * @param messageKey the property key of the message to add.
	 */
	static public void addErrorMessageFromProperties(final String clientId, final String messageKey)
	{
		addMessage(clientId,javax.faces.application.FacesMessage.SEVERITY_ERROR, Messages.get(messageKey));
	}

	/**
	 * Adds the given error <code>message</code> to the current faces context.
	 *
	 * @param message the message to add.
	 */
	static public void addErrorMessage(final String clientId, final String message)
	{
		addMessage(clientId,javax.faces.application.FacesMessage.SEVERITY_ERROR, message);
	}

	/**
	 * Adds the given warning <code>message</code> to the current faces context.
	 *
	 * @param messageKey the property key of the message to add.
	 */
	static public void addWarningMessageFromProperties(final String messageKey)
	{
		addMessage(javax.faces.application.FacesMessage.SEVERITY_WARN, Messages.get(messageKey));
	}

	/**
	 * Adds the given warning <code>message</code> to the current faces context.
	 *
	 * @param message the message to add.
	 */
	static public void addWarningMessage(final String message)
	{
		addMessage(javax.faces.application.FacesMessage.SEVERITY_WARN, message);
	}

	/**
	 * Returns the HttpSession for the JSF session
	 * 
	 */
	public static HttpSession getHttpSession() {
		return (HttpSession) getFacesContext().getExternalContext().getSession(false);
	}

	/**
	 * <p>Return any attribute stored in request scope, session scope, or
	 * application scope under the specified name.  If no such
	 * attribute is found, and if this name is the registered name of a
	 * managed bean, cause a new instance of this managed bean to be created
	 * (and stored in an appropriate scope, if necessary) and returned.
	 * If no attribute exists, and no managed bean was created, return
	 * <code>null</code>.</p>
	 *
	 * @param name Name of the attribute to be retrieved
	 */
	public static Object getBean(String name) {
		return getApplication().getVariableResolver().resolveVariable(getFacesContext(), name);
	}

	/**
	 * <p>Replace the value of any attribute stored in request scope,
	 * session scope, or application scope under the specified name.  If there
	 * is no such attribute, create a new request scope attribute under this
	 * name, and store the value there.</p>
	 */
	static public void setBean(String name, Object value) {
		setValue("#{" + name + "}", value); //NOI18N
	}

	// ------------------------------------------------------ Value Manipulation

	/**
	 * <p>Evaluate the specified value binding expression, and return
	 * the value that it points at.</p>
	 *
	 * @param expr Value binding expression (including delimiters)
	 */
	static public Object getValue(String expr) {
		ValueBinding vb = getApplication().createValueBinding(expr);
		return (vb.getValue(getFacesContext()));
	}

	/**
	 * <p>Evaluate the specified value binding expression, and update
	 * the value that it points at.</p>
	 *
	 * @param expr Value binding expression (including delimiters) that
	 *  must point at a writeable property
	 * @param value New value for the property pointed at by <code>expr</code>
	 */
	
	static public void setValue(String expr, Object value) {
		ValueBinding vb = getApplication().createValueBinding(expr);
		vb.setValue(getFacesContext(), value);
	}

	/**
	 * <p>
	 * Return the <code>Application</code> instance for the current web
	 * application.
	 * </p>
	 */
	static public Application getApplication() {
		return FacesContext.getCurrentInstance().getApplication();
	}

	/**
	 * <p>
	 * Return the <code>FacesContext</code> instance for the current request.
	 * </p>
	 */
	public static FacesContext getFacesContext() {
		return FacesContext.getCurrentInstance();
	}

	/**
	 * <p>
	 * Return the <code>ExternalContext</code> instance for the current
	 * request.
	 * </p>
	 */
	public static ExternalContext getExternalContext() {
		return FacesContext.getCurrentInstance().getExternalContext();
	}

	/**
	 * <p>
	 * Skip any remaining request processing lifecycle phases for the current
	 * request, and go immediately to <em>Render Response</em> phase. This
	 * method is typically invoked when you want to throw away input values
	 * provided by the user, instead of processing them.
	 * </p>
	 */
	public static void renderResponse() {
		getFacesContext().renderResponse();
	}

	/**
	 * Recursively locates a component in the view tree
	 * @param id
	 * @return
	 */
	public static UIComponent findComponentInRoot(String scopedId) {
		UIComponent component = null;

		FacesContext facesContext = FacesContext.getCurrentInstance();
		if (facesContext != null) {
			UIComponent root = facesContext.getViewRoot();
			component = ComponentUtils.findRelativeComponent(root, scopedId);
		}

		return component;
	}

	// usar ComponentUtils.findRelativeComponent(root, id);
	//    /**
	//     * Recursively locates a component in the view tree
	//     * @param id
	//     * @return
	//     */        
	//    @SuppressWarnings("unchecked")
	//    static public UIComponent findComponent(UIComponent base, String id) {
	//        if (id.equals(base.getId()))
	//          return base;
	//      
	//        UIComponent kid = null;
	//        UIComponent result = null;
	//        Iterator<UIComponent> kids = base.getFacetsAndChildren();
	//        while (kids.hasNext() && (result == null)) {
	//          kid = kids.next();
	//          if (id.equals(kid.getId())) {
	//            result = kid;
	//            break;
	//          }
	//          result = findComponent(kid, id);
	//          if (result != null) {
	//            break;
	//          }
	//        }
	//        return result;
	//    }

	/*
	 * Altera o valor do component e opcionalmente inclui no PPR
	 */
	static public void setComponentValue(UIComponent component, Object newValue, boolean addToPPR){
		if(component != null){
			if(component instanceof UIXEditableValue){
				final UIXEditableValue uixev=(UIXEditableValue)component;
				uixev.resetValue();
				uixev.setValue(newValue);
			} else if(component instanceof UIXValue){
				((UIXValue)component).setValue(newValue);
			} else if(component instanceof UIXIterator){
				((UIXIterator)component).setValue(newValue);
			} else {
				throw new RuntimeException("Tipo desconhecido em WebAppUtil.setComponentValue");
			}
			if(addToPPR){
				WebAppUtil.getRequestContext().addPartialTarget(component);
			}
		}
	}

	/*
	 * Altera o valor do component e opcionalmente inclui no PPR
	 */
	static public void setComponentValue(String scopedId, Object newValue, boolean addToPPR){
		setComponentValue(WebAppUtil.findComponentInRoot(scopedId), newValue, addToPPR);
	}


	/*
	 * Altera o valor dos componentes e opcionalmente os inclui no PPR
	 */
	static public void setComponentsValues(String[] scopedIds, Object[] newValues, boolean addToPPR){
		final UIComponent root=FacesContext.getCurrentInstance().getViewRoot();
		for(int i=0; i< scopedIds.length; i++){
			final UIComponent component=ComponentUtils.findRelativeComponent(root,scopedIds[i]);
			setComponentValue(component, newValues[i], addToPPR);
		}
	}

	/*
	 * Inclui o componente no PPR
	 */
	static public void addComponentToPPR(String id){
		final UIComponent component=findComponentInRoot(id);
		addComponentToPPR(component);
	}

	/*
	 * Inclui o componente no PPR
	 */
	static public void addComponentToPPR(String id, boolean resetValue){
		final UIComponent component=findComponentInRoot(id);
		addComponentToPPR(component, resetValue);
	}

	/*
	 * Inclui todos os componentes do viewRoot no PPR
	 */
	static public void addComponentToPPR(UIComponent component, boolean recursive, boolean resetValue){
		if(component instanceof UIXComponent){
			WebAppUtil.getRequestContext().addPartialTarget(component);
			if(resetValue && component instanceof UIXEditableValue){
				((UIXEditableValue)component).resetValue();
			}
		}
		if(recursive){
			for(Object uic: component.getChildren()){
				addComponentToPPR((UIComponent)uic,true,resetValue);
			}
		}
	}

	/*
	 * Inclui o componente no PPR
	 */
	static public void addComponentToPPR(UIComponent component, boolean resetValue){
		addComponentToPPR(component,false,resetValue);
	}

	/*
	 * Inclui o componente no PPR
	 */
	static public void addComponentToPPR(UIComponent component){
		addComponentToPPR(component,false,false);
	}

	/*
	 * Transforma o valor em String, usando o converter id
	 */
	static public java.lang.String convertUsingConverter(java.lang.Object value, java.lang.String converterId){
		final FacesContext facesContext=FacesContext.getCurrentInstance();
		final Converter converter=facesContext.getApplication().createConverter(converterId);
		return converter.getAsString(facesContext, null, value);
	}

	/*
	 * Transforma mensagens recebidas da camada de serviço em mensagens para o usuário.
	 */
	public static void setPropertyInPageFlowScope(String property, Object value){
		try {
			if(property.indexOf('.') < 0){
				WebAppUtil.getPageFlowScope().put(property, value);
			} else {
				PropertyUtils.setProperty(WebAppUtil.getPageFlowScope(), property, value);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Object getPropertyInPageFlowScope(String property){
		try {
			if(property.indexOf('.') < 0){
				return WebAppUtil.getPageFlowScope().get(property);
			} else {
				return PropertyUtils.getProperty(WebAppUtil.getPageFlowScope(), property);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void addPPRScript(String script){
		FacesContext facesContext = FacesContext.getCurrentInstance();
		final ExtendedRenderKitService service = (ExtendedRenderKitService)
		Service.getRenderKitService(facesContext, ExtendedRenderKitService.class);
		service.addScript(facesContext, script);
	}

	private static void addChildrenToPPR(RequestContext rc, UIComponent component, boolean resetValue){
		rc.addPartialTarget(component);
		if(resetValue && component instanceof UIXEditableValue){
			((UIXEditableValue)component).resetValue();
		}
		for(Object obj: component.getChildren()){
			addChildrenToPPR(rc,(UIComponent)obj, resetValue);
		}
	}

	public static void addComponentAndChildrenToPPR(String clientId, boolean resetValue){
		UIComponent component=WebAppUtil.getFacesContext().getViewRoot().findComponent(clientId.replace(':', NamingContainer.SEPARATOR_CHAR ));
		if(component != null){
			RequestContext rc = RequestContext.getCurrentInstance();
			addChildrenToPPR(rc,component, resetValue);
			addComponentToPPR(component, resetValue);
		}
	}

	public static void addComponentAndChildrenToPPR(String clientId){
		addComponentAndChildrenToPPR(clientId, false);
	}

	public static void refreshTable(CoreTable tbl, Collection<?> value){
		if(tbl != null){
			if(tbl.getSelectedRowKeys() != null){
				tbl.getSelectedRowKeys().removeAll();
			}
			tbl.setValue(value);
			WebAppUtil.getRequestContext().addPartialTarget(tbl);
		}
	}

	public static void refreshTable(String scopedId, Collection<?> value){
		final CoreTable tbl=(CoreTable)findComponentInRoot(scopedId);
		refreshTable(tbl, value);
	}

	public static void clearTableSelection(CoreTable tbl){
		if(tbl != null){
			tbl.setRowIndex(-1);
			tbl.getSelectedRowKeys().clear();
			WebAppUtil.getRequestContext().addPartialTarget(tbl);
		}
	}

	public static void clearTableSelection(String componentId){
		clearTableSelection((CoreTable)WebAppUtil.findComponentInRoot(componentId));
	}

	public static void clearDisclosedRowKeys(CoreTable table){
		table.getDisclosedRowKeys().clear();
	}

	public static void clearDislosedRowKeys(String tableId){
		clearDisclosedRowKeys((CoreTable)WebAppUtil.findComponentInRoot(tableId));
	}

	//====

	public static Boolean pdaRenderer(){
		return Agent.TYPE_PDA.equals(getRequestContext().getAgent().getType());
	}

	public static Boolean userIsManager(){
		return getRequest().isUserInRole("LOG_ONE_MANAGER") || getRequest().isUserInRole("LOG_ONE_ADMIN");
	}
	
}