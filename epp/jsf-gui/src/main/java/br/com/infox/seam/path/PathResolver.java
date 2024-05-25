package br.com.infox.seam.path;

import java.io.Serializable;

import javax.ejb.Stateless;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ApplicationPath;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.ServletLifecycle;

import br.com.infox.epp.cdi.util.Beans;
import br.com.infox.epp.ws.RestApplication;

@Name(PathResolver.NAME)
@Scope(ScopeType.APPLICATION)
@AutoCreate
@Stateless
public class PathResolver implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String NAME = "pathResolver";
    public static final String SEAM_RESOURCE_SERVLET_URL = "/seam/resource";
    public static final String SEAM_REST_URL = SEAM_RESOURCE_SERVLET_URL + "/rest";

    /**
     * @return o caminho do projeto.
     */
    public String getContextPath() {
        FacesContext fc = FacesContext.getCurrentInstance();
        return fc.getExternalContext().getRequestContextPath();
    }

    public String getContextPath(String relativePath) {
        FacesContext fc = FacesContext.getCurrentInstance();
        return fc.getExternalContext().getRequestContextPath() + relativePath;
    }

    /**
     * @return caminho completo do projeto desde o servidor
     */
    public String getContextRealPath() {
        return ServletLifecycle.getServletContext().getRealPath("");
    }

    public final String getRealPath(String relativePath) {
        return ServletLifecycle.getServletContext().getRealPath(relativePath);
    }

    /**
     * @return Retorna o diretório do JSF View Id, ou seja, o diretório da
     *         página atual.
     */
    public String getViewIdDirectory() {
        FacesContext fc = FacesContext.getCurrentInstance();
        String viewId = fc.getViewRoot().getViewId();
        return viewId.substring(0, viewId.lastIndexOf('/') + 1);
    }
    
    public String getRestBaseUrl() {
    	ApplicationPath applicationPath = RestApplication.class.getAnnotation(ApplicationPath.class);
    	return getUrlProject() + applicationPath.value();
    }

    public String getUrlProject() {
        HttpServletRequest rc = getRequest();
        String url = rc.getRequestURL().toString();
        //caso a requisição seja redirecionada pega o protocolo utilizado originalmente. Precisa que o parâmetro ProxyPreserveHost = On esteja configurado no apache.
  		String originalRequestProtocol = rc.getHeader("X_FORWARDED_PROTO");
  		if( originalRequestProtocol != null){
  			url = url.replace("http://", originalRequestProtocol + "://");
  		}
        String protEnd = "://";
        int pos = url.indexOf(protEnd) + protEnd.length() + 1;
        return url.substring(0, url.indexOf('/', pos)) + rc.getContextPath();
    }

    private HttpServletRequest getRequest() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null && facesContext.getExternalContext() != null) {
            Object requestObj = facesContext.getExternalContext().getRequest();
            if (requestObj instanceof HttpServletRequest) {
                return (HttpServletRequest) requestObj;
            }
        } else {
            return Beans.getReference(HttpServletRequest.class);
        }
        return Beans.getReference(HttpServletRequest.class);
    }
}
