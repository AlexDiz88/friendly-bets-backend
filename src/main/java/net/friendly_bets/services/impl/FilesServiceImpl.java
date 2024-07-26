package net.friendly_bets.services.impl;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.models.Team;
import net.friendly_bets.models.User;
import net.friendly_bets.repositories.TeamsRepository;
import net.friendly_bets.repositories.UsersRepository;
import net.friendly_bets.services.FilesService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static net.friendly_bets.utils.GetEntityOrThrow.getTeamOrThrow;

@RequiredArgsConstructor
@Service
public class FilesServiceImpl implements FilesService {

    private final UsersRepository usersRepository;
    private final TeamsRepository teamsRepository;

    @Value("${upload.path.avatars}")
    private String UPLOAD_PATH_AVATARS;

    @Value("${upload.path.logo}")
    private String UPLOAD_PATH_LOGO;

    @Override
    @Transactional
    public String saveAvatarImage(String currentUserId, MultipartFile image) {
        User user = usersRepository.findById(currentUserId)
                .orElseThrow(IllegalArgumentException::new);
        String fileName = user.getId();
        fileName = saveImage(image, fileName, UPLOAD_PATH_AVATARS);
        user.setAvatar(fileName);
        usersRepository.save(user);
        return fileName;
    }

    @Override
    @Transactional
    public String saveLogoImage(String teamId, MultipartFile image) {
        Team team = getTeamOrThrow(teamsRepository, teamId);
        // TODO: проработать путь файла (пробелы, нижние подчеркивания)
        String fileName = team.getTitle();
        fileName = saveImage(image, fileName, UPLOAD_PATH_LOGO);
        System.out.println(fileName);
        team.setLogo(fileName);
        teamsRepository.save(team);
        return fileName;
    }

    private String saveImage(MultipartFile image, String fileName, String uploadPath) {
        if (image == null) {
            throw new RuntimeException("Изображение отсутствует");
        }
        File uploadDir = new File(uploadPath);

        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        fileName = fileName + ".png";
        Path path = uploadDir.toPath().resolve(fileName);
        try {
            image.transferTo(path);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка сохранения изображения", e);
        }
        return fileName;
    }
}
