package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.friendly_bets.dto.ImageDto;
import net.friendly_bets.dto.StandardResponseDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.User;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.repositories.UsersRepository;
import org.bson.types.Binary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

import static net.friendly_bets.utils.Constants.AWS_AVATARS_FOLDER;
import static net.friendly_bets.utils.Constants.MAX_AVATAR_DIMENSION;
import static net.friendly_bets.utils.GetEntityOrThrow.getTeamOrThrow;
import static net.friendly_bets.utils.GetEntityOrThrow.getUserOrThrow;

@RequiredArgsConstructor
@Service
@Slf4j
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
        long maxAvatarSize = 2 * 1024 * 1024; // 2MB
        List<String> allowedMimeTypes = List.of("image/jpeg", "image/png", "image/webp", "image/heic", "application/octet-stream");

        checkFileParameters(file, maxAvatarSize, allowedMimeTypes);

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

    private void checkFileParameters(MultipartFile file, long maxAvatarSize, List<String> allowedMimeTypes) {
        if (file == null) {
            throw new BadRequestException("imageIsNull");
        }
        if (file.getSize() > maxAvatarSize) {
            throw new BadRequestException("fileSizeLimit");
        }
        if (!allowedMimeTypes.contains(file.getContentType())) {
            throw new BadRequestException("invalidFileFormat");
        }

        InputStream inputStream = null;
        try {
            inputStream = file.getInputStream();
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new BadRequestException("imageIsNull");
            }

            if (image.getWidth() > MAX_AVATAR_DIMENSION || image.getHeight() > MAX_AVATAR_DIMENSION) {
                throw new BadRequestException("imageDimensionsLimit");
            }
        } catch (IOException e) {
            throw new BadRequestException("errorReadingImage");
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
        }
    }

}
