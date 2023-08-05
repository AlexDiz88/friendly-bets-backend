package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.User;

import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Зарегистрированный пользователь")
public class UserDto {

    @Schema(description = "идентификатор пользователя", example = "12-байтовый хэш ID")
    private Long id;

    @Schema(description = "е-мейл пользователя", example = "example@gmail.com")
    private String email;

    @Schema(description = "роль пользователя", example = "USER")
    private String role;

    @Schema(description = "имя пользователя", example = "example_name")
    private String username;

    @Schema(description = "аватар пользователя", example = "avatar.jpeg")
    private String avatar;


    public static UserDto from(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole().toString())
                .username(user.getUsername())
                .avatar(user.getAvatar())
                .build();
    }

    public static List<UserDto> from(List<User> users) {
        return users.stream()
                .map(UserDto::from)
                .collect(Collectors.toList());
    }
}
