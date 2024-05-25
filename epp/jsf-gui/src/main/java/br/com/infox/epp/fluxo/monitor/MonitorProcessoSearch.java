package br.com.infox.epp.fluxo.monitor;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;

import com.google.common.base.Strings;

import br.com.infox.cdi.producer.EntityManagerProducer;
import br.com.infox.epp.fluxo.entity.Fluxo;

@Stateless
public class MonitorProcessoSearch {

    private EntityManager getEntityManager() {
        return EntityManagerProducer.getEntityManager();
    }

    public List<MonitorTarefaDTO> listTarefaHumanaByProcessDefinition(Long processDefinition, String key) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<MonitorTarefaDTO> cq = cb.createQuery(MonitorTarefaDTO.class);

        Root<TaskInstance> ti = cq.from(TaskInstance.class);
        Join<TaskInstance, Task> t = ti.join("task", JoinType.INNER);
        Join<Task, Node> n = t.join("taskNode", JoinType.INNER);

        cq.groupBy(n.get("key"));
        cq.select(cb.construct(MonitorTarefaDTO.class, n.<String>get("key"), cb.countDistinct(ti.get("processInstance").get("id"))));

        Predicate where = cq.getRestriction();
        where = cb.and(cb.equal(t.get("processDefinition").get("id"), processDefinition),
                cb.isNull(ti.get("end")));
        if (!Strings.isNullOrEmpty(key)) {
            where = cb.and(where, cb.equal(n.get("key"), key));
        }
        cq.where(where);

        return getEntityManager().createQuery(cq).getResultList();
    }

    public List<MonitorTarefaDTO> listNosAutomaticosByProcessDefinition(Long processDefinition, String key) {
        String where = "where token.end IS NULL "
                + "and token.lock is null "
                + "and node.class IN ('N', 'M', 'D') "
                + "and node.processDefinition.id = :idProcessDefinition "
                + "and exists(select 1 from LongInstance v where v.name = 'processo' and v.token = token "
                + " and exists(select 1 from Processo p where p.idProcesso = v.value)) ";
        if (!Strings.isNullOrEmpty(key)) {
            where += "and node.key = :key ";
        }

        String qlString = "select new br.com.infox.epp.fluxo.monitor.MonitorTarefaDTO(node.key, count(token.id)) "
                + "from org.jbpm.graph.exe.Token token "
                    + "inner join token.node node " + where
                + "group by node.key";

        TypedQuery<MonitorTarefaDTO> typedQuery = getEntityManager().createQuery(qlString, MonitorTarefaDTO.class);
        typedQuery.setParameter("idProcessDefinition", processDefinition);
        if (!Strings.isNullOrEmpty(key)) {
            typedQuery.setParameter("key", key);
        }
        return typedQuery.getResultList();
    }

    public ProcessDefinition getProcessDefinitionByFluxo(Fluxo f) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<ProcessDefinition> cq = cb.createQuery(ProcessDefinition.class);
        Root<ProcessDefinition> pd = cq.from(ProcessDefinition.class);
        cq.select(pd);
        cq.where(cb.equal(pd.get("name"), f.getFluxo()));
        cq.orderBy(cb.desc(pd.get("version")));
        TypedQuery<ProcessDefinition> query = getEntityManager().createQuery(cq);
        List<ProcessDefinition> fluxoList = query.getResultList();
        return fluxoList != null && !fluxoList.isEmpty() ? fluxoList.get(-0) : null;
    }
}
