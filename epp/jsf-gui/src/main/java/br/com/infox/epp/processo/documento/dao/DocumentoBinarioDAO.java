package br.com.infox.epp.processo.documento.dao;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;

import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;

import br.com.infox.cdi.producer.EntityManagerProducer;
import br.com.infox.core.dao.DAO;
import br.com.infox.core.persistence.DAOException;
import br.com.infox.epp.documento.entity.DocumentoBinario;
import br.com.infox.epp.processo.documento.query.DocumentoBinarioQuery;

@Stateless
@AutoCreate
@Name(DocumentoBinarioDAO.NAME)
public class DocumentoBinarioDAO extends DAO<DocumentoBinario> {

    private static final long serialVersionUID = 1L;
    public static final String NAME = "documentoBinarioDAO";

    @Override
    public EntityManager getEntityManager() {
        return EntityManagerProducer.getEntityManagerBin();
    }

    public byte[] getData(int idDocumentoBin) {
        return find(idDocumentoBin).getDocumentoBinario();
    }

    public DocumentoBinario gravarBinario(int idDocumentoBinario, byte[] file) throws DAOException {
        return persist(createDocumentoBinario(idDocumentoBinario, file));
    }

    private DocumentoBinario createDocumentoBinario(int idDocumentoBinario, byte[] file) {
        DocumentoBinario documentoBinario = new DocumentoBinario();
        documentoBinario.setId(idDocumentoBinario);
        documentoBinario.setDocumentoBinario(file);
        return documentoBinario;
    }

    public boolean existeBinario(int idDocumentoBinario) {
        Map<String, Object> params = new HashMap<>();
        params.put(DocumentoBinarioQuery.QUERY_PARAM_ID_DOCUMENTO_BIN, idDocumentoBinario);
        return getNamedSingleResult(DocumentoBinarioQuery.EXISTE_BINARIO, params) != null;
    }
}
