package com.nexblocks.authguard.rest.mappers;

import com.nexblocks.authguard.api.dto.entities.*;
import com.nexblocks.authguard.api.dto.requests.CreateAccountRequestDTO;
import com.nexblocks.authguard.api.dto.requests.CreateAppRequestDTO;
import com.nexblocks.authguard.service.model.*;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.api.Randomizer;
import org.jeasy.random.randomizers.misc.BooleanRandomizer;
import org.jeasy.random.randomizers.text.StringRandomizer;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class RestMapperTest {
    private final EasyRandom easyRandom = new EasyRandom(new EasyRandomParameters()
            .collectionSizeRange(1, 3)
            .randomize(UserIdentifierBO.class, new Randomizer<UserIdentifierBO>() {
                @Override
                public UserIdentifierBO getRandomValue() {
                    return UserIdentifierBO.builder()
                            .identifier(new StringRandomizer().getRandomValue())
                            .active(new BooleanRandomizer().getRandomValue())
                            .build();
                }
            })
    );
    private final RestMapper restMapper = new RestMapperImpl();

    private <T, R> void convertAndBack(final Class<T> fromClass, final Function<T, R> map,
                                       final Function<R, T> inverse, final String... ignoreFields) {
        final T original = easyRandom.nextObject(fromClass);
        final R converted = map.apply(original);
        final T back = inverse.apply(converted);

        assertThat(back).usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(ignoreFields)
                .isEqualTo(original);
    }

    @Test
    void toAccountBOAndBack() {
        convertAndBack(AccountDTO.class, restMapper::toBO, restMapper::toDTO);
    }

    @Test
    void toAccountDTOAndBack() {
        convertAndBack(AccountBO.class, restMapper::toDTO, restMapper::toBO, ".*entityType");
    }

    @Test
    void toCredentialsBOAndBack() {
        convertAndBack(CredentialsDTO.class, restMapper::toBO, restMapper::toDTO);
    }

    @Test
    void toCredentialsDTOAndBack() {
        convertAndBack(CredentialsBO.class, restMapper::toDTO, restMapper::toBO,
                "entityType", "hashedPassword");
    }

    @Test
    void toAppBOAndBack() {
        convertAndBack(AppDTO.class, restMapper::toBO, restMapper::toDTO);
    }

    @Test
    void toAppDTOAndBack() {
        convertAndBack(AppBO.class, restMapper::toDTO, restMapper::toBO, ".*entityType");
    }

    @Test
    void toPermissionBOAndBack() {
        convertAndBack(PermissionDTO.class, restMapper::toBO, restMapper::toDTO);
    }

    @Test
    void toPermissionDTOAndBack() {
        convertAndBack(PermissionBO.class, restMapper::toDTO, restMapper::toBO, "entityType");
    }

    @Test
    void toRoleBOAndBack() {
        convertAndBack(RoleDTO.class, restMapper::toBO, restMapper::toDTO);
    }

    @Test
    void toRoleDTOAndBack() {
        convertAndBack(RoleBO.class, restMapper::toDTO, restMapper::toBO, "entityType");
    }

    @Test
    void toAccountEmailBOAndBack() {
        convertAndBack(AccountEmailDTO.class, restMapper::toBO, restMapper::toDTO);
    }

    @Test
    void toAccountEmailDTOAndBack() {
        convertAndBack(AccountEmailBO.class, restMapper::toDTO, restMapper::toBO, "entityType");
    }

    @Test
    void toTokensBOAndBack() {
        convertAndBack(AuthResponseDTO.class, restMapper::toBO, restMapper::toDTO);
    }

    @Test
    void toTokensDTOAndBack() {
        convertAndBack(AuthResponseBO.class, restMapper::toDTO, restMapper::toBO, "entityType", "id", "entityId");
    }

    @Test
    void toTokenRestrictionsBOAndBack() {
        convertAndBack(TokenRestrictionsDTO.class, restMapper::toBO, restMapper::toDTO);
    }

    @Test
    void toTokenRestrictionsDTOAndBack() {
        convertAndBack(TokenRestrictionsBO.class, restMapper::toDTO, restMapper::toBO, "entityType");
    }

    @Test
    void toAccountLockBOAndBack() {
        convertAndBack(AccountLockDTO.class, restMapper::toBO, restMapper::toDTO);
    }

    @Test
    void toAccountLockDTOAndBack() {
        convertAndBack(AccountLockBO.class, restMapper::toDTO, restMapper::toBO, "entityType");
    }

    @Test
    void createAccountRequestToAccountBO() {
        final CreateAccountRequestDTO requestDTO = easyRandom.nextObject(CreateAccountRequestDTO.class);
        final AccountDTO accountDTO = AccountDTO.builder()
                .externalId(requestDTO.getExternalId())
                .firstName(requestDTO.getFirstName())
                .lastName(requestDTO.getLastName())
                .middleName(requestDTO.getMiddleName())
                .fullName(requestDTO.getFullName())
                .email(requestDTO.getEmail())
                .backupEmail(requestDTO.getBackupEmail())
                .phoneNumber(requestDTO.getPhoneNumber())
                .permissions(requestDTO.getPermissions())
                .roles(requestDTO.getRoles())
                .active(requestDTO.isActive())
                .build();

        final AccountBO expected = restMapper.toBO(accountDTO); // verified in another test case
        final AccountBO actual = restMapper.toBO(requestDTO);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void createAppRequestToAccountBO() {
        final CreateAppRequestDTO requestDTO = easyRandom.nextObject(CreateAppRequestDTO.class);
        final AppDTO appDTO = AppDTO.builder()
                .externalId(requestDTO.getExternalId())
                .name(requestDTO.getName())
                .accountId(requestDTO.getAccountId())
                .permissions(requestDTO.getPermissions())
                .roles(requestDTO.getRoles())
                .active(requestDTO.isActive())
                .build();

        final AppBO expected = restMapper.toBO(appDTO); // verified in another test case
        final AppBO actual = restMapper.toBO(requestDTO);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void exchangeAttempt() {
        final ExchangeAttemptBO exchangeAttemptBO = easyRandom.nextObject(ExchangeAttemptBO.class);
        final ExchangeAttemptDTO mapped = restMapper.toDTO(exchangeAttemptBO);

        assertThat(mapped).isEqualToComparingFieldByField(exchangeAttemptBO);
    }
}