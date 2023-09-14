package ru.blogic.muzedodevwebutils.config.logging;

import io.vavr.control.Option;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Aspect
@Component
public class LoggingAspect {
 /*   @Around("execution(* *(..))" +
        "&& !execution(* *.main(..))" +
        "&& !execution(*.new(..))" +
        "&& !execution(* *.*lambda*(..))" +
        "&& !@annotation(ru.blogic.muzedodevwebutils.logging.DisableLoggingAspect)" +
        "&& !@within(ru.blogic.muzedodevwebutils.logging.DisableLoggingAspect)" +
        "&& !within(ru.blogic.muzedodevwebutils.logging..*)" +
        "&& (" +
        "@within(org.springframework.stereotype.Service) " +
        "|| @within(org.springframework.stereotype.Component) " +
        //"|| @within(org.springframework.web.bind.annotation.RestController) " +
        ")")*/
    public Object around(ProceedingJoinPoint point) throws Throwable {
        final Logger logger = LoggerFactory.getLogger(point.getSignature().getDeclaringType());
        final String methodName = ((MethodSignature) point.getSignature()).getMethod().getName();
        logger.debug(methodName + " <- " +
            Option.of(point.getArgs())
                .map(Arrays::stream)
                .getOrElse(Stream::empty)
                .map(Objects::toString)
                .collect(Collectors.joining(", ", "(", ")")));

        final Object result;
        try {
            result = point.proceed(point.getArgs());
        } catch (Throwable e) {
            logger.error(methodName + " -> " + e + " " + e.getMessage(), e);
            throw e;
        }

        logger.debug(methodName + " -> " + result);

        return result;
    }
}
