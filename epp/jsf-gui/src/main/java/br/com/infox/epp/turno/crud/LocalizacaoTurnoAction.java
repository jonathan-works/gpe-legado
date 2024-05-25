package br.com.infox.epp.turno.crud;

import java.io.Serializable;

import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.seam.faces.FacesMessages;

import br.com.infox.core.action.ActionMessagesService;
import br.com.infox.core.persistence.DAOException;
import br.com.infox.epp.access.entity.Localizacao;
import br.com.infox.epp.cdi.ViewScoped;
import br.com.infox.epp.cdi.transaction.Transactional;
import br.com.infox.epp.turno.component.TurnoBean;
import br.com.infox.epp.turno.component.TurnoHandler;
import br.com.infox.epp.turno.entity.LocalizacaoTurno;
import br.com.infox.epp.turno.manager.LocalizacaoTurnoManager;
import br.com.infox.log.Log;
import br.com.infox.log.Logging;
import br.com.infox.util.time.DateRange;

@Named
@ViewScoped
public class LocalizacaoTurnoAction implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Log LOG = Logging.getLog(LocalizacaoTurnoAction.class);

    private static final int UMA_HORA_EM_MINUTOS = 60;

    @Inject
    private LocalizacaoTurnoManager localizacaoTurnoManager;
    @Inject
    private ActionMessagesService actionMessagesService;

    private Localizacao localizacao;
    private TurnoHandler turnoHandler;

    private void createTurnoHandler() {
        this.turnoHandler = new TurnoHandler(
                LocalizacaoTurnoAction.UMA_HORA_EM_MINUTOS);
        for (final LocalizacaoTurno localizacaoTurno : this.localizacaoTurnoManager
                .listByLocalizacao(this.localizacao)) {
            this.turnoHandler.addIntervalo(localizacaoTurno.getDiaSemana(),
                    localizacaoTurno.getHoraInicio(),
                    localizacaoTurno.getHoraFim());
        }
    }

    public TurnoHandler getTurnoHandler() {
        return this.turnoHandler;
    }

    @Transactional
    public void gravarTurnos() {
        try {
            this.localizacaoTurnoManager
            .removerTurnosAnteriores(this.localizacao);
            inserirTurnosSelecionados();
        } catch (final DAOException e) {
            this.actionMessagesService.handleDAOException(e);
        }
    }

    private void inserirTurnosSelecionados() {
        boolean houveErro = false;
        for (final TurnoBean turno : this.turnoHandler.getTurnosSelecionados()) {
            final LocalizacaoTurno localizacaoTurno = new LocalizacaoTurno();
            localizacaoTurno.setLocalizacao(this.localizacao);
            localizacaoTurno.setDiaSemana(turno.getDiaSemana());
            localizacaoTurno.setHoraInicio(turno.getHoraInicial());
            localizacaoTurno.setHoraFim(turno.getHoraFinal());

            localizacaoTurno.setTempoTurno(new DateRange(turno.getHoraFinal(),
                    turno.getHoraFinal()).get(DateRange.MINUTES).intValue());

            try {
                this.localizacaoTurnoManager.persist(localizacaoTurno);
            } catch (final DAOException e) {
                houveErro = true;
                LocalizacaoTurnoAction.LOG.error(
                        ".inserirTurnosSelecionados()", e);
            }
        }
        if (!houveErro) {
            FacesMessages.instance().add("#{infoxMessages['entity_updated']}");
        } else {
            FacesMessages.instance().add(
                    "#{infoxMessages['localizacaoTurno.erroGravacaoTurno']}");
        }
    }

    public void newInstance(final Localizacao localizacao) {
        this.localizacao = localizacao;
        createTurnoHandler();
    }

    public void setTurnoHandler(final TurnoHandler turnoHandler) {
        this.turnoHandler = turnoHandler;
    }
}
