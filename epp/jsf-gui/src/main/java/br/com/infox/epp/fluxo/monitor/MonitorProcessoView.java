package br.com.infox.epp.fluxo.monitor;

import java.io.Serializable;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.seam.faces.FacesMessages;
import org.primefaces.component.datatable.DataTable;

import br.com.infox.epp.cdi.ViewScoped;
import br.com.infox.epp.fluxo.crud.FluxoController;
import br.com.infox.epp.processo.node.AutomaticNodeService;
import br.com.infox.jbpm.graphic.GraphicExecutionView;
import br.com.infox.jsf.function.JsfFunctions;
import br.com.infox.log.LogProvider;
import br.com.infox.log.Logging;

@Named
@ViewScoped
public class MonitorProcessoView implements Serializable {
    private static final String DATATABLE_ID = "instanciaDataTable";

    private static final long serialVersionUID = 1L;

    @Inject
    private AutomaticNodeService automaticNodeService;
    @Inject
    private MonitorProcessoService monitorProcessoService;
    @Inject
    private GraphicExecutionView graphicExecutionView;
    @Inject
    private FluxoController fluxoController;

    private LogProvider LOG = Logging.getLogProvider(MonitorProcessoView.class);

    private boolean success;
    private MonitorProcessoDTO monitor;
    private MonitorProcessoDTO filterMonitor;
    private boolean filter;
    private String filterKey;
    private boolean executionGraphic;
    private MonitorProcessoDataModel monitorProcessoDataModel;

    public void selectFluxo() {
        filter = false;
        try {
            monitor = monitorProcessoService.createSvgMonitoramentoProcesso(fluxoController.getFluxo(), null);
            monitorProcessoDataModel = new MonitorProcessoDataModel(monitor.getProcessDefinition().getId());
            success = true;
        } catch (Exception e) {
            success = false;
            LOG.error("Erro ao tentar carregar SVG do fluxo " + fluxoController.getFluxo().getCodFluxo(), e);
        }
    }

    public void filterElement() {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String elementId = params.get("elementId");
        filterKey = elementId;
        filterElement(filterKey);
    }

    private void filterElement(String elementId) {
        try {
            filterMonitor = monitorProcessoService.createSvgMonitoramentoProcesso(fluxoController.getFluxo(), elementId);
            monitorProcessoDataModel.setNodeKey(elementId);
            resetPaginator();
            filter = true;
        } catch (Exception e) {
            success = false;
            FacesMessages.instance().add("Não foi possível filtrar o fluxo pela tarefa selecionada. Favor tentar novamente");
            LOG.error("Erro ao tentar filtrar SVG do fluxo " + fluxoController.getFluxo().getCodFluxo(), e);
        }
    }

    private void resetPaginator() {
        DataTable dataTable = (DataTable) FacesContext.getCurrentInstance().getViewRoot().findComponent(JsfFunctions.clientId(DATATABLE_ID));
        dataTable.setFirst(0);
    }

    public void resetFilter() {
        filter = false;
        filterMonitor = null;
        monitorProcessoDataModel.setNodeKey(null);
        resetPaginator();
    }

    public void executeNode(MonitorProcessoInstanceDTO row) {
        try {
            automaticNodeService.executeNode(row.getToken().getId());
        } catch (Exception e) {
            FacesMessages.instance().add("Erro ao tentar executar o nó" + e.getMessage() );
        }
        if (filter) {
            filterElement(filterKey);
        } else {
            selectFluxo();
        }
    }

    public void viewGraph(MonitorProcessoInstanceDTO row) {
        executionGraphic = true;
        graphicExecutionView.setToken(row.getToken());
    }

    public void closeGraph() {
        executionGraphic = false;
    }

    public String getSvg() {
        return !filter ? monitor.getSvg() : filterMonitor.getSvg();
    }

    public String getFluxoNome() {
        return fluxoController.getFluxo().getFluxo();
    }

    public String getFluxoDescricao() {
        return monitor.getProcessDefinition().getDescription();
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFilter() {
        return filter;
    }

    public MonitorProcessoDataModel getMonitorProcessoDataModel() {
        return monitorProcessoDataModel;
    }

    public boolean isExecutionGraphic() {
        return executionGraphic;
    }
}
