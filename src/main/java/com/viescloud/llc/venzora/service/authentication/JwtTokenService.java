package com.viescloud.llc.venzora.service.authentication;

import java.io.Serializable;

import org.springframework.stereotype.Component;

import com.viescloud.eco.viesspringutils.exception.HttpResponseThrowers;
import com.viescloud.llc.venzora.model.authentication.User;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Component
public class JwtTokenService implements Serializable
{
    private static final long serialVersionUID = -2550185165626007488L;

	public static final long JWT_TOKEN_VALIDITY = 24 * 60 * 60; //24 hours

	@Autowired
	private UserService userService;

	@Value("${jwt.secret}")
	private String secret;

	public static String getTokenFromBearerAuthenticator(String authenticator) {
		var splits = authenticator.split(" ");
		if(splits.length != 2 || !splits[0].equalsIgnoreCase("Bearer"))
			return HttpResponseThrowers.throwUnauthorized("Authorization header is invalid or not a bearer token");
		return splits[1];
	}

	//retrieve username from jwt token
	public String getUsernameFromToken(String token) {
		return getClaimFromToken(token, Claims::getSubject);
	}

	//retrieve username from jwt token
	public String getPwdFromToken(String token) {
		return getClaimFromToken(token, (c) -> c.get("pwd").toString());
	}

	//retrieve expiration date from jwt token
	public Date getExpirationDateFromToken(String token) {
		return getClaimFromToken(token, Claims::getExpiration);
	}

	public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = getAllClaimsFromToken(token);
		return claimsResolver.apply(claims);
	}
    //for retrieving any information from token we will need the secret key
	private Claims getAllClaimsFromToken(String token) {
		return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
	}

	//check if the token has expired
	public Boolean isTokenExpired(String token) {
		final Date expiration = getExpirationDateFromToken(token);
		return expiration.before(new Date());
	}

	//generate token for user
	public String generateToken(User user) {
		Map<String, Object> claims = new HashMap<>();
		return doGenerateToken(claims, user);
	}

	@Deprecated
	//while creating the token -
	//1. Define  claims of the token, like Issuer, Expiration, Subject, and the ID
	//2. Sign the JWT using the HS512 algorithm and secret key.
	//3. According to JWS Compact Serialization(https://tools.ietf.org/html/draft-ietf-jose-json-web-signature-41#section-3.1)
	//   compaction of the JWT to a URL-safe string 
	public String doGenerateToken(Map<String, Object> claims, String subject) {
		return Jwts.builder()
				.setClaims(claims)
				.setSubject(subject)
				.setIssuedAt(new Date(System.currentTimeMillis()))
				.setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY * 1000))
				.signWith(SignatureAlgorithm.HS512, secret).compact();
	}

	private String doGenerateToken(Map<String, Object> claims, User user) {
		// claims.put("pwd", user.getPassword());
		return Jwts.builder()
				.setClaims(claims)
				.setSubject(user.getUsername())
				.setIssuedAt(new Date(System.currentTimeMillis()))
				.setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY * 1000))
				.signWith(SignatureAlgorithm.HS512, secret).compact();
	}

	//validate token
	public Boolean isValidToken(String token, User user) {
		final String username = getUsernameFromToken(token);
		return (username.equals(user.getUsername()) && !isTokenExpired(token));
	}

	public boolean isNotValidToken(String token, User user) {
		return !isValidToken(token, user);
	}

	public User checkIsValidToken(String jwt) { 
		var token = JwtTokenService.getTokenFromBearerAuthenticator(jwt);
        var username = this.getUsernameFromToken(token);
        var user = this.userService.getByUsername(username)
								   .orElseThrow(HttpResponseThrowers.throwUnauthorizedException("Invalid token"));
        if(!user.getUsername().equals(username) || isTokenExpired(token))
			return HttpResponseThrowers.throwUnauthorized("Invalid token");
        
        user.setPassword(null);
        return user;
	}

	public Optional<User> tryCheckIsValidToken(String jwt) {
		try {
			return Optional.of(this.checkIsValidToken(jwt));
		} catch (Exception ex) {
			return Optional.empty();
		}
	}
}
