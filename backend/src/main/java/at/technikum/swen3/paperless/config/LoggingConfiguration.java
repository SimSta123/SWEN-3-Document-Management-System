package at.technikum.swen3.paperless.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class LoggingConfiguration {

    @Bean
    @Scope("prototype") //Bean wird bei jeder Injection die anfordernde Klasse neu, damit nicht SampleService und ProjectService den gleichen Logger bekommen (Damit bei INFO die richtige Klasse steht)
    //Wenn eine Klasse einen Logger benötigt, liefere einen Logger mit dem Namen dieser Klasse.
    //InjectionPoint: Wo wird der Logger benötigt?
    //.getDeclaringClass: Welche Klasse dieses Bean beansprucht
    public Logger logger(InjectionPoint injectionPoint) {
        return LoggerFactory.getLogger(
                injectionPoint.getMember().getDeclaringClass()
        );
    }
}