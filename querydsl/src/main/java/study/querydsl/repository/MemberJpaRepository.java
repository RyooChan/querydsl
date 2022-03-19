package study.querydsl.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static study.querydsl.entity.QMember.*;

@Repository
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public MemberJpaRepository(EntityManager em) {
        // 순수 JPA를 사용하기 때문에 entityManager 필요.
        this.em = em;
        // querydsl을 사용할 때에 entitymanager를 받아와서 사용하도록 생성자 생성
        this.queryFactory = new JPAQueryFactory(em);
    }

    // QuerydslApplication에 bean으로 queryFactory 등록시 바로 injection 받기 가능.
    // 이렇게 쓰면 이제 @RequiredArgument사용가능
//    public MemberJpaRepository(EntityManager em, JPAQueryFactory queryFactory) {
//        this.em = em;
//        this.queryFactory = queryFactory;
//    }

    public void save(Member member){
        em.persist(member);
    }

    public Optional<Member> findById(Long id){
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }

    public List<Member> findAll(){
        return em.createQuery("select m from Member m", Member.class).getResultList();
    }
    public List<Member> findAll_Querydsl(){
        return queryFactory
                .selectFrom(member)
                .fetch();
    }

    public List<Member> findByUsername(String username){
        return em.createQuery("select m from Member m where m.username = :username ", Member.class)
                .setParameter("username", username)
                .getResultList();
    }
    public List<Member> findByUsername_Qeurydsl(String username){
        return queryFactory
                .selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }

}
