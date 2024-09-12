package net.friendly_bets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.friendly_bets.models.User;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Зарегистрированный пользователь")
public class UserSimpleDto {

    @Schema(description = "идентификатор пользователя", example = "12-байтовый хэш ID")
    private String id;

    @Schema(description = "имя пользователя", example = "example_name")
    private String username;

    @Schema(description = "аватар пользователя в формате base64")
    private String avatar;

    public static UserSimpleDto from(User user) {
        return UserSimpleDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .avatar(user.getAvatar() != null ?
                        Base64.getEncoder().encodeToString(user.getAvatar().getData()) : null)
                .build();
    }

    public static List<UserSimpleDto> from(List<User> users) {
        return users.stream()
                .map(UserSimpleDto::from)
                .collect(Collectors.toList());
    }
}
