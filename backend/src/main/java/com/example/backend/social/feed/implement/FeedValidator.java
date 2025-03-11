package com.example.backend.social.feed.implement;

import static com.example.backend.social.feed.constant.FeedConstants.*;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.example.backend.social.feed.dto.FeedMemberRequest;
import com.example.backend.social.feed.dto.FeedRequest;
import com.example.backend.social.feed.exception.FeedErrorCode;
import com.example.backend.social.feed.exception.FeedException;

/***
 * FeedValidator
 * 피드 요청 정보의 유효성을 검사하는 클래스
 * @author ChoiHyunSan
 * @since 2025-02-01
 */
@Component
public class FeedValidator {

	public void validateRequest(FeedRequest request) {
		checkTimestamp(request);
		checkLastPostId(request.lastPostId());
		checkMaxSize(request.maxSize());
	}

	public void validateRequest(FeedMemberRequest request) {
		checkLastPostId(request.lastPostId());
		checkMaxSize(request.maxSize());
	}

	private static void checkTimestamp(FeedRequest request) {
		LocalDateTime requestTime = request.timestamp();
		if (requestTime == null || requestTime.isAfter(LocalDateTime.now())) {
			throw new FeedException(FeedErrorCode.INVALID_TIMESTAMP_REQUEST);
		}
	}

	private static void checkLastPostId(Long lastPostId) {
		if (lastPostId == null || lastPostId < 0) {
			throw new FeedException(FeedErrorCode.INVALID_POST_ID_REQUEST);
		}
	}

	private static void checkMaxSize(Integer maxSize) {
		if (maxSize == null || maxSize <= 0 || maxSize > REQUEST_FEED_MAX_SIZE) {
			throw new FeedException(FeedErrorCode.INVALID_MAXSIZE_REQUEST);
		}
	}
}
