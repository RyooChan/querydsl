package study.querydsl.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.repository.MemberJpaRepository;
import study.querydsl.repository.MemberRepository;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberJpaRepository memberJpaRepository;
    private final MemberRepository memberRepository;

    /**
     * 순수 JPA : 동적 쿼리 성능 최적화 - Dto, where을 통한 조회 API 컨트롤러
     * @param condition
     * @return
     */
    @GetMapping("/v1/members")
    public List<MemberTeamDto> searchMemberV1(MemberSearchCondition condition){
        return memberJpaRepository.searchByWhere(condition);
    }

    /**
     * Spring data JPA : 동적 쿼리 성능 최적화 - Dto, where을 통한 조회 API 컨트롤러
     * fetchResults()를 이용한 페이징 구현.
     * @param condition
     * @return
     */
    @GetMapping("/v2/members")
    public Page<MemberTeamDto> searchMemberV2(MemberSearchCondition condition, Pageable pageable){
        return memberRepository.searchByWherePageSimple(condition, pageable);
    }

    /**
     * Spring data JPA : 동적 쿼리 성능 최적화 - Dto, where을 통한 조회 API 컨트롤러
     * 두번의 쿼링을 통한 페이징 구현.
     * 첫장, 마지막장의 로직 확인을 통한 페이징 최적화 추가구현.
     * @param condition
     * @return
     */
    @GetMapping("/v3/members")
    public Page<MemberTeamDto> searchMemberV3(MemberSearchCondition condition, Pageable pageable){
        return memberRepository.searchByWherePageComplex(condition, pageable);
    }
}
