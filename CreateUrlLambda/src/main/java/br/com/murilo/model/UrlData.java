package br.com.murilo.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class UrlData {

    private String originalUrl;
    private long expirationTime;
}
