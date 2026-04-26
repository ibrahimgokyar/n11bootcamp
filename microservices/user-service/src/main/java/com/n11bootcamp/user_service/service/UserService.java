package com.n11bootcamp.user_service.service;

import java.util.ArrayList;
import java.util.List;


import com.n11bootcamp.user_service.entity.ShoppingCart;
import com.n11bootcamp.user_service.entity.User;
import com.n11bootcamp.user_service.repository.UserRepository;
import com.n11bootcamp.user_service.request.LoginRequest;
import com.n11bootcamp.user_service.request.SignupRequest;
import com.n11bootcamp.user_service.request.UpdateUserRequest;
import com.n11bootcamp.user_service.response.JwtResponse;
import com.n11bootcamp.user_service.response.MessageResponse;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


import jakarta.persistence.EntityNotFoundException;

@Service
public class UserService {

    // ?? JWT Authentication i�in gerekli config de�erleri application.yml�den okunuyor
    @Value("${jwt.issuer_uri}")   String jwtIssuerUri;
    @Value("${jwt.client_id}")    String jwtClientId;
    @Value("${jwt.client_secret}")String jwtClientSecret;
    @Value("${jwt.grant_type}")   String jwtGrantType;
    @Value("${jwt.scope}")        String jwtScope;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestTemplate restTemplate;
    // ?? Di�er microservice�lerle haberle�mek i�in (ShoppingCartService vs.)

    /**
     * ?? Kullan�c� giri�ini do�rular, Identity Provider�dan JWT al�r.
     */
    public ResponseEntity<?> authenticateUser(LoginRequest loginRequest) {
        User user;
        try {
            // Kullan�c�y� DB�den bul
            user = userRepository.findByUsername(loginRequest.getUsername()).orElseThrow();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("User not found!"));
        }

        // ?? �ifre kontrol� (�u an yoruma al�nm�� durumda)
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        // if (!encoder.matches(loginRequest.getPassword(), user.getPassword())) {
        //     return ResponseEntity.badRequest().body(new MessageResponse("User credentials are not valid"));
        // }

        // ?? Apache HttpClient ile Token Endpoint�e POST iste�i at
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(jwtIssuerUri.trim());

        // Form-Data parametreleri (OAuth2 Resource Owner Password Credentials Flow)
        List<BasicNameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("grant_type", jwtGrantType.trim()));
        params.add(new BasicNameValuePair("client_id", jwtClientId.trim()));
        params.add(new BasicNameValuePair("client_secret", jwtClientSecret.trim()));
        params.add(new BasicNameValuePair("username", loginRequest.getUsername().trim()));
        params.add(new BasicNameValuePair("password", loginRequest.getPassword().trim()));
        // params.add(new BasicNameValuePair("scope", jwtScope)); // Opsiyonel

        String accessToken = "";
        try {
            // Request body�ye parametreleri ekle
            httpPost.setEntity(new UrlEncodedFormEntity(params));

            // Request g�nder
            HttpResponse response = httpClient.execute(httpPost);

            // Response body�yi al
            String responseBody = EntityUtils.toString(response.getEntity());
            System.out.println("responseBody = " + responseBody);

            // Access Token�� JSON�dan ��kar
            accessToken = extractAccessToken(responseBody);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // ? Kullan�c�ya JWT token ile birlikte d�n
        return ResponseEntity.ok(new JwtResponse(accessToken, user.getId(), user.getUsername(), user.getEmail(),user.getRole()));
    }

    /**
     * ?? JSON response�tan "access_token" de�erini ��kar�r.
     */
    private static String extractAccessToken(String jsonResponse) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            return rootNode.path("access_token").asText();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ?? Yeni kullan�c� kayd�
     */
    public ResponseEntity<?> registerUser(SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Email is already in use!"));
        }

        PasswordEncoder encoder = new BCryptPasswordEncoder();

        // ?? Yeni kullan�c�y� olu�tur ve kaydet
        User user = new User(
                signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()),
                "Customer"
        );

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    /**
     * ? Kullan�c�y� sil.
     * - �nce ShoppingCartService �a�r�l�r (kullan�c�n�n sepeti varsa silinir).
     * - Sonra User DB�den silinir.
     */
    public ResponseEntity<?> deleteUser(Long userId) {
        try {
            // Kullan�c� var m� kontrol et
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found!"));

            try {
                // Kullan�c�n�n shopping cart��n� bul
                ShoppingCart shoppingCart = restTemplate.getForObject(
                        "http://SHOPPING-CART-SERVICE/api/shopping-cart/by-name/" + user.getUsername(),
                        ShoppingCart.class);

                // Shopping cart varsa sil
                restTemplate.delete("http://SHOPPING-CART-SERVICE/api/shopping-cart/" + shoppingCart.getId());
            } catch (Exception e) {
                // Sepet bulunamazsa sorun de�il, user silmeye devam
            }

            // Kullan�c�y� sil
            userRepository.delete(user);

            return ResponseEntity.ok(new MessageResponse("User account deleted successfully!"));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(new MessageResponse("Internal Server Error"));
        }
    }

    /**
     * ?? Kullan�c� bilgilerini g�ncelle
     */
    public ResponseEntity<?> updateUser(Long userId, UpdateUserRequest updateUserRequest) {
        try {
            // Kullan�c�y� bul
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found!"));

            // �ifre g�ncelle
            if (updateUserRequest.getPassword() != null && !updateUserRequest.getPassword().isEmpty()) {
                PasswordEncoder encoder = new BCryptPasswordEncoder();
                user.setPassword(encoder.encode(updateUserRequest.getPassword()));
            }

            // Email g�ncelle
            if (updateUserRequest.getEmail() != null && !updateUserRequest.getEmail().isEmpty()) {
                if (userRepository.existsByEmail(updateUserRequest.getEmail())) {
                    return ResponseEntity.badRequest().body(new MessageResponse("Email is already in use!"));
                }
                user.setEmail(updateUserRequest.getEmail());
            }

            // Kaydet
            userRepository.save(user);

            return ResponseEntity.ok(new MessageResponse("User account updated successfully!"));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(new MessageResponse("Internal Server Error"));
        }
    }
}

