package br.com.infox.epp.access.api;

import java.io.Serializable;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.inject.Inject;
import javax.inject.Named;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.jboss.seam.faces.FacesMessages;

import br.com.infox.core.exception.EppConfigurationException;
import br.com.infox.core.messages.InfoxMessages;
import br.com.infox.epp.access.entity.UsuarioLogin;
import br.com.infox.epp.access.manager.UsuarioLoginManager;
import br.com.infox.epp.access.service.AuthenticatorService;
import br.com.infox.epp.access.service.RecuperacaoSenhaService;
import br.com.infox.epp.access.type.UsuarioEnum;
import br.com.infox.epp.cdi.ViewScoped;
import br.com.infox.epp.system.dao.ParametroDAO;
import br.com.infox.log.LogProvider;
import br.com.infox.log.Logging;

@Named
@ViewScoped
public class RecuperacaoSenhaView implements Serializable {
	private static final long serialVersionUID = 1L;
	private final LogProvider LOG = Logging.getLogProvider(RecuperacaoSenhaView.class);

	private final String PARAMETRO_MINUTES_TO_EXPIRE = "tempoExpiracaoCodigoRecuperacaoSenha";

	@Inject
	private ParametroDAO parametroDAO;
	@Inject
	private RecuperacaoSenhaService recuperacaoSenhaService;
	@Inject
	private UsuarioLoginManager usuarioLoginManager;
	@Inject
	private AuthenticatorService authenticatorService;
	@Inject
	private InfoxMessages infoxMessages;

	private String login;
	private String codigo;
	private String pass1;
	private String pass2;
	private Integer minutesToExpire;

	@PostConstruct
	private void init() {
		Flash flash = FacesContext.getCurrentInstance().getExternalContext().getFlash();
		if (flash.containsKey("login")) {
			login = (String) flash.get("login");
		} else {
			Map<String, String> parameterMap = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
			if (parameterMap.containsKey("code")) {
				String code = parameterMap.get("code");
				String decode = new String(Base64.decodeBase64(code));
				String[] split = decode.split(":");
				if (split.length == 2) {
					login = split[0];
					codigo = split[1];
				}
			}
		}

		String minutos = parametroDAO.getValorParametroByNome(PARAMETRO_MINUTES_TO_EXPIRE);
    	if (minutos == null) {
    		throw new EppConfigurationException("O parâmetro " + PARAMETRO_MINUTES_TO_EXPIRE + " não tem valor definido.");
    	}
    	minutesToExpire = Integer.parseInt(minutos);

	}

	public String requisitarCodigoRecuperacao() {
		HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
		String requestURL = request.getRequestURL().toString();
		String eppURL = requestURL.substring(0, requestURL.indexOf("/codigoRecuperacaoSenha"));
		
		try {
			UsuarioLogin usuario = usuarioLoginManager.getUsuarioLoginByLogin(login);
                        authenticatorService.checkValidadeUsuarioLogin(usuario, UsuarioEnum.P);
			recuperacaoSenhaService.requisitarCodigoRecuperacao(usuario, minutesToExpire, eppURL);
			return "/recuperacaoSenha.seam?faces-redirect=true";
		} catch (LoginException e) {
		    FacesMessages.instance().add(e.getMessage());
		} catch (Exception e) {
			LOG.error("Erro ao requisitar código de recuperação de senha", e);
			FacesMessages.instance().add(e.getMessage());
		}
		return null;
	}

	public String trocarSenha() {
		UsuarioLogin usuario = usuarioLoginManager.getUsuarioLoginByLogin(login);
		try {
		    authenticatorService.checkValidadeUsuarioLogin(usuario, UsuarioEnum.P);
		} catch (LoginException e) {
		    FacesMessages.instance().add(e.getMessage());
		    return null;
		}
    	if (!recuperacaoSenhaService.verificarValidadeCodigo(codigo, usuario, minutesToExpire)) {
    		FacesMessages.instance().add(infoxMessages.get("login.error.codigoInvalido"));
    		FacesContext.getCurrentInstance().validationFailed();
    		return null;
    	}
    	if (pass1 == null || pass2 == null || pass1.isEmpty() || pass2.isEmpty() || !pass1.equals(pass2)) {
    		FacesMessages.instance().add(infoxMessages.get("login.error.novaSenhaNaoConfere"));
    		FacesContext.getCurrentInstance().validationFailed();
    		return null;
    	}
		recuperacaoSenhaService.changePassword(usuario, pass1, codigo);
		FacesMessages.instance().add(infoxMessages.get("login.error.senhaAlteradaSucesso"));
		setLogin(null);
		setCodigo(null);
		setPass1(null);
		setPass2(null);
		return "/login.xhtml?faces-redirect=true";
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}

	public String getPass1() {
		return pass1;
	}

	public void setPass1(String pass1) {
		this.pass1 = pass1;
	}

	public String getPass2() {
		return pass2;
	}

	public void setPass2(String pass2) {
		this.pass2 = pass2;
	}
}
