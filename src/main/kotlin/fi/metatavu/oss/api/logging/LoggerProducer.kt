package fi.metatavu.oss.api.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import jakarta.enterprise.context.Dependent
import jakarta.enterprise.inject.Produces
import jakarta.enterprise.inject.spi.InjectionPoint

/**
 * Producer for Logger object
 *
 * @author Jari Nykänen
 */
@Dependent
class LoggerProducer {

    /**
     * Producer for Logger object
     *
     * @param injectionPoint injection point
     * @return Logger
     */
    @Produces
    fun produceLog(injectionPoint: InjectionPoint): Logger {
        return LoggerFactory.getLogger(injectionPoint.member.declaringClass.name)
    }
}
