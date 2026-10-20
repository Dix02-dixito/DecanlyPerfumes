package com.cibertec.app.controller;

import com.cibertec.app.service.*;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ControllerAdvice(assignableTypes = ProductoController.class)
public class ErroresController {
    @ExceptionHandler({
        IllegalArgumentException.class,
        org.springframework.orm.ObjectOptimisticLockingFailureException.class
    })
    public String error(Exception ex, Model model, HttpServletResponse response) {
        response.setStatus(400);
        model.addAttribute(
                "mensaje",
                ex instanceof IllegalArgumentException
                        ? ex.getMessage()
                        : "Otro cambio modificó este perfume. Vuelve a abrir Editar.");
        return "error";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String archivo(Model model, HttpServletResponse response) {
        response.setStatus(413);
        model.addAttribute(
                "mensaje", "Cada fotografía admite hasta 2 MB. Selecciona archivos más pequeños.");
        return "error";
    }
}
