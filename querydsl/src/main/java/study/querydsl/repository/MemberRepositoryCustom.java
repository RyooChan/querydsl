package study.querydsl.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;

import java.util.List;

public interface MemberRepositoryCustom {
    List<MemberTeamDto> searchByWhere(MemberSearchCondition condition);
    Page<MemberTeamDto> searchByWherePageSimple(MemberSearchCondition condition, Pageable pageable);
    Page<MemberTeamDto> searchByWherePageComplex(MemberSearchCondition condition, Pageable pageable);
}
