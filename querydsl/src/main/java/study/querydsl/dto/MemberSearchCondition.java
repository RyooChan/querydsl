package study.querydsl.dto;

import lombok.Data;

@Data
public class MemberSearchCondition {
    // 회원명, 팀명, 나이(ageGoe, ageLoe)

    private String username;
    private String teamName;
    private Integer ageGoe;     // 값이 null이 될 수도 있으니 Integer 사용.
    private Integer ageLoe;
}
