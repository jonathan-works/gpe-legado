package br.com.infox.epp.processo.consulta.action;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Remove;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.inject.Inject;

import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.faces.Redirect;

import br.com.infox.core.controller.AbstractController;
import br.com.infox.epp.access.api.Authenticator;
import br.com.infox.epp.access.entity.Localizacao;
import br.com.infox.epp.access.manager.PapelManager;
import br.com.infox.epp.cdi.seam.ContextDependency;
import br.com.infox.epp.fluxo.definicaovariavel.DefinicaoVariavelProcessoRecursos;
import br.com.infox.epp.processo.documento.action.DocumentoProcessoAction;
import br.com.infox.epp.processo.documento.action.PastaAction;
import br.com.infox.epp.processo.documento.entity.Documento;
import br.com.infox.epp.processo.documento.list.DocumentoList;
import br.com.infox.epp.processo.documento.list.PastaList;
import br.com.infox.epp.processo.documento.manager.PastaRestricaoAction;
import br.com.infox.epp.processo.documento.sigilo.manager.SigiloDocumentoPermissaoManager;
import br.com.infox.epp.processo.entity.Processo;
import br.com.infox.epp.processo.list.FiltrosBeanList;
import br.com.infox.epp.processo.manager.ProcessoManager;
import br.com.infox.epp.processo.metadado.manager.MetadadoProcessoManager;
import br.com.infox.epp.processo.sigilo.service.SigiloProcessoService;
import br.com.infox.epp.processo.variavel.bean.VariavelProcesso;
import br.com.infox.epp.processo.variavel.service.VariavelProcessoService;
import br.com.infox.ibpm.task.manager.UsuarioTaskInstanceManager;

@AutoCreate
@Name(ConsultaController.NAME)
@ContextDependency
public class ConsultaController extends AbstractController {

    private static final long serialVersionUID = 1L;
    public static final String NAME = "consultaController";
    
    @In
    private ProcessoManager processoManager;
    @In
    private SigiloDocumentoPermissaoManager sigiloDocumentoPermissaoManager;
    @In
    private SigiloProcessoService sigiloProcessoService;
    @In
    private MetadadoProcessoManager metadadoProcessoManager;
    @In
    private DocumentoList documentoList;
    @Inject
    private PastaRestricaoAction pastaRestricaoAction;
    @Inject
    private PastaList pastaList;
    @In
    private UsuarioTaskInstanceManager usuarioTaskInstanceManager;
    @Inject
    private DocumentoProcessoAction documentoProcessoAction;
    @Inject
    private PastaAction pastaAction;
    @Inject
    private VariavelProcessoService variavelProcessoService;
    @Inject
    private PapelManager papelManager;
    
    private FiltrosBeanList filtros;
    
    private Processo processo;
    private boolean showAllDocuments = false;
    private boolean showBackButton = true;
    private List<Localizacao> localizacoesProcesso;
    private List<VariavelProcesso> variaveisDetalhe;
    private String url;

	public boolean isShowBackButton() {
		return showBackButton;
	}

	public void setShowBackButton(boolean showBackButton) {
		this.showBackButton = showBackButton;
	}

	public boolean isShowAllDocuments() {
        return showAllDocuments;
    }

    public void setShowAllDocuments(boolean showAllDocuments) {
        this.showAllDocuments = showAllDocuments;
    }

    @Override
    public void setId(Object id) {
    	if (id instanceof String) {
    		id = Integer.valueOf((String) id);
    	}
    	Processo processo = processoManager.find(id);
    	if (processo == null || processo.getProcessoPai() == null) {
    		this.setProcesso(processo);
    		super.setId(id);
    	} else {
    		this.setProcesso(null);
    		super.setId(null);
    	}
    }
    
    public Processo getProcesso() {
		return processo;
	}

	public void setProcesso(Processo processo) {
		this.processo = processo;
	}

    public List<Documento> getProcessoDocumentoList(Long idTask) {
        List<Documento> list = sigiloDocumentoPermissaoManager.getDocumentosPermitidos(getProcesso(), Authenticator.getUsuarioLogado());
        list = filtrarPorTarefa(list, idTask);
        return filtrarAnexos(list);
    }

    private List<Documento> filtrarPorTarefa(List<Documento> list, Long taskId) {
        if (!showAllDocuments && taskId != null) {
            List<Documento> ret = new ArrayList<Documento>();
            for (Documento documento : list) {
                if (taskId.equals(documento.getIdJbpmTask())) {
                    ret.add(documento);
                }
            }
            return ret;
        }
        return list;
    }

    private List<Documento> filtrarAnexos(List<Documento> list) {
        List<Documento> ret = new ArrayList<Documento>();
        for (Documento documento : list) {
            if (documento.getAnexo() != null && documento.getAnexo()) {
                ret.add(documento);
            }
        }
        return ret;
    }
    
    public void checarVisibilidade() {
        if (!sigiloProcessoService.usuarioPossuiPermissao(Authenticator.getUsuarioLogado(), getProcesso())) {
            FacesMessages.instance().add("Usuário sem permissão");
            Redirect.instance().setViewId("/error.seam");
            Redirect.instance().setConversationPropagationEnabled(false);
            Redirect.instance().execute();
        }
    }
    
    public List<Localizacao> getLocalizacoes() {
        if (localizacoesProcesso == null) {
            localizacoesProcesso = usuarioTaskInstanceManager.getLocalizacoes(getProcesso());
        }
        return localizacoesProcesso;
    }
    
    public void setLocalizacoes(List<Localizacao> localizacoesProcesso) {
		this.localizacoesProcesso = localizacoesProcesso;
	}

	public Boolean getShowBackButton() {
		return showBackButton;
	}

	public void setShowBackButton(Boolean showBackButton) {
		this.showBackButton = showBackButton;
	}

	@Override
    public void setTab(String tab) {
        if (tab != null && tab.equals(getTab())) return;
	    super.setTab(tab);
        if(tab.equals("tabAnexos") || tab.equals("tabAnexar")){
        	pastaAction.setProcesso(this.getProcesso());
        }
        if(tab.equals("tabAnexos")){
        	documentoList.setProcesso(this.getProcesso());
        	documentoProcessoAction.setProcesso(getProcesso());
        	documentoProcessoAction.setListClassificacaoDocumento(null);
        }
        if(tab.equals("tabPastaRestricao")) {
            pastaRestricaoAction.setProcesso(getProcesso());
            pastaList.setProcesso(getProcesso().getProcessoRoot());
        }
    }
    
	@Remove
	public void remove(){}
    
	public List<VariavelProcesso> getVariaveisDetalhe() {
		if (variaveisDetalhe == null) {
			variaveisDetalhe = variavelProcessoService.getVariaveis(getProcesso(), 
				DefinicaoVariavelProcessoRecursos.DETALHE_PROCESSO.getIdentificador(), papelManager.isUsuarioExterno(Authenticator.getPapelAtual().getIdentificador()));
		}
		return variaveisDetalhe;
	}
	
	public String getUrl() {
		return url;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
	
	public void redirectToView() {
		try {
			URL url = new URL(FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("url"));
			String path = url.getPath();
			String contextPath = FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();
			String redirectUrl = path.substring(path.indexOf(contextPath) + contextPath.length()).replace(".seam", ".xhtml");
			if (redirectUrl.equals("/Processo/Consulta/listView.xhtml")) {
				Flash flash = FacesContext.getCurrentInstance().getExternalContext().getFlash();
				flash.put("idFluxo", getProcesso().getNaturezaCategoriaFluxo().getFluxo().getIdFluxo());
				flash.put("recurso", DefinicaoVariavelProcessoRecursos.CONSULTA_PROCESSOS);
				
				flash.put("filtros", getFiltros());
				
				Redirect.instance().setConversationPropagationEnabled(false);
				Redirect.instance().getParameters().clear();
			} else {
				Redirect.instance().setConversationPropagationEnabled(true);
			}
	        Redirect.instance().setViewId(redirectUrl);
	        Redirect.instance().execute();
		} catch (MalformedURLException e) {
			FacesMessages.instance().add("URL de retorno mal-formada: " + FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("url"));
		}
	}

	public FiltrosBeanList getFiltros() {
		return filtros;
	}

	public void setFiltros(FiltrosBeanList filtros) {
		this.filtros = filtros;
	}
}
