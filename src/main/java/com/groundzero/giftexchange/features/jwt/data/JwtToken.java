package com.groundzero.giftexchange.features.jwt.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class JwtToken {

  private String token;
  @JsonProperty("expiration_date")
  private Date expirationDate;

  public JwtToken(String token, Date expirationDate) {
    this.token = token;
    this.expirationDate = expirationDate;
  }

  public String getToken() {
    return token;
  }

  public JwtToken setToken(String token) {
    this.token = token;
    return this;
  }

  public Date getExpirationDate() {
    return expirationDate;
  }

  public JwtToken setExpirationDate(Date expirationDate) {
    this.expirationDate = expirationDate;
    return this;
  }
}
