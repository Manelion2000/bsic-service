package com.bakouan.app.config;


import com.bakouan.app.dto.BaApiErrorDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.StringJoiner;


/**
 * Entité listener, pour faire varier la version
 * à chaque modification.
 *
 * @author : <A HREF="mailto:abdramanbakouan@gmail.com">Bakouan Abdramane (ManeLion2000)</A>
 * @version : 1.0
 * Copyright (c) 2024 Bakouan, All rights reserved.
 * @since : 29/04/2024 à 05:14
 */
@RestControllerAdvice
@RestController
@Slf4j
public class BaRestExceptionHandler {

    /**
     * Constructeur par défaut.
     */
    public BaRestExceptionHandler() {
        super();
    }

    /**
     * Cette exception survient lorsque l'API ne peut pas lire un message HTTP
     * (Exple : lorsque l'URI est malformée).
     *
     * @param ex      HttpMessageNotReadableException
     * @param headers Contenu de l'entête
     * @param request WebRequest
     * @return un objet de type ResponseEntity
     */
    @NonNull
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleHttpMessageNotReadable(
            final HttpMessageNotReadableException ex,
            final HttpHeaders headers,
            final WebRequest request) {
        log.error("Requête JSON mal formatée");
        BaApiErrorDetail detailErreur = new BaApiErrorDetail(HttpStatus.BAD_REQUEST,
                "Requête JSON mal formatée", ex);
        return Objects.requireNonNull(handleExceptionInternal(ex,
                detailErreur,
                headers,
                detailErreur.getStatus(),
                request));
    }

    /**
     * Gérer l'exception, des requêtes contenant des données invalides.
     *
     * @param ex
     * @param request
     * @return une réponse
     */
    @NonNull
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValid(
            final MethodArgumentNotValidException ex,
            final WebRequest request) {

        StringJoiner errors = new StringJoiner("\n");
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.add(error.getDefaultMessage());
        }
        for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            errors.add(error.getDefaultMessage());
        }
        BaApiErrorDetail detailErreur = new BaApiErrorDetail(HttpStatus.BAD_REQUEST,
                errors.toString(), ex);

        return Objects.requireNonNull(handleExceptionInternal(ex,
                detailErreur, HttpHeaders.EMPTY, detailErreur.getStatus(), request));
    }

    /**
     * Gérer l'exception, des requêtes contenant des données invalides.
     *
     * @param ex
     * @param headers
     * @param request
     * @return une réponse
     */
    @NonNull
    @ExceptionHandler(MissingRequestValueException.class)
    public ResponseEntity<Object> handleMissingRequestValue(
            final MissingServletRequestParameterException ex, final HttpHeaders headers, final WebRequest request) {
        BaApiErrorDetail detailErreur = new BaApiErrorDetail(HttpStatus.BAD_REQUEST,
                "Paramètre : " + ex.getParameterName() + " obligatoire.", ex);
        return Objects.requireNonNull(handleExceptionInternal(ex,
                detailErreur, headers, detailErreur.getStatus(), request));
    }

    /**
     * Cette classe traite de toutes les autres exceptions.
     *
     * @param ex      Exception
     * @param request WebRequest
     * @return un objet de type ResponseEntity
     */
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Object> handleUnsupportedMethod(final org.springframework.web.HttpRequestMethodNotSupportedException ex) {
        HttpHeaders headers = new HttpHeaders();
        if (ex.getSupportedHttpMethods() != null) headers.setAllow(ex.getSupportedHttpMethods());
        return new ResponseEntity<>(new BaApiErrorDetail(HttpStatus.METHOD_NOT_ALLOWED,
                "Cette opération n'est pas disponible sur cette ressource.", ex), headers, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleInvalidParameter(final org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        return new ResponseEntity<>(new BaApiErrorDetail(HttpStatus.BAD_REQUEST,
                "Paramètre invalide : " + ex.getName() + ".", ex), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<Object> handleMissingResource(final org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        return new ResponseEntity<>(new BaApiErrorDetail(HttpStatus.NOT_FOUND, "Ressource introuvable.", ex), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<Object> handleAll(final Exception ex, final WebRequest request) {
        BaApiErrorDetail detailErreur;
        if (ex instanceof org.springframework.web.multipart.MaxUploadSizeExceededException) {
            detailErreur = new BaApiErrorDetail(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Le fichier ou la sélection dépasse la taille autorisée : 10 Mo par fichier, 20 Mo de manuels par enregistrement.", ex);
        } else if (ex instanceof final ResponseStatusException responseStatusException) {
            detailErreur = new BaApiErrorDetail(HttpStatus.resolve(responseStatusException.getStatusCode().value()),
                    responseStatusException.getReason(), ex);
        }
         else if (ex instanceof InsufficientAuthenticationException) { detailErreur =
         new BaApiErrorDetail(HttpStatus.FORBIDDEN, "Utilisateur non connecté", ex); }
          else if (ex instanceof org.springframework.dao.ConcurrencyFailureException || ex instanceof jakarta.persistence.OptimisticLockException || ex instanceof jakarta.persistence.PessimisticLockException) {
              detailErreur = new BaApiErrorDetail(HttpStatus.CONFLICT,"Une operation concurrente a modifie la demande. Actualisez avant de reessayer.",ex);
          }
          else {
            detailErreur = new BaApiErrorDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Une erreur interne est survenue. Veuillez reessayer plus tard.", ex);
        }
        log.error("Exception occurred : ", ex);
        return new ResponseEntity<>(detailErreur, new HttpHeaders(), detailErreur.getStatus());
    }

    private ResponseEntity<Object> handleExceptionInternal(
            final Exception ex, @Nullable final Object body, final HttpHeaders headers,
            final HttpStatus status, final WebRequest request) {
        if (HttpStatus.INTERNAL_SERVER_ERROR.equals(status)) {
            request.setAttribute("javax.servlet.error.exception", ex, 0);
        }

        return new ResponseEntity<>(body, headers, status);
    }

}
