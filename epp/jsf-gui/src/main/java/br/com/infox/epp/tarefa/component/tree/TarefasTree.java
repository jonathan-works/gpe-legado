package br.com.infox.epp.tarefa.component.tree;

import br.com.infox.epp.cdi.ViewScoped;
import br.com.infox.epp.painel.FluxoBean;
import br.com.infox.epp.painel.TarefaBean;
import br.com.infox.epp.processo.consulta.list.ConsultaProcessoListagem;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Named
@ViewScoped
public class TarefasTree implements Serializable {

    @Getter
    private TreeNode raiz;

    @Getter
    private boolean naoPossuiRaiz = true;

    @Getter @Setter
    private TreeNode selectedNode;

    @Getter @Setter
    private String nomeFluxo;
    @Inject
    private ConsultaProcessoListagem consultaProcessoList;

    public void iniciar(List<TarefaBean> tasks, FluxoBean fluxo){
        raiz = new DefaultTreeNode("FLUXO", null);
        nomeFluxo = fluxo.getName();

        Map<Integer, List<TarefaBean>> collect = tasks.stream().collect(groupingBy(TarefaBean::getIdFluxo));

        collect.entrySet().stream().forEach(a ->{

            List<DefaultTreeNode> collect1 = a.getValue().stream().map(b -> new DefaultTreeNode(b)).collect(Collectors.toList());

            if(a.getKey() == Integer.valueOf(fluxo.getProcessDefinitionId())) {
                naoPossuiRaiz = false;
                raiz.getChildren().addAll(collect1);
            } else{

                Optional<String> first = a.getValue().stream().map(c -> c.getNomeFluxo()).findFirst();

                DefaultTreeNode subFluxo = new DefaultTreeNode("SUB-PROCESSO - " + first.get(), raiz);
                subFluxo.getChildren().addAll(collect1);

                raiz.getChildren().add(subFluxo);
            }

        });

        expandir(raiz);
    }

    private void expandir(TreeNode treeNode){
        treeNode.setExpanded(true);
        if(treeNode.getChildren() != null && !treeNode.getChildren().isEmpty()){
            treeNode.getChildren().forEach(node -> expandir(node));
        }
    }

    public void onNodeSelect(NodeSelectEvent ev) {

        if(ev.getTreeNode() != null && ev.getTreeNode().getData() != null && ev.getTreeNode().getData().toString().startsWith("SUB-PROCESSO - ") ){
            return;
        }

        if(ev.getTreeNode() != null && ev.getTreeNode().isSelectable() ){
            selectedNode = ev.getTreeNode();
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("taskSelecionado", selectedNode.getData());
        }else{
            selectedNode = null;
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().remove("taskSelecionado");
        }


        consultaProcessoList.onSelectNode((TarefaBean) selectedNode.getData());

    }

    public void onNodeUnselect(NodeSelectEvent ev) {



        //painelUsuarioController.onSelectNode();
    }

    public String getBackgroundColor(String node) {
        String codColor = "";

        if(node.startsWith("SUB-PROCESSO - ")){
            codColor = "#006600";
        }

        return codColor;
    }

    public String getTextColor(String node) {
        String codColor = "";

        if(node.startsWith("SUB-PROCESSO - ")){
            codColor = "#FFFFFF";
        }

        return codColor;
    }

}
