package com.groundzero.giftexchange.user.controller;

import com.groundzero.giftexchange.common.EmptyDataResponse;
import com.groundzero.giftexchange.common.Response;
import com.groundzero.giftexchange.jwt.data.JwtAccessToken;
import com.groundzero.giftexchange.jwt.service.JwtUserDetailsService;
import com.groundzero.giftexchange.jwt.utils.JwtUtils;
import com.groundzero.giftexchange.user.api.LoginRequest;
import com.groundzero.giftexchange.user.api.RegistrationRequest;
import com.groundzero.giftexchange.user.api.LoginDataResponse;
import com.groundzero.giftexchange.user.data.RegistrationDto;
import com.groundzero.giftexchange.user.api.RegistrationDataResponse;
import com.groundzero.giftexchange.user.data.UserDao;
import com.groundzero.giftexchange.user.repository.UserInfoRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
public class UserController {

  private final UserInfoRepository userInfoRepository;
  private final JwtUserDetailsService jwtUserDetailsService;
  private final JwtUtils jwtUtils;
  private final AuthenticationManager authenticationManager;

  public UserController(UserInfoRepository userInfoRepository, JwtUserDetailsService jwtUserDetailsService, JwtUtils jwtUtils, AuthenticationManager authenticationManager) {
    this.userInfoRepository = userInfoRepository;
    this.jwtUserDetailsService = jwtUserDetailsService;
    this.jwtUtils = jwtUtils;
    this.authenticationManager = authenticationManager;
  }

  @PostMapping("/user/register")
  public Response createUser(@RequestBody RegistrationRequest request) {

    if (userInfoRepository.existsByUsername(request.getUsername())) {
      return new Response(500, "User already exists", new EmptyDataResponse());
    }
    userInfoRepository.save(RegistrationDto.toEntity(request));
    UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(request.getUsername());
    String token = jwtUtils.generateToken(userDetails);
    Date expirationDate = jwtUtils.getExpirationDateFromToken(token);

    return new Response(
        200,
        "User successfully created",
        new RegistrationDataResponse(
            new JwtAccessToken(token, expirationDate),
            UserDao.fromRegistration(request))
    );
  }

  @RequestMapping(value = "/user/login", method = RequestMethod.POST)
  public Response loginUser(@RequestBody LoginRequest request) {
    try {
      authenticate(request.getUsername(), request.getPassword());
    } catch (Exception e) {
      return new Response(500, "Wrong username or password", new EmptyDataResponse());
    }

    UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(request.getUsername());
    String token = jwtUtils.generateToken(userDetails);
    Date expirationDate = jwtUtils.getExpirationDateFromToken(token);
    return new Response(200, "Successfully login", new LoginDataResponse(new JwtAccessToken(token, expirationDate)));
  }

  private void authenticate(String username, String password) {
    authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
  }
}
