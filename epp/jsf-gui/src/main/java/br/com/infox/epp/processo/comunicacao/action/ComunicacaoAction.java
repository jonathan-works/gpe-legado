package br.com.infox.epp.processo.comunicacao.action;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;

import org.jboss.seam.faces.FacesMessages;

import br.com.infox.core.messages.InfoxMessages;
import br.com.infox.core.persistence.DAOException;
import br.com.infox.epp.access.api.Authenticator;
import br.com.infox.epp.access.entity.UsuarioLogin;
import br.com.infox.epp.access.entity.UsuarioPerfil;
import br.com.infox.epp.cdi.ViewScoped;
import br.com.infox.epp.processo.comunicacao.DestinatarioModeloComunicacao;
import br.com.infox.epp.processo.comunicacao.DocumentoModeloComunicacao;
import br.com.infox.epp.processo.comunicacao.ModeloComunicacao;
import br.com.infox.epp.processo.comunicacao.envio.action.EnvioComunicacaoController;
import br.com.infox.epp.processo.comunicacao.list.ModeloComunicacaoRascunhoList;
import br.com.infox.epp.processo.comunicacao.manager.ModeloComunicacaoManager;
import br.com.infox.epp.processo.comunicacao.service.ComunicacaoService;
import br.com.infox.epp.processo.documento.anexos.DocumentoDownloader;
import br.com.infox.epp.processo.documento.entity.Documento;
import br.com.infox.epp.processo.documento.service.ProcessoAnaliseDocumentoService;
import br.com.infox.epp.processo.entity.Processo;
import br.com.infox.ibpm.util.JbpmUtil;
import br.com.infox.jsf.util.JsfUtil;
import br.com.infox.log.LogProvider;
import br.com.infox.log.Logging;
import br.com.infox.seam.path.PathResolver;
import br.com.infox.seam.util.ComponentUtil;

@Named
@Stateful
@ViewScoped
public class ComunicacaoAction implements Serializable {
    
	private static final long serialVersionUID = 1L;
	public static final String NAME = "comunicacaoAction";
	private static final LogProvider LOG = Logging.getLogProvider(ComunicacaoAction.class);
	private static final String OPEN_CRIAR_COMUNICACAO = "infox.openPopUp('comunicacao%d', '%s/Processo/criarComunicacao.seam?idModeloComunicacao=%d','1024'); ";
	
	@Inject
	private ProcessoAnaliseDocumentoService processoAnaliseDocumentoService;
	@Inject
	private DocumentoDownloader documentoDownloader;
	@Inject
	protected ComunicacaoService comunicacaoService; 
	@Inject
	private ModeloComunicacaoManager modeloComunicacaoManager;
	@Inject	
	protected ModeloComunicacaoRascunhoList modeloComunicacaoRascunhoList;
	@Inject
	protected InfoxMessages infoxMessages;
	@Inject
	private EntityManager entityManager;
	@Inject
	private EnvioComunicacaoController envioComunicacaoController;
	@Inject
	private JsfUtil jsfUtil;
	
	private List<ModeloComunicacao> comunicacoes;
	private Processo processo;
	private List<Documento> documentosDestinatario; // Cache dos documentos do destinatário selecionado
	private Map<Long, Boolean> dadosCiencia = new HashMap<>(); // Cache das confirmações de ciência dos destinatários
	private List<DestinatarioBean> destinatarios;
	
	private DestinatarioBean destinatario;
	private boolean documentos;
	private boolean documentoResposta;
	private List<Documento> documentosListResposta;
	private Boolean destinatarioComCienciaEmComunicacao;
	
	protected static final Comparator<DestinatarioBean> comparatorDestinatarios = new Comparator<DestinatarioBean>() {
		@Override
		public int compare(DestinatarioBean o1, DestinatarioBean o2) {
			try {
				SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
				Date d1 = dateFormat.parse(o1.getDataEnvio());
				Date d2 = dateFormat.parse(o2.getDataEnvio());
				return d2.compareTo(d1);
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		}
	};
	
	@PostConstruct
	public void init() {
		setProcesso(JbpmUtil.getProcesso());
	}
	
	public void reabrirComunicacao(ModeloComunicacao modeloComunicacao) {
		try {
			ModeloComunicacao modeloNovo = comunicacaoService.reabrirComunicacao(modeloComunicacao);
			
			PathResolver pathResolver = ComponentUtil.getComponent(PathResolver.NAME);
			jsfUtil.execute(String.format(OPEN_CRIAR_COMUNICACAO, modeloNovo.getId(), pathResolver.getContextPath(), modeloNovo.getId()));
			envioComunicacaoController.init();
			modeloComunicacaoRascunhoList.refresh();

			FacesMessages.instance().add(InfoxMessages.getInstance().get("comunicacao.msg.sucesso.reabertura"));
		} catch (DAOException | CloneNotSupportedException e) {
			LOG.error("Erro ao rebarir comunicação", e);
			FacesMessages.instance().add(InfoxMessages.getInstance().get("comunicacao.msg.erro.reabertura"));
		}
	}
	
	public void excluirComunicacao(ModeloComunicacao modeloComunicacao) {
		try {
			comunicacaoService.excluirComunicacao(modeloComunicacao);
			envioComunicacaoController.init(); //para recarregar a página de tarefa
			modeloComunicacaoRascunhoList.refresh();
			FacesMessages.instance().add(InfoxMessages.getInstance().get("comunicacao.msg.sucesso.exclusao"));
		} catch (DAOException e) {
			LOG.error("Erro ao excluir comunicação", e);
			FacesMessages.instance().add(InfoxMessages.getInstance().get("comunicacao.msg.erro.exclusao"));
		}
	}
	
	public boolean podeReabrirComunicacao(ModeloComunicacao modeloComunicacao) {
		return modeloComunicacao.getFinalizada() && !modeloComunicacaoManager.isExpedida(modeloComunicacao);
	}
	
	public boolean podeExcluirModeloComunicacao(ModeloComunicacao modeloComunicacao) {
		return !modeloComunicacao.getFinalizada() || (modeloComunicacao.getFinalizada() && !modeloComunicacaoManager.hasComunicacaoExpedida(modeloComunicacao));
	}
	
	public void setProcesso(Processo processo) {
	    this.processo = processo;
	    modeloComunicacaoRascunhoList.setProcesso(processo);
	    clear();
	}
	
	public Processo getProcesso() {
        return processo;
    }

    public List<ModeloComunicacao> getComunicacoesDoProcesso() {
		if (comunicacoes == null) {
			comunicacoes = modeloComunicacaoManager.listModelosComunicacaoPorProcessoRoot(getProcesso().getNumeroProcessoRoot());
		}
		return comunicacoes;
	}
	
	public void clearCacheModelos() {
		modeloComunicacaoRascunhoList.refresh();
		this.comunicacoes = null;
		this.destinatario = null;
		this.destinatarios = null;
	}
	
	public List<DestinatarioBean> getDestinatarios() {
		if(destinatarios == null){
		    destinatarios = initDestinatarios();
		    Collections.sort(destinatarios, comparatorDestinatarios );
		}
	    return destinatarios;
	}

	protected List<DestinatarioBean> initDestinatarios() {
		List<DestinatarioBean> destinatarios = modeloComunicacaoManager.listDestinatarios(getProcesso().getNumeroProcessoRoot());
		for (DestinatarioBean destinatario : destinatarios) {
			dadosCiencia.put(destinatario.getIdDestinatario(), !destinatario.getDataConfirmacao().equals("-"));
		}
		return destinatarios;
	}

	public DestinatarioBean getDestinatario() {
		return destinatario;
	}
	
	protected void setDestinatario(DestinatarioBean destinatarioBean) {
		this.destinatario = destinatarioBean;
	}
		
	public boolean isDocumentos() {
		return documentos;
	}
	
	public void setDestinatarioDocumentos(DestinatarioBean destinatario) {
		clear();
		this.destinatario = destinatario;
		documentos = true;
	}
	
	public Long getJbpmProcessId() {
		return getProcesso().getIdJbpm();
	}
	
	public List<Documento> getDocumentosDestinatario() {
		if (documentosDestinatario == null) {
			DestinatarioModeloComunicacao destinatarioModelo = getDestinatarioModeloComunicacao(destinatario);
			documentosDestinatario = new ArrayList<>();
			for (DocumentoModeloComunicacao documentoModelo : destinatarioModelo.getModeloComunicacao().getDocumentos()) {
				documentosDestinatario.add(documentoModelo.getDocumento());
				// FIXME: Só para não dar lazy na tela
				documentoModelo.getDocumento().getDocumentoBin().getSize();
			}
		}
		return documentosDestinatario;
	}

	protected DestinatarioModeloComunicacao getDestinatarioModeloComunicacao(DestinatarioBean bean) {
		return entityManager.find(DestinatarioModeloComunicacao.class, bean.getIdDestinatario());
	}
	
	public void downloadDocumento(Documento documento) {
		documentoDownloader.downloadDocumento(documento);
	}
	
	public void downloadComunicacao() {
		documentoDownloader.downloadDocumento(getDestinatarioModeloComunicacao(destinatario).getDocumentoComunicacao());
	}
	
	public boolean isCienciaConfirmada(DestinatarioBean bean) {
		return dadosCiencia.get(bean.getIdDestinatario());
	}
	
    public boolean isDestinatario(DestinatarioBean bean) {
        DestinatarioModeloComunicacao destinatario = getDestinatarioModeloComunicacao(bean);
        UsuarioLogin usuario = Authenticator.getUsuarioLogado();
        UsuarioPerfil perfil = Authenticator.getUsuarioPerfilAtual();
        if ((destinatario.getDestinatario() != null && usuario.getPessoaFisica() != null && usuario.getPessoaFisica().equals(destinatario.getDestinatario()))
                || (destinatario.getDestino() != null && destinatario.getDestino().equals(perfil.getLocalizacao())
                        && (destinatario.getPerfilDestino() == null || (destinatario.getPerfilDestino().equals(perfil.getPerfilTemplate()))))) {
            return true;
        }
        return false;
    }
	
	public void clear() {
		clearCacheModelos();
		documentos = false;
		documentosDestinatario = null;
		destinatario = null;
		documentoResposta = false;
		documentosListResposta = null;
	}
	
	public List<Documento> getDocumentosRespostaList(){
		if (documentosListResposta == null) {
			if(destinatario != null){
				documentosListResposta = processoAnaliseDocumentoService.getDocumentosRespostaComunicacao(getDestinatarioModeloComunicacao(destinatario).getProcesso());
			}
		}
		return documentosListResposta;
	}
	
	public void setDestinatarioResposta(DestinatarioBean destinatario){
		clear();
		this.destinatario = destinatario;
		this.documentoResposta = true;
		this.documentosListResposta = null;
	}

	public boolean isDocumentoResposta() {
		return documentoResposta;
	}

	public void setDocumentoResposta(boolean documentoResposta) {
		this.documentoResposta = documentoResposta;
	}
	
	public String getComunicacoesExpedidasTitle(){
		return infoxMessages.get("comunicacao.comunicacoes");
	}
	
	public Documento getComunicacaoDestinatario() {
		if (destinatario != null) {
			DestinatarioModeloComunicacao destinatarioModeloComunicacao = getDestinatarioModeloComunicacao(destinatario);
			return destinatarioModeloComunicacao.getDocumentoComunicacao();
		}
		return null;
	}
	
	public Map<Long, Boolean> getDadosCiencia() {
		return dadosCiencia;
	}

    public boolean isDestinatarioComCienciaEmComunicacao() {
        if (destinatarioComCienciaEmComunicacao == null) {
            List<DestinatarioBean> destinatarios = getDestinatarios();
            destinatarioComCienciaEmComunicacao = false;
            for (DestinatarioBean destinatario : destinatarios) {
                if (isCienciaConfirmada(destinatario) && isDestinatario(destinatario)) {
                    destinatarioComCienciaEmComunicacao = true;
                }
            }
        }
        return destinatarioComCienciaEmComunicacao;
    }

}
