package me.dodo.readingnotes.service;

import jakarta.servlet.http.HttpServletRequest;
import me.dodo.readingnotes.domain.User;
import me.dodo.readingnotes.repository.UserRepository;
import me.dodo.readingnotes.util.ApiKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static me.dodo.readingnotes.util.RequestUtils.getCurrentHttpRequest;

@Service
public class CustomOAuth2UserService implements
        OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final UserAuthLogService userAuthLogService;
    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    public CustomOAuth2UserService(UserRepository userRepository,
                                   UserAuthLogService userAuthLogService) {
        this.userRepository = userRepository;
        this.userAuthLogService = userAuthLogService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // request 가져오기
        HttpServletRequest httpRequest = getCurrentHttpRequest();

        // DefaultOAuth2UserService = 유저 정보 로딩하는 서비스
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        // userRequest에 있는 액세스 토큰으로 사용자 정보를 요청하여 OAuth2User 형태로 받아옴. (attributes에 email,name 등이 담김)
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // atrributes 추출
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 로그인할 서비스 구분(ex. google, naver, kakao)
        // registrasion은 application-oauth.properties에 등록한 소셜 로그인 ID
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        String email, name, providerId;

        String userNameAttribute = null;

        if ("naver".equals(registrationId)) {
            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            email = (String) response.get("email");
            name = (String) response.get("name");
            providerId = (String) response.get("id");
            attributes = response; // 저장용 attributes도 바꿔줌
            userNameAttribute = "id";
        } else if ("google".equals(registrationId)) {
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
            providerId = (String) attributes.get("sub");
            userNameAttribute = "sub";
        } else if ("kakao".equals(registrationId)) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            email = (String) kakaoAccount.get("email");
            name = (String) profile.get("nickname");
            providerId = String.valueOf(attributes.get("id"));
            userNameAttribute = "id";
        }
        else {
            userAuthLogService.logLoginFail(null, null, registrationId, "지원하지 않는 소셜 로그인입니다.", httpRequest);
            throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다: " + registrationId);
        }

        // OAuth2 로그인 진행 시 기본키가 되는 필드 = Primary Key
//        String userNameAttribute = userRequest.getClientRegistration()
//                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
//        log.debug("userNameAttribute: {}", userNameAttribute);

        // 직접 attributes를 생성하여 평탄화함.
        Map<String, Object> customAttributes = new HashMap<>();
        customAttributes.put("email", email);
        customAttributes.put("name", name);
        customAttributes.put("provider", registrationId);
        customAttributes.put(userNameAttribute, providerId);
        customAttributes.put("original", attributes);

        // 일반 회원가입한 이메일로 로그인 시 소셜 로그인 거부
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getProvider() == null || !user.getProvider().equals(registrationId)) {
                throw new IllegalArgumentException("이미 가입된 이메일입니다.");
            }
        });

        // api_key 생성
        String api_key = ApiKeyGenerator.generate();
        if (api_key != null){
//            log.info("api_key:" + api_key.substring(0,8));
        } else{
            log.warn("api_key가 null임.");
        }

        // DB에 저장
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(
                        User.fromSocial(email, name, registrationId, providerId, api_key)
                ));
        log.debug("user: {}", user);

        // 소셜 로그인 로그 저장
        userAuthLogService.logLoginSuccess(user, email, registrationId, httpRequest);

        // 객체로 만들어 전달
        return new DefaultOAuth2User(
                // 권한 지정 = ROLE_USER
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                // 불러온 사용자 정보
                customAttributes,
                // 식별자로 삼을 key
                userNameAttribute
        );
    }
}
