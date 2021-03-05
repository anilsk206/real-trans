package com.test.realtrans.exception;



import com.test.realtrans.constants.APIConstants;
import com.test.realtrans.model.ErrorModel;
//import com.compozed.appfabric.logging.AppFabricLogger;
//import com.compozed.appfabric.logging.LogEvent;
//import com.compozed.appfabric.logging.LoggingEventType;
//import com.compozed.appfabric.logging.annotations.AppFabricLog;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import javax.validation.ConstraintViolationException;
import java.util.UUID;

@ControllerAdvice
public class RealTimeTranscriptionExceptionHandler {

//    @AppFabricLog
//    AppFabricLogger logger;
    private UUID interactionID = UUID.randomUUID();
    private String applicationID = "RealTimeTranscription";
    private String log;

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorModel> handleException(final Exception exception) {
//        logger.error(new LogEvent.Builder("ApplicationError", LoggingEventType.APPLICATION, this.getClass().getSimpleName())
//                .interactionId(interactionID.toString())
//                .userId(applicationID)
//                .description(String.format("error occurred %s", exception.getMessage())).build());
        System.out.println(String.format("error occurred %s", exception.getMessage()));
        ErrorModel error = getErrorModel(HttpStatus.BAD_REQUEST.value(), APIConstants.APPLICATION_ERROR, exception.getMessage());
        return new ResponseEntity<>(
                error, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RealTimeTranscriptionException.class)
    public ResponseEntity<ErrorModel> handleCustomUDPForensicsException(RealTimeTranscriptionException exception) {
//        logger.error(new LogEvent.Builder("CustomError", LoggingEventType.APPLICATION, this.getClass().getSimpleName())
//                .interactionId(interactionID.toString())
//                .userId(applicationID)
//                .description(String.format("custom exception %s", exception.getMessage())).build());
        System.out.println(String.format("error occurred %s", exception.getMessage()));
        ErrorModel error = getErrorModel(HttpStatus.BAD_REQUEST.value(), exception.getErrorCode(), exception.getMessage());

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({ConstraintViolationException.class})
    public ResponseEntity<ErrorModel> handleConstraintViolation(ConstraintViolationException exception, WebRequest request) {
//        logger.error(new LogEvent.Builder("ConstraintError", LoggingEventType.APPLICATION, this.getClass().getSimpleName())
//                .interactionId(interactionID.toString())
//                .userId(applicationID)
//                .description(String.format("constraint violation error %s", exception.getMessage())).build());
        System.out.println(String.format("error occurred %s", exception.getMessage()));
        ErrorModel imageForensicsError = getErrorModel(HttpStatus.BAD_REQUEST.value(), APIConstants.INVALID_INPUT, exception.getMessage());
        return new ResponseEntity<>(
                imageForensicsError, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

//    @ExceptionHandler({ServletRequestBindingException.class,
//            HttpMediaTypeNotAcceptableException.class,
//            HttpMediaTypeNotSupportedException.class,
//            HttpMessageNotReadableException.class,
//            HttpRequestMethodNotSupportedException.class,
//            MissingServletRequestParameterException.class,
//            MissingServletRequestPartException.class,
//            TypeMismatchException.class
//    })
//
//    @ResponseBody
//    public ResponseEntity<ErrorModel> servletRequestBindingExHandler(Exception exception) {
////        logger.error(new LogEvent.Builder("ServletBindingError", LoggingEventType.APPLICATION, this.getClass().getSimpleName())
////                .interactionId(interactionID.toString())
////                .userId(applicationID)
////                .description(String.format("servlet binding failed %s", exception.getMessage())).build());
//        System.out.println(String.format("error occurred %s", exception.getMessage()));
//        ErrorModel imageForensicsError = getErrorModel(HttpStatus.BAD_REQUEST.value(), APIConstants.INVALID_INPUT, exception.getLocalizedMessage());
//        return new ResponseEntity<>(
//                imageForensicsError, new HttpHeaders(), HttpStatus.BAD_REQUEST);
//    }

    private ErrorModel getErrorModel(int statusCode, String errorCode, String message) {
        return new ErrorModel().errorStatusCode(statusCode).errorCode(errorCode).message(message);
    }

}
