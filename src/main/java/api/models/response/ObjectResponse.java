package api.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * GET returns id/name/data. POST adds createdAt, PUT/PATCH add updatedAt —
 * both are null on responses that don't include them.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ObjectResponse {
    private String id;
    private String name;
    private Map<String, Object> data;
    private Long createdAt;
    private Long updatedAt;
}
