package com.adobe.printservice.web;

import java.util.Map;

public record CreateJobRequest(String templateId, Map<String, Object> parameters) {
}
