package br.com.infox.epp.tarefaexterna.view;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import com.lowagie.text.DocumentException;

import br.com.infox.core.exception.EppConfigurationException;
import br.com.infox.core.messages.InfoxMessages;
import br.com.infox.core.pdf.PdfManager;
import br.com.infox.core.util.StringUtil;
import br.com.infox.epp.cdi.ViewScoped;
import br.com.infox.epp.cdi.exception.ExceptionHandled;
import br.com.infox.epp.documento.entity.ModeloDocumento;
import br.com.infox.epp.documento.manager.ModeloDocumentoManager;
import br.com.infox.epp.documento.modelo.ModeloDocumentoSearch;
import br.com.infox.epp.documento.type.ExpressionResolverChain;
import br.com.infox.epp.documento.type.ExpressionResolverChain.ExpressionResolverChainBuilder;
import br.com.infox.epp.processo.entity.ProcessoJbpm;
import br.com.infox.epp.system.Parametros;
import br.com.infox.jsf.util.JsfUtil;
import br.com.infox.seam.exception.BusinessRollbackException;
import lombok.Getter;

@Named
@ViewScoped
public class DadosTarefaExternaView implements Serializable {
    private static final long serialVersionUID = 1L;

    @Getter
    private DadosTarefaExternaVO vo;
    @Inject
    private ModeloDocumentoSearch modeloDocumentoSearch;
    @Inject
    private ModeloDocumentoManager modeloDocumentoManager;
    @Getter
    private boolean canOpen;
    @Inject
    private PdfManager pdfManager;
    @Inject
    private TarefaExternaSearch tarefaExternaSearch;

    @PostConstruct
    private void init() {
        String uuidTarefaExterna = JsfUtil.instance().getFlashParam(CadastroTarefaExternaView.PARAM_UUID_TAREFA_EXTERNA, String.class);
        if(StringUtil.isEmpty(uuidTarefaExterna)) {
            throw new BusinessRollbackException("Parâmetro não encontrado");
        }

        ProcessoJbpm pj = tarefaExternaSearch.getProcessoJbpmByUUID(UUID.fromString(uuidTarefaExterna));
        if(pj == null){
            throw new EppConfigurationException(
                InfoxMessages.getInstance().get("configuracao.erroGenerico")
            );
        };

        CadastroTarefaExternaVO ctVO = (CadastroTarefaExternaVO) pj.getProcessInstance().getContextInstance().getVariable(CadastroTarefaExternaView.PARAM_TAREFA_EXTERNA);
        this.vo = new DadosTarefaExternaVO();
        this.vo.setDataInico(ctVO.getDataAbertura());
        this.vo.setTituloManifestacao(ctVO.getTituloManifesto());
        this.vo.setNumeroManifestacao(pj.getProcesso().getNumeroProcesso());
        this.vo.setTipoManifestacao(ctVO.getTipoManifesto());

        String modDocChave = Parametros.MODELO_DOC_CHAVE_CONSULTA.getValue();
        if(StringUtil.isEmpty(modDocChave)) {
            throw new EppConfigurationException(String.format("Parâmetro não configurado: ", Parametros.MODELO_DOC_CHAVE_CONSULTA.getLabel()));
        }
        ModeloDocumento modeloDocumento = modeloDocumentoSearch.getModeloDocumentoByCodigo(modDocChave);
        ExpressionResolverChain chain = ExpressionResolverChainBuilder
            .with(ExpressionResolverChainBuilder.defaultExpressionResolverChain(pj.getProcesso().getIdProcesso(), pj.getProcessInstance()))
            .build();
        getVo().setConteudo(modeloDocumentoManager.evaluateModeloDocumento(modeloDocumento, chain));


        String modDocPDF = Parametros.MODELO_DOC_DOWNLOAD_PDF.getValue();
        if(StringUtil.isEmpty(modDocPDF)) {
            throw new EppConfigurationException(String.format("Parâmetro não configurado: ", Parametros.MODELO_DOC_CHAVE_CONSULTA.getLabel()));
        }
        ModeloDocumento modeloDocumentoPDF = modeloDocumentoSearch.getModeloDocumentoByCodigo(modDocPDF);
        getVo().setConteudoPDF(modeloDocumentoManager.evaluateModeloDocumento(modeloDocumentoPDF, chain));

        this.canOpen = true;
    }

    @ExceptionHandled
    public void imprimir() {
        try (ByteArrayOutputStream pdf = new ByteArrayOutputStream()){
            pdfManager.convertHtmlToPdf(getVo().getConteudoPDF(), pdf);
            JsfUtil jsfUtil = JsfUtil.instance();
            jsfUtil.addFlashParam("documentoPreview", pdf.toByteArray());
            jsfUtil.addFlashParam("fileName", String.format("chaveConsultaTarefaExterna_%s.pdf", getVo().getNumeroManifestacao()));
            jsfUtil.applyLastPhaseFlashAction();
        } catch (IOException | DocumentException e) {
            throw new BusinessRollbackException("Falha ao gerar o documento", e);
        }
    }


}