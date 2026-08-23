package net.friendly_bets.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Зарегистрированный пользователь")
public class UserSimpleDto {

    @Schema(description = "идентификатор пользователя", example = "12-байтовый хэш ID")
    private String id;

    @Schema(description = "имя пользователя", example = "example_name")
    private String username;

    @Schema(description = "аватар пользователя в формате base64")
    private String avatar;

    public static UserSimpleDto from(User user) {
        return from(user, true);
    }

    public static UserSimpleDto from(User user, boolean includeAvatar) {
        if (user == null) {
            return null;
        }
        return UserSimpleDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .avatar(includeAvatar && user.getAvatar() != null ?
                        Base64.getEncoder().encodeToString(user.getAvatar().getData()) : null)
                .build();
    }

    public static List<UserSimpleDto> from(List<User> users) {
        return from(users, true);
    }

    public static List<UserSimpleDto> from(List<User> users, boolean includeAvatar) {
        if (users == null) {
            return List.of();
        }
        return users.stream()
                .map(user -> from(user, includeAvatar))
                .collect(Collectors.toList());
    }
}
