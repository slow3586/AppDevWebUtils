package ru.blogic.muzedodevwebutils.logging;

import lombok.val;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
public class LoggingAspect {
    /*@Around("!execution(*.new(..)) " +
        "&& (" +
        "@within(org.springframework.stereotype.Component) " +
        "|| @within(org.springframework.stereotype.Service) " +
        "|| @within(org.springframework.stereotype.Repository) " +
        ")" +
        "&& execution(* *(..))" +
        "&& !execution(* *.main(..))" +
        "&& !execution(* *.*$*(..))" +
        "&& !execution(* *.*lambda*(..))" +
        //"&& !staticinitialization(*)" +
        //"&& !initialization(*.new(..)) " +
        //"&& !preinitialization(*.new(..)) " +
        //"&& !@within(ru.blogic.muzedodevwebutils.DisableLoggingAspect)" +
        "&& !within(ru.blogic.muzedodevwebutils.logging..*)"
    )*/
    @Around("execution(* *(..))" +
        "&& !execution(* *.main(..))" +
        //"&& !execution(*.new(..))" +
        "&& !execution(* *.*lambda*(..))" +
        "&& !@annotation(ru.blogic.muzedodevwebutils.logging.DisableLoggingAspect)" +
        "&& (" +
        "@within(org.springframework.stereotype.Service) " +
        "|| @within(org.springframework.stereotype.Repository) " +
        "|| @within(org.springframework.stereotype.Component) " +
        //"|| @within(org.springframework.web.bind.annotation.RestController) " +
        ")")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        final var logger = LoggerFactory.getLogger(point.getSignature().getDeclaringType());
        final var methodName = ((MethodSignature) point.getSignature()).getMethod().getName();
        logger.debug(methodName + " <- " + Arrays.stream(point.getArgs())
            .map(Object::toString)
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
