package br.com.infox.epp.processo.documento.manager;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.jboss.seam.bpm.TaskInstance;

import br.com.infox.core.persistence.DAOException;
import br.com.infox.core.persistence.PersistenceController;
import br.com.infox.epp.access.api.Authenticator;
import br.com.infox.epp.access.entity.Localizacao;
import br.com.infox.epp.access.entity.Papel;
import br.com.infox.epp.access.entity.UsuarioPerfil;
import br.com.infox.epp.access.manager.PermissaoService;
import br.com.infox.epp.documento.entity.ClassificacaoDocumento;
import br.com.infox.epp.documento.manager.ClassificacaoDocumentoManager;
import br.com.infox.epp.processo.documento.dao.DocumentoTemporarioDao;
import br.com.infox.epp.processo.documento.entity.Documento;
import br.com.infox.epp.processo.documento.entity.DocumentoBin;
import br.com.infox.epp.processo.documento.entity.DocumentoTemporario;
import br.com.infox.epp.processo.documento.service.ProcessoAnaliseDocumentoService;
import br.com.infox.epp.processo.entity.Processo;

@Stateless
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class DocumentoTemporarioManager extends PersistenceController {
    
    private static final String RECURSO_ANEXAR_DOCUMENTO_SEM_ANALISE = "anexarDocumentoSemAnalise";
    
    @Inject
    private DocumentoBinManager documentoBinManager;
    @Inject
    private PastaManager pastaManager;
    @Inject
    private PermissaoService permissaoService;
    @Inject
    private DocumentoManager documentoManager;
    @Inject
    private ProcessoAnaliseDocumentoService processoAnaliseDocumentoService;
    @Inject
    private ClassificacaoDocumentoManager classificacaoDocumentoManager;
    @Inject
    private DocumentoTemporarioDao documentoTemporarioDao;
    
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void gravarDocumentoTemporario(DocumentoTemporario documentoTemporario, InputStream inputStream) throws DAOException, IOException {
    	DocumentoBin processoDocumentoBin = documentoBinManager.createProcessoDocumentoBin(documentoTemporario.getDocumentoBin(), inputStream);
        documentoTemporario.setDocumentoBin(processoDocumentoBin);
    	if (TaskInstance.instance() != null) {
    		documentoTemporario.setIdJbpmTask(TaskInstance.instance().getId());
    	}
    	if (documentoTemporario.getPasta() == null) {
    		documentoTemporario.setPasta(pastaManager.getDefault(documentoTemporario.getProcesso()));
    	}
    	documentoTemporarioDao.persist(documentoTemporario);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void gravarDocumentoTemporario(DocumentoTemporario documentoTemporario) throws DAOException {
    	documentoTemporario.setDocumentoBin(documentoBinManager.createProcessoDocumentoBin(documentoTemporario.getDocumentoBin()));
    	if (TaskInstance.instance() != null) {
    		documentoTemporario.setIdJbpmTask(TaskInstance.instance().getId());
    	}
    	if (documentoTemporario.getPasta() == null) {
    		documentoTemporario.setPasta(pastaManager.getDefault(documentoTemporario.getProcesso()));
    	}
    	documentoTemporarioDao.persist(documentoTemporario);
    }
    
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public DocumentoTemporario update(DocumentoTemporario documentoTemporario) throws DAOException {
    	documentoTemporario.setUsuarioAlteracao(Authenticator.getUsuarioLogado());
    	documentoTemporario.setDataAlteracao(new Date());
    	documentoBinManager.update(documentoTemporario.getDocumentoBin());
    	return documentoTemporarioDao.update(documentoTemporario);
    }

    public List<DocumentoTemporario> listByProcesso(Processo processo, Localizacao localizacao, String order) {
        return documentoTemporarioDao.listByProcesso(processo, localizacao, order);
    }

    public List<DocumentoTemporario> listByProcesso(Processo processo, UsuarioPerfil usuarioPerfil, String order) {
        return documentoTemporarioDao.listByProcesso(processo, usuarioPerfil, order);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void removeAllSomenteTemporario(List<DocumentoTemporario> documentoTemporarioList) throws DAOException {
		documentoTemporarioDao.removeAllSomenteTemporario(documentoTemporarioList);
	}
    
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void removeAll(List<DocumentoTemporario> documentoTemporarioList) throws DAOException {
        documentoTemporarioDao.removeAll(documentoTemporarioList);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void transformarEmDocumento(List<DocumentoTemporario> documentosParaEnviar) throws DAOException {
        List<Documento> documentosParaAnalise = new ArrayList<>();
        
        for (DocumentoTemporario documentoTemporario : documentosParaEnviar) {
            Documento documentoCriado = createDocumento(documentoTemporario);
            documentoManager.persist(documentoCriado);
            Papel papelInclusao = documentoTemporario.getPerfilTemplate().getPapel();
            if (!permissaoService.papelPossuiPermissaoParaRecurso(papelInclusao, RECURSO_ANEXAR_DOCUMENTO_SEM_ANALISE)) {
                documentosParaAnalise.add(documentoCriado);
            }
        }
        
        if (!documentosParaAnalise.isEmpty()) {
            Processo processo = documentosParaAnalise.get(0).getPasta().getProcesso();
            Documento[] arrayDocumentos = documentosParaAnalise.toArray(new Documento[documentosParaAnalise.size()]);
            Processo processoAnaliseDoc = processoAnaliseDocumentoService.criarProcessoAnaliseDocumentos(processo, arrayDocumentos);
            processoAnaliseDocumentoService.inicializarFluxoDocumento(processoAnaliseDoc, null);
        }
    }
    
    private Documento createDocumento(DocumentoTemporario dt) throws DAOException {
        ClassificacaoDocumento classificacaoDocumento = classificacaoDocumentoManager.find(dt.getClassificacaoDocumento().getId());
        DocumentoBin docBin = documentoBinManager.find(dt.getDocumentoBin().getId());
        Documento documento = new Documento();
        documento.setClassificacaoDocumento(classificacaoDocumento);
        documento.setDocumentoBin(docBin);
        documento.setDescricao(dt.getDescricao());
        documento.setPasta(dt.getPasta());
        documento.setNumeroSequencialDocumento(documentoManager.getNextNumeracao(documento));
        documento.setNumeroDocumento(dt.getNumeroDocumento());
        documento.setAnexo(dt.getAnexo());
        documento.setIdJbpmTask(dt.getIdJbpmTask());
        documento.setPerfilTemplate(dt.getPerfilTemplate());
        documento.setDataInclusao(new Date());
        documento.setUsuarioInclusao(dt.getUsuarioInclusao());
        documento.setDataAlteracao(dt.getDataAlteracao());
        documento.setUsuarioAlteracao(dt.getUsuarioAlteracao());
        documento.setExcluido(Boolean.FALSE);
        documento.setLocalizacao(dt.getLocalizacao());
        return documento;
    }
    
    public DocumentoTemporario loadById(Integer id) {
        return documentoTemporarioDao.loadById(id);
    }
}