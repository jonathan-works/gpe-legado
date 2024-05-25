package br.com.infox.epp.meiocontato.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import br.com.infox.epp.meiocontato.validator.EmailValidator;


@Target({ METHOD, FIELD, PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EmailValidator.class)
public @interface Email {
	
	String pattern() default ".+@.+\\.[a-z]+";
	
	String message() default "formato inválido.";
	
	Class<?>[] groups() default {};
	
	Class<? extends Payload>[] payload() default {};

}
