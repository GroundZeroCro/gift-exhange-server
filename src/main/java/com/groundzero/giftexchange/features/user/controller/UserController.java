package com.groundzero.giftexchange.features.user.controller;

import com.groundzero.giftexchange.data.EmptyDataResponse;
import com.groundzero.giftexchange.data.Response;
import com.groundzero.giftexchange.features.jwt.data.JwtToken;
import com.groundzero.giftexchange.features.jwt.service.JwtUserDetailsService;
import com.groundzero.giftexchange.features.user.api.LoginDataResponse;
import com.groundzero.giftexchange.features.user.api.LoginRequest;
import com.groundzero.giftexchange.features.user.api.RegistrationDataResponse;
import com.groundzero.giftexchange.features.user.api.RegistrationRequest;
import com.groundzero.giftexchange.features.user.data.RegistrationDto;
import com.groundzero.giftexchange.features.user.data.UserDao;
import com.groundzero.giftexchange.features.user.entity.UserEntity;
import com.groundzero.giftexchange.features.user.entity.UserEntityDto;
import com.groundzero.giftexchange.features.user.repository.UserRepository;
import com.groundzero.giftexchange.utils.JwtType;
import com.groundzero.giftexchange.utils.JwtUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/user")
public class UserController {

  private final UserRepository userRepository;
  private final JwtUserDetailsService jwtUserDetailsService;
  private final JwtUtils jwtUtils;
  private final AuthenticationManager authenticationManager;

  public UserController(UserRepository userRepository, JwtUserDetailsService jwtUserDetailsService, JwtUtils jwtUtils, AuthenticationManager authenticationManager) {
    this.userRepository = userRepository;
    this.jwtUserDetailsService = jwtUserDetailsService;
    this.jwtUtils = jwtUtils;
    this.authenticationManager = authenticationManager;
  }

  @PostMapping("/login")
  public Response loginUser(@RequestBody LoginRequest request) {
    try {
      authenticate(request.getUsername(), request.getPassword());
    } catch (Exception e) {
      return new Response(500, "Wrong username or password", new EmptyDataResponse());
    }
    return new Response(
        200,
        "Successfully login",
        new LoginDataResponse(
            getToken(request.getUsername(), JwtType.ACCESS),
            getToken(request.getUsername(), JwtType.REFRESH)
        )
    );
  }

  @PostMapping("/register")
  public Response createUser(@RequestBody RegistrationRequest request) {

    if (userRepository.existsByUsername(request.getUsername())) {
      return new Response(500, "User already exists", new EmptyDataResponse());
    }
    UserEntity userEntity = RegistrationDto.toEntity(request);
    return getNewUserResponse(userEntity, userEntity);
  }

  @DeleteMapping("/delete")
  public Response deleteUser(
      @RequestHeader(value = "Authorization") String bearerAuthorization
  ) {

    UserEntity userEntity = getUserEntityFromToken(bearerAuthorization);
    if (userEntity == null) {
      return new Response(500, "User not found", new EmptyDataResponse());
    }
    userRepository.delete(userEntity);
    return new Response(200, "User deleted successfully", new EmptyDataResponse());
  }

  @PatchMapping("/update")
  public Response updateUser(
      @RequestHeader(value = "Authorization") String bearerAuthorization,
      @RequestBody RegistrationRequest request
  ) {

    UserEntity userEntity = getUserEntityFromToken(bearerAuthorization);
    if (userEntity.getUsername() == null) {
      return new Response(500, "User not found", new EmptyDataResponse());
    } else if (userRepository.existsByUsername(request.getUsername())) {
      return new Response(500, "Username already exists", new EmptyDataResponse());
    }

    UserEntity updatedUserEntity = UserEntityDto.fromRegistrationRequest(userEntity, request);
    return getNewUserResponse(userEntity, updatedUserEntity);
  }

  private Response getNewUserResponse(UserEntity userEntity, UserEntity updatedUserEntity) {
    userRepository.save(updatedUserEntity);
    return new Response(
        200,
        "Request was successful",
        new RegistrationDataResponse(
            getToken(userEntity.getUsername(), JwtType.ACCESS),
            getToken(userEntity.getUsername(), JwtType.REFRESH),
            UserDao.fromEntity(userEntity))
    );
  }

  private JwtToken getToken(String username, JwtType tokenType) {
    UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(username);
    String token = jwtUtils.generateToken(userDetails, tokenType);
    System.out.println(jwtUtils.getTokenType(token));
    Date expirationDate = jwtUtils.getExpirationDateFromToken(token);
    return new JwtToken(token, expirationDate);
  }

  private UserEntity getUserEntityFromToken(String bearerAuthorization) {
    String username = jwtUtils.getUsernameFromToken(bearerAuthorization.substring(7));
    return userRepository.findByUsername(username);
  }

  private void authenticate(String username, String password) {
    authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
  }
}