package org.ihtsdo.rvf.config;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Matches unless {@value ExecutionEngine#PROPERTY} selects another engine.
 *
 * <p>A condition of its own rather than {@code @ConditionalOnProperty(havingValue
 * = "mysql", matchIfMissing = true)}: that would drop the MySQL beans for any
 * value other than {@code mysql}, including a typo, and its matching rule could
 * drift from {@link ExecutionEngine#isDuckDb(String)}, which the
 * auto-configuration exclusions use.
 */
class OnMysqlEngineCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		String engine = context.getEnvironment().getProperty(ExecutionEngine.PROPERTY);
		if (ExecutionEngine.isDuckDb(engine)) {
			return ConditionOutcome.noMatch(ExecutionEngine.PROPERTY + "=" + engine + " selects another engine");
		}
		return ConditionOutcome.match(ExecutionEngine.PROPERTY + "=" + engine + " is the MySQL engine");
	}
}
