package com.tmdt.marketplace.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import com.tmdt.marketplace.service.MarketplaceService.MasterDataOption;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GHNService {
    private final String ghnToken;
    private final String ghnShopId;
    private final String ghnBaseUrl;
    private final Integer ghnFromDistrictId;
    private final String ghnFromWardCode;
    private final Integer ghnServiceTypeId;
    private final Integer ghnPackageLength;
    private final Integer ghnPackageWidth;
    private final Integer ghnPackageHeight;
    private final RestTemplate restTemplate;

    public GHNService(
            @Value("${ghn.token:}") String ghnToken,
            @Value("${ghn.shop-id:}") String ghnShopId,
            @Value("${ghn.base-url:https://online-gateway.ghn.vn/shiip/public-api}") String ghnBaseUrl,
            @Value("${ghn.from-district-id:1442}") Integer ghnFromDistrictId,
            @Value("${ghn.from-ward-code:20101}") String ghnFromWardCode,
            @Value("${ghn.service-type-id:2}") Integer ghnServiceTypeId,
            @Value("${ghn.package.length:20}") Integer ghnPackageLength,
            @Value("${ghn.package.width:15}") Integer ghnPackageWidth,
            @Value("${ghn.package.height:10}") Integer ghnPackageHeight
    ) {
        this.ghnToken = ghnToken;
        this.ghnShopId = ghnShopId;
        this.ghnBaseUrl = ghnBaseUrl;
        this.ghnFromDistrictId = ghnFromDistrictId;
        this.ghnFromWardCode = ghnFromWardCode;
        this.ghnServiceTypeId = ghnServiceTypeId;
        this.ghnPackageLength = ghnPackageLength;
        this.ghnPackageWidth = ghnPackageWidth;
        this.ghnPackageHeight = ghnPackageHeight;
        this.restTemplate = new RestTemplate();
    }

    private JsonNode callGhnGet(String path, Map<String, Object> queryParams) {
        requireGhnToken();
        HttpHeaders headers = new HttpHeaders();
        headers.set("token", ghnToken);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(ghnBaseUrl + path);
        if (queryParams != null) {
            queryParams.forEach(builder::queryParam);
        }
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                JsonNode.class
        );
        JsonNode body = response.getBody();
        if (body != null && body.has("data")) {
            return body.get("data");
        }
        return null;
    }

    private boolean hasGhnToken() {
        return StringUtils.hasText(ghnToken);
    }

    private boolean hasGhnFeeCredentials() {
        return hasGhnToken() && StringUtils.hasText(ghnShopId);
    }

    private void requireGhnToken() {
        if (!hasGhnToken()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Chua cau hinh GHN_TOKEN nen khong the lay du lieu dia chi GHN.");
        }
    }

    private void requireGhnFeeCredentials() {
        if (!hasGhnFeeCredentials()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Chua cau hinh GHN_TOKEN/GHN_SHOP_ID nen khong the tinh phi GHN.");
        }
    }

    public List<MasterDataOption> getProvinces() {
        try {
            JsonNode data = callGhnGet("/master-data/province", null);
            List<MasterDataOption> list = new ArrayList<>();
            if (data != null && data.isArray()) {
                for (JsonNode node : data) {
                    list.add(new MasterDataOption(node.path("ProvinceID").asText(), node.path("ProvinceName").asText()));
                }
            }
            return list;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Khong lay duoc tinh/thanh tu GHN.", e);
        }
    }

    public List<MasterDataOption> getDistricts(Integer provinceId) {
        if (provinceId == null) return new ArrayList<>();
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("province_id", provinceId);
            JsonNode data = callGhnGet("/master-data/district", params);
            List<MasterDataOption> list = new ArrayList<>();
            if (data != null && data.isArray()) {
                for (JsonNode node : data) {
                    list.add(new MasterDataOption(node.path("DistrictID").asText(), node.path("DistrictName").asText()));
                }
            }
            return list;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Khong lay duoc quan/huyen tu GHN.", e);
        }
    }

    public List<MasterDataOption> getWards(Integer districtId) {
        if (districtId == null) return new ArrayList<>();
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("district_id", districtId);
            JsonNode data = callGhnGet("/master-data/ward", params);
            List<MasterDataOption> list = new ArrayList<>();
            if (data != null && data.isArray()) {
                for (JsonNode node : data) {
                    list.add(new MasterDataOption(node.path("WardCode").asText(), node.path("WardName").asText()));
                }
            }
            return list;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Khong lay duoc phuong/xa tu GHN.", e);
        }
    }

    public BigDecimal calculateFee(Integer toDistrictId, String toWardCode, int quantity) {
        if (toDistrictId == null || toWardCode == null || toWardCode.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            requireGhnFeeCredentials();
            HttpHeaders headers = new HttpHeaders();
            headers.set("token", ghnToken);
            headers.set("ShopId", ghnShopId);

            Map<String, Object> body = new HashMap<>();
            body.put("from_district_id", ghnFromDistrictId);
            body.put("from_ward_code", ghnFromWardCode);
            body.put("service_type_id", ghnServiceTypeId);
            body.put("to_district_id", toDistrictId);
            body.put("to_ward_code", toWardCode);
            body.put("height", ghnPackageHeight);
            body.put("length", ghnPackageLength);
            body.put("width", ghnPackageWidth);
            body.put("weight", 200 * quantity); // 200g per item
            body.put("insurance_value", 0);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    ghnBaseUrl + "/v2/shipping-order/fee",
                    HttpMethod.POST,
                    entity,
                    JsonNode.class
            );

            JsonNode resBody = response.getBody();
            if (resBody != null && resBody.has("data") && resBody.get("data").has("total")) {
                return BigDecimal.valueOf(resBody.get("data").get("total").asLong());
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Khong tinh duoc phi van chuyen tu GHN.", e);
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "GHN khong tra ve tong phi van chuyen.");
    }
}
