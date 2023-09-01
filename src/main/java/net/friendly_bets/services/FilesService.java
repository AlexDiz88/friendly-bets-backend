package net.friendly_bets.services;

import org.springframework.web.multipart.MultipartFile;

public interface FilesService {

    String saveAvatarImage(String currentUserId, MultipartFile image);

    String saveLogoImage(String teamId, MultipartFile image);
}
