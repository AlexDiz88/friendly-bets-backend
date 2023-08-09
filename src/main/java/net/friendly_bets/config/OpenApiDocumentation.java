package net.friendly_bets.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import java.util.Collections;

public class OpenApiDocumentation {
    static Paths buildAuthenticationPath() {
        return new Paths()
                .addPathItem("/login", buildAuthenticationPathItem())
                .addPathItem("/logout", buildLogoutPathItem())
                .addPathItem("/auth/register", buildRegistrationPathItem());
    }

    private static PathItem buildLogoutPathItem() {
        return new PathItem().post(
                new Operation()
                        .addTagsItem("Authentication")
                        .responses(new ApiResponses()
                                .addApiResponse("200", new ApiResponse().description("Успешный выход"))));
    }
    private static PathItem buildAuthenticationPathItem() {
        return new PathItem().post(
                new Operation()
                        .addTagsItem("Authentication")
                        .requestBody(buildAuthenticationRequestBody())
                        .responses(new ApiResponses()
                                .addApiResponse("200",
                                        new ApiResponse()
                                                .description("Успешная аутентификация")
                                                .content(new Content().addMediaType("application/json",
                                                        new MediaType().schema(new Schema<>().$ref("StandardResponseDto"))))
                                                .headers(
                                                        Collections
                                                                .singletonMap("Set-Cookie",
                                                                        new Header()
                                                                                .example("JSESSIONID=1234")
                                                                                .description("Идентификатор сессии"))))
                                .addApiResponse("401",
                                        new ApiResponse()
                                                .description("Неверный логин или пароль")
                                                .content(new Content()
                                                        .addMediaType("application/json",
                                                                new MediaType()
                                                                        .schema(new Schema<>().$ref("StandardResponseDto")))))));
    }

    private static PathItem buildRegistrationPathItem() {
        return new PathItem().post(
                new Operation()
                        .addTagsItem("Registration")
                        .requestBody(buildRegistrationRequestBody())
                        .responses(new ApiResponses()
                                .addApiResponse("200",
                                        new ApiResponse()
                                                .description("Успешная регистрация")
                                                .content(new Content().addMediaType("application/json",
                                                        new MediaType().schema(new Schema<>().$ref("StandardResponseDto"))))
                                )
                                .addApiResponse("400",
                                        new ApiResponse()
                                                .description("Ошибка в данных регистрации")
                                                .content(new Content().addMediaType("application/json",
                                                        new MediaType().schema(new Schema<>().$ref("StandardResponseDto"))))
                                )
                        )
        );
    }

    static RequestBody buildAuthenticationRequestBody() {
        return new RequestBody().content(
                new Content()
                        .addMediaType("application/x-www-form-urlencoded",
                                new MediaType()
                                        .schema(new Schema<>()
                                                .$ref("EmailAndPassword"))));
    }

    static RequestBody buildRegistrationRequestBody() {
        return new RequestBody().content(
                new Content()
                        .addMediaType("application/x-www-form-urlencoded",
                                new MediaType()
                                        .schema(new Schema<>().$ref("RegistrationData"))));
    }

    static SecurityRequirement buildSecurity() {
        return new SecurityRequirement().addList("CookieAuthentication");
    }

    static Schema<?> emailAndPassword() {
        return new Schema<>()
                .type("object")
                .description("Email и пароль пользователя")
                .addProperty("username", new Schema<>().type("string"))
                .addProperty("password", new Schema<>().type("string"));
    }

    static SecurityScheme securityScheme() {
        return new SecurityScheme()
                .name("cookieAuth")
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name("JSESSOINID");
    }
}
