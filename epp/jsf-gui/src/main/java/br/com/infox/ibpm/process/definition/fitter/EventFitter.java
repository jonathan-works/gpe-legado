package br.com.infox.ibpm.process.definition.fitter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Named;

import org.jbpm.graph.def.Event;

import br.com.infox.core.util.ReflectionsUtil;
import br.com.infox.epp.cdi.ViewScoped;
import br.com.infox.jbpm.event.EventHandler;

@Named
@ViewScoped
public class EventFitter extends Fitter implements Serializable {

	private static final long serialVersionUID = 1L;

	private List<EventHandler> eventList;
	private EventHandler currentEvent;

	public EventHandler getCurrentEvent() {
		return currentEvent;
	}

	public void setCurrentEvent(EventHandler cEvent) {
		this.currentEvent = cEvent;
	}

	public String getEventType() {
		if (currentEvent == null) {
			return null;
		}
		return currentEvent.getEvent().getEventType();
	}

	public void setEventType(String type) {
		Event event = currentEvent.getEvent();
		getProcessBuilder().getInstance().removeEvent(event);
		ReflectionsUtil.setValue(event, "eventType", type);
		getProcessBuilder().getInstance().addEvent(event);
	}

	public List<EventHandler> getEventList() {
		if (eventList == null) {
			eventList = EventHandler.createList(getProcessBuilder().getInstance());
			if (eventList.size() == 1) {
				setCurrentEvent(eventList.get(0));
			}
		}
		return eventList;
	}

	public List<String> getSupportedEventTypes() {
		List<String> list = new ArrayList<String>();
		String[] eventTypes = getProcessBuilder().getInstance().getSupportedEventTypes();
		List<String> currentEvents = new ArrayList<String>();
		Collection<Event> values = getProcessBuilder().getInstance().getEvents().values();
		for (Event event : values) {
			currentEvents.add(event.getEventType());
		}
		for (String type : eventTypes) {
			if (!currentEvents.contains(type)) {
				list.add(type);
			}
		}
		return list;
	}

	@Override
	public void clear() {
		eventList = null;
		currentEvent = null;
	}
}
