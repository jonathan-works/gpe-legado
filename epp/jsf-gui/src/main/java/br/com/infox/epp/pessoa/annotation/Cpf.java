package br.com.infox.epp.pessoa.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import br.com.infox.epp.pessoa.validator.CpfValidator;

@Target({ METHOD, FIELD, PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CpfValidator.class)
public @interface Cpf {
	
	String pattern() default "^[0-9]{11}$";
	
	String message() default "formato inválido";
	
	Class<?>[] groups() default {};
	
	Class<? extends Payload>[] payload() default {};

}
