package com.example.backend.social.feed.implement;

import static com.example.backend.entity.QBookmarkEntity.*;
import static com.example.backend.entity.QCommentEntity.*;
import static com.example.backend.entity.QHashtagEntity.*;
import static com.example.backend.entity.QImageEntity.*;
import static com.example.backend.entity.QLikeEntity.*;
import static com.example.backend.entity.QPostEntity.*;
import static com.example.backend.entity.QPostHashtagEntity.*;
import static com.example.backend.social.feed.constant.FeedConstants.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.backend.entity.MemberEntity;
import com.example.backend.social.feed.Feed;
import com.example.backend.social.feed.exception.FeedErrorCode;
import com.example.backend.social.feed.exception.FeedException;
import com.example.backend.social.feed.schedular.FeedScheduler;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

/***
 * FeedSelector
 * 피드 객체를 제공하는 컴포넌트
 * @author ChoiHyunSan
 * @since 2025-01-31
 */
@Component
@RequiredArgsConstructor
public class FeedSelectorCache {

	private final JPAQueryFactory queryFactory;
	private final FeedScheduler scheduler;

	/**
	 * 단건 게시물에 대한 피드를 반환
	 * @param postId 게시물 ID
	 * @param member 멤버 엔티티 객체
	 * @return 피드 단건
	 */
	public Feed findByPostId(Long postId, MemberEntity member) {
		List<Feed> feedList = queryFactory.select(
				Projections.constructor(Feed.class,
					postEntity,
					commentCountByPost()))
			.from(postEntity)
			.where(postEntity.id.eq(postId).and(postEntity.isDeleted.isFalse()))
			.fetch();

		if (feedList.size() != 1) {
			throw new FeedException(FeedErrorCode.INVALID_POST_REQUEST);
		}

		fillFeedData(feedList, member);
		return feedList.getFirst();
	}

	/***
	 * 팔로워가 팔로우중인 Member 들의 게시물을 얻는다.
	 * 파라미터로 넘어오는 timestamp 이전에 등록된 게시물들에 한해서 최대 limit 개수만큼 리스트에 담는다.
	 * timestamp 가 동일한 경우에 대비하여 lastPostId를 같이 받아서 처리한다.
	 * @param member 팔로워 Entity 객체
	 * @param lastPostId 최근 받아간 피드 중 가장 마지막 ID
	 * @param limit 한 번에 받아올 리스트의 최대 크기
	 * @return 피드 리스트
	 */
	public List<Feed> findByFollower(final MemberEntity member, final Long lastPostId, final int limit) {

		// Post 정보와 count 를 조회
		List<Feed> feedList = queryFactory.select(
				Projections.constructor(Feed.class,
					postEntity,
					commentCountByPost()))
			.from(postEntity)
			.join(postEntity.member)
			.fetchJoin()
			.where(
				findPostsBeforeId(lastPostId)
					.and(isFollowingOrOwnPost(member))
					.and(postEntity.isDeleted.isFalse()))
			.groupBy(postEntity)
			.orderBy(postEntity.createDate.desc())
			.limit(limit)
			.fetch();

		fillFeedData(feedList, member);
		return feedList;
	}

	/**
	 * 추천 게시물을 취합하여 반환한다
	 * 팔로잉 게시물과 member 자신의 게시물은 제외한다
	 * @param member 요청한 유저의 멤버 Entity 객체
	 * @param startTime 가장 최근 받은 추천 게시물의 timestamp
	 * @param lastTime 추천 게시물을 요청할 범위
	 * @param limit 추천 게시물 요청 페이징 개수
	 * @return 피드 리스트
	 */
	public List<Feed> findRecommendFinder(
		final MemberEntity member, final LocalDateTime startTime, final LocalDateTime lastTime, final int limit) {

		// 이거로 구할 수 있는 것 => 좋아요 개수가 많은 순, 댓글 수가 많은 순으로 구할 수 있다.
		List<Feed> feedList = queryFactory.select(Projections.constructor(Feed.class,
				postEntity,
				commentCountByPost()))
			.from(postEntity)
			.join(postEntity.member)
			.fetchJoin()
			.where(
				findByDateBetweenExclusiveStart(startTime, lastTime)
					.and(isRecommendableToMember(member))
					.and(postEntity.isDeleted.isFalse()))
			.orderBy(calculatePostPopularityScore())
			.limit(limit * RECOMMEND_RANDOM_POOL_MULTIPLIER)
			.fetch();

		// 랜덤하게 뽑는다
		Collections.shuffle(feedList);
		feedList = feedList.subList(0, Math.min(limit, feedList.size()));

		fillFeedData(feedList, member);
		return feedList;
	}

	/**
	 * 해당 멤버가 작성한 게시물에 대한 피드를 반환
	 * @param member 멤버 엔티티 객체
	 * @param lastPostId 마지막으로 받은 게시물의 ID
	 * @param limit 페이징 최대 크기
	 * @return 피드 리스트
	 */
	public List<Feed> findByMember(final MemberEntity member, final Long lastPostId, final Integer limit) {
		List<Feed> feedList = queryFactory.select(Projections.constructor(Feed.class,
				postEntity,
				commentCountByPost()))
			.from(postEntity)
			.join(postEntity.member)
			.fetchJoin()
			.where(findMemberPostAndPaging(member, lastPostId).and(postEntity.isDeleted.isFalse()))
			.orderBy(postEntity.createDate.desc())
			.limit(limit)
			.fetch();

		fillFeedData(feedList, member);
		return feedList;
	}

	private static BooleanExpression findMemberPostAndPaging(MemberEntity member, Long lastPostId) {
		if (lastPostId == 0L) {
			return postEntity.member.id.eq(member.getId());
		}
		return postEntity.member.id.eq(member.getId()).and(postEntity.id.lt(lastPostId));
	}

	private void fillFeedData(List<Feed> feeds, MemberEntity member) {

		List<Long> postIds = feeds.stream().map(feed -> feed.getPost().getId()).collect(Collectors.toList());

		Map<Long, List<String>> hashtagsByPostId = queryFactory.select(postHashtagEntity.post.id, hashtagEntity.content)
			.from(postHashtagEntity)
			.join(hashtagEntity)
			.on(postHashtagEntity.hashtag.eq(hashtagEntity))
			.where(postHashtagEntity.post.id.in(postIds))
			.fetch()  // Tuple 리스트로 조회
			.stream()
			.collect(Collectors.groupingBy(tuple -> tuple.get(0, Long.class),        // postId로 그룹핑
				Collectors.mapping(tuple -> tuple.get(1, String.class),            // content를 리스트로 수집
					Collectors.toList())));

		Map<Long, List<String>> imageUrlsByPostId = queryFactory.select(imageEntity.post.id, imageEntity.imageUrl)
			.from(imageEntity)
			.where(imageEntity.post.id.in(postIds))
			.fetch()  // Tuple 리스트로 조회
			.stream()
			.collect(Collectors.groupingBy(tuple -> tuple.get(0, Long.class),    // postId로 그룹핑
				Collectors.mapping(tuple -> tuple.get(1, String.class),            // imageUrl을 리스트로 수집
					Collectors.toList())));

		Map<Long, Long> bookmarkByPostId = queryFactory.select(bookmarkEntity.id, bookmarkEntity.post.id)
			.from(bookmarkEntity)
			.where(
				bookmarkEntity.post.id.in(postIds)
					.and(bookmarkEntity.member.id.eq(member.getId())))
			.fetch()
			.stream()
			.collect(Collectors.toMap(
				tuple -> tuple.get(bookmarkEntity.post.id),
				tuple -> tuple.get(bookmarkEntity.id)
			));

		// 좋아요 정보 조회 추가
		Map<Long, Boolean> likeByPostId = queryFactory.select(likeEntity.resourceId)
			.from(likeEntity)
			.where(
				likeEntity.resourceId.in(postIds)
					.and(likeEntity.member.id.eq(member.getId()))
					.and(likeEntity.resourceType.eq("POST"))  // 게시물 타입만 필터링
					.and(likeEntity.isLiked.isTrue()))        // 좋아요가 활성화된 상태만
			.fetch()
			.stream()
			.collect(Collectors.toMap(
				resourceId -> resourceId,
				resourceId -> true,
				(existing, replacement) -> existing  // In case of duplicate keys, keep existing
			));

		feeds.forEach(feed -> {
			Long postId = feed.getPost().getId();
			feed.fillData(
				hashtagsByPostId.getOrDefault(postId, new ArrayList<>()),
				imageUrlsByPostId.getOrDefault(postId, new ArrayList<>()),
				bookmarkByPostId.getOrDefault(postId, -1L),
				likeByPostId.getOrDefault(postId, false)

			);
		});

	}

	// 좋아요 개수 / 팔로워 수 / 댓글 수에 각각 점수를 매겨서 정렬
	private OrderSpecifier<Double> calculatePostPopularityScore() {
		return Expressions.numberTemplate(Double.class,
				"({1} * 2) + "
					+ "(select count(*) from CommentEntity c where c.post.id = {0}) + "
					+ "(select case when count(*) > 0 then 3 else 0 end "
					+ "from PostHashtagEntity ph where ph.post.id = {0} and ph.hashtag.id in ({2}))",
				postEntity.id,
				postEntity.member.followerCount,
				scheduler.getPopularHashtagList())
			.desc();
	}

	private static BooleanExpression findByDateBetweenExclusiveStart(LocalDateTime startTime, LocalDateTime lastTime) {
		return postEntity.createDate.before(startTime)
			.and(postEntity.createDate.goe(lastTime));
	}

	private BooleanExpression isNotFollowingPostAuthor(MemberEntity member) {
		List<String> followingUsernames = member.getFollowingList();

		// 팔로잉 목록이 없으면 항상 true 조건을 반환
		if (followingUsernames.isEmpty()) {
			return Expressions.TRUE;
		}

		return postEntity.member.username.notIn(followingUsernames);
	}

	private static BooleanExpression isNotAuthorOfPost(MemberEntity member) {
		return postEntity.member.id.notIn(member.getId());
	}

	private BooleanExpression isFollowingOrOwnPost(MemberEntity member) {
		List<String> followingList = member.getFollowingList();

		if (followingList.isEmpty()) {
			return postEntity.member.eq(member);
		}

		return postEntity.member.username.in(followingList)
			.or(postEntity.member.eq(member));
	}

	private BooleanExpression isRecommendableToMember(MemberEntity member) {
		return isNotFollowingPostAuthor(member)
			.and(isNotAuthorOfPost(member));
	}

	private static JPQLQuery<Long> commentCountByPost() {
		return JPAExpressions.select(commentEntity.count())
			.from(commentEntity)
			.where(commentEntity.post.eq(postEntity));
	}

	private BooleanExpression findPostsBeforeId(Long lastPostId) {
		if (lastPostId == 0L) {
			return postEntity.createDate.before(LocalDateTime.now());
		}
		return postEntity.id.lt(lastPostId);
	}
}
