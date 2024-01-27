package com.nexblocks.authguard.rest.mappers;

import com.nexblocks.authguard.api.dto.entities.*;
import com.nexblocks.authguard.api.dto.requests.CreateAccountRequestDTO;
import com.nexblocks.authguard.api.dto.requests.CreateAppRequestDTO;
import com.nexblocks.authguard.service.model.*;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.misc.BooleanRandomizer;
import org.jeasy.random.randomizers.number.NumberRandomizer;
import org.jeasy.random.randomizers.text.StringRandomizer;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class RestMapperTest {
    private final EasyRandom boRandomizer = new EasyRandom(new EasyRandomParameters()
            .collectionSizeRange(1, 3)
            .randomize(UserIdentifierBO.class, () -> UserIdentifierBO.builder()
                    .identifier(new StringRandomizer().getRandomValue())
                    .active(new BooleanRandomizer().getRandomValue())
                    .build())
    );

    private final EasyRandom dtoRandomizer = new EasyRandom(new EasyRandomParameters()
            .collectionSizeRange(1, 3)
            .randomize(field -> field.getName().equals("id") || field.getName().endsWith("Id"), () -> String.valueOf(new NumberRandomizer().getRandomValue()))
    );

    private final RestMapper restMapper = new RestMapperImpl();

    private <T, R> void boToDtoAndBack(final Class<T> fromClass, final Function<T, R> map,
                                       final Function<R, T> inverse, final String... ignoreFields) {
        T original = boRandomizer.nextObject(fromClass);
        R converted = map.apply(original);
        T back = inverse.apply(converted);

        assertThat(back).usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(ignoreFields)
                .isEqualTo(original);
    }

    private <T, R> void dtoToBoAndBack(final Class<T> fromClass, final Function<T, R> map,
                                       final Function<R, T> inverse, final String... ignoreFields) {
        T original = dtoRandomizer.nextObject(fromClass);
        R converted = map.apply(original);
        T back = inverse.apply(converted);

        assertThat(back).usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(ignoreFields)
                .isEqualTo(original);
    }

    @Test
    void toAccountBOAndBack() {
        dtoToBoAndBack(AccountDTO.class, restMapper::toBO, restMapper::toDTO);
    }

    @Test
    void toAccountDTOAndBack() {
        boToDtoAndBack(AccountBO.class, restMapper::toDTO, restMapper::toBO, ".*entityType", ".*hashedPassword", ".*plainPassword");
    }

    @Test
    void toCredentialsBOAndBack() {
        dtoToBoAndBack(CredentialsDTO.class, restMapper::toBO, restMapper::toDTO);
    }

    @Test
    void toCredentialsDTOAndBack() {
        boToDtoAndBack(CredentialsBO.class, restMapper::toDTO, restMapper::toBO,
                "entityType", "hashedPassword");
    }

    @Test
    void toAppBOAndBack() {
        dtoToBoAndBack(AppDTO.class, restMapper::toBO, restMapper::toDTO);
    }

    @Test
    void toAppDTOAndBack() {
        boToDtoAndBack(AppBO.class, restMapper::toDTO, restMapper::toBO, ".*entityType");
    }

    @Test
    void toPermissionBOAndBack() {
        dtoToBoAndBack(PermissionDTO.class, restMapper::toBO, restMapper::toDTO);
    }

    @Test
    void toPermissionDTOAndBack() {
        boToDtoAndBack(PermissionBO.class, restMapper::toDTO, restMapper::toBO, "entityType");
    }

    @Test
    void toRoleBOAndBack() {
        dtoToBoAndBack(RoleDTO.class, restMapper::toBO, restMapper::toDTO);
    }

    @Test
    void toRoleDTOAndBack() {
        boToDtoAndBack(RoleBO.class, restMapper::toDTO, restMapper::toBO, "entityType");
    }

    @Test
    void toAccountEmailBOAndBack() {
        dtoToBoAndBack(AccountEmailDTO.class, restMapper::toBO, restMapper::toDTO);
    }

    @Test
    void toAccountEmailDTOAndBack() {
        boToDtoAndBack(AccountEmailBO.class, restMapper::toDTO, restMapper::toBO, "entityType");
    }

    @Test
    void toTokensBOAndBack() {
        dtoToBoAndBack(AuthResponseDTO.class, restMapper::toBO, restMapper::toDTO);
    }

    @Test
    void toTokensDTOAndBack() {
        boToDtoAndBack(AuthResponseBO.class, restMapper::toDTO, restMapper::toBO, "entityType", "id", "entityId");
    }

    @Test
    void toTokenRestrictionsBOAndBack() {
        dtoToBoAndBack(TokenRestrictionsDTO.class, restMapper::toBO, restMapper::toDTO);
    }

    @Test
    void toTokenRestrictionsDTOAndBack() {
        boToDtoAndBack(TokenRestrictionsBO.class, restMapper::toDTO, restMapper::toBO, "entityType");
    }

    @Test
    void toAccountLockBOAndBack() {
        dtoToBoAndBack(AccountLockDTO.class, restMapper::toBO, restMapper::toDTO);
    }

    @Test
    void toAccountLockDTOAndBack() {
        boToDtoAndBack(AccountLockBO.class, restMapper::toDTO, restMapper::toBO, "entityType");
    }

    @Test
    void createAccountRequestToAccountBO() {
        CreateAccountRequestDTO requestDTO = dtoRandomizer.nextObject(CreateAccountRequestDTO.class);
        AccountDTO accountDTO = AccountDTO.builder()
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
                .metadata(requestDTO.getMetadata())
                .domain(requestDTO.getDomain())
                .identifiers(requestDTO.getIdentifiers())
                .build();

        AccountBO expected = restMapper.toBO(accountDTO)
                .withPlainPassword(requestDTO.getPlainPassword()); // verified in another test case
        AccountBO actual = restMapper.toBO(requestDTO);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void createAppRequestToAccountBO() {
        CreateAppRequestDTO requestDTO = dtoRandomizer.nextObject(CreateAppRequestDTO.class);
        AppDTO appDTO = AppDTO.builder()
                .externalId(requestDTO.getExternalId())
                .name(requestDTO.getName())
                .accountId(requestDTO.getAccountId())
                .permissions(requestDTO.getPermissions())
                .roles(requestDTO.getRoles())
                .active(requestDTO.isActive())
                .domain(requestDTO.getDomain())
                .build();

        AppBO expected = restMapper.toBO(appDTO); // verified in another test case
        AppBO actual = restMapper.toBO(requestDTO);

        assertThat(actual).isEqualTo(expected);
    }
}