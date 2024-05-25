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

@FacesValidator("ordemModeloPastaValidator")
public class OrdemModeloPastaValidator implements Validator {

    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        Integer ordem = null;
        Integer idFluxo = null;
        Integer idModeloPasta = null;
        Map<String, Object> attrs = component.getAttributes();

        ordem = Integer.parseInt((String) attrs.get("submittedValue"));
        idFluxo = (Integer) attrs.get("idFluxo");
        idModeloPasta = (Integer) attrs.get("idModeloPasta");

        if (idFluxo == null) {
            throw new ValidatorException(new FacesMessage("Erro ao validar número de ordem"));
        }

        ModeloPastaManager modeloPastaManager = ComponentUtil.getComponent(ModeloPastaManager.NAME);
        List<ModeloPasta> modeloPastaList = modeloPastaManager.getByIdFluxo(idFluxo);
        for (ModeloPasta modeloPasta : modeloPastaList) {
            if (idModeloPasta != null && modeloPasta.getId().equals(idModeloPasta))
                continue;
            if (modeloPasta.getOrdem() != null && modeloPasta.getOrdem().equals(ordem)) {
                throw new ValidatorException(new FacesMessage("Já existe Modelo de Pasta com este número de ordem"));
            }
        }
    }
}