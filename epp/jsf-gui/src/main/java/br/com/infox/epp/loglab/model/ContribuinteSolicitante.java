package br.com.infox.epp.loglab.model;

import static javax.persistence.FetchType.LAZY;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.Persister;

import br.com.infox.core.persistence.SchemaSingleTableEntityPersister;
import br.com.infox.epp.loglab.contribuinte.type.ContribuinteEnum;
import br.com.infox.epp.municipio.Estado;
import br.com.infox.epp.pessoa.annotation.Cpf;
import br.com.infox.epp.pessoa.entity.PessoaFisica;
import br.com.infox.epp.pessoa.type.TipoGeneroEnum;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = ContribuinteSolicitante.TABLE_NAME)
@Persister(impl = SchemaSingleTableEntityPersister.class)
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@Getter
@Setter
public class ContribuinteSolicitante implements Serializable {

    public static final String TABLE_NAME = "tb_contribuinte";

    private static final long serialVersionUID = 1L;
    private static final String GENERATOR = "ContribuinteGenerator";

    @Id
    @SequenceGenerator(name = GENERATOR, sequenceName = "sq_contribuinte", allocationSize = 1, initialValue=1)
    @GeneratedValue(generator = GENERATOR, strategy = GenerationType.SEQUENCE)
    @Column(name = "id_contribuinte", nullable = false, unique = true)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "id_pessoa_fisica", nullable = false)
    @NotNull
    private PessoaFisica pessoaFisica;

    @Enumerated(EnumType.STRING)
    @Column(name = "tp_contribuinte", nullable = false)
    private ContribuinteEnum tipoContribuinte;

    @NotNull
    @Cpf
    @Column(name = "nr_cpf", nullable = false)
    private String cpf;

    @Column(name = "nr_matricula", nullable = true)
    private String matricula;

    @NotNull
    @Size(min = 6, max = 256)
    @Column(name = "nm_contribuinte", nullable = false)
    private String nomeCompleto;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tp_sexo", nullable = false)
    private TipoGeneroEnum sexo;

    @NotNull
    @Column(name = "dt_nascimento", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date dataNascimento;

    @Size(min = 3, max = 20)
    @Column(name = "nr_rg", nullable = true)
    private String numeroRg;

    @Size(min = 3, max = 256)
    @Column(name = "ds_emissor_rg", nullable = true)
    private String emissorRg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_rg", nullable = true)
    private Estado estadoRg;

    @NotNull
    @Size(min = 6, max = 256)
    @Column(name = "nm_mae_contribuinte", nullable = false)
    private String nomeMae;

    @NotNull
    @Column(name = "ds_email", nullable = false)
    private String email;

    @Column(name = "nr_telefone_celular", nullable = true)
    private String telefone;

    @Column(name = "ds_cidade", nullable = true)
    private String cidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado", nullable = true)
    private Estado estado;

    @Column(name = "ds_logradouro", nullable = true)
    private String logradouro;

    @Column(name = "ds_bairro", nullable = true)
    private String bairro;

    @Column(name = "ds_complemento", nullable = true)
    private String complemento;

    @Column(name = "nr_residencia", nullable = true)
    private String numero;

    @Column(name = "nr_cep", nullable = true)
    private String cep;

    @Override
    public String toString() {
        return getCpf() + " - " + getNomeCompleto();
    }

}
