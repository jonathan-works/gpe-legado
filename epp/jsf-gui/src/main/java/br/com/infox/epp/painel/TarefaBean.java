package br.com.infox.epp.painel;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TarefaBean {

    private int qtd;
    private String nome;
    private long idTarefa;
    private int idFluxo;
    private String nomeFluxo;
    private String ids;

    public boolean isSubFluxo(final int idFluxo){
        return idFluxo != idFluxo;
    }
    @Override
    public String toString() {
        return nome + "("+ qtd +")";
    }

}
