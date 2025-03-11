package com.example.backend.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "likes")
public class LikeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@CreatedDate
	private LocalDateTime createDate;

	@Column(name = "is_liked", nullable = false)
	private boolean isLiked;

	@Column(name = "updated_date")
	private LocalDateTime updatedDate;

	@Column(name = "resource_id", nullable = false)
	private Long resourceId;

	@Column(name = "resource_type", nullable = false)
	private String resourceType; // "POST", "COMMENT", "REPLY"

	@JoinColumn(nullable = false, name = "member_id")
	@ManyToOne(fetch = FetchType.LAZY)
	private MemberEntity member;

	// 게시물 좋아요용 생성자
	public LikeEntity(MemberEntity member, Long resourceId, String resourceType, boolean isActive) {
		this.member = member;
		this.resourceId = resourceId;
		this.resourceType = resourceType;
		this.isLiked = isLiked();
		this.createDate = LocalDateTime.now();
		this.updatedDate = LocalDateTime.now();
	}

	// 정적 팩토리 메서드
	public static LikeEntity create(MemberEntity member, Long resourceId, String resourceType) {
		return new LikeEntity(member, resourceId, resourceType, true);
	}

	// 좋아요 상태 업데이트
	public void updateLikeStatus(boolean isLiked) {
		this.isLiked = isLiked();
		this.updatedDate = LocalDateTime.now();
	}

	public Long getMemberId() {
		return member.getId();
	}

	public Long getResourceId() {
		return resourceId;
	}

	public String getResourceType() {
		return resourceType;
	}
}
