package com.newsai.news_ai_backend.dto;

import java.util.Collection;

public class ApiResponseDto<T> {

	private T data;
	private int count;
	private int responseCode;
	private String responseMessage;
	private boolean error;

	public ApiResponseDto() {
	}

	public ApiResponseDto(T data, int count, int responseCode, String responseMessage, boolean error) {
		this.data = data;
		this.count = count;
		this.responseCode = responseCode;
		this.responseMessage = responseMessage;
		this.error = error;
	}

	public static <T> ApiResponseDto<T> success(T data) {
		return new ApiResponseDto<>(data, count(data), 200, "Records fetched successfully", false);
	}

	public static <T> ApiResponseDto<T> success(T data, String message) {
		return new ApiResponseDto<>(data, count(data), 200, message, false);
	}

	public static <T> ApiResponseDto<T> success(T data, int count) {
		return new ApiResponseDto<>(data, count, 200, "Records fetched successfully", false);
	}

	public static <T> ApiResponseDto<T> success(T data, int count, String message) {
		return new ApiResponseDto<>(data, count, 200, message, false);
	}
	public static <T> ApiResponseDto<T> error(int responseCode, String message) {
		return new ApiResponseDto<>(null, 0, responseCode, message, true);
	}

	private static int count(Object data) {
		if (data == null) {
			return 0;
		}
		if (data instanceof Collection<?> collection) {
			return collection.size();
		}
		return 1;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}

	public String getResponseMessage() {
		return responseMessage;
	}

	public void setResponseMessage(String responseMessage) {
		this.responseMessage = responseMessage;
	}

	public boolean isError() {
		return error;
	}

	public void setError(boolean error) {
		this.error = error;
	}
}