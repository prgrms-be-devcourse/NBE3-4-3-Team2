package com.example.backend.global.rs;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * @author kwak
 * 공통 응답 구조
 * 정상 응답 케이스 success
 * 에러 케이스 error
 */

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RsData<T> {

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
	private final LocalDateTime time;
	private final boolean isSuccess;
	private final String message;
	private final T data;

	@Builder(access = AccessLevel.PRIVATE)
	private RsData(T data, String message, boolean isSuccess) {
		this.time = LocalDateTime.now();
		this.data = data;
		this.message = message;
		this.isSuccess = isSuccess;
	}

	public static <T> RsData<T> success(T data) {
		return RsData.<T>builder()
			.data(data)
			.isSuccess(true)
			.build();
	}

	public static <T> RsData<T> success(T data, String message) {
		return RsData.<T>builder()
			.data(data)
			.message(message)
			.isSuccess(true)
			.build();
	}

	public static <T> RsData<T> error(T data) {
		return RsData.<T>builder()
			.data(data)
			.isSuccess(false)
			.build();
	}

	public static <T> RsData<T> error(T data, String message) {
		return RsData.<T>builder()
			.data(data)
			.message(message)
			.isSuccess(false)
			.build();
	}
}
