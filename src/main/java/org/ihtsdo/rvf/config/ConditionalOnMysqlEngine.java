package org.ihtsdo.rvf.config;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Registers the annotated bean only when this process runs the MySQL engine,
 * which is the default: see {@link ExecutionEngine}.
 *
 * <p>Applied to the beans that need the MySQL database: the datasource, JPA,
 * the release schemas, the assertion tables and the endpoints that administer
 * them. With the property absent or set to {@value ExecutionEngine#MYSQL}
 * every one of them is registered exactly as before.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnMysqlEngineCondition.class)
public @interface ConditionalOnMysqlEngine {
}
