package com.hotelsystem.audit;

import com.hotelsystem.entity.OperationLog;
import com.hotelsystem.repository.OperationLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final OperationLogRepository operationLogRepository;
    private final HttpServletRequest request;

    @Around("@annotation(com.hotelsystem.audit.Auditable) ")
    public Object aroundAuditable(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Auditable aud = method.getAnnotation(Auditable.class);

        String action = aud != null && !aud.action().isEmpty() ? aud.action() : method.getName();
        String username = "anonymous";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            username = auth.getName();
        }

        String targetEntity = pjp.getTarget() != null ? pjp.getTarget().getClass().getSimpleName() : "";
        String targetId = null;
        // try to extract id-like arg
        Object[] args = pjp.getArgs();
        if (args != null) {
            for (Object a : args) {
                if (a == null) continue;
                if (a instanceof Number) {
                    targetId = String.valueOf(a);
                    break;
                }
                try {
                    Method getId = a.getClass().getMethod("getId");
                    Object idVal = getId.invoke(a);
                    if (idVal != null) {
                        targetId = String.valueOf(idVal);
                        break;
                    }
                } catch (NoSuchMethodException ignored) {
                } catch (Exception ignored) {
                }
            }
        }

        String details = "args=" + Arrays.toString(args) + ", remoteAddr=" + request.getRemoteAddr();

        OperationLog log = new OperationLog();
        log.setUsername(username);
        log.setAction(action);
        log.setTargetEntity(targetEntity);
        log.setTargetId(targetId);
        log.setDetails(details);
        operationLogRepository.save(log);

        Object result;
        try {
            result = pjp.proceed();
        } catch (Throwable ex) {
            // update log with exception info
            log.setDetails(log.getDetails() + "; exception=" + ex.getMessage());
            operationLogRepository.save(log);
            throw ex;
        }

        return result;
    }
}
