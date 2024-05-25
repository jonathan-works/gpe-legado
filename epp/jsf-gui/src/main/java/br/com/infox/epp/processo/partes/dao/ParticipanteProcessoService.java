package br.com.infox.epp.processo.partes.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import br.com.infox.epp.fluxo.entity.Natureza;
import br.com.infox.epp.pessoa.type.TipoPessoaEnum;
import br.com.infox.epp.processo.entity.Processo;
import br.com.infox.epp.processo.entity.RelacionamentoProcessoInterno;
import br.com.infox.epp.processo.partes.entity.ParticipanteProcesso;
import br.com.infox.epp.processo.partes.manager.ParticipanteProcessoManager;
import br.com.infox.epp.processo.partes.type.ParteProcessoEnum;
import br.com.infox.epp.relacionamentoprocessos.RelacionamentoProcessoDAO;

@Stateless
public class ParticipanteProcessoService {

	@Inject
	private RelacionamentoProcessoDAO relacionamentoProcessoDAO;
	@Inject
	private ParticipanteProcessoManager participanteProcessoManager;

	public void adicionaParticipantesProcessoRelacionado(Processo processo) {
		List<RelacionamentoProcessoInterno> relacionados = relacionamentoProcessoDAO.getListProcessosEletronicosRelacionados(processo);
		Iterator<RelacionamentoProcessoInterno> it = relacionados.iterator();
		while (it.hasNext()) {
			Processo relacionado = it.next().getProcesso();
			List<ParticipanteProcesso> participantesRaizProcesso = participanteProcessoManager.getParticipantesProcessoRaiz(relacionado);
			for (ParticipanteProcesso raiz : participantesRaizProcesso) {
				try {
					adicionaParticipantesFilhos(processo, raiz, relacionado);

				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void adicionaParticipantesFilhos(Processo processo, ParticipanteProcesso parte, Processo relacionado) throws CloneNotSupportedException {

		List<ParticipanteProcesso> listaPartes = new ArrayList<>();
		boolean insereParticipante = false;

		ParticipanteProcesso existente = participanteProcessoManager.getParticipanteProcessoByPessoaProcesso(parte.getPessoa(), processo);
		if (existente != null) {

			boolean mesmoTipo = existente.getTipoParte().getId() == parte.getTipoParte().getId();

			boolean mesmoSuperior = false;
			if ((existente.getParticipantePai() != null && parte.getParticipantePai() != null
					&& existente.getParticipantePai().getId() == parte.getParticipantePai().getId())
					|| (existente.getParticipantePai() == null && parte.getParticipantePai() == null))
				mesmoSuperior = true;

			Date ptIni = parte.getDataInicio();
			Date ptFim = parte.getDataFim();
			Date exIni = existente.getDataInicio();
			Date exFim = existente.getDataFim();

			boolean mesmoPeriodo = (exIni.compareTo(ptIni) == 0)
					&& ((exFim == null && ptFim == null) || (exFim != null && ptFim != null && exFim.compareTo(ptFim) == 0));

			if (mesmoTipo && mesmoSuperior && mesmoPeriodo) {
				// se é tudo igual nao faz nada
			} else if (mesmoTipo && mesmoSuperior && !mesmoPeriodo) {
				if (ptIni.before(exIni) && (ptFim == null || (ptFim != null && ptFim.after(exIni)))) {
					mesmoPeriodo = false;
					existente.setDataInicio(ptIni);
				}

				if (ptFim == null || (ptFim != null && exFim != null && ptFim.after(exFim))) {
					mesmoPeriodo = false;
					existente.setDataFim(ptFim);
				}
				
				if (existente.getDataFim() != null && existente.getDataFim().compareTo(ptIni) == 1) {
					existente.setDataFim(ptFim);
				}
				
				if (parte.getDataFim() != null && existente.getDataInicio().compareTo(parte.getDataFim()) == 1) {
					existente.setDataInicio(ptIni);
				}

				participanteProcessoManager.update(existente);
			} else {
				insereParticipante = true;
			}
		} else {
			insereParticipante = true;
		}

		if (insereParticipante) {
			ParticipanteProcesso participanteRaizCopy = parte.copiarParticipanteMantendoFilhos();
			participanteRaizCopy.setProcesso(processo);
			participanteRaizCopy.setParticipantesFilhos(getCopyParticipantesFilhos(participanteRaizCopy, processo));
			listaPartes.add(participanteRaizCopy);
			preencherParticipantesFilhos(processo, listaPartes, participanteRaizCopy.getParticipantesFilhos());
			participanteProcessoManager.gravarListaParticipantes(listaPartes);
		}

	}

	private List<ParticipanteProcesso> getCopyParticipantesFilhos(ParticipanteProcesso participanteAtual, Processo processo)
			throws CloneNotSupportedException {

		List<ParticipanteProcesso> participantesFilhos = new ArrayList<>();
		for (ParticipanteProcesso participanteProcesso : participanteAtual.getParticipantesFilhos()) {
			ParticipanteProcesso parteFilho = participanteProcesso.copiarParticipanteMantendoFilhos();
			parteFilho.setProcesso(processo);
			parteFilho.setParticipantePai(participanteAtual);
			participantesFilhos.add(parteFilho);
		}
		return participantesFilhos;
	}

	private void preencherParticipantesFilhos(Processo processo, List<ParticipanteProcesso> participantesParaPersistir,
			List<ParticipanteProcesso> participantesFilhos) throws CloneNotSupportedException {

		for (ParticipanteProcesso filho : participantesFilhos) {
			filho.setParticipantesFilhos(getCopyParticipantesFilhos(filho, processo));
			participantesParaPersistir.add(filho);
			preencherParticipantesFilhos(processo, participantesParaPersistir, filho.getParticipantesFilhos());
		}
	}
    
    /**
     * MÉTODOS MIGRADOS DA CAMADA DE APRESENTAÇÃO
     *   
     * FIXME: OTIMIZAR MÉTODOS
     * {@link ParticipanteProcessoService#podeAdicionarAlgumTipoDeParte(Processo)}
     * {@link ParticipanteProcessoService#podeAdicionarAmbosTiposDeParte(Processo)}
     * {@link ParticipanteProcessoService#podeAdicionarPartesFisicas(Processo)}
     * {@link ParticipanteProcessoService#podeAdicionarPartesJuridicas(Processo)}
     * {@link ParticipanteProcessoService#filtrar(List, TipoPessoaEnum)}
     * {@link ParticipanteProcessoService#getPartesAtivas(List)}
     */
    public boolean podeAdicionarAlgumTipoDeParte(Processo processo) {
        return podeAdicionarPartesFisicas(processo) || podeAdicionarPartesJuridicas(processo);
    }
    
    public boolean podeAdicionarAmbosTiposDeParte(Processo processo) {
        return podeAdicionarPartesFisicas(processo) && podeAdicionarPartesJuridicas(processo);
    }
    
    public boolean podeAdicionarPartesFisicas(Processo processo) {
        if (processo == null)
            return false;
        Natureza natureza = processo.getNaturezaCategoriaFluxo().getNatureza();
        return natureza != null
                && natureza.getHasPartes()
                && !ParteProcessoEnum.J.equals(natureza.getTipoPartes())
                && (natureza.getNumeroPartesFisicas() == QUANTIDADE_INFINITA_PARTES || getPartesAtivas(filtrar(processo.getParticipantes(), TipoPessoaEnum.F)).size() < natureza.getNumeroPartesFisicas());
    }

    public boolean podeAdicionarPartesJuridicas(Processo processo) {
        if (processo == null)
            return false;
        Natureza natureza = processo.getNaturezaCategoriaFluxo().getNatureza();
        return natureza != null &&
                natureza.getHasPartes()
                && !ParteProcessoEnum.F.equals(natureza.getTipoPartes())
                && (natureza.getNumeroPartesJuridicas() == QUANTIDADE_INFINITA_PARTES || getPartesAtivas(filtrar(processo.getParticipantes(), TipoPessoaEnum.J)).size() < natureza.getNumeroPartesJuridicas());
    }
    
    private static final int QUANTIDADE_INFINITA_PARTES = 0;
    
    private List<ParticipanteProcesso> filtrar(List<ParticipanteProcesso> participantes, 
            TipoPessoaEnum tipoPessoa) {
        List<ParticipanteProcesso> filtrado = new ArrayList<>();
        for (ParticipanteProcesso participante : participantes) {
            if (tipoPessoa.equals(participante.getPessoa().getTipoPessoa())) {
                filtrado.add(participante);
            }
        }
        return filtrado;
    }
    
    private List<ParticipanteProcesso> getPartesAtivas(List<ParticipanteProcesso> participantes) {
        List<ParticipanteProcesso> participantesAtivas = new ArrayList<>();
        for (ParticipanteProcesso participante : participantes) {
            if (participante.getAtivo()) {
                participantesAtivas.add(participante);
            }
        }
        return participantesAtivas;
    }
}
