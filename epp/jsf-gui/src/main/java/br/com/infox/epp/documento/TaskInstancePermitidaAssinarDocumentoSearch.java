package br.com.infox.epp.documento;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

import br.com.infox.cdi.producer.EntityManagerProducer;
import br.com.infox.core.persistence.PersistenceController;
import br.com.infox.epp.access.api.Authenticator;
import br.com.infox.epp.access.entity.UsuarioLogin;
import br.com.infox.epp.access.entity.UsuarioLogin_;
import br.com.infox.epp.documento.entity.ClassificacaoDocumento;
import br.com.infox.epp.documento.entity.ClassificacaoDocumento_;
import br.com.infox.epp.documento.entity.TaskInstancePermitidaAssinarDocumento;
import br.com.infox.epp.documento.entity.TaskInstancePermitidaAssinarDocumento_;
import br.com.infox.epp.documento.query.DocumentosParaSeremAssinadosQuery;
import br.com.infox.epp.documento.service.DocumentoVO;
import br.com.infox.epp.processo.documento.assinatura.AssinaturaDocumentoService;
import br.com.infox.epp.processo.documento.dao.DocumentoDAO;
import br.com.infox.epp.processo.documento.entity.Documento;
import br.com.infox.epp.processo.documento.entity.DocumentoBin;
import br.com.infox.epp.processo.documento.entity.DocumentoBin_;
import br.com.infox.epp.processo.documento.entity.Documento_;
import br.com.infox.epp.processo.documento.manager.DocumentoManager;

@Stateless
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class TaskInstancePermitidaAssinarDocumentoSearch extends PersistenceController {

	@Inject
	private AssinaturaDocumentoService assinaturaDocumentoService;

    public List<DocumentoVO> getListaDocumentosParaAssinar(Long idTaskInstance){
        EntityManager em = EntityManagerProducer.getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<DocumentoVO> query = cb.createQuery(DocumentoVO.class);
        Root<TaskInstancePermitidaAssinarDocumento> taskInstancePermitidaAssinarDocumento = query.from(TaskInstancePermitidaAssinarDocumento.class);
        Join<TaskInstancePermitidaAssinarDocumento, Documento> documento = taskInstancePermitidaAssinarDocumento.join(TaskInstancePermitidaAssinarDocumento_.documento);
        Join<Documento, DocumentoBin> documentoBin = documento.join(Documento_.documentoBin);
        Join<Documento, UsuarioLogin> usuarioInclusao = documento.join(Documento_.usuarioInclusao);
        Join<Documento, ClassificacaoDocumento> classificacaoDocumento = documento.join(Documento_.classificacaoDocumento);

        query.where(cb.equal(taskInstancePermitidaAssinarDocumento.get(TaskInstancePermitidaAssinarDocumento_.idTaskInstance), idTaskInstance));

        query.select(cb.construct(DocumentoVO.class, documento.get(Documento_.id)
            , documentoBin.get(DocumentoBin_.id)
            , documento.get(Documento_.numeroSequencialDocumento)
            , classificacaoDocumento.get(ClassificacaoDocumento_.descricao)
            , documento.get(Documento_.descricao)
            , usuarioInclusao.get(UsuarioLogin_.nomeUsuario)
            , documento.get(Documento_.dataInclusao)
        ));

        return em.createQuery(query).getResultList();
    }

	public TaskInstanceListagemDocumentoDTO getListaDocumentoDTOParaSeremAssinados(Set<String> listaIdTaskInstance) {
		TaskInstanceListagemDocumentoDTO listagemDocumentoDTO = new TaskInstanceListagemDocumentoDTO();

		for(String idTaskInstance : listaIdTaskInstance) {
			for(Documento documento : getListaDocumentosParaSeremAssinados(idTaskInstance)) {
				if (documento.getDocumentoBin().getMinuta()) {
					listagemDocumentoDTO.getListaDocumentoMinuta().add(documento);
				} else if (assinaturaDocumentoService.podeRenderizarApplet(Authenticator.getPapelAtual(),
						documento.getClassificacaoDocumento(),
						documento.getDocumentoBin(), Authenticator.getUsuarioLogado())) {
					listagemDocumentoDTO.getListaDocumentoAssinavel().add(documento);
				} else {
					listagemDocumentoDTO.getListaDocumentoNaoAssinavel().add(documento);
				}
			}
		}
		return listagemDocumentoDTO;
	}

	public List<Documento> getListaDocumentosParaSeremAssinados(String idTaskInstance) {
		CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Documento> query = cb.createQuery(Documento.class);
        Root<TaskInstancePermitidaAssinarDocumento> taskPermitida = query.from(TaskInstancePermitidaAssinarDocumento.class);
        Join<TaskInstancePermitidaAssinarDocumento, Documento> documento = taskPermitida.join(TaskInstancePermitidaAssinarDocumento_.documento);
        query.where(cb.equal(taskPermitida.get(TaskInstancePermitidaAssinarDocumento_.idTaskInstance), Long.valueOf(idTaskInstance)));
        query.select(documento);

        List<Documento> listaDocumento = getEntityManager().createQuery(query).getResultList();

        for(Iterator<Documento> itDocumento = listaDocumento.iterator(); itDocumento.hasNext();) {
        	Documento doc = itDocumento.next();
        	boolean usuarioPodeAssinar = assinaturaDocumentoService.isAssinavelPorUsuarioAtual(Authenticator.getPapelAtual(), doc.getClassificacaoDocumento(), doc.getDocumentoBin(), Authenticator.getUsuarioLogado());
        	if(!usuarioPodeAssinar) {
        		itDocumento.remove();
        	}
        }

        return listaDocumento;
	}

	public List<DocumentoAssinavelDTO> getDTODocumentosAssinar(Set<String> idTaskInstance) {

		List<Object[]> resultList = getEntityManager().createNativeQuery(DocumentosParaSeremAssinadosQuery.DOCUMENTOS_PARA_SEREM_ASSINADOS_QUERY
				.toString()).setParameter(DocumentosParaSeremAssinadosQuery.PARAM_TASK_INSTANCE, idTaskInstance).getResultList();

		List<DocumentoAssinavelDTO> listaDocumentoDTO = new ArrayList<>();
		for(Object[] record : resultList){
			listaDocumentoDTO.add(new DocumentoAssinavelDTO(Integer.valueOf(record[0].toString()),
					Integer.valueOf(record[1].toString()),
					Integer.valueOf(record[2].toString()),
					Boolean.valueOf(record[3].toString()),
					(String) record[4]));
		}

		for(Iterator<DocumentoAssinavelDTO> itDocumento = listaDocumentoDTO.iterator(); itDocumento.hasNext();) {
			DocumentoAssinavelDTO doc = itDocumento.next();
		boolean usuarioPodeAssinar = assinaturaDocumentoService
					.isAssinavelPorUsuarioAtual(Authenticator.getPapelAtual(), doc.getIdClassificacao(),
							doc.getIdDocumentoBin(), Authenticator.getUsuarioLogado());
		if(!usuarioPodeAssinar) {
				itDocumento.remove();
			}
		}

		return listaDocumentoDTO;
	}
}
