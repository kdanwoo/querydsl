package com.kdanwoo.study.querydsl;

import com.kdanwoo.study.querydsl.entity.Hello;
import com.kdanwoo.study.querydsl.entity.QHello;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

@SpringBootTest
@Transactional
class QuerydslApplicationTests {

    @Autowired
    EntityManager em;

    @Test
    void contextLoads() {

        Hello hello = new Hello();
        em.persist(hello);

        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        QHello qHello = QHello.hello; //Querydsl Q타입 동작 확인

        Hello result = queryFactory.selectFrom(qHello).fetchOne();

        Assertions.assertThat(result).isEqualTo(hello);

        //lombok 동작확인
        Assertions.assertThat(result.getId()).isEqualTo(hello.getId());
    }

}
