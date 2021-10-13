package com.kdanwoo.study.querydsl;

import com.kdanwoo.study.querydsl.entity.Member;
import com.kdanwoo.study.querydsl.entity.QMember;
import com.kdanwoo.study.querydsl.entity.Team;
import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.util.List;

import static com.kdanwoo.study.querydsl.entity.QMember.member;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    /**
     * JPAQueryFactory를 필드로 제공하면 동시성 문제는 어떻게 될까?
     * 동시성 문제는 JPAQueryFactory를 생성할 때 제공하는 EntityManager(em)에 달려있다.
     * 스프링 프레임워크는 여러 쓰레드에서 동시에 같은 EntityManager에 접근해도,
     * 트랜잭션 마다 별도의 영속성 컨텍스트를 제공하기 때문에, 동시성 문제는 걱정하지 않아도 된다.
     * */
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

    @Test
    public void search(){
        Member member = queryFactory.selectFrom(QMember.member)
                .where(QMember.member.username.eq("member1").and(QMember.member.age.eq(10)))
                .fetchOne();

        assertThat(member.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search1(){
        Member member = queryFactory.selectFrom(QMember.member)
                .where(QMember.member.username.eq("member1").and(QMember.member.age.eq(10)))
                .fetchOne();

        assertThat(member.getUsername()).isEqualTo("member1");
    }

    /**
     * where() 에 파라미터로 검색조건을 추가하면 AND 조건이 추가됨
     * 이경우 null 값은무시 메서드추출을활용해서동적쿼리를깔끔하게만들수있음 뒤에서설명
     * */
    @Test
    public void searchAndParam(){
        List<Member> member1 = queryFactory.selectFrom(member)
                .where(member.username.eq("member1"), member.age.eq(10)).fetch();

        assertThat(member1.get(0).getUsername()).isEqualTo("member1");

    }

    @Test
    public void resultFetch(){

        //리스트로 조회한다.
        List<Member> fetch = queryFactory.selectFrom(member).fetch();

        //단 건 엔티티 객체로 조회한다
        //Member member = queryFactory.selectFrom(QMember.member).fetchOne();

        //처음 한 건 조회
        Member memberFirst = queryFactory.selectFrom(QMember.member).fetchFirst();

        //페이징에서 사용
        QueryResults<Member> results = queryFactory
                .selectFrom(QMember.member)
                .fetchResults(); //쿼리가 2번 나감, totalcount 때문에

        System.out.println("results = " + results.getResults());

        //count 쿼리로 변경
        long count = queryFactory
                .selectFrom(QMember.member)
                .fetchCount();

    }

    /**
     *회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     * */
    @Test
    public void sortTest(){
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

}
