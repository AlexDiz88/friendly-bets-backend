package net.friendly_bets.services;

import net.friendly_bets.dto.ImageDto;
import org.springframework.web.multipart.MultipartFile;

public interface FilesService {

    ImageDto saveAvatarImage(String currentUserId, MultipartFile image);

    ImageDto saveLogoImage(String teamId, MultipartFile image);
}
