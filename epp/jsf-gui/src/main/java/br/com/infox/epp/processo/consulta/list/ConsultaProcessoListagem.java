package br.com.infox.epp.processo.consulta.list;

import br.com.infox.assinador.rest.api.StatusToken;
import br.com.infox.cdi.producer.EntityManagerProducer;
import br.com.infox.componentes.column.DynamicColumnModel;
import br.com.infox.core.list.DataList;
import br.com.infox.core.messages.InfoxMessages;
import br.com.infox.core.util.DateUtil;
import br.com.infox.core.util.StringUtil;
import br.com.infox.epp.access.api.Authenticator;
import br.com.infox.epp.access.entity.UsuarioPerfil;
import br.com.infox.epp.access.manager.PapelManager;
import br.com.infox.epp.assinador.AssinadorService;
import br.com.infox.epp.assinador.DadosAssinatura;
import br.com.infox.epp.assinador.assinavel.AssinavelDocumentoBinProvider;
import br.com.infox.epp.assinador.assinavel.AssinavelProvider;
import br.com.infox.epp.assinador.view.AssinaturaCallback;
import br.com.infox.epp.cdi.ViewScoped;
import br.com.infox.epp.documento.DocumentoAssinavelDTO;
import br.com.infox.epp.documento.TaskInstanceListagemDocumentoDTO;
import br.com.infox.epp.documento.TaskInstancePermitidaAssinarDocumentoSearch;
import br.com.infox.epp.documento.manager.ClassificacaoDocumentoPapelManager;
import br.com.infox.epp.documento.type.TipoMeioAssinaturaEnum;
import br.com.infox.epp.fluxo.definicaovariavel.DefinicaoVariavelProcesso;
import br.com.infox.epp.fluxo.definicaovariavel.DefinicaoVariavelProcessoSearch;
import br.com.infox.epp.fluxo.entity.Categoria;
import br.com.infox.epp.fluxo.entity.Natureza;
import br.com.infox.epp.fluxo.manager.FluxoManager;
import br.com.infox.epp.movimentarlote.TaskInstancePermitidaMovimentarLoteSearch;
import br.com.infox.epp.painel.*;
import br.com.infox.epp.processo.documento.assinatura.AssinaturaDocumento;
import br.com.infox.epp.processo.documento.assinatura.AssinaturaException;
import br.com.infox.epp.processo.documento.entity.Documento;
import br.com.infox.epp.processo.documento.entity.DocumentoBin;
import br.com.infox.epp.processo.documento.manager.DocumentoBinManager;
import br.com.infox.epp.processo.home.ProcessoEpaHome;
import br.com.infox.epp.processo.list.FiltroVariavelProcessoSearch;
import br.com.infox.epp.processo.list.FiltroVariavelProcessoVO;
import br.com.infox.epp.processo.situacao.manager.SituacaoProcessoManager;
import br.com.infox.epp.processo.variavel.bean.VariavelProcesso;
import br.com.infox.epp.processo.variavel.service.VariavelProcessoService;
import br.com.infox.ibpm.sinal.SignalService;
import br.com.infox.ibpm.task.home.TaskInstanceHome;
import br.com.infox.ibpm.task.manager.TaskInstanceManager;
import br.com.infox.jsf.util.JsfUtil;
import br.com.infox.log.LogProvider;
import br.com.infox.log.Logging;
import br.com.infox.seam.exception.ApplicationException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.primefaces.context.RequestContext;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;

import javax.faces.component.html.HtmlSelectBooleanCheckbox;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;
import javax.validation.ValidationException;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Named
@ViewScoped
public class ConsultaProcessoListagem  implements Serializable, AssinaturaCallback {

    private static final long serialVersionUID = 1L;

    private static final LogProvider LOG = Logging.getLogProvider(ConsultaProcessoListagem.class);

    public static final String SINAL_ASSINATURA_LOTE_PAINEL_USUARIO = "assinaturaDocumentosEmLote";

    @Inject
    protected VariavelProcessoService variavelProcessoService;
    @Inject
    protected DefinicaoVariavelProcessoSearch definicaoVariavelProcessoSearch;
    @Inject
    private FluxoManager fluxoManager;
    @Inject
    private PapelManager papelManager;
    @Inject
    private TaskInstancePermitidaAssinarDocumentoSearch taskInstancePermitidaAssinarDocumentoSearch;
    @Inject
	private ClassificacaoDocumentoPapelManager classificacaoDocumentoPapelManager;
    @Inject
	private AssinadorService assinadorService;
    @Inject
    private DocumentoBinManager documentoBinManager;
    @Inject
	private SignalService signalService;

    private String numeroProcesso;
    private String numeroProcessoRoot;
    private Natureza natureza;
    private Categoria categoria;
    private Date dataInicio;
    private Date dataFim;

    @Getter @Setter
    private Long idTipoFiltroVariavelProcesso;
    @Getter @Setter
    private Object valorFiltroVariavelProcesso;
    @Getter @Setter
    private Object valorFiltroVariavelProcessoComplemento;
    @Getter @Setter
    private FiltroVariavelProcessoVO filtroVariavelSelecionado;

    private FluxoBean fluxoBean;
    private PanelDefinition panelDefinition;

    @Getter
    private TaskInstanceListagemDocumentoDTO listagemDocumentoAssinatura;
    @Getter @Setter
    private String tokenAssinatura;
    @Getter @Setter
    private boolean showMarcarTodosAssinaveis;

    @Getter @Setter
    private boolean showMarcarTodosMovimentarEmLote = false;


    @Getter
    private int qtdProcessos;

    @Inject
    private TaskInstancePermitidaMovimentarLoteSearch taskInstancePermitidaMovimentarLoteSearch;
    
    @Inject
    private FiltroVariavelProcessoSearch filtroVariavelProcessoSearch;

    @Getter @Setter
    private List<SelectItem> transitionsSelectItens;

    @Getter @Setter
    private String nome;

    @Inject
    private TaskInstanceManager taskInstanceManager;

    @Inject
    private PainelUsuarioController painelUsuarioController;

    private TarefaBean tarefaBean;



    @Getter
    private boolean podeVisualizarProcessos = false;

    @Getter @Setter
    private String numeroProcessoPesquisa;

    @Getter @Setter
    private Date dataDePesquisa;

    @Getter @Setter
    private Date dataAtePesquisa;

    private List<ColumnModel> columns;

    @Getter
    @Inject
    private TaskBeanLazyDataModel filteredTasks;

    public void onSelectNode(PanelDefinition panelDefinition) {
        this.panelDefinition = panelDefinition;
    }
    private void createDynamicColumns() {
        if (fluxoBean != null) {
            columns = new ArrayList<>();
            columns.add(new ColumnModel("", "", true));
            List<DefinicaoVariavelProcesso> definicoes = definicaoVariavelProcessoSearch.getDefinicoesVariaveis(Integer.valueOf(fluxoBean.getProcessDefinitionId()));
            for (DefinicaoVariavelProcesso definicao : definicoes) {
                columns.add(new ColumnModel(definicao.getLabel(), definicao.getNome(), false));
            }
        }
    }

    public void onSelectFluxo(FluxoBean fluxoBean) {
        this.fluxoBean = fluxoBean;
        podeVisualizarProcessos = false;
        this.columns = null;
        filteredTasks.setWrappedData(null);
        JsfUtil.instance().render("divPainelProcessosInterna");
        JsfUtil.instance().render("divPainelProcessosInterna:painelListagem");

        JsfUtil.instance().render(filteredTasks.getDataTable().getClientId());

        try {
            TarefaBean taskSelecionado = (TarefaBean) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("taskSelecionado");

            if (taskSelecionado != null && taskSelecionado.getIdFluxo() == Integer.valueOf(fluxoBean.getProcessDefinitionId())) {
                onSelectNode(taskSelecionado);
            }
        }catch (Exception e){

        }
    }


    public void onSelectNode(TarefaBean tarefaBean) {
        podeVisualizarProcessos = false;
        this.tarefaBean = tarefaBean;

        createDynamicColumns();
        filteredTasks.search(tarefaBean, numeroProcessoPesquisa, dataDePesquisa, dataAtePesquisa);
        newInstance();
    }

    public List<ColumnModel> getColumns() {
        if (columns == null) {
            createDynamicColumns();
        }
        return columns;
    }


    public void newInstance() {
    	listagemDocumentoAssinatura = new TaskInstanceListagemDocumentoDTO();


        setValorFiltroVariavelProcesso(null);
        setValorFiltroVariavelProcessoComplemento(null);
        setIdTipoFiltroVariavelProcesso(null);
        setFiltroVariavelSelecionado(null);

        setNumeroProcesso(null);
        setNumeroProcessoRoot(null);
        setNatureza(null);
        setCategoria(null);
        setDataInicio(null);
        setDataFim(null);
        clearExtraFilters();
        //setPage(1);
       // search();
        buildExibirSelecaoAssinarDocumentosLote((List<TaskBean>) filteredTasks.getWrappedData());
        buildExibirSelecaoMovimentarEmLote();
       RequestContext.getCurrentInstance().execute("enableDisableAssinarButton();");
       RequestContext.getCurrentInstance().execute("enableDisableMovimentarLote();");
        podeVisualizarProcessos = true;
        JsfUtil.instance().render("divPainelProcessosInterna");
        JsfUtil.instance().render("divPainelProcessosInterna:painelListagem");

        JsfUtil.instance().render(filteredTasks.getDataTable().getClientId());
        JsfUtil.instance().render("formToolbarProcessos");
        JsfUtil.instance().render("formToolbarProcessos:checkboxColumnMovimentarLotes");
    }
    
    public void buildExibirSelecaoAssinarDocumentosLote(List<TaskBean> listaTaskBean) {
        showMarcarTodosAssinaveis = false;

        Set<String> collect = listaTaskBean.stream().map(tb -> tb.getIdTaskInstance()).collect(Collectors.toSet());

        if(collect.isEmpty()){
            return;
        }
        List<DocumentoAssinavelDTO> result = taskInstancePermitidaAssinarDocumentoSearch.getDTODocumentosAssinar(collect);

        for(TaskBean taskBean : listaTaskBean) {
            taskBean.setExibirSelecaoAssinaturaLote(result.stream().anyMatch(doc -> taskBean.getIdTaskInstance().equals(doc.getIdTaskIntance())));
            if(!showMarcarTodosAssinaveis && taskBean.isExibirSelecaoAssinaturaLote()) {
                showMarcarTodosAssinaveis = true;
            }
        }
	}

    public void buildExibirSelecaoMovimentarEmLote() {
        showMarcarTodosMovimentarEmLote = ((List<TaskBean>) filteredTasks.getWrappedData()).stream().anyMatch(task -> task.isExibirmovimentarEmLote());
    }
    
   public void marcarAssinaveisNaoAssinaveis() {
        Set<String> listaIdTaskInstance = ((List<TaskBean>)filteredTasks.getWrappedData()).stream().filter(TaskBean::isSelecaoAssinaturaLote).map(TaskBean::getIdTaskInstance).collect(Collectors.toSet());
    	listagemDocumentoAssinatura = taskInstancePermitidaAssinarDocumentoSearch.getListaDocumentoDTOParaSeremAssinados(listaIdTaskInstance);
    }

    public void movimentarEmLote() throws IOException {
        Set<TaskBean> listaIdTaskInstance = ((List<TaskBean>)filteredTasks.getWrappedData()).stream().filter(TaskBean::isSelecaoMovimentarEmLote).collect(Collectors.toSet());

        if(!listaIdTaskInstance.isEmpty()){
            listaIdTaskInstance.forEach(tk ->{

                try {
                    TaskInstanceHome tih = TaskInstanceHome.instance();
                    ProcessoEpaHome processoEpaHome = ProcessoEpaHome.instance();
                    processoEpaHome.setProcessoIdProcesso(tk.getIdProcesso());
                    tih.setProcessoEpaHome(processoEpaHome);
                    TaskInstance taskInstanceOpen = taskInstanceManager.getTaskInstanceOpen(tk.getIdProcesso());
                    tih.setCurrentTaskInstance(taskInstanceOpen);
                    tih.setTaskId(Long.valueOf(tk.getIdTaskInstance()));

                    tih.end(this.nome, true);
                    tih.clear();

                }catch (Exception e){
                    e.printStackTrace();
                    FacesMessages.instance().add("Erro ao movimentar processo");
                }
            });
            ServletContext servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
            FacesContext.getCurrentInstance().getExternalContext().redirect(servletContext.getContextPath() + "/Painel/list.seam");
        }

    }

	public void marcarTodosAssinaveis(AjaxBehaviorEvent event) {
		boolean selecionarTodos = ((HtmlSelectBooleanCheckbox)event.getSource()).isSelected();
		for (TaskBean taskBean : filteredTasks) {
			if (taskBean.isExibirSelecaoAssinaturaLote()) {
				taskBean.setSelecaoAssinaturaLote(selecionarTodos);
			}
		}
	}

    public void marcarTodosMovimentar(AjaxBehaviorEvent event) {
        boolean selecionarTodos = ((HtmlSelectBooleanCheckbox)event.getSource()).isSelected();
        for (TaskBean taskBean : filteredTasks) {
            if (taskBean.isExibirmovimentarEmLote()) {
                taskBean.setSelecaoMovimentarEmLote(selecionarTodos);
            }
        }

        podeVisualizarTransitions();
    }

    public boolean podeVisualizarTransitions(){
        if(filteredTasks == null || ((List<TaskBean>)filteredTasks.getWrappedData()).isEmpty()){
            return false;
        }

        Optional<TaskBean> tk = ((List<TaskBean>)filteredTasks.getWrappedData()).stream().filter(t -> t.isSelecaoMovimentarEmLote() == true).findFirst();
        transitionsSelectItens = new ArrayList<>();
        if(tk.isPresent()){

            TaskInstanceHome tih = TaskInstanceHome.instance();
            tih.setTaskId(Long.valueOf(tk.get().getIdTaskInstance()));
            tih.getTransitions().forEach(t ->  transitionsSelectItens.add(new SelectItem(t.getName(), t.getName())));
        }

        JsfUtil.instance().render("formButtons");

        return tk.isPresent() ;
    }

    protected void clearExtraFilters() {}

    public void pesquisarComFiltro(){
        filteredTasks.search(tarefaBean, numeroProcessoPesquisa, dataDePesquisa, dataAtePesquisa);
        newInstance();
    }

    public void limparFiltros(){
        numeroProcessoPesquisa = null;
        dataDePesquisa = null;
        dataAtePesquisa = null;
    }

    public VariavelProcesso getVariavelProcesso(Integer idProcesso, String nome, String idTaskInstance) {

        if(idProcesso == 0 || nome.isEmpty())
            return new VariavelProcesso("", "","");

        if (StringUtil.isEmpty(idTaskInstance)) {
    		return variavelProcessoService.getVariavelProcesso(idProcesso, nome);
    	} else {
    		return variavelProcessoService.getVariavelProcesso(idProcesso, nome, Long.parseLong(idTaskInstance));
    	}
    }

    public Object getVariavelProcesso(TaskBean taskBean, String expression) {
        Token token = EntityManagerProducer.getEntityManager().find(TaskInstance.class, Long.parseLong(taskBean.getIdTaskInstance())).getToken();
        ExecutionContext executionContext = new ExecutionContext(token);
        Object object = JbpmExpressionEvaluator.evaluate("#{"+expression+"}", executionContext);
        return object;
    }
    
    public AssinavelProvider getAssinavelProvider() {
	    List<AssinavelDocumentoBinProvider.DocumentoComRegraAssinatura> lista = new ArrayList<>();
        for (Documento documento : listagemDocumentoAssinatura.getListaDocumentoAssinavel()) {
            TipoMeioAssinaturaEnum tma = classificacaoDocumentoPapelManager.getTipoMeioAssinaturaUsuarioLogadoByClassificacaoDocumento(documento.getClassificacaoDocumento());
			lista.add(new AssinavelDocumentoBinProvider.DocumentoComRegraAssinatura(tma, documento.getDocumentoBin()));
        }

		return new AssinavelDocumentoBinProvider(lista);
	}
    
    public void signDocuments() {
		try {
			List<DadosAssinatura> dadosAssinaturaList = assinadorService.getDadosAssinatura(tokenAssinatura);

			UsuarioPerfil usuarioPerfil = Authenticator.getUsuarioPerfilAtual();
			for (DadosAssinatura dadosAssinatura : dadosAssinaturaList) {
				DocumentoBin docBin =  getDocumentoTemporarioByUuid(dadosAssinatura.getUuidDocumentoBin());
				if (docBin != null) {
					if (!isAssinadoPor(docBin, usuarioPerfil)) {
						assinadorService.assinar(dadosAssinatura, usuarioPerfil);
					}
				} else {
					throw new ApplicationException("Documento não localizado!");
				}
			}
			FacesMessages.instance().add(InfoxMessages.getInstance().get("anexarDocumentos.sucessoAssinatura"));
		}catch(AssinaturaException | ValidationException e){
			LOG.error("Erro signDocuments ", e);
			FacesMessages.instance().add(Severity.ERROR,e.getMessage());
		}catch (Exception e) {
			LOG.error("Erro signDocuments ", e);
			FacesMessages.instance().add(Severity.ERROR,
					InfoxMessages.getInstance().get("anexarDocumentos.erroAssinarDocumentos"));
		}
	}
    
	@Override
	public void onSuccess(List<DadosAssinatura> dadosAssinatura) {
		try {
			if (dadosAssinatura != null) {
                UsuarioPerfil usuarioPerfilAtual = Authenticator.getUsuarioPerfilAtual();
                for (DadosAssinatura dadoAssinatura : dadosAssinatura) {
					assinadorService.assinar(dadoAssinatura, usuarioPerfilAtual);
				}
				dispararSinalAssinaturaEmLote();
				FacesMessages.instance().add(Severity.INFO,	InfoxMessages.getInstance().get("Documentos assinados com sucesso"));
			} else {
				FacesMessages.instance().add(Severity.ERROR, InfoxMessages.getInstance().get("anexarDocumentos.erroAssinarDocumentos"));
			}
		} catch (AssinaturaException e) {
			FacesMessages.instance().add(Severity.ERROR, InfoxMessages.getInstance().get("anexarDocumentos.erroAssinarDocumentos"));
		}
	}

	@Override
	public void onFail(StatusToken statusToken, List<DadosAssinatura> dadosAssinatura) {		
		FacesMessages.instance().add(Severity.ERROR, InfoxMessages.getInstance().get("anexarDocumentos.erroAssinarDocumentos"));
	}
    
    private void dispararSinalAssinaturaEmLote() {

        ((List<TaskBean>)filteredTasks.getWrappedData()).stream().filter(task -> task.isSelecaoAssinaturaLote())
              .collect(Collectors.toList())
              .forEach(able -> signalService.dispatch(able.getIdProcesso(), SINAL_ASSINATURA_LOTE_PAINEL_USUARIO));
    }
    
    private DocumentoBin getDocumentoTemporarioByUuid(UUID uuid) {
		for (Documento documento : listagemDocumentoAssinatura.getListaDocumentoAssinavel()) {
			UUID wrapperUuid = documento.getDocumentoBin().getUuid();
			if (uuid.equals(wrapperUuid))
				return documentoBinManager.getByUUID(wrapperUuid);
		}
		return null;
	}
    
    private boolean isAssinadoPor(DocumentoBin docBin, UsuarioPerfil usuarioPerfil) {
		List<AssinaturaDocumento> assinaturas = docBin.getAssinaturas();
		if (assinaturas != null) {
			for (AssinaturaDocumento assinatura : assinaturas) {
				if (usuarioPerfil.getPerfilTemplate().getPapel().equals(assinatura.getPapel())) {
					return true;
				}
			}
		}
		return false;
	}

 /*   @Override
    protected String getDefaultEjbql() {
        return "";
    }

    @Override
    protected String getDefaultOrder() {
        return "";
    }

    @Override
    protected Map<String, String> getCustomColumnsOrder() {
        return null;
    }*/

    public List<TaskBean> getTasks() {
        return panelDefinition.getTasks();
    }

/*    public List<TaskBean> getFilteredTasks() {


        return filteredTasks;
    }*/

    public FluxoBean getFluxoBean() {
        return fluxoBean;
    }

    public String getNumeroProcesso() {
        return numeroProcesso;
    }

    public void setNumeroProcesso(String numeroProcesso) {
        this.numeroProcesso = numeroProcesso;
    }

    public String getNumeroProcessoRoot() {
        return numeroProcessoRoot;
    }

    public void setNumeroProcessoRoot(String numeroProcessoRoot) {
        this.numeroProcessoRoot = numeroProcessoRoot;
    }

    public Natureza getNatureza() {
        return natureza;
    }

    public void setNatureza(Natureza natureza) {
        this.natureza = natureza;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public Date getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(Date dataInicio) {
        this.dataInicio = dataInicio;
    }

    public Date getDataFim() {
        return dataFim;
    }

    public void setDataFim(Date dataFim) {
        this.dataFim = dataFim;
    }

    public static class ColumnModel implements Serializable {

        private String header;
        private String value;

        private boolean toolBar = false;

        public ColumnModel(String header, String value, boolean toolBar) {
            this.header = header;
            this.value = value;
            this.toolBar = toolBar;
        }

        public String getHeader() {
            return header;
        }

        public String getValue() {
            return value;
        }


        public boolean isToolBar() {
            return toolBar;
        }
    }
}
