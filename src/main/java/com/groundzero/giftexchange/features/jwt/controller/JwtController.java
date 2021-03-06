package com.groundzero.giftexchange.features.jwt.controller;

import com.groundzero.giftexchange.data.EmptyDataResponse;
import com.groundzero.giftexchange.data.Response;
import com.groundzero.giftexchange.features.jwt.api.JwtAccessTokenRequest;
import com.groundzero.giftexchange.features.jwt.data.JwtToken;
import com.groundzero.giftexchange.features.jwt.api.JwtAccessTokenDataResponse;
import com.groundzero.giftexchange.features.jwt.service.JwtUserDetailsService;
import com.groundzero.giftexchange.utils.JwtType;
import com.groundzero.giftexchange.utils.JwtUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

@RestController
@CrossOrigin
class JwtController {

  private final JwtUtils jwtUtils;
  private final JwtUserDetailsService jwtUserDetailsService;

  JwtController(JwtUtils jwtUtils, JwtUserDetailsService jwtUserDetailsService) {
    this.jwtUtils = jwtUtils;
    this.jwtUserDetailsService = jwtUserDetailsService;
  }

  @RequestMapping(value = "/authenticate/access-token", method = RequestMethod.POST)
  public Response createAuthenticationTokenWithToken(@RequestBody JwtAccessTokenRequest request) {

    Map<Boolean, String> validRefresherToken = jwtUtils.validateToken(request.getRefreshToken());

    for (Map.Entry<Boolean, String> entry : validRefresherToken.entrySet()) {
      boolean isTokenValid = !entry.getKey();
      if (isTokenValid) {
        return new Response(500, entry.getValue(), new EmptyDataResponse());
      }
    }
    String username = jwtUtils.getUsernameFromToken(request.getRefreshToken());
    Date expirationDate = jwtUtils.getExpirationDateFromToken(request.getRefreshToken());
    UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(username);
    String token = jwtUtils.generateToken(userDetails, JwtType.ACCESS);
    return new Response(
        200,
        "Successfully fetched access token",
        new JwtAccessTokenDataResponse(
            new JwtToken(token, expirationDate)
        )
    );
  }
}