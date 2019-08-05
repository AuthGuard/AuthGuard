package org.auther.api.routes;

import org.auther.api.dto.AccountDTO;
import org.auther.api.dto.TokensDTO;
import org.auther.service.model.AccountBO;
import org.auther.service.model.TokensBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface RestMapper {
    RestMapper INSTANCE = Mappers.getMapper(RestMapper.class);

    AccountDTO toDTO(AccountBO accountBO);
    TokensDTO toDTO(TokensBO tokensBO);

    AccountBO toBO(AccountDTO accountDTO);
    TokensBO toBO(TokensDTO tokensDTO);
}
