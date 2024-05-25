package br.com.infox.core.file.download;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;

import javax.ejb.Stateless;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.mail.internet.MimeUtility;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.faces.FacesMessages;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfReader;

import br.com.infox.core.messages.InfoxMessages;
import br.com.infox.core.pdf.PdfManager;
import br.com.infox.core.util.CollectionUtil;
import br.com.infox.core.util.StringUtil;
import br.com.infox.epp.cdi.util.Beans;
import br.com.infox.epp.processo.documento.dao.DocumentoDAO;
import br.com.infox.epp.processo.documento.download.Carimbo;
import br.com.infox.epp.processo.documento.entity.Documento;
import br.com.infox.epp.processo.documento.entity.DocumentoBin;
import br.com.infox.epp.processo.documento.entity.DocumentoTemporario;
import br.com.infox.epp.processo.documento.manager.DocumentoBinManager;
import br.com.infox.epp.processo.documento.manager.DocumentoBinarioManager;
import br.com.infox.epp.processo.documento.manager.DocumentoManager;
import br.com.infox.epp.processo.entity.Processo;
import br.com.infox.log.LogProvider;
import br.com.infox.log.Logging;
import br.com.infox.seam.path.PathResolver;

@Stateless
@BypassInterceptors
@Name(FileDownloader.NAME)
@Scope(ScopeType.STATELESS)
public class FileDownloader implements Serializable {

    private static final String EXTENSAO_PADRAO = "pdf";

    private static final long serialVersionUID = 1L;
    public static final String NAME = "fileDownloader";
    private static final LogProvider LOG = Logging.getLogProvider(FileDownloader.class);

    @Inject
    private DocumentoBinarioManager documentoBinarioManager;
    @Inject
    private PdfManager pdfManager;
    @Inject
    private InfoxMessages infoxMessages;
    @Inject
    private DocumentoBinManager documentoBinManager;
    @Inject
    private DocumentoManager documentoManager;
    @Inject
    private PathResolver pathResolver;
    @Inject
    private DocumentoDAO documentoDAO;

    public static void download(DownloadResource downloadResource){
        if (downloadResource == null)
            return;
        HttpServletResponse response;
        FacesContext facesContext = FacesContext.getCurrentInstance();
        response = facesContext != null ? (HttpServletResponse) facesContext.getExternalContext().getResponse() : Beans.getReference(HttpServletResponse.class);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentLength((int) downloadResource.getDataLength());
        response.setContentType(downloadResource.getContentType());
        if ("application/pdf".equals(downloadResource.getContentType())){
            response.setHeader("Content-disposition", String.format("attachment; filename=\"%s\"", downloadResource.getFileName()));
        }
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        if(externalContext.isSecure()) {
            externalContext.setResponseHeader("Cache-Control", "public");
            externalContext.setResponseHeader("Pragma", "public");
        }
        try {
            IOUtils.copy(downloadResource.getInputStream(), response.getOutputStream());
            response.getOutputStream().flush();
            if (facesContext != null){
                facesContext.responseComplete();
            }
            downloadResource.delete();
        } catch (IOException e) {
            LOG.error(".download()", e);
            FacesMessages.instance().add(String.format("Erro ao descarregar o arquivo: %s", downloadResource.getFileName()));
        }
    }

    public static void download(byte[] data, String contentType, String fileName) {
        if (data == null) {
            return;
        }
        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletResponse response = prepareDownloadResponse(contentType, fileName);
        response.setContentLength(data.length);
        try {
            OutputStream out = response.getOutputStream();
            out.write(data);
            out.flush();
            facesContext.responseComplete();
        } catch (IOException ex) {
            LOG.error(".download()", ex);
            FacesMessages.instance().add("Erro ao descarregar o arquivo: "
                    + fileName);
        }
    }

    public static HttpServletResponse prepareDownloadResponse(String contentType, String fileName) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
        response.setContentType(contentType);
        if (!contentType.equals("text/html") && !contentType.equals("application/pdf")) {
            response.setHeader("Content-disposition", "attachment; filename=\""
                    + fileName + "\"");
        }
        return response;
    }

    public boolean isPdf(DocumentoTemporario documento){
        return isPdf(documento.getDocumentoBin());
    }

    public boolean isPdf(Documento documento){
        return isPdf(documento.getDocumentoBin());
    }

    public boolean isPdf(DocumentoBin documentoBin){
        String extensao = getExtensao(documentoBin);
        return "pdf".equalsIgnoreCase(extensao);
    }

    public String getContentType(DocumentoTemporario documento){
        return getContentType(documento.getDocumentoBin());
    }

    public String getContentType(Documento documento){
        return getContentType(documento.getDocumentoBin());
    }

    private String getExtensao(DocumentoBin documentoBin) {
        return StringUtils.isEmpty(documentoBin.getExtensao()) ? EXTENSAO_PADRAO : documentoBin.getExtensao();
    }

    public String getContentType(DocumentoBin documentoBin){
        return String.format("application/%s", getExtensao(documentoBin));
    }

    public void downloadDocumento(DocumentoBin documentoBin, boolean gerarMargens) throws IOException {
        if (documentoBin == null)
            return;
        downloadDocumento(getData(documentoBin, gerarMargens), getContentType(documentoBin), extractNomeArquivo(documentoBin));
    }

    public void downloadDocumentoOriginal(DocumentoBin documentoBin) throws IOException {
        downloadDocumento(documentoBin, false);
    }

    public void downloadDocumento(DocumentoBin documentoBin) throws IOException {
        downloadDocumento(documentoBin, true);
    }

    public void downloadDocumento(Documento documento) throws IOException {
        if (documento == null)
            return;
        downloadDocumento(documento.getDocumentoBin());
    }

    public static interface Exporter {
        public void export(OutputStream outputStream) throws IOException;
    }

    public static class ByteArrayExporter implements Exporter {

        private byte[] data;

        public ByteArrayExporter(byte[] data) {
            this.data = data;
        }

        @Override
        public void export(OutputStream outputStream) throws IOException {
            outputStream.write(data);
        }

    }

    public void downloadDocumento(Exporter exporter, String contentType, String fileName) throws IOException{
        HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
        response.setContentType(contentType);
        response.addHeader("Content-disposition", "filename=\"" + MimeUtility.encodeWord(fileName ) + "\"");
        exporter.export(response.getOutputStream());
        response.getOutputStream().flush();
        FacesContext.getCurrentInstance().responseComplete();
    }

    public void downloadDocumento(byte[] data, String contentType, String fileName) throws IOException{
//    	Descomentar código que inserir o Title do documento, possibilitando que o nome do arquivo apareça no cabeçalho do pdfViewer.
        if(contentType.contains("pdf")){
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            if(putTitleOfPDF(data,fileName,outputStream))
                data = outputStream.toByteArray();
        }
        HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
        response.setContentLength(data.length);
        downloadDocumento(new ByteArrayExporter(data), contentType, fileName);
    }

    /**
     * Método que insere o title de um pdf necessário para o PDFViewer do Chrome.
     * @param data
     * @param fileName
     * @param bos
     * @return
     */
    private boolean putTitleOfPDF(byte[] data, String fileName, ByteArrayOutputStream bos ) {
        Document document = new Document();
        try {
            PdfReader pdfReader = new PdfReader(data);
            PdfCopy copy = null;
            copy = new PdfCopy(document, bos);
            document.open();

            for (int i = 1; i <= pdfReader.getNumberOfPages(); i++) {
                copy.addPage(copy.getImportedPage(pdfReader, i));
            }
            copy.freeReader(pdfReader);
            pdfReader.close();
            document.addTitle(fileName.substring(0,fileName.lastIndexOf('.')));
        }catch (Exception ex) {
            return false;
        }finally {
            if(document.isOpen())
                document.close();
        }
        return true;
    }

    public String getMensagemDocumentoNulo() {
        return infoxMessages.get("documentoProcesso.error.noFileOrDeleted");
    }

    public String extractNomeArquivo(DocumentoBin documento) {
        String nomeArquivo=documento.getNomeArquivo();
        String extensao = documento.getExtensao();
        if (StringUtil.isEmpty(extensao)){
            documento.setExtensao(EXTENSAO_PADRAO);
        }
        if (StringUtil.isEmpty(nomeArquivo)){
            nomeArquivo=documento.getUuid().toString();
        }
        if (!nomeArquivo.endsWith("."+extensao))
            nomeArquivo = nomeArquivo+"."+documento.getExtensao();
        return nomeArquivo;
    }

    public byte[] getData(DocumentoBin documento) {
        return getData(documento, true);
    }

    public byte[] getData(DocumentoBin documento, boolean gerarMargens) {
        try(ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            export(documento, stream);
            return stream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao exportar", e);
        }
    }

    public byte[] getOriginalData(DocumentoBin documento) {
        if (documentoBinarioManager.existeBinario(documento.getId())) {
            byte[] data = documentoBinarioManager.getData(documento.getId());
            documentoBinarioManager.detach(documento.getId());
            return data;
        } else {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                String modeloDocumento = documento.isBinario() ? getMensagemDocumentoNulo()
                        : defaultIfNull(documento.getModeloDocumento(), getMensagemDocumentoNulo());
                pdfManager.convertHtmlToPdf(modeloDocumento, outputStream);
                documento.setExtensao("pdf");
            } catch (DocumentException e) {
            }
            return outputStream.toByteArray();
        }
    }

    public void export(DocumentoBin documento, OutputStream outputStream) {
        export(documento, outputStream, true);
    }

    public void export(DocumentoBin documento, OutputStream outputStream, boolean gerarMargens) {
        List<Documento> listaDocumento = documentoDAO.getDocumentosFromDocumentoBin(documento);
        boolean documentoCancelado = CollectionUtil.isEmpty(listaDocumento)? false : listaDocumento.get(0).getExcluido();
        List<Carimbo> carimbos = documentoBinManager.gerarCarimbos(documento, gerarMargens, documentoCancelado);
        boolean documentoSemCarimbos =  (CollectionUtil.isEmpty(carimbos));
        byte[] originalData = getOriginalData(documento);
        if (documentoSemCarimbos) {
            try {
                outputStream.write(originalData);
            } catch(IOException e) {
                throw new RuntimeException("Erro ao gravar no stream", e);
            }
        } else {
            documentoBinManager.writeCarimbos(originalData, outputStream, carimbos);
        }
    }

    public HttpServletResponse getResponse() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null){
            return (HttpServletResponse) facesContext.getExternalContext().getResponse();
        }
        return Beans.getReference(HttpServletResponse.class);
    }

    public HttpServletRequest getRequest() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null){
            return (HttpServletRequest) facesContext.getExternalContext().getRequest();
        }
        return Beans.getReference(HttpServletRequest.class);
    }

    public void download(DocumentoBin documentoBin) {
        byte[] data = getData(documentoBin);
        download(data, "application/" + getExtensao(documentoBin), documentoBin.getNomeArquivo());
    }

    public String getWindowOpen(Boolean isPdf) {
        return isPdf
                ? "infox.openPopUp('_blank', '" + pathResolver.getContextPath() + "/downloadDocumento.seam');"
                : "infox.openPopUp('_self', '" + pathResolver.getContextPath() + "/downloadDocumento.seam');";
    }

    public String getWindowOpen(DocumentoBin documentoBin) {
        return getWindowOpen(isPdf(documentoBin));
    }

    public String getWindowOpen(Processo processo) {
        return getWindowOpen(true);
    }

    public String getWindowOpenByIdDocumento(Integer idDocumento) {
        Documento documento = documentoManager.find(idDocumento);
        return documento == null ? "" : getWindowOpen(documento.getDocumentoBin());
    }

    public String getWindowOpenByIdDocumentoBin(Integer idDocumentoBin) {
        DocumentoBin documentoBin = documentoBinManager.find(idDocumentoBin);
        return documentoBin == null ? "" : getWindowOpen(documentoBin);
    }
}
