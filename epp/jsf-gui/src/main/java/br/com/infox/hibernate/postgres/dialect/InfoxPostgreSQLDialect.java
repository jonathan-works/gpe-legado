package br.com.infox.hibernate.postgres.dialect;

import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;

import br.com.infox.hibernate.function.CustomSqlFunctions;
import br.com.infox.hibernate.function.DataAdd;
import br.com.infox.hibernate.function.DataUtilAdd;
import br.com.infox.hibernate.function.DateDiffDayPostgresSQL;
import br.com.infox.hibernate.function.DateToChar;
import br.com.infox.hibernate.function.DocumentoSuficientementeAssinado;
import br.com.infox.hibernate.function.NumeroProcessoRoot;
import br.com.infox.hibernate.function.StringAgg;
import br.com.infox.hibernate.function.ToDateJpql;
import br.com.infox.hibernate.function.ToMD5Binary;

public class InfoxPostgreSQLDialect extends PostgreSQL82Dialect {
    
    public InfoxPostgreSQLDialect() {
        registerKeyword("true");
        registerKeyword("false");
        registerFunction(CustomSqlFunctions.DOCUMENTO_SUFICIENTEMENTE_ASSINADO, new DocumentoSuficientementeAssinado());
        registerFunction(CustomSqlFunctions.NUMERO_PROCESSO_ROOT, new NumeroProcessoRoot());
        registerFunction(CustomSqlFunctions.DATA_ADD, new DataAdd());
        registerFunction(CustomSqlFunctions.DATA_UTIL_ADD, new DataUtilAdd());
        registerFunction(CustomSqlFunctions.DATE_DIFF_DAY, new DateDiffDayPostgresSQL());
        registerFunction(CustomSqlFunctions.TO_DATE, new ToDateJpql());
        registerFunction(CustomSqlFunctions.MD5_BINARY, new ToMD5Binary());
        registerFunction(CustomSqlFunctions.STRING_AGG, new StringAgg());
        registerFunction(CustomSqlFunctions.DATE_TO_CHAR, new DateToChar());
        registerFunction( "concat", new StandardSQLFunction("concat", StandardBasicTypes.STRING));
    }

}
