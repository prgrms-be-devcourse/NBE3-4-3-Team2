package com.example.backend.entity;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Table(name = "postHashtag")
public class PostHashtagEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@JoinColumn(nullable = false, name = "post_id")
	@ManyToOne(fetch = FetchType.LAZY)
	PostEntity post;

	@JoinColumn(nullable = false, name = "hashtag_id")
	@ManyToOne(fetch = FetchType.LAZY)
	HashtagEntity hashtag;

	@Builder
	protected PostHashtagEntity(PostEntity post, HashtagEntity hashtag) {
		this.post = post;
		this.hashtag = hashtag;
	}

	public static PostHashtagEntity create(PostEntity post, HashtagEntity hashtag) {
		return PostHashtagEntity.builder()
			.post(post)
			.hashtag(hashtag)
			.build();
	}

	public Long getId() {
		return id;
	}

	public PostEntity getPost() {
		return post;
	}

	public HashtagEntity getHashtag() {
		return hashtag;
	}
}
