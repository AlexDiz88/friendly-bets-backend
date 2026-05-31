package net.friendly_bets.services;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.friendly_bets.dto.*;
import net.friendly_bets.exceptions.BadRequestException;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.exceptions.NotFoundException;
import net.friendly_bets.config.WcTournamentSlots;
import net.friendly_bets.models.ExpandedMatchdaySlot;
import net.friendly_bets.models.TournamentFormat;
import net.friendly_bets.repositories.LeaguesRepository;
import net.friendly_bets.repositories.TournamentFormatsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TournamentFormatsService {

    TournamentFormatsRepository tournamentFormatsRepository;
    LeaguesRepository leaguesRepository;
    TournamentFormatExpander tournamentFormatExpander;
    GetEntityService getEntityService;

    @Transactional(readOnly = true)
    public TournamentFormatsPage getAll() {
        List<TournamentFormatDto> formats = tournamentFormatsRepository.findAll().stream()
                .map(this::toDtoWithSlots)
                .collect(Collectors.toList());
        return TournamentFormatsPage.builder().formats(formats).build();
    }

    @Transactional(readOnly = true)
    public TournamentFormatDto getById(String id) {
        return toDtoWithSlots(getEntityService.getTournamentFormatOrThrow(id));
    }

    @Transactional
    public TournamentFormatDto create(NewTournamentFormatDto dto) {
        validateNewFormat(dto);
        validateGroupStageSplit(dto.getGroupStage());
        if (tournamentFormatsRepository.existsByFormatCode(dto.getFormatCode())) {
            throw new BadRequestException("tournamentFormatCodeAlreadyExists");
        }

        TournamentFormat format = TournamentFormat.builder()
                .formatCode(dto.getFormatCode().trim())
                .name(dto.getName().trim())
                .createdAt(LocalDateTime.now())
                .regularStage(dto.getRegularStage() != null ? dto.getRegularStage().toEntity() : null)
                .groupStage(dto.getGroupStage() != null ? dto.getGroupStage().toEntity() : null)
                .playoff(toPlayoffEntity(dto.getPlayoff()))
                .build();

        tournamentFormatExpander.expand(format);
        tournamentFormatsRepository.save(format);
        return toDtoWithSlots(format);
    }

    @Transactional
    public TournamentFormatDto updateName(String id, UpdateTournamentFormatNameDto dto) {
        return update(id, new UpdateTournamentFormatDto(dto.getName(), null, null, null));
    }

    @Transactional
    public TournamentFormatDto update(String id, UpdateTournamentFormatDto dto) {
        TournamentFormat format = getEntityService.getTournamentFormatOrThrow(id);
        format.setName(dto.getName().trim());

        long linkedLeagueCount = leaguesRepository.countByTournamentFormatId(id);
        if (linkedLeagueCount > 0) {
            if (hasStructureUpdate(dto)) {
                throw new ConflictException("tournamentFormatStructureLockedByLeagues");
            }
        } else {
            applyStructureUpdate(format, dto);
            tournamentFormatExpander.expand(format);
        }

        tournamentFormatsRepository.save(format);
        return toDtoWithSlots(format);
    }

    @Transactional
    public void delete(String id) {
        getEntityService.getTournamentFormatOrThrow(id);
        if (leaguesRepository.countByTournamentFormatId(id) > 0) {
            throw new ConflictException("tournamentFormatInUseByLeagues");
        }
        tournamentFormatsRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ExpandedMatchdaySlot> expandSlots(String formatId) {
        return tournamentFormatExpander.expand(getEntityService.getTournamentFormatOrThrow(formatId));
    }

    @Transactional(readOnly = true)
    public List<ExternalMatchdaySlotDto> buildExternalSlots(String formatId) {
        return tournamentFormatExpander.expand(getEntityService.getTournamentFormatOrThrow(formatId)).stream()
                .map(TournamentFormatsService::toExternalSlot)
                .collect(Collectors.toList());
    }

    public static ExternalMatchdaySlotDto toExternalSlot(ExpandedMatchdaySlot slot) {
        String kind = switch (slot.getKind()) {
            case KNOCKOUT -> "KNOCKOUT";
            case GROUP -> "GROUP";
            case REGULAR -> "REGULAR";
        };
        return ExternalMatchdaySlotDto.builder()
                .value(slot.getOrder())
                .slotId(slot.getId())
                .label(slot.getId())
                .kind(kind)
                .build();
    }

    private void validateNewFormat(NewTournamentFormatDto dto) {
        validateFormatStructure(dto.getRegularStage(), dto.getGroupStage(), dto.getPlayoff());
        validateGroupStageSplit(dto.getGroupStage());
    }

    private void validateFormatStructure(
            RoundRobinStageDto regularStage,
            RoundRobinStageDto groupStage,
            List<PlayoffRoundDto> playoff
    ) {
        boolean hasRegular = regularStage != null;
        boolean hasGroup = groupStage != null;
        boolean hasPlayoff = playoff != null && !playoff.isEmpty();

        if (!hasRegular && !hasGroup && !hasPlayoff) {
            throw new BadRequestException("tournamentFormatMustHaveAtLeastOneStage");
        }
        if (hasRegular && hasGroup) {
            throw new BadRequestException("tournamentFormatRegularAndGroupMutuallyExclusive");
        }
    }

    private boolean hasStructureUpdate(UpdateTournamentFormatDto dto) {
        return dto.getRegularStage() != null
                || dto.getGroupStage() != null
                || dto.getPlayoff() != null;
    }

    private void applyStructureUpdate(TournamentFormat format, UpdateTournamentFormatDto dto) {
        if (WcTournamentSlots.FORMAT_CODE.equals(format.getFormatCode())) {
            if (dto.getGroupStage() != null) {
                validateGroupStageSplit(dto.getGroupStage());
                format.setGroupStage(dto.getGroupStage().toEntity());
            }
            format.setPlayoff(toPlayoffEntity(dto.getPlayoff()));
            return;
        }
        validateFormatStructure(dto.getRegularStage(), dto.getGroupStage(), dto.getPlayoff());
        validateGroupStageSplit(dto.getGroupStage());
        format.setRegularStage(dto.getRegularStage() != null ? dto.getRegularStage().toEntity() : null);
        format.setGroupStage(dto.getGroupStage() != null ? dto.getGroupStage().toEntity() : null);
        format.setPlayoff(toPlayoffEntity(dto.getPlayoff()));
    }

    private void validateGroupStageSplit(RoundRobinStageDto groupStage) {
        if (groupStage == null || !Boolean.TRUE.equals(groupStage.getSplitSlotsPerRound())) {
            return;
        }
        List<Integer> perRound = groupStage.getSlotsPerRound();
        if (perRound == null || perRound.size() != groupStage.getMatchdayCount()) {
            throw new BadRequestException("groupSlotsPerRoundSizeMismatch");
        }
        for (Integer count : perRound) {
            if (count == null || count < 1 || count > 8) {
                throw new BadRequestException("invalidGroupSlotCount");
            }
        }
    }

    private static List<net.friendly_bets.models.PlayoffRound> toPlayoffEntity(List<PlayoffRoundDto> playoff) {
        if (playoff == null || playoff.isEmpty()) {
            return null;
        }
        return playoff.stream().map(PlayoffRoundDto::toEntity).collect(Collectors.toList());
    }

    private TournamentFormatDto toDtoWithSlots(TournamentFormat format) {
        List<ExpandedMatchdaySlotDto> slots = tournamentFormatExpander.expand(format).stream()
                .map(ExpandedMatchdaySlotDto::from)
                .collect(Collectors.toList());
        long linkedLeagueCount = leaguesRepository.countByTournamentFormatId(format.getId());
        return TournamentFormatDto.from(format, slots, linkedLeagueCount);
    }

}
