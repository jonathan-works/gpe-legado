package br.com.infox.epp.painel;

import br.com.infox.epp.cdi.ViewScoped;
import br.com.infox.epp.processo.situacao.manager.SituacaoProcessoManager;
import br.com.infox.jsf.util.JsfUtil;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Named
@ViewScoped

public class TaskBeanLazyDataModel extends LazyDataModel<TaskBean> {
    @Inject
    private SituacaoProcessoManager situacaoProcessoManager;

    @Getter
    @Setter
    private TarefaBean tarefaBean;

    private String numeroProcessoPesquisa;

    private Date dataDePesquisa;

    private Date dataAtePesquisa;

    @Getter
    private static final int MAX_RESULTS = 10;

    @Override
    public List<TaskBean> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, Object> filters) {
        setRowCount(tarefaBean.getQtd());
        return  situacaoProcessoManager.loadTasks(tarefaBean,numeroProcessoPesquisa, dataDePesquisa, dataAtePesquisa, first, MAX_RESULTS);
    }

    public void search(TarefaBean tarefaBean, String numeroProcessoPesquisa, Date dataDePesquisa, Date dataAtePesquisa) {
        this.dataAtePesquisa = dataAtePesquisa;
        this.dataDePesquisa = dataDePesquisa;
        this.numeroProcessoPesquisa = numeroProcessoPesquisa;
        this.tarefaBean = tarefaBean;
        setWrappedData(null);
        getDataTable().reset();
        getDataTable().setRows(MAX_RESULTS);
        getDataTable().loadLazyData();
    }

    public void setDataTable(DataTable dataTable) {
        JsfUtil.instance().setRequestValue(getClass().getName() + "_binding", dataTable);
    }

    public DataTable getDataTable() {
        return JsfUtil.instance().getRequestValue(getClass().getName() + "_binding", DataTable.class);
    }

}