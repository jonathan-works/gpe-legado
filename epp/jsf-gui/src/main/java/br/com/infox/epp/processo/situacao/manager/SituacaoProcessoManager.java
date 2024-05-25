package br.com.infox.epp.processo.situacao.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import br.com.infox.epp.painel.FluxoBean;
import br.com.infox.epp.painel.TarefaBean;
import br.com.infox.epp.painel.TaskBean;
import br.com.infox.epp.processo.situacao.dao.SituacaoProcessoDAO;
import br.com.infox.epp.processo.status.entity.StatusProcesso;
import br.com.infox.epp.processo.status.manager.StatusProcessoSearch;
import br.com.infox.epp.processo.type.TipoProcesso;
import br.com.infox.seam.security.SecurityUtil;

@Stateless
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class SituacaoProcessoManager {

    private static final String statusProcessoArquivado = "Processo Arquivado";

    @Inject
    private SituacaoProcessoDAO situacaoProcessoDAO;
    @Inject
    protected SecurityUtil securityUtil;
    @Inject
    private StatusProcessoSearch statusProcessoSearch;

    public List<FluxoBean> getFluxos(List<TipoProcesso> tiposProcessosDisponiveis, String numeroProcessoRoot) {
        List<FluxoBean> result = new ArrayList<>();
        StatusProcesso statusArquivado = statusProcessoSearch.getStatusByNameAtivo(statusProcessoArquivado);

        List<FluxoBean> fluxos = situacaoProcessoDAO.getFluxoList(null, false, numeroProcessoRoot, statusArquivado);
        result.addAll(fluxos);

        Collections.sort(result);
        return result;
    }

    public void loadTasks(FluxoBean fluxoBean) {
        if (fluxoBean.getTasks() == null) {
            List<TaskBean> taskBeans = situacaoProcessoDAO.getTaskIntances(fluxoBean);
            for (TaskBean taskBean : taskBeans) {
                fluxoBean.addTaskDefinition(taskBean);
            }
        }
    }

    public List<TarefaBean> carregarTarefas(FluxoBean fluxoBean){
        return situacaoProcessoDAO.getTasks(fluxoBean);
    }

    public List<TaskBean> loadTasks(TarefaBean tarefaBean, String numeroProcessoPesquisa, Date dataDePesquisa, Date dataAtePesquisa, int offset, int max) {

       return situacaoProcessoDAO.getTaskIntances(tarefaBean,numeroProcessoPesquisa, dataDePesquisa, dataAtePesquisa, offset, max);

    }
}
