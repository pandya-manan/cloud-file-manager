
package com.manan.cloudfilemanager.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.manan.cloudfilemanager.controller.PageController;

import org.springframework.web.server.ResponseStatusException;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ControllerAdvice(assignableTypes = PageController.class)
public class GlobalPageExceptionHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(GlobalPageExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(
            IllegalArgumentException exception,
            RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute(
                "errorMessage",
                exception.getMessage()
        );

        return "redirect:/";
    }

    @ExceptionHandler(ResponseStatusException.class)
    public String handleResponseStatus(
            ResponseStatusException exception,
            RedirectAttributes redirectAttributes) {

        String message = exception.getReason() != null
                ? exception.getReason()
                : "The requested resource could not be found.";

        redirectAttributes.addFlashAttribute(
                "errorMessage",
                message
        );

        return "redirect:/";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSize(
            MaxUploadSizeExceededException exception,
            RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute(
                "errorMessage",
                "File size exceeds the maximum allowed limit of 10 MB."
        );

        return "redirect:/";
    }

    @ExceptionHandler({S3Exception.class, SdkClientException.class})
    public String handleAwsException(
            RuntimeException exception,
            RedirectAttributes redirectAttributes) {

        logger.error("AWS S3 operation failed", exception);

        redirectAttributes.addFlashAttribute(
                "errorMessage",
                "Unable to communicate with AWS S3. Please try again later."
        );

        return "redirect:/";
    }

    @ExceptionHandler(Exception.class)
    public String handleGenericException(
            Exception exception,
            RedirectAttributes redirectAttributes) {

        logger.error("Unexpected frontend application error", exception);

        redirectAttributes.addFlashAttribute(
                "errorMessage",
                "Something went wrong. Please try again later."
        );

        return "redirect:/";
    }
}
