package br.com.infox.epp.access.manager;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.security.auth.login.LoginException;

import org.apache.commons.lang3.StringUtils;
import org.jboss.seam.util.RandomStringUtils;
import org.jbpm.taskmgmt.exe.TaskInstance;

import com.google.common.base.Strings;

import br.com.infox.core.manager.Manager;
import br.com.infox.core.persistence.DAOException;
import br.com.infox.core.util.EntityUtil;
import br.com.infox.epp.access.dao.UsuarioLoginDAO;
import br.com.infox.epp.access.entity.Localizacao;
import br.com.infox.epp.access.entity.UsuarioLogin;
import br.com.infox.epp.access.service.PasswordService;
import br.com.infox.epp.mail.service.AccessMailService;
import br.com.infox.epp.pessoa.entity.PessoaFisica;
import br.com.infox.epp.system.Parametros;
import br.com.infox.epp.system.dao.ParametroDAO;
import br.com.infox.epp.system.entity.Parametro;
import br.com.infox.epp.system.util.ParametroUtil;
import br.com.infox.epp.usuario.UsuarioLoginSearch;
import br.com.infox.log.LogProvider;
import br.com.infox.log.Logging;
import br.com.infox.seam.exception.BusinessException;

@Stateless
public class UsuarioLoginManager extends Manager<UsuarioLoginDAO, UsuarioLogin> {

    private static final long serialVersionUID = 1L;
    private static final LogProvider LOG = Logging.getLogProvider(UsuarioLoginManager.class);
    public static final String USER_ADMIN = "admin";

    @Inject
    private PasswordService passwordService;
    @Inject
    private AccessMailService accessMailService;
    @Inject
    private ParametroDAO parametroDAO;

    public boolean usuarioExpirou(final UsuarioLogin usuarioLogin) {
        boolean result = Boolean.FALSE;
        if (usuarioLogin != null) {
            final Date dataExpiracao = usuarioLogin.getDataExpiracao();
            result = usuarioLogin.getProvisorio() && dataExpiracao != null
                    && dataExpiracao.before(new Date());
        }
        return result;
    }
    
    public void inativarUsuario(final UsuarioLogin usuario) throws DAOException {
        getDao().inativarUsuario(usuario);
    }

    public UsuarioLogin getUsuarioLoginByEmail(final String email) {
        return getDao().getUsuarioLoginByEmail(email);
    }
    
    /**
     * Utilizar {@link UsuarioLoginSearch#getUsuarioByLogin(String)} pra recuperar usuários que podem efetuar login.
     * Para encontrar um usuário sem impor restrições continuar utilizando este método
     * @param login Login do usuário a recuperar
     * @return Usuário referente ao login
     */
    public UsuarioLogin getUsuarioLoginByLogin(final String login) {
        return getDao().getUsuarioLoginByLogin(login);
    }

    public String getLoginUsuarioByTaskInstance(TaskInstance taskInstance) {
        return getDao().getLoginUsuarioByTaskInstance(taskInstance);
    }
    
    public String getNomeUsuarioByTaskInstance(TaskInstance taskInstance) {
        return getDao().getNomeUsuarioByTaskInstance(taskInstance);
    }

    public UsuarioLogin getUsuarioLoginByPessoaFisica(final PessoaFisica pessoaFisica) {
        return getDao().getUsuarioLoginByPessoaFisica(pessoaFisica);
    }
    
    public UsuarioLogin getUsuarioFetchPessoaFisicaByNrCpf(String nrCpf){
    	return getDao().getUsuarioFetchPessoaFisicaByCpf(nrCpf);
    }

    private void validarPermanencia(final UsuarioLogin usuario) {
        if (usuario.getProvisorio() == null || !usuario.getProvisorio()) {
            usuario.setDataExpiracao(null);
        }
        if (!usuario.isHumano()) {
            usuario.setPessoaFisica(null);
        }
        if (usuario.getSenha() == null || "".equals(usuario.getSenha())) {
            String senha = RandomStringUtils.randomAlphabetic(8);
            usuario.setSenha(senha);
        }
        if (usuario.getSalt() == null) {
            usuario.setSalt("");
        }
    }

    public UsuarioLogin createLDAPUser(UsuarioLogin usuario) throws DAOException, IllegalAccessException, InvocationTargetException {
        validarPermanencia(usuario);
        String password = usuario.getSenha();
        Object id = EntityUtil.getIdValue(super.persist(usuario));
        UsuarioLogin persisted = find(id);
        passwordService.changePassword(persisted, password);
        return update(persisted);
    }

    public UsuarioLogin persist(UsuarioLogin usuario, boolean sendMail){
    	validarPermanencia(usuario);
        try {
            UsuarioLogin persisted = getDao().persist(usuario);
            String password = usuario.getSenha();
            passwordService.changePassword(persisted, password);
            if (sendMail){
            	accessMailService.enviarEmailDeMudancaDeSenha("email", persisted, password);
            }
	    return getDao().update(persisted);
        } catch (IllegalArgumentException e) {
            throw new DAOException(e);
        }
    }
    
    @Override
    public UsuarioLogin persist(UsuarioLogin usuario) throws DAOException {
        return persist(usuario, usuario.isLoginComSenhaHabilitado());
    }
    
    public UsuarioLogin getUsuarioSistema() {
        Parametro parametro = parametroDAO.getParametroByNomeVariavel(Parametros.ID_USUARIO_SISTEMA.getLabel());
        String strIdUsuarioSistema = parametro.getValorVariavel();
        if(StringUtils.isEmpty(strIdUsuarioSistema)) {
        	return null;
        }
		return find(Integer.valueOf(strIdUsuarioSistema));
    }
    
    public UsuarioLogin getUsuarioDeProcessosDoSistema() {
    	String idUsuarioSistema = ParametroUtil.getParametroOrFalse(Parametros.ID_USUARIO_PROCESSO_SISTEMA.getLabel());
    	if (Strings.isNullOrEmpty(idUsuarioSistema) || "false".equals(idUsuarioSistema)) {
    		String mensagem = "Não foi configurado o usuário de processos do sistema";
			LOG.error(mensagem);
    		throw new BusinessException(mensagem);
    	} else {
    		UsuarioLogin usuario = find(Integer.parseInt(idUsuarioSistema));
    		if (!usuario.isHumano()) {
    			return usuario;
    		} else {
    			String mensagem = "Usuario " + usuario + "não é um usuário de sistema";
				LOG.error(mensagem);
    			throw new BusinessException(mensagem);
    		}
    	}
    }
    
    @Deprecated // Não vi nenhuma classe utilizando este método
    public void requisitarNovaSenhaPorEmail(UsuarioLogin usuario, String tipoParametro) throws LoginException, DAOException {
        if (usuario == null) {
            throw new BusinessException("Usuário não encontrado");
        }
        String plainTextPassword = passwordService.gerarNovaSenha(usuario);
        passwordService.changePassword(usuario, plainTextPassword);
        update(usuario);
        accessMailService.enviarEmailDeMudancaDeSenha(tipoParametro, usuario, plainTextPassword);
    }
    
    public List<UsuarioLogin> getUsuariosLogin(Localizacao localizacao, String... papeis){
    	return getDao().getUsuariosLoginLocalizacaoPapeis(localizacao, papeis);
    }
    
    public boolean isAdminDefaultPassword() {
		UsuarioLogin admin = getUsuarioLoginByLogin(USER_ADMIN);
		String password = passwordService.generatePasswordHash("admin", admin.getSalt());
		return password.equals(admin.getSenha());
	}
    
    public boolean existeTaskInstaceComUsuario(String login) {
        CriteriaBuilder cb = getDao().getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<TaskInstance> taskInstance = cq.from(TaskInstance.class);
        cq.select(cb.count(taskInstance));
        cq.where(
            cb.equal(taskInstance.get("assignee"), cb.literal(login))        
        );
        Long count = getDao().getEntityManager().createQuery(cq).getSingleResult();
        return count > 0L;
    }
}
