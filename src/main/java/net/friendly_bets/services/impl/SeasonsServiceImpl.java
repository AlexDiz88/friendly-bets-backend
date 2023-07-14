package net.friendly_bets.services.impl;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.NewSeasonDto;
import net.friendly_bets.dto.SeasonDto;
import net.friendly_bets.dto.SeasonsPage;
import net.friendly_bets.exceptions.BadDataException;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.models.Season;
import net.friendly_bets.repositories.SeasonsRepository;
import net.friendly_bets.services.SeasonsService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class SeasonsServiceImpl implements SeasonsService {

    private final SeasonsRepository seasonsRepository;

    @Override
    public SeasonsPage getAll() {
        List<Season> allSeasons = seasonsRepository.findAll();
        return SeasonsPage.builder()
                .seasons(SeasonDto.from(allSeasons))
                .build();
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public SeasonDto addSeason(NewSeasonDto newSeason) {
        if (newSeason.getTitle() == null || newSeason.getTitle().trim().length() < 1) {
            throw new BadDataException("Название сезона не может быть пустым");
        }
        if (newSeason.getBetCountPerMatchDay() == null || newSeason.getBetCountPerMatchDay() < 1) {
            throw new BadDataException("Количество ставок на тур не может быть меньше 1");
        }
        if (seasonsRepository.existsByTitle(newSeason.getTitle())) {
            throw new BadDataException("Сезон с таким названием уже существует");
        }

        Season season = Season.builder()
                .createdAt(LocalDateTime.now())
                .title(newSeason.getTitle())
                .betCountPerMatchDay(newSeason.getBetCountPerMatchDay())
                .status(Season.Status.CREATED)
                .players(new ArrayList<>())
                .leagues(new ArrayList<>())
                .bets(new ArrayList<>())
                .build();

        seasonsRepository.save(season);
        return SeasonDto.from(season);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public SeasonDto changeSeasonStatus(String title, String status) {
        status = status.substring(1, status.length() - 1);
        try {
            Season.Status.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Недопустимый статус: " + status);
        }
        Season season = seasonsRepository.findByTitleEquals(title);
        if (season == null) {
            throw new NotFoundException("Сезон с таким именем не найден");
        }
        if (season.getStatus().toString().equals(status)) {
            throw new ConflictException("Сезон уже имеет этот статус");
        }
        if (season.getStatus().equals(Season.Status.FINISHED)) {
            throw new BadDataException("Сезон завершен и его статус больше нельзя изменить");
        }
        // TODO исправить отображение ошибки на фронте

        season.setStatus(Season.Status.valueOf(status));
        seasonsRepository.save(season);

        return SeasonDto.from(season);
    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public List<String> getSeasonStatusList() {
        return Arrays.stream(Season.Status.values())
                .map(Enum::toString)
                .toList();

    }

    // ------------------------------------------------------------------------------------------------------ //

    @Override
    public SeasonDto getActiveSeason() {
        List<Season> seasonList = seasonsRepository.findAll();

        Optional<Season> activeSeason = seasonList.stream()
                .filter(season -> season.getStatus() == Season.Status.ACTIVE)
                .findFirst();

        if (activeSeason.isEmpty()) {
        return null;
        }
        return SeasonDto.from(activeSeason.get());
    }

//    @Override
//    public SeasonDto getActiveSeason() {
//        Optional<Season> seasonByStatus = seasonsRepository.findSeasonByStatus(Season.Status.ACTIVE);
//        if (seasonByStatus.isEmpty()) {
//            return null;
//        }
//        Season season = seasonByStatus.get();
//        return SeasonDto.from(season);
//    }

    // ------------------------------------------------------------------------------------------------------ //
}
