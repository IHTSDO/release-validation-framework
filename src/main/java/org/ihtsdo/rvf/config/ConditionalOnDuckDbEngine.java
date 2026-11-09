package org.ihtsdo.rvf.config;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Registers the annotated bean only when {@value ExecutionEngine#PROPERTY}
 * selects the DuckDB engine: see {@link ExecutionEngine}.
 *
 * <p>The counterpart of {@link ConditionalOnMysqlEngine}. Exactly one of the two
 * matches for any value of the property.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnDuckDbEngineCondition.class)
public @interface ConditionalOnDuckDbEngine {
}
