package br.com.infox.epp.processo.partes.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

import br.com.infox.core.persistence.generator.CustomIdGenerator;
import br.com.infox.epp.pessoa.entity.Pessoa;
import br.com.infox.epp.processo.entity.Processo;

import static br.com.infox.epp.processo.partes.query.ParticipanteProcessoQuery.*;
import static br.com.infox.epp.processo.query.ProcessoQuery.*;
import static br.com.infox.epp.processo.query.ProcessoQuery.GET_PROCESSO_BY_ID_PROCESSO_AND_ID_USUARIO_QUERY;

@Entity
@Table(name = ParticipanteProcesso.TABLE_NAME)
@NamedQueries(value={
		@NamedQuery(name=PARTICIPANTE_PROCESSO_BY_PESSOA_PROCESSO, query=PARTICIPANTE_PROCESSO_BY_PESSOA_PROCESSO_QUERY),
		@NamedQuery(name=EXISTE_PARTICIPANTE_BY_PESSOA_PROCESSO_PAI_TIPO, query=EXISTE_PARTICIPANTE_BY_PESSOA_PROCESSO_PAI_TIPO_QUERY),
		@NamedQuery(name=EXISTE_PARTICIPANTE_BY_PESSOA_PROCESSO_TIPO, query=EXISTE_PARTICIPANTE_BY_PESSOA_PROCESSO_TIPO_QUERY),
		@NamedQuery(name = PARTICIPANTES_PROCESSO, query = PARTICIPANTES_PROCESSO_QUERY),
		@NamedQuery(name = PARTICIPANTES_PROCESSO_RAIZ, query = PARTICIPANTES_PROCESSO_RAIZ_QUERY),
		@NamedQuery(name = PARTICIPANTES_BY_PROCESSO_PARTICIPANTE_FILHO, query = PARTICIPANTES_BY_PROCESSO_PARTICIPANTE_FILHO_QUERY),
		@NamedQuery(name = PESSOA_BY_PARTICIPANTE_PROCESSO, query = PESSOA_BY_PARTICIPANTE_PROCESSO_QUERY),
		@NamedQuery(name = PARTICIPANTE_BY_PESSOA_FETCH, query = PARTICIPANTE_BY_PESSOA_FETCH_QUERY),
		@NamedQuery(name = EXISTE_PARTICIPANTE_FILHO_BY_PROCESSO, query = EXISTE_PARTICIPANTE_FILHO_BY_PROCESSO_QUERY)
})
public class ParticipanteProcesso implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;
    public static final String TABLE_NAME = "tb_participante_processo";

    @Id
    @NotNull
    @Column(name = "id_participante_processo", nullable = false)
    private Integer id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_processo", nullable = false)
    private Processo processo;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pessoa", nullable = false)
    private Pessoa pessoa;
    
    @NotNull
    @Column(name = "nm_participante", nullable = false)
    private String nome;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_parte", nullable = false)
    private TipoParte tipoParte;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_participante_pai", nullable = true)
    private ParticipanteProcesso participantePai;
    
    @Column(name = "in_ativo")
    private Boolean ativo = Boolean.TRUE;
    
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "dt_inicio_participacao", nullable = false)
	private Date dataInicio;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "dt_fim_participacao")
	private Date dataFim;
	
	@Column(name = "ds_caminho_absoluto")
	private String caminhoAbsoluto;
    
    @OneToMany(fetch=FetchType.LAZY, mappedBy="participantePai")
    private List<ParticipanteProcesso> participantesFilhos = new ArrayList<>();
    
    @OneToMany(fetch=FetchType.LAZY, mappedBy="participanteModificado", cascade= {CascadeType.PERSIST, CascadeType.REMOVE})
    private List<HistoricoParticipanteProcesso> historicoParticipanteList;
    
    @PrePersist
    private void prePersist(){
        if (id == null) {
            Integer generatedId = CustomIdGenerator.create("sq_participante_processo").nextValue().intValue();
            setId(generatedId);
        }
        resolveCaminhoAbsoluto();
    	if (getNome() == null){
    		setNome(getPessoa().getNome());
    	}
    }

    private void resolveCaminhoAbsoluto() {
        if (getParticipantePai() == null){
            String caminho = String.format("P%09d", getId());
            setCaminhoAbsoluto(caminho);
        } else {
            String caminho = String.format("%s|P%09d", getParticipantePai().getCaminhoAbsoluto(), getId());
            setCaminhoAbsoluto(caminho);
        }
    }
    
    public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Processo getProcesso() {
        return processo;
    }

    public void setProcesso(Processo processo) {
        this.processo = processo;
    }

    public Pessoa getPessoa() {
        return pessoa;
    }

    public void setPessoa(Pessoa pessoa) {
        this.pessoa = pessoa;
    }
    
	public TipoParte getTipoParte() {
		return tipoParte;
	}

	public void setTipoParte(TipoParte tipoParte) {
		this.tipoParte = tipoParte;
	}

	public ParticipanteProcesso getParticipantePai() {
		return participantePai;
	}

	public void setParticipantePai(ParticipanteProcesso participantePai) {
		this.participantePai = participantePai;
	}
	
	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }
    
	public Date getDataInicio() {
		return dataInicio;
	}

	public void setDataInicio(Date dataInicio) {
		this.dataInicio = dataInicio;
	}

	public Date getDataFim() {
		return dataFim;
	}

	public void setDataFim(Date dataFim) {
		this.dataFim = dataFim;
	}
	
	public String getCaminhoAbsoluto() {
		return caminhoAbsoluto;
	}

	public void setCaminhoAbsoluto(String caminhoAbsoluto) {
		this.caminhoAbsoluto = caminhoAbsoluto;
	}

	public List<ParticipanteProcesso> getParticipantesFilhos() {
		return participantesFilhos;
	}

	public void setParticipantesFilhos(List<ParticipanteProcesso> participantesFilhos) {
		this.participantesFilhos = participantesFilhos;
	}
	
	@Override
	public String toString() {
		return nome;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ParticipanteProcesso))
			return false;
		ParticipanteProcesso other = (ParticipanteProcesso) obj;
		if (getId() == null) {
			if (other.getId() != null)
				return false;
		} else if (!getId().equals(other.getId()))
			return false;
		return true;
	}

    public List<HistoricoParticipanteProcesso> getHistoricoParticipanteList() {
        return historicoParticipanteList;
    }

    public void setHistoricoParticipanteList(
            List<HistoricoParticipanteProcesso> historicoParticipanteList) {
        this.historicoParticipanteList = historicoParticipanteList;
    }
    
    public ParticipanteProcesso makeCopy() throws CloneNotSupportedException {
    	ParticipanteProcesso clone = (ParticipanteProcesso) clone();
    	clone.setId(null);
    	clone.setProcesso(null);
    	clone.setParticipantePai(null);
    	clone.setParticipantesFilhos(null);
    	List<HistoricoParticipanteProcesso> cHistoricos = new ArrayList<>();
    	for (HistoricoParticipanteProcesso his : getHistoricoParticipanteList()) {
    		HistoricoParticipanteProcesso cHistorico = his.makeCopy();
    		cHistorico.setParticipanteModificado(clone);
    		cHistoricos.add(cHistorico);
    	}
    	clone.setHistoricoParticipanteList(cHistoricos);
    	return clone;
    }

    //Os relacionamentos com os participantes filhoes matém a referência para os filhos do participante clonado e devem ser refeitos de alguma forma para o clone
    public ParticipanteProcesso copiarParticipanteMantendoFilhos() throws CloneNotSupportedException {
		ParticipanteProcesso clone = (ParticipanteProcesso) clone();
		clone.setId(null);
		clone.setProcesso(null);
		clone.setParticipantePai(null);
		clone.setHistoricoParticipanteList(null);
		clone.setParticipantesFilhos(new ArrayList<>(clone.getParticipantesFilhos()));
		return clone;
	}
    
    public static final Comparator<ParticipanteProcesso> COMPARATOR_NOME = new Comparator<ParticipanteProcesso>() {
		@Override
		public int compare(ParticipanteProcesso o1, ParticipanteProcesso o2) {
			return o1.getNome().compareTo(o2.getNome());
		}
	};
	
}
