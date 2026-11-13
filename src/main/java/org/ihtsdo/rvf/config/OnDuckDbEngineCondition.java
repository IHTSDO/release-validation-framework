package org.ihtsdo.rvf.config;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Matches when {@value ExecutionEngine#PROPERTY} selects DuckDB.
 *
 * <p>Uses {@link ExecutionEngine#isDuckDb(String)}, as {@link OnMysqlEngineCondition}
 * does, so the two conditions are exact complements. {@code @ConditionalOnProperty(havingValue
 * = "duckdb")} would not trim the value, and {@code " duckdb "} would then register
 * neither engine.
 */
class OnDuckDbEngineCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		String engine = context.getEnvironment().getProperty(ExecutionEngine.PROPERTY);
		if (ExecutionEngine.isDuckDb(engine)) {
			return ConditionOutcome.match(ExecutionEngine.PROPERTY + "=" + engine + " is the DuckDB engine");
		}
		return ConditionOutcome.noMatch(ExecutionEngine.PROPERTY + "=" + engine + " selects the MySQL engine");
	}
}
