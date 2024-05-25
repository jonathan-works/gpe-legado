package br.com.infox.epp.painel;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;

import br.com.infox.core.tree.EntityNode;
import br.com.infox.epp.processo.consulta.list.ConsultaProcessoListagem;
import br.com.infox.epp.tarefa.component.tree.TarefasTree;
import br.com.infox.jsf.util.JsfUtil;
import org.jboss.seam.faces.Redirect;
import org.richfaces.event.DropEvent;

import br.com.infox.core.action.ActionMessagesService;
import br.com.infox.core.messages.InfoxMessages;
import br.com.infox.epp.access.api.Authenticator;
import br.com.infox.epp.cdi.ViewScoped;
import br.com.infox.epp.cdi.exception.ExceptionHandled;
import br.com.infox.epp.cdi.exception.ExceptionHandled.MethodType;
import br.com.infox.epp.documento.TaskInstancePermitidaAssinarDocumentoSearch;
import br.com.infox.epp.documento.service.DocumentoVO;
import br.com.infox.epp.painel.caixa.Caixa;
import br.com.infox.epp.painel.caixa.CaixaManager;
import br.com.infox.epp.processo.consulta.list.ConsultaProcessoList;
import br.com.infox.epp.processo.situacao.manager.SituacaoProcessoManager;
import br.com.infox.epp.processo.type.TipoProcesso;
import br.com.infox.epp.system.Parametros;
import br.com.infox.epp.tarefa.component.tree.PainelEntityNode;
import br.com.infox.epp.tarefa.component.tree.PainelTreeHandler;
import br.com.infox.ibpm.task.manager.TaskInstanceManager;
import br.com.infox.seam.security.SecurityUtil;
import lombok.Getter;

@Named
@ViewScoped
public class PainelUsuarioController implements Serializable {

	public static final String NUMERO_PROCESSO_FILTERED = "numeroProcessoFiltered";

	private static final long serialVersionUID = 1L;

	@Inject
	private SituacaoProcessoManager situacaoProcessoManager;
	@Inject
	protected InfoxMessages infoxMessages;
	@Inject
	protected SecurityUtil securityUtil;
	@Inject
	protected PainelTreeHandler painelTreeHandler;
	@Inject @Getter
	private ConsultaProcessoListagem consultaProcessoList;
	@Inject
	protected CaixaManager caixaManager;
	@Inject
	protected ActionMessagesService actionMessagesService;
	@Inject
	private TaskInstanceManager taskInstanceManager;
	@Inject
	private TaskInstancePermitidaAssinarDocumentoSearch taskInstancePermitidaAssinarDocumentoSearch;

	private FluxoBean selectedFluxo;
	protected List<FluxoBean> fluxosDisponiveis;
	private List<TipoProcesso> tipoProcessoDisponiveis;
	private boolean exibirColunasPadrao = true;
	private Boolean expedida;
	private String numeroProcesso;
	private String idProcessDefinition;
	private Integer pollInterval;

	@Inject
	private JsfUtil jsfUtil;

	@Getter
    private List<DocumentoVO> listaDocumentosParaAssinar = new ArrayList<>();

	@PostConstruct
	protected void init() {
		this.pollInterval = Parametros.INTERVALO_ATUALIZACAO_PAINEL.getValueOrDefault(Integer.class, 10);
		setNumeroProcesso(getNumeroProcessoFromSession());
		loadTipoProcessoDisponiveis();
		loadFluxosDisponiveis();
	}

	private String getNumeroProcessoFromSession(){
		Map<String, Object> sessionMap = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
		return (String) sessionMap.get(NUMERO_PROCESSO_FILTERED);
	}

	public void changePerfil() throws IOException{
		limparFiltros();
		atualizarPainelProcessos();
	}

	public void atualizarPainelProcessos() throws IOException {
	    List<FluxoBean> fluxosDisponiveisTemp = situacaoProcessoManager.getFluxos(tipoProcessoDisponiveis, getNumeroProcesso());
	    verificaHouveAlteracao(fluxosDisponiveisTemp);


	}

	protected void verificaHouveAlteracao(List<? extends FluxoBean> fluxosDisponiveisTemp) throws IOException {
	    ServletContext servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
	    if (fluxosDisponiveisTemp.size() != getFluxosDisponiveis().size()) {
            FacesContext.getCurrentInstance().getExternalContext().redirect(servletContext.getContextPath() + "/Painel/list.seam");
        } else {
            fluxosDisponiveisTemp.removeAll(getFluxosDisponiveis());
            if (!fluxosDisponiveisTemp.isEmpty()) {
                FacesContext.getCurrentInstance().getExternalContext().redirect(servletContext.getContextPath() + "/Painel/list.seam");
            }
        }
	    fluxosDisponiveisTemp.clear();
	}

	private void loadFluxosDisponiveis() {
		setFluxosDisponiveis(situacaoProcessoManager.getFluxos(tipoProcessoDisponiveis, getNumeroProcesso()));
	}

	protected void loadTipoProcessoDisponiveis() {
		tipoProcessoDisponiveis = new ArrayList<>(4);
		tipoProcessoDisponiveis.add(null);
		if (hasRecursoPainelComunicacaoEletronica()) {
			tipoProcessoDisponiveis.add(TipoProcesso.COMUNICACAO);
		}
		if (hasRecursoPainelComunicacaoNaoEletronica()) {
			tipoProcessoDisponiveis.add(TipoProcesso.COMUNICACAO_NAO_ELETRONICA);
		}
		if (hasRecursoPainelDocumento()) {
			tipoProcessoDisponiveis.add(TipoProcesso.DOCUMENTO);
		}
	}

    public List<? extends FluxoBean> getFluxosDisponiveis() {
    	return fluxosDisponiveis;
    }

    @SuppressWarnings("unchecked")
    public void setFluxosDisponiveis(List<? extends FluxoBean> fluxosDisponiveis) {
        this.fluxosDisponiveis = (List<FluxoBean>) fluxosDisponiveis;
    }

	@Inject private TarefasTree tree;

	public void onSelectFluxo() {
		//painelTreeHandler.clearTree();
		//painelTreeHandler.setFluxoBean(getSelectedFluxo());
		consultaProcessoList.onSelectFluxo(getSelectedFluxo());
		//situacaoProcessoManager.loadTasks(getSelectedFluxo());
		tree.iniciar(situacaoProcessoManager.carregarTarefas(getSelectedFluxo()), getSelectedFluxo());
		FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("fluxoSelecionado", getSelectedFluxo().getProcessDefinitionId());
		FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("endTransisiton", false);

	}

    @ExceptionHandled(value = MethodType.UNSPECIFIED)
	public void atribuirTarefa(TaskBean taskBean) {
        taskInstanceManager.atribuirTarefa(Long.valueOf(taskBean.getIdTaskInstance()));
        taskBean.setAssignee(Authenticator.getUsuarioLogado().getLogin());
	}

	@ExceptionHandled(value = MethodType.UNSPECIFIED)
	public void liberarTarefa(TaskBean taskBean) {
	    taskInstanceManager.removeUsuario(Long.valueOf(taskBean.getIdTaskInstance()));
        taskBean.setAssignee(null);

    }

	@ExceptionHandled(value = MethodType.UNSPECIFIED)
	public void carregarDocumentosParaAssinar(TaskBean taskBean) {
	    this.listaDocumentosParaAssinar = taskInstancePermitidaAssinarDocumentoSearch.getListaDocumentosParaAssinar(Long.valueOf(taskBean.getIdTaskInstance()));
	}

	public void onSelectNode() {
		consultaProcessoList.onSelectNode(getSelected());
    }

	public String getTaskNodeKey() {
	    return getSelected().getId().toString();
	}

	@SuppressWarnings("unchecked")
	@ExceptionHandled(value = MethodType.UNSPECIFIED)
	public void moverProcessoParaCaixaDropEventListener(DropEvent evt) {
		Caixa caixa = caixaManager.find((Integer) evt.getDropValue());
		Object dragValue = evt.getDragValue();
		if (dragValue instanceof TaskBean) {
			moverProcessoParaCaixa((TaskBean) dragValue, caixa);
		} else if (dragValue instanceof List<?>) {
			moverProcessosParaCaixa((List<TaskBean>) dragValue, caixa);
		}
	}

	private void moverProcessoParaCaixa(TaskBean taskBean, Caixa caixa) {
        caixaManager.moverProcessoParaCaixa(taskBean.getIdProcesso(), caixa);
        getSelected().moverParaCaixa(taskBean, caixa);
        painelTreeHandler.clearTree();
    }

	private void moverProcessosParaCaixa(List<TaskBean> taskBeans, Caixa caixa) {
	    for (TaskBean taskBean : taskBeans) {
	        caixaManager.moverProcessoParaCaixa(taskBean.getIdProcesso(), caixa);
            getSelected().moverParaCaixa(taskBean, caixa);
	    }
	    painelTreeHandler.clearTree();
	}

	@ExceptionHandled(value = MethodType.REMOVE)
	public void removerCaixa(PainelEntityNode painelEntityNode) {
        Integer idCaixa = (Integer) painelEntityNode.getEntity().getId();
        caixaManager.remove(idCaixa);
        TaskDefinitionBean taskDefinitionBean = (TaskDefinitionBean) painelEntityNode.getParent().getEntity();
        taskDefinitionBean.removerCaixa(idCaixa);
        painelTreeHandler.clearTree();
    }

	@ExceptionHandled(value = MethodType.PERSIST)
	public void adicionarCaixa(ActionEvent event) {
	    String inputNomeCaixa = (String) event.getComponent().getAttributes().get("inputNomeCaixa");
	    String nomeCaixa = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(inputNomeCaixa);
        Caixa caixa = new Caixa();
        caixa.setTaskKey(getSelected().getId().toString());
        caixa.setNomeCaixa(nomeCaixa);
        caixaManager.persist(caixa);
        painelTreeHandler.clearTree();
    }

	public void editarCaixa() {
		Redirect r = new Redirect();
		r.setViewId("/Caixa/listView.xhtml");
		r.setParameter("tab", "form");
		r.setParameter("id", getSelected().getId());
		r.execute();
	}

	public PanelDefinition getSelected() {
		return painelTreeHandler.getSelected();
	}

	public void refresh() {
		painelTreeHandler.refresh();
	}

	public void adicionarFiltroNumeroProcessoRoot(){
		setSelectedFluxo(null);
		painelTreeHandler.clearTree();
		loadFluxosDisponiveis();
		FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put(NUMERO_PROCESSO_FILTERED, getNumeroProcesso());
	}

	public void limparFiltros(){
		FacesContext.getCurrentInstance().getExternalContext().getSessionMap().remove(NUMERO_PROCESSO_FILTERED);
		init();
		painelTreeHandler.clearTree();
		setSelectedFluxo(null);
	}

	public FluxoBean getSelectedFluxo() {
		return selectedFluxo;
	}

	public void setSelectedFluxo(FluxoBean selectedFluxo) {
		this.selectedFluxo = selectedFluxo;
	}

	public void setIdProcessDefinition(String idProcessDefinition) {
		this.idProcessDefinition = idProcessDefinition;
	}

    public String getIdProcessDefinition() {
        return idProcessDefinition;
    }

	public void setExpedida(Boolean expedida) {
		this.expedida = expedida;
	}

    public Boolean getExpedida() {
        return expedida;
    }

	public void selectFluxo() {
		FluxoBean fluxoBean = null;
		if (idProcessDefinition != null) {
    		for (FluxoBean fluxoBeanDisponivel : getFluxosDisponiveis()) {
                if (fluxoBeanDisponivel.getProcessDefinitionId().equals(idProcessDefinition)
                                && fluxoBeanDisponivel.getExpedida().equals(expedida)){
                    fluxoBean = fluxoBeanDisponivel;
                    break;
                }
            }
		}
		setSelectedFluxo(fluxoBean);
		onSelectFluxo();

	/*	if(FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("taskSelecionado") != null){
			String taskSelecionado = FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("taskSelecionado").toString();

			Optional<PainelEntityNode> first = painelTreeHandler.getTarefasRoots().stream().filter(task -> task.getEntity().getName().equals(taskSelecionado)).findFirst();

			if(first.isPresent()){

				painelTreeHandler.setSelected((TaskDefinitionBean) first.get().getEntity());
				onSelectNode();
				painelTreeHandler.lancarEvento();
			}

		}*/
	}

	public String nomeTarefaAberta(){
		if(FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("taskSelecionado") != null) {
			return FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("taskSelecionado").toString();
		}

		return null;
	}
	public boolean validarAtualizarList(){

		try {
			idProcessDefinition = FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("fluxoSelecionado").toString();
			boolean transitou = Boolean.valueOf(FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("endTransisiton").toString());

			return transitou && idProcessDefinition != null;
		}catch (Exception e){
			return false;
		}
	}


	public boolean canShowProcessoList() {
	    return getSelected() != null && getSelected() instanceof TaskDefinitionBean;
	}

	public boolean hasRecursoPainelComunicacaoEletronica() {
		return securityUtil.checkPage("/pages/Painel/Comunicacao/painel.seam");
	}

	public boolean hasRecursoPainelComunicacaoNaoEletronica() {
		return hasRecursoPainelComunicacaoEletronica() && hasRecursoPainelComunicacaoExpedida();
	}

	public boolean hasRecursoPainelComunicacaoRecebida() {
		return securityUtil.checkPage("/pages/Painel/comunicacoesRecebidas.seam");
	}

	public boolean hasRecursoPainelComunicacaoExpedida() {
		return securityUtil.checkPage("/pages/Painel/comunicacoesExpedidas.seam");
	}

	public boolean hasRecursoPainelDocumento() {
		return securityUtil.checkPage("/pages/Painel/fluxoDocumento.seam");
	}

	public boolean isShowPainelProcessosComum() {
		return getSelectedFluxo() != null && getSelectedFluxo().getTipoProcesso() == null && getSelectedFluxo().getProcessDefinitionId() != null
				&& !getSelectedFluxo().isBpmn20();
	}

	public boolean isShowPainelComunicacoesEletronicasComum() {
		return getSelectedFluxo() != null && TipoProcesso.COMUNICACAO.equals(getSelectedFluxo().getTipoProcesso()) && getSelectedFluxo().getProcessDefinitionId() != null
				&& !getSelectedFluxo().isBpmn20();
	}

	public boolean isShowPainelComunicacoesNaoEletronicasComum() {
		return getSelectedFluxo() != null && TipoProcesso.COMUNICACAO_NAO_ELETRONICA.equals(getSelectedFluxo().getTipoProcesso()) && getSelectedFluxo().getProcessDefinitionId() != null
				&& !getSelectedFluxo().isBpmn20();
	}

	public boolean isShowPainelDocumentosComum() {
		return getSelectedFluxo() != null && TipoProcesso.DOCUMENTO.equals(getSelectedFluxo().getTipoProcesso()) && getSelectedFluxo().getProcessDefinitionId() != null
				&& !getSelectedFluxo().isBpmn20();
	}

	public boolean isShowTarefasTree() {
		return getSelectedFluxo() != null;
	}

	public boolean isShowFiltroInfo() {
		return getNumeroProcesso() != null && !getNumeroProcesso().isEmpty();
	}

	public List<TipoProcesso> getTipoProcessoDisponiveis() {
		return tipoProcessoDisponiveis;
	}

	public String getLocaleTitleKey() {
	    return "painel.fluxos";
	}

	public boolean isExibirColunasPadrao() {
		return exibirColunasPadrao;
	}

	public void setExibirColunasPadrao(boolean exibirColunasPadrao) {
		this.exibirColunasPadrao = exibirColunasPadrao;
	}

	public String getNumeroProcesso() {
		return numeroProcesso;
	}

	public void setNumeroProcesso(String numeroProcesso) {
		this.numeroProcesso = numeroProcesso;
	}

	public Integer getPollInterval() {
		return pollInterval;
	}

}
