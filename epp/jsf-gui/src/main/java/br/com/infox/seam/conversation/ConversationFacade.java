package br.com.infox.seam.conversation;

import br.com.infox.cdi.producer.EntityManagerProducer;
import br.com.infox.epp.processo.consulta.list.ConsultaProcessoList;
import br.com.infox.log.LogProvider;
import br.com.infox.log.Logging;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.core.Conversation;

@Name(ConversationFacade.NAME)
public class ConversationFacade {

    private static final LogProvider LOG = Logging.getLogProvider(ConversationFacade.class);

    public static final String NAME = "conversationFacade";

    /**
     * Finaliza a conversação
     * 
     * @param toUrl
     * @return
     */

    // TODO Verificar como utilizar outcome em vez da url nos menus
    public String endBeforeRedirect(String toUrl) {

        LOG.info(toUrl);

        Conversation.instance().root();
        Conversation.instance().endBeforeRedirect();
        EntityManagerProducer.clear();
        return toUrl;
    }
}
