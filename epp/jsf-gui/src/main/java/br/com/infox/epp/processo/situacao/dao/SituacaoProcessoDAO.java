package br.com.infox.epp.processo.situacao.dao;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;

import br.com.infox.epp.painel.TarefaBean;
import br.com.infox.epp.processo.status.entity.StatusProcesso;
import br.com.infox.epp.view.query.ViewSituacaoProcessoQuery;
import org.jbpm.context.exe.variableinstance.LongInstance;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.PooledActor;
import org.jbpm.taskmgmt.exe.TaskInstance;

import br.com.infox.core.persistence.PersistenceController;
import br.com.infox.core.util.StringUtil;
import br.com.infox.epp.access.api.Authenticator;
import br.com.infox.epp.access.entity.Localizacao_;
import br.com.infox.epp.access.entity.PerfilTemplate;
import br.com.infox.epp.access.entity.UsuarioLogin_;
import br.com.infox.epp.fluxo.entity.Fluxo;
import br.com.infox.epp.fluxo.entity.Fluxo_;
import br.com.infox.epp.painel.FluxoBean;
import br.com.infox.epp.painel.TaskBean;
import br.com.infox.epp.pessoa.entity.PessoaFisica;
import br.com.infox.epp.processo.entity.Processo;
import br.com.infox.epp.processo.entity.Processo_;
import br.com.infox.epp.processo.metadado.entity.MetadadoProcesso;
import br.com.infox.epp.processo.metadado.entity.MetadadoProcesso_;
import br.com.infox.epp.processo.metadado.type.EppMetadadoProvider;
import br.com.infox.epp.processo.sigilo.entity.SigiloProcesso;
import br.com.infox.epp.processo.sigilo.entity.SigiloProcessoPermissao;
import br.com.infox.epp.processo.sigilo.entity.SigiloProcessoPermissao_;
import br.com.infox.epp.processo.sigilo.entity.SigiloProcesso_;
import br.com.infox.epp.processo.type.TipoProcesso;
import br.com.infox.epp.view.ViewSituacaoProcesso;
import br.com.infox.epp.view.ViewSituacaoProcesso_;
import br.com.infox.ibpm.type.PooledActorType;

import static br.com.infox.epp.view.query.ViewSituacaoProcessoQuery.*;
import static java.util.stream.Collectors.groupingBy;

@Stateless
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class SituacaoProcessoDAO extends PersistenceController {

	public List<FluxoBean> getFluxoList(TipoProcesso tipoProcesso, boolean comunicacoesExpedidas, String numeroProcessoRootFilter, StatusProcesso statusArquivado) {
        StringBuilder query = new StringBuilder("");
        query.append(ViewSituacaoProcessoQuery.FLUXO_QTD_QUERY);

        Map<String, Object> params = new HashMap<>();

        query.append(ViewSituacaoProcessoQuery.subQuerySigilo);
        query.append(ViewSituacaoProcessoQuery.subQueryTipoProcessoNull);
        params.put(PARAM_TIPO_PROCESSO_METADADO, EppMetadadoProvider.TIPO_PROCESSO.getMetadadoType());

        query.append(ViewSituacaoProcessoQuery.subQueryTipoProcessoFilter);
        params.put(PARAM_ID_PERFIL_TEMPLATE, Authenticator.getUsuarioPerfilAtual().getPerfilTemplate().getId().toString());
        params.put(PARAM_NOME_USUARIO_LOGIN, Authenticator.getUsuarioLogado().getLogin());
        params.put(PARAM_TIPO_USER, PooledActorType.USER.getValue());
        params.put(PARAM_TIPO_GROUP, PooledActorType.GROUP.getValue());
        params.put(PARAM_TIPO_LOCAL, PooledActorType.LOCAL.getValue());
        params.put(PARAM_CODIGO_PERFIL_TEMPLATE, Authenticator.getUsuarioPerfilAtual().getPerfilTemplate().getCodigo());
        params.put(PARAM_CODIGO_LOCALIZACAO, Authenticator.getLocalizacaoAtual().getCodigo());
        params.put(PARAM_ID_USUARIO_LOGIN, Authenticator.getUsuarioLogado().getIdUsuarioLogin());

        query.append(")");

        if(!Objects.isNull(statusArquivado)) {
            query.append(PROCESSO_NAO_ARQUIVADO_QUERY);
            params.put(PARAM_ID_STATUS, statusArquivado);
        }

        if (!StringUtil.isEmpty(numeroProcessoRootFilter)) {
            query.append(ViewSituacaoProcessoQuery.queryProcRoot);
            params.put(PARAM_NUMERO_PROCESSO_ROOT, numeroProcessoRootFilter);
        }


        query.append(" group by flux.ds_fluxo, flux.id_fluxo ");

        Query nativeQuery = getEntityManager().createNativeQuery(query.toString());

        params.entrySet().forEach( p -> {
            nativeQuery.setParameter(p.getKey(), p.getValue());
        });

        List<Object[]> resultList = nativeQuery.getResultList();
        List<FluxoBean> result = new LinkedList<>();
        for(Object[] record : resultList){
            result.add(new FluxoBean(String.valueOf(record[1]), String.valueOf(record[0]), Long.valueOf(record[2].toString()), null, comunicacoesExpedidas == true ? "true" : "false", numeroProcessoRootFilter));
        }

        return result;

	}

	public List<TaskBean> getTaskIntances(FluxoBean fluxoBean) {

        Map<String, Object> params = new HashMap<>();

        StringBuilder query = new StringBuilder("");
        query.append(ViewSituacaoProcessoQuery.TASK_INSTANCES_QUERY);
        query.append(ViewSituacaoProcessoQuery.subQuerySigilo);

        if(fluxoBean.getTipoProcesso() == null){
            query.append(ViewSituacaoProcessoQuery.subQueryTipoProcessoNull);
            params.put(PARAM_TIPO_PROCESSO_METADADO, EppMetadadoProvider.TIPO_PROCESSO.getMetadadoType());
        }else{
            query.append(ViewSituacaoProcessoQuery.subQueryTipoProcesso);
            params.put(PARAM_TIPO_PROCESSO_METADADO, EppMetadadoProvider.TIPO_PROCESSO.getMetadadoType());
            params.put(PARAM_TIPO_PROCESSO_VALOR, fluxoBean.getTipoProcesso().value());
        }

        if (TipoProcesso.COMUNICACAO.equals(fluxoBean.getTipoProcesso())) {
            if (fluxoBean.getExpedida() != null && fluxoBean.getExpedida()) {
               query.append(ViewSituacaoProcessoQuery.queryLocExpedidor);
               params.put(PARAM_ID_LOCALIZACAO, Authenticator.getLocalizacaoAtual().getIdLocalizacao());
            } else {
                PessoaFisica pessoaFisica = Authenticator.getUsuarioLogado().getPessoaFisica();
                Integer idPessoaFisica = pessoaFisica == null ? -1 : pessoaFisica.getIdPessoa();
                query.append(ViewSituacaoProcessoQuery.queryDestDestinatario);
                params.put(PARAM_LOCALIZACAO_DESTINO, EppMetadadoProvider.LOCALIZACAO_DESTINO.getMetadadoType());
                params.put(PARAM_ID_LOCALIZACAO, Authenticator.getLocalizacaoAtual().getIdLocalizacao());
                params.put(PARAM_PERFIL_DESTINO, EppMetadadoProvider.PERFIL_DESTINO.getMetadadoType());
                params.put(PARAM_ID_PERFIL_TEMPLATE, Authenticator.getUsuarioPerfilAtual().getPerfilTemplate().getId());
                params.put(PARAM_PERFIL_DESTINO, EppMetadadoProvider.PERFIL_DESTINO.getMetadadoType());
                params.put(PARAM_ID_PESSOA_FISICA, idPessoaFisica);
            }
        } else {
            query.append(ViewSituacaoProcessoQuery.subQueryTipoProcessoFilter);
            params.put(PARAM_ID_PERFIL_TEMPLATE, Authenticator.getUsuarioPerfilAtual().getPerfilTemplate().getId().toString());
            params.put(PARAM_NOME_USUARIO_LOGIN, Authenticator.getUsuarioLogado().getLogin());
            params.put(PARAM_TIPO_USER, PooledActorType.USER.getValue());
            params.put(PARAM_TIPO_GROUP, PooledActorType.GROUP.getValue());
            params.put(PARAM_TIPO_LOCAL, PooledActorType.LOCAL.getValue());
        }

        params.put(PARAM_CODIGO_PERFIL_TEMPLATE, Authenticator.getUsuarioPerfilAtual().getPerfilTemplate().getCodigo());
        params.put(PARAM_CODIGO_LOCALIZACAO, Authenticator.getLocalizacaoAtual().getCodigo());

        if (!StringUtil.isEmpty(fluxoBean.getNumeroProcessoRootFilter())) {
            query.append(ViewSituacaoProcessoQuery.queryProcRoot);
            params.put(PARAM_NUMERO_PROCESSO_ROOT, fluxoBean.getNumeroProcessoRootFilter());
        }
        query.append(")");
        query.append(" OPTION (FORCE ORDER)");

        params.put(PARAM_PROCESS_DEFINITION, fluxoBean.getProcessDefinitionId());
        params.put(PARAM_ID_USUARIO_LOGIN, Authenticator.getUsuarioLogado().getIdUsuarioLogin());

        Query nativeQuery = getEntityManager().createNativeQuery(query.toString());

        params.entrySet().forEach( p -> {
            nativeQuery.setParameter(p.getKey(), p.getValue());
        });

        List<Object[]> resultList = nativeQuery.getResultList();
        List<TaskBean> result = new LinkedList<>();
        for(Object[] record : resultList){
            result.add(new TaskBean(record, fluxoBean.isPodeVisualizarComExcessao()));
        }

        return result;
    }

    protected void appendNumeroProcessoRootFilter(AbstractQuery<?> abstractQuery, String numeroProcesso, Path<Processo> processoRoot) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        abstractQuery.where(cb.like(processoRoot.get(Processo_.numeroProcesso), cb.literal("%" + numeroProcesso + "%")),
                abstractQuery.getRestriction());
    }

    public void appendTipoProcessoFilters(AbstractQuery<?> abstractQuery, TipoProcesso tipoProcesso, Boolean comunicacoesExpedidas,
            From<?, TaskInstance> taskInstance, From<?, Processo> processo) {
        if (TipoProcesso.COMUNICACAO.equals(tipoProcesso)) {
            if (comunicacoesExpedidas != null && comunicacoesExpedidas) {
                appendLocalizacaoExpedidoraFilter(abstractQuery, processo);
            } else {
                appendDestinoOrDestinatarioFilter(abstractQuery, processo);
            }
        } else {
            appendPooledActorFilter(abstractQuery, taskInstance);
        }
    }

	protected void appendSigiloProcessoFilter(AbstractQuery<?> principalQuery, Path<Processo> processo) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        Subquery<Integer> existsSigiloProcesso = principalQuery.subquery(Integer.class);
        Root<SigiloProcesso> sigiloProcesso = existsSigiloProcesso.from(SigiloProcesso.class);
        existsSigiloProcesso.select(cb.literal(1));
        Predicate whereSigiloProcesso = cb.equal(sigiloProcesso.get(SigiloProcesso_.processo).get(Processo_.idProcesso), processo.get(Processo_.idProcesso));
        whereSigiloProcesso = cb.and(cb.equal(sigiloProcesso.get("ativo"), true), whereSigiloProcesso);
        whereSigiloProcesso = cb.and(cb.isTrue(sigiloProcesso.get(SigiloProcesso_.sigiloso)), whereSigiloProcesso);
        existsSigiloProcesso.where(whereSigiloProcesso);

        Subquery<Integer> existsSigiloProcessoPermissao = principalQuery.subquery(Integer.class);
        Root<SigiloProcessoPermissao> sigiloProcessoPermissao = existsSigiloProcessoPermissao.from(SigiloProcessoPermissao.class);
        existsSigiloProcessoPermissao.select(cb.literal(1));

        Subquery<Integer> subquery3 = existsSigiloProcessoPermissao.subquery(Integer.class);
        Root<SigiloProcesso> sigiloProcesso2 = subquery3.from(SigiloProcesso.class);
        subquery3.select(sigiloProcesso2.get(SigiloProcesso_.id).as(Integer.class));
        Predicate predicateSubquery3 = cb.equal(sigiloProcesso2.get(SigiloProcesso_.processo).get(Processo_.idProcesso), processo.get(Processo_.idProcesso));
        predicateSubquery3 = cb.and(cb.isTrue(sigiloProcesso2.get(SigiloProcesso_.ativo)) , predicateSubquery3);
        subquery3.where(predicateSubquery3);

        Integer idUsuarioLogado = Authenticator.getUsuarioLogado().getIdUsuarioLogin();
        Predicate whereSigiloProcessoPermissao = sigiloProcessoPermissao.get(SigiloProcessoPermissao_.sigiloProcesso).get(SigiloProcesso_.id).in(subquery3);
        whereSigiloProcessoPermissao = cb.and(cb.equal(sigiloProcessoPermissao.get(SigiloProcessoPermissao_.ativo), Boolean.TRUE), whereSigiloProcessoPermissao);
        whereSigiloProcessoPermissao = cb.and(cb.equal(sigiloProcessoPermissao.get(SigiloProcessoPermissao_.usuario).get(UsuarioLogin_.idUsuarioLogin), idUsuarioLogado), whereSigiloProcessoPermissao);
        existsSigiloProcessoPermissao.where(whereSigiloProcessoPermissao);

        Predicate predicate = principalQuery.getRestriction();
        predicate = cb.and(cb.or(cb.not(cb.exists(existsSigiloProcesso)), cb.exists(existsSigiloProcessoPermissao)), predicate);
        principalQuery.where(predicate);
    }

	protected void appendDestinoOrDestinatarioFilter(AbstractQuery<?> abstractQuery, From<?, Processo> processo) {
		CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        Subquery<Integer> subqueryDestino = createSubqueryDestino(abstractQuery, processo);
        Subquery<Integer> subqueryPerfilDestino = createSubqueryPerfilDestino(abstractQuery, processo);
        Subquery<Integer> subqueryDestinatario = createSubqueryDestinatario(abstractQuery, processo);
        Predicate predicateQuery = abstractQuery.getRestriction();
        predicateQuery =
                cb.and(
                        cb.or(
                                cb.and(
                                    cb.exists(subqueryDestino),
                                    cb.exists(subqueryPerfilDestino)
                                ),
                                cb.exists(subqueryDestinatario)
                        ),
                        predicateQuery
                );
        abstractQuery.where(predicateQuery);
    }

	protected void appendLocalizacaoExpedidoraFilter(AbstractQuery<?> abstractQuery, From<?, Processo> processo) {
	    CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        Integer idLocalizacao = Authenticator.getLocalizacaoAtual().getIdLocalizacao();
        Predicate predicate = abstractQuery.getRestriction();
        abstractQuery.where(
                cb.and(
                        cb.equal(processo.get(Processo_.localizacao).get(Localizacao_.idLocalizacao), idLocalizacao),
                        predicate
                )
        );
	}

	private Subquery<Integer> createSubqueryPerfilDestino(AbstractQuery<?> abstractQuery, From<?, Processo> processo) {
	    CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        Subquery<Integer> subquery = abstractQuery.subquery(Integer.class);
        Root<MetadadoProcesso> metadado = subquery.from(MetadadoProcesso.class);
        subquery.select(cb.literal(1));
        String metadadoDestino = EppMetadadoProvider.PERFIL_DESTINO.getMetadadoType();
        Subquery<Integer> subqueryExistsMetadado = abstractQuery.subquery(Integer.class);
        Root<MetadadoProcesso> metadadoExists = subqueryExistsMetadado.from(MetadadoProcesso.class);
        subqueryExistsMetadado.select(cb.literal(1));
        Predicate predicateSubqueryExists = cb.and(cb.equal(metadadoExists.get(MetadadoProcesso_.metadadoType), metadadoDestino));
        predicateSubqueryExists = cb.and(cb.equal(metadadoExists.get(MetadadoProcesso_.processo).get(Processo_.idProcesso), processo.get(Processo_.idProcesso)), predicateSubqueryExists);
        subqueryExistsMetadado.where(predicateSubqueryExists);
        Integer idPerfilAtual = Authenticator.getUsuarioPerfilAtual().getPerfilTemplate().getId();
        Predicate predicateSubquery = cb.and(cb.equal(metadado.get(MetadadoProcesso_.metadadoType), metadadoDestino));
        predicateSubquery = cb.and(cb.equal(metadado.get(MetadadoProcesso_.valor), idPerfilAtual.toString()), predicateSubquery);
        predicateSubquery = cb.and(cb.equal(metadado.get(MetadadoProcesso_.processo).get(Processo_.idProcesso), processo.get(Processo_.idProcesso)), predicateSubquery);
        predicateSubquery = cb.or(cb.not(cb.exists(subqueryExistsMetadado)), predicateSubquery);
        subquery.where(predicateSubquery);
        return subquery;
    }

    private Subquery<Integer> createSubqueryDestino(AbstractQuery<?> abstractQuery, From<?, Processo> processo) {
		CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
		Subquery<Integer> subquery = abstractQuery.subquery(Integer.class);
        Root<MetadadoProcesso> metadado = subquery.from(MetadadoProcesso.class);
        subquery.select(cb.literal(1));
        String metadadoDestino = EppMetadadoProvider.LOCALIZACAO_DESTINO.getMetadadoType();
        Integer idLocalizacao = Authenticator.getLocalizacaoAtual().getIdLocalizacao();
        Predicate predicateSubquery = cb.and(cb.equal(metadado.get(MetadadoProcesso_.metadadoType), metadadoDestino));
        predicateSubquery = cb.and(cb.equal(metadado.get(MetadadoProcesso_.valor), idLocalizacao.toString()), predicateSubquery);
        predicateSubquery = cb.and(cb.equal(metadado.get(MetadadoProcesso_.processo).get(Processo_.idProcesso), processo.get(Processo_.idProcesso)), predicateSubquery);
        subquery.where(predicateSubquery);
		return subquery;
	}

    protected Subquery<Integer> createSubqueryDestinatario(AbstractQuery<?> abstractQuery,  From<?, Processo> processo) {
        String metadadoDestinatario = EppMetadadoProvider.PESSOA_DESTINATARIO.getMetadadoType();
        PessoaFisica pessoaFisica = Authenticator.getUsuarioLogado().getPessoaFisica();
        Integer idPessoaFisica = pessoaFisica == null ? -1 : pessoaFisica.getIdPessoa();
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        Subquery<Integer> subquery = abstractQuery.subquery(Integer.class);
        Root<MetadadoProcesso> metadado = subquery.from(MetadadoProcesso.class);
        subquery.select(cb.literal(1));
        Predicate predicateSubquery = cb.and(cb.equal(metadado.get(MetadadoProcesso_.metadadoType), metadadoDestinatario));
        predicateSubquery = cb.and(cb.equal(metadado.get(MetadadoProcesso_.processo).get(Processo_.idProcesso), processo.get(Processo_.idProcesso)), predicateSubquery);
        predicateSubquery = cb.and(cb.equal(metadado.get(MetadadoProcesso_.valor), idPessoaFisica.toString()), predicateSubquery);
        subquery.where(predicateSubquery);
        return subquery;
    }

    protected void appendTipoProcessoFilter(AbstractQuery<?> abstractQuery, TipoProcesso tipoProcesso, Path<Processo> processo) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        Subquery<Integer> subquery = abstractQuery.subquery(Integer.class);
        Root<MetadadoProcesso> metadado = subquery.from(MetadadoProcesso.class);
        subquery.select(cb.literal(1));
        String metadadoTipoProcesso = EppMetadadoProvider.TIPO_PROCESSO.getMetadadoType();
        Predicate predicateSubquery = cb.equal(metadado.get(MetadadoProcesso_.metadadoType), metadadoTipoProcesso);
        if (tipoProcesso != null) {
            predicateSubquery = cb.and(cb.equal(metadado.get(MetadadoProcesso_.valor), tipoProcesso.toString()), predicateSubquery);
        }
        predicateSubquery = cb.and(cb.equal(metadado.get(MetadadoProcesso_.processo).get(Processo_.idProcesso), processo.get(Processo_.idProcesso)), predicateSubquery);
        subquery.where(predicateSubquery);
        Predicate predicate = abstractQuery.getRestriction();
        if (tipoProcesso == null) {
            predicate = cb.and(cb.not(cb.exists(subquery)), predicate);
        } else {
            predicate = cb.and(cb.exists(subquery), predicate);
        }
        abstractQuery.where(predicate);
    }

    protected void appendPooledActorFilter(AbstractQuery<?> abstractQuery, From<?, TaskInstance> taskInstance) {
        PerfilTemplate perfilTemplate = Authenticator.getUsuarioPerfilAtual().getPerfilTemplate();
        String login = Authenticator.getUsuarioLogado().getLogin();
        String localizacao = Authenticator.getLocalizacaoAtual().getCodigo();
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        Subquery<Integer> subquery = abstractQuery.subquery(Integer.class);
        subquery.select(cb.literal(1));
        Root<PooledActor> pooledActor = subquery.from(PooledActor.class);
        Join<PooledActor, TaskInstance> taskInstances = pooledActor.join("taskInstances", JoinType.INNER);
        subquery.where(
            cb.equal(taskInstance.<Long>get("id"), taskInstances.<Long>get("id")),
            cb.or(
                cb.and(
                    cb.isNull(pooledActor.get("type")),
                    cb.or(
                        cb.equal(pooledActor.<String>get("actorId"), cb.literal(perfilTemplate.getId().toString())),
                        cb.equal(pooledActor.<String>get("actorId"), cb.literal(perfilTemplate.getCodigo()))
                    )
                ),
                cb.and(
                    cb.equal(pooledActor.<String>get("actorId"), cb.literal(login)),
                    cb.equal(pooledActor.<String>get("type"), PooledActorType.USER.getValue())
                ),
                cb.and(
                    cb.equal(pooledActor.<String>get("actorId"), cb.literal(localizacao+"&"+perfilTemplate.getCodigo())),
                    cb.equal(pooledActor.<String>get("type"), PooledActorType.GROUP.getValue())
                ),
                cb.and(
                    cb.equal(pooledActor.<String>get("actorId"), cb.literal(localizacao)),
                    cb.equal(pooledActor.<String>get("type"), PooledActorType.LOCAL.getValue())
                )
            )
        );
        Predicate predicate = abstractQuery.getRestriction();
        abstractQuery.where(cb.and(cb.exists(subquery), predicate));
    }

	public boolean canOpenTask(long idTaskInstance, TipoProcesso tipoProcesso, Boolean comunicacoesExpedidas) {
	    CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<TaskInstance> taskInstance = cq.from(TaskInstance.class);
        Root<LongInstance> variableInstance = cq.from(LongInstance.class);
        Root<Processo> processo = cq.from(Processo.class);
        Join<TaskInstance, ProcessInstance> processInstance = taskInstance.join("processInstance", JoinType.INNER);

        cq.select(cb.count(taskInstance));

        cq.where(
                cb.equal(variableInstance.get("processInstance").<Long>get("id"), processInstance.<Long>get("id")),
                cb.equal(variableInstance.<String>get("name"), cb.literal("processo")),
                cb.equal(variableInstance.<Long>get("value"), processo.get(Processo_.idProcesso)),
                cb.isNull(processInstance.<Date>get("end")),
                cb.isTrue(taskInstance.<Boolean>get("isOpen")),
                cb.isFalse(taskInstance.<Boolean>get("isSuspended")),
                cb.equal(taskInstance.<Long>get("id"), cb.literal(idTaskInstance))
        );

        appendSigiloProcessoFilter(cq, processo);
        appendTipoProcessoFilter(cq, tipoProcesso, processo);
        appendTipoProcessoFilters(cq, tipoProcesso, comunicacoesExpedidas, taskInstance, processo);
        TypedQuery<Long> query = getEntityManager().createQuery(cq);
        query.setHint("org.hibernate.cacheable", "true");
        Long count = query.getSingleResult();
        return count > 0;
	}

	public boolean canAccessProcesso(Integer idProcesso, TipoProcesso tipoProcesso, Boolean comunicacoesExpedidas) {
	    CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();

	    CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<ViewSituacaoProcesso> viewSituacaoProcesso = cq.from(ViewSituacaoProcesso.class);
        Join<ViewSituacaoProcesso, TaskInstance> taskInstance = viewSituacaoProcesso.join(ViewSituacaoProcesso_.taskInstance);
        Join<ViewSituacaoProcesso, ProcessInstance> processInstance = viewSituacaoProcesso.join(ViewSituacaoProcesso_.processInstance);
        Join<ViewSituacaoProcesso, Processo> processo = viewSituacaoProcesso.join(ViewSituacaoProcesso_.processo);

        cq.select(cb.count(taskInstance));

        cq.where(
                cb.isNull(processInstance.<Date>get("end")),
                cb.isTrue(taskInstance.<Boolean>get("isOpen")),
                cb.isFalse(taskInstance.<Boolean>get("isSuspended")),
                cb.equal(processo.get(Processo_.idProcesso), cb.literal(idProcesso))
        );

        appendSigiloProcessoFilter(cq, processo);
        appendTipoProcessoFilter(cq, tipoProcesso, processo);
        appendTipoProcessoFilters(cq, tipoProcesso, comunicacoesExpedidas, taskInstance, processo);
        TypedQuery<Long> query = getEntityManager().createQuery(cq);
        query.setHint("org.hibernate.cacheable", "true");
        Long count = query.getSingleResult();
        return count > 0;
	}

    public void appendMandatoryFilters(AbstractQuery<?> abstractQuery, TipoProcesso tipoProcesso, Path<Processo> processo) {
		appendSigiloProcessoFilter(abstractQuery, processo);
		appendTipoProcessoFilter(abstractQuery, tipoProcesso, processo);
	}

    private Fluxo getFluxoByIdProcessoAndIdTaskInstance(Integer idProcesso, String idTaskInstance) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Fluxo> cq = cb.createQuery(Fluxo.class);
        Root<TaskInstance> taskInstance = cq.from(TaskInstance.class);
        Root<Fluxo> fluxo = cq.from(Fluxo.class);
        Root<Processo> processo = cq.from(Processo.class);
        Join<TaskInstance, ProcessInstance> processInstance = taskInstance.join("processInstance", JoinType.INNER);
        Join<ProcessInstance, ProcessDefinition> processDefinition = processInstance.join("processDefinition", JoinType.INNER);

        cq.select(fluxo);
        cq.where(
           cb.equal(processo.get(Processo_.idProcesso), idProcesso),
           cb.equal(taskInstance.<Long>get("id").as(String.class), idTaskInstance),
           cb.equal(processDefinition.get("name"), fluxo.get(Fluxo_.fluxo))
        );
        return getEntityManager().createQuery(cq).getSingleResult();
    }

	protected Authenticator getAuthenticator() {
	    return Authenticator.instance();
	}

    public List<TarefaBean> getTasks(FluxoBean fluxoBean){
        Map<String, Object> params = new HashMap<>();

        StringBuilder query = new StringBuilder("");
        query.append(ViewSituacaoProcessoQuery.TASK_QUERY);
        query.append(ViewSituacaoProcessoQuery.subQuerySigilo);

        if(fluxoBean.getTipoProcesso() == null){
            query.append(ViewSituacaoProcessoQuery.subQueryTipoProcessoNull);
            params.put(PARAM_TIPO_PROCESSO_METADADO, EppMetadadoProvider.TIPO_PROCESSO.getMetadadoType());
        }else{
            query.append(ViewSituacaoProcessoQuery.subQueryTipoProcesso);
            params.put(PARAM_TIPO_PROCESSO_METADADO, EppMetadadoProvider.TIPO_PROCESSO.getMetadadoType());
            params.put(PARAM_TIPO_PROCESSO_VALOR, fluxoBean.getTipoProcesso().value());
        }


            query.append(ViewSituacaoProcessoQuery.subQueryTipoProcessoFilter);
            params.put(PARAM_ID_PERFIL_TEMPLATE, Authenticator.getUsuarioPerfilAtual().getPerfilTemplate().getId().toString());
            params.put(PARAM_NOME_USUARIO_LOGIN, Authenticator.getUsuarioLogado().getLogin());
            params.put(PARAM_TIPO_USER, PooledActorType.USER.getValue());
            params.put(PARAM_TIPO_GROUP, PooledActorType.GROUP.getValue());
            params.put(PARAM_TIPO_LOCAL, PooledActorType.LOCAL.getValue());


        params.put(PARAM_CODIGO_PERFIL_TEMPLATE, Authenticator.getUsuarioPerfilAtual().getPerfilTemplate().getCodigo());
        params.put(PARAM_CODIGO_LOCALIZACAO, Authenticator.getLocalizacaoAtual().getCodigo());

        if (!StringUtil.isEmpty(fluxoBean.getNumeroProcessoRootFilter())) {
            query.append(ViewSituacaoProcessoQuery.queryProcRoot);
            params.put(PARAM_NUMERO_PROCESSO_ROOT, fluxoBean.getNumeroProcessoRootFilter());
        }
        query.append(")");

        query.append(" group by task.ID_, task.NAME_, vssp.id_fluxo, fluxo.ds_fluxo  ");

        params.put(PARAM_PROCESS_DEFINITION, fluxoBean.getProcessDefinitionId());
        params.put(PARAM_ID_USUARIO_LOGIN, Authenticator.getUsuarioLogado().getIdUsuarioLogin());

        Query nativeQuery = getEntityManager().createNativeQuery(query.toString());

        params.entrySet().forEach( p -> {
            nativeQuery.setParameter(p.getKey(), p.getValue());
        });

        List<Object[]> resultList = nativeQuery.getResultList();
        List<TarefaBean> result = new LinkedList<>();
        for(Object[] record : resultList){
            result.add(new TarefaBean( Integer.valueOf(record[0].toString()), String.valueOf(record[1]), Long.valueOf(record[2].toString()),Integer.valueOf(record[3].toString()), String.valueOf(record[4]), String.valueOf(record[5])));
        }

        return result;
    }

    public List<TaskBean> getTaskIntances(TarefaBean tarefaBean, String numeroProcessoPesquisa, Date dataDePesquisa, Date dataAtePesquisa, int offset, int max) {

        Map<String, Object> params = new HashMap<>();

        StringBuilder query = new StringBuilder("select Cast(taskins.ID_ as varchar) idti,")
                .append("task.NAME_ , ")
                .append("taskins.ASSIGNEE_, ")
                .append("Cast(processins.ID_ as varchar) as proid, ")
                .append("tasknode.KEY_, ")
                .append("proce.id_processo as idProcesso,")
                .append("caixa.nm_caixa ,")
                .append("caixa.id_caixa ,")
                .append("nat.ds_natureza as nomeNatureza,")
                .append("cat.ds_categoria as nomeCategoria,")
                .append("proce.nr_processo as numeroProcesso,")
                .append("procroot.id_processo ,")
                .append("procroot.nr_processo ,")
                .append("usulog.nm_usuario,")
                .append("prioproc.id_prioridade_processo, ")
                .append("prioproc.ds_prioridade_processo, ")
                .append("prioproc.nr_peso, ")
                .append("convert(varchar, proce.dt_inicio, 120), ")
                .append("natroot.ds_natureza ,")
                .append("catroot.ds_categoria ,")
                .append("vssp.in_documento_assinar ,")
                .append("fluxo.ds_fluxo ,")
                .append("fluxo.id_fluxo, ")
                .append("vi.NAME_ as movimentarLote ")
                .append("from  vs_situacao_processo vssp ")
                .append("inner join JBPM_TASKINSTANCE taskins on vssp.id_taskinstance = taskins.ID_  ")
                .append("inner join JBPM_PROCESSINSTANCE processins on vssp.id_processinstace = processins.ID_  ")
                .append("inner join tb_processo proce on vssp.id_processo = proce.id_processo ")
                .append("inner join tb_processo procroot on vssp.id_processo_root = procroot.id_processo_root ")
                .append("inner join tb_usuario_login usulog on vssp.id_usuario_cadastro_processo = usulog.id_usuario_login  ")
                .append("left join tb_caixa caixa on vssp.id_caixa = caixa.id_caixa ")
                .append("left join tb_prioridade_processo prioproc on vssp.id_prioridade_processo = prioproc.id_prioridade_processo ")
                .append("inner join tb_natureza_categoria_fluxo natcat on vssp.id_natureza_categoria_fluxo = natcat.id_natureza_categoria_fluxo ")
                .append("inner join tb_natureza nat on natcat.id_natureza = nat.id_natureza ")
                .append("inner join tb_categoria cat on natcat.id_categoria  = cat.id_categoria ")
                .append("inner join tb_fluxo flux on natcat.id_fluxo = flux.id_fluxo ")
                .append("inner join tb_natureza_categoria_fluxo natcatroot on vssp.id_natureza_categoria_fluxo_root = natcatroot.id_natureza_categoria_fluxo ")
                .append("inner join tb_natureza natroot on natcatroot.id_natureza = natroot.id_natureza ")
                .append("inner join tb_categoria catroot on natcatroot.id_categoria  = catroot.id_categoria ")
                .append("inner join JBPM_TASK task on taskins.TASK_ = task.ID_  ")
                .append("inner join JBPM_NODE tasknode on task.TASKNODE_ = tasknode.ID_ ")
                .append("inner join dbo.JBPM_PROCESSDEFINITION processdefin on  processins.PROCESSDEFINITION_ = processdefin.ID_ " )
                .append("inner join dbo.tb_fluxo fluxo on processdefin.NAME_ = fluxo.ds_fluxo ")
                .append("left join JBPM_VARIABLEINSTANCE vi on (vi.TASKINSTANCE_ = taskins.ID_ AND vi.NAME_ = 'tarefaMovimentarLote' AND vi.STRINGVALUE_ = 'true') ")
                .append(" where task.ID_  = :tarefaID ")
                .append(" and proce.id_processo  in (:ids)");

        if(!StringUtil.isEmpty(numeroProcessoPesquisa)){
            query.append(ViewSituacaoProcessoQuery.queryProcRoot);
            params.put(PARAM_NUMERO_PROCESSO_ROOT, numeroProcessoPesquisa);
        }

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        if (dataDePesquisa != null && dataAtePesquisa != null) {
            String strDataInicio = dateFormat.format(dataDePesquisa);
            String strDataFim = dateFormat.format(dataAtePesquisa);
            query.append(String.format(" and (proce.dt_inicio between '%s' and '%s') ", strDataInicio, strDataFim));
        } else if(dataDePesquisa != null && dataAtePesquisa == null){
            String strDataInicio = dateFormat.format(dataDePesquisa);
            query.append(String.format(" and (proce.dt_inicio >= '%s')", strDataInicio));
        }else if(dataDePesquisa == null && dataAtePesquisa != null){
            String strDataFim = dateFormat.format(dataAtePesquisa);
            query.append(String.format(" and (proce.dt_inicio <= '%s')", strDataFim));
        }

        List<Integer> ids = new ArrayList<>();
        String[] split = tarefaBean.getIds().split(",");
        for (String id: split) {
            ids.add(Integer.valueOf(id));
        }


        query.append(" order by proce.id_processo asc ")
                .append("  OFFSET  ")
                .append("  :offSetT ")
                .append("   ROWS FETCH NEXT  ")
                .append("  :maxR  ")
                .append(" ROWS ONLY ");

        params.put("tarefaID", tarefaBean.getIdTarefa());
        params.put("ids", ids);
        params.put("offSetT", offset);
        params.put("maxR", max);

        Query nativeQuery = getEntityManager().createNativeQuery(query.toString());

        params.entrySet().forEach( p -> {
            nativeQuery.setParameter(p.getKey(), p.getValue());
        });

        List<Object[]> resultList = nativeQuery.getResultList();
        List<TaskBean> result = new LinkedList<>();
        for(Object[] record : resultList){
            result.add(new TaskBean(record, false));
        }

        return result;
    }
}
