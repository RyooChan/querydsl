package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import study.querydsl.entity.Member;

import java.util.List;

/**
 * Spring data JPA는 인터페이스이기 때문에 복잡한 쿼리를 구현하기 위해서는
 * 사용자 정의 리포지토리
 * 를 사용해야 한다.
 */
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom, QuerydslPredicateExecutor<Member> {
    // select m from Member m where m.username = ?
    List<Member> findByUsername(String username);
}
