package com.example.backend.content.comment.dto;

import lombok.Builder;

@Builder
public record CommentCreateResponse(
	Long id,
	String content,
	Long ref,
	Long postId,
	Long memberId,
	Long parentNum
) {
}