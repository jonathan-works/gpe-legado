package br.com.infox.epp.processo.variavel.service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.seam.core.Expressions;
import org.jboss.seam.core.Expressions.MethodExpression;
import org.jboss.seam.core.Expressions.ValueExpression;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;
import org.jbpm.taskmgmt.exe.TaskInstance;

import com.google.common.base.Strings;

import br.com.infox.core.persistence.PersistenceController;
import br.com.infox.epp.access.entity.UsuarioLogin;
import br.com.infox.epp.fluxo.definicaovariavel.DefinicaoVariavelProcesso;
import br.com.infox.epp.fluxo.definicaovariavel.DefinicaoVariavelProcessoSearch;
import br.com.infox.epp.fluxo.entity.Fluxo;
import br.com.infox.epp.fluxo.manager.FluxoManager;
import br.com.infox.epp.processo.entity.Processo;
import br.com.infox.epp.processo.manager.ProcessoManager;
import br.com.infox.epp.processo.metadado.entity.MetadadoProcesso;
import br.com.infox.epp.processo.metadado.manager.MetadadoProcessoManager;
import br.com.infox.epp.processo.prioridade.entity.PrioridadeProcesso;
import br.com.infox.epp.processo.variavel.bean.VariavelProcesso;
import br.com.infox.epp.tarefa.entity.ProcessoTarefa;
import br.com.infox.epp.tarefa.manager.ProcessoTarefaManager;
import br.com.infox.log.LogProvider;
import br.com.infox.log.Logging;
import br.com.infox.seam.exception.BusinessException;

@Named
@Stateless
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class VariavelProcessoService extends PersistenceController {

    @Inject
    private MetadadoProcessoManager metadadoProcessoManager;
    @Inject
    private ProcessoManager processoManager;
    @Inject
    private ProcessoTarefaManager processoTarefaManager;
    @Inject
    private FluxoManager fluxoManager;
    @Inject
    private DefinicaoVariavelProcessoSearch definicaoVariavelProcessoSearch;

    private final LogProvider LOG = Logging.getLogProvider(VariavelProcessoService.class);

    public List<VariavelProcesso> getVariaveis(Processo processo, String recursoVariavel, boolean usuarioExterno) {
        List<VariavelProcesso> variaveis = new ArrayList<>();
        Fluxo fluxo = processo.getNaturezaCategoriaFluxo().getFluxo();
        List<DefinicaoVariavelProcesso> definicaoVariavelList = definicaoVariavelProcessoSearch.getDefinicoesVariaveis(fluxo, recursoVariavel, usuarioExterno);

        for (DefinicaoVariavelProcesso definicao : definicaoVariavelList) {
        	variaveis.add(getPrimeiraVariavelProcessoAncestral(processo, definicao, null));
        }

        return variaveis;
    }

    public VariavelProcesso getVariavelProcesso(Integer idProcesso, String nome) {
    	Processo processo =  processoManager.find(idProcesso);
    	ProcessoTarefa processoTarefa = processoTarefaManager.getUltimoProcessoTarefa(processo);
    	return getVariavelProcesso(processo, nome, processoTarefa == null? null : processoTarefa.getTaskInstance());
    }

    public VariavelProcesso getVariavelProcesso(Integer idProcesso, String nome, Long idTaskInstance) {
        Processo processo = processoManager.find(idProcesso);
        return getVariavelProcesso(processo, nome, idTaskInstance);
    }

    public VariavelProcesso getVariavelProcesso(Processo processo, String nome, Long idTaskInstance) {
    	DefinicaoVariavelProcesso definicao = null;
    	definicao = definicaoVariavelProcessoSearch.getDefinicao(processo.getNaturezaCategoriaFluxo().getFluxo(), nome);
    	if(definicao == null && idTaskInstance != null){
    		//TODO: melhorar código pois foi feito rapidamente...
    		//caso não encontre a definição no processo, procura na definicao do subprocesso.
    		ProcessInstance subrProcessInstance = processoManager.findProcessByTaskInstance(idTaskInstance);
    		Fluxo subFluxo = fluxoManager.getFluxoByDescricao(subrProcessInstance.getProcessDefinition().getName());
   			definicao = definicaoVariavelProcessoSearch.getDefinicao(subFluxo, nome);
    	}
    	if(definicao == null)
    		throw new BusinessException("Não foi possível encontrar a definição da variável " + nome);

        TaskInstance taskInstance = idTaskInstance != null ? getEntityManager().find(TaskInstance.class, idTaskInstance) : null;
        return getPrimeiraVariavelProcessoAncestral(processo, definicao, taskInstance);
    }

    public String getValorVariavelSemDefinicao(Processo processo, String nome) {
    	ProcessInstance processInstance = getEntityManager().find(ProcessInstance.class, processo.getIdJbpm());
        Object variable = processInstance.getContextInstance().getVariable(nome);
        return variable != null ? formatarValor(variable) : null;
    }

    private VariavelProcesso getPrimeiraVariavelProcessoAncestral(Processo processo, DefinicaoVariavelProcesso definicao, TaskInstance taskInstance) {
    	VariavelProcesso variavelProcesso = null;
        Processo corrente = processo;
        while (corrente != null && (variavelProcesso == null || Strings.isNullOrEmpty(variavelProcesso.getValor()))) {
            variavelProcesso = getVariavelProcesso(corrente, definicao, taskInstance);
            // Só deve olhar na taskInstance na primeira iteração
            taskInstance = null;
            corrente = corrente.getProcessoPai();
        }
        return variavelProcesso;
    }

    private VariavelProcesso getVariavelProcesso(Processo processo, DefinicaoVariavelProcesso definicao, TaskInstance taskInstance) {
        Long idJbpm = processo.getIdJbpm();
        VariavelProcesso variavelProcesso = inicializaVariavelProcesso(definicao);
        if (idJbpm != null) {
            ProcessInstance processInstance = getEntityManager().find(ProcessInstance.class, idJbpm);
            Object variable;
            if (taskInstance != null) {
                // Aqui já pega do processInstance caso não tenha na taskInstance por causa da hierarquia de VariableContainer do jBPM
                        variable = taskInstance.getVariable(definicao.getNome());
            } else {
                variable = processInstance.getContextInstance().getVariable(definicao.getNome());
            }
            if (variable != null) {
                 variavelProcesso.setValor(formatarValor(variable));
            } else {
                try{
                    List<MetadadoProcesso> metadados = metadadoProcessoManager.getMetadadoProcessoByType(processo, definicao.getNome());
                    if (metadados != null && metadados.size() > 0) {
                        setValor(processo, metadados, variavelProcesso);
                    } else {
                        final String valorPadrao = definicao.getValorPadrao();
                        if (valorPadrao != null) {
                            Object valorJbpmExpressionEvaluator = null;
                            try {
                                if ((valorPadrao.startsWith("#") || valorPadrao.startsWith("$"))) {
                                    ExecutionContext executionContext = null;
                                    if (taskInstance != null) {
                                        executionContext = new ExecutionContext(taskInstance.getToken());
                                        executionContext.setTaskInstance(taskInstance);
                                    } else if (processInstance != null) {
                                        executionContext = new ExecutionContext(processInstance.getRootToken());
                                    }
                                    if (executionContext != null) {
                                        valorJbpmExpressionEvaluator = JbpmExpressionEvaluator.evaluate(valorPadrao, executionContext);
                                        if (valorJbpmExpressionEvaluator != null) {
                                            variavelProcesso.setValor(formatarValor(valorJbpmExpressionEvaluator));
                                        }
                                    }
                                }
                            } catch (Exception e) {
                            }
                            if(valorJbpmExpressionEvaluator == null){
                                setValor(valorPadrao, processo, variavelProcesso);
                            }
                        } else {
                            variavelProcesso.setValor(null);
                        }
                    }
                }catch(Exception e){
                        variavelProcesso.setValor(null);
                        LOG.error("Não foi possível recuperar o metadado "+ definicao.getNome() + " do processo id="+ processo.getIdProcesso().toString(), e);
                }
            }
        }
        return variavelProcesso;
    }


    private String formatarValor(Object variable) {
    	if(variable == null)
    		return "";
    	if (variable instanceof Date) {
    		return new SimpleDateFormat("dd/MM/yyyy").format(variable);
    	} else if (variable instanceof Boolean) {
    		return (Boolean) variable ? "Sim" : "Não";
    	} else if (variable instanceof Double) {
    	    return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(variable);
    	} else if (variable instanceof BigDecimal) {
    	    BigDecimal bd = (BigDecimal) variable;
    	    DecimalFormat df = null;
    	    if(bd.scale() > 0) {
    	        df = new DecimalFormat("#,###.".concat(Strings.repeat("0", bd.scale())));
    	    } else {
    	        df = new DecimalFormat("#,###");
    	    }
    	    return df.format(bd.doubleValue());
    	}
		return variable.toString();
	}

	public List<VariavelProcesso> getVariaveisHierquiaProcesso(Integer idProcesso) {
        List<VariavelProcesso> variaveis = new ArrayList<>();
        Processo processo = processoManager.find(idProcesso);
        List<DefinicaoVariavelProcesso> definicaoVariavelList = definicaoVariavelProcessoSearch
                .listVariaveisByFluxo(processo.getNaturezaCategoriaFluxo().getFluxo());

        for (DefinicaoVariavelProcesso definicao : definicaoVariavelList) {
            VariavelProcesso variavel = getPrimeiraVariavelProcessoAncestral(processo, definicao, null);
            if (variavel != null) {
                variaveis.add(variavel);
            }
        }
        return variaveis;
    }

    /**
     * Adiciona valor baseado em expressão, de valor padrão configurada na definição do fluxo, ao container de variável
     * de processo
     *
     * @param valorPadrao
     * @param processo
     * @param variavelProcesso
     * @return
     */
    private void setValor(String valorPadrao, Processo processo, VariavelProcesso variavelProcesso) {
        Object value = null;
        Expressions expressions = Expressions.instance();
        try {
            ValueExpression<Object> valueExpression = expressions.createValueExpression(valorPadrao);
            value = valueExpression.getValue();
        } catch (Exception e) {
        	MethodExpression<Object> methodExpression = expressions.createMethodExpression(valorPadrao, Object.class, Processo.class);
            value = methodExpression.invoke(processo);
        }
        variavelProcesso.setValor(value != null ? value.toString() : null);
    }

    /**
     * Adiciona valor baseado em metadado de processo ao container de variável de processo
     *
     * @param processo
     * @param variavelProcesso
     * @param metadados
     * @return
     */
    private void setValor(Processo processo, List<MetadadoProcesso> metadados, VariavelProcesso variavelProcesso) {
        StringBuilder sb = new StringBuilder();
        boolean firstValue = true;
        for (MetadadoProcesso metadadoProcesso : metadados) {
            if (!firstValue) {
                sb.append(", ");
            }
            sb.append(formatarValor(metadadoProcesso.getValue()));
            firstValue = false;
        }
        variavelProcesso.setValor(sb.toString());
    }

    private VariavelProcesso inicializaVariavelProcesso(DefinicaoVariavelProcesso definicao) {
        VariavelProcesso variavelProcesso = new VariavelProcesso();
        variavelProcesso.setLabel(definicao.getLabel());
        variavelProcesso.setValor(definicao.getValorPadrao());
        return variavelProcesso;
    }

    public UsuarioLogin getUsuarioCadastro(Processo processo) {
        return processo.getUsuarioCadastro();
    }

    public PrioridadeProcesso getPrioridadeProcesso(Processo processo) {
        return processo.getPrioridadeProcesso();
    }
}
