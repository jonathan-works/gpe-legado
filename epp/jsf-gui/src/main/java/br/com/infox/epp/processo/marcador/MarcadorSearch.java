package br.com.infox.epp.processo.marcador;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import br.com.infox.core.persistence.PersistenceController;
import br.com.infox.epp.entrega.documentos.Entrega;
import br.com.infox.epp.entrega.documentos.Entrega_;
import br.com.infox.epp.processo.documento.entity.Documento;
import br.com.infox.epp.processo.documento.entity.DocumentoBin;
import br.com.infox.epp.processo.documento.entity.DocumentoBin_;
import br.com.infox.epp.processo.documento.entity.DocumentoTemporario;
import br.com.infox.epp.processo.documento.entity.DocumentoTemporario_;
import br.com.infox.epp.processo.documento.entity.Documento_;
import br.com.infox.epp.processo.documento.entity.Pasta;
import br.com.infox.epp.processo.documento.entity.Pasta_;
import br.com.infox.epp.processo.entity.Processo_;

@Stateless
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class MarcadorSearch extends PersistenceController {
    
    public List<String> listCodigoByProcesso(Integer idProcesso) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<String> cq = cb.createQuery(String.class);
        Root<DocumentoBin> documentoBin = cq.from(DocumentoBin.class);
        Root<Pasta> pasta = cq.from(Pasta.class); 
        Join<DocumentoBin, Documento> documento = documentoBin.join(DocumentoBin_.documentoList, JoinType.LEFT);
        Join<DocumentoBin, DocumentoTemporario> documentoTemporario = documentoBin.join(DocumentoBin_.documentoTemporarioList, JoinType.LEFT);
        Join<DocumentoBin, Marcador> marcador = documentoBin.join(DocumentoBin_.marcadores, JoinType.INNER);
        Expression<String> codigo = marcador.get(Marcador_.codigo);
        cq.select(codigo).distinct(true);
        cq.where(
            cb.or(
                cb.equal(documento.get(Documento_.pasta).get(Pasta_.id), pasta.get(Pasta_.id)),
                cb.equal(documentoTemporario.get(DocumentoTemporario_.pasta).get(Pasta_.id), pasta.get(Pasta_.id))
            ),
            cb.isNotNull(pasta.get(Pasta_.processo)),
            cb.equal(pasta.get(Pasta_.processo).get(Processo_.idProcesso), cb.literal(idProcesso))
        );
        return getEntityManager().createQuery(cq).getResultList();
    }
    
    public List<Marcador> listMarcadorByProcessoAndInCodigosMarcadores(Integer idProcesso, Collection<String> codigoMarcadores) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Marcador> cq = cb.createQuery(Marcador.class);
        Root<DocumentoBin> documentoBin = cq.from(DocumentoBin.class);
        Root<Pasta> pasta = cq.from(Pasta.class);
        Join<DocumentoBin, Documento> documento = documentoBin.join(DocumentoBin_.documentoList, JoinType.LEFT);
        Join<DocumentoBin, DocumentoTemporario> documentoTemporario = documentoBin.join(DocumentoBin_.documentoTemporarioList, JoinType.LEFT);
        Join<DocumentoBin, Marcador> marcador = documentoBin.join(DocumentoBin_.marcadores, JoinType.INNER);
        cq.select(marcador).distinct(true);
        cq.where(
            cb.or(
                cb.equal(documento.get(Documento_.pasta).get(Pasta_.id), pasta.get(Pasta_.id)),
                cb.equal(documentoTemporario.get(DocumentoTemporario_.pasta).get(Pasta_.id), pasta.get(Pasta_.id))
            ),
            cb.isNotNull(pasta.get(Pasta_.processo)),
            cb.equal(pasta.get(Pasta_.processo).get(Processo_.idProcesso), cb.literal(idProcesso)),
            marcador.get(Marcador_.codigo).in(upperStrings(codigoMarcadores))
        );
        return getEntityManager().createQuery(cq).getResultList();
    }
    
    public List<String> listCodigoByProcessoAndCodigo(Integer idProcesso, String codigoMarcador) {
        return listCodigoByProcessoAndNotInListCodigos(idProcesso, codigoMarcador, null);
    }
    
    public List<String> listCodigoByProcessoAndNotInListCodigos(Integer idProcesso, String codigoMarcador, Collection<String> codigoMarcadores) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<String> cq = cb.createQuery(String.class);
        Root<DocumentoBin> documentoBin = cq.from(DocumentoBin.class);
        Root<Pasta> pasta = cq.from(Pasta.class); 
        Join<DocumentoBin, Documento> documento = documentoBin.join(DocumentoBin_.documentoList, JoinType.LEFT);
        Join<DocumentoBin, DocumentoTemporario> documentoTemporario = documentoBin.join(DocumentoBin_.documentoTemporarioList, JoinType.LEFT);
        Join<DocumentoBin, Marcador> marcador = documentoBin.join(DocumentoBin_.marcadores, JoinType.INNER);
        Expression<String> codigo = marcador.get(Marcador_.codigo);
        cq.select(codigo).distinct(true);
        Predicate predicate = cb.and(
                                  cb.or(
                                      cb.equal(documento.get(Documento_.pasta).get(Pasta_.id), pasta.get(Pasta_.id)),
                                      cb.equal(documentoTemporario.get(DocumentoTemporario_.pasta).get(Pasta_.id), pasta.get(Pasta_.id))
                                  ),
                                  cb.isNotNull(pasta.get(Pasta_.processo)),
                                  cb.equal(pasta.get(Pasta_.processo).get(Processo_.idProcesso), cb.literal(idProcesso)),
                                  cb.like(marcador.get(Marcador_.codigo), cb.upper(cb.literal("%" + codigoMarcador + "%")))
                              );
        if (codigoMarcadores != null && !codigoMarcadores.isEmpty()) {
            predicate = cb.and(
                            predicate,
                            cb.not(marcador.get(Marcador_.codigo).in(upperStrings(codigoMarcadores)))
                        );
        }
        cq.where(predicate);
        return getEntityManager().createQuery(cq).getResultList();
    }
    
    public List<String> listByPastaAndCodigo(Integer idPasta, String codigoMarcador, Collection<String> codigoMarcadores) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<String> cq = cb.createQuery(String.class);
        Root<DocumentoBin> documentoBin = cq.from(DocumentoBin.class);
        Join<DocumentoBin, Documento> documento = documentoBin.join(DocumentoBin_.documentoList, JoinType.LEFT);
        Join<DocumentoBin, DocumentoTemporario> documentoTemporario = documentoBin.join(DocumentoBin_.documentoTemporarioList, JoinType.LEFT);
        Join<DocumentoBin, Marcador> marcador = documentoBin.join(DocumentoBin_.marcadores, JoinType.INNER);
        Expression<String> codigo = marcador.get(Marcador_.codigo);
        cq.select(codigo).distinct(true);
        cq.where(
            cb.or(
                cb.equal(documento.get(Documento_.pasta).get(Pasta_.id), cb.literal(idPasta)),
                cb.equal(documentoTemporario.get(DocumentoTemporario_.pasta).get(Pasta_.id), cb.literal(idPasta))
            ),
            cb.like(marcador.get(Marcador_.codigo), cb.upper(cb.literal("%" + codigoMarcador + "%")))
        );
        if (codigoMarcadores != null && !codigoMarcadores.isEmpty()) {
            Predicate where = cq.getRestriction();
            cq.where(
                where,
                cb.not(marcador.get(Marcador_.codigo).in(upperStrings(codigoMarcadores)))
            );
        }
        return getEntityManager().createQuery(cq).getResultList();
    }
    
    public List<Marcador> listMarcadorByProcessoAndCodigo(Integer idProcesso, String codigoMarcador) {
        return listMarcadorByProcessoAndCodigo(idProcesso, codigoMarcador, null);
    }
    
    public List<Marcador> listMarcadorByProcessoAndCodigo(Integer idProcesso, String codigoMarcador, Collection<String> codigoMarcadores) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Marcador> cq = cb.createQuery(Marcador.class);
        Root<DocumentoBin> documentoBin = cq.from(DocumentoBin.class);
        Root<Pasta> pasta = cq.from(Pasta.class); 
        Join<DocumentoBin, Documento> documento = documentoBin.join(DocumentoBin_.documentoList, JoinType.LEFT);
        Join<DocumentoBin, DocumentoTemporario> documentoTemporario = documentoBin.join(DocumentoBin_.documentoTemporarioList, JoinType.LEFT);
        Join<DocumentoBin, Marcador> marcador = documentoBin.join(DocumentoBin_.marcadores, JoinType.INNER);
        cq.select(marcador).distinct(true);
        cq.where(
            cb.or(
                cb.equal(documento.get(Documento_.pasta).get(Pasta_.id), pasta.get(Pasta_.id)),
                cb.equal(documentoTemporario.get(DocumentoTemporario_.pasta).get(Pasta_.id), pasta.get(Pasta_.id))
            ),
            cb.isNotNull(pasta.get(Pasta_.processo)),
            cb.equal(pasta.get(Pasta_.processo).get(Processo_.idProcesso), cb.literal(idProcesso)),
            cb.like(marcador.get(Marcador_.codigo), cb.upper(cb.literal("%" + codigoMarcador + "%")))
        );
        if (codigoMarcadores != null && !codigoMarcadores.isEmpty()) {
            Predicate where = cq.getRestriction();
            cq.where(
                where,
                cb.not(marcador.get(Marcador_.codigo).in(upperStrings(codigoMarcadores)))
            );
        }
        return getEntityManager().createQuery(cq).setMaxResults(20).getResultList();
    }

    public List<String> listCodigoFromDocumentoByProcessoAndCodigoAndNotInCodigos(Integer idProcesso, String codigoMarcador, List<String> codigoMarcadores) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<String> cq = cb.createQuery(String.class);
        Root<DocumentoBin> documentoBin = cq.from(DocumentoBin.class);
        Join<DocumentoBin, Documento> documento = documentoBin.join(DocumentoBin_.documentoList, JoinType.INNER);
        Join<Documento, Pasta> pasta = documento.join(Documento_.pasta, JoinType.INNER); 
        Join<DocumentoBin, Marcador> marcador = documentoBin.join(DocumentoBin_.marcadores, JoinType.INNER);
        Expression<String> codigo = marcador.get(Marcador_.codigo);
        cq.select(codigo);
        cq.where(
            cb.equal(pasta.get(Pasta_.processo).get(Processo_.idProcesso), cb.literal(idProcesso)),
            cb.like(marcador.get(Marcador_.codigo), cb.upper(cb.literal("%" + codigoMarcador + "%")))
        );
        if (codigoMarcadores != null && !codigoMarcadores.isEmpty()) {
            Predicate where = cq.getRestriction();
            cq.where(
                where,
                cb.not(marcador.get(Marcador_.codigo).in(upperStrings(codigoMarcadores)))
            );
        }
        cq.groupBy(codigo);
        cq.orderBy(cb.desc(cb.countDistinct(documentoBin.get(DocumentoBin_.id))));
        return getEntityManager().createQuery(cq).setMaxResults(20).getResultList();
    }
    
    public List<String> listCodigoMarcadorByEntrega(Long idEntrega) {
        return listCodigoByEntregaAndCodigoAndNotInListCodigo(idEntrega, null, null);
    }
    
    public List<String> listCodigoMarcadorByEntregaAndCodigo(Long idEntrega, String codigoMarcador) {
        return listCodigoByEntregaAndCodigoAndNotInListCodigo(idEntrega, codigoMarcador, null);
    }
    
    public List<String> listCodigoByEntregaAndCodigoAndNotInListCodigo(Long idEntrega, String codigoMarcador, List<String> codigosMarcadores) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<String> cq = cb.createQuery(String.class);
        Root<DocumentoBin> documentoBin = cq.from(DocumentoBin.class);
        Root<Entrega> entrega = cq.from(Entrega.class);
        Join<DocumentoBin, Documento> documento = documentoBin.join(DocumentoBin_.documentoList, JoinType.INNER);
        Join<Documento, Pasta> pasta = documento.join(Documento_.pasta, JoinType.INNER);
        Join<DocumentoBin, Marcador> marcador = documentoBin.join(DocumentoBin_.marcadores, JoinType.INNER);
        cq.select(marcador.get(Marcador_.codigo));
        Predicate predicate = cb.and(
                                  cb.equal(pasta.get(Pasta_.id), entrega.get(Entrega_.pasta).get(Pasta_.id)),
                                  cb.equal(entrega.get(Entrega_.id), cb.literal(idEntrega))
                              );
        if (codigoMarcador != null) {
            predicate = cb.and( predicate, 
                            cb.like(marcador.get(Marcador_.codigo), cb.upper(cb.literal("%" + codigoMarcador + "%")))
                        );
        }
        
        if (codigosMarcadores != null && !codigosMarcadores.isEmpty()) {
            predicate = cb.and( predicate, 
                            cb.not(marcador.get(Marcador_.codigo).in(upperStrings(codigosMarcadores)))
                        );
        }
        cq.where(predicate);
        return getEntityManager().createQuery(cq).getResultList();
    }
    
    public List<Marcador> listMarcadorByEntrega(Long idEntrega) {
        return listMarcadorByEntregaAndCodigoAndNotInListCodigo(idEntrega, null, null);
    }
    
    public List<Marcador> listMarcadorByEntregaAndCodigo(Long idEntrega, String codigoMarcador) {
        return listMarcadorByEntregaAndCodigoAndNotInListCodigo(idEntrega, codigoMarcador, null);
    }
    
    public List<Marcador> listMarcadorByEntregaAndCodigoAndNotInListCodigo(Long idEntrega, String codigoMarcador, List<String> codigosMarcadores) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Marcador> cq = cb.createQuery(Marcador.class);
        Root<DocumentoBin> documentoBin = cq.from(DocumentoBin.class);
        Root<Entrega> entrega = cq.from(Entrega.class);
        Join<DocumentoBin, Documento> documento = documentoBin.join(DocumentoBin_.documentoList, JoinType.INNER);
        Join<Documento, Pasta> pasta = documento.join(Documento_.pasta, JoinType.INNER);
        Join<DocumentoBin, Marcador> marcador = documentoBin.join(DocumentoBin_.marcadores, JoinType.INNER);
        cq.select(marcador);
        Predicate predicate = cb.and(
                                  cb.equal(pasta.get(Pasta_.id), entrega.get(Entrega_.pasta).get(Pasta_.id)),
                                  cb.equal(entrega.get(Entrega_.id), cb.literal(idEntrega))
                              );
        if (codigoMarcador != null) {
            predicate = cb.and( predicate, 
                            cb.like(marcador.get(Marcador_.codigo), cb.upper(cb.literal("%" + codigoMarcador + "%")))
                        );
        }
        
        if (codigosMarcadores != null && !codigosMarcadores.isEmpty()) {
            predicate = cb.and( predicate, 
                            cb.not(marcador.get(Marcador_.codigo).in(upperStrings(codigosMarcadores)))
                        );
        }
        cq.where(predicate);
        return getEntityManager().createQuery(cq).getResultList();
    }
    
    public List<Marcador> listMarcadorByEntregaAndInCodigosMarcadores(Long idEntrega, Collection<String> codigoMarcadores) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Marcador> cq = cb.createQuery(Marcador.class);
        
        Root<DocumentoBin> documentoBin = cq.from(DocumentoBin.class);
        Root<Entrega> entrega = cq.from(Entrega.class);
        Join<DocumentoBin, Documento> documento = documentoBin.join(DocumentoBin_.documentoList, JoinType.INNER);
        Join<Documento, Pasta> pasta = documento.join(Documento_.pasta, JoinType.INNER);
        Join<DocumentoBin, Marcador> marcador = documentoBin.join(DocumentoBin_.marcadores, JoinType.INNER);
        cq.select(marcador);
        
        cq.where(
        	cb.equal(entrega.get(Entrega_.id), cb.literal(idEntrega)),
        	cb.equal(pasta.get(Pasta_.id), entrega.get(Entrega_.pasta).get(Pasta_.id)),
            marcador.get(Marcador_.codigo).in(upperStrings(codigoMarcadores))
        );
        return getEntityManager().createQuery(cq).getResultList();
    }
    
    private List<String> upperStrings(Collection<String> strings) {
    	List<String> stringsUpperCase = new ArrayList<String>();
    	Iterator<String> it = strings.iterator();
    	while (it.hasNext()) {
    		stringsUpperCase.add(it.next().toUpperCase());
    	}
    	return stringsUpperCase;
    }
    
    public Marcador getMarcadorByCodigo(String codigoMarcador, Integer idProcesso) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Marcador> cq = cb.createQuery(Marcador.class);
        Root<DocumentoBin> documentoBin = cq.from(DocumentoBin.class);
        Root<Pasta> pasta = cq.from(Pasta.class); 
        Join<DocumentoBin, Documento> documento = documentoBin.join(DocumentoBin_.documentoList, JoinType.LEFT);
        Join<DocumentoBin, DocumentoTemporario> documentoTemporario = documentoBin.join(DocumentoBin_.documentoTemporarioList, JoinType.LEFT);
        Join<DocumentoBin, Marcador> marcador = documentoBin.join(DocumentoBin_.marcadores, JoinType.INNER);
        cq.select(marcador).distinct(true);
        Predicate predicate = cb.and(
                                  cb.or(
                                      cb.equal(documento.get(Documento_.pasta).get(Pasta_.id), pasta.get(Pasta_.id)),
                                      cb.equal(documentoTemporario.get(DocumentoTemporario_.pasta).get(Pasta_.id), pasta.get(Pasta_.id))
                                  ),
                                  cb.isNotNull(pasta.get(Pasta_.processo)),
                                  cb.equal(pasta.get(Pasta_.processo).get(Processo_.idProcesso), cb.literal(idProcesso)),
                                  cb.equal(marcador.get(Marcador_.codigo), cb.upper(cb.literal(codigoMarcador)))
                              );
        cq.where(predicate);
        List<Marcador> resultList =  getEntityManager().createQuery(cq).setMaxResults(1).getResultList();
        return resultList.isEmpty() ? null : resultList.get(0);
    }
    
}
