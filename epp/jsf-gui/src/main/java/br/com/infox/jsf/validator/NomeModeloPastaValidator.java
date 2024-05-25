package br.com.infox.jsf.validator;

import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import br.com.infox.epp.fluxo.entity.ModeloPasta;
import br.com.infox.epp.fluxo.manager.ModeloPastaManager;
import br.com.infox.seam.util.ComponentUtil;

@FacesValidator("nomeModeloPastaValidator")
public class NomeModeloPastaValidator implements Validator {

    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        String nome = null;
        Integer idFluxo = null;
        Integer idModeloPasta = null;
        Map<String, Object> attrs = component.getAttributes();

        nome = (String) attrs.get("submittedValue");
        idFluxo = (Integer) attrs.get("idFluxo");
        idModeloPasta = (Integer) attrs.get("idModeloPasta");

        if (idFluxo == null) {
            throw new ValidatorException(new FacesMessage("Erro ao validar nome da pasta"));
        }

        ModeloPastaManager modeloPastaManager = ComponentUtil.getComponent(ModeloPastaManager.NAME);
        List<ModeloPasta> modeloPastaList = modeloPastaManager.getByIdFluxo(idFluxo);
        for (ModeloPasta modeloPasta : modeloPastaList) {
            if (idModeloPasta != null && modeloPasta.getId().equals(idModeloPasta))
                continue;
            if (modeloPasta.getNome().equals(nome)) {
                throw new ValidatorException(new FacesMessage("Já existe modelo de pasta com este nome"));
            }
        }
    }
}