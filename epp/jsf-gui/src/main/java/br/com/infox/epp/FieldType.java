package br.com.infox.epp;

import java.util.Date;

import javax.persistence.Entity;

public enum FieldType {
	DATE, BOOLEAN, STRING, TEXT, SELECT_ONE, SELECT_ONE_ENUM, CHECKBOX_ENUM, RADIO_ENUM;

	public static FieldType getByClass(Class<?> type) {
		if (Date.class.equals(type)) {
			return DATE;
		} else if (Boolean.class.equals(type)) {
			return BOOLEAN;
		} else if (String.class.equals(type)) {
			return STRING;
		} else if (type != null && type.isAnnotationPresent(Entity.class)) {
			return SELECT_ONE;
		}
		return null;
	}
}
