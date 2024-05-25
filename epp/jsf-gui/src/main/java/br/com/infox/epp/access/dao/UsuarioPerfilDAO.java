package br.com.infox.epp.access.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;

import br.com.infox.core.dao.DAO;
import br.com.infox.epp.access.entity.Localizacao;
import br.com.infox.epp.access.entity.PerfilTemplate;
import br.com.infox.epp.access.entity.UsuarioLogin;
import br.com.infox.epp.access.entity.UsuarioLogin_;
import br.com.infox.epp.access.entity.UsuarioPerfil;
import br.com.infox.epp.access.entity.UsuarioPerfil_;
import br.com.infox.epp.access.query.UsuarioPerfilQuery;
import br.com.infox.epp.pessoa.entity.PessoaFisica;

@Stateless
@AutoCreate
@Name(UsuarioPerfilDAO.NAME)
public class UsuarioPerfilDAO extends DAO<UsuarioPerfil> {

    private static final long serialVersionUID = 1L;
    public static final String NAME = "usuarioPerfilDAO";
    
    public List<PerfilTemplate> getPerfisPermitidos(Localizacao localizacao) {
        String hql;
        Map<String, Object> params = new HashMap<>();
        if (localizacao.getEstruturaFilho() != null) {
            hql = "select o from PerfilTemplate o where o.localizacao.estruturaPai = :estruturaFilho"; 
            params.put("estruturaFilho", localizacao.getEstruturaFilho());
        } else {
            hql = "select o from PerfilTemplate o where o.localizacao is null";
        }
        return getResultList(hql, params);
    }
    
    public List<PessoaFisica> getPessoasPermitidos(Localizacao localizacao, PerfilTemplate perfilTemplate) {
        Map<String,Object> params = new HashMap<>();
        params.put(UsuarioPerfilQuery.PARAM_LOCALIZACAO, localizacao.getIdLocalizacao());
        params.put(UsuarioPerfilQuery.PARAM_PERFIL_TEMPLATE, perfilTemplate.getId());
        return getNamedResultList(UsuarioPerfilQuery.LIST_PESSOA_BY_LOCALIZACAO_PERFIL_ATIVO, params);
    }

    public UsuarioPerfil getByUsuarioLoginPerfilTemplateLocalizacao(UsuarioLogin usuarioLogin, PerfilTemplate perfilTemplate,
            Localizacao localizacao, boolean ativo) {
        final Map<String,Object> params = new HashMap<>();
        params.put(UsuarioPerfilQuery.PARAM_USUARIO_LOGIN, usuarioLogin);
        params.put(UsuarioPerfilQuery.PARAM_PERFIL_TEMPLATE, perfilTemplate);
        params.put(UsuarioPerfilQuery.PARAM_LOCALIZACAO, localizacao);
        params.put(UsuarioPerfilQuery.PARAM_ATIVO, ativo);
        return getNamedSingleResult(UsuarioPerfilQuery.GET_BY_USUARIO_LOGIN_PERFIL_TEMPLATE_LOCALIZACAO, params);
    }
    
    public List<UsuarioPerfil> listByUsuarioLogin(UsuarioLogin usuarioLogin) {
        final Map<String,Object> params = new HashMap<>();
        params.put(UsuarioPerfilQuery.PARAM_USUARIO_LOGIN, usuarioLogin);
        return getNamedResultList(UsuarioPerfilQuery.LIST_BY_USUARIO_LOGIN, params);
    }
    
    public List<UsuarioPerfil> listByLogin(String login) {
    	CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
    	CriteriaQuery<UsuarioPerfil> cq = cb.createQuery(UsuarioPerfil.class);
    	Root<UsuarioPerfil> up = cq.from(UsuarioPerfil.class);
    	Join<?, UsuarioLogin> ul = up.join(UsuarioPerfil_.usuarioLogin, JoinType.INNER);
    	cq.select(up);
    	cq.where(
			cb.equal(ul.get(UsuarioLogin_.login), login),
			cb.isTrue(up.get(UsuarioPerfil_.ativo))
		);
    	return getEntityManager().createQuery(cq).getResultList();
    }
    
    public boolean existeUsuarioPerfil(UsuarioLogin usuarioLogin, String descricaoPerfil, boolean ativo) {
    	Map<String,Object> params = new HashMap<>(2);
    	params.put(UsuarioPerfilQuery.PARAM_USUARIO_LOGIN, usuarioLogin);
    	params.put(UsuarioPerfilQuery.PARAM_DS_PERFIL_TEMPLATE, descricaoPerfil);
    	params.put(UsuarioPerfilQuery.PARAM_ATIVO, ativo);
    	return (Long) getNamedSingleResult(UsuarioPerfilQuery.EXISTE_USUARIO_COM_DESCRICAO_PERFIL_ATIVO, params) > 0;
    }
    
    public List<PessoaFisica> listByLocalizacaoAtivo(Localizacao localizacao) {
        Map<String,Object> params = new HashMap<>(1);
        params.put(UsuarioPerfilQuery.PARAM_LOCALIZACAO, localizacao.getIdLocalizacao());
        return getNamedResultList(UsuarioPerfilQuery.LIST_BY_LOCALIZACAO_ATIVO, params);
    }
    
    public List<UsuarioPerfil> listUsuarioPerfilByLocalizacaoAtivo(Localizacao localizacao) {
        Map<String,Object> params = new HashMap<>(1);
        params.put(UsuarioPerfilQuery.PARAM_LOCALIZACAO, localizacao.getIdLocalizacao());
        return getNamedResultList(UsuarioPerfilQuery.LIST_BY_USUARIO_PERFIL_LOCALIZACAO_ATIVO, params);
    }
    
    public boolean existeUsuarioPerfil(UsuarioLogin usuarioLogin, PerfilTemplate perfilTemplate, boolean ativo) {
    	CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
    	CriteriaQuery<Integer> query = cb.createQuery(Integer.class);
    	Root<UsuarioPerfil> up = query.from(UsuarioPerfil.class);
    	query.where(cb.equal(up.get(UsuarioPerfil_.usuarioLogin), usuarioLogin), cb.equal(up.get(UsuarioPerfil_.perfilTemplate), perfilTemplate),
    			cb.equal(up.get(UsuarioPerfil_.ativo), ativo));
    	query.select(cb.literal(1));
    	return getSingleResult(getEntityManager().createQuery(query)) != null;
    }
    
    public UsuarioPerfil getUsuarioPerfil(UsuarioLogin usuarioLogin, PerfilTemplate perfilTemplate, Localizacao localizacao) {
    	CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
    	CriteriaQuery<UsuarioPerfil> query = cb.createQuery(UsuarioPerfil.class);
    	Root<UsuarioPerfil> up = query.from(UsuarioPerfil.class);
    	query.where(cb.equal(up.get(UsuarioPerfil_.usuarioLogin), usuarioLogin), cb.equal(up.get(UsuarioPerfil_.perfilTemplate), perfilTemplate),
    			cb.equal(up.get(UsuarioPerfil_.localizacao), localizacao));
    	return getSingleResult(getEntityManager().createQuery(query));
    }
}
