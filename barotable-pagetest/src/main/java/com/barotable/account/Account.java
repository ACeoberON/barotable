package com.barotable.account;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 계정: 로그인하는 모든 사람 (손님, 점주, 관리자를 권한으로 구분) */
@Entity
@Table(name = "account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 손님 회원가입 (비밀번호는 해시로만 저장) */
    public static Account customer(String email, String passwordHash, String name, String phone) {
        Account a = new Account();
        a.email = email;
        a.passwordHash = passwordHash;
        a.name = name;
        a.phone = phone;
        a.role = Role.CUSTOMER;
        a.status = AccountStatus.ACTIVE;
        return a;
    }
}
