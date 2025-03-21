package com.example.backend.global.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtil {
	public static void downloadByHttp(String url, String dirPath) {
		try {
			HttpClient client = HttpClient.newBuilder()
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.GET()
				.build();

			// 먼저 헤더만 가져오기 위한 HEAD 요청
			HttpResponse<Void> headResponse = client.send(
				HttpRequest.newBuilder(URI.create(url))
					.method("HEAD", HttpRequest.BodyPublishers.noBody())
					.build(),
				HttpResponse.BodyHandlers.discarding()
			);

			// 실제 파일 다운로드
			HttpResponse<Path> response = client.send(request,
				HttpResponse.BodyHandlers.ofFile(
					createTargetPath(url, dirPath, headResponse)
				));
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("다운로드 중 오류 발생: " + e.getMessage(), e);
		}
	}

	private static Path createTargetPath(String url, String dirPath, HttpResponse<?> response) {
		// 디렉토리가 없으면 생성
		Path directory = Path.of(dirPath);
		if (!Files.exists(directory)) {
			try {
				Files.createDirectories(directory);
			} catch (IOException e) {
				throw new RuntimeException("디렉토리 생성 실패: " + e.getMessage(), e);
			}
		}

		// 파일명 생성
		String filename = getFilenameFromUrl(url);
		String extension = getExtensionFromResponse(response);

		return directory.resolve(filename + extension);
	}

	private static String getFilenameFromUrl(String url) {
		try {
			String path = new URI(url).getPath();
			String filename = Path.of(path).getFileName().toString();
			// 확장자 제거
			return filename.contains(".")
				? filename.substring(0, filename.lastIndexOf('.'))
				: filename;
		} catch (URISyntaxException e) {
			// URL에서 파일명을 추출할 수 없는 경우 타임스탬프 사용
			return "download_" + System.currentTimeMillis();
		}
	}

	private static String getExtensionFromResponse(HttpResponse<?> response) {
		return response.headers()
			.firstValue("Content-Type")
			.map(contentType -> {
				// MIME 타입에 따른 확장자 매핑
				return switch (contentType.split(";")[0].trim().toLowerCase()) {
					case "application/json" -> ".json";
					case "text/plain" -> ".txt";
					case "text/html" -> ".html";
					case "image/jpeg" -> ".jpg";
					case "image/png" -> ".png";
					case "application/pdf" -> ".pdf";
					case "application/xml" -> ".xml";
					case "application/zip" -> ".zip";
					default -> "";
				};
			})
			.orElse("");
	}
}
