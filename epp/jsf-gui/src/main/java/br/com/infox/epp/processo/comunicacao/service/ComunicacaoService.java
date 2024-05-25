package br.com.infox.epp.processo.comunicacao.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.transaction.SystemException;

import org.jboss.seam.bpm.ManagedJbpmContext;
import org.jboss.seam.transaction.Transaction;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.exe.ProcessInstance;
import org.joda.time.DateTime;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfCopy;

import br.com.infox.core.manager.GenericManager;
import br.com.infox.core.pdf.PdfManager;
import br.com.infox.core.persistence.DAOException;
import br.com.infox.epp.access.api.Authenticator;
import br.com.infox.epp.access.manager.UsuarioLoginManager;
import br.com.infox.epp.assinador.AssinadorService;
import br.com.infox.epp.assinador.DadosAssinatura;
import br.com.infox.epp.estatistica.type.SituacaoPrazoEnum;
import br.com.infox.epp.fluxo.entity.Fluxo;
import br.com.infox.epp.fluxo.entity.NaturezaCategoriaFluxo;
import br.com.infox.epp.fluxo.manager.FluxoManager;
import br.com.infox.epp.fluxo.manager.NaturezaCategoriaFluxoManager;
import br.com.infox.epp.processo.comunicacao.ComunicacaoMetadadoProvider;
import br.com.infox.epp.processo.comunicacao.DestinatarioModeloComunicacao;
import br.com.infox.epp.processo.comunicacao.DocumentoModeloComunicacao;
import br.com.infox.epp.processo.comunicacao.ModeloComunicacao;
import br.com.infox.epp.processo.comunicacao.manager.ModeloComunicacaoManager;
import br.com.infox.epp.processo.documento.entity.Documento;
import br.com.infox.epp.processo.documento.entity.DocumentoBin;
import br.com.infox.epp.processo.documento.manager.DocumentoBinManager;
import br.com.infox.epp.processo.documento.manager.DocumentoBinarioManager;
import br.com.infox.epp.processo.entity.Processo;
import br.com.infox.epp.processo.manager.ProcessoManager;
import br.com.infox.epp.processo.metadado.entity.MetadadoProcesso;
import br.com.infox.epp.processo.metadado.manager.MetadadoProcessoManager;
import br.com.infox.epp.processo.metadado.system.MetadadoProcessoProvider;
import br.com.infox.epp.processo.metadado.type.EppMetadadoProvider;
import br.com.infox.epp.processo.service.IniciarProcessoService;
import br.com.infox.epp.processo.type.TipoProcesso;
import br.com.infox.epp.system.Parametros;
import br.com.infox.hibernate.util.HibernateUtil;
import br.com.infox.ibpm.sinal.SignalService;
import br.com.infox.ibpm.task.service.MovimentarTarefaService;

@Stateless
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class ComunicacaoService {
	
	public static final String NAME = "comunicacaoService";
	public static final String COMUNICACAO_EM_ELABORACAO = "comunicacaoEmElaboracao";
	public static final String SINAL_COMUNICACAO_EXPEDIDA = "comunicacaoExpedida";
	
	@Inject
	private NaturezaCategoriaFluxoManager naturezaCategoriaFluxoManager;
	@Inject
	private FluxoManager fluxoManager;
	@Inject
	private DocumentoBinarioManager documentoBinarioManager;
	@Inject
	private PdfManager pdfManager;
	@Inject
	private IniciarProcessoService iniciarProcessoService;
	@Inject
	private DocumentoBinManager documentoBinManager;
	@Inject
	private ProcessoManager processoManager;
	@Inject
	private GenericManager genericManager;
	@Inject
	private MetadadoProcessoManager metadadoProcessoManager;
	@Inject
	private DocumentoComunicacaoService documentoComunicacaoService;
	@Inject
	private UsuarioLoginManager usuarioLoginManager;
    @Inject
    private MovimentarTarefaService movimentarTarefaService;
	@Inject
	private ModeloComunicacaoManager modeloComunicacaoManager;
	@Inject
	private PrazoComunicacaoService prazoComunicacaoService;
	@Inject
    private SignalService signalService;
	@Inject
    private AssinadorService assinadorService;
	
	private String codigoFluxoComunicacao = Parametros.CODIGO_FLUXO_COMUNICACAO_ELETRONICA.getValue();
	private String codigoFluxoComunicacaoNaoEletronico = Parametros.CODIGO_FLUXO_COMUNICACAO_NAO_ELETRONICA.getValue();
	
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void expedirComunicacao(ModeloComunicacao modeloComunicacao) throws DAOException {
		for (DestinatarioModeloComunicacao destinatario : modeloComunicacao.getDestinatarios()) {
			expedirComunicacao(destinatario);
		}
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void expedirComunicacao(DestinatarioModeloComunicacao destinatario) throws DAOException {
		ModeloComunicacao modeloComunicacao = destinatario.getModeloComunicacao();
		
		Processo comunicacao = new Processo();
		comunicacao.setLocalizacao(Authenticator.getLocalizacaoAtual());
		comunicacao.setNaturezaCategoriaFluxo(getNaturezaCategoriaFluxo(destinatario));
		comunicacao.setNumeroProcesso("");
		comunicacao.setSituacaoPrazo(SituacaoPrazoEnum.SAT);
		comunicacao.setProcessoPai(modeloComunicacao.getProcesso());
		comunicacao.setDataInicio(DateTime.now().toDate());
		comunicacao.setUsuarioCadastro(Authenticator.getUsuarioLogado());
		processoManager.persist(comunicacao);
		destinatario.setProcesso(comunicacao);
		
		iniciarProcessoService.iniciarProcesso(comunicacao, createVariaveisJbpm(destinatario));

		criarMetadados(destinatario, comunicacao);
		
		destinatario.setExpedido(true);
		genericManager.update(destinatario);
		if (!modeloComunicacao.getDestinatarios().contains(destinatario)) {
			modeloComunicacao.getDestinatarios().add(destinatario);
		}
		atualizaModeloEmElaboracaoAoExpedir(modeloComunicacao);
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public ModeloComunicacao reabrirComunicacao (ModeloComunicacao modeloComunicacao) throws CloneNotSupportedException, DAOException {
		ModeloComunicacao  copyModeloComunicacao = modeloComunicacao.makeCopy();
		if (modeloComunicacao.getDestinatarios().isEmpty()) { //não é possível gravar uma comunicação sem destinatário
			excluirComunicacao(modeloComunicacao);
		} else {
			if (modeloComunicacaoManager.hasComunicacaoExpedida(modeloComunicacao)) {
				copyModeloComunicacao.setFinalizada(false);
				copyModeloComunicacao.setMinuta(true);
				copyModeloComunicacao =  modeloComunicacaoManager.persist(copyModeloComunicacao);
				Iterator<DestinatarioModeloComunicacao> it = modeloComunicacao.getDestinatarios().iterator();
				while (it.hasNext()){
					DestinatarioModeloComunicacao destinatario  = it.next();
					if (!destinatario.getExpedido()) {
						copyModeloComunicacao.getDestinatarios().add(destinatario);
						destinatario.setModeloComunicacao(copyModeloComunicacao);
						it.remove();
						genericManager.update(destinatario);
					}
				}
				atualizaVariavelModeloComunicacao(modeloComunicacao, copyModeloComunicacao.getId());
				modeloComunicacaoManager.update(copyModeloComunicacao);
				atualizaModeloEmElaboracaoAoReabrirComunicacao(modeloComunicacao, copyModeloComunicacao);
			} else {
				modeloComunicacao.setFinalizada(false);
				modeloComunicacao.setMinuta(true);
				copyModeloComunicacao = modeloComunicacao;
			}
			modeloComunicacaoManager.update(modeloComunicacao);
		}
		modeloComunicacaoManager.flush();
		return copyModeloComunicacao;
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	private void atualizaVariavelModeloComunicacao(ModeloComunicacao antigoModeloComunicacao, Long novoModeloComunicacaoId) {
		String nomeVariavel = modeloComunicacaoManager.getNomeVariavelModeloComunicacao(antigoModeloComunicacao.getId());
		if (nomeVariavel != null) {
			ProcessInstance processInstance = ManagedJbpmContext.instance().getProcessInstance(antigoModeloComunicacao.getProcesso().getIdJbpm());
			ContextInstance contextInstance = processInstance.getContextInstance();
			contextInstance.setVariable(nomeVariavel, novoModeloComunicacaoId);
			ManagedJbpmContext.instance().getSession().flush();
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void excluirComunicacao (ModeloComunicacao modeloComunicacao) throws DAOException {
		if (!modeloComunicacaoManager.hasComunicacaoExpedida(modeloComunicacao)) {
		    removeModeloEmElaboracao(modeloComunicacao);
			modeloComunicacaoManager.removerDestinatariosModelo(modeloComunicacao);
			modeloComunicacaoManager.removerDocumentosRelacionados(modeloComunicacao);
			modeloComunicacaoManager.remove(modeloComunicacao);
			atualizaVariavelModeloComunicacao(modeloComunicacao, null);
		}
	}
	
	public byte[] gerarPdfCompleto(ModeloComunicacao modeloComunicacao, DestinatarioModeloComunicacao destinatario) throws DAOException {
		ByteArrayOutputStream pdf = new ByteArrayOutputStream();
		try {
			ByteArrayOutputStream pdfComunicacao = gerarByteArrayComunicacao(modeloComunicacao, destinatario);
			
			com.lowagie.text.Document pdfDocument = new com.lowagie.text.Document();
			PdfCopy copy = new PdfCopy(pdfDocument, pdf);
			pdfDocument.open();
			
			copy = pdfManager.copyPdf(copy, pdfComunicacao.toByteArray());
			pdfComunicacao = null;
			Integer documentoComunicacaoId = getDocumentoComunicacao(modeloComunicacao).getId();			
			for (DocumentoModeloComunicacao documentoModelo : modeloComunicacao.getDocumentos()) {
				if (documentoModelo.getDocumento().getId() != documentoComunicacaoId){
					DocumentoBin documentoBin = documentoModelo.getDocumento().getDocumentoBin();
					if ("pdf".equals(documentoBin.getExtensao()) && documentoBinarioManager.existeBinario(documentoBin.getId())) {
						byte[] documento = documentoBinarioManager.getData(documentoBin.getId());
						copy = pdfManager.copyPdf(copy, documento);
					} else if (!documentoBin.isBinario()) {
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						pdfManager.convertHtmlToPdf(documentoBin.getModeloDocumento(), bos);
						copy = pdfManager.copyPdf(copy, bos.toByteArray());
					}
				}
			}
			pdfDocument.addTitle("Comunicação");
			pdfDocument.close();
			
			if (destinatario != null && destinatario.getExpedido()) {
				byte[] generatedPdf = pdf.toByteArray();
				pdf = new ByteArrayOutputStream();
				
				documentoBinManager.writeMargemDocumento(destinatario.getDocumentoComunicacao().getDocumentoBin(), generatedPdf, pdf);
			}
		} catch (DocumentException | IOException e) {
			rollbackAndThrow("", e);
		}
		return pdf.toByteArray();
	}
	
	public byte[] gerarPdfComunicacao(ModeloComunicacao modeloComunicacao, DestinatarioModeloComunicacao destinatario) throws DAOException {
		ByteArrayOutputStream pdf = gerarByteArrayComunicacao(modeloComunicacao, destinatario);
		if (destinatario != null && destinatario.getExpedido()) {
			byte[] generatedPdf = pdf.toByteArray();
			pdf = new ByteArrayOutputStream();
			documentoBinManager.writeMargemDocumento(destinatario.getDocumentoComunicacao().getDocumentoBin(), generatedPdf, pdf);
		}
		return pdf.toByteArray();
	}
	
	private ByteArrayOutputStream gerarByteArrayComunicacao(ModeloComunicacao modeloComunicacao, DestinatarioModeloComunicacao destinatario) throws DAOException {
		ByteArrayOutputStream pdfComunicacao = new ByteArrayOutputStream();
		try {
			String textoComunicacao = modeloComunicacao.getTextoComunicacao();
			if (textoComunicacao != null) {
				if (destinatario == null) {
					pdfManager.convertHtmlToPdf(modeloComunicacao.getTextoComunicacao(), pdfComunicacao);
				} else if (modeloComunicacao.getFinalizada()) {
					pdfManager.convertHtmlToPdf(destinatario.getDocumentoComunicacao().getDocumentoBin().getModeloDocumento(), pdfComunicacao);
				} else {
					pdfManager.convertHtmlToPdf(documentoComunicacaoService.evaluateComunicacao(destinatario), pdfComunicacao);
				}
			} else {
				DocumentoBin documentoComunicacao = getDocumentoComunicacao(modeloComunicacao).getDocumentoBin();	
				// TODO Analisar outros tipos de documento. Aqui só funciona com PDF e HTML. Mas provavelmente essa é a regra mesmo.
				byte[] doc;
				if (documentoComunicacao.isBinario()) {
					doc = documentoBinarioManager.getData(documentoComunicacao.getId());
				} else {
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					pdfManager.convertHtmlToPdf(documentoComunicacao.getModeloDocumento(), bos);
					doc = bos.toByteArray();
				}
				pdfComunicacao.write(doc);
			}
		} catch (DocumentException | IOException e) {
			rollbackAndThrow("", e);
		}
		return pdfComunicacao;
	}

	public boolean hasDocumentoComunicacaoInclusoUsuarioInterno(List<Documento> documentos){
		return documentoComunicacaoService.hasDocumentoInclusoPorUsuarioInterno(documentos);
	}
	private Documento getDocumentoComunicacao(ModeloComunicacao modeloComunicacao){
		if (!modeloComunicacao.getFinalizada()) {
			return documentoComunicacaoService.getDocumentoInclusoPorUsuarioInterno(modeloComunicacao).getDocumento();
		} else {
			return modeloComunicacao.getDestinatarios().get(0).getDocumentoComunicacao();
		}
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void finalizarComunicacao(ModeloComunicacao modeloComunicacao) throws DAOException {
		if (!modeloComunicacao.isDocumentoBinario()) {
			if (modeloComunicacao.isMinuta()) {
				rollbackAndThrow("Não é possível finalizar pois o texto no editor da comunicação é minuta", null);
			}
			if (modeloComunicacao.getClassificacaoComunicacao() == null) {
				rollbackAndThrow("Escolha a classificação de documento do editor", null);
			}
		} else {
		    DocumentoModeloComunicacao documentoInclusoPorUsuarioInterno = documentoComunicacaoService.getDocumentoInclusoPorUsuarioInterno(modeloComunicacao);
		    if (documentoInclusoPorUsuarioInterno == null) {
		        rollbackAndThrow("Deve haver texto no editor da comunicação ou pelo menos um documento incluso por usuário interno", null);
	        } else {
	            modeloComunicacao.setClassificacaoComunicacao(documentoInclusoPorUsuarioInterno.getDocumento().getClassificacaoDocumento());
	        }
		}
		
		modeloComunicacao.setFinalizada(true);
		if (modeloComunicacao.getLocalizacaoResponsavelAssinatura() == null) {
			modeloComunicacao.setLocalizacaoResponsavelAssinatura(Authenticator.getLocalizacaoAtual());
		}
		modeloComunicacaoManager.update(modeloComunicacao);
		documentoComunicacaoService.gravarDocumentos(modeloComunicacao);
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	private void criarMetadados(DestinatarioModeloComunicacao destinatario, Processo comunicacao) throws DAOException {
		MetadadoProcessoProvider metadadoProcessoProvider = new MetadadoProcessoProvider(comunicacao);
		List<MetadadoProcesso> metadados = new ArrayList<>();

		criarMetadadoDestinatario(destinatario, metadadoProcessoProvider);
		
		metadados.add(metadadoProcessoProvider.gerarMetadado(ComunicacaoMetadadoProvider.MEIO_EXPEDICAO, destinatario.getMeioExpedicao().getId().toString()));
		metadados.add(metadadoProcessoProvider.gerarMetadado(ComunicacaoMetadadoProvider.DESTINATARIO, destinatario.getId().toString()));
		metadados.add(metadadoProcessoProvider.gerarMetadado(ComunicacaoMetadadoProvider.MODELO_COMUNICACAO, 
		        destinatario.getModeloComunicacao().getId().toString()));
		
		if (destinatario.getPrazo() != null) {
			metadados.add(metadadoProcessoProvider.gerarMetadado(
					ComunicacaoMetadadoProvider.PRAZO_DESTINATARIO, destinatario.getPrazo().toString()));
			
		}
		
		if (destinatario.getMeioExpedicao().getEletronico()) {
			metadados.add(metadadoProcessoProvider.gerarMetadado(
					EppMetadadoProvider.TIPO_PROCESSO, TipoProcesso.COMUNICACAO.toString()));
		} else {
			metadados.add(metadadoProcessoProvider.gerarMetadado(
					EppMetadadoProvider.TIPO_PROCESSO, TipoProcesso.COMUNICACAO_NAO_ELETRONICA.toString()));
		}
		
		metadadoProcessoManager.persistMetadados(metadadoProcessoProvider, metadados);
		createMetadadosCiencia(destinatario, comunicacao);
		
	}
	
	protected void createMetadadosCiencia(DestinatarioModeloComunicacao destinatario, Processo comunicacao) {
		MetadadoProcessoProvider metadadoProcessoProvider = new MetadadoProcessoProvider(comunicacao);
		List<MetadadoProcesso> metadados;
		metadados = new ArrayList<>();
		if (destinatario.getModeloComunicacao().getTipoComunicacao().getQuantidadeDiasCiencia() == 0) {
			prazoComunicacaoService.darCiencia(comunicacao, DateTime.now().toDate(), usuarioLoginManager.find(Integer.valueOf(Parametros.ID_USUARIO_SISTEMA.getValue())));
		    movimentarTarefaService.finalizarTarefasEmAberto(comunicacao);
		} else {
		    Date dataLimiteCiencia = prazoComunicacaoService.contabilizarPrazoCiencia(comunicacao);
            metadados.add(metadadoProcessoProvider.gerarMetadado(
		            ComunicacaoMetadadoProvider.LIMITE_DATA_CIENCIA, new SimpleDateFormat(MetadadoProcesso.DATE_PATTERN).format(dataLimiteCiencia)));
            metadadoProcessoManager.persistMetadados(metadadoProcessoProvider, metadados);
		}
	}
	
	private Map<String, Object> createVariaveisJbpm(DestinatarioModeloComunicacao destinatario) {
		Map<String, Object> variaveis = new HashMap<>();
		variaveis.put(VariaveisJbpmComunicacao.MEIO_EXPEDICAO, destinatario.getMeioExpedicao().getDescricao());
		variaveis.put(VariaveisJbpmComunicacao.CODIGO_MEIO_EXPEDICAO, destinatario.getMeioExpedicao().getCodigo());
		variaveis.put(VariaveisJbpmComunicacao.NOME_DESTINATARIO, destinatario.getNome());
		if(destinatario.getTipoParte() != null) {
			variaveis.put(VariaveisJbpmComunicacao.TIPO_PARTICIPANTE_COMUNICACAO, HibernateUtil.removeProxy(destinatario.getTipoParte()));
		}
		if(destinatario.getDestinatario() != null && destinatario.getDestinatario().getUsuarioLogin() != null){
			variaveis.put(VariaveisJbpmComunicacao.EMAIL_DESTINATARIO, destinatario.getDestinatario().getUsuarioLogin().getEmail());
		}
		variaveis.put(VariaveisJbpmComunicacao.PRAZO_DESTINATARIO, destinatario.getPrazo());
		variaveis.put(VariaveisJbpmComunicacao.TIPO_COMUNICACAO, destinatario.getModeloComunicacao().getTipoComunicacao().getDescricao());
		variaveis.put(VariaveisJbpmComunicacao.CIENCIA_AUTOMATICA, destinatario.getModeloComunicacao().getTipoComunicacao().getQuantidadeDiasCiencia() == 0);
		variaveis.put(VariaveisJbpmComunicacao.DOCUMENTO_COMUNICACAO, destinatario.getDocumentoComunicacao().getId());
		return variaveis;
	}
	
	private NaturezaCategoriaFluxo getNaturezaCategoriaFluxo(DestinatarioModeloComunicacao destinatario) throws DAOException {
		Fluxo fluxo;
		if (destinatario.getMeioExpedicao().getEletronico()) {
			fluxo = fluxoManager.getFluxoByCodigo(codigoFluxoComunicacao);
		} else {
			fluxo = fluxoManager.getFluxoByCodigo(codigoFluxoComunicacaoNaoEletronico);
		}
		if (fluxo == null) {
			rollbackAndThrow("Fluxo de comunicação não encontrado", null);
		}
		List<NaturezaCategoriaFluxo> ncfs = naturezaCategoriaFluxoManager.getActiveNaturezaCategoriaFluxoListByFluxo(fluxo);
		if (ncfs.isEmpty()) {
			rollbackAndThrow("Não existe natureza/categoria/fluxo configurada para o fluxo de comunicação", null);
		}
		return ncfs.get(0);
	}
	
	private void criarMetadadoDestinatario(DestinatarioModeloComunicacao destinatario, MetadadoProcessoProvider metadadoProcessoProvider) throws DAOException {
		List<MetadadoProcesso> metadadosCriados = new ArrayList<>();
	    if (destinatario.getDestinatario() != null) {
			metadadosCriados.add(metadadoProcessoProvider.gerarMetadado(EppMetadadoProvider.PESSOA_DESTINATARIO, destinatario.getDestinatario().getIdPessoa().toString()));
		} else {
		    metadadosCriados.add(metadadoProcessoProvider.gerarMetadado(EppMetadadoProvider.LOCALIZACAO_DESTINO, destinatario.getDestino().getIdLocalizacao().toString()));
		    if (destinatario.getPerfilDestino() != null) {
		        metadadosCriados.add(metadadoProcessoProvider.gerarMetadado(EppMetadadoProvider.PERFIL_DESTINO, destinatario.getPerfilDestino().getId().toString()));
		    }
		}
	    
	    metadadoProcessoManager.persistMetadados(metadadoProcessoProvider, metadadosCriados);
	}

	private void rollbackAndThrow(String message, Exception cause) throws DAOException {
		try {
			Transaction.instance().setRollbackOnly();
			throw new DAOException(message, cause);
		} catch (IllegalStateException | SecurityException | SystemException e) {
			throw new DAOException(e);
		}
	}
	
    private void atualizaModeloEmElaboracaoAoExpedir(ModeloComunicacao modeloComunicacao) {
        for (DestinatarioModeloComunicacao destinatario : modeloComunicacao.getDestinatarios()) {
            if (!destinatario.getExpedido()) {
                return;
            }
        }
        removeModeloEmElaboracao(modeloComunicacao);
    }

    private ModeloComunicacao getModeloEmElaboracao(Processo processo) {
        ProcessInstance processInstance = ManagedJbpmContext.instance().getProcessInstance(processo.getIdJbpm());
        ContextInstance contextInstance = processInstance.getContextInstance();
        return (ModeloComunicacao) contextInstance.getVariable(COMUNICACAO_EM_ELABORACAO);
    }
    
    private void atualizaModeloEmElaboracaoAoReabrirComunicacao(ModeloComunicacao oldModeloComunicacao, ModeloComunicacao newModeloComunicacao) {
        if (getModeloEmElaboracao(oldModeloComunicacao.getProcesso()) != null) {
            if (removeModeloEmElaboracao(oldModeloComunicacao)) {
                ProcessInstance processInstance = ManagedJbpmContext.instance().getProcessInstance(newModeloComunicacao.getProcesso().getIdJbpm());
                ContextInstance contextInstance = processInstance.getContextInstance();
                contextInstance.setVariable(COMUNICACAO_EM_ELABORACAO, newModeloComunicacao);
                ManagedJbpmContext.instance().getSession().flush();
            }
        }
    }

    private boolean removeModeloEmElaboracao(ModeloComunicacao modeloComunicacao) {
        ModeloComunicacao modeloEmElaboracao = getModeloEmElaboracao(modeloComunicacao.getProcesso());
        if (modeloEmElaboracao != null && modeloEmElaboracao.equals(modeloComunicacao)) {
            ProcessInstance processInstance = ManagedJbpmContext.instance().getProcessInstance(modeloComunicacao.getProcesso().getIdJbpm());
            ContextInstance contextInstance = processInstance.getContextInstance();
            contextInstance.deleteVariable(COMUNICACAO_EM_ELABORACAO);
            ManagedJbpmContext.instance().getSession().flush();
            return true;
        }
        return false;
    }
    
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void assinarExpedirComunicacao(List<DadosAssinatura> dadosAssinatura, ModeloComunicacao modelo, Map<Long, List<Long>> documentosEnviadosAssinador) throws Exception  {
        if (dadosAssinatura != null && documentosEnviadosAssinador.containsKey(modelo.getId())) { // dos selecionados esse modelo foi enviado?
            List<Long> list = documentosEnviadosAssinador.get(modelo.getId());
            for (DestinatarioModeloComunicacao destinatario : modelo.getDestinatarios()) { //desse modelo, este destinatario foi enviado?
                if (list.contains(destinatario.getId())) {
                    for(DadosAssinatura dadoAssinatura : dadosAssinatura){
                        if(dadoAssinatura.getUuidDocumentoBin().equals(destinatario.getDocumentoComunicacao().getDocumentoBin().getUuid())){
                            assinadorService.assinar(dadoAssinatura, Authenticator.getUsuarioPerfilAtual());
                            expedirComunicacao(destinatario);
                        }
                    }
                }
            }
        }
        if(modelo.isModeloTotalmenteExpedido()) { //se todos foram expedidos, dispara o sinal
            signalService.dispatch(modelo.getProcesso().getIdProcesso(), ComunicacaoService.SINAL_COMUNICACAO_EXPEDIDA);
        }
    }
}
