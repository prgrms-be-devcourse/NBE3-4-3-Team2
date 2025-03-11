// package com.example.backend.global.config;
//
// import static com.example.backend.global.config.SpringDocConfig.*;
//
// import java.util.Collections;
// import java.util.List;
//
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
// import org.springframework.security.config.http.SessionCreationPolicy;
// import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.security.web.SecurityFilterChain;
// import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
// import org.springframework.web.cors.CorsConfiguration;
// import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//
// import com.example.backend.identity.security.config.handler.CustomAccessDeniedHandler;
// import com.example.backend.identity.security.config.handler.CustomAuthenticationEntryPoint;
// import com.example.backend.identity.security.config.handler.OAuth2FailureHandler;
// import com.example.backend.identity.security.config.handler.OAuth2SuccessHandler;
// import com.example.backend.identity.security.jwt.JwtAuthenticationFilter;
// import com.example.backend.identity.security.oauth.service.CustomOAuth2UserService;
//
// import lombok.RequiredArgsConstructor;
//
// /**
//  *
//  * 스프링 시큐리티 설정
//  * H2-console 페이지 허용
//  *
//  * @author Metronon
//  * @since 25. 1. 28.
//  */
// @Configuration
// @EnableWebSecurity
// @RequiredArgsConstructor
// public class SecurityConfig {
// 	private final JwtAuthenticationFilter jwtAuthenticationFilter;
// 	private final CustomAuthenticationEntryPoint authenticationEntryPoint;
// 	private final CustomAccessDeniedHandler accessDeniedHandler;
// 	private final CustomOAuth2UserService oAuth2UserService;
// 	private final OAuth2SuccessHandler authenticationSuccessHandler;
// 	private final OAuth2FailureHandler authenticationFailureHandler;
//
//
// 	@Bean
// 	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//
//
// 		http
// 			// ✅ H2 CONSOLE 허용
// 			.headers(headers -> headers
// 				.frameOptions(frameOptions -> frameOptions.disable()) // X-Frame-Options 비활성화
// 			)
//
// 			// ✅ 보안 관련 설정 (CSRF, CORS, 세션)
// 			.csrf(AbstractHttpConfigurer::disable) // CSRF 비활성화 (JWT 사용 시 불필요)
// 			.cors(cors -> corsConfigurationSource()) // CORS 설정 적용
// 			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 사용 안함 (JWT 방식)
//
// 			// ✅ 필터 설정 (JWT 인증 필터 → UsernamePasswordAuthenticationFilter)
// 			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
//
// 			// ✅ OAuth2 로그인 설정
// 			.oauth2Login(oauth2 -> oauth2
// 				.userInfoEndpoint(userInfoEndpointConfig -> userInfoEndpointConfig
// 					.userService(oAuth2UserService)) // 사용자 정보 서비스 설정
// 				.successHandler(authenticationSuccessHandler) // OAuth2 로그인 성공 핸들러
// 				.failureHandler(authenticationFailureHandler) // OAuth2 로그인 실패 핸들러
// 			)
//
// 			// ✅ 인증 및 접근 권한 설정
// 			.authorizeHttpRequests(authorize -> authorize
// 				.requestMatchers("/h2-console/**").permitAll() // H2 콘솔 허용
// 				.requestMatchers("/api-v1/notification/subscribe").permitAll() // SSE 엔드포인트 명시적 설정
// 				.requestMatchers("/error", "/favicon.ico").permitAll() // 프론트엔드에서 적용될 예외 포인트 설정
// 				.requestMatchers("/api-v1/members/login", "/api-v1/members/join", "/api-v1/members/logout").permitAll() // 로그인 & 회원가입 허용
// 				.requestMatchers(SWAGGER_PATHS).permitAll() // Swagger 문서 접근 허용
// 				.anyRequest().authenticated()) // 그 외 요청은 인증 필요
//
// 			// ✅ 기본 인증 방식 비활성화 (JWT 사용)
// 			.httpBasic(AbstractHttpConfigurer::disable) // HTTP Basic 인증 비활성화
// 			.formLogin(AbstractHttpConfigurer::disable) // 폼 로그인 비활성화
//
// 			// ✅ 예외 처리 설정
// 			.exceptionHandling(exceptionHandling -> exceptionHandling
// 				.authenticationEntryPoint(authenticationEntryPoint) // 인증이 수행되지 않았을 때 호출되는 핸들러
// 				.accessDeniedHandler(accessDeniedHandler)); // 접근 권한이 없을 때 호출되는 핸들러
//
// 		return http.build();
// 	}
//
// 	@Bean
// 	public UrlBasedCorsConfigurationSource corsConfigurationSource() {
// 		CorsConfiguration configuration = new CorsConfiguration();
// 		// 허용할 오리진 설정
// 		configuration.setAllowedOrigins(
// 			List.of(AppConfig.getSiteFrontUrl())); // 프론트 엔드
// 		// 허용할 HTTP 메서드 설정
// 		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE")); // 프론트 엔드 허용 메서드
// 		// 자격 증명 허용 설정
// 		configuration.setAllowCredentials(true);
// 		// 허용할 헤더 설정
// 		configuration.setAllowedHeaders(Collections.singletonList("*"));
//
// 		configuration.setExposedHeaders(List.of("Authorization","Set-Cookie")); // client가 Authorization 헤더를 읽을 수 있도록 해야한다.
//
//
// 		// CORS 설정을 소스에 등록
// 		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
// 		source.registerCorsConfiguration("/**", configuration);
// 		return source;
// 	}
//
// 	@Bean
// 	public PasswordEncoder passwordEncoder() { // SecurityConfig에 정의하면 순환참조
// 		return new BCryptPasswordEncoder();
// 	}
// }
