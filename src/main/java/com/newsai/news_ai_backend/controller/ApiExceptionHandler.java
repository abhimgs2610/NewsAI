package com.newsai.news_ai_backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.newsai.news_ai_backend.dto.ApiResponseDto;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);
	private static final String BAD_REQUEST_MESSAGE = "Invalid request. Please check the request and try again.";
	private static final String ERROR_MESSAGE = "An error occurred. Please contact support.";

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponseDto<Object>> handleBadRequest(IllegalArgumentException e) {
		logger.warn("Bad request handled by API exception handler.", e);
		return buildError(HttpStatus.BAD_REQUEST, BAD_REQUEST_MESSAGE);
	}

	@ExceptionHandler({ MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class })
	public ResponseEntity<ApiResponseDto<Object>> handleRequestError(Exception e) {
		logger.warn("Request parameter error handled by API exception handler.", e);
		return buildError(HttpStatus.BAD_REQUEST, BAD_REQUEST_MESSAGE);
	}

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<ApiResponseDto<Object>> handleRuntime(RuntimeException e) {
		logger.error("Runtime error handled by API exception handler.", e);
		return buildError(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_MESSAGE);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponseDto<Object>> handleGeneric(Exception e) {
		logger.error("Unexpected error handled by API exception handler.", e);
		return buildError(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_MESSAGE);
	}

	private ResponseEntity<ApiResponseDto<Object>> buildError(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(ApiResponseDto.error(status.value(), message));
	}
}