package study.querydsl.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.repository.support.Querydsl4RepositorySupport;

import java.util.List;

import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.team;

@Repository
public class MemberTestRepository extends Querydsl4RepositorySupport {

    public MemberTestRepository(){
        super(Member.class);
    }

    public List<Member> basicSelect(){
        return select(member)
                .from(member)
                .fetch();
    }

    public List<Member> basicSelectFrom(){
        return selectFrom(member)
                .fetch();
    }

    /**
     * searchByWherePageSimple과 대응되는 방법이다.
     * 한번에 count까지 진행해온다.
     * @param condition
     * @param pageable
     * @return
     */
    public Page<Member> searchPageApplyPage(MemberSearchCondition condition, Pageable pageable){
        JPAQuery<Member> query = selectFrom(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername())
                        , teamNameEq(condition.getTeamName())
                        , ageGoe(condition.getAgeGoe())
                        , ageLoe(condition.getAgeLoe())
                );

        List<Member> content = getQuerydsl().applyPagination(pageable, query).fetch();

        return PageableExecutionUtils.getPage(content, pageable, query::fetchCount);
    }

    // 사실 위의 코드와 완전히 동일한 로직의 코드이다.
    // Querydsl4RepositorySupport쪽에서 위의 코드의 applypagination부분을 한번더 적용해서 진행해주었다.
    // 이 덕분에 훨씬 코드가 깔끔해졌다.
    public Page<Member> applyPagination(MemberSearchCondition condition, Pageable pageable){
        return applyPagination(pageable, query ->query
                        .selectFrom(member)
                        .leftJoin(member.team, team)
                        .where(
                                usernameEq(condition.getUsername())
                                , teamNameEq(condition.getTeamName())
                                , ageGoe(condition.getAgeGoe())
                                , ageLoe(condition.getAgeLoe())
                        )
        );
    }

    /**
     * searchByWherePageComplex 과 대응되는 방법이다.
     * 위의 방식과 동일하게 값 가져오기, countQuery 두개가 따로따로 진행된다.
     * 하지만 그것보다 훨씬 코드가 짧고 보기 편한것을 알 수 있다.
     * @param condition
     * @param pageable
     * @return
     */
    public Page<Member> applyPagination2(MemberSearchCondition condition, Pageable pageable){
        return applyPagination(pageable, contentQuery -> contentQuery
                        .selectFrom(member)
                        .leftJoin(member.team, team)
                        .where(
                                usernameEq(condition.getUsername())
                                , teamNameEq(condition.getTeamName())
                                , ageGoe(condition.getAgeGoe())
                                , ageLoe(condition.getAgeLoe())

                        )
                        , countQuery -> countQuery
                        .select(member.id)
                        .from(member)
                        .leftJoin(member.team, team)
                        .where(
                                usernameEq(condition.getUsername())
                                , teamNameEq(condition.getTeamName())
                                , ageGoe(condition.getAgeGoe())
                                , ageLoe(condition.getAgeLoe())
                        )
        );
    }

    private BooleanExpression usernameEq(String username) {
        return StringUtils.hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return StringUtils.hasText(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }

}
