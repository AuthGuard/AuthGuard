package org.auther.service.impl;

import com.auther.config.ConfigContext;
import org.auther.dal.OtpRepository;
import org.auther.dal.model.OneTimePasswordDO;
import org.auther.emb.MessagePublisher;
import org.auther.service.AccountsService;
import org.auther.service.JwtProvider;
import org.auther.service.config.ImmutableOtpConfig;
import org.auther.service.config.OtpMode;
import org.auther.service.exceptions.ServiceAuthorizationException;
import org.auther.service.impl.mappers.ServiceMapperImpl;
import org.auther.service.model.AccountBO;
import org.auther.service.model.TokensBO;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OtpServiceImplTest {
    private final EasyRandom random = new EasyRandom(new EasyRandomParameters().collectionSizeRange(1, 4));

    private OtpRepository mockOtpRepository;
    private AccountsService mockAccountsService;
    private JwtProvider mockJwtProvider;

    private OtpServiceImpl otpService;

    void setup(final ImmutableOtpConfig otpConfig) {
        mockOtpRepository = Mockito.mock(OtpRepository.class);
        mockAccountsService = Mockito.mock(AccountsService.class);
        mockJwtProvider = Mockito.mock(JwtProvider.class);

        final ConfigContext configContext = Mockito.mock(ConfigContext.class);

        Mockito.when(configContext.asConfigBean(ImmutableOtpConfig.class)).thenReturn(otpConfig);

        otpService = new OtpServiceImpl(mockOtpRepository, Mockito.mock(MessagePublisher.class),
                mockAccountsService, mockJwtProvider, new ServiceMapperImpl(), configContext);
    }

    @Test
    void generateAlphanumeric() {
        final ImmutableOtpConfig otpConfig = ImmutableOtpConfig.builder()
                .mode(OtpMode.ALPHANUMERIC)
                .length(6)
                .lifeTime("5m")
                .build();

        setup(otpConfig);

        final AccountBO account = random.nextObject(AccountBO.class);

        final TokensBO generated = otpService.generate(account);

        assertThat(generated.getType()).isEqualTo("OTP");
        assertThat(generated.getToken()).isNotNull();
        assertThat(generated.getRefreshToken()).isNull();

        final ArgumentCaptor<OneTimePasswordDO> argumentCaptor = ArgumentCaptor.forClass(OneTimePasswordDO.class);

        Mockito.verify(mockOtpRepository).save(argumentCaptor.capture());

        final OneTimePasswordDO persisted = argumentCaptor.getValue();

        assertThat(persisted.getAccountId()).isEqualTo(account.getId());
        assertThat(persisted.getExpiresAt())
                .isAfter(ZonedDateTime.now())
                .isBefore(ZonedDateTime.now().plus(Duration.ofMinutes(6)));
        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getPassword()).isNotNull();
        assertThat(persisted.getPassword()).hasSize(6);
    }

    @Test
    void generateAlphabetic() {
        final ImmutableOtpConfig otpConfig = ImmutableOtpConfig.builder()
                .mode(OtpMode.ALPHABETIC)
                .length(6)
                .lifeTime("5m")
                .build();

        setup(otpConfig);

        final AccountBO account = random.nextObject(AccountBO.class);

        final TokensBO generated = otpService.generate(account);

        assertThat(generated.getType()).isEqualTo("OTP");
        assertThat(generated.getToken()).isNotNull();
        assertThat(generated.getRefreshToken()).isNull();

        final ArgumentCaptor<OneTimePasswordDO> argumentCaptor = ArgumentCaptor.forClass(OneTimePasswordDO.class);

        Mockito.verify(mockOtpRepository).save(argumentCaptor.capture());

        final OneTimePasswordDO persisted = argumentCaptor.getValue();

        assertThat(persisted.getAccountId()).isEqualTo(account.getId());
        assertThat(persisted.getExpiresAt())
                .isAfter(ZonedDateTime.now())
                .isBefore(ZonedDateTime.now().plus(Duration.ofMinutes(6)));
        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getPassword()).isNotNull();
        assertThat(persisted.getPassword()).hasSize(6);

        for (final char ch : persisted.getPassword().toCharArray()) {
            assertThat(Character.isDigit(ch)).isFalse();
        }
    }

    @Test
    void generateNumeric() {
        final ImmutableOtpConfig otpConfig = ImmutableOtpConfig.builder()
                .mode(OtpMode.NUMERIC)
                .length(6)
                .lifeTime("5m")
                .build();

        setup(otpConfig);

        final AccountBO account = random.nextObject(AccountBO.class);

        final TokensBO generated = otpService.generate(account);

        assertThat(generated.getType()).isEqualTo("OTP");
        assertThat(generated.getToken()).isNotNull();
        assertThat(generated.getRefreshToken()).isNull();

        final ArgumentCaptor<OneTimePasswordDO> argumentCaptor = ArgumentCaptor.forClass(OneTimePasswordDO.class);

        Mockito.verify(mockOtpRepository).save(argumentCaptor.capture());

        final OneTimePasswordDO persisted = argumentCaptor.getValue();

        assertThat(persisted.getAccountId()).isEqualTo(account.getId());
        assertThat(persisted.getExpiresAt())
                .isAfter(ZonedDateTime.now())
                .isBefore(ZonedDateTime.now().plus(Duration.ofMinutes(6)));
        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getPassword()).isNotNull();
        assertThat(persisted.getPassword()).hasSize(6);

        for (final char ch : persisted.getPassword().toCharArray()) {
            assertThat(Character.isAlphabetic(ch)).isFalse();
        }
    }

    @Test
    void authenticate() {
        final ImmutableOtpConfig otpConfig = ImmutableOtpConfig.builder()
                .mode(OtpMode.ALPHANUMERIC)
                .length(6)
                .lifeTime("5m")
                .build();

        setup(otpConfig);

        final OneTimePasswordDO otp = random.nextObject(OneTimePasswordDO.class);
        final AccountBO account = random.nextObject(AccountBO.class)
                .withId(otp.getAccountId());
        final TokensBO tokens = random.nextObject(TokensBO.class);

        Mockito.when(mockOtpRepository.getById(otp.getId())).thenReturn(Optional.of(otp));
        Mockito.when(mockAccountsService.getById(account.getId())).thenReturn(Optional.of(account));
        Mockito.when(mockJwtProvider.generateToken(account)).thenReturn(tokens);

        final TokensBO generated = otpService.authenticate(otp.getId(), otp.getPassword());

        assertThat(generated).isEqualTo(tokens);
    }

    @Test
    void authenticateWrongPassword() {
        final ImmutableOtpConfig otpConfig = ImmutableOtpConfig.builder()
                .mode(OtpMode.ALPHANUMERIC)
                .length(6)
                .lifeTime("5m")
                .build();

        setup(otpConfig);

        final OneTimePasswordDO otp = random.nextObject(OneTimePasswordDO.class);
        final AccountBO account = random.nextObject(AccountBO.class)
                .withId(otp.getAccountId());
        final TokensBO tokens = random.nextObject(TokensBO.class);

        Mockito.when(mockOtpRepository.getById(otp.getId())).thenReturn(Optional.of(otp));
        Mockito.when(mockAccountsService.getById(account.getId())).thenReturn(Optional.of(account));
        Mockito.when(mockJwtProvider.generateToken(account)).thenReturn(tokens);

        assertThatThrownBy(() -> otpService.authenticate(otp.getId(), "wrong"))
                .isInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void authenticateAccountNotFound() {
        final ImmutableOtpConfig otpConfig = ImmutableOtpConfig.builder()
                .mode(OtpMode.ALPHANUMERIC)
                .length(6)
                .lifeTime("5m")
                .build();

        setup(otpConfig);

        final OneTimePasswordDO otp = random.nextObject(OneTimePasswordDO.class);

        Mockito.when(mockOtpRepository.getById(otp.getId())).thenReturn(Optional.of(otp));
        Mockito.when(mockAccountsService.getById(otp.getAccountId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> otpService.authenticate(otp.getId(), otp.getPassword()))
                .isInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void authenticatePasswordNotFound() {
        final ImmutableOtpConfig otpConfig = ImmutableOtpConfig.builder()
                .mode(OtpMode.ALPHANUMERIC)
                .length(6)
                .lifeTime("5m")
                .build();

        setup(otpConfig);

        final OneTimePasswordDO otp = random.nextObject(OneTimePasswordDO.class);

        Mockito.when(mockOtpRepository.getById(otp.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> otpService.authenticate(otp.getId(), otp.getPassword()))
                .isInstanceOf(ServiceAuthorizationException.class);
    }
}