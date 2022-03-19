package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.persistence.EntityManager;

@SpringBootApplication
public class QuerydslApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuerydslApplication.class, args);
	}

	// querydsl 바로 사용을 위한 bean 등록
	// 따로 적용받아서 사용하는 방식을 사용중이므로 주석처리함.
//	@Bean
//	JPAQueryFactory jpaQueryFactory(EntityManager em){
//		return new JPAQueryFactory(em);
//	}

}
