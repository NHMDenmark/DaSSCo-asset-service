package dk.northtech.dasscoassetservice.webapi.domain;

import dk.northtech.dasscoassetservice.domain.MinimalAsset;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HttpShareRequest {
    public Set<String> users = new HashSet<>();
    public List<MinimalAsset> assets = new ArrayList<>();
    public int allocation_mb;
}
