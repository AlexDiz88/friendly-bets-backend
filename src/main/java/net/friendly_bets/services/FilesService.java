package net.friendly_bets.services;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.friendly_bets.dto.StandardResponseDto;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.models.User;
import net.friendly_bets.repositories.UsersRepository;
import org.bson.types.Binary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static net.friendly_bets.utils.Constants.AWS_AVATARS_FOLDER;
import static net.friendly_bets.utils.Constants.MAX_AVATAR_DIMENSION;

@RequiredArgsConstructor
@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FilesService {

    UsersRepository usersRepository;
    GetEntityService getEntityService;
    S3Service s3Service;

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

        User user = getEntityService.getUserOrThrow(currentUserId);

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
