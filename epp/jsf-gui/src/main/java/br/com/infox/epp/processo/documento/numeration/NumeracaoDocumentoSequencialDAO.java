package br.com.infox.epp.processo.documento.numeration;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.Stateless;
import javax.persistence.LockModeType;

import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;

import br.com.infox.core.dao.DAO;
import br.com.infox.core.persistence.DAOException;
import br.com.infox.epp.processo.entity.Processo;

@Stateless
@AutoCreate
@Name(NumeracaoDocumentoSequencialDAO.NAME)
public class NumeracaoDocumentoSequencialDAO extends DAO<NumeracaoDocumentoSequencial> {
	
	private static final long serialVersionUID = 1L;
	public static final String NAME = "numeracaoDocumentoSequencialDAO";
	
	public Integer getNextNumeracaoDocumentoSequencial(Processo processo) throws DAOException {
	    Map<String, Object> parameters = new HashMap<>();
        parameters.put("processo", processo.getProcessoRoot());
        NumeracaoDocumentoSequencial next = getNamedSingleResult(NumeracaoDocumentoSequencial.GET_NEXT_VALUE, parameters);
        if (next == null) {
            next = new NumeracaoDocumentoSequencial();
            next.setProcessoRaiz(processo.getProcessoRoot());
            next.setNextNumero(2);
            persist(next);
            return 1;
        } else {
            detach(next);
            next = getNamedSingleResult(NumeracaoDocumentoSequencial.GET_NEXT_VALUE, parameters);
            lock(next, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
            Integer result = next.getNextNumero();
            next.setNextNumero(next.getNextNumero() + 1);
            update(next);
            return result;
        }
	}
	
    public NumeracaoDocumentoSequencial removeByProcesso(Processo processo) throws DAOException {
        Map<String, Object> params = new HashMap<>();
        params.put(NumeracaoDocumentoSequencial.PARAM_PROCESSO, processo);
        executeNamedQueryUpdate(NumeracaoDocumentoSequencial.DELETE_BY_PROCESSO, params);
        return null;
    }

	public void createNumeracao(Processo processo, Integer valorInicial) {
		NumeracaoDocumentoSequencial numeracao = new NumeracaoDocumentoSequencial();
		numeracao.setNextNumero(valorInicial);
		numeracao.setProcessoRaiz(processo);
		persist(numeracao);
	}
}
