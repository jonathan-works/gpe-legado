package br.com.infox.epp.processo.documento.manager;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

import br.com.infox.core.manager.Manager;
import br.com.infox.core.persistence.DAOException;
import br.com.infox.epp.documento.pasta.PastaSearch;
import br.com.infox.epp.fluxo.entity.ModeloPasta;
import br.com.infox.epp.fluxo.manager.ModeloPastaManager;
import br.com.infox.epp.processo.documento.dao.PastaDAO;
import br.com.infox.epp.processo.documento.entity.Documento;
import br.com.infox.epp.processo.documento.entity.Documento_;
import br.com.infox.epp.processo.documento.entity.Pasta;
import br.com.infox.epp.processo.documento.entity.PastaRestricao;
import br.com.infox.epp.processo.documento.filter.DocumentoFilter;
import br.com.infox.epp.processo.documento.numeration.NumeracaoDocumentoSequencialManager;
import br.com.infox.epp.processo.documento.service.DocumentoService;
import br.com.infox.epp.processo.documento.type.PastaRestricaoEnum;
import br.com.infox.epp.processo.entity.Processo;
import br.com.infox.epp.processo.manager.ProcessoManager;
import br.com.infox.epp.processo.metadado.entity.MetadadoProcesso;
import br.com.infox.epp.processo.metadado.manager.MetadadoProcessoManager;
import br.com.infox.epp.processo.metadado.type.EppMetadadoProvider;
import br.com.infox.seam.exception.BusinessRollbackException;

@AutoCreate
@Name(PastaManager.NAME)
@Stateless
public class PastaManager extends Manager<PastaDAO, Pasta> {

    private static final long serialVersionUID = 1L;
    public static final String NAME = "pastaManager";
    
    @Inject
    private DocumentoService documentoService;
    @Inject
    private MetadadoProcessoManager metadadoProcessoManager;
    @Inject
    private PastaRestricaoManager pastaRestricaoManager;
    @Inject
    private ModeloPastaManager modeloPastaManager;
    @In
    private ProcessoManager processoManager;
    @In
    private DocumentoManager documentoManager;
    @In
    private NumeracaoDocumentoSequencialManager numeracaoDocumentoSequencialManager;
    @Inject
    private PastaSearch pastaSearch;
    
    public Pasta getDefaultFolder(Processo processo) throws DAOException {
        Pasta pasta = getDefault(processo);
        return pasta;
    }
    
    public List<Pasta> getByProcesso(Processo processo) {
        List<Pasta> pastaList = getDao().getByProcesso(processo);
        return pastaList;
    }
    
    public int getTotalDocumentosPasta(Pasta pasta) {
    	return getDao().getTotalDocumentosPasta(pasta);
    }
    
    public int getTotalDocumentosPastaPorFiltros(Pasta pasta, DocumentoFilter documentoFilter, Boolean semExcluidos) {
    	return getDao().getTotalDocumentosPastaPorFiltros(pasta, documentoFilter, semExcluidos);
    }
    public int getTotalDocumentosPasta(Pasta pasta, String customFilter, Map<String, Object> params) {
    	return getDao().getTotalDocumentosPasta(pasta, customFilter, params);
    }
    
    public String getNomePasta(Pasta pasta) {
    	return getNomePasta(pasta, getTotalDocumentosPasta(pasta));
    }
    
    public String getNomePasta(Pasta pasta, int totalDocumentos) {
    	return MessageFormat.format(pasta.getTemplateNomePasta(), totalDocumentos);
    }
    
    public String getNomePasta(Pasta pasta,  DocumentoFilter documentoFilter, Boolean semExcluidos) {
    	return getNomePasta(pasta, getTotalDocumentosPastaPorFiltros(pasta, documentoFilter, semExcluidos));
    }
    
    public List<Pasta> createDefaultFolders(Processo processo) throws DAOException {
        Processo root = processo.getProcessoRoot();
        List<Pasta> pastaList = root.getPastaList();
        List<ModeloPasta> modeloPastaList = modeloPastaManager.getByFluxo(processo.getNaturezaCategoriaFluxo().getFluxo());
        Pasta padraoFromModelo = null;
        for (ModeloPasta modeloPasta : modeloPastaList) {
            Pasta createFromModelo = createFromModelo(modeloPasta, root);
            pastaList.add(createFromModelo);
            if (modeloPasta.getPadrao()) {
                padraoFromModelo = createFromModelo;
            }
        }
        MetadadoProcesso metadadoPastaDefault = processo.getMetadado(EppMetadadoProvider.PASTA_DEFAULT);
        Pasta padrao = metadadoPastaDefault == null ? null : (Pasta) metadadoPastaDefault.getValue();
        if (padrao == null && padraoFromModelo != null) {
            documentoService.setDefaultFolder(padraoFromModelo);
            metadadoProcessoManager.addMetadadoProcesso(processo, EppMetadadoProvider.PASTA_DEFAULT, padraoFromModelo.getId().toString());
        }
        return pastaList;
    }
    
    public List<Pasta> createDefaultFoldersChild(Processo processo) throws DAOException {
        List<Pasta> pastaList = processo.getPastaList();
        List<ModeloPasta> modeloPastaList = modeloPastaManager.getByFluxo(processo.getNaturezaCategoriaFluxo().getFluxo());
        for (ModeloPasta modeloPasta : modeloPastaList) {
            pastaList.add(createFromModeloChild(modeloPasta, processo));
        }
        if (!pastaList.isEmpty()) {
            Pasta padrao = pastaList.get(0);
            documentoService.setDefaultFolder(padrao);
            metadadoProcessoManager.addMetadadoProcesso(processo, EppMetadadoProvider.PASTA_DEFAULT, padrao.getId().toString());
        }
        return pastaList;
    }
    
    public Pasta createFromModelo(ModeloPasta modeloPasta) throws DAOException {
    	return createPastaFromModelo(modeloPasta, null);
    }
    
    protected Pasta createFromModelo(ModeloPasta modeloPasta, Processo processo) throws DAOException {
        return createPastaFromModelo(modeloPasta, processo.getProcessoRoot());
    }
    
    protected Pasta createFromModeloChild(ModeloPasta modeloPasta, Processo processo) throws DAOException {
        return createPastaFromModelo(modeloPasta, processo);
    }

    private Pasta createPastaFromModelo(ModeloPasta modeloPasta, Processo processo) {
        Pasta pasta = new Pasta();
        pasta.setNome(modeloPasta.getNome());
        pasta.setCodigo(modeloPasta.getCodigo());
        pasta.setRemovivel(modeloPasta.getRemovivel());
        pasta.setProcesso(processo);
        pasta.setSistema(modeloPasta.getSistema());
        pasta.setEditavel(modeloPasta.getEditavel());
        pasta.setDescricao(modeloPasta.getDescricao());
        pasta.setOrdem(modeloPasta.getOrdem());
        persist(pasta);
        pastaRestricaoManager.createRestricoesFromModelo(modeloPasta, pasta);
        return pasta;
    }

    public Pasta getDefault(Processo processo) {
    	List<MetadadoProcesso> metaPastas = metadadoProcessoManager.getMetadadoProcessoByType(processo, EppMetadadoProvider.PASTA_DEFAULT.getMetadadoType());
        if (!metaPastas.isEmpty()) {
        	return (Pasta)metaPastas.get(0).getValue();
        } else if (!processo.getProcessoRoot().getIdProcesso().equals(processo.getIdProcesso())) {
            return getDefault(processo.getProcessoRoot());
        } else {
        	return null;
        }
    }
    
    public Pasta getByCodigoAndProcesso(String codigoPasta, Processo processo) {
		Pasta pasta = pastaSearch.getPastaByCodigoIdProcesso(codigoPasta, processo.getIdProcesso());
		if (pasta == null) {
			pasta = pastaSearch.getPastaByCodigoIdProcesso(codigoPasta, processo.getProcessoRoot().getIdProcesso());
		}
		return pasta;
    }
    
    public Pasta persistWithDefault(Pasta o) throws DAOException {
    	Boolean editavel = (o.getEditavel() == null) ? Boolean.TRUE : o.getEditavel();
    	o.setEditavel(editavel);
		Boolean removivel = (o.getRemovivel() == null) ? Boolean.TRUE : o.getRemovivel();
		o.setRemovivel(removivel);
    	Pasta pasta = super.persist(o);
    	pastaRestricaoManager.persistRestricaoDefault(pasta); 
    	return pasta;
    }

    public void deleteComRestricoes(Pasta pasta) throws DAOException {
        pastaRestricaoManager.deleteByPasta(pasta);
        remove(pasta);
    }
    
    public Pasta getByProcessoAndDescricao(Processo processo, String descricao) {
        return getDao().getByProcessoAndDescricao(processo, descricao);
    }
    
    public Pasta getPastaByNome(String nome, Processo processo) {
    	return getDao().getPastaByNome(nome, processo);
    }
    
    public void disponibilizarPastaParaParticipantesProcesso(String descricaoPasta, Long idProcesso)
            throws DAOException {
        Processo processo = processoManager.find(idProcesso.intValue());
        if (processo != null) {
            Pasta pasta = getDao().getByProcessoAndDescricao(processo.getProcessoRoot(), descricaoPasta);
            if (pasta != null) {
                disponibilizarParaLeitura(pasta, 1, PastaRestricaoEnum.R);

            }
        }
    }

    public void tornarPastaPublica(String nomePasta, Long idProcesso) throws DAOException {
        Processo processo = processoManager.find(idProcesso.intValue());
        if (processo != null) {
            Pasta pasta = getDao().getByProcessoAndDescricao(processo.getProcessoRoot(), nomePasta);
            if (pasta != null) {
                disponibilizarParaLeitura(pasta, 0, PastaRestricaoEnum.R);
                disponibilizarParaLeitura(pasta, null, PastaRestricaoEnum.D);
            }
        }
    }

    private void disponibilizarParaLeitura(Pasta pasta, Integer alvo, PastaRestricaoEnum tipoRestricao)
            throws DAOException {
        PastaRestricao pastaRestricao = pastaRestricaoManager.getByPastaAlvoTipoRestricao(pasta, alvo, tipoRestricao);
        if (pastaRestricao == null) {
            pastaRestricao = new PastaRestricao();
            pastaRestricao.setAlvo(alvo);
            pastaRestricao.setTipoPastaRestricao(tipoRestricao);
            pastaRestricao.setWrite(false);
            pastaRestricao.setRemove(false);
            pastaRestricao.setRead(true);
            pastaRestricao.setPasta(pasta);
            pastaRestricao.setLogicDelete(false);
            pastaRestricaoManager.persist(pastaRestricao);
        } else {
            pastaRestricao.setRead(true);
            pastaRestricaoManager.update(pastaRestricao);
        }
    }
    
    public Pasta copiarPastaDocumentosParaProcesso(Pasta pasta, Processo processo) {
    	try {
	    	Pasta copia = pasta.makeCopy();
	    	copia.setProcesso(processo);
	    	persist(copia);
	    	for (Documento documento : copia.getDocumentosList()) {
	    		documentoManager.persist(documento);
	    	}
	    	List<PastaRestricao> restricoes = pastaRestricaoManager.copyRestricoes(pasta, copia);
	    	for (PastaRestricao restricao : restricoes) {
	    		pastaRestricaoManager.persist(restricao);
	    	}
	    	return copia;
    	} catch (CloneNotSupportedException e) {
    		throw new BusinessRollbackException(e);
    	}
    }
    
	public void renumerarDocumentos(Pasta pasta) {
		EntityManager entityManager = getDao().getEntityManager();
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Documento> query = cb.createQuery(Documento.class);
		Root<Documento> root = query.from(Documento.class);
		query.where(cb.equal(root.get(Documento_.pasta), pasta), cb.isNull(root.get(Documento_.numeroSequencialDocumento)));
		query.orderBy(cb.asc(root.get(Documento_.dataInclusao)));
		List<Documento> documentos = entityManager.createQuery(query).getResultList();
		
		for (Documento documento : documentos) {
			documento.setNumeroSequencialDocumento(numeracaoDocumentoSequencialManager.getNextNumeracaoDocumentoSequencial(pasta.getProcesso()));
			documentoManager.update(documento);
		}
	}
	
	public void copiarDocumentos(Pasta original, Pasta novaPasta) {
		try {
			for (Documento documento : original.getDocumentosList()) {
				Documento copia = documento.makeCopy();
				copia.setNumeroSequencialDocumento(null);
				copia.setPasta(novaPasta);
				documentoManager.persist(copia);
			}
		} catch (CloneNotSupportedException e) {
    		throw new BusinessRollbackException(e);
    	}
	}

	public Boolean isPadraoEmAlgumProcesso(Pasta pasta) {
	    return getDao().isPadraoEmAlgumProcesso(pasta);
	}

    public void removeMetadadoPadrao(Pasta pasta) {
        List<MetadadoProcesso> metadadoList = getDao().listMetadadoPastaDefault(pasta);
        metadadoProcessoManager.removeAll(metadadoList);
    }
}
