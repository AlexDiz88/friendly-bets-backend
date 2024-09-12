package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.ImageDto;
import net.friendly_bets.dto.StandardResponseDto;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.User;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.repositories.UsersRepository;
import net.friendly_bets.services.impl.S3Service;
import org.bson.types.Binary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static net.friendly_bets.utils.Constants.AWS_AVATARS_FOLDER;
import static net.friendly_bets.utils.GetEntityOrThrow.getTeamOrThrow;
import static net.friendly_bets.utils.GetEntityOrThrow.getUserOrThrow;

@RequiredArgsConstructor
@Service
public class FilesService {

    private final UsersRepository usersRepository;
    private final TeamsRepository teamsRepository;
    private final S3Service s3Service;

    @Value("${upload.path.avatars}")
    private String UPLOAD_PATH_AVATARS;

    @Value("${upload.path.logo}")
    private String UPLOAD_PATH_LOGO;

    public void s3uploadUserAvatar(String userId, MultipartFile file) throws IOException {
        String filenameWithExtension = userId + extractExtension(file.getOriginalFilename());
        String key = AWS_AVATARS_FOLDER + "/" + filenameWithExtension;

        s3Service.uploadFileToS3Bucket(key, file, filenameWithExtension);
    }

    @Transactional
    public StandardResponseDto saveAvatarImage(String currentUserId, MultipartFile file) {
        if (file == null) {
            throw new RuntimeException("imageIsNull");
        }
        User user = getUserOrThrow(usersRepository, currentUserId);

        try {
            Binary avatarImage = new Binary(file.getBytes());
            user.setAvatar(avatarImage);
            usersRepository.save(user);
        } catch (IOException e) {
            throw new RuntimeException("imageSaveError", e);
        }

        return StandardResponseDto.builder()
                .status(HttpStatus.OK.value())
                .message("avatarWasSuccessfullySaved")
                .build();
    }

    @Transactional
    public ImageDto saveLogoImage(String teamId, MultipartFile image) {
        Team team = getTeamOrThrow(teamsRepository, teamId);
        // TODO: проработать путь файла (пробелы, нижние подчеркивания)
        String fileName = team.getTitle();
        fileName = saveImage(image, fileName, UPLOAD_PATH_LOGO);
        System.out.println(fileName);
        team.setLogo(fileName);
        teamsRepository.save(team);

        return ImageDto.builder()
                .filename(fileName)
                .build();
    }

    private String saveImage(MultipartFile image, String fileName, String uploadPath) {
        if (image == null) {
            throw new RuntimeException("imageIsNull");
        }
        File uploadDir = new File(uploadPath);

        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        fileName = fileName + ".jpg";
        Path path = uploadDir.toPath().resolve(fileName);
        System.out.println("Сохранение файла в: " + path);
        try {
            image.transferTo(path);
        } catch (IOException e) {
            throw new RuntimeException("imageSaveError", e);
        }
        return fileName;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        return "";
    }
}
