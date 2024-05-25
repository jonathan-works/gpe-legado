package br.com.infox.core.file.reader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.lowagie.text.DocumentException;
import com.lowagie.text.ExceptionConverter;
import com.lowagie.text.exceptions.BadPasswordException;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

import br.com.infox.log.LogProvider;
import br.com.infox.log.Logging;
import br.com.infox.seam.exception.BusinessRollbackException;

public class InfoxPdfReader {
    
    private static final LogProvider LOG = Logging.getLogProvider(InfoxPdfReader.class);

    public static boolean isCriptografado(byte[] pdf) {
    	try {
    		new PdfReader(pdf);    	
    	}
        catch(NoClassDefFoundError e) {
        	if(e.getMessage().equals("org/bouncycastle/asn1/DEREncodable")) {
        		return true;
        	}
        }
    	catch(Exception e) {
        	return false;    		
    	}
    	return false;
    }
    
    public static String readPdfFromInputStream(InputStream inputStream) {
        try {
            return readPdf(new PdfReader(inputStream));
        } catch (IOException e) {
            LOG.error("Não foi possível recuperar o conteúdo do pdf", e);
            return null;
        }
    }

    public static String readPdfFromByteArray(byte[] pdf) {
    	if(isCriptografado(pdf)) {
    		throw new BusinessRollbackException("Documento somente leitura, não é possível gravar");
    	}
        try {
            return readPdf(new PdfReader(pdf));
        }
        catch (IOException e) {
            LOG.error("Não foi possível recuperar o conteúdo do pdf", e);
            return null;
        }
    }
    
    private static String readPdf(PdfReader pdfReader) {
        StringBuilder sb = new StringBuilder();
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(pdfReader);
            int qtdPaginas = pdfReader.getNumberOfPages();
            for (int i = 1; i <= qtdPaginas; i++) {
                try {
                    String textFromPage = null;
                    try {
                        System.out.println("####### " + i);
                        sb.append(extractor.getTextFromPage(i));
                    } catch (NullPointerException e){}
                    //FIXME: PREVISTO DEVE ADICIONAR TEXTO VAZIO.
                    //Ticket aberto solicitando correção https://github.com/LibrePDF/OpenPDF/issues/34
                    sb.append(textFromPage == null ? null : textFromPage.replace("\u0000", ""));
                } catch (ExceptionConverter | Error e) {
                    LOG.error("Erro ao extrair texto da página " + i, e);
                    continue;
                }
            }
        } catch (IOException e) {
            LOG.error("Não foi possível recuperar o conteúdo do pdf", e);
        }
        try {
			PdfStamper pdfStamper = new PdfStamper(pdfReader, new ByteArrayOutputStream());
			pdfStamper.close();
		} catch (BadPasswordException e) {
			throw new BusinessRollbackException("Documento somente leitura, não é possível gravar");
		} catch (DocumentException | IOException e) {
			LOG.error("Não foi possível recuperar o conteúdo do pdf", e);
		} 
        return sb.toString();
    }
}
