package br.com.infox.jwt.verifiers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import br.com.infox.jwt.claims.JWTClaim;

public class AnyOfJWTClaimVerifier implements JWTClaimVerifier {
    
    private List<JWTClaim> jwtClaims;
    private String errorMessagePattern;
    
    AnyOfJWTClaimVerifier(JWTClaim... jwtClaims){
        this("JWT Token expected to have one of the following missing claims [ %s ]", jwtClaims);
    }
    
    AnyOfJWTClaimVerifier(String errorMessagePattern, JWTClaim... jwtClaims){
        this.jwtClaims = Arrays.asList(jwtClaims);
        this.errorMessagePattern = errorMessagePattern;
    }
    
    @Override
    public void verify(Map<String, Object> payload) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        
        for (JWTClaim jwtClaim : jwtClaims) {
            String key = jwtClaim.getClaim();
            if (payload.containsKey(key)){
                return;
            }
            if (!first){
                sb.append(", ");
            }
            sb.append(key);
            first=false;
        }
        
        throw new IllegalArgumentException(String.format(errorMessagePattern, sb.toString()));
    }

}
