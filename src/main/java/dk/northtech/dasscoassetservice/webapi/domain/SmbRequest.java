package dk.northtech.dasscoassetservice.webapi.domain;

import dk.northtech.dasscoassetservice.domain.MinimalAsset;

import java.util.ArrayList;
import java.util.List;

public class SmbRequest {
    public List<String> users = new ArrayList<>();
    public List<MinimalAsset> assets = new ArrayList<>();
}
