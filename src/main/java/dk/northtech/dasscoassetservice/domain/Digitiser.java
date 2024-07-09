package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

public class Digitiser {
    @Schema(description = "Unique Identifier")
    public String userId;
    @Schema(description = "Username of the User")
    public String name;

    public Digitiser(String userId, String name){
        this.userId = userId;
        this.name = name;
    }
}
