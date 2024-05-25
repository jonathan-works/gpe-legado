package br.com.infox.epp.access.api;

import static br.com.infox.constants.WarningConstants.UNCHECKED;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.el.ELException;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.naming.NamingException;
import javax.security.auth.login.LoginException;

import org.apache.commons.lang3.StringUtils;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.bpm.Actor;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.faces.Redirect;
import org.jboss.seam.faces.RedirectException;
import org.jboss.seam.international.StatusMessage.Severity;
import org.jboss.seam.security.Credentials;
import org.jboss.seam.security.Identity;
import org.jboss.seam.security.management.IdentityManager;
import org.jboss.seam.security.management.JpaIdentityStore;

import com.google.common.base.Strings;

import br.com.infox.core.messages.InfoxMessages;
import br.com.infox.core.persistence.DAOException;
import br.com.infox.epp.access.TermoAdesaoService;
import br.com.infox.epp.access.dao.UsuarioLoginDAO;
import br.com.infox.epp.access.dao.UsuarioPerfilDAO;
import br.com.infox.epp.access.entity.Localizacao;
import br.com.infox.epp.access.entity.Papel;
import br.com.infox.epp.access.entity.UsuarioLogin;
import br.com.infox.epp.access.entity.UsuarioPerfil;
import br.com.infox.epp.access.manager.PapelManager;
import br.com.infox.epp.access.manager.UsuarioLoginManager;
import br.com.infox.epp.access.manager.ldap.LDAPManager;
import br.com.infox.epp.access.service.AuthenticatorService;
import br.com.infox.epp.access.service.PasswordService;
import br.com.infox.epp.access.type.UsuarioEnum;
import br.com.infox.epp.cdi.seam.ContextDependency;
import br.com.infox.epp.cdi.util.Beans;
import br.com.infox.epp.menu.MenuNavigation;
import br.com.infox.epp.painel.PainelUsuarioController;
import br.com.infox.epp.pessoa.entity.PessoaFisica;
import br.com.infox.epp.system.Parametros;
import br.com.infox.epp.system.manager.ParametroManager;
import br.com.infox.epp.system.util.ParametroUtil;
import br.com.infox.log.LogProvider;
import br.com.infox.log.Logging;
import br.com.infox.seam.security.SecurityUtil;

@AutoCreate
@Name(Authenticator.NAME)
@Install(precedence = Install.APPLICATION)
@ContextDependency
public class Authenticator implements Serializable {

	private static final long serialVersionUID = 1L;
	public static final String NAME = "authenticator";
    private static final LogProvider LOG = Logging.getLogProvider(Authenticator.class);
    
    @Inject
    protected UsuarioLoginManager usuarioLoginManager;
    @Inject
    protected InfoxMessages infoxMessages;
    @Inject
    private TermoAdesaoService termoAdesaoService;
    @Inject
    private SecurityUtil securityUtil;
    @Inject
    private CdiAuthenticator cdiAuthenticator;
    
    private String newPassword1;
    private String newPassword2;
    private String login;

    public static final String PAPEIS_USUARIO_LOGADO = "papeisUsuarioLogado";
    public static final String USUARIO_LOGADO = "usuarioLogado";
    public static final String USUARIO_PERFIL_ATUAL = "usuarioLogadoPerfilAtual";
    public static final String USUARIO_PERFIL_LIST = "usuarioPerfilList";
    public static final String INDENTIFICADOR_PAPEL_ATUAL = "identificadorPapelAtual";
    public static final String LOCALIZACOES_FILHAS_ATUAIS = "localizacoesFilhasAtuais";
    public static final String ID_LOCALIZACOES_FILHAS_ATUAIS = "idLocalizacoesFilhasAtuais";

    public String getNewPassword1() {
        return newPassword1;
    }

    public static Authenticator instance() {
        return (Authenticator) Component.getInstance(Authenticator.NAME);
    }

    public void setNewPassword1(String newPassword1) {
        if (this.newPassword1 != newPassword1
                && (this.newPassword1 == null || !this.newPassword1.equals(newPassword1))) {
            this.newPassword1 = newPassword1;
        }
    }

    public String getNewPassword2() {
        return newPassword2;
    }

    public void setNewPassword2(String newPassword2) {
        if (newPassword2IsValid(newPassword2)) {
            this.newPassword2 = newPassword2;
        }
    }

    private boolean newPassword2IsValid(String newPassword2) {
        return this.newPassword2 != newPassword2
                && (this.newPassword2 == null || !this.newPassword2.equals(newPassword2));
    }

    @Observer(Identity.EVENT_POST_AUTHENTICATE)
    public void postAuthenticate() throws LoginException {
        String id = Identity.instance().getCredentials().getUsername();
        if (id != null) {
            JpaIdentityStore store = getJpaIdentyStore();
            UsuarioLogin usuario = (UsuarioLogin) store.lookupUser(id);
            try {
                if (isTrocarSenha()) {
                    trocarSenhaUsuario(usuario);
                } else {
                    realizarLoginDoUsuario(usuario);
                }
            } catch (LoginException e) {
                Identity.instance().unAuthenticate();
                FacesMessages.instance().add(e.getMessage());
            } catch (DAOException e) {
                LOG.error("postAuthenticate()", e);
            }
        }
    }

    public boolean hasToSignTermoAdesao() throws LoginException {
    	UsuarioLogin usuarioLogado = getUsuarioLogado();
    	if (usuarioLogado != null) {
    		return hasToSignTermoAdesao(usuarioLogado);
    	}
    	return false;
    }
    
    private boolean hasToSignTermoAdesao(UsuarioLogin usuario) throws LoginException {
        
        PessoaFisica pessoaFisica = usuario.getPessoaFisica();
        boolean hasToSign = Beans.getReference(PapelManager.class).hasToSignTermoAdesao(usuario);
        if (hasToSign){
            if (pessoaFisica == null)
               throw new LoginException(String.format(infoxMessages.get(AuthenticatorService.LOGIN_ERROR_SEM_PESSOA_FISICA), usuario));
            
            hasToSign = !termoAdesaoService.isTermoAdesaoAssinado(pessoaFisica.getCpf());
        }
        return hasToSign;
    }

    private void realizarLoginDoUsuario(final UsuarioLogin usuario) throws LoginException {
        getAuthenticatorService().setUsuarioLogadoSessao(usuario);
        obterPerfilAtual(usuario);
        Actor.instance().setId(usuario.getLogin());
    }

    private JpaIdentityStore getJpaIdentyStore() {
        return (JpaIdentityStore) IdentityManager.instance().getIdentityStore();
    }

    private boolean isTrocarSenha() {
        return newPassword1 != null && !newPassword1.trim().equals("");
    }

    private void trocarSenhaUsuario(final UsuarioLogin usuario) throws LoginException, DAOException {
        if (newPassword1.equals(newPassword2)) {
            PasswordService passwordService = (PasswordService) Component.getInstance(PasswordService.NAME);
            passwordService.changePassword(usuario, newPassword1);
            usuarioLoginManager.update(usuario);
            getMessagesHandler().add(infoxMessages.get("login.error.senhaAlteradaSucesso"));
            Identity.instance().unAuthenticate();
        } else {
            throw new LoginException(infoxMessages.get("login.error.novaSenhaNaoConfere"));
        }
    }

    private FacesMessages getMessagesHandler() {
        return FacesMessages.instance();
    }

    public void login() {
        Identity identity = Identity.instance();
        Credentials credentials = identity.getCredentials();
        JpaIdentityStore store = getJpaIdentyStore();
        UsuarioLogin usuario = (UsuarioLogin) store.lookupUser(credentials.getUsername());
        try {
            getAuthenticatorService().checkValidadeUsuarioLogin(usuario, UsuarioEnum.P);
        } catch (LoginException e) {
            getMessagesHandler().add(Severity.ERROR, e.getMessage());
            return;
        }
        if (cdiAuthenticator.authenticate(credentials.getUsername(), credentials.getPassword())){
        	getAuthenticatorService().loginWithoutPassword(credentials.getUsername());
        	return;
        }
        if (loginExists(credentials) || ldapLoginExists(credentials)) {
            try {
            	if (validateLoginDefaultUsers()) {
            		identity.login();
            	}
            } catch (ELException e) {
                if (e.getCause() instanceof RedirectException) {
                    LOG.warn("Erro de redirecionamento", e);                        
                } else {
                    LOG.error(e);
                }  
            } finally {
				setNewPassword1(null);
				setNewPassword2(null);
			}
        } else {
            getMessagesHandler().add(Severity.ERROR, infoxMessages.get("login.error.invalid"));
        }
    }

    protected boolean loginExists(final Credentials credentials) {
        String login = credentials.getUsername();
        UsuarioLogin user = usuarioLoginManager.getUsuarioLoginByLogin(login);
        return user != null;
    }

    protected boolean ldapLoginExists(final Credentials credentials) {
        boolean ldapUserExists = false;
        try {
            String providerUrl = getProviderUrl();
            if (StringUtils.isEmpty(providerUrl) || "-1".equals(providerUrl)){
                return false;
            }
            LDAPManager ldapManager = Beans.getReference(LDAPManager.class);
            UsuarioLogin user = ldapManager.autenticarLDAP(credentials.getUsername(), credentials.getPassword(), providerUrl, getDomainName());
            if (user != null)
                usuarioLoginManager.persist(user);
            ldapUserExists = user != null;
        } catch (NamingException | DAOException e) {
            LOG.warn("ldapException", e);
        }
        return ldapUserExists;
    }

    protected String getDomainName() {
        final ParametroManager parametroManager = (ParametroManager) Component.getInstance(ParametroManager.NAME);
        return parametroManager.getValorParametro("ldapDomainName");
    }

    protected String getProviderUrl() {
        final ParametroManager parametroManager = (ParametroManager) Component.getInstance(ParametroManager.NAME);
        return parametroManager.getValorParametro("ldapProviderUrl");
    }

    @Observer(Identity.EVENT_LOGIN_FAILED)
    public void loginFailed(Object obj) throws LoginException {
        UsuarioLogin usuario = usuarioLoginManager.getUsuarioLoginByLogin(Identity.instance().getCredentials().getUsername());
        if (usuario != null && !usuario.getAtivo()) {
            FacesMessages.instance().add(infoxMessages.get("login.error.inativo"));
        }
        if(!isTrocarSenha())
        	FacesMessages.instance().add(infoxMessages.get("login.error.usuarioOuSenhaInvalidos"));
    }

    @Observer(Identity.EVENT_LOGGED_OUT)
    public void limparContexto() {
        Credentials credentials = (Credentials) Component.getInstance(Credentials.class);
        credentials.clear();
        Context context = Contexts.getSessionContext();
        context.remove(USUARIO_LOGADO);
        context.remove(USUARIO_PERFIL_ATUAL);
        context.remove(PAPEIS_USUARIO_LOGADO);
        context.remove(INDENTIFICADOR_PAPEL_ATUAL);
        context.remove(LOCALIZACOES_FILHAS_ATUAIS);
        context.remove(ID_LOCALIZACOES_FILHAS_ATUAIS);
        context.remove(USUARIO_PERFIL_LIST);
        context.remove(PainelUsuarioController.NUMERO_PROCESSO_FILTERED);
    }

    public static List<Localizacao> getLocalizacoesFilhas(
            Localizacao localizacao) {
        return getLocalizacoesFilhas(localizacao, new ArrayList<Localizacao>());
    }

    private static List<Localizacao> getLocalizacoesFilhas(Localizacao loc, List<Localizacao> list) {
        list.add(loc);
        if (loc.getEstruturaFilho() != null && !list.contains(loc.getEstruturaFilho())) {
//            getLocalizacoesFilhas(loc.getEstruturaFilho(), list);
        }
        for (Localizacao locFilho : loc.getLocalizacaoList()) {
            getLocalizacoesFilhas(locFilho, list);
        }
        return list;
    }

    public static String getIdsLocalizacoesFilhas(Localizacao localizacao) {
        StringBuilder sb = new StringBuilder();
        List<Localizacao> localizacoesFilhas = getLocalizacoesFilhas(localizacao, new ArrayList<Localizacao>());
        for (Localizacao loc : localizacoesFilhas) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(loc.getIdLocalizacao());
        }
        return sb.toString();
    }

    public String unAuthenticate() throws DAOException {
		LOG.info("unAuthenticate sessao do usuário: " + Contexts.getSessionContext().get(USUARIO_LOGADO));
		Identity.instance().unAuthenticate();
		Identity.instance().logout();
		limparContexto();
		return "/login.seam";
    }

    private boolean obterPerfilAtual(UsuarioLogin usuario) throws LoginException {
        UsuarioPerfil usuarioPerfil = getAuthenticatorService().obterPerfilAtual(usuario);
        if (usuarioPerfil != null) {
            setUsuarioPerfilAtual(usuarioPerfil);
            return true;
        }
        throw new LoginException(String.format(infoxMessages.get(AuthenticatorService.LOGIN_ERROR_USUARIO_SEM_PERFIL), usuario));
    }

    /**
     * Muda a localização do usuário logado, removendo todos os roles da
     * localização anterior (se hover) e atribuindo os roles da nova
     * localização, recursivamente.
     * 
     * @param usuarioPerfil
     * @throws LoginException 
     */
    public void setUsuarioPerfilAtual(UsuarioPerfil usuarioPerfil) throws LoginException {
        setUsuarioPerfilAtual(usuarioPerfil, true);
    }
    
    private String setUsuarioPerfilAtual(UsuarioPerfil usuarioPerfil, boolean doRedirect) throws LoginException {
        if (!usuarioPerfil.getUsuarioLogin().equals(getUsuarioLogado())) {
            throw new LoginException("Perfil não permitido: " + usuarioPerfil.getPerfilTemplate().getDescricao());
        }
        Set<String> roleSet = getRolesAtuais(usuarioPerfil);
        getAuthenticatorService().removeRolesAntigas();
        getAuthenticatorService().logDaBuscaDasRoles(usuarioPerfil);
        getAuthenticatorService().addRolesAtuais(roleSet);
        setVariaveisDoContexto(usuarioPerfil, roleSet);
        securityUtil.clearPermissionCache();
        Beans.getReference(MenuNavigation.class).refresh();
        
        if (!isUsuarioExterno()) {
            if (!hasToSignTermoAdesao()) {
                if (doRedirect) {
                    redirectToPainelDoUsuario();
                } else {
                    return getCaminhoPainel();
                }
            } else {
                if (doRedirect) {
                    redirectToTermoAdesao();
                } else {
                    return "/termoAdesao.seam";
                }
            }
        }
        
        return null;
    }

    public void redirectToPainelDoUsuario() {
    	try {
	        Redirect redirect = Redirect.instance();
	        redirect.getParameters().clear();
	        redirect.setViewId(getCaminhoPainel());
	        redirect.setParameter("scid", null);
	        redirect.execute();
    	} catch (NullPointerException e){
    		if (FacesContext.getCurrentInstance() != null){
    			throw e;
    		}
    	}
    }
    
    public String getCaminhoPainel() {
        return "/Painel/list.seam";
    }
    
    public String getUrlPainel() {
        return getCaminhoPainel();
    }

    private void redirectToTermoAdesao() {
    	try {
	        Redirect redirect = Redirect.instance();
	        redirect.getParameters().clear();
	        redirect.setViewId("/termoAdesao.seam");
	        redirect.setParameter("scid", null);
	        redirect.execute();
    	} catch (NullPointerException e){
    		if (FacesContext.getCurrentInstance() != null){
    			throw e;
    		}
    	}
    }
    
    private void setVariaveisDoContexto(UsuarioPerfil usuarioPerfil, Set<String> roleSet) {
        Contexts.getSessionContext().set(USUARIO_PERFIL_ATUAL, usuarioPerfil);
        Contexts.getSessionContext().set(INDENTIFICADOR_PAPEL_ATUAL, usuarioPerfil.getPerfilTemplate().getPapel().getIdentificador());
        Contexts.getSessionContext().set(PAPEIS_USUARIO_LOGADO, roleSet);
        Contexts.getSessionContext().set(LOCALIZACOES_FILHAS_ATUAIS, getLocalizacoesFilhas(usuarioPerfil.getLocalizacao()));
        Contexts.getSessionContext().remove("mainMenu");
    }

    private Set<String> getRolesAtuais(UsuarioPerfil usuarioPerfil) {
        return RolesMap.instance().getChildrenRoles(usuarioPerfil.getPerfilTemplate().getPapel().getIdentificador());
    }

    @SuppressWarnings(UNCHECKED)
    public static List<Localizacao> getLocalizacoesFilhasAtuais() {
        return (List<Localizacao>) Contexts.getSessionContext().get(LOCALIZACOES_FILHAS_ATUAIS);
    }

    /**
     * @return o UsuarioPerfil atual do usuário logado
     */
    public static UsuarioPerfil getUsuarioPerfilAtual() {
    	Context context = Contexts.getSessionContext();
    	if(context == null) {
    		return null;
    	}
        UsuarioPerfil usuarioPerfil = (UsuarioPerfil) context.get(USUARIO_PERFIL_ATUAL);
        if (usuarioPerfil != null) {
            usuarioPerfil = getUsuarioPerfilDAO().find(usuarioPerfil.getIdUsuarioPerfil());
        }
        return usuarioPerfil;
    }

    public static boolean isUsuarioAtualResponsavel() {
        return getUsuarioPerfilAtual().getResponsavelLocalizacao();
    }

    private static UsuarioLoginDAO getUsuarioLoginDAO() {
        return (UsuarioLoginDAO) Component.getInstance(UsuarioLoginDAO.NAME);
    }

    private static UsuarioPerfilDAO getUsuarioPerfilDAO() {
        return (UsuarioPerfilDAO) Component.getInstance(UsuarioPerfilDAO.NAME);
    }

    /**
     * Atalho para a localização atual
     * 
     * @return localização atual do usuário logado
     */
    public static Localizacao getLocalizacaoAtual() {
        UsuarioPerfil usuarioPerfilAtual = getUsuarioPerfilAtual();
        if (usuarioPerfilAtual != null) {
            return usuarioPerfilAtual.getLocalizacao();
        }
        return null;
    }

    public static Papel getPapelAtual() {
        UsuarioPerfil usuarioPerfilAtual = getUsuarioPerfilAtual();
        if (usuarioPerfilAtual != null) {
            return usuarioPerfilAtual.getPerfilTemplate().getPapel();
        }
        return null;
    }

    public boolean perfilTemplateAtualPossuiLocalizacao(){
    	UsuarioPerfil usuarioPerfilAtual = getUsuarioPerfilAtual();
		if(usuarioPerfilAtual == null || usuarioPerfilAtual.getPerfilTemplate() == null || usuarioPerfilAtual.getPerfilTemplate().getLocalizacao() == null){
			return false;
		}
		return true;
    }
    /**
     * Atalho para o usuario logado
     * 
     * @return usuário logado
     */
    public static UsuarioLogin getUsuarioLogado() {
    	if(Contexts.getSessionContext() == null) {
    		return null;
    	}
        UsuarioLogin usuario = (UsuarioLogin) Contexts.getSessionContext().get("usuarioLogado");
        if (usuario == null) {
            return null;
        }
        return getUsuarioLoginDAO().find(usuario.getIdUsuarioLogin());
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getLogin() {
        return login;
    }

    @SuppressWarnings(UNCHECKED)
    public List<SelectItem> getUsuarioPerfilListItems() {
        List<SelectItem> list = (List<SelectItem>) Contexts.getSessionContext().get(USUARIO_PERFIL_LIST);
        return list;
    }

    public String trocarPerfil(Integer id) throws LoginException {
        return setUsuarioPerfilAtual(getUsuarioPerfilDAO().find(id), false);
    }
    
    public void setUsuarioPerfilAtualCombo(Integer id) throws LoginException {
        setUsuarioPerfilAtual(getUsuarioPerfilDAO().find(id));
    }

    public Integer getUsuarioPerfilAtualCombo() {
        return getUsuarioPerfilAtual().getIdUsuarioPerfil();
    }
    
    public String getUsuarioPerfilAtualSingle(){
    	return getUsuarioPerfilAtual().toString();
    }

    private static AuthenticatorService getAuthenticatorService() {
        return (AuthenticatorService) Component.getInstance(AuthenticatorService.NAME);
    }

    public static void loginUsuarioExterno() {
        Identity identity = Identity.instance();
        Credentials credentials = identity.getCredentials();
        credentials.setUsername(ParametroUtil.LOGIN_USUARIO_EXTERNO);
        credentials.setPassword("usuarioexterno");
        identity.quietLogin();
        identity.login();
        UsuarioLoginManager usuarioLoginManager = Beans.getReference(UsuarioLoginManager.class);
        Contexts.getSessionContext().set(USUARIO_LOGADO, usuarioLoginManager.getUsuarioLoginByLogin(credentials.getUsername()));
    }

    public boolean isUsuarioExterno() {
        if (Identity.instance().isLoggedIn()) {
            return getUsuarioLogado().getLogin().equals(ParametroUtil.getLoginUsuarioExterno());
        }
        return false;
    }
    
    protected boolean validateLoginDefaultUsers() {
    	if (ParametroUtil.isProducao()) {
    		Credentials credentials = Identity.instance().getCredentials();
    		if (UsuarioLoginManager.USER_ADMIN.equals(credentials.getUsername()) && usuarioLoginManager.isAdminDefaultPassword()) {
    			FacesMessages.instance().add("Usuário sem permissão de login");
    			return false;
    		} else if (ParametroUtil.getLoginUsuarioExterno().equals(credentials.getUsername())) {
    			FacesMessages.instance().add("Usuário sem permissão de login");
    			return false;
    		} else {
    			String usuarioSistema = null;
    			String usuarioProcessoSistema = null;
    			if (!Strings.isNullOrEmpty(Parametros.ID_USUARIO_SISTEMA.getValue())) {
    				UsuarioLogin u = usuarioLoginManager.find(Integer.valueOf(Parametros.ID_USUARIO_SISTEMA.getValue()));
    				if (u != null) {
    					usuarioSistema = u.getLogin();
    				}
    			}
    			if (!Strings.isNullOrEmpty(Parametros.ID_USUARIO_PROCESSO_SISTEMA.getValue())) {
    				UsuarioLogin u = usuarioLoginManager.find(Integer.valueOf(Parametros.ID_USUARIO_PROCESSO_SISTEMA.getValue()));
    				if (u != null) {
    					usuarioProcessoSistema = u.getLogin();
    				}
    			}
    			if (credentials.getUsername().equals(usuarioSistema) || credentials.getUsername().equals(usuarioProcessoSistema)) {
    				FacesMessages.instance().add("Usuário sem permissão de login");
        			return false;
    			}
    		}
    	}
    	return true;
    }

}
