package com.kdanwoo.study.querydsl;

import com.kdanwoo.study.querydsl.entity.Member;
import com.kdanwoo.study.querydsl.entity.QMember;
import com.kdanwoo.study.querydsl.entity.Team;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static com.kdanwoo.study.querydsl.entity.QMember.member;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;


    @BeforeEach //테스트 실행전에 수행되는 로직임.
    public void before(){
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1",10, teamA);
        Member member2 = new Member("member2",20, teamA);

        Member member3 = new Member("member3",30, teamB);
        Member member4 = new Member("member4",40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

    }

    @Test
    public void startJPQL(){
        //member1 찾아라.
        Member findByJPQL = em.createQuery("select m from Member m where m.username = :username",Member.class)
                .setParameter("username", "member1").getSingleResult();

        assertThat(findByJPQL.getUsername()).isEqualTo("member1");

    }

    @Test
    public void startQuerydsl(){
        //member1을 찾아라.
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember m = new QMember("m");

        //JPQL: 문자(실행 시점 오류),
        // Querydsl: 코드(컴파일 시점 오류)
        // JPQL: 파라미터 바인딩 직접, Querydsl: 파라미터 바인딩 자동 처리
        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))//파라미터 바인딩 처리
                .fetchOne(); //왜 fetchOne?


        /**
         * fetch : 조회 대상이 여러건일 경우. 컬렉션 반환
         * fetchOne : 조회 대상이 1건일 경우(1건 이상일 경우 에러). generic에 지정한 타입으로 반환
         * fetchFirst : 조회 대상이 1건이든 1건 이상이든 무조건 1건만 반환. 내부에 보면 return limit(1).fetchOne() 으로 되어있음
         * fetchCount : 개수 조회. long 타입 반환
         * fetchResults : 조회한 리스트 + 전체 개수를 포함한 QueryResults 반환. count 쿼리가 추가로 실행된다.
         * */
        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void startQuerydsl3(){
        Member findMember = queryFactory
                .select(member) //static import
                .from(member)
                .where(member.username.eq("member1"))//파라미터 바인딩 처리
                .fetchOne(); //왜 fetchOne?

        assertThat(findMember.getUsername()).isEqualTo("member1");

    }
}
