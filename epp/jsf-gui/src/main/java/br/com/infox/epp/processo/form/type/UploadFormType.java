package br.com.infox.epp.processo.form.type;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.richfaces.event.FileUploadEvent;
import org.richfaces.model.UploadedFile;

import br.com.infox.core.action.ActionMessagesService;
import br.com.infox.core.messages.InfoxMessages;
import br.com.infox.core.persistence.DAOException;
import br.com.infox.epp.cdi.util.Beans;
import br.com.infox.epp.documento.entity.ClassificacaoDocumento;
import br.com.infox.epp.processo.documento.entity.Documento;
import br.com.infox.epp.processo.documento.service.DocumentoUploaderService;
import br.com.infox.epp.processo.form.FormData;
import br.com.infox.epp.processo.form.FormField;
import br.com.infox.epp.processo.form.variable.value.TypedValue;
import br.com.infox.epp.processo.form.variable.value.ValueType;
import br.com.infox.ibpm.variable.file.FileVariableHandler;
import br.com.infox.seam.exception.BusinessException;
import br.com.infox.seam.exception.BusinessRollbackException;

public class UploadFormType extends FileFormType {
    
    private static final Logger LOG = Logger.getLogger(UploadFormType.class.getName());
    
    public UploadFormType() {
        super("upload", "/Processo/form/upload.xhtml");
    }

    @Override
    public Object convertToFormValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            value = Integer.valueOf((String) value);
        }
        if (value instanceof Integer) {
            Documento documento = getDocumentoManager().find((Integer) value);
            return documento;
        }
        return null;
    }
    
    @Override
    public boolean isInvalid(FormField formField, FormData formData) throws BusinessException {
        if(formField.isRequired() && formField.getValue() == null){
            FacesContext.getCurrentInstance().addMessage(formField.getComponent().getClientId(), new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, "", InfoxMessages.getInstance().get("beanValidation.notNull")));
            return true;
        }
        return super.isInvalid(formField, formData);
    }
    
    public void processFileUpload(FileUploadEvent fileUploadEvent) {
        UploadedFile file = fileUploadEvent.getUploadedFile();
        UIComponent uploadFile = fileUploadEvent.getComponent();
        FormField formField = (FormField) uploadFile.getAttributes().get("formField");
        FormData formData = (FormData) uploadFile.getAttributes().get("formData");
        ClassificacaoDocumento classificacao = formField.getProperty("classificacaoDocumento", ClassificacaoDocumento.class);
        try {
            getDocumentoUploadService().validaDocumento(file, classificacao, file.getData());
            getFileVariableHandler().gravarDocumento(file, uploadFile.getId(), formField, formData.getProcesso());
            formData.setSingleVariable(formField, new TypedValue(formField.getValue(), ValueType.FILE));
        } catch (BusinessRollbackException e) {
             LOG.log(Level.SEVERE, "Erro ao remover o documento existente", e);
             if (e.getCause() instanceof DAOException) {
                 getActionMessagesService().handleDAOException((DAOException) e.getCause());
             } else {
                 getActionMessagesService().handleException("Erro ao substituir o documento." + e.getMessage(), e);
             }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "", e);
            getActionMessagesService().handleGenericException(e, "Registro alterado por outro usuário, tente novamente");
        }
    }
    
    protected DocumentoUploaderService getDocumentoUploadService() {
        return Beans.getReference(DocumentoUploaderService.class);
    }
    
    protected ActionMessagesService getActionMessagesService() {
        return Beans.getReference(ActionMessagesService.class);
    }
    
    protected FileVariableHandler getFileVariableHandler() {
        return Beans.getReference(FileVariableHandler.class);
    }
}
