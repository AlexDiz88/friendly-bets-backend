package net.friendly_bets.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import net.friendly_bets.dto.ImageDto;
import net.friendly_bets.security.details.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@Tags(value = {
        @Tag(name = "Files")
})
public interface FilesApi {

    @Operation(summary = "Сохранение/изменение изображения пользователя", description = "Доступно только зарегистрированному пользователю")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Сохранённый/обновленный файл изображения",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ImageDto.class))
                    }
            )
    })
    ResponseEntity<ImageDto> saveAvatarImage(@Parameter(hidden = true) AuthenticatedUser authenticatedUser,
                                             MultipartFile image);


    @Operation(summary = "Сохранение/изменение логотипа команды", description = "Доступно только администратору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Сохранённый/обновленный файл изображения",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ImageDto.class))
                    }
            )
    })
    ResponseEntity<ImageDto> saveLogoImage(@Parameter(hidden = true) AuthenticatedUser authenticatedUser,
                                           @Parameter(description = "идентификатор карточки помощи") String teamId,
                                           MultipartFile image);
}
