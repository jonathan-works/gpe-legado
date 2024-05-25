package br.com.infox.epp.assinador.view.jsf;

import static br.com.infox.core.util.ObjectUtil.is;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesEvent;
import javax.faces.event.PhaseId;
import javax.faces.render.FacesRenderer;
import javax.faces.render.Renderer;
import javax.ws.rs.core.UriBuilder;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import br.com.infox.assinador.rest.api.StatusToken;
import br.com.infox.componentes.util.ComponentUtil;
import br.com.infox.core.messages.InfoxMessages;
import br.com.infox.epp.assinador.assinavel.AssinavelSource;
import br.com.infox.epp.assinador.view.AssinadorController;
import br.com.infox.epp.cdi.util.Beans;
import br.com.infox.epp.documento.type.TipoMeioAssinaturaEnum;
import br.com.infox.jsf.AjaxRequestBuilderFactory;

@FacesRenderer(componentFamily = Assinador.COMPONENT_FAMILY, rendererType = Assinador.RENDERER_TYPE)
public class AssinadorRenderer extends Renderer {

    @Override
    public void decode(FacesContext context, UIComponent component) {
        super.decode(context, component);
        Assinador button = (Assinador) component;
        if (button.isDisabled()) {
            return;
        }

        decodeBehaviors(context, component);
//        String param = component.getClientId(context);
        //        if (context.getExternalContext().getRequestParameterMap().containsKey(param)) {
        //            component.queueEvent(new ActionEvent(component));
        //        }
        String behaviorEvent = context.getExternalContext().getRequestParameterMap().get("javax.faces.behavior.event");
//        String jsfEvent = context.getExternalContext().getRequestParameterMap().get("javax.faces.partial.event");
        if (behaviorEvent != null) {
            String behaviorSource = context.getExternalContext().getRequestParameterMap().get("javax.faces.source");
            String clientId = component.getClientId(context);
            if (behaviorSource != null && behaviorSource.equals(clientId)) {
                ActionEvent actionEvent = new ActionEvent(component);
                FacesEvent signEvent = null;
                switch (behaviorEvent) {
                case "sign":
                    signEvent = new AssinadorSignEvent(component, button.getToken());
                    break;
                case "updatestatus":
                    signEvent = new AssinadorUpdateEvent(component);
                    break;
                case "click":
                    signEvent = new AssinadorClickEvent(component);
                    break;
                case "assinaturaEletronicaClick":{
                    final ExternalContext externalContext = context.getExternalContext();
                    Map<String, String> requestParameterMap = externalContext.getRequestParameterMap();
                    signEvent = new AssinadorEletronicoClickEvent(component, requestParameterMap.getOrDefault("params", ""));
                    break;
                }
                default:
                    break;
                }
                if (signEvent != null) {
                    if (button.isImmediate()) {
                        signEvent.setPhaseId(PhaseId.APPLY_REQUEST_VALUES);
                        actionEvent.setPhaseId(PhaseId.APPLY_REQUEST_VALUES);
                    } else {
                        signEvent.setPhaseId(PhaseId.INVOKE_APPLICATION);
                        actionEvent.setPhaseId(PhaseId.INVOKE_APPLICATION);
                    }
                    signEvent.queue();
                    actionEvent.queue();
                }
            }
        }
    }

    protected void decodeBehaviors(FacesContext context, UIComponent component) {
        ClientBehaviorHolder component2 = (ClientBehaviorHolder) component;
        Map<String, List<ClientBehavior>> behaviors = component2.getClientBehaviors();
        if (behaviors.isEmpty()) {
            return;
        }

        String behaviorEvent = context.getExternalContext().getRequestParameterMap().get("javax.faces.behavior.event");
        if (behaviorEvent != null) {
            List<ClientBehavior> behaviorsForEvent = behaviors.get(behaviorEvent);
            if (!behaviorsForEvent.isEmpty()) {
                String behaviorSource = context.getExternalContext().getRequestParameterMap().get("javax.faces.source");
                String clientId = component.getClientId(context);
                if (behaviorSource != null && clientId.equals(behaviorSource)) {
                    for (ClientBehavior behavior : behaviorsForEvent) {
                        behavior.decode(context, component);
                    }
                }
            }
        }
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        Assinador button = (Assinador) component;

        encodeMarkup(context, button);
        encodeScript(context, button);
    }

    private String createSignBehavior(final FacesContext context, final Assinador button){
        ComponentUtil componentUtil = new ComponentUtil();
        String render = button.getRender();
        if (render != null && !render.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            String[] ids;
            if (render.contains(",")) {
                ids = render.split(",");
            } else {
                ids = render.split(" ");
            }
            for (String id : ids) {
                id = id.trim();
                UIComponent c = componentUtil.findComponentFor(button, id);
                if (c != null) {
                    sb.append(" ").append(c.getClientId()).append(" ");
                }
            }
            render = sb.toString().trim();
        }
        String execute = button.getExecute();
        if (execute != null && !execute.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            String[] ids;
            if (execute.contains(",")) {
                ids = execute.split(",");
            } else {
                ids = execute.split(" ");
            }
            for (String id : ids) {
                id = id.trim();
                UIComponent c = componentUtil.findComponentFor(button, id);
                if (c != null) {
                    sb.append(" ").append(c.getClientId()).append(" ");
                }
            }
            execute = sb.toString().trim();
        }
        return AjaxRequestBuilderFactory.create(context).from(button).execute(execute).render(render)
                .oncomplete(button.getOncomplete())
                .behavior("sign").build();
    }

    private String createCompletedBehavior(final FacesContext context, final Assinador button) {
        return AjaxRequestBuilderFactory.create(context).from(button).execute("").render("")
        .oncomplete(button.getOncomplete())
        .behavior("signaturecomplete")
        .preventDefault().build();
    }

    private String createUpdateStatusBehavior(FacesContext context, final Assinador button) {
        return AjaxRequestBuilderFactory.create(context).from(button).execute("@this").render("@this")
        .behavior("updatestatus").build();
    }

    private <T> T jndi(Class<T> type, Annotation... annotations){
        return Beans.getReference(type, annotations);
    }

    protected void encodeScript(FacesContext context, Assinador button) throws IOException {
        if (SignPhase.BEFORE_CLICK.equals( button.getCurrentPhase()))
            return;
        ResponseWriter writer = context.getResponseWriter();
        writer.startElement("script", null);
        writer.writeAttribute("id", button.getClientId() + "_s", null);
        writer.writeAttribute("type", "text/javascript", null);
        writer.write("(function(){");
        if (SignPhase.AFTER_CLICK.equals(button.getCurrentPhase())) {
            writer.write(createJnlpDownloadStatement(button));
        }
        StatusToken status = button.getStatus();
        switch (button.getCurrentPhase()) {
        case AFTER_CLICK:
        case WAITING_SIGNATURE:
            if (is(status).in(StatusToken.AGUARDANDO_ASSINATURA, null)) {
                createTimeoutFunction(context, createUpdateStatusBehavior(context, button), button.getTimeout());
            } else {
                createTimeoutFunction(context, createSignBehavior(context, button), 1);
            }
            break;
        default:
            break;
        }
        writer.write("})();");
        writer.endElement("script");
    }

    private String createJnlpDownloadStatement(Assinador button) {
        return String.format("location.href = \"%s\";", UriBuilder.fromPath(jndi(AssinadorController.class).getJNLPUrl()).queryParam("token", button.getToken()).build());
    }

    private void createTimeoutFunction(FacesContext context, String innerFunction, Number timeout) throws IOException {
        ResponseWriter writer = context.getResponseWriter();

        writer.write(" setTimeout(function() { ");
        writer.write(innerFunction);
        writer.write(" }, " + timeout + " );");
    }

    protected void encodeMarkup(FacesContext context, Assinador button) throws IOException {

        ResponseWriter writer = context.getResponseWriter();
        String clientId = button.getClientId(context);

        writer.startElement("div", button);
        writer.writeAttribute("id", clientId, "id");
        if(podeExibirAssinadorToken(button)) {
            createAssinadorToken(context, button);
        }

        if(podeExibirAssinadorEletronico(button)) {
            createAssinadorEletronico(context, button);
        }
        writer.endElement("div");
    }

    private void createAssinadorToken(FacesContext context, Assinador button) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        String clientId = button.getClientId(context);

        Object value = button.getValue();
        boolean disabled = button.isDisabled() || !SignPhase.BEFORE_CLICK.equals(button.getCurrentPhase());
        StringBuilder styleClass = new StringBuilder("buttons");


        if (disabled){
            styleClass.append(" is-disabled ");
        }
        if (is(button.getStyleClass()).notEmpty())
            styleClass.append(" ").append(button.getStyleClass()).append(" ");

        writer.startElement("button", button);
        writer.writeAttribute("id", clientId.concat("_token"), "id");
        writer.writeAttribute("name", clientId.concat("_token"), "name");
        writer.writeAttribute("type", "button", "type");
        writer.writeAttribute("class", styleClass.toString(), "styleClass");
        if (!disabled){
            String onclick=AjaxRequestBuilderFactory.create(context).from(button).behavior("click").execute(button.getExecute()).render("@this")
            .preventDefault().onbegin(button.getOnbegin()).build();
            if (onclick != null) {
                writer.writeAttribute("onclick", onclick, null);
            }
        } else {
            writer.writeAttribute("disabled", "disabled", "disabled");
        }

        //text
        writer.startElement("span", null);
        //        writer.writeAttribute("class", HTML.BUTTON_TEXT_CLASS, null);

        if (value == null) {
            writer.write(InfoxMessages.getInstance().get("assinaturas.assinar"));
        } else {
            writer.writeText(value, "value");
        }

        writer.endElement("span");

        writer.endElement("button");
    }

    private void createAssinadorEletronico(FacesContext context, Assinador button) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        String clientId = button.getClientId(context);

        boolean disabled = button.isDisabled() || !SignPhase.BEFORE_CLICK.equals(button.getCurrentPhase());
        StringBuilder styleClass = new StringBuilder("buttons");

        writer.startElement("button", button);
        writer.writeAttribute("id", clientId.concat("_assinaturaEletronica"), "id");
        writer.writeAttribute("name", clientId.concat("_assinaturaEletronica"), "name");
        writer.writeAttribute("type", "button", "type");
        writer.writeAttribute("class", styleClass.toString(), "styleClass");
        if (!disabled){

            ComponentUtil componentUtil = new ComponentUtil();
            String render = button.getRender();
            if (render != null && !render.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                String[] ids;
                if (render.contains(",")) {
                    ids = render.split(",");
                } else {
                    ids = render.split(" ");
                }
                for (String id : ids) {
                    id = id.trim();
                    UIComponent c = componentUtil.findComponentFor(button, id);
                    if (c != null) {
                        sb.append(" ").append(c.getClientId()).append(" ");
                    }
                }
                render = sb.toString().trim();
            }
            String execute = button.getExecute();
            if (execute != null && !execute.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                String[] ids;
                if (execute.contains(",")) {
                    ids = execute.split(",");
                } else {
                    ids = execute.split(" ");
                }
                for (String id : ids) {
                    id = id.trim();
                    UIComponent c = componentUtil.findComponentFor(button, id);
                    if (c != null) {
                        sb.append(" ").append(c.getClientId()).append(" ");
                    }
                }
                execute = sb.toString().trim();
            }

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("mensagem", button.getTextoConfirmacaoAssinaturaEletronica());
            jsonObject.addProperty("labelConfirmBtn", "Confirmar");
            jsonObject.addProperty("labelCancelBtn", "Cancelar");
            jsonObject.addProperty("source", button.getClientId(context));
            jsonObject.addProperty("execute", execute);
            jsonObject.addProperty("render", render);
            jsonObject.addProperty("onbegin", button.getOnbegin());
            jsonObject.addProperty("oncomplete", button.getOncomplete());
            String jsonArgs = new Gson().toJson(jsonObject);

            StringBuilder assinarEltronicamentFunc = new StringBuilder();
            assinarEltronicamentFunc.append("invoke(['infox.Assinador'], function(assinador) {");
            assinarEltronicamentFunc.append("assinador.assinarEletronicamente(");
            assinarEltronicamentFunc.append("JSON.parse('").append(jsonArgs).append("')");
            assinarEltronicamentFunc.append(");");
            assinarEltronicamentFunc.append("});");
            assinarEltronicamentFunc.append("event.preventDefault();return false;");
            String onclick = assinarEltronicamentFunc.toString();
            writer.writeAttribute("onclick", onclick, null);
        } else {
            writer.writeAttribute("disabled", "disabled", "disabled");
        }

        //text
        writer.startElement("span", null);

        writer.writeText(button.getLabelAssinaturaEletronica(), "value");

        writer.endElement("span");

        writer.endElement("button");
    }

    private boolean podeExibirAssinadorToken(Assinador button) {
        return button.getAssinavelProvider() != null && button.getAssinavelProvider().getAssinaveis().stream().map(AssinavelSource::getTipoMeioAssinatura)
                .anyMatch(tipo -> tipo == null || TipoMeioAssinaturaEnum.T.equals(tipo));
    }

    private boolean podeExibirAssinadorEletronico(Assinador button) {
        return button.getRenderedAssinaturaEletronica()
                && button.getAssinavelProvider() != null && button.getAssinavelProvider().getAssinaveis().stream().map(AssinavelSource::getTipoMeioAssinatura)
                        .anyMatch(tipo -> tipo == null || TipoMeioAssinaturaEnum.E.equals(tipo));
    }
}
