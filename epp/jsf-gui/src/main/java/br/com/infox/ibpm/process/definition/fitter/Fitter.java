package br.com.infox.ibpm.process.definition.fitter;

import br.com.infox.epp.cdi.util.Beans;
import br.com.infox.epp.fluxo.definicao.ProcessBuilder;

public abstract class Fitter {

    public abstract void clear();

    protected ProcessBuilder getProcessBuilder() {
        return Beans.getReference(ProcessBuilder.class);
    }

}
