package br.com.infox.epp.system.parametro;

import static br.com.infox.epp.Filter.equal;
import static br.com.infox.epp.Filter.isFalse;
import static br.com.infox.epp.Filter.isNull;
import static br.com.infox.epp.Filter.isTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.persistence.metamodel.SingularAttribute;

import br.com.infox.epp.FieldType;
import br.com.infox.epp.access.entity.Localizacao_;
import br.com.infox.epp.access.entity.Papel_;
import br.com.infox.epp.access.entity.UsuarioLogin_;
import br.com.infox.epp.access.type.UsuarioEnum;
import br.com.infox.epp.documento.entity.ModeloDocumento_;
import br.com.infox.epp.fluxo.entity.Fluxo_;
import br.com.infox.epp.fluxo.entity.ModeloPasta_;
import br.com.infox.epp.security.ControleAcessoEnum;
import br.com.infox.epp.system.Parametros;
import br.com.infox.epp.system.parametro.ParametroDefinition.Precedencia;

public class EppParametroProvider implements ParametroProvider {

    public static final String NOME_GRUPO_SISTEMA = "sistema";

    private List<ParametroDefinition<?>> parametroDefinitions;

    @PostConstruct
    public void init() {
        parametroDefinitions = new ArrayList<>();
        for (Parametros parametro : Parametros.values()) {
            if (parametro.getParametroDefinition() != null)
                parametroDefinitions.add(parametro.getParametroDefinition());
        }
        initParametrosControleAcesso();
        initParametrosComunicacao();
        initParametrosAnaliseDocumento();
        initParametrosExecFluxo();
        initParametrosSistema();
        initParametrosWSProcesso();
        create("consultaExterna", "ativaConsultaExternaPadrao", FieldType.BOOLEAN);
    }

    private void initParametrosWSProcesso() {
        final String grupo = "wsProcesso";
        create(grupo, "folhaRostoProcesso", ModeloDocumento_.tituloModeloDocumento, ModeloDocumento_.codigo);
        create(grupo, "folhaRostoMovimentacoes", ModeloDocumento_.tituloModeloDocumento, ModeloDocumento_.codigo);
    }

    private void initParametrosControleAcesso() {
        final String grupo = "controleAcesso";
        create(grupo, "usuarioExternoPodeVerDocExcluido", FieldType.BOOLEAN);
        create(grupo, "somenteUsuarioInternoVerMotivoExclusaoDoc", FieldType.BOOLEAN);
        create(grupo, "authorizationSecret", FieldType.STRING);
        create(grupo, "webserviceToken", FieldType.STRING);
        create(grupo, "externalAuthenticationServiceUrl", FieldType.STRING);
        create(grupo, "ldapDomainName", FieldType.STRING);
        create(grupo, "ldapProviderUrl", FieldType.STRING);
        create(grupo, "recaptchaPrivateKey", FieldType.STRING);
        create(grupo, "recaptchaPublicKey", FieldType.STRING);
        create(grupo, "usuarioInterno", Papel_.nome, Papel_.identificador);
        create(grupo, "usuarioExterno", Papel_.nome, Papel_.identificador);
    }

    private void initParametrosExecFluxo() {
        create("fluxo", "idUsuarioProcessoSistema", UsuarioLogin_.nomeUsuario, UsuarioLogin_.idUsuarioLogin)
                .addFilter(equal(UsuarioLogin_.tipoUsuario, UsuarioEnum.S)).addFilter(isTrue(UsuarioLogin_.ativo))
                .addFilter(isFalse(UsuarioLogin_.bloqueio));
        // Não deveria ser configurado por fluxo?
        create("fluxo", "pastaDocumentoGerado", ModeloPasta_.descricao, ModeloPasta_.nome);
    }

    private void initParametrosAnaliseDocumento() {
        create("analiseDocumento", "codigoFluxoDocumento", Fluxo_.fluxo, Fluxo_.codFluxo).addFilter(isTrue(Fluxo_.ativo))
                .addFilter(isTrue(Fluxo_.publicado));
    }

    private void initParametrosComunicacao() {
        create("comunicacao", "raizLocalizacoesComunicacao", Localizacao_.localizacao, Localizacao_.localizacao)
                .addFilter(isNull(Localizacao_.estruturaPai)).addFilter(isNull(Localizacao_.estruturaFilho)).addFilter(isTrue(Localizacao_.ativo));
        create("comunicacao", "codigoFluxoComunicacao", Fluxo_.fluxo, Fluxo_.codFluxo).addFilter(isTrue(Fluxo_.ativo))
                .addFilter(isTrue(Fluxo_.publicado));
        create("comunicacao", "codigoFluxoComunicacaoNaoEletronico", Fluxo_.fluxo, Fluxo_.codFluxo).addFilter(isTrue(Fluxo_.ativo))
                .addFilter(isTrue(Fluxo_.publicado));
        create("comunicacao", "codigoFluxoComunicacaoInterna", Fluxo_.fluxo, Fluxo_.codFluxo).addFilter(isTrue(Fluxo_.ativo))
                .addFilter(isTrue(Fluxo_.publicado));
        create("comunicacao", "codigoRaizResponsavelAssinaturaLocalizacao", Localizacao_.localizacao, Localizacao_.codigo)
                .addFilter(isTrue(Localizacao_.ativo)).addFilter(isNull(Localizacao_.estruturaPai));
    }

    private void initParametrosSistema() {
        create(NOME_GRUPO_SISTEMA, "nomeSistema", FieldType.STRING);
        create(NOME_GRUPO_SISTEMA, "emailSistema", FieldType.STRING);
        create(NOME_GRUPO_SISTEMA, "subNomeSistema", FieldType.STRING);
        create(NOME_GRUPO_SISTEMA, "exportarXLS", FieldType.BOOLEAN);
        create(NOME_GRUPO_SISTEMA, "exportarPDF", FieldType.BOOLEAN);
        create(NOME_GRUPO_SISTEMA, Parametros.PRODUCAO.getLabel(), FieldType.BOOLEAN);
        create(NOME_GRUPO_SISTEMA, Parametros.NAO_EXECUTAR_QUARTZ.getLabel(), FieldType.STRING);

        create(NOME_GRUPO_SISTEMA, "idUsuarioSistema", UsuarioLogin_.nomeUsuario, UsuarioLogin_.idUsuarioLogin).addFilter(isTrue(UsuarioLogin_.ativo))
                .addFilter(equal(UsuarioLogin_.tipoUsuario, UsuarioEnum.S)).addFilter(isFalse(UsuarioLogin_.bloqueio));
        create(NOME_GRUPO_SISTEMA, "tituloModeloEmailMudancaSenha", ModeloDocumento_.tituloModeloDocumento, ModeloDocumento_.tituloModeloDocumento)
                .addFilter(isTrue(ModeloDocumento_.ativo));
        create(NOME_GRUPO_SISTEMA, "tituloModeloEmailMudancaSenhaComLogin", ModeloDocumento_.tituloModeloDocumento,
                ModeloDocumento_.tituloModeloDocumento).addFilter(isTrue(ModeloDocumento_.ativo));
        create(NOME_GRUPO_SISTEMA, Parametros.CODIGO_UF_SISTEMA.getLabel(), FieldType.STRING);

        ParametroDefinition<Object> controleAcessoEnum = create(NOME_GRUPO_SISTEMA, "apiControleAcesso", FieldType.SELECT_ONE_ENUM);
        controleAcessoEnum.setEnumValues(ControleAcessoEnum.values());
    }

    public <T> ParametroDefinition<T> create(String grupo, String nome, SingularAttribute<T, ?> keyAttribute,
            SingularAttribute<T, ?> labelAttribute) {
        return create(grupo, nome, FieldType.SELECT_ONE, keyAttribute, labelAttribute);
    }

    public <T> ParametroDefinition<T> create(String grupo, String nome, FieldType tipo, SingularAttribute<T, ?> keyAttribute,
            SingularAttribute<T, ?> labelAttribute) {
        ParametroDefinition<T> parametroDefinition = new ParametroDefinition<T>(grupo, nome, tipo, keyAttribute, labelAttribute, Precedencia.DEFAULT);
        parametroDefinitions.add(parametroDefinition);
        return parametroDefinition;
    }

    public <T> ParametroDefinition<T> create(String grupo, String nome, FieldType tipo) {
        return create(grupo, nome, tipo, null, null);
    }

    @Override
    public List<ParametroDefinition<?>> getParametroDefinitions() {
        return Collections.unmodifiableList(parametroDefinitions);
    }

}
