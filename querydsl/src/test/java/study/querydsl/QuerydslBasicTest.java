package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;


    @BeforeEach
    public void before(){
        // 미리 factory를 생성해서 여기저기서 사용 가능하다. 동시성 문제도 생기지 않는다.
        queryFactory = new JPAQueryFactory(em);

        // 테스트 실행 전 기본 데이터 입력
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL () throws Exception {
        // member1찾기
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() throws Exception {

        // sql과 비슷한 문법으로 사용하였다.
        // 그리고 파라미터 바인딩을 사용하지 않고, 바로 eq를 사용해서 파라미터가 바인딩된다.
        Member findMember = queryFactory
                .select(member)     // (QMember.member)을 사용하여 static import -> member
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() throws Exception {
        // 바로 조건으로 검색도 가능하다.
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1").and(member.age.eq(10))
                        // and의 경우는 member.username.eq("member1"), (member.age.eq(10)) 이렇게도 가능.
                ).fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("memeber1");
    }

    @Test
    public void resultFetch() throws Exception {
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        Member fetchOne = queryFactory.selectFrom(member).fetchOne();

        Member fetchFirst = queryFactory.selectFrom(member).fetchFirst();

        // 이건 쿼리가 2번 실행된다. 1. results가져오기 2. totalcount가져오기(count)
        QueryResults<Member> results = queryFactory.selectFrom(member).fetchResults();
        results.getTotal();
        List<Member> content = results.getResults();
    }

    /*
        회원 정렬 순서
        1. 회원 나이 내림차순(desc)
        2. 회원 이름 올림차순(asc)
        단 2에서 회원 이름이 없으면 마지막에 출력 (nulls last)
     */
    @Test
    public void sort() throws Exception {
        // 정렬 확인을 위한 데이터 추가
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1() throws Exception {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    // 이 방식은 바로 2번의 쿼리가 한번에 나간다.
    // 실무에서는 2번째의 count 쿼리는 더 단순하게 짜서 진행할 수 있으면 따로 작성하는 것이 좋다.
    // 예를 들어 제약조건이 붙는 경우 여기서는 양쪽 쿼리에 다 붙기 때문이다.
    @Test
    public void paging2() throws Exception {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }

    // 집합 dsl
    @Test
    public void aggregation() throws Exception {
        // 해당 쿼리의 결과로는 여러 개의 타입의 결과가 들어올 것이다.
        // count, sum, avg, max, min .... 이렇게 여러가지이다.
        // 이럴 때에 Tuple을 사용한다.
        // 그리고 실무에서는 Tuple을 많이 쓰지는 않고, DTO로 뽑는다.
        List<Tuple> result = queryFactory
                .select(
                        member.count()
                        , member.age.sum()
                        , member.age.avg()
                        , member.age.max()
                        , member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);

    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라
     */
    @Test
    public void group() throws Exception {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
//                .having(member.age.gt(10))  // 이 groupBy한 member중 나이가 10살 이상인 사람을 뽑아라.
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * 기본 조인 예제
     * teamA에 소속된 모든 회원 찾기
     */
    @Test
    public void join() throws Exception {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");

//        select
//        member0_.member_id as member_i1_1_,
//                member0_.age as age2_1_,
//        member0_.team_id as team_id4_1_,
//                member0_.username as username3_1_
//        from
//        member member0_
//        inner join
//        team team1_
//        on member0_.team_id=team1_.id
//        where
//        team1_.name=?
//        요렇게 찾아옴
    }

    /**
     * 세타 조인
     * 회원의 이름이 팀 이름과 같은 회원 조회(딱봐도 실제로 연관관계가 없을만한 것을 조회하기)
     * @throws Exception
     */
    // 이 세타 조인은 outer join이 불가능하다. -> 조인 on을 사용하여 outer join을 가능하게 한다.
    @Test
    public void theta_join() throws Exception {

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    // 조인 - on절
    // 1. 조인 대상 필터링
    // 2. 연관관계 없는 대상 필터링

    /**
     * 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회하기.
     * JPQL : select m , t from Member m left join m.team t on t.name = 'teamA'
     * @throws Exception
     */
    @Test
    public void join_on_filtering() throws Exception {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
//            tuple = [Member(id=3, username=member1, age=10), Team(id=1, name=teamA)]
//            tuple = [Member(id=4, username=member2, age=20), Team(id=1, name=teamA)]
//            tuple = [Member(id=5, username=member3, age=30), null]
//            tuple = [Member(id=6, username=member4, age=40), null]
        }
    }

    /**
     * 연관관계가 없는 엔티티 외부 조인
     * 회원의 이름이 팀 이름과 같은 대상을 외부 조회(딱봐도 실제로 연관관계가 없을만한 것을 조회하기)
     * @throws Exception
     */
    // 이 세타 조인은 outer join이 불가능하다. -> 조인 on을 사용하여 outer join을 가능하게 한다.
    @Test
    public void join_on_no_relation() throws Exception {

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))       // 싹다 조인한 뒤에 on절로 다시 필터링 진행함.
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
//            tuple = [Member(id=3, username=member1, age=10), null]
//            tuple = [Member(id=4, username=member2, age=20), null]
//            tuple = [Member(id=5, username=member3, age=30), null]
//            tuple = [Member(id=6, username=member4, age=40), null]
//            tuple = [Member(id=7, username=teamA, age=0), Team(id=1, name=teamA)]
//            tuple = [Member(id=8, username=teamB, age=0), Team(id=2, name=teamB)]
//            tuple = [Member(id=9, username=teamC, age=0), null]
        }
    }


    @PersistenceUnit
    EntityManagerFactory emf;

    //페치조인 미적용
    @Test
    public void fetchJoinNo() throws Exception {
        // fetch join의 경우 테스트에서 영속성 컨텍스트에 남아있는 데이터를 다 지워주지 않으면 결과를 보기 어렵다.
        // 그래서 영속성 컨텍스트 반영 후 비운 뒤 테스트 진행
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        // isLoaded는 해당 getTeam()이 초기화 되었는지 아닌지를 가르쳐주는 엔티티이다.
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isTrue();
    }

    // 페치조인 적용
    @Test
    public void fetchJoinUse() throws Exception {
        // fetch join의 경우 테스트에서 영속성 컨텍스트에 남아있는 데이터를 다 지워주지 않으면 결과를 보기 어렵다.
        // 그래서 영속성 컨텍스트 반영 후 비운 뒤 테스트 진행
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()   // 페치조인 적용. 이게 적용되었으므로 isLoaded는 true가 나올것이다.
                .where(member.username.eq("member1"))
                .fetchOne();

        // isLoaded는 해당 getTeam()이 초기화 되었는지 아닌지를 가르쳐주는 엔티티이다.
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isTrue();
    }

    /**
     * 서브쿼리
     * 나이가 가장 많은 회원 조회하기
     */
    @Test
    public void subQuery() throws Exception {

        // alias가 겹치는 경우 다른거 하나 생성하기!
        QMember memberSub = new QMember("memberSub");

        // 서브쿼리에서 최대의 나이를 가져오고(40)
        // 그 나이를 갖는 사람을 가져오는것이다.
        // 사실 한쿼리로 가능한데 서브쿼리 연습을 위해 사용
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions.select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    /**
     * 서브쿼리
     * 나이가 평균 이상인 회원
     */
    @Test
    public void subQueryGoe() throws Exception {

        // alias가 겹치는 경우 다른거 하나 생성하기!
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe( // goe -> 크거나 같음(>=)
                        JPAExpressions.select(memberSub.age.avg())  // 나이 평균 가져오기
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30, 40);       // 평균보다 큰 나이는 30, 40 두개임.
    }

    /**
     * 서브쿼리
     * 서브쿼리 in 사용(중요함)
     * 나이가 평균 이상인 회원
     */
    @Test
    public void subQueryIn() throws Exception {

        // alias가 겹치는 경우 다른거 하나 생성하기!
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions      // 해당 서브쿼리는 10살 초과의 나이를 들고온다. 20, 30, 40살 들고옴.
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))        // gt -> (초과 > )
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);       // 평균보다 큰 나이는 30, 40 두개임.
    }

    /**
     * 서브쿼리
     * 서브쿼리에서 select하기
     */
    @Test
    public void selectSubQuery() throws Exception {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        JPAExpressions      // 참고로 이거 static import 된다. 나중에 함 해볼것
                                .select(memberSub.age.avg())
                                .from(memberSub)
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * JPA사용시 서브쿼리의 한계
     * JPA서브쿼리는 from절의 서브쿼리가 안된다.(인라인 뷰 미지원)
     * 당연히 Querydsl도 지원하지 않는다.
     * 하이버네이트 구현체를 사용하면 select절의 서브쿼리는 지원한다.
     * Queryldsl도 하이버네이트 구현체를 사용하면 select절의 서브쿼리를 지원한다.
     */

    /**
     * from절 서브쿼리 해결방안
     * 1. 서브쿼리를 join으로 변경한다(가능한 상황도 있고, 불가능한 상황도 있다.)
     * 2. 애플리케이션에서 쿼리를 2번 분리해서 사용한다.
     * 3. nativeSQL을 사용한다. -> 이게 바로 JPA의 한계이다...
     */

    //---------------------------------------------------------------

    /**
     * case문 기초
     * @throws Exception
     */
    @Test
    public void basicCase() throws Exception {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 복잡한 case문
     * @throws Exception
     */
    @Test
    public void complexCase() throws Exception {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 상수 더하기
     */
    @Test
    public void constant() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 문자 더하기
     */
    @Test
    public void concat() throws Exception {

        // {username}_{age} 로 문자를 더해서 만들어주기.
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))   // age는 문자가 아니므로 stringValue()를 사용하여 문자화
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 프로젝션 : select 대상 지정
     * 프로젝션시 대상이 하나이면 타입을 명확하게 지정할 수 있다.
     * @throws Exception
     */
    @Test
    public void simpleProjection() throws Exception {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 둘 이상의 대상 프로젝션
     * Tuple은 프로젝션 대상이 여러 종류일 때 이를 따로따로 꺼낼 수 있도록 도와준다.
     * @throws Exception
     */
    /**
     * 튜플은 확인해 보면, querydsl.core에 존재한다.
     * 그렇기 때문에 이 튜플을 repository 밖(service, controller)에서 사용하는 것은 좋은 설계가 아니다.
     * 뒷단에서 JPA나 querydsl을 사용한다는 것을 핵심 비즈니스 서비스가 있는 앞단에서 아는것은 별로 좋지 않다.
     * 만약 이후에 해당 기술을 변경할 일이 생겼을 경우, 비즈니스 로직에서 이 tuple을 사용하지 않으면 유연하게 대처할 수 있을 것이다.
     * 따라서 앞단에서 사용할 때에는 DTO로 변경하여 사용하는 것이 권장된다.
     */
    @Test
    public void tupleProjection() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    /**
     * 기존의 JPQL을 사용하여 Dto를 프로젝션 하는 방법 -> new를 사용하여 마치 생성자를 만들듯이 가져온다.
     * dto의 package이름을 다 적어줘야 해서 굉장히 귀찮은 방식이다.
     * 생성자 방식만 지원한다 -> setter를 이용하거나 필드 주입이 불가능하다.
     * @throws Exception
     */
    @Test
    public void findDtoByJPQL() throws Exception {
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * querydsl은 위의 JPQL의 단점을 극복한 3가지 방법을 지원해 준다.
     * 1. 프로퍼티 접근 - setter
     * 2. 필드 직접 접근
     * 3. 생성자 사용
     */

    /**
     * 1. 프로퍼티 접근 방법(setter)
     * setter을 통해 값을 넣어주는 방식이다.
     */
    @Test
    public void findDtoBySetter() throws Exception {
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class
                        , member.username
                        , member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("result = " + memberDto);
        }
    }

    /**
     * 2. 필드 접근 방법
     * getter setter 필요없이 바로 필드에 값을 꽂아버린다.
     */
    @Test
    public void findDtoByField() throws Exception {
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class
                        , member.username
                        , member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("result = " + memberDto);
        }
    }

    /**
     * 3. 생성자 방식
     * 생성자 호출 방식이기 때문에 객체들의 타입이 딱 맞아야 한다.
     * 실제로 만들어진 생성자를 호출시켜서 진행하는 방법이다.
     * 생성자 방식은 컴파일 도중에 에러를 찾을 수 없고, 런타임 에러로 발생하게 된다.
     */
    @Test
    public void findDtoByConstructor() throws Exception {
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class
                        , member.username
                        , member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("result = " + memberDto);
        }
    }

    /**
     * 필드 접근 - projection 사용 별칭 부여
     * 가져오는 값과 저장되는 값의 이름이 다른 경우, 별칭을 사용하여 변환해주면 잘 들어간다.
     * memberDto -> username / userDto -> name
     * memberDto의 username을 name으로 변경.
     */
    @Test
    public void findUserDtoByField() throws Exception {
        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class
                        , member.username.as("name")
                        /**
                         * subquery를 사용하여 나이를 모두 최대나이로 맞추려고 한다.
                         * 해당 예제를 사용한 이유는 서브쿼리에서 별칭 사용에 관해 알기 위함이다.(subquery alias)
                         * 서브쿼리에서 가져온 값을 감싸서 alias해 줄 수 있다. -> 이를 통해 매칭
                         * subquery는 이렇게 ExpressionUtils로 감싸서 해당 값에 alias해두어야 한다.
                         */
                        , ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age")
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userdto = " + userDto);
        }
    }

    /**
     * 생성자 - projection 사용 별칭 부여
     * 생성자는 만들어진 생성자에 값을 넣으므로 따로 별칭을 사용하지 않고도 타입이 맞으면 잘 들어간다.
     * @throws Exception
     */
    @Test
    public void findUserDtoByConstructor() throws Exception {
        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class
                        , member.username
                        , member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("result = " + userDto);
        }
    }

    /**
     * DTO에 @QueryProjection을 지정하여 사용하는 방법 - 이 방식은 3번째 constructor사용 방식을 대체한다.
     * 매우 쉬운 방법이고, 컴파일 시점에 바로 에러 확인도 가능하다.
     * 단, 한가지 고민거리가 생기는데 dto가 querydsl과 관련한 의존성을 갖게 된다.
     * 이후 querydsl을 빼게 되면 이런 dto들이 영향을 받게 될 것이다.
     * 그리고 dto는 여러 레이어에 걸쳐서 돌아다니는데 그 dto안에 querydsl이 들어가 있게 된다.
     * 1, 2, 3번째 방식 중 본인이 맞다고 생각하는 내용을 취사선택하면 된다.
     * @throws Exception
     */
    @Test
    public void findBuQueryProjection() throws Exception {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

}
