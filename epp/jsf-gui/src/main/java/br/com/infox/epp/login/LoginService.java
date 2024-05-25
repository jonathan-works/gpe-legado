package br.com.infox.epp.login;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.naming.NamingException;
import javax.security.auth.login.LoginException;

import br.com.infox.core.messages.InfoxMessages;
import br.com.infox.epp.access.entity.UsuarioLogin;
import br.com.infox.epp.access.manager.UsuarioLoginManager;
import br.com.infox.epp.access.manager.ldap.LDAPManager;
import br.com.infox.epp.access.service.AuthenticatorService;
import br.com.infox.epp.access.service.PasswordService;
import br.com.infox.epp.access.type.UsuarioEnum;
import br.com.infox.seam.exception.BusinessException;

@Stateless
public class LoginService {
	
    @Inject
    protected InfoxMessages infoxMessages;
    @Inject
    protected UsuarioLoginManager usuarioLoginManager;
    @Inject
    private Logger logger;
    @Inject
    private LDAPManager ldapManager;
    @Inject
    private PasswordService passwordService;
    @Inject
    private AuthenticatorService authenticatorService;
    
    private boolean autenticarBanco(String login, String senha) {
        UsuarioLogin usuario = usuarioLoginManager.getUsuarioLoginByLogin(login);
        if(usuario == null) {
        	return false;
        }
        try {
            authenticatorService.checkValidadeUsuarioLogin(usuario, UsuarioEnum.P);
        } catch (LoginException e) {
            throw new BusinessException(e.getMessage());
        }
        if(usuario.getSenha().equals(passwordService.generatePasswordHash(senha, usuario.getSalt()))) {
        	return true;
        }
        return false;        
    }
    
    private boolean autenticarLdap(String login, String senha) {
    	
		try {
			UsuarioLogin usuario = ldapManager.autenticarLDAP(login, senha);
			if (usuario != null){
			    authenticatorService.checkValidadeUsuarioLogin(usuario, UsuarioEnum.P);
	                    usuarioLoginManager.persist(usuario);
			}
	                return usuario != null;
		} catch (NamingException e) {
			logger.log(Level.WARNING, "ldapException", e);
			return false;
		} catch (LoginException e) {
	            throw new BusinessException(e.getMessage());
	        }
    }
    
    
    public boolean autenticar(String usuario, String senha) {
    	return (autenticarBanco(usuario, senha) || autenticarLdap(usuario, senha));
    }
}
