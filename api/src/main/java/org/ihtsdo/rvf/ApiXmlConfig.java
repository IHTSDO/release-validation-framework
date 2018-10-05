package org.ihtsdo.rvf;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication
@Configuration
@ImportResource({"classpath:servletContext.xml"})
public class ApiXmlConfig extends SpringBootServletInitializer {
}

