package br.com.infox.epp.access;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;

import com.lowagie.text.DocumentException;

import br.com.infox.certificado.exception.CertificadoException;
import br.com.infox.core.exception.SystemExceptionFactory;
import br.com.infox.core.file.download.DocumentoServlet;
import br.com.infox.core.file.download.DocumentoServletOperation;
import br.com.infox.core.messages.InfoxMessages;
import br.com.infox.core.pdf.PdfManager;
import br.com.infox.epp.access.entity.UsuarioLogin;
import br.com.infox.epp.access.entity.UsuarioPerfil;
import br.com.infox.epp.access.manager.PapelManager;
import br.com.infox.epp.access.provider.TermoAdesaoVariableProducer;
import br.com.infox.epp.access.service.AuthenticatorService;
import br.com.infox.epp.assinador.AssinadorService;
import br.com.infox.epp.assinador.DadosAssinatura;
import br.com.infox.epp.documento.entity.ModeloDocumento;
import br.com.infox.epp.documento.manager.ModeloDocumentoManager;
import br.com.infox.epp.documento.type.ArbitraryExpressionResolver;
import br.com.infox.epp.documento.type.ExpressionResolverChain;
import br.com.infox.epp.documento.type.ExpressionResolverChain.ExpressionResolverChainBuilder;
import br.com.infox.epp.documento.type.SeamExpressionResolver;
import br.com.infox.epp.pessoa.entity.PessoaFisica;
import br.com.infox.epp.pessoa.manager.PessoaFisicaManager;
import br.com.infox.epp.pessoaFisica.PessoaFisicaSearch;
import br.com.infox.epp.processo.documento.assinatura.AssinaturaDocumentoService;
import br.com.infox.epp.processo.documento.assinatura.AssinaturaException;
import br.com.infox.epp.processo.documento.entity.DocumentoBin;
import br.com.infox.epp.processo.documento.manager.DocumentoBinManager;
import br.com.infox.epp.processo.documento.manager.DocumentoBinarioManager;
import br.com.infox.epp.system.Parametros;
import br.com.infox.epp.system.manager.ParametroManager;
import br.com.infox.epp.usuario.UsuarioLoginSearch;
import br.com.infox.jwt.JWT;
import br.com.infox.jwt.claims.InfoxPrivateClaims;
import br.com.infox.jwt.claims.JWTRegisteredClaims;
import br.com.infox.jwt.verifiers.Verifiers;
import br.com.infox.seam.exception.BusinessException;
import br.com.infox.security.rsa.RSAErrorCodes;
import br.com.infox.security.rsa.RSAUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

@Stateless
public class TermoAdesaoService {

    @Inject 
    private DocumentoBinManager documentoBinManager;
    @Inject 
    private DocumentoBinarioManager documentoBinarioManager;
    @Inject 
    private ModeloDocumentoManager modeloDocumentoManager;
    @Inject 
    private PessoaFisicaManager pessoaFisicaManager;
    @Inject 
    private ParametroManager parametroManager;
    @Inject 
    private PessoaFisicaSearch pessoaFisicaSearch;
    @Inject 
    private PdfManager pdfManager;
    @Inject 
    private UsuarioLoginSearch usuarioLoginSearch;
    @Inject 
    private AssinaturaDocumentoService assinaturaDocumentoService;
    @Inject 
    private TermoAdesaoVariableProducer termoAdesaoVariableProducer;
    @Inject 
    private InfoxMessages infoxMessages;
    @Inject 
    private PapelManager papelManager;
    @Inject
    private AssinadorService assinadorService;

    public String buildUrlDownload(String contextPath, String jwt, String uidTermoAdesao) {
        UriBuilder uriBuilder = UriBuilder.fromPath(contextPath);
        uriBuilder = uriBuilder.path(DocumentoServlet.BASE_SERVLET_PATH);
        uriBuilder = uriBuilder.path(uidTermoAdesao);
        uriBuilder = uriBuilder.path(DocumentoServletOperation.DOWNLOAD.getPath());
        if (jwt != null){
            uriBuilder = uriBuilder.queryParam("jwt", jwt);
        }
        return uriBuilder.build().toString();
    }

    public boolean isTermoAdesaoAssinado(String cpf) {
        return usuarioLoginSearch.getAssinouTermoAdesao(cpf);
    }
    
    public boolean isDeveAssinarTermoAdesao(PessoaFisica pessoaFisica){
        return papelManager.hasToSignTermoAdesao(pessoaFisica.getUsuarioLogin()) 
                && !usuarioLoginSearch.getAssinouTermoAdesao(pessoaFisica.getCpf());
    }

    public DocumentoBin createTermoAdesaoFor(PessoaFisica pessoaFisica) {
        if (pessoaFisica == null) {
            throw new BusinessException("termoAdesao.authorization.fail");
        }
        if (isTermoAdesaoAssinado(pessoaFisica.getCpf())) {
            return pessoaFisica.getTermoAdesao();
        }
        String tituloTermoAdesao = Parametros.TERMO_ADESAO.getValue();
        ModeloDocumento modeloDocumento = modeloDocumentoManager.getModeloDocumentoByTitulo(tituloTermoAdesao);
        Map<String, String> variables = termoAdesaoVariableProducer.getTermoAdesaoVariables(pessoaFisica);
        byte[] termoAdesao;
        try {
            termoAdesao = getTermoAdesaoByteArray(modeloDocumento, variables);
        } catch (DocumentException e) {
            throw new BusinessException("termoAdesao.conversion.fail");
        }
        return createTermoAdesao(pessoaFisica, tituloTermoAdesao, termoAdesao);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    private DocumentoBin createTermoAdesao(PessoaFisica pessoaFisica, String tituloTermoAdesao, byte[] termoAdesao) {
        pessoaFisica.setTermoAdesao(null);
        pessoaFisica = pessoaFisicaManager.update(pessoaFisica);
        removeIfExists(pessoaFisica.getTermoAdesao());
        DocumentoBin bin = documentoBinManager.createProcessoDocumentoBin(tituloTermoAdesao, termoAdesao, "pdf");
        bin = documentoBinManager.createProcessoDocumentoBin(bin);
        pessoaFisica.setTermoAdesao(bin);
        pessoaFisica = pessoaFisicaManager.update(pessoaFisica);
        return bin;
    }

    private void removeIfExists(DocumentoBin o) {
        if (o != null) {
            Integer id = o.getId();
            if (documentoBinarioManager.existeBinario(id)){
                documentoBinarioManager.remove(id);
            }
            documentoBinManager.remove(o);
        }
    }

    private byte[] getTermoAdesaoByteArray(ModeloDocumento modeloDocumento, Map<String, String> variables)
            throws DocumentException {
        String termoAdesao = modeloDocumentoManager.evaluateModeloDocumento(modeloDocumento,
                getExpressionResolver(variables));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        pdfManager.convertHtmlToPdf(termoAdesao, outputStream);
        return outputStream.toByteArray();
    }

    private ExpressionResolverChain getExpressionResolver(Map<String, String> variables) {
        return ExpressionResolverChainBuilder.with(new ArbitraryExpressionResolver(variables))
                .and(new SeamExpressionResolver()).build();
    }
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void assinarTermoAdesao(DadosAssinatura dadosAssinatura, PessoaFisica pessoaFisica){
        UsuarioLogin usuarioLogin = pessoaFisica.getUsuarioLogin();
        if (usuarioLogin == null){
            throw new BusinessException(AuthenticatorService.LOGIN_ERROR_SEM_PESSOA_FISICA);
        }
        try{
            assinaturaDocumentoService.verificaCertificadoUsuarioLogado(dadosAssinatura.getCertChainBase64(), usuarioLogin.getPessoaFisica());
            UsuarioPerfil perfil = papelManager.getPerfilTermoAdesao(usuarioLogin);
            if (perfil == null){
                perfil = usuarioLogin.getUsuarioPerfilList() == null || usuarioLogin.getUsuarioPerfilList().isEmpty() ? null : usuarioLogin.getUsuarioPerfilList().get(0);
            }
            if (perfil != null) {
                assinadorService.assinar(dadosAssinatura, perfil);
            } else {
                throw new BusinessException(AuthenticatorService.LOGIN_ERROR_USUARIO_SEM_PERFIL);
            }
        } catch (CertificadoException | AssinaturaException e) {
            throw new BusinessException(e.getMessage());
        }
    }
    
    public PessoaFisica retrievePessoaFisica(String jwt) {
        try {
            Map<String, Object> decodedPayload = JWT.parser().setKey(getSecret()).parse(jwt);
            
            Verifiers.allOf(infoxMessages.get("termoAdesao.exception.requiredClaims"),InfoxPrivateClaims.CPF, JWTRegisteredClaims.ISSUED_AT, JWTRegisteredClaims.EXPIRATION_DATE)
            .verify(decodedPayload);
            String cpf = (String) decodedPayload.get(InfoxPrivateClaims.CPF.getClaim());
            if (cpf != null) {
                return pessoaFisicaSearch.getByCpf(cpf);
            }
        } catch (ExpiredJwtException e){
            Integer expirationDate = (Integer) e.getClaims().get(JWTRegisteredClaims.EXPIRATION_DATE.getClaim());
            throw new BusinessException(String.format(infoxMessages.get("termoAdesao.exception.expiredJwt"), new Date(expirationDate*1000L)));
        } catch (JwtException|IllegalArgumentException e){
            throw new BusinessException(e.getMessage());
        }
        return null;
    }

    private byte[] getSecret() {
        String base64RsaKey = parametroManager.getValorParametro(Parametros.EPP_API_RSA_PUBLIC_KEY.getLabel());
        if (base64RsaKey == null || base64RsaKey.isEmpty()) {
            throw SystemExceptionFactory.create(RSAErrorCodes.INVALID_PRIVATE_KEY_STRUCTURE)
                    .set(Parametros.EPP_API_RSA_PUBLIC_KEY.getLabel(), base64RsaKey);
        }
        return RSAUtil.getPublicKeyFromBase64(base64RsaKey).getEncoded();
    }
}
