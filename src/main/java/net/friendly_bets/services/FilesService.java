package net.friendly_bets.services;

import net.friendly_bets.dto.BetDto;
import net.friendly_bets.dto.BetsPage;
import net.friendly_bets.dto.EditedBetDto;
import org.springframework.web.multipart.MultipartFile;

public interface FilesService {

    String saveAvatarImage(String currentUserId, MultipartFile image);

    String saveLogoImage(String teamId, MultipartFile image);
}
