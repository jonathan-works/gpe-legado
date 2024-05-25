package br.com.infox.ibpm.variable.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;

import br.com.infox.epp.cdi.ViewScoped;
import br.com.infox.epp.cdi.exception.ExceptionHandled;
import br.com.infox.epp.cdi.exception.ExceptionHandled.MethodType;
import br.com.infox.ibpm.variable.VariableAccessHandler;
import br.com.infox.ibpm.variable.action.DominioVariavelTarefaCrudAction.DefinicaoParametro.TipoParametro;
import br.com.infox.ibpm.variable.dao.DominioVariavelTarefaDAO;
import br.com.infox.ibpm.variable.dao.ListaDadosSqlDAO;
import br.com.infox.ibpm.variable.entity.DominioVariavelTarefa;
import br.com.infox.seam.util.ComponentUtil;

@Named
@ViewScoped
public class DominioVariavelTarefaCrudAction implements Serializable {

	private static final String TAB_SEARCH = "search";
	private static final String TAB_FORM = "form";
	private static final long serialVersionUID = 1L;
	
	@Inject
    private DominioVariavelTarefaDAO dominioVariavelTarefaDAO;
	@Inject
	private Logger logger;

	private VariableAccessHandler currentVariable;
	private Integer id;
	private String codigo;
	private String nome;
	private String dominio;
	private List<SelectItem> selectItems;
	private Map<String, DefinicaoParametro> parametros = new LinkedHashMap<>();
	private String mensagemErro;
	
	private String tab = TAB_SEARCH;
	
	public static class DefinicaoParametro {
		public enum TipoParametro {
			TEXTO, DATA, INTEIRO, BOOLEAN
		}
		
		private TipoParametro tipo = TipoParametro.TEXTO;
		private Object valor;
		
		public TipoParametro getTipo() {
			return tipo;
		}
		public void setTipo(TipoParametro tipo) {
			this.tipo = tipo;
		}
		public Object getValor() {
			return valor;
		}
		public void setValor(Object valor) {
			this.valor = valor;
		}
	}

	public void onClickSearchTab() {
		newInstance();
		setTab(TAB_SEARCH);
	}

	public void onClickFormTab() {
		newInstance();
		setTab(TAB_FORM);
	}
	
	public boolean isManaged() {
		return getId() != null;
	}

	public void editDominio(DominioVariavelTarefa dominioVariavelTarefa) {
		setId(dominioVariavelTarefa.getId());
		setCodigo(dominioVariavelTarefa.getCodigo());
		setNome(dominioVariavelTarefa.getNome());
		setDominio(dominioVariavelTarefa.getDominio());
		setTab(TAB_FORM);
	}

	public void newInstance() {
		setId(null);
		setCodigo(null);
		setNome(null);
		setDominio(null);
	}
	
	public void setDominioConfig() {
		getCurrentVariable().getDominioHandler().setDominioVariavelTarefa(createInstance());
		newInstance();
		setTab(TAB_SEARCH);
	}
	
	private DominioVariavelTarefa createInstance() {
		DominioVariavelTarefa instance = new DominioVariavelTarefa();
		instance.setId(getId());
		instance.setCodigo(getCodigo());
		instance.setNome(getNome());
		instance.setDominio(getDominio());
		return instance;
	}

	@ExceptionHandled(MethodType.PERSIST)
	public void save() {
		dominioVariavelTarefaDAO.persist(createInstance());
	}
	
	@ExceptionHandled(MethodType.UPDATE)
	public void update() {
		dominioVariavelTarefaDAO.update(createInstance());
	}

	public String getTab() {
		return tab;
	}

	public void setTab(String tab) {
		this.tab = tab;
	}

	public VariableAccessHandler getCurrentVariable() {
		return currentVariable;
	}

	public void setCurrentVariable(VariableAccessHandler currentVariable) {
		this.currentVariable = currentVariable;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getDominio() {
		return dominio;
	}

	public void setDominio(String dominio) {
		this.dominio = dominio;
		
		selectItems = null;
		mensagemErro = null;
		atualizarParametros();
	}
	
	private boolean isSqlQuery() {
		return dominio.startsWith("select");
	}
	
	public void atualizarParametros() {
		if(dominio == null) {
			this.parametros.clear();
			return;
		}
		
    	ListaDadosSqlDAO listaDadosSqlDAO = ComponentUtil.getComponent(ListaDadosSqlDAO.NAME);
    	Collection<String> parametros = listaDadosSqlDAO.getParameters(dominio);
    	
    	for(String parametro : parametros) {
    		if(!this.parametros.containsKey(parametro)) {
    			this.parametros.put(parametro, new DefinicaoParametro());
    		}
    	}

    	Iterator<String> it = this.parametros.keySet().iterator();
    	while(it.hasNext()) {
    		String parametro = it.next();
    		if(!parametros.contains(parametro)) {
    			it.remove();
    		}
    	}
	}
	
	public void atualizarItens() {
        selectItems = new ArrayList<>();
        mensagemErro = null;
        
        if(dominio == null) {
        	return;
        }
        
        if (isSqlQuery()) {
        	ListaDadosSqlDAO listaDadosSqlDAO = ComponentUtil.getComponent(ListaDadosSqlDAO.NAME);
        	
        	Map<String, Object> mapaParametros = new HashMap<>(); 
        	for(String chave : this.parametros.keySet()) {
        		mapaParametros.put(chave, this.parametros.get(chave).valor);
        	}
        	
        	try {
        		List<SelectItem> items = listaDadosSqlDAO.getListSelectItem(dominio, mapaParametros);
        		selectItems.addAll(items);
        	}
        	catch(Exception e) {
        		mensagemErro = "Erro ao executar consulta: " + e.getMessage();
        		logger.log(Level.SEVERE, mensagemErro, e);
        		selectItems = null;
        		return;
        	}
        } else {
            String[] itens = dominio.split(";");
            for (String item : itens) {
                String[] pair = item.split("=");
                SelectItem selectItem = new SelectItem(pair[1], pair[0]);
                selectItems.add(selectItem);
            }
        }
		
	}
	
	public List<SelectItem> getItens() {
		return selectItems;
	}

	public Map<String, DefinicaoParametro> getParametros() {
		return parametros;
	}
	
	public List<TipoParametro> getTiposParametros() {
		return Arrays.asList(TipoParametro.values());
	}

	public String getMensagemErro() {
		return mensagemErro;
	}
	
	public void tipoAlterado(DefinicaoParametro parametro) {
		parametro.setValor(null);
	}
}
