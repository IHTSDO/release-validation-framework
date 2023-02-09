package org.ihtsdo.rvf.api.helper;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;

@Component
public class ControllerHelper {

    public static ResponseEntity <Void> getCreatedResponse(String id) {
        return getCreatedResponse(id, "");
    }

    public static ResponseEntity <Void> getCreatedResponse(String id, String removePathPart) {
        HttpHeaders httpHeaders = getCreatedLocationHeaders(id, removePathPart, null);
        return new ResponseEntity <>(httpHeaders, HttpStatus.CREATED);
    }

    public static ResponseEntity <Void> getCreatedResponse(String id, MultiValueMap <String, String> queryParams) {
        HttpHeaders httpHeaders = getCreatedLocationHeaders(id, null, queryParams);
        return new ResponseEntity <>(httpHeaders, HttpStatus.CREATED);
    }

    public static HttpHeaders getCreatedLocationHeaders(String id) {
        return getCreatedLocationHeaders(id, null, null);
    }

    public static HttpHeaders getCreatedLocationHeaders(String id, String removePathPart, MultiValueMap <String, String> params) {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        Assert.state(attrs instanceof ServletRequestAttributes, "No current ServletRequestAttributes");
        HttpServletRequest request = ((ServletRequestAttributes) attrs).getRequest();

        String requestUrl = request.getRequestURL().toString();
        // Decode branch path
        requestUrl = requestUrl.replace("%7C", "/");
        if (!StringUtils.isEmpty(removePathPart)) {
            requestUrl = requestUrl.replace(removePathPart, "");
        }

        HttpHeaders httpHeaders = new HttpHeaders();
        if (params != null) {
            httpHeaders.setLocation(ServletUriComponentsBuilder.fromHttpUrl(requestUrl).path("/{id}").queryParams(params).buildAndExpand(id).toUri());
        } else {
            httpHeaders.setLocation(ServletUriComponentsBuilder.fromHttpUrl(requestUrl).path("/{id}").buildAndExpand(id).toUri());
        }
        return httpHeaders;
    }
}
