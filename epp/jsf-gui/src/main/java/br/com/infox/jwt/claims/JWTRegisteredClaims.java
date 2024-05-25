package br.com.infox.jwt.claims;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Iterator;

import br.com.infox.core.exception.SystemExceptionFactory;
import br.com.infox.jwt.JWTErrorCodes;

public enum JWTRegisteredClaims implements JWTClaim {
    EXPIRATION_DATE("exp",BasicJWTValidators.INT_DATE),
    ISSUED_AT("iat",BasicJWTValidators.INT_DATE),
    JWT_ID("jti", BasicJWTValidators.STRING),
    SUBJECT("sub",BasicJWTValidators.STRING_OR_URI),
    AUDIENCE("aud",BasicJWTValidators.STRING_OR_URI_COLLECTION),
    NOT_BEFORE("nbf",BasicJWTValidators.INT_DATE),
    ISSUER("iss",BasicJWTValidators.STRING_OR_URI);
    private final String key;
    private final JWTValidator validator;
    private JWTRegisteredClaims(String key){
        this(key, BasicJWTValidators.STRING);
    }
    private JWTRegisteredClaims(String key, JWTValidator validator) {
        this.key = key;
        this.validator = validator;
    }
    
    @Override
    public String getClaim(){
        return key;
    }

    @Override
    public JWTValidator validator() {
        return validator;
    }

}

class BasicJWTValidators {
    public static final JWTValidator STRING = new JWTValidator(){
        @Override
        public void validate(Object value) {
            if (!(value instanceof String))
                throw SystemExceptionFactory.create(JWTErrorCodes.INVALID_CLAIM).set("value", value)
                .set("expected","string");
        }
    };
    public static final JWTValidator STRING_OR_URI = new JWTValidator(){
        @Override
        public void validate(Object value) {
            if (!(value instanceof String))
                throw SystemExceptionFactory.create(JWTErrorCodes.INVALID_CLAIM).set("value", value)
                .set("expected","string");
            String stringOrUri = (String) value;
            if (!stringOrUri.contains(":"))
                return;
            try {
                new URI(stringOrUri);
            } catch (URISyntaxException e) {
                throw SystemExceptionFactory.create(JWTErrorCodes.INVALID_CLAIM).set("value", value)
                .set("expected","URI");
            }
        }
        
    };
    public static final JWTValidator STRING_OR_URI_COLLECTION = new JWTValidator(){
        @Override
        public void validate(Object values) {
            if (values instanceof Collection) {
                @SuppressWarnings({ "unchecked" })
                Iterator<Object> iterator = ((Collection<Object>) values).iterator();
                while (iterator.hasNext()) {
                    Object value = iterator.next();
                    STRING_OR_URI.validate(value);
                }
            } else {
                STRING_OR_URI.validate(values);
            }
        }
        
    };
    public static final JWTValidator INT_DATE = new JWTValidator(){
        @Override
        public void validate(Object value) {
            if (value == null)
                return;
            if (!(value instanceof Number)) {
                throw SystemExceptionFactory.create(JWTErrorCodes.INVALID_CLAIM).set("value", value)
                    .set("expected","number");
            }
            long longValue = ((Number) value).longValue();
            if (longValue < 0)
                throw SystemExceptionFactory.create(JWTErrorCodes.INVALID_CLAIM).set("value", value)
                    .set("expected","positive number");
        }
        
    };
    
    private BasicJWTValidators(){
    }
}
