package home.inventory.notification.common;

import home.inventory.notification.digest.NotificationFailedException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiControllerAdvice {

    @ExceptionHandler(NotificationFailedException.class)
    ProblemDetail notificationFailed(NotificationFailedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage());
    }

    @ExceptionHandler({ConflictException.class, OptimisticLockingFailureException.class})
    ProblemDetail conflict(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(DbActionExecutionException.class)
    ProblemDetail dbAction(DbActionExecutionException e) {
        if (e.getCause() instanceof OptimisticLockingFailureException) {
            return conflict((RuntimeException) e.getCause());
        }
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Speichern fehlgeschlagen");
    }
}
